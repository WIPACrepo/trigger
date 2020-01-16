package icecube.daq.trigger.test;

import icecube.daq.payload.IPayload;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.TriggerRequestFactory;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.control.ITriggerCollector;
import icecube.daq.trigger.control.ITriggerManager;
import icecube.daq.trigger.control.Interval;
import icecube.daq.trigger.control.PayloadSubscriber;
import icecube.daq.trigger.control.SubscribedList;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockAlgorithm
    implements ITriggerAlgorithm
{
    private String name;
    private boolean sawFlush;

    private TriggerException runException;
    private boolean sentLast;
    private ArrayList<Interval> intervals = new ArrayList<Interval>();

    private int nextUID;
    private int type;
    private int cfgId;
    private int srcId;
    private boolean validMultiplicity;

    private PayloadSubscriber sub;
    private Map<String, Object> trigMoniMap;

    private boolean fetchAll = true;

    private ITriggerCollector coll;

    public MockAlgorithm(String name)
    {
        this(name, 1, 1, SourceIdRegistry.INICE_TRIGGER_SOURCE_ID, true);
    }

    public MockAlgorithm(String name, int type, int cfgId, int srcId)
    {
        this(name, type, cfgId, srcId, true);
    }

    public MockAlgorithm(String name, int type, int cfgId, int srcId,
                         boolean validMultiplicity)
    {
        this.name = name;
        this.type = type;
        this.cfgId = cfgId;
        this.srcId = srcId;
        this.validMultiplicity = validMultiplicity;
    }

    public void addInterval(long start, long end)
    {
        addInterval(new Interval(start, end));
    }

    public void addInterval(Interval interval)
    {
        intervals.add(interval);

        if (coll != null) {
            coll.setChanged();
        }
    }

    @Override
    public void addParameter(String name, String value)
        throws UnknownParameterException, IllegalParameterValueException
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void addReadout(int rdoutType, int offset, int minus, int plus)
    {
        throw new Error("Unimplemented");
    }

    public void addTriggerMonitorData(String key, Object value)
    {
        if (trigMoniMap == null) {
            trigMoniMap = new HashMap<String, Object>();
        }

        trigMoniMap.put(key, value);
    }

    @Override
    public int compareTo(ITriggerAlgorithm a)
    {
        int val = getTriggerName().compareTo(a.getTriggerName());
        if (val == 0) {
            val = type - a.getTriggerType();
            if (val == 0) {
                val = cfgId - a.getTriggerConfigId();
                if (val == 0) {
                    val = srcId - a.getSourceId();
                }
            }
        }

        return val;
    }

    @Override
    public void flush()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public IPayload getEarliestPayloadOfInterest()
    {
        throw new Error("Unimplemented");
    }

    /**
     * Get the input queue size.
     *
     * @return input queue size
     */
    @Override
    public int getInputQueueSize()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public Interval getInterval(Interval interval)
    {
        if (intervals.size() == 0) {
            return null;
        }

        return intervals.get(0);
    }

    /**
     * Return the difference between the start of the first cached request
     * and the earliest payload of interest (in DAQ ticks).
     *
     * @return latency in DAQ ticks
     */
    @Override
    public long getLatency()
    {
        return -1L;
    }

    @Override
    public String getMonitoringName()
    {
        return "MOCK";
    }

    /**
     * Get number of cached requests.
     *
     * @return number of cached requests
     */
    @Override
    public int getNumberOfCachedRequests()
    {
        return intervals.size();
    }

    public int getNumberOfIntervals()
    {
        return intervals.size();
    }

    @Override
    public long getReleaseTime()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public long getSentTriggerCount()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public int getSourceId()
    {
        return srcId;
    }

    @Override
    public PayloadSubscriber getSubscriber()
    {
        return sub;
    }

    @Override
    public int getTriggerConfigId()
    {
        return cfgId;
    }

    @Override
    public int getTriggerCounter()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public Map<String, Object> getTriggerMonitorMap()
    {
        return trigMoniMap;
    }

    @Override
    public String getTriggerName()
    {
        return name;
    }

    @Override
    public int getTriggerType()
    {
        return type;
    }

    @Override
    public boolean hasCachedRequests()
    {
        return intervals.size() > 0 && !intervals.get(0).isFlush();
    }

    @Override
    public boolean hasData()
    {
        return sub.hasData();
    }

    @Override
    public boolean hasValidMultiplicity()
    {
        return validMultiplicity;
    }

    @Override
    public boolean isConfigured()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void recycleUnusedRequests()
    {
        intervals.clear();
    }

    @Override
    public int release(Interval interval,
                       List<ITriggerRequestPayload> released)
    {
        int rtnval = 0;

        int i = 0;
        while (i < intervals.size()) {
            Interval iv = intervals.get(i);
            if (iv.start < interval.start || iv.end > interval.end) {
                i++;
            } else {
                final int uid = nextUID++;
                MockTriggerRequest req =
                    new MockTriggerRequest(uid, type, cfgId, iv.start,
                                           iv.end);
                req.setReadoutRequest(new MockReadoutRequest(uid, srcId));
                if (srcId == SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID) {
                    final int guid = nextUID++;
                    MockTriggerRequest gReq =
                        new MockTriggerRequest(guid, type, -1, iv.start,
                                               iv.end);
                    gReq.setReadoutRequest(new MockReadoutRequest(guid, srcId));
                    gReq.addPayload(req);
                    req = gReq;
                }
                released.add(req);
                rtnval++;

                intervals.remove(i);
                if (!fetchAll) {
                    break;
                }
            }
        }

        return rtnval;
    }

    @Override
    public void resetAlgorithm()
    {
        // do nothing
    }

    @Override
    public void resetUID()
    {
        nextUID = 0;
    }

    @Override
    public void runTrigger(IPayload pay)
        throws TriggerException
    {
        if (runException != null) {
            throw runException;
        }

        if (pay == TriggerManager.FLUSH_PAYLOAD) {
            sawFlush = true;
        } else if (sawFlush) {
            throw new Error("Saw payload after FLUSH");
        }
    }

    public boolean sawFlush()
    {
        return sawFlush;
    }

    @Override
    public void sendLast()
    {
        sentLast = true;
    }

    public void setFetchAll(boolean val)
    {
        fetchAll = val;
    }

    public void setRunException(TriggerException ex)
    {
        runException = ex;
    }

    public void setSawFlush()
    {
        sawFlush = true;
    }

    @Override
    public void setSourceId(int srcId)
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void setSubscriber(PayloadSubscriber sub)
    {
        this.sub = sub;
    }

    @Override
    public void setTriggerCollector(ITriggerCollector coll)
    {
        this.coll = coll;
    }

    @Override
    public void setTriggerConfigId(int cfgId)
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void setTriggerFactory(TriggerRequestFactory factory)
    {
        // do nothing
    }

    @Override
    public void setTriggerManager(ITriggerManager mgr)
    {
        // do nothing
    }

    @Override
    public void setTriggerName(String name)
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void checkTriggerType(int type)
        throws ConfigException
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void unsubscribe(SubscribedList list)
    {
        if (!list.unsubscribe(sub)) {
            throw new Error("Cannot remove " + toString() + " from " + list);
        }

        sub = null;
    }

    @Override
    public String toString()
    {
        return String.format("MockAlgorithm[%s typ %d cfg %d src %d uid %d]",
                             name, type, cfgId, srcId, nextUID);
    }
}
