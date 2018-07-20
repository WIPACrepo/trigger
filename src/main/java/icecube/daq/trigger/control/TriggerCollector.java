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
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.FlushRequest;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.exceptions.MultiplicityDataException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Collect requests from trigger algorithms in the proper order.
 */
public class TriggerCollector
    implements ITriggerCollector
{
    /** Monitoring task frequency (in seconds) */
    public static final long MONI_SECONDS = 600;

    private static final Log LOG = LogFactory.getLog(TriggerCollector.class);

    private ICollectorThread collThrd;
    private IOutputThread outThrd;

    /**
     * Create a trigger collector.
     *
     * @param srcId trigger handler ID (used when creating merged triggers)
     * @param algorithms list of active trigger algorithms
     * @param outputEngine object which writes out requests
     * @param outCache output payload cache
     */
    public TriggerCollector(int srcId, List<ITriggerAlgorithm> algorithms,
                            DAQComponentOutputProcess outputEngine,
                            IByteBufferCache outCache,
                            IMonitoringDataManager moniDataMgr,
                            SubscriptionManager subMgr)
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

        for (ITriggerAlgorithm a : algorithms) {
            a.setTriggerCollector(this);
        }

        outThrd = createOutputThread("OutputThread", srcId, outputEngine,
                                     outCache);

        collThrd = createCollectorThread("TriggerCollector", srcId, algorithms,
                                         moniDataMgr, outThrd, subMgr);
    }

    public ICollectorThread createCollectorThread(String name, int srcId,
                                                  List<ITriggerAlgorithm> algo,
                                                  IMonitoringDataManager mdm,
                                                  IOutputThread outThrd,
                                                  SubscriptionManager subMgr)
    {
        return new CollectorThread("TriggerCollector", srcId, algo,
                                   mdm, outThrd, subMgr);
    }

    public IOutputThread createOutputThread(String name, int srcId,
                                            DAQComponentOutputProcess outEng,
                                            IByteBufferCache outCache)
    {
        return new OutputThread(name, srcId, outEng, outCache);
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
     * Return the number of dropped SNDAQ alerts
     *
     * @return number of dropped alerts
     */
    public long getSNDAQAlertsDropped()
    {
        return collThrd.getSNDAQAlertsDropped();
    }

    /**
     * Return the number of alerts queued for writing.
     *
     * @return alerter queue size
     */
    public int getSNDAQAlertsQueued()
    {
        return collThrd.getSNDAQAlertsQueued();
    }

    /**
     * Return the number of alerts sent to SNDAQ
     *
     * @return number of alerts
     */
    public long getSNDAQAlertsSent()
    {
        return collThrd.getSNDAQAlertsSent();
    }

    /**
     * Get the number of requests collected from all algorithms
     *
     * @return total number of collected requests
     */
    public long getTotalCollected()
    {
        return collThrd.getTotalCollected();
    }

    /**
     * Get the number of requests released for collection
     *
     * @return total number of released requests
     */
    public long getTotalReleased()
    {
        return collThrd.getTotalReleased();
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

    public void setRunNumber(int runNumber, boolean isSwitched)
    {
        collThrd.setRunNumber(runNumber, isSwitched);
    }

    /**
     * Start collector and output threads.
     *
     * @param splicer object to which requests are sent
     */
    public void startThreads(Splicer splicer)
    {
        if (splicer == null) {
            LOG.error("Splicer cannot be null");
        }

        collThrd.start(splicer);
    }

    /**
     * Signal to all threads that they should stop as soon as possible.
     */
    public void stop()
    {
        collThrd.stop();
    }

    public String toString()
    {
        return "TrigColl[" + collThrd + "," + outThrd + "]";
    }
}

/**
 * Send periodic monitoring messages
 */
class MonitoringTask
    extends TimerTask
{
    private static final Log LOG = LogFactory.getLog(MonitoringTask.class);

    /** Multiplicity data manager */
    private IMonitoringDataManager moniDataMgr;

    MonitoringTask(IMonitoringDataManager moniDataMgr)
    {
        this.moniDataMgr = moniDataMgr;
    }

    public void run()
    {
        try {
            moniDataMgr.sendSingleBin(false);
        } catch (MultiplicityDataException mde) {
            LOG.error("Cannot send monitoring bin", mde);
        }
    }
}

interface ICollectorThread
{
    long getSNDAQAlertsDropped();

    int getSNDAQAlertsQueued();

    long getSNDAQAlertsSent();

    long getTotalCollected();

    long getTotalReleased();

    void resetUID();

    void setChanged();

    void setRunNumber(int runNumber, boolean isSwitched);

    void start(Splicer splicer);

    void stop();
}

class CollectorThread
    implements ICollectorThread, Runnable
{
    /**
     * Pass in <tt>-Dicecube.sndaq.ignore</tt> to avoid sending data to SNDAQ
     */
    public static final String IGNORE_SNDAQ_PROPERTY = "icecube.sndaq.ignore";

    private static final Log LOG = LogFactory.getLog(CollectorThread.class);

    /** Number of milliseconds in a second */
    private static final long MILLIS_PER_SECOND = 1000L;

    int srcId;
    private List<ITriggerAlgorithm> algorithms;

    private SubscriptionManager subMgr;

    /** Multiplicity data manager */
    private IMonitoringDataManager moniDataMgr;

    /** SNDAQ alerter */
    private SNDAQAlerter alerter;

    private int mergedUID;
    private boolean switchMerged;

    private Object threadLock = new Object();
    private Thread thread;

    private boolean changed;

    private long totalReleased;
    private long totalCollected;
    private long pushed;

    private IOutputThread outThrd;

    private List<TriggerThread> trigThreads = new ArrayList<TriggerThread>();

    private boolean stopping;
    private boolean stopped;

    private int runNumber = Integer.MIN_VALUE;

    private Timer moniTimer;

    public CollectorThread(String name, int srcId,
                           List<ITriggerAlgorithm> algorithms,
                           IMonitoringDataManager moniDataMgr,
                           IOutputThread outThrd, SubscriptionManager subMgr)
    {
        thread = new Thread(this);
        thread.setName(name);

        this.srcId = srcId;
        this.algorithms = new ArrayList<ITriggerAlgorithm>(algorithms);
        this.moniDataMgr = moniDataMgr;

        this.outThrd = outThrd;
        this.subMgr = subMgr;

        // in-ice trigger should try to send SMT8 alerts to SNDAQ
        if (srcId / 1000 == SourceIdRegistry.INICE_TRIGGER_SOURCE_ID / 1000) {
            if (System.getProperty(IGNORE_SNDAQ_PROPERTY, null) == null) {
                initializeSNDAQAlerter(algorithms);
            }
        }

        createTriggerThreads(algorithms);
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
        for (ITriggerAlgorithm a : algorithms) {
            totalReleased += a.release(interval, list);
        }
    }

    private void createTriggerThreads(List<ITriggerAlgorithm> algorithms)
    {
        int id = 0;
        for (ITriggerAlgorithm algo : algorithms) {
            TriggerThread thread = new TriggerThread(id, algo);

            trigThreads.add(thread);

            id++;
        }
    }

    public Interval findInterval()
    {
        Interval interval = new Interval();
        while (interval != null) {
            boolean sameInterval = true;
            for (ITriggerAlgorithm a : algorithms) {
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

    public long getNumPushed()
    {
        return pushed;
    }

    public long getSNDAQAlertsDropped()
    {
        return alerter.getNumDropped();
    }

    public int getSNDAQAlertsQueued()
    {
        return alerter.getNumQueued();
    }

    public long getSNDAQAlertsSent()
    {
        return alerter.getNumSent();
    }

    /**
     * Get the number of requests collected from all algorithms
     *
     * @return total number of collected requests
     */
    public long getTotalCollected()
    {
        return totalCollected;
    }

    /**
     * Get the number of requests released for collection
     *
     * @return total number of released requests
     */
    public long getTotalReleased()
    {
        return totalReleased;
    }

    private void initializeSNDAQAlerter(List<ITriggerAlgorithm> algorithms)
    {
        try {
            alerter = new SNDAQAlerter(algorithms);
        } catch (AlertException ae) {
            LOG.error("Cannot create SNDAQ alerter;" +
                      " no SNDAQ alerts will be sent", ae);
            alerter = null;
            return;
        }
    }

    private void notifySNDAQ(List<ITriggerRequestPayload> list)
    {
        if (!alerter.isActive()) {
            LOG.error("Alerter " + alerter + " is not active");
            return;
        }

        if (runNumber != alerter.getRunNumber()) {
            alerter.setRunNumber(runNumber);
        }

        for (ITriggerRequestPayload req : list) {
            alerter.process(req);
        }
    }

    public void pushTrigger(ITriggerRequestPayload req)
    {
        if (srcId == SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID) {
            // if we're switching runs, reset mergedUID
            if (switchMerged) {
                if (moniTimer != null) {
                    // stop monitoring task(s)
                    moniTimer.cancel();
                    moniTimer = null;
                }

                try {
                    moniDataMgr.sendFinal();
                } catch (MultiplicityDataException mde) {
                    LOG.error("Failed to send monitoring data", mde);
                }

                try {
                    moniDataMgr.reset();
                } catch (MultiplicityDataException mde) {
                    LOG.error("Failed to reset multiplicity data", mde);
                }

                switchMerged = false;
                mergedUID = 0;
            }

            // we've got data, start monitoring task
            if (moniTimer == null) {
                final long period =
                    TriggerCollector.MONI_SECONDS * MILLIS_PER_SECOND;

                moniTimer = new Timer("Monitor#" + runNumber);
                moniTimer.scheduleAtFixedRate(new MonitoringTask(moniDataMgr),
                                              period, period);
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
                alerter.setRunNumber(runNumber);
            }
        }

        while (true) {
            if (stopping) {
                boolean algoStopped = true;
                for (ITriggerAlgorithm a : algorithms) {
                    if (a.hasData() || a.hasCachedRequests()) {
                        algoStopped = false;
                        break;
                    }
                }

                if (algoStopped) {
                    break;
                }
            }

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
                    // if the interval is a FLUSH, we're done
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

        stopTriggerThreads();

        outThrd.stop();

        if (alerter != null) {
            alerter.close();
        }

        // recycle all unused requests still held by the algorithms.
        for (ITriggerAlgorithm a : algorithms) {
            a.recycleUnusedRequests();
        }

        stopped = true;
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

            totalCollected++;
            pushed++;
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

            totalCollected += list.size();
            pushed++;
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

    public void setRunNumber(int runNumber, boolean isSwitched)
    {
        if (isSwitched) {
            switchMerged = true;
        }

        this.runNumber = runNumber;
    }

    public void start(Splicer splicer)
    {
        thread.start();

        outThrd.start(splicer);

        subMgr.subscribeAll();
        for (TriggerThread tt : trigThreads) {
            tt.start();
        }
    }

    public void stop()
    {
        stopping = true;

        synchronized (threadLock) {
            threadLock.notify();
        }

        outThrd.notifyThread();
    }

    public void stopTriggerThreads()
    {
        // finish all trigger threads
        for (TriggerThread thread : trigThreads) {
            thread.stop();
        }
        for (TriggerThread thread : trigThreads) {
            thread.join();
        }
        trigThreads.clear();

        subMgr.unsubscribeAll();
    }

    public String toString()
    {
        String stateStr;
        if (thread == null) {
            stateStr = "noThread";
        } else if (!thread.isAlive()) {
            stateStr = "dead";
        } else if (stopped) {
            stateStr = "stopped";
        } else if (stopping) {
            stateStr = "stopping";
        } else {
            stateStr = "running";
        }

        return "CollThrd[" + stateStr +
            ",uid=" + mergedUID +
            ",rel=" + totalReleased +
            ",coll=" + totalCollected +
            ",push=" + pushed + "]";
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
                        IByteBufferCache outCache)
    {
        thread = new Thread(this);
        thread.setName(name);

        this.outputEngine = outputEngine;
        this.outCache = outCache;

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
        boolean warnedChannel = false;

        ByteBuffer trigBuf;
        while (!stopping || outputQueue.size() > 0) {
            synchronized (outputQueue) {
                if (!stopping && outputQueue.size() == 0) {
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
                    if (!warnedChannel) {
                        LOG.error("Output channel has not been set in " +
                                  outputEngine + "; stopping");
                        warnedChannel = true;
                    }
                    stopping = true;
                }
            }

            //--ship the trigger to its destination
            if (outChan != null) {
                outChan.receiveByteBuffer(trigBuf);
            }
        }

        // yikes, must have stopped without sending anything
        if (outChan == null) {
            outChan = outputEngine.getChannel();
            if (outChan == null && !warnedChannel) {
                LOG.error("Output channel has not been set in " +
                          outputEngine + "; not sending last payload");
            }
        }

        // send stop message
        if (outChan != null) {
            outChan.sendLastAndStop();
        }

        stopped = true;
    }

    public void start(Splicer splicer)
    {
        thread.start();
    }

    public void stop()
    {
        synchronized (outputQueue) {
            stopping = true;
            outputQueue.notify();
        }
    }

    public String toString()
    {
        String stateStr;
        if (thread == null) {
            stateStr = "noThread";
        } else if (!thread.isAlive()) {
            stateStr = "dead";
        } else if (stopped) {
            stateStr = "stopped";
        } else if (stopping) {
            stateStr = "stopping";
        } else if (waiting) {
            stateStr = "waiting";
        } else {
            stateStr = "running";
        }

        return "OutThrd[" + stateStr + ",outQ#" + outputQueue.size() +
            ",uid=" + eventUID + "]";
    }
}
