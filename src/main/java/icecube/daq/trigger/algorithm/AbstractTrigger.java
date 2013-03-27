package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IDOMID;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.ReadoutRequest;
import icecube.daq.payload.impl.ReadoutRequestElement;
import icecube.daq.payload.impl.TriggerRequest;
import icecube.daq.payload.impl.TriggerRequestFactory;
import icecube.daq.trigger.common.ITriggerManager;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.config.TriggerReadout;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.control.HitFilter;
import icecube.daq.trigger.control.INewManager;
import icecube.daq.trigger.control.Interval;
import icecube.daq.trigger.control.PayloadSubscriber;
import icecube.daq.trigger.control.TriggerCollector;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnimplementedError;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for trigger algorithms.
 */
public abstract class AbstractTrigger
    implements AbstractTriggerMBean, INewAlgorithm
{
    /** Log object for this class */
    private static final Log LOG = LogFactory.getLog(AbstractTrigger.class);

    /** SPE hit type */
    public static final int SPE_HIT = 0x02;

    protected int triggerPrescale;
    protected int domSetId = -1;
    protected HitFilter hitFilter = new HitFilter();

    protected String triggerName;
    private int trigCfgId;
    private int trigType;
    private int srcId;
    private ArrayList<TriggerReadout> readouts =
        new ArrayList<TriggerReadout>();
    private ArrayList<TriggerParameter> params =
        new ArrayList<TriggerParameter>();

    private ITriggerManager mgr;
    protected TriggerRequestFactory triggerFactory;
    protected boolean onTrigger;
    protected int triggerCounter;
    protected int sentTriggerCounter;
    private int printMod = 1000;

    private IPayload earliestPayloadOfInterest;
    private boolean sawFlush;

    private IPayload releaseTime;

    private ArrayList<ITriggerRequestPayload> requests =
        new ArrayList<ITriggerRequestPayload>();
    private TriggerCollector collector;

    private PayloadSubscriber subscriber;

    private long earliestMonitorTime = Long.MIN_VALUE;

    /**
     * Add a trigger parameter.
     *
     * @param name parameter name
     * @param value parameter value
     *
     * @throws UnknownParameterException if the parameter is unknown
     * @throws IllegalParameterValueException if the parameter value is bad
     */
    public void addParameter(String name, String value)
        throws UnknownParameterException, IllegalParameterValueException
    {
        params.add(new TriggerParameter(name, value));
    }

    /**
     * Add a readout entry to the cached list.
     *
     * @param rdoutType readout type
     * @param offset offset value
     * @param minus minus
     * @param plus plus
     */
    public void addReadout(int rdoutType, int offset, int minus, int plus)
    {
        readouts.add(new TriggerReadout(rdoutType, offset, minus, plus));
    }

    protected void configHitFilter(int domSetId)
        throws ConfigException
    {
        hitFilter = new HitFilter(domSetId);
    }

    /**
     * Form a ReadoutRequestElement based on the trigger and the readout
     * configuration.
     * @param firstTime earliest time of trigger
     * @param roCfg ReadoutConfiguration object
     * @param domId domId, null if readout type is not MODULE
     * @param stringId stringId, null if readout type is not MODULE or STRING
     * @return IReadoutRequestElement
     *
     */
    protected IReadoutRequestElement createReadoutElement(IUTCTime firstTime,
                                                          IUTCTime lastTime,
                                                          TriggerReadout roCfg,
                                                          IDOMID domId,
                                                          ISourceID stringId)
    {
        IUTCTime timeOffset;
        IUTCTime timeMinus;
        IUTCTime timePlus;

        int type = roCfg.getType();
        switch (type) {
        case IReadoutRequestElement.READOUT_TYPE_GLOBAL:
            if (null != stringId) {
                stringId = null;
            }
            if (null != domId) {
                domId = null;
            }
            timeMinus = firstTime.getOffsetUTCTime(-roCfg.getMinus());
            timePlus = lastTime.getOffsetUTCTime(roCfg.getPlus());
            break;
        case IReadoutRequestElement.READOUT_TYPE_II_GLOBAL:
            if (null != stringId) {
                stringId = null;
            }
            if (null != domId) {
                domId = null;
            }
            if (srcId == SourceIdRegistry.ICETOP_TRIGGER_SOURCE_ID) {
                timeOffset =
                    firstTime.getOffsetUTCTime(roCfg.getOffset());
                timeMinus =
                    timeOffset.getOffsetUTCTime(-roCfg.getMinus());
                timePlus =
                    timeOffset.getOffsetUTCTime(roCfg.getPlus());
            } else {
                timeMinus =
                    firstTime.getOffsetUTCTime(-roCfg.getMinus());
                timePlus =
                    lastTime.getOffsetUTCTime(roCfg.getPlus());
            }
            break;
        case IReadoutRequestElement.READOUT_TYPE_II_STRING:
            // need stringId
            if (null == stringId) {
                LOG.error("ReadoutType = " + type +
                          " but StringId is NULL!");
            }
            if (null != domId) {
                domId = null;
            }
            if (srcId == SourceIdRegistry.ICETOP_TRIGGER_SOURCE_ID) {
                timeOffset = firstTime.getOffsetUTCTime(roCfg.getOffset());
                timeMinus = timeOffset.getOffsetUTCTime(-roCfg.getMinus());
                timePlus = timeOffset.getOffsetUTCTime(roCfg.getPlus());
            } else {
                timeMinus = firstTime.getOffsetUTCTime(-roCfg.getMinus());
                timePlus = lastTime.getOffsetUTCTime(roCfg.getPlus());
            }
            break;
        case IReadoutRequestElement.READOUT_TYPE_II_MODULE:
            // need stringId and domId
            if (null == stringId) {
                LOG.error("ReadoutType = " + type +
                          " but StringId is NULL!");
            }
            if (null == domId) {
                LOG.error("ReadoutType = " + type + " but DomId is NULL!");
            }
            if (srcId == SourceIdRegistry.ICETOP_TRIGGER_SOURCE_ID) {
                timeOffset = firstTime.getOffsetUTCTime(roCfg.getOffset());
                timeMinus = timeOffset.getOffsetUTCTime(-roCfg.getMinus());
                timePlus = timeOffset.getOffsetUTCTime(roCfg.getPlus());
            } else {
                timeMinus = firstTime.getOffsetUTCTime(-roCfg.getMinus());
                timePlus = lastTime.getOffsetUTCTime(roCfg.getPlus());
            }
            break;
        case IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL:
            if (null != stringId) {
                stringId = null;
            }
            if (null != domId) {
                domId = null;
            }
            if (srcId == SourceIdRegistry.INICE_TRIGGER_SOURCE_ID) {
                timeOffset = firstTime.getOffsetUTCTime(roCfg.getOffset());
                timeMinus = timeOffset.getOffsetUTCTime(-roCfg.getMinus());
                timePlus = timeOffset.getOffsetUTCTime(roCfg.getPlus());
            } else {
                timeMinus = firstTime.getOffsetUTCTime(-roCfg.getMinus());
                timePlus = lastTime.getOffsetUTCTime(roCfg.getPlus());
            }
            break;
        case IReadoutRequestElement.READOUT_TYPE_IT_MODULE:
            // need stringId and domId
            if (null == stringId) {
                LOG.error("ReadoutType = " + type +
                          " but StringId is NULL!");
            }
            if (null == domId) {
                LOG.error("ReadoutType = " + type + " but DomId is NULL!");
            }
            if (srcId == SourceIdRegistry.INICE_TRIGGER_SOURCE_ID) {
                timeOffset = firstTime.getOffsetUTCTime(roCfg.getOffset());
                timeMinus = timeOffset.getOffsetUTCTime(-roCfg.getMinus());
                timePlus = timeOffset.getOffsetUTCTime(roCfg.getPlus());
            } else {
                timeMinus = firstTime.getOffsetUTCTime(-roCfg.getMinus());
                timePlus = lastTime.getOffsetUTCTime(roCfg.getPlus());
            }
            break;
        default:
            LOG.error("Unknown ReadoutType: " + type +
                      " -> Making it GLOBAL");
            type = IReadoutRequestElement.READOUT_TYPE_GLOBAL;
            timeMinus = firstTime.getOffsetUTCTime(-roCfg.getMinus());
            timePlus = lastTime.getOffsetUTCTime(roCfg.getPlus());
            break;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating readout: Type = " + type +
                      " FirstTime = " + timeMinus.longValue() / 10.0 +
                      " LastTime = " + timePlus.longValue() / 10.0);
        }

        int rreSrcId;
        if (stringId == null) {
            rreSrcId = -1;
        } else {
            rreSrcId = stringId.getSourceID();
        }

        long rreDomId;
        if (domId == null) {
            rreDomId = -1;
        } else {
            rreDomId = domId.longValue();
        }

        return new ReadoutRequestElement(type, rreSrcId,
                                         timeMinus.longValue(),
                                         timePlus.longValue(),
                                         rreDomId);
    }

    /**
     * Flush the algorithm.
     */
    public abstract void flush();

    protected void formTrigger(IUTCTime time)
    {
        if (null == triggerFactory) {
            throw new Error("TriggerFactory is not set!");
        }

        // create readout requests
        ArrayList<IReadoutRequestElement> readoutElements =
            new ArrayList<IReadoutRequestElement>();
        Iterator readoutIter = readouts.iterator();
        while (readoutIter.hasNext()) {
            TriggerReadout readout = (TriggerReadout) readoutIter.next();
            readoutElements.add(createReadoutElement(time, time, readout, null,
                                                     null));
        }

        final int uid = getNextUID();

        IReadoutRequest readoutRequest =
            new ReadoutRequest(time.longValue(), uid, srcId, readoutElements);

        // make payload
        ArrayList<IWriteablePayload> hitList =
            new ArrayList<IWriteablePayload>();
        TriggerRequest triggerPayload =
            (TriggerRequest) triggerFactory.createPayload(uid, trigType,
                                                          trigCfgId, srcId,
                                                          time.longValue(),
                                                          time.longValue(),
                                                          readoutRequest,
                                                          hitList);

        // report it
        reportTrigger(triggerPayload);
    }

    protected void formTrigger(IUTCTime firstTime, IUTCTime lastTime)
    {
        throw new UnimplementedError();
    }

    protected void formTrigger(IHitPayload hit, IDOMID dom, ISourceID string)
    {
        List hitList = new ArrayList(1);
        hitList.add(hit);
        formTrigger(hitList, dom, string);
    }

    protected void formTrigger(List hits, IDOMID dom, ISourceID string)
    {
        if (null == triggerFactory) {
            throw new Error("TriggerFactory is not set!");
        }

        final int numberOfHits = hits.size();
        if (numberOfHits == 0) {
            throw new Error("Cannot form trigger from empty list of hits");
        }

        // get times (this assumes that the hits are time-ordered)
        IUTCTime firstTime =
            ((IHitPayload) hits.get(0)).getPayloadTimeUTC();
        IUTCTime lastTime =
            ((IHitPayload) hits.get(numberOfHits - 1)).getPayloadTimeUTC();

        if (LOG.isDebugEnabled() && (triggerCounter % printMod == 0)) {
            LOG.debug("New Trigger " + triggerCounter + " from " +
                      triggerName + " includes " + numberOfHits +
                      " hits:  First time = " + firstTime + " Last time = " +
                      lastTime);
        }

        // set earliest payload of interest to 1/10 ns after the last hit
        IUTCTime lastHitTime =
            ((IHitPayload) hits.get(numberOfHits - 1)).getHitTimeUTC();

        final int uid = getNextUID();

        // create readout requests
        ArrayList<IReadoutRequestElement> readoutElements =
            new ArrayList<IReadoutRequestElement>();
        Iterator readoutIter = readouts.iterator();
        while (readoutIter.hasNext()) {
            TriggerReadout readout = (TriggerReadout) readoutIter.next();
            readoutElements.add(createReadoutElement(firstTime, lastTime,
                                                     readout, dom, string));
        }
        IReadoutRequest readoutRequest =
            new ReadoutRequest(firstTime.longValue(), uid, srcId,
                               readoutElements);

        // copy hits so they can be recycled
        ArrayList<IWriteablePayload> hitList =
            new ArrayList<IWriteablePayload>();
        for (Object obj : hits) {
            IWriteablePayload hit = (IWriteablePayload) obj;
            hitList.add((IWriteablePayload) hit.deepCopy());
        }

        // make payload
        TriggerRequest triggerPayload =
            (TriggerRequest) triggerFactory.createPayload(uid, trigType,
                                                          trigCfgId, srcId,
                                                          firstTime.longValue(),
                                                          lastTime.longValue(),
                                                          readoutRequest,
                                                          hitList);

        // report it
        reportTrigger(triggerPayload);

        // update earliest hit time
        IPayload dummy = new DummyPayload(lastHitTime.getOffsetUTCTime(0.1));
        setEarliestPayloadOfInterest(dummy);
    }

    /**
     * Get the earliest payload of interest for this algorithm.
     *
     * @return earliest payload
     */
    public IPayload getEarliestPayloadOfInterest()
    {
        return earliestPayloadOfInterest;
    }

    /**
     * Get the earliest event time of interest for this algorithm.
     *
     * @return earliest UTC time
     */
    public long getEarliestTime()
    {
        if (earliestPayloadOfInterest == null) {
            return 0;
        }

        final long val = earliestPayloadOfInterest.getUTCTime();
        if (earliestMonitorTime == Long.MIN_VALUE) {
            earliestMonitorTime = val;
        }

        if (val == Long.MAX_VALUE) {
            return earliestMonitorTime;
        }

        return val;
    }

    /**
     * Get this hit's type.
     *
     * @param hit hit to evaluate
     *
     * @return hit type
     */
    public static int getHitType(IHitPayload hit)
    {
        return hit.getTriggerType() & 0xf;
    }

    /**
     * Get the input queue size.
     *
     * @return input queue size
     */
    public int getInputQueueSize()
    {
        return subscriber.size();
    }

    /**
     * Get the next interval.
     *
     * @param interval interval being considered
     *
     * @return next interval
     */
    public Interval getInterval(Interval interval)
    {
        if (interval.isEmpty()) {
            Interval rtnInterval;
            synchronized (requests) {
                if (requests.size() == 0) {
                    rtnInterval = interval;
                } else {
                    ITriggerRequestPayload req = requests.get(0);

                    rtnInterval =
                        new Interval(req.getFirstTimeUTC().longValue(),
                                     req.getLastTimeUTC().longValue());
                }
            }

            return rtnInterval;
        }

        long start = interval.start;
        long end = interval.end;

        synchronized (requests) {
            for (ITriggerRequestPayload req : requests) {
                long firstTime = req.getFirstTimeUTC().longValue();
                long lastTime = req.getLastTimeUTC().longValue();

                // if this request precedes the interval, start a new interval
                if (start > lastTime) {
                    start = firstTime;
                    end = lastTime;
                    break;
                }

                // if this request is past the end of the interval, we're done
                if (end < firstTime) {
                    break;
                }

                // if necessary, widen the interval
                if (firstTime < start) {
                    start = firstTime;
                }
                if (lastTime > end) {
                    end = lastTime;
                }
            }
        }

        // if this trigger is still interested in hits within the interval,
        //  return null to signal that the current interval is invalid
        if (earliestPayloadOfInterest == null ||
            (earliestPayloadOfInterest.getUTCTime() !=
             FlushRequest.FLUSH_TIME &&
             (start >= earliestPayloadOfInterest.getUTCTime() ||
              end >= earliestPayloadOfInterest.getUTCTime())))
        {
            return null;
        }

        if (start == interval.start && end == interval.end) {
            return interval;
        }

        return new Interval(start, end);
    }

    /**
     * Get the next request UID.
     * NOTE: this increments the trigger-wide UID counter
     *
     * @return next uid
     */
    public int getNextUID()
    {
        return triggerCounter++;
    }

    /**
     * Get number of cached requests.
     *
     * @return number of cached requests
     */
    public int getNumberOfCachedRequests()
    {
        return requests.size();
    }

    /**
     * Get the time of the last released trigger.
     *
     * @return release time
     */
    public IPayload getReleaseTime()
    {
        return releaseTime;
    }

    /**
     * Get the source ID.
     *
     * @return source ID
     */
    public int getSourceId()
    {
        return srcId;
    }

    /**
     * Get the configuration ID.
     *
     * @return configuration ID
     */
    public int getTriggerConfigId()
    {
        return trigCfgId;
    }

    /**
     * Get the ID of the most recent trigger request.
     *
     * @return counter value
     */
    public int getTriggerCounter()
    {
        return triggerCounter;
    }

    /**
     * Get the trigger handler.
     *
     * @return trigger handler
     * @deprecated use getTriggerManager()
     */
    public INewManager getTriggerHandler()
    {
        return (INewManager) mgr;
    }

    /**
     * Get the trigger manager.
     *
     * @return trigger manager
     */
    public ITriggerManager getTriggerManager()
    {
        return mgr;
    }

    /**
     * Get the map of monitored quantitied for this algorithm.
     *
     * @return map of monitored quantity names and values
     */
    public Map<String, Object> getTriggerMonitorMap()
    {
        return null;
    }

    /**
     * Get trigger name.
     * @return triggerName
     */
    public String getTriggerName()
    {
        return triggerName;
    }

    /**
     * Get the trigger type.
     *
     * @return trigger type
     */
    public int getTriggerType()
    {
        return trigType;
    }

    /**
     * Has this algorithm been fully configured?
     *
     * @return <tt>true</tt> if the algorithm has been fully configured
     */
    public abstract boolean isConfigured();

    /**
     * Recycle all unused requests still cached in the algorithms.
     */
    public void recycleUnusedRequests()
    {
        int count = 0;
        for (ITriggerRequestPayload req : requests) {
            req.recycle();
            if (!(req instanceof FlushRequest)) {
                count++;
            }
        }
        if (count > 0) {
            LOG.error("Recycled " + count + " unused " + toString() +
                      " requests");
        }
    }

    /**
     * Add all requests in the interval to the list of released requests.
     *
     * @param interval time interval to check
     * @param released list of released requests
     */
    public void release(Interval interval,
                        List<ITriggerRequestPayload> released)
    {
        synchronized (requests) {
            int i = 0;
            for (ITriggerRequestPayload req : requests) {
                if (interval.start > req.getFirstTimeUTC().longValue() ||
                    interval.start > req.getLastTimeUTC().longValue())
                {
                    // yikes, found a request preceding the interval!
                    LOG.error("Found request " + req +
                              " before start of interval " + interval +
                              " (startDiff " +
                              (interval.start -
                               req.getFirstTimeUTC().longValue()) +
                              ", endDiff " +
                              (interval.start -
                               req.getLastTimeUTC().longValue()) + ")");
                    break;
                }

                if (interval.end < req.getLastTimeUTC().longValue()) {
                    // if request is past the end of the interval, we're done
                    break;
                }

                i++;
            }

            // release all requests found in the interval
            if (i > 0) {
                List<ITriggerRequestPayload> sub =
                    requests.subList(0, i);

                // save the last released time
                ITriggerRequestPayload req = sub.get(i-1);
                if (req instanceof FlushRequest) {
                    releaseTime = req;
                } else {
                    releaseTime = new DummyPayload(req.getLastTimeUTC());
                }

                // add released requests to the list and remove from the cache
                released.addAll(sub);
                sub.clear();
            }
        }
    }

    public void reportHit(IHitPayload hit)
    {
        throw new UnimplementedError();
    }

    /**
     * Report the trigger request.
     *
     * @param trigReq new request
     */
    public void reportTrigger(ITriggerRequestPayload trigReq)
    {
        // if prescaling, should we throw out this trigger request?
        if ((triggerPrescale != 0) &&
            ((triggerCounter % triggerPrescale) != 0))
        {
            trigReq.recycle();
        } else {
            synchronized (requests) {
                requests.add(trigReq);
                sentTriggerCounter++;
            }

            collector.setChanged();
        }
    }

    /**
     * Reset the UID to signal a run switch.
     */
    public void resetUID()
    {
        // do nothing
    }

    /**
     * Run trigger algorithm on the payload.
     *
     * @param payload payload to process
     *
     * @throws TriggerException if there was a problem running the algorithm
     */
    public abstract void runTrigger(IPayload payload)
        throws TriggerException;

    /**
     * Have we seen a flush request?
     *
     * @return <tt>true</tt> if all hits have been seen
     */
    public boolean sawFlush()
    {
        return sawFlush;
    }

    /**
     * Clear out all remaining payloads.
     */
    public void sendLast()
    {
        flush();

        FlushRequest flushReq = new FlushRequest();
        setEarliestPayloadOfInterest(flushReq);
        synchronized (requests) {
            requests.add(flushReq);
        }
    }

    /**
     * Set DOM set ID
     *
     * @param domSetId DOM set ID
     *
     * @throws ConfigException if the DOMSet ID was not valid
     */
    public void setDomSetId(int domSetId)
        throws ConfigException
    {
        this.domSetId = domSetId;
        configHitFilter(domSetId);
    }

    /**
     * Set earliest hit to keep.
     *
     * @param payload earliest payload
     */
    protected void setEarliestPayloadOfInterest(IPayload payload)
    {
        earliestPayloadOfInterest = payload;
        mgr.setEarliestPayloadOfInterest(earliestPayloadOfInterest);
    }

    /**
     * Set source ID.
     *
     * @param val source ID
     */
    public void setSourceId(int val)
    {
        srcId = val;
    }

    /**
     * Set the list subscriber client (for monitoring the input queue).
     *
     * @param subscriber input queue subscriber
     */
    public void setSubscriber(PayloadSubscriber subscriber)
    {
        this.subscriber = subscriber;
    }

    /**
     * Set request collector.
     *
     * @param collector trigger collector
     */
    public void setTriggerCollector(TriggerCollector collector)
    {
        this.collector = collector;
    }

    /**
     * Set configuration ID.
     *
     * @param val configuration ID
     */
    public void setTriggerConfigId(int val)
    {
        trigCfgId = val;
    }

    /**
     * Set the factory used to create trigger requests.
     *
     * @param triggerFactory trigger factory
     */
    public void setTriggerFactory(TriggerRequestFactory triggerFactory)
    {
        this.triggerFactory = triggerFactory;
    }

    /**
     * Set the trigger manager for this trigger.
     *
     * @param mgr trigger manager
     */
    public void setTriggerManager(ITriggerManager mgr)
    {
        this.mgr = mgr;
    }

    /**
     * Set trigger name.
     *
     * @param triggerName trigger name
     */
    public void setTriggerName(String triggerName)
    {
        this.triggerName = triggerName;
        if (LOG.isDebugEnabled()) {
            LOG.debug("TriggerName = " + triggerName);
        }
    }

    /**
     * Set trigger type.
     *
     * @param val trigger type
     */
    public void setTriggerType(int val)
    {
        trigType = val;
    }

    /**
     * Wrap the trigger in a global trigger jacket and report it.
     *
     * @param trigReq trigger request
     */
    public void wrapTrigger(ITriggerRequestPayload trigReq)
    {
        // rReq is the same for a single payload.
        IReadoutRequest rReq =
            trigReq.getReadoutRequest();

        List elems;
        if (rReq == null) {
            elems = null;
        } else {
            elems = rReq.getReadoutRequestElements();
        }

        IUTCTime earliest;
        IUTCTime latest;

        if (rReq == null || elems == null || elems.size() == 0) {
            earliest = trigReq.getFirstTimeUTC();
            latest = trigReq.getLastTimeUTC();
        } else {
            earliest = null;
            latest = null;
            for (Object obj : elems) {
                IReadoutRequestElement rrElem = (IReadoutRequestElement) obj;

                if (earliest == null ||
                    earliest.longValue() >
                    rrElem.getFirstTimeUTC().longValue())
                {
                    earliest = rrElem.getFirstTimeUTC();
                }

                if (latest == null ||
                    latest.longValue() < rrElem.getLastTimeUTC().longValue())
                {
                    latest = rrElem.getLastTimeUTC();
                }
            }

            if (earliest == null || latest == null) {
                throw new Error("Couldn't find earliest/latest time for " +
                                rReq);
            }
        }

        List list = new ArrayList();
        list.add(trigReq.deepCopy());

        final int uid = getNextUID();

        IReadoutRequest newRdoutReq =
            new ReadoutRequest(rReq.getUTCTime(), uid, srcId, elems);

        ITriggerRequestPayload newReq =
            (ITriggerRequestPayload) triggerFactory.
            createPayload(uid, getTriggerType(), getTriggerConfigId(),
                          srcId, earliest.longValue(), latest.longValue(),
                          newRdoutReq, list);

        reportTrigger(newReq);

        setEarliestPayloadOfInterest(new DummyPayload(earliest));
    }

    /**
     * Debugging string.
     *
     * @return debugging string
     */
    public String toString()
    {
        return triggerName + "#" + triggerCounter;
    }
}
