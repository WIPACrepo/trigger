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
import icecube.daq.splicer.SpliceableFactory;
import icecube.daq.splicer.SplicedAnalysis;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerChangedEvent;
import icecube.daq.splicer.SplicerListener;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.exceptions.MultiplicityDataException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnimplementedError;
import icecube.daq.util.DOMRegistry;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    public boolean equals(Object obj)
    {
        throw new UnimplementedError();
    }

    /**
     * Unimplemented method suggested by the interface.
     *
     * @return UnimplementedError
     */
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
    private static final Log LOG = LogFactory.getLog(TriggerManager.class);

    /** Source ID for this trigger component */
    private int srcId;

    private TriggerRequestFactory trFactory;
    private DAQComponentOutputProcess outputEngine;
    private List<ITriggerAlgorithm> algorithms =
        new ArrayList<ITriggerAlgorithm>();

    private SubscribedList inputList = new SubscribedList();

    /** gather histograms for monitoring */
    private MultiplicityDataManager multiDataMgr;
    private AlertQueue alertQueue;

    private IByteBufferCache outCache;
    private TriggerCollector collector;

    /** splicer associated with this manager */
    private Splicer splicer;

    private DOMRegistry domRegistry;

    /** spliceable input count */
    private long inputCount;

    private long recycleCount;

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
     * @param val algorithm being added
     */
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
     * XXX Unimplemented.
     *
     * @param evt ignored
     */
    public void disposed(SplicerChangedEvent<Spliceable> evt)
    {
        throw new UnimplementedError();
    }

    /**
     * Add the new list of hits from the splicer to the input queue.
     *
     * @param splicedObjects list of hits
     */
    public void analyze(List<Spliceable> splicedObjects)
    {
        // Loop over the new objects in the splicer
        int numberOfObjectsInSplicer = splicedObjects.size();

        for (Spliceable spl : splicedObjects) {
            ILoadablePayload payload = (ILoadablePayload) spl;

            if (LOG.isDebugEnabled()) {
                LOG.debug("  Processing payload " + inputCount +
                          " with time " + payload.getPayloadTimeUTC());
            }

            if (isValidIncomingPayload(payload)) {
                synchronized (inputList) {
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
    public void failed(SplicerChangedEvent<Spliceable> evt)
    {
        throw new UnimplementedError();
    }

    /**
     * flush the handler
     * including the input buffer, all triggers, and the output bag
     */
    public void flush()
    {
        inputList.push(FLUSH_PAYLOAD);
    }

    /**
     * Get the alert queue for messages sent to I3Live
     *
     * @return alert queue
     */
    public AlertQueue getAlertQueue()
    {
        return alertQueue;
    }

    /**
     * @deprecated
     *
     * @return total number of hits read from splicer
     */
    public long getCount()
    {
        return getTotalProcessed();
    }

    /**
     * Get the DOM registry.
     *
     * @return DOM registry
     */
    public DOMRegistry getDOMRegistry()
    {
        return domRegistry;
    }

    /**
     * XXX Use getQueuedInputsMap instead
     *
     * @return number of queued inputs
     * @deprecated use getQueuedInputsMap()
     */
    public int getNumInputsQueued()
    {
        Map<String, Integer> map = getQueuedInputsMap();

        int num = 0;
        if (map != null) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                num += entry.getValue();
            }
        }

        return num;
    }

    /**
     * XXX Use getQueuedRequestsMap instead
     *
     * @return number of queued requests
     * @deprecated use getQueuedRequestsMap()
     */
    public int getNumRequestsQueued()
    {
        Map<String, Integer> map = getQueuedRequestsMap();

        int num = 0;
        if (map != null) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                num += entry.getValue();
            }
        }

        return num;
    }

    /**
     * Get the number of requests queued for writing
     *
     * @return size of output queue
     */
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
    public Map<String, Integer> getQueuedInputsMap()
    {
        return inputList.getLengths();
    }

    /**
     * Get map of trigger names to number of queued requests
     *
     * @return map of {name : numQueuedRequests}
     */
    public Map<String, Integer> getQueuedRequestsMap()
    {
        HashMap map = new HashMap<String, Integer>();
        for (ITriggerAlgorithm a : algorithms) {
            map.put(a.getTriggerName(), a.getNumberOfCachedRequests());
        }
        return map;
    }

    /**
     * Get the number of dropped SNDAQ alerts
     *
     * @return number of dropped SNDAQ alerts
     */
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
    public int getSourceId()
    {
        return srcId;
    }

    /**
     * Get the total number of hits pushed onto the input queue
     *
     * @return total number of hits received from the splicer
     */
    public long getTotalProcessed()
    {
        return inputCount;
    }

    /**
     * Get map of trigger names to number of issued requests
     *
     * @return map of {name : numRequests}
     */
    public Map<String, Long> getTriggerCounts()
    {
        HashMap<String, Long> map = new HashMap<String, Long>();

        for (ITriggerAlgorithm trigger : algorithms) {
            map.put(trigger.getTriggerName(),
                    Long.valueOf(trigger.getTriggerCounter()));
        }

        return map;
    }

    /**
     * Get any special monitoring quantities for all algorithms.
     *
     * @return map of {name-configID-quantity: quantityObject}
     */
    public Map<String, Object> getTriggerMonitorMap()
    {
        HashMap<String, Object> map = new HashMap<String, Object>();

        for (ITriggerAlgorithm trigger : algorithms) {
            Map<String, Object> moniMap = trigger.getTriggerMonitorMap();
            if (moniMap != null && moniMap.size() > 0) {
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
            double timeDiff;
            if (timeOfLastHit == null) {
                timeDiff = 0.0;
            } else {
                timeDiff = hit.getHitTimeUTC().timeDiff_ns(timeOfLastHit);
            }

            // check to see if timeDiff is reasonable, if not ignore it
            if (timeDiff < 0.0) {
                LOG.error("Hit from " + hit.getSourceID() +
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
            inputList.push((IPayload) payload.deepCopy());
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
                inputList.push((IPayload) payload.deepCopy());
            } else {
                // extract list of merged triggers
                List subList;
                try {
                    subList = req.getPayloads();
                } catch (PayloadFormatException pfe) {
                    LOG.error("Cannot fetch triggers from " + req, pfe);
                    return;
                }

                if (subList == null || subList.size() == 0) {
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

                    inputList.push((IPayload) sub.deepCopy());
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
    public void setDOMRegistry(DOMRegistry domRegistry)
    {
        this.domRegistry = domRegistry;
        DomSetFactory.setDomRegistry(domRegistry);
    }

    /**
     * Notify the thread that there is a new earliest time
     *
     * @param payload earliest payload
     */
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
    public void started(SplicerChangedEvent<Spliceable> evt)
    {
        // do nothing
    }

    /**
     * Create trigger collector and start all algorithm threads.
     *
     * @param evt ignored
     */
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
    public void stopping(SplicerChangedEvent<Spliceable> evt)
    {
        // do nothing
    }

    public void subscribeAll()
    {
        for (ITriggerAlgorithm algo : algorithms) {
            PayloadSubscriber subscriber =
                inputList.subscribe(algo.getTriggerName());
            algo.setSubscriber(subscriber);
        }
    }

    /**
     * Switch to a new run.
     *
     * @param runNumber new run number
     */
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

    public void unsubscribeAll()
    {
        if (inputList.getNumSubscribers() > 0) {
            for (ITriggerAlgorithm a : algorithms) {
                a.unsubscribe(inputList);
                a.resetAlgorithm();
            }

            if (inputList.getNumSubscribers() > 0) {
                LOG.error(String.format("SubscribedList still has %d entries",
                                        inputList.size()));
            }
        }
    }

    public String toString()
    {
        return "TrigMgr[in#" + inputList.size() + ",req#" +
            getNumRequestsQueued() + "," + collector + "]";
    }
}
