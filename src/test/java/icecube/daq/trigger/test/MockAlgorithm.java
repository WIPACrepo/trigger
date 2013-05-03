package icecube.daq.trigger.test;

import icecube.daq.payload.IPayload;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.impl.TriggerRequestFactory;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.common.ITriggerManager;
import icecube.daq.trigger.control.Interval;
import icecube.daq.trigger.control.PayloadSubscriber;
import icecube.daq.trigger.control.TriggerCollector;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import java.util.ArrayList;
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

    private PayloadSubscriber sub;

    public MockAlgorithm(String name)
    {
        this.name = name;
    }

    public MockAlgorithm(String name, int type, int cfgId, int srcId)
    {
        this.name = name;
        this.type = type;
        this.cfgId = cfgId;
        this.srcId = srcId;
    }

    public void addInterval(long start, long end)
    {
        intervals.add(new Interval(start, end));
    }

    public void addInterval(Interval interval)
    {
        intervals.add(interval);
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

    public IPayload getEarliestPayloadOfInterest()
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

    public Map getTriggerMonitorMap()
    {
        throw new Error("Unimplemented");
    }

    public String getTriggerName()
    {
        return name;
    }

    public int getTriggerType()
    {
        return type;
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
                MockTriggerRequest req =
                    new MockTriggerRequest(nextUID++, type, cfgId, iv.start,
                                           iv.end);
                released.add(req);
                intervals.remove(i);
            }
        }
    }

    public void resetUID()
    {
        throw new Error("Unimplemented");
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

    public void setTriggerCollector(TriggerCollector coll)
    {
        // do nothing
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
}
