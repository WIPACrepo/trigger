package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.AlertQueue;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadFormatException;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.TriggerRequestFactory;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.splicer.Spliceable;
import icecube.daq.splicer.SplicedAnalysis;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerChangedEvent;
import icecube.daq.splicer.SplicerListener;
import icecube.daq.trigger.algorithm.AlgorithmStatistics;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.exceptions.MultiplicityDataException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnimplementedError;
import icecube.daq.util.IDOMRegistry;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Compare two trigger requests.
 */
class TriggerRequestComparator
    implements Comparator<ITriggerRequestPayload>, Serializable
{
    /**
     * Create a comparator.
     */
    TriggerRequestComparator()
    {
    }

    /**
     * Compare two requests.
     *
     * @param tr1 first request
     * @param tr2 second request
     *
     * @return the usual values
     */
    @Override
    public int compare(ITriggerRequestPayload tr1, ITriggerRequestPayload tr2)
    {
        long val = tr1.getFirstTimeUTC().longValue() -
            tr2.getFirstTimeUTC().longValue();
        if (val == 0) {
            val = tr1.getLastTimeUTC().longValue() -
                tr2.getLastTimeUTC().longValue();
            if (val == 0) {
                val = tr1.getUID() - tr2.getUID();
                if (val == 0) {
                    val = tr1.getTriggerType() - tr2.getTriggerType();
                }
            }
        }

        if (val < 0) {
            return -1;
        } else if (val > 0) {
            return 1;
        }

        return 0;
    }

    /**
     * Unimplemented method required by the interface.
     *
     * @param obj compared object
     *
     * @return UnimplementedError
     */
    @Override
    public boolean equals(Object obj)
    {
        throw new UnimplementedError();
    }

    /**
     * Unimplemented method suggested by the interface.
     *
     * @return UnimplementedError
     */
    @Override
    public int hashCode()
    {
        throw new UnimplementedError();
    }
}

/**
 * Read in hits from the splicer and send them to the algorithms.
 */
public class TriggerManager
    implements ITriggerManager, SplicedAnalysis<Spliceable>,
               SplicerListener<Spliceable>, SubscriptionManager,
               TriggerManagerMBean
{
    /** Payload used to indicate that input is finished */
    public static final DummyPayload FLUSH_PAYLOAD =
        new DummyPayload(new UTCTime(0));

    /** Log object for this class */
    private static final Logger LOG = Logger.getLogger(TriggerManager.class);

    /** Source ID for this trigger component */
    private int srcId;

    private TriggerRequestFactory trFactory;
    private DAQComponentOutputProcess outputEngine;
    private List<ITriggerAlgorithm> algorithms =
        new ArrayList<ITriggerAlgorithm>();

    private SubscribedList queueList = new SubscribedList();

    /** gather histograms for monitoring */
    private MultiplicityDataManager multiDataMgr;
    private AlertQueue alertQueue;

    private IByteBufferCache outCache;
    private TriggerCollector collector;

    /** splicer associated with this manager */
    private Splicer splicer;

    private IDOMRegistry domRegistry;

    /** spliceable input count */
    private long inputCount;

    /**
     * source of last hit, used for monitoring
     */
    private ISourceID srcOfLastHit;

    /**
     * time of last hit, used for monitoring
     */
    private IUTCTime timeOfLastHit;

    /** Current run number */
    private int runNumber = Integer.MIN_VALUE;

    /**
     * Create a trigger manager.
     *
     * @param srcObj source ID object
     * @param outCache output buffer cache
     */
    public TriggerManager(ISourceID srcObj, IByteBufferCache outCache)
    {
        this.srcId = srcObj.getSourceID();
        this.outCache = outCache;

        trFactory = new TriggerRequestFactory(outCache);
        multiDataMgr = new MultiplicityDataManager();

        init();
    }

    /**
     * Add a trigger algorithm.
     *
     * @param trig algorithm being added
     */
    @Override
    public void addTrigger(ITriggerAlgorithm trig)
    {
        boolean good = true;
        for (ITriggerAlgorithm t : algorithms) {
            if ((trig.getTriggerType() == t.getTriggerType()) &&
                trig.getTriggerConfigId() == t.getTriggerConfigId() &&
                trig.getSourceId() == t.getSourceId())
            {
                final String msg =
                    String.format("Attempt to add duplicate trigger with" +
                                  " type %d cfgId %d srcId %d" +
                                  " (old %s, new %s)", t.getTriggerType(),
                                  t.getTriggerConfigId(), t.getSourceId(),
                                  trig.getTriggerName(), t.getTriggerName());
                LOG.error(msg);
                good = false;
                break;
            }
        }

        if (good) {
            algorithms.add(trig);
            trig.setTriggerManager(this);
            trig.setTriggerFactory(trFactory);

            multiDataMgr.addAlgorithm(trig);

            if (collector != null && !collector.isStopped()) {
                collector.stop();
            }
        }
    }

    /**
     * Add a list of algorithms for this handler
     *
     * @param list list of trigger algorithms to add
     */
    @Override
    public void addTriggers(Iterable<ITriggerAlgorithm> list)
    {
        for (ITriggerAlgorithm trig: list) {
            addTrigger(trig);
        }
    }

    /**
     * Add the list of algorithms configured for all other handlers, to
     * be used by the global trigger monitoring code
     *
     * @param extra list of additional trigger algorithms to add
     */
    public void addExtraAlgorithms(List<ITriggerAlgorithm> extra)
    {
        for (ITriggerAlgorithm trig: extra) {
            multiDataMgr.addAlgorithm(trig);
        }
    }

    /**
     * Add the new list of hits from the splicer to the input queue.
     *
     * @param splicedObjects list of hits
     */
    @Override
    public void analyze(List<Spliceable> splicedObjects)
    {
        if (queueList.isEmpty()) {
            throw new Error("No consumers for " + splicedObjects.size() +
                            " hits");
        }

        for (Spliceable spl : splicedObjects) {
            ILoadablePayload payload = (ILoadablePayload) spl;

            if (LOG.isDebugEnabled()) {
                LOG.debug("  Processing payload " + inputCount +
                          " with time " + payload.getPayloadTimeUTC());
            }

            if (isValidIncomingPayload(payload)) {
                synchronized (queueList) {
                    pushInput(payload);
                }

                inputCount++;
            } else {
                LOG.error("Ignoring invalid payload " + payload);
            }

            // we're done with this payload
            payload.recycle();
        }
    }

    /**
     * XXX Unimplemented.
     *
     * @param evt ignored
     */
    @Override
    public void disposed(SplicerChangedEvent<Spliceable> evt)
    {
        throw new UnimplementedError();
    }

    /**
     * XXX Unimplemented.
     *
     * @param evt ignored
     */
    @Override
    public void failed(SplicerChangedEvent<Spliceable> evt)
    {
        throw new UnimplementedError();
    }

    /**
     * flush the handler
     * including the input buffer, all triggers, and the output bag
     */
    @Override
    public void flush()
    {
        if (!queueList.isEmpty()) {
            try {
                queueList.push(FLUSH_PAYLOAD);
            } catch (Error err) {
                LOG.error("Failed to flush input list", err);
            }
        }
    }

    /**
     * Get the alert queue for messages sent to I3Live
     *
     * @return alert queue
     */
    @Override
    public AlertQueue getAlertQueue()
    {
        return alertQueue;
    }

    /**
     * Get list of algorithm I/O statistics
     *
     * @return list of ITriggerStatistics
     */
    @Override
    public Iterable<AlgorithmStatistics> getAlgorithmStatistics()
    {
        ArrayList<AlgorithmStatistics> list =
            new ArrayList<AlgorithmStatistics>(algorithms.size());

        for (ITriggerAlgorithm trigger : algorithms) {
            list.add(new AlgorithmStatistics(trigger));
        }

        return list;
    }

    /**
     * Get the DOM registry.
     *
     * @return DOM registry
     */
    @Override
    public IDOMRegistry getDOMRegistry()
    {
        return domRegistry;
    }

    /**
     * Get the number of requests queued for writing
     *
     * @return size of output queue
     */
    @Override
    public int getNumOutputsQueued()
    {
        if (collector == null) {
            return 0;
        }

        return (int) collector.getNumQueued();
    }

    /**
     * Get map of trigger names to number of queued hits
     *
     * @return map of {name : numQueuedHits}
     */
    @Override
    public Map<String, Integer> getQueuedInputs()
    {
        return queueList.getLengths();
    }

    /**
     * Get map of trigger names to number of queued requests
     *
     * @return map of {name : numQueuedRequests}
     */
    public Map<String, Integer> getQueuedRequests()
    {
        HashMap map = new HashMap<String, Integer>();
        for (ITriggerAlgorithm a : algorithms) {
            map.put(a.getTriggerName(), a.getNumberOfCachedRequests());
        }
        return map;
    }

    /**
     * Return a map of algorithm names to the time of their most recently
     * released request.  This can be useful for determining which algorithm
     * is causing the trigger output stream to stall.
     *
     * @return map of names to times
     */
    @Override
    public Map<String, Long> getReleaseTimes()
    {
        HashMap<String, Long> map = new HashMap<String, Long>();
        for (ITriggerAlgorithm a : algorithms) {
            map.put(a.getTriggerName(), a.getReleaseTime());
        }
        return map;
    }

    /**
     * Get the number of dropped SNDAQ alerts
     *
     * @return number of dropped SNDAQ alerts
     */
    @Override
    public long getSNDAQAlertsDropped()
    {
        if (collector == null) {
            return 0;
        }

        return collector.getSNDAQAlertsDropped();
    }

    /**
     * Get the number of SNDAQ alerts queued for writing
     *
     * @return number of queued SNDAQ alerts
     */
    @Override
    public int getSNDAQAlertsQueued()
    {
        if (collector == null) {
            return 0;
        }

        return collector.getSNDAQAlertsQueued();
    }

    /**
     * Get the number of alerts sent to SNDAQ
     *
     * @return number of SNDAQ alerts
     */
    @Override
    public long getSNDAQAlertsSent()
    {
        if (collector == null) {
            return 0;
        }

        return collector.getSNDAQAlertsSent();
    }

    /**
     * Get this component's source ID.
     *
     * @return source ID
     */
    @Override
    public int getSourceId()
    {
        return srcId;
    }

    /**
     * Get the total number of hits pushed onto the input queue
     *
     * @return total number of hits received from the splicer
     */
    @Override
    public long getTotalProcessed()
    {
        return inputCount;
    }

    /**
     * Get the number of requests collected from all algorithms
     *
     * @return number of collected requests
     */
    @Override
    public int getTotalRequestsCollected()
    {
        if (collector == null) {
            return 0;
        }

        return (int) collector.getTotalCollected();
    }

    /**
     * Get the number of requests released by all algorithms
     *
     * @return number of collected requests
     */
    @Override
    public int getTotalRequestsReleased()
    {
        if (collector == null) {
            return 0;
        }

        return (int) collector.getTotalReleased();
    }

    /**
     * Get any special monitoring quantities for all algorithms.
     *
     * @return map of {name-configID-quantity: quantityObject}
     */
    @Override
    public Map<String, Object> getTriggerMonitorMap()
    {
        HashMap<String, Object> map = new HashMap<String, Object>();

        for (ITriggerAlgorithm trigger : algorithms) {
            Map<String, Object> moniMap = trigger.getTriggerMonitorMap();
            if (moniMap != null && !moniMap.isEmpty()) {
                String trigName = trigger.getTriggerName() + "-" +
                    trigger.getTriggerConfigId();
                for (Map.Entry<String, Object> entry : moniMap.entrySet()) {
                    map.put(trigName + "-" + entry.getKey(), entry.getValue());
                }
            }
        }

        return map;
    }

    protected void init()
    {
        inputCount = 0;
    }

    /**
     * Are all data collection threads stopped?
     *
     * @return <tt>true</tt> if the data collection threads are stopped
     */
    @Override
    public boolean isStopped()
    {
        if (collector == null) {
            return true;
        }

        return collector.isStopped();
    }

    private boolean isValidIncomingPayload(ILoadablePayload payload)
    {
        int iType = payload.getPayloadInterfaceType();

        // make sure we have hit payloads (or hit data payloads)
        if (iType == PayloadInterfaceRegistry.I_HIT_PAYLOAD ||
            iType == PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD)
        {
            IHitPayload hit = (IHitPayload) payload;
            if (hit.getHitTimeUTC() == null) {
                LOG.error("Bad hit buf " + payload.getPayloadBacking() +
                          " len " + payload.length() +
                          " type " + payload.getPayloadType() +
                          " utc " + payload.getPayloadTimeUTC());
                return false;
            }

            // Calculate time since last hit
            long timeDiff;
            if (timeOfLastHit == null) {
                timeDiff = 0;
            } else {
                timeDiff = hit.getUTCTime() - timeOfLastHit.longValue();
            }

            // check to see if timeDiff is reasonable, if not ignore it
            if (timeDiff < 0) {
                LOG.error("Hit " + hit.getUTCTime() +
                          " from " + hit.getSourceID() +
                          " out of order! This time - Last time = " +
                          timeDiff + (srcOfLastHit == null ? "" :
                                      ", src of last hit = " +
                                      srcOfLastHit));
                return false;
            }

            timeOfLastHit = hit.getHitTimeUTC();
            srcOfLastHit = hit.getSourceID();
        } else if (iType == PayloadInterfaceRegistry.I_TRIGGER_REQUEST) {
            if (srcId != SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID) {
                LOG.error("Source #" + srcId +
                          " cannot process trigger requests");
                return false;
            }

            try {
                payload.loadPayload();
            } catch (IOException e) {
                LOG.error("Couldn't load payload", e);
                return false;
            } catch (PayloadFormatException e) {
                LOG.error("Couldn't load payload", e);
                return false;
            }

            ITriggerRequestPayload tPayload =
                (ITriggerRequestPayload) payload;

            if (tPayload.getSourceID() == null) {
                if (tPayload.length() == 0 &&
                    tPayload.getPayloadTimeUTC() == null &&
                    ((IPayload) tPayload).getPayloadBacking() == null)
                {
                    LOG.error("Ignoring recycled payload");
                } else {
                    LOG.error("Unexpected null SourceID in payload (len=" +
                              tPayload.length() + ", time=" +
                              (tPayload.getPayloadTimeUTC() == null ?
                               "null" : "" + tPayload.getPayloadTimeUTC()) +
                              ", buf=" + payload.getPayloadBacking() + ")");
                }

                return false;
            }
        } else {
            LOG.warn("TriggerHandler only knows about either HitPayloads or" +
                     " TriggerRequestPayloads!");
            return false;
        }

        return true;
    }

    private void pushInput(ILoadablePayload payload)
    {
        if (payload.getPayloadInterfaceType() !=
            PayloadInterfaceRegistry.I_TRIGGER_REQUEST)
        {
            // queue ordinary payload
            queueList.push((IPayload) payload.deepCopy());
        } else {
            try {
                payload.loadPayload();
            } catch (IOException ioe) {
                LOG.error("Cannot load new payload " + payload, ioe);
                return;
            } catch (PayloadFormatException pfe) {
                LOG.error("Cannot load new payload " + payload, pfe);
                return;
            }

            ITriggerRequestPayload req = (ITriggerRequestPayload) payload;
            if (!req.isMerged()) {
                // queue single trigger request
                queueList.push((IPayload) payload.deepCopy());
            } else {
                // extract list of merged triggers
                List subList;
                try {
                    subList = req.getPayloads();
                } catch (PayloadFormatException pfe) {
                    LOG.error("Cannot fetch triggers from " + req, pfe);
                    return;
                }

                if (subList == null || subList.isEmpty()) {
                    LOG.error("No subtriggers found in " + req);
                    return;
                }

                // queue individual triggers
                for (Object obj : subList) {
                    ITriggerRequestPayload sub = (ITriggerRequestPayload) obj;

                    try {
                        sub.loadPayload();
                    } catch (IOException ioe) {
                        LOG.error("Cannot load subtrigger " + sub, ioe);
                        continue;
                    } catch (PayloadFormatException pfe) {
                        LOG.error("Cannot load subtrigger " + sub, pfe);
                        continue;
                    }

                    queueList.push((IPayload) sub.deepCopy());
                }
            }
        }
    }

    public void resetUIDs()
    {
        for (ITriggerAlgorithm a : algorithms) {
            a.resetUID();
        }
        collector.resetUID();
    }

    /**
     * Send final monitoring messages
     */
    public void sendFinalMoni()
    {
        try {
            multiDataMgr.sendFinal();
        } catch (MultiplicityDataException mde) {
            LOG.error("Cannot send multiplicity data", mde);
        }
    }

    public void sendTriplets(int runNumber)
        throws TriggerException
    {
        if (alertQueue == null) {
            throw new TriggerException("AlertQueue has not been set");
        } else if (alertQueue.isStopped()) {
            throw new TriggerException("AlertQueue is stopped");
        }

        if (runNumber == Integer.MIN_VALUE) {
            runNumber = this.runNumber;
        }
        if (runNumber < 0) {
            throw new TriggerException("Cannot send triplets alert;" +
                                       " bad run number " + runNumber);
        }

        String prefix;
        if (srcId >= SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID &&
            srcId < SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID + 1000)
        {
            prefix = "GLOBAL_";
        } else if (srcId >= SourceIdRegistry.ICETOP_TRIGGER_SOURCE_ID &&
            srcId < SourceIdRegistry.ICETOP_TRIGGER_SOURCE_ID + 1000)
        {
            prefix = "ICETOP_";
        } else if (srcId >= SourceIdRegistry.INICE_TRIGGER_SOURCE_ID &&
            srcId < SourceIdRegistry.INICE_TRIGGER_SOURCE_ID + 1000)
        {
            prefix = "INICE_";
        } else {
            LOG.error("Unknown trigger source ID " + srcId);
            prefix = "UNKNOWN_";
        }

        ArrayList<Object[]> triplets = new ArrayList<Object[]>();
        for (ITriggerAlgorithm a : algorithms) {
            if (a.getTriggerConfigId() < 0) {
                // Live doesn't care about Throughput trigger
                continue;
            }

            Object[] data = new Object[4];
            data[0] = Integer.valueOf(a.getTriggerConfigId());
            data[1] = Integer.valueOf(a.getTriggerType());
            data[2] = Integer.valueOf(a.getSourceId());
            data[3] = prefix  + a.getMonitoringName();
            triplets.add(data);
        }

        HashMap<String, Object> values = new HashMap<String, Object>();
        values.put("runNumber", runNumber);
        values.put("triplets", triplets);

        try {
            alertQueue.push("trigger_triplets", Alerter.Priority.EMAIL,
                            values);
        } catch (AlertException ae) {
            throw new TriggerException("Cannot send alert", ae);
        }
    }

    /**
     * Set the object which sends I3Live alerts
     *
     * @param alertQueue alert queue
     */
    public void setAlertQueue(AlertQueue alertQueue)
    {
        if (alertQueue == null) {
            throw new Error("AlertQueue cannot be null");
        }

        this.alertQueue = alertQueue;
        if (alertQueue.isStopped()) {
            alertQueue.start();
        }

        multiDataMgr.setAlertQueue(alertQueue);
    }

    /**
     * Set the DOM registry.
     *
     * @param domRegistry DOM registry
     */
    @Override
    public void setDOMRegistry(IDOMRegistry domRegistry)
    {
        this.domRegistry = domRegistry;
        DomSetFactory.setDomRegistry(domRegistry);
    }

    /**
     * Notify the thread that there is a new earliest time
     *
     * @param payload earliest payload
     */
    @Override
    public void setEarliestPayloadOfInterest(IPayload payload)
    {
    }

    /**
     * Set the first "good" time for the current run.
     *
     * @param firstTime first "good" time
     */
    public void setFirstGoodTime(long firstTime)
    {
        multiDataMgr.setFirstGoodTime(firstTime);
    }

    /**
     * Set the last "good" time for the current run.
     *
     * @param lastTime last "good" time
     */
    public void setLastGoodTime(long lastTime)
    {
        multiDataMgr.setLastGoodTime(lastTime);
    }

    /**
     * Set the output engine.
     *
     * @param outputEngine output engine
     */
    @Override
    public void setOutputEngine(DAQComponentOutputProcess outputEngine)
    {
        this.outputEngine = outputEngine;
    }

    /**
     * Set the run number for conventional runs.
     *
     * @param runNum run number
     */
    public void setRunNumber(int runNum)
    {
        this.runNumber = runNum;

        multiDataMgr.start(runNum);

        if (collector != null) {
            collector.setRunNumber(runNum, false);
        }
    }

    /**
     * setter for splicer
     * @param splicer splicer associated with this object
     */
    @Override
    public void setSplicer(Splicer splicer)
    {
        this.splicer = splicer;
        this.splicer.addSplicerListener(this);
    }

    /**
     * Do nothing.
     *
     * @param evt ignored
     */
    @Override
    public void started(SplicerChangedEvent<Spliceable> evt)
    {
        // do nothing
    }

    /**
     * Create trigger collector and start all algorithm threads.
     *
     * @param evt ignored
     */
    @Override
    public void starting(SplicerChangedEvent<Spliceable> evt)
    {
        if (collector != null && !collector.isStopped()) {
            LOG.error("Collector was not stopped");
        }

        collector = new TriggerCollector(srcId, algorithms, outputEngine,
                                         outCache, multiDataMgr, this);

        if (runNumber != Integer.MIN_VALUE) {
            collector.setRunNumber(runNumber, false);
        }

        collector.startThreads(splicer);
    }

    /**
     * Stop the threads
     */
    @Override
    public void stopThread()
    {
        if (collector != null && !collector.isStopped()) {
            collector.stop();
        }
    }

    /**
     * Stop all associated threads.
     *
     * @param evt ignored
     */
    @Override
    public void stopped(SplicerChangedEvent<Spliceable> evt)
    {
        flush();
        stopThread();

        // clear cached values
        timeOfLastHit = null;
        srcOfLastHit = null;
    }

    /**
     * Do nothing
     *
     * @param evt ignored
     */
    @Override
    public void stopping(SplicerChangedEvent<Spliceable> evt)
    {
        // do nothing
    }

    @Override
    public void subscribeAll()
    {
        for (ITriggerAlgorithm algo : algorithms) {
            PayloadSubscriber subscriber =
                queueList.subscribe(algo.getTriggerName());
            algo.setSubscriber(subscriber);
        }
    }

    /**
     * Switch to a new run.
     *
     * @param runNumber new run number
     */
    @Override
    public void switchToNewRun(int runNumber)
    {
        if (srcId == SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID) {
            try {
                multiDataMgr.setNextRunNumber(runNumber);
            } catch (MultiplicityDataException mde) {
                LOG.error("Cannot set next run number", mde);
            }
            resetUIDs();
        } else {
            multiDataMgr.start(runNumber);
        }

        if (collector == null) {
            LOG.error("Collector has not been created before run switch");
        } else {
            collector.setRunNumber(runNumber, true);
        }

        // update private copy of run number
        this.runNumber = runNumber;
    }

    @Override
    public void unsubscribeAll()
    {
        if (!queueList.isEmpty()) {
            for (ITriggerAlgorithm a : algorithms) {
                a.unsubscribe(queueList);
                a.resetAlgorithm();
            }

            if (!queueList.isEmpty()) {
                LOG.error(String.format("SubscribedList still has %d entries",
                                        queueList.size()));
            }
        }
    }

    @Override
    public String toString()
    {
        int numQueued = 0;
        for (ITriggerAlgorithm a : algorithms) {
            numQueued += a.getNumberOfCachedRequests();
        }

        return "TrigMgr[in#" + queueList.size() + ",req#" + numQueued +
            "," + collector + "]";
    }
}
