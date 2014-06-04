package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.OutputChannel;
import icecube.daq.juggler.alert.AlertException;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.PayloadFormatException;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.ReadoutRequest;
import icecube.daq.payload.impl.TriggerRequest;
import icecube.daq.splicer.Spliceable;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.FlushRequest;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.exceptions.MultiplicityDataException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Collect requests from trigger algorithms in the proper order.
 */
public class TriggerCollector
{
    private static final Log LOG = LogFactory.getLog(TriggerCollector.class);

    private ICollectorThread collThrd;
    private ITruncateThread truncThrd;
    private IOutputThread outThrd;

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
                            IByteBufferCache outCache,
                            IMonitoringDataManager moniDataMgr)
    {
        if (algorithms == null || algorithms.size() == 0) {
            throw new Error("No algorithms specified");
        }

        if (outputEngine == null) {
            throw new Error("Trigger request output engine is not set");
        }

        if (outCache == null) {
            throw new Error("Output cache is not set");
        }

        for (INewAlgorithm a : algorithms) {
            a.setTriggerCollector(this);
        }

        truncThrd = createTruncateThread("TruncateThread");

        outThrd = createOutputThread("OutputThread", srcId, outputEngine,
                                     outCache, truncThrd);

        collThrd = createCollectorThread("TriggerCollector", srcId, algorithms,
                                         moniDataMgr, outThrd);
    }

    public ICollectorThread createCollectorThread(String name, int srcId,
                                                  List<INewAlgorithm> algo,
                                                  IMonitoringDataManager mdm,
                                                  IOutputThread outThrd)
    {
        return new CollectorThread("TriggerCollector", srcId, algo,
                                   mdm, outThrd);
    }

    public IOutputThread createOutputThread(String name, int srcId,
                                            DAQComponentOutputProcess outEng,
                                            IByteBufferCache outCache,
                                            ITruncateThread truncThrd)
    {
        return new OutputThread(name, srcId, outEng, outCache, truncThrd);
    }

    public ITruncateThread createTruncateThread(String name)
    {
        return new TruncateThread(name);
    }

    /**
     * Return the number of requests queued for writing.
     *
     * @return output queue size
     */
    public long getNumQueued()
    {
        return outThrd.getNumQueued();
    }

    /**
     * Has the output thread stopped?
     *
     * @return <tt>true</tt> if the thread has stopped
     */
    public boolean isStopped()
    {
        return outThrd.isStopped();
    }

    /**
     * Reset the UID in order to switch to a new run.
     */
    public void resetUID()
    {
        collThrd.resetUID();
    }

    /**
     * Notify the collector thread that one or more lists has changed.
     */
    public void setChanged()
    {
        collThrd.setChanged();
    }

    public void setRunNumber(int runNumber)
    {
        collThrd.setRunNumber(runNumber);
    }

    /**
     * Start collector and output threads.
     *
     * @param splicer object to which requests are sent
     */
    public void startThreads(Splicer splicer, int runNum)
    {
        if (splicer == null) {
            LOG.error("Splicer cannot be null");
        }

        collThrd.start(splicer, runNum);
    }

    /**
     * Signal to all threads that they should stop as soon as possible.
     */
    public void stop()
    {
        collThrd.stop();
    }
}

interface ICollectorThread
{
    void resetUID();

    void setChanged();

    void setRunNumber(int runNumber);

    void start(Splicer splicer, int runNumber);

    void stop();
}

class CollectorThread
    implements ICollectorThread, Runnable
{
    private static final Log LOG = LogFactory.getLog(CollectorThread.class);

    int srcId;
    private List<INewAlgorithm> algorithms;

    /** Multiplicity data manager */
    private IMonitoringDataManager moniDataMgr;

    /** SNDAQ alerter */
    private SNDAQAlerter alerter;

    private int mergedUID;
    private boolean switchMerged;

    private Object threadLock = new Object();
    private Thread thread;

    private boolean changed;

    private IOutputThread outThrd;

    private boolean stopping;

    private int runNumber = Integer.MIN_VALUE;

    public CollectorThread(String name, int srcId,
                           List<INewAlgorithm> algorithms,
                           IMonitoringDataManager moniDataMgr,
                           IOutputThread outThrd)
    {
        thread = new Thread(this);
        thread.setName(name);

        this.srcId = srcId;
        this.algorithms = new ArrayList<INewAlgorithm>(algorithms);
        this.moniDataMgr = moniDataMgr;

        this.outThrd = outThrd;

        // in-ice trigger should try to send SMT8 alerts to SNDAQ
        if (srcId / 1000 == SourceIdRegistry.INICE_TRIGGER_SOURCE_ID / 1000) {
            initializeSNDAQAlerter(algorithms);
        }
    }

    /**
     * Add all requests within the interval to the list.
     *
     * @param interval time interval
     * @param list list of requests
     */
    public void addRequests(Interval interval,
                            List<ITriggerRequestPayload> list)
    {
        for (INewAlgorithm a : algorithms) {
            a.release(interval, list);
        }
    }

    public Interval findInterval()
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

    private void initializeSNDAQAlerter(List<INewAlgorithm> algorithms)
    {
        try {
            alerter = new SNDAQAlerter();
        } catch (AlertException ae) {
            LOG.error("Cannot create SNDAQ alerter;" +
                      " no SNDAQ alerts will be sent", ae);
            alerter = null;
            return;
        }

        alerter.loadAlgorithms(algorithms);
    }

    private void notifySNDAQ(List<ITriggerRequestPayload> list)
    {
        if (!alerter.isActive()) {
            LOG.error("Alerter " + alerter + " is not active");
            return;
        }

        for (ITriggerRequestPayload req : list) {
            try {
                alerter.process(req);
            } catch (AlertException ae) {
                LOG.error("SNDAQ alerter cannot process " + req, ae);
            }
        }
    }

    public void pushTrigger(ITriggerRequestPayload req)
    {
        if (srcId == SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID) {
            if (req.getUID() == 0) {
                boolean doReset;
                try {
                    doReset = moniDataMgr.send();
                } catch (MultiplicityDataException mde) {
                    LOG.error("Failed to send multiplicity data", mde);
                    doReset = true;
                }

                if (doReset) {
                    try {
                        moniDataMgr.reset();
                    } catch (MultiplicityDataException mde) {
                        LOG.error("Failed to reset multiplicity data", mde);
                    }
                }

                // if we've switching runs, reset mergedUID
                if (switchMerged) {
                    switchMerged = false;
                    mergedUID = 0;
                }
            }

            try {
                moniDataMgr.add(req);
            } catch (MultiplicityDataException mde) {
                LOG.error("Cannot add multiplicity data", mde);
            }
        }

        outThrd.push(req);
    }

    /**
     * Reset the UID in order to switch to a new run.
     */
    public void resetUID()
    {
        outThrd.resetUID();
    }

    /**
     * Run the collector thread.
     */
    public void run()
    {
        Interval oldInterval = null;
        List<ITriggerRequestPayload> requestCache =
            new ArrayList<ITriggerRequestPayload>();

        // let SNDAQ know a run is starting
        if (runNumber == Integer.MIN_VALUE) {
            LOG.error("Run number has not been set for CollectorThread.run()");
        } else if (alerter != null) {
            if (!alerter.isActive()) {
                LOG.error("Alerter " + alerter + " is not active");
            } else {
                alerter.sendAction("start", runNumber);
            }
        }

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

                    outThrd.notifyThread();

                    break;
                }

                if (oldInterval == null) {
                    // cache the first batch of requests
                    addRequests(interval, requestCache);
                    oldInterval = interval;
                } else if (interval.start > oldInterval.end) {
                    // send cached requests
                    sendRequests(oldInterval, requestCache);

                    // cache current requests
                    requestCache.clear();
                    addRequests(interval, requestCache);
                    oldInterval = interval;
                } else {
                    // Deal with overlapping request
                    if (interval.end < oldInterval.start) {
                        LOG.error("New interval " + interval +
                                  " precedes old interval " + oldInterval);
                        oldInterval = null;
                        stopping = true;
                        break;
                    } else {
                        addRequests(interval, requestCache);
                        if (interval.start < oldInterval.start) {
                            oldInterval.start = interval.start;
                        }
                        if (interval.end > oldInterval.end) {
                            oldInterval.end = interval.end;
                        }
                    }
                }
            }
        }

        if (oldInterval != null) {
            // send final batch of requests
            sendRequests(oldInterval, requestCache);
        }

        outThrd.stop();

        if (alerter != null) {
            if (runNumber != Integer.MIN_VALUE) {
                alerter.sendAction("stop", runNumber);
            }

            alerter.close();
        }

        // recycle all unused requests still held by the algorithms.
        for (INewAlgorithm a : algorithms) {
            a.recycleUnusedRequests();
        }
    }

    public void sendRequests(Interval interval,
                             List<ITriggerRequestPayload> list)
    {
        // if there's an active SNDAQ alerter, hand off the list of requests
        if (alerter != null) {
            notifySNDAQ(list);
        }

        if (list.size() == 0) {
            LOG.error("No requests found for interval " + interval);
        } else if (list.size() == 1) {
            pushTrigger(list.get(0));
        } else {
            TriggerRequestComparator trigReqCmp =
                new TriggerRequestComparator();
            Collections.sort((List) list, trigReqCmp);

            mergedUID++;

            ReadoutRequest rReq =
                new ReadoutRequest(interval.start, mergedUID, srcId);
            if (srcId == SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID) {
                ElementMerger.merge(rReq, list);
            }

            ArrayList<IWriteablePayload> hack =
                new ArrayList<IWriteablePayload>(list);
            ITriggerRequestPayload mergedReq =
                new TriggerRequest(mergedUID, -1, -1, srcId, interval.start,
                                   interval.end, rReq, hack);
            pushTrigger(mergedReq);
        }
    }

    /**
     * Notify the collector thread that one or more lists has changed.
     */
    public void setChanged()
    {
        synchronized (threadLock) {
            changed = true;
            threadLock.notify();
        }
    }

    public void setRunNumber(int runNumber)
    {
        this.runNumber = runNumber;
    }

    public void start(Splicer splicer, int runNumber)
    {
        setRunNumber(runNumber);

        thread.start();

        outThrd.start(splicer);
    }

    public void stop()
    {
        stopping = true;

        synchronized (threadLock) {
            threadLock.notify();
        }

        outThrd.notifyThread();
    }
}

interface IOutputThread
{
    long getNumQueued();

    boolean isStopped();

    void notifyThread();

    void push(ITriggerRequestPayload req);

    void resetUID();

    void start(Splicer splicer);

    void stop();
}

/**
 * Class which writes triggers to output channel.
 */
class OutputThread
    implements IOutputThread, Runnable
{
    private static final Log LOG = LogFactory.getLog(OutputThread.class);

    private Thread thread;
    private boolean waiting;
    private boolean stopping;
    private boolean stopped;

    private boolean isGlobalTrigger;

    /** Output queue. */
    private Deque<ByteBuffer> outputQueue =
        new ArrayDeque<ByteBuffer>();

    /** Outgoing byte buffer cache. */
    private IByteBufferCache outCache;

    /** Output process */
    private DAQComponentOutputProcess outputEngine;

    /** Output channel */
    private OutputChannel outChan;

    /** Global trigger UID which will eventually be the event UID */
    private int eventUID = 1;

    /** Thread which manages splicer truncation */
    private ITruncateThread truncThrd;

    /**
     * Create and start output thread.
     *
     * @param name thread name
     * @param srcId trigger handler ID (used when creating merged triggers)
     * @param outputEngine object which writes out requests
     * @param outCache output payload cache
     * @param splicer splicer
     */
    public OutputThread(String name, int srcId,
                        DAQComponentOutputProcess outputEngine,
                        IByteBufferCache outCache, ITruncateThread truncThrd)
    {
        thread = new Thread(this);
        thread.setName(name);

        this.outputEngine = outputEngine;
        this.outCache = outCache;
        this.truncThrd = truncThrd;

        isGlobalTrigger = srcId == SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID;
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
     * Is the thread alive?
     *
     * @return <tt>true</tt> if the thread is running
     */
    public boolean isAlive()
    {
        return thread.isAlive();
    }

    public boolean isStopped()
    {
        return stopped;
    }

    public boolean isWaiting()
    {
        return waiting;
    }

    /**
     * Change non-merged Throughput requests to match old global trigger
     * handler behavior (readout request source should match enclosed
     * trigger's source instead of using the global trigger's source).
     *
     * @param req trigger request to possibly modify
     */
    public static boolean makeBackwardCompatible(ITriggerRequestPayload req)
    {
        // only modify merged requests
        if (req.getTriggerType() != -1 || req.getTriggerConfigID() != -1) {
            return true;
        }

        List payList;
        try {
            payList = req.getPayloads();
        } catch (PayloadFormatException pfe) {
            LOG.error("Cannot get list of payloads from " + req, pfe);
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
            } catch (PayloadFormatException pfe) {
                LOG.error("Cannot get list of subpayloads from " + tr,
                          pfe);
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
            if (rReq == null) {
                LOG.error("Cannot find readout request for request " + req);
                fixed = false;
                continue;
            }

            rReq.setSourceID(lowSrcId);
        }

        return fixed;
    }

    public void notifyThread()
    {
        synchronized (outputQueue) {
            outputQueue.notify();
        }
    }

    public void push(ITriggerRequestPayload req)
    {
        if (isGlobalTrigger) {
            // global requests need a unique ID since they'll
            // be turned into events
            req.setUID(eventUID++);
            // modify non-merged requests to match old behavior
            makeBackwardCompatible(req);
        }

        int bufLen = req.length();

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
        truncThrd.truncate(new DummyPayload(req.getFirstTimeUTC()));

        // now recycle it
        req.recycle();
    }

    public void resetUID()
    {
        eventUID = 1;
    }

    /**
     * Main output loop.
     */
    public void run()
    {
        ByteBuffer trigBuf;
        while (!stopping || outputQueue.size() > 0) {
            synchronized (outputQueue) {
                if (outputQueue.size() == 0) {
                    try {
                        waiting = true;
                        outputQueue.wait();
                    } catch (InterruptedException ie) {
                        LOG.error("Interrupt while waiting for output" +
                                  " queue", ie);
                    }
                    waiting = false;
                }

                if (outputQueue.size() == 0) {
                    trigBuf = null;
                } else {
                    trigBuf = outputQueue.removeFirst();
                }
            }

            if (trigBuf == null) {
                continue;
            }

            // if we haven't already, get the output channel
            if (outChan == null) {
                outChan = outputEngine.getChannel();
                if (outChan == null) {
                    throw new Error("Output channel has not been set" +
                                    " in " + outputEngine);
                }
            }

            //--ship the trigger to its destination
            outChan.receiveByteBuffer(trigBuf);
        }

        if (outChan != null) {
            outChan.sendLastAndStop();
        }

        stopped = true;
    }

    public void start(Splicer splicer)
    {
        thread.start();

        truncThrd.start(splicer);
    }

    public void stop()
    {
        synchronized (outputQueue) {
            stopping = true;
            outputQueue.notify();
        }

        truncThrd.stop();
    }
}

interface ITruncateThread
{
    void start(Splicer splicer);

    void stop();

    void truncate(Spliceable spl);
}

class TruncateThread
    implements ITruncateThread, Runnable
{
    private static final Log LOG = LogFactory.getLog(TruncateThread.class);

    private Thread thread;
    private Splicer splicer;

    private Object threadLock = new Object();
    private Spliceable nextTrunc;
    private boolean stopping;

    TruncateThread(String name)
    {
        thread = new Thread(this);
        thread.setName(name);
    }

    public void run()
    {
        if (splicer == null) {
            LOG.error("Splicer has not been set");
            return;
        }

        while (!stopping) {
            Spliceable spl;
            synchronized (threadLock) {
                if (nextTrunc == null) {
                    try {
                        threadLock.wait();
                    } catch (InterruptedException ie) {
                        // ignore interrupts
                        continue;
                    }
                }

                spl = nextTrunc;
                nextTrunc = null;
            }

            // XXX I'm not sure why, but 'spl' will occasionally be set to null
            if (spl != null) {
                // let the splicer know it's safe to recycle
                // everything before the end of this request
                try {
                    splicer.truncate(spl);
                } catch (Throwable thr) {
                    LOG.error("Truncate failed for " + spl, thr);
                }
            }
        }
    }

    public void start(Splicer splicer)
    {
        this.splicer = splicer;

        thread.start();
    }

    public void stop()
    {
        synchronized (threadLock) {
            stopping = true;
            threadLock.notify();
        }
    }

    public void truncate(Spliceable spl)
    {
        if (spl == null) {
            LOG.error("Cannot truncate null spliceable");
        } else {
            synchronized (threadLock) {
                nextTrunc = spl;
                threadLock.notify();
            }
        }
    }
}
