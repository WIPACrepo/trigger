package icecube.daq.trigger.test;

import icecube.daq.payload.IPayload;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.TriggerRequestFactory;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.common.ITriggerManager;
import icecube.daq.trigger.control.Interval;
import icecube.daq.trigger.control.PayloadSubscriber;
import icecube.daq.trigger.control.ITriggerCollector;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockAlgorithm
    implements INewAlgorithm
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

    public void addParameter(String name, String value)
        throws UnknownParameterException, IllegalParameterValueException
    {
        throw new Error("Unimplemented");
    }

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

    public void flush()
    {
        throw new Error("Unimplemented");
    }

    public IPayload getEarliestPayloadOfInterest()
    {
        throw new Error("Unimplemented");
    }

    /**
     * Get the input queue size.
     *
     * @return input queue size
     */
    public int getInputQueueSize()
    {
        throw new Error("Unimplemented");
    }

    public Interval getInterval(Interval interval)
    {
        if (intervals.size() == 0) {
            return null;
        }

        return intervals.get(0);
    }

    public String getMonitoringName()
    {
        return "MOCK";
    }

    /**
     * Get number of cached requests.
     *
     * @return number of cached requests
     */
    public int getNumberOfCachedRequests()
    {
        throw new Error("Unimplemented");
    }

    public int getNumberOfIntervals()
    {
        return intervals.size();
    }

    public IPayload getReleaseTime()
    {
        throw new Error("Unimplemented");
    }

    public int getSourceId()
    {
        return srcId;
    }

    public PayloadSubscriber getSubscriber()
    {
        return sub;
    }

    public int getTriggerConfigId()
    {
        return cfgId;
    }

    public int getTriggerCounter()
    {
        throw new Error("Unimplemented");
    }

    public Map<String, Object> getTriggerMonitorMap()
    {
        return trigMoniMap;
    }

    public String getTriggerName()
    {
        return name;
    }

    public int getTriggerType()
    {
        return type;
    }

    public boolean hasValidMultiplicity()
    {
        return validMultiplicity;
    }

    public boolean isConfigured()
    {
        throw new Error("Unimplemented");
    }

    public void recycleUnusedRequests()
    {
        intervals.clear();
    }

    public void release(Interval interval,
                        List<ITriggerRequestPayload> released)
    {
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
                intervals.remove(i);
                if (!fetchAll) {
                    break;
                }
            }
        }
    }

    public void resetUID()
    {
        nextUID = 0;
    }

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

    public void setSourceId(int srcId)
    {
        throw new Error("Unimplemented");
    }

    public void setSubscriber(PayloadSubscriber sub)
    {
        this.sub = sub;
    }

    public void setTriggerCollector(ITriggerCollector coll)
    {
        this.coll = coll;
    }

    public void setTriggerConfigId(int cfgId)
    {
        throw new Error("Unimplemented");
    }

    public void setTriggerFactory(TriggerRequestFactory factory)
    {
        // do nothing
    }

    public void setTriggerManager(ITriggerManager mgr)
    {
        // do nothing
    }

    public void setTriggerName(String name)
    {
        throw new Error("Unimplemented");
    }

    public void setTriggerType(int type)
    {
        throw new Error("Unimplemented");
    }

    public String toString()
    {
        return String.format("MockAlgorithm[%s typ %d cfg %d src %d uid %d]",
                             name, type, cfgId, srcId, nextUID);
    }
}
