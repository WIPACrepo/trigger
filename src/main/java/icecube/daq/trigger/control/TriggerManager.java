package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
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
import icecube.daq.splicer.SplicedAnalysis;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerChangedEvent;
import icecube.daq.splicer.SplicerListener;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.common.ITriggerAlgorithm;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.exceptions.MultiplicityDataException;
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
    implements INewManager, SplicedAnalysis, SplicerListener,
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
    private List<INewAlgorithm> algorithms =
        new ArrayList<INewAlgorithm>();

    private SubscribedList inputList = new SubscribedList();
    private List<TriggerThread> threadList = new ArrayList<TriggerThread>();

    /** gather histograms for monitoring */
    private MultiplicityDataManager multiDataMgr;

    private IByteBufferCache outCache;
    private TriggerCollector collector;

    /** splicer associated with this manager */
    private Splicer splicer;

    private DOMRegistry domRegistry;

    /**
     * the begining position of each new spliced buffer, modulo the decrement
     * due to shifting
     */
    private int start;

    /** spliceable input count */
    private long inputCount;

    private long recycleCount;

    /** size of last input list */
    private int lastInputListSize;

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
    public void addTrigger(ITriggerAlgorithm val)
    {
        if (!(val instanceof INewAlgorithm)) {
            throw new Error("Algorithm " + val +
                            " must implement INewAlgorithm");
        }

        INewAlgorithm trig = (INewAlgorithm) val;

        boolean good = true;
        for (INewAlgorithm t : algorithms) {
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
            algorithms.add((INewAlgorithm) trig);
            trig.setTriggerManager(this);
            trig.setTriggerFactory(trFactory);

            if (collector != null && !collector.isStopped()) {
                collector.stop();
            }
        }
    }

    /**
     * XXX Unimplemented.
     *
     * @param payload ignored
     */
    public void addTriggerRequest(ITriggerRequestPayload payload)
    {
        throw new UnimplementedError();
    }

    /**
     * Add a list of algorithms.
     *
     * @param list list of trigger algorithms to add
     */
    public void addTriggers(List<ITriggerAlgorithm> list)
    {
        for (ITriggerAlgorithm trig: list) {
            addTrigger(trig);
        }
    }

    /**
     * XXX Unimplemented.
     *
     * @param evt ignored
     */
    public void disposed(SplicerChangedEvent evt)
    {
        throw new UnimplementedError();
    }

    /**
     * Add the new list of hits from the splicer to the input queue.
     *
     * @param splicedObjects list of hits
     * @param decrement starting index
     */
    public void execute(List splicedObjects, int decrement)
    {
        // Loop over the new objects in the splicer
        int numberOfObjectsInSplicer = splicedObjects.size();
        lastInputListSize = numberOfObjectsInSplicer - (start - decrement);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Splicer contains: [" + lastInputListSize + ":" +
                      numberOfObjectsInSplicer + "]");
        }

        if (lastInputListSize > 0) {
            for (int index = start - decrement;
                 numberOfObjectsInSplicer != index; index++)
            {
                ILoadablePayload payload =
                    (ILoadablePayload) splicedObjects.get(index);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("  Processing payload " + inputCount +
                              " with time " + payload.getPayloadTimeUTC());
                }

                if (isValidIncomingPayload(payload)) {
                    synchronized (inputList) {
                        pushInput(payload);
                    }

                    inputCount++;
                }
            }
        }

        start = numberOfObjectsInSplicer;
    }

    /**
     * XXX Unimplemented.
     *
     * @param evt ignored
     */
    public void failed(SplicerChangedEvent evt)
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
     * @deprecated
     *
     * @return total number of hits read from splicer
     */
    public long getCount()
    {
        if (collector == null) {
            return 0;
        }

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
     * Get the most recent set of trigger counts to be used for
     * detector monitoring.
     *
     * @return list of trigger count data.
     */
    public List<Map<String, Object>> getMoniCounts()
    {
        List<Map<String, Object>> mapList;
        try {
            mapList = multiDataMgr.getCounts();
        } catch (MultiplicityDataException mde) {
            LOG.error("Cannot get trigger counts for monitoring", mde);
            mapList = null;
        }

        if (mapList == null) {
            return new ArrayList<Map<String, Object>>();
        }

        return mapList;
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
     * Get this component's source ID.
     *
     * @return source ID
     */
    public int getSourceId()
    {
        return srcId;
    }

    /**
     * XXX Unimplemented
     *
     * @return UnimplementedError
     */
    public TreeMap<Integer, TreeSet<Integer>> getStringMap()
    {
        throw new UnimplementedError();
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

        for (INewAlgorithm trigger : algorithms) {
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

        for (INewAlgorithm trigger : algorithms) {
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
        start = 0;
        inputCount = 0;
        lastInputListSize = 0;
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
        if (payload.getPayloadInterfaceType() ==
            PayloadInterfaceRegistry.I_TRIGGER_REQUEST)
        {
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
            if (req.isMerged()) {
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

                    inputList.push(sub);
                }

                return;
            }
        }

        // queue ordinary payload
        inputList.push(payload);
    }

    public void resetUIDs()
    {
        for (INewAlgorithm a : algorithms) {
            a.resetUID();
        }
        collector.resetUID();
    }

    /**
     * Send per-run histograms
     */
    public void sendHistograms()
    {
        try {
            multiDataMgr.send();
        } catch (MultiplicityDataException mde) {
            LOG.error("Cannot send multiplicity data", mde);
        }
    }

    /**
     * Set the object which sends I3Live alerts
     *
     * @param alerter alerter object
     */
    public void setAlerter(Alerter alerter)
    {
        multiDataMgr.setAlerter(alerter);
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
    public void started(SplicerChangedEvent evt)
    {
        // do nothing
    }

    /**
     * Create trigger collector and start all algorithm threads.
     *
     * @param evt ignored
     */
    public void starting(SplicerChangedEvent evt)
    {
        if (collector == null || collector.isStopped()) {
            collector = new TriggerCollector(srcId, algorithms, outputEngine,
                                             outCache, multiDataMgr);

            if (runNumber != Integer.MIN_VALUE) {
                collector.setRunNumber(runNumber, false);
            }

            collector.startThreads(splicer);
        }

        subscribeAlgorithms();

        int id = 0;
        for (INewAlgorithm algo : algorithms) {
            TriggerThread thread = new TriggerThread(id, algo);
            thread.start();

            threadList.add(thread);

            id++;
        }
    }

    /**
     * Stop the threads
     */
    public void stopThread()
    {
        for (TriggerThread thread : threadList) {
            thread.stop();
        }
        for (TriggerThread thread : threadList) {
            thread.join();
        }

        if (collector != null && !collector.isStopped()) {
            collector.stop();
        }
    }

    /**
     * Stop all associated threads.
     *
     * @param evt ignored
     */
    public void stopped(SplicerChangedEvent evt)
    {
        stopThread();
    }

    /**
     * Do nothing
     *
     * @param evt ignored
     */
    public void stopping(SplicerChangedEvent evt)
    {
        // do nothing
    }

    void subscribeAlgorithms()
    {
        for (INewAlgorithm algo : algorithms) {
            PayloadSubscriber subscriber =
                inputList.subscribe(algo.getTriggerName());
            algo.setSubscriber(subscriber);
        }
    }

    /**
     * Switch to a new run.
     *
     * @param alerter unused
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
    }

    /**
     * Called when the {@link icecube.daq.splicer.Splicer Splicer} has
     * truncated its "rope". This method is called whenever the "rope" is cut,
     * for example to make a clean start from the frayed beginning of a "rope",
     * and not just when the {@link Splicer#truncate(Spliceable)} method is
     * invoked. This enables the client to be notified as to which Spliceables
     * are never going to be accessed again by the Splicer.
     *
     * @param event the event encapsulating this truncation.
     */
    public void truncated(SplicerChangedEvent event)
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Splicer truncated: " + event.getSpliceable());
        }

        if (event.getSpliceable() == Splicer.LAST_POSSIBLE_SPLICEABLE) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("This is the LAST POSSIBLE SPLICEABLE!");
            }
            flush();
            stopThread();
        }

        for (Object obj : event.getAllSpliceables()) {
            ILoadablePayload payload = (ILoadablePayload) obj;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Recycle payload " + recycleCount + " at " +
                          payload.getPayloadTimeUTC());
            }

            payload.recycle();
            recycleCount++;
        }
    }

    public String toString()
    {
        return "TrigMgr[in#" + inputList.size() + "," + collector + "]";
    }
}
