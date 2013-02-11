package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.OutputChannel;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.ReadoutRequest;
import icecube.daq.payload.impl.TriggerRequest;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.FlushRequest;
import icecube.daq.trigger.algorithm.INewAlgorithm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Collect requests from trigger algorithms in the proper order.
 */
public class TriggerCollector
    implements Runnable
{
    private static final Log LOG = LogFactory.getLog(TriggerCollector.class);

    private int srcId;
    private List<INewAlgorithm> algorithms;

    private Object threadLock = new Object();
    private boolean changed;

    private int mergedUID;

    private OutputThread outThrd;

    /** Output process */
    private DAQComponentOutputProcess outputEngine;

    /** Output channel */
    private OutputChannel outChan;

    /** Outgoing byte buffer cache. */
    private IByteBufferCache outCache;

    /** Output queue  -- ACCESS MUST BE SYNCHRONIZED. */
    private Deque<ByteBuffer> outputQueue =
        new ArrayDeque<ByteBuffer>();

    private boolean stopping;
    private boolean stopOutput;
    private boolean stopped;

    /**
     * Create a trigger collector.
     *
     * @param srcId trigger handler ID (used when creating merged triggers)
     * @param algorithms list of active trigger algorithms
     * @param outputEngine object which writes out requests
     * @param outCache output payload cache
     * @param splicer used in the output thread to truncate the splicer
     */
    public TriggerCollector(int srcId, List<INewAlgorithm> algorithms,
                            DAQComponentOutputProcess outputEngine,
                            IByteBufferCache outCache, Splicer splicer)
    {
        this.srcId = srcId;
        this.algorithms = new ArrayList<INewAlgorithm>(algorithms);
        this.outputEngine = outputEngine;
        this.outCache = outCache;

        if (outCache == null) {
            throw new Error("Output cache is not set");
        }

        for (INewAlgorithm a : algorithms) {
            a.setTriggerCollector(this);
        }

        Thread thread = new Thread(this);
        thread.setName("TriggerCollector");
        thread.start();

        outThrd = new OutputThread("OutputThread", splicer);
        outThrd.start();
    }

    private Interval findInterval()
    {
        Interval interval = new Interval();
        while (interval != null) {
            boolean sameInterval = true;
            for (INewAlgorithm a : algorithms) {
                Interval i2 = a.getInterval(interval);
                if (!interval.equals(i2)) {
                    sameInterval = false;
                }

                interval = i2;
                if (interval == null) {
                    break;
                }
            }

            if (sameInterval) {
                break;
            }
        }

        return interval;
    }

    /**
     * Return the number of requests queued for writing.
     *
     * @return output queue size
     */
    public long getNumQueued()
    {
        return outputQueue.size();
    }

    /**
     * Has the collector thread stopped?
     *
     * @return <tt>true</tt> if the thread has stopped
     */
    public boolean isStopped()
    {
        return stopped;
    }

    /**
     * Run the collector thread.
     */
    public void run()
    {
        Interval oldInterval = null;
        while (!stopping) {
            synchronized (threadLock) {
                if (!changed) {
                    try {
                        threadLock.wait();
                    } catch (InterruptedException ie) {
                        // ignore interrupts
                    }
                }

                changed = false;
            }

            while (true) {
                Interval interval = findInterval();
                if (interval == null || interval.isEmpty()) {
                    break;
                }

                if (interval.start == FlushRequest.FLUSH_TIME &&
                    interval.end == FlushRequest.FLUSH_TIME)
                {
                    stopping = true;

                    synchronized (outputQueue) {
                        outputQueue.notify();
                    }

                    break;
                }

                if (oldInterval != null && interval.start < oldInterval.end) {
                    throw new Error("Old interval " + oldInterval +
                                    " overlaps new interval " + interval);
                }
                oldInterval = interval;

                sendRequests(interval);
            }
        }

        stopOutput = true;

        synchronized (outputQueue) {
            outputQueue.notify();
        }

        // recycle all unused requests still cached in the algorithms.
        for (INewAlgorithm a : algorithms) {
            a.recycleUnusedRequests();
        }
    }

    private void sendRequests(Interval interval)
    {
        ArrayList<ITriggerRequestPayload> released =
            new ArrayList<ITriggerRequestPayload>();
        for (INewAlgorithm a : algorithms) {
            a.release(interval, released);
        }

        if (released.size() == 0) {
            LOG.error("No requests found for interval " + interval);
        } else if (released.size() == 1) {
            outThrd.push(released.get(0));
        } else {
            TriggerRequestComparator trigReqCmp =
                new TriggerRequestComparator();
            Collections.sort((List) released, trigReqCmp);

            mergedUID++;

            ReadoutRequest rReq =
                new ReadoutRequest(interval.start, mergedUID, srcId);
            if (srcId == SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID) {
                ElementMerger.merge(rReq, released);
            }

            // XXX shouldn't need to rewrite 'released' to 'outList'
            List<IWriteablePayload> outList =
                new ArrayList<IWriteablePayload>();
            for (ITriggerRequestPayload req : released) {
                outList.add(req);
            }

            ITriggerRequestPayload mergedReq =
                new TriggerRequest(mergedUID, -1, -1, srcId, interval.start,
                                   interval.end, rReq, outList);
            outThrd.push(mergedReq);
        }
    }

    /**
     * Notify the collector thread that one or more lists has changed.
     */
    public void setChanged()
    {
        // if all algorithms have finished, 'sawFlush' is set to true
        boolean sawFlush = true;
        for (INewAlgorithm a : algorithms) {
            sawFlush &= a.sawFlush();
            if (!sawFlush) {
                break;
            }
        }

        synchronized (threadLock) {
            changed = true;
            if (sawFlush) {
                stopping = true;
            }
            threadLock.notify();
        }
    }

    /**
     * Signal to all threads that they should stop as soon as possible.
     */
    public void stop()
    {
        stopping = true;

        synchronized (threadLock) {
            threadLock.notify();
        }

        synchronized (outputQueue) {
            outputQueue.notify();
        }
    }

    /**
     * Class which writes triggers to output channel.
     */
    class OutputThread
        implements Runnable
    {
        private Thread thread;
        private boolean waiting;

        /** Global trigger UID which will eventually be the event UID */
        private int eventUID = 1;

        /** Splicer needs to be told where to truncate its input */
        private Splicer splicer;

        /**
         * Create and start output thread.
         *
         * @param name thread name
         * @param splicer splicer
         */
        OutputThread(String name, Splicer splicer)
        {
            thread = new Thread(this);
            thread.setName(name);

            this.splicer = splicer;
        }

        /**
         * Change non-merged Throughput requests to match old global trigger
         * handler behavior (readout request source should match enclosed
         * trigger's source instead of using the global trigger's source).
         *
         * @param req trigger request to possibly modify
         */
        private boolean makeBackwardCompatible(ITriggerRequestPayload req)
        {
            // only modify merged requests
            if (req.getTriggerType() != -1 || req.getTriggerConfigID() != -1) {
                return true;
            }

            List payList;
            try {
                payList = req.getPayloads();
            } catch (DataFormatException dfe) {
                LOG.error("Cannot get list of payloads from " + req, dfe);
                return false;
            }

            // cannot fix this request if there are no subrequests
            if (payList == null || payList.size() == 0) {
                return false;
            }

            boolean fixed = true;
            for (Object obj : payList) {
                ITriggerRequestPayload tr = (ITriggerRequestPayload) obj;

                // XXX should verify that each subrequest is ThroughputTrigger

                List subList;
                try {
                    subList = tr.getPayloads();
                } catch (DataFormatException dfe) {
                    LOG.error("Cannot get list of subpayloads from " + tr,
                              dfe);
                    fixed = false;
                    continue;
                }

                if (subList == null || subList.size() != 1) {
                    LOG.error("Not fixing " + tr + "; found " +
                              (subList == null ? 0 : subList.size()) +
                              " enclosed requests");
                    fixed = false;
                    continue;
                }

                ITriggerRequestPayload lowest =
                    (ITriggerRequestPayload) subList.get(0);
                ISourceID lowSrcId = lowest.getSourceID();
                if (lowSrcId == null) {
                    LOG.error("Cannot find source ID of request " + lowest +
                              " enclosed by " + req);
                    fixed = false;
                    continue;
                }

                IReadoutRequest rReq = tr.getReadoutRequest();
                rReq.setSourceID(lowSrcId);
            }

            return fixed;
        }

        boolean isWaiting()
        {
            return waiting;
        }

        void push(ITriggerRequestPayload req)
        {
            if (srcId == SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID) {
                // global requests need a unique ID since they'll
                // be turned into events
                req.setUID(eventUID++);
                // modify non-merged requests to match old behavior
                makeBackwardCompatible(req);
            }

            int bufLen = req.getPayloadLength();

            // write trigger to allocated ByteBuffer
            ByteBuffer trigBuf = outCache.acquireBuffer(bufLen);
            try {
                req.writePayload(false, 0, trigBuf);
            } catch (IOException ioe) {
                LOG.error("Couldn't create payload", ioe);
                trigBuf = null;
            }

            if (trigBuf != null) {
                synchronized (outputQueue) {
                    outputQueue.addLast(trigBuf);
                    outputQueue.notify();
                }
            }

            // let the splicer know it's safe to recycle
            // everything before the end of this request
            splicer.truncate(new DummyPayload(req.getFirstTimeUTC()));

            // now recycle it
            req.recycle();
        }

        /**
         * Main output loop.
         */
        public void run()
        {
//System.err.println("OTtop");
            ByteBuffer trigBuf;
            while (!stopOutput || outputQueue.size() > 0) {
//System.err.println("OTloop");
                synchronized (outputQueue) {
//System.err.println("OTq="+outputQueue.size());
                    if (outputQueue.size() == 0) {
                        try {
                            waiting = true;
//System.err.println("OTwait");
                            outputQueue.wait();
                        } catch (InterruptedException ie) {
                            LOG.error("Interrupt while waiting for output" +
                                      " queue", ie);
                        }
                        waiting = false;
//System.err.println("OTawake");
                    }

                    if (outputQueue.size() == 0) {
//System.err.println("OTempty");
                        trigBuf = null;
                    } else {
                        trigBuf = outputQueue.removeFirst();
//System.err.println("OTgotBuf");
                    }
                }

                if (trigBuf == null) {
//System.err.println("OTcont");
                    continue;
                }

                // if we haven't already, get the output channel
                if (outChan == null) {
                    if (outputEngine == null) {
//System.err.println("OT!!noOut!!");
                        throw new Error("Trigger destination not set");
                    }

                    outChan = outputEngine.getChannel();
                    if (outChan == null) {
//System.err.println("OT!!noChan!!");
                        throw new Error("Output channel has not been set" +
                                        " in " + outputEngine);
                    }
                }

                //--ship the trigger to its destination
//System.err.println("OTsend");
                outChan.receiveByteBuffer(trigBuf);
            }
//System.err.println("OTexit");

            if (outChan != null) {
                outChan.sendLastAndStop();
            }

            stopped = true;
        }

        void start()
        {
            thread.start();
        }
    }
}
