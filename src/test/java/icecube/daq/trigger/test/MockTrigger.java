package icecube.daq.trigger.test;

import icecube.daq.payload.IDOMID;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.SourceIdRegistry;

import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.IReadoutRequestElement;

import icecube.daq.trigger.config.ITriggerConfig;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.config.TriggerReadout;

import icecube.daq.trigger.control.ITriggerControl;
import icecube.daq.trigger.control.ITriggerHandler;

import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import icecube.daq.trigger.impl.TriggerRequestPayload;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;

import icecube.daq.trigger.monitor.ITriggerMonitor;
import icecube.daq.trigger.monitor.TriggerMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class MockTrigger
    implements ITriggerConfig, ITriggerControl, ITriggerMonitor
{
    private static final int DEFAULT_HITS_PER_TRIGGER = 1000;

    private static final int OFFSET_MINUS = 1000;
    private static final int OFFSET_PLUS = 1000;
    private static final int PRESCALE = 0;

    private int hitsPerTrigger;

    private String name;
    private ISourceID srcId;
    private int type;
    private int cfgId;

    private TriggerRequestPayloadFactory factory;
    private ITriggerHandler handler;
    private IPayload earliest;

    private ArrayList<IHitPayload> hits = new ArrayList<IHitPayload>();
    private ArrayList<TriggerReadout> readouts =
        new ArrayList<TriggerReadout>();

    private int counter;

    public MockTrigger()
    {
        this(DEFAULT_HITS_PER_TRIGGER);
    }

    public MockTrigger(int hitsPerTrigger)
    {
        this.hitsPerTrigger = hitsPerTrigger;

        this.name = "MockTrigger";
        this.srcId =
            new MockSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);
        this.type = 1;
        this.cfgId = 1;
    }

    public void addParameter(TriggerParameter param)
        throws UnknownParameterException, IllegalParameterValueException
    {
        throw new Error("Unimplemented");
    }

    public void addReadout(TriggerReadout rdout)
    {
        readouts.add(rdout);
    }

    public void flush()
    {
        // do nothing
    }

    private void formTrigger(List<IHitPayload> hits)
    {
        if (factory == null) {
            throw new Error("TriggerFactory is not set!");
        }

        // get times (this assumes that the hits are time-ordered)
        int numberOfHits = hits.size();
        IUTCTime firstTime = hits.get(0).getPayloadTimeUTC();
        IUTCTime lastTime = hits.get(numberOfHits - 1).getPayloadTimeUTC();

        // set earliest payload of interest to 1/10 ns after the last hit
        IHitPayload lastHit = hits.get(numberOfHits - 1);

        long offset =
            lastHit.getHitTimeUTC().getOffsetUTCTime(0.1).getUTCTimeAsLong();
        IPayload earliest = new MockHit(offset);
        setEarliestPayloadOfInterest(earliest);

        // create readout requests
        Vector readoutElems = new Vector();
        for (TriggerReadout readout : readouts) {
            final int roType = IReadoutRequestElement.READOUT_TYPE_GLOBAL;

            final IUTCTime timeMinus =
                firstTime.getOffsetUTCTime(-OFFSET_MINUS);
            final IUTCTime timePlus =
                lastTime.getOffsetUTCTime(OFFSET_PLUS);

            final IReadoutRequestElement elem =
                factory.createReadoutRequestElement(roType, timeMinus,
                                                    timePlus, null, null);
            readoutElems.add(elem);
        }

        final IReadoutRequest readoutRequest =
            factory.createReadoutRequest(srcId, counter, readoutElems);

        // make payload
        TriggerRequestPayload triggerPayload
            = (TriggerRequestPayload) factory.createPayload(counter, type,
                                                            cfgId, srcId,
                                                            firstTime,
                                                            lastTime,
                                                            new Vector(hits),
                                                            readoutRequest);

        // report it
        reportTrigger(triggerPayload);
    }

    public IPayload getEarliestPayloadOfInterest()
    {
        return earliest;
    }

    public List getParamterList()
    {
        throw new Error("Unimplemented");
    }

    public List getReadoutList()
    {
        throw new Error("Unimplemented");
    }

    public ISourceID getSourceId()
    {
        return srcId;
    }

    public int getTriggerConfigId()
    {
        return cfgId;
    }

    public int getTriggerCounter()
    {
        return counter;
    }

    public ITriggerHandler getTriggerHandler()
    {
        throw new Error("Unimplemented");
    }

    public TriggerMonitor getTriggerMonitor()
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

    public boolean isOnTrigger()
    {
        throw new Error("Unimplemented");
    }

    public void reportTrigger(ILoadablePayload payload)
    {
        if (handler == null) {
            throw new Error("TriggerHandler was not set!");
        }

        counter++;
        if (PRESCALE == 0 || (counter % PRESCALE) == 0) {
            handler.addToTriggerBag(payload);
        } else {
            payload.recycle();
        }
    }

    public void runTrigger(IPayload payload)
        throws TriggerException
    {
        int interfaceType = payload.getPayloadInterfaceType();
        if ((interfaceType != PayloadInterfaceRegistry.I_HIT_PAYLOAD) &&
            (interfaceType != PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD))
        {
            throw new TriggerException("Expecting an IHitPayload, got type " +
                                       interfaceType);
        }
        hits.add((IHitPayload) payload);

        if (hits.size() >= hitsPerTrigger) {
            formTrigger(hits);
            hits.clear();
        }
    }

    public void setEarliestPayloadOfInterest(IPayload payload)
    {
        earliest = payload;
    }

    public void setSourceId(ISourceID srcId)
    {
        this.srcId = srcId;
    }

    public void setTriggerConfigId(int cfgId)
    {
        this.cfgId = cfgId;
    }

    public void setTriggerFactory(TriggerRequestPayloadFactory factory)
    {
        this.factory = factory;
    }

    public void setTriggerHandler(ITriggerHandler handler)
    {
        this.handler = handler;
    }

    public void setTriggerName(String name)
    {
        this.name = name;
    }

    public void setTriggerType(int type)
    {
        this.type = type;
    }

    public String toString()
    {
        return "MockTrigger " + name + " src " + srcId + " type " + type +
            " cfgId " + cfgId + " earliest " + earliest + " cnt " + counter;
    }
}
