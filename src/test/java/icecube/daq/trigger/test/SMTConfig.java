package icecube.daq.trigger.test;

import icecube.daq.payload.IPayload;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.PayloadChecker;
import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.StrandTail;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.config.DomSet;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.config.TriggerReadout;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.util.DOMInfo;
import icecube.daq.util.DOMRegistryException;
import icecube.daq.util.DOMRegistryFactory;
import icecube.daq.util.IDOMRegistry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Set;

class SMTParameters
{
    int cfgId;
    int srcId;
    int threshold;
    int timeWindow;
    int domSetId;
    int readoutSet;

    SMTParameters(int threshold)
        throws TriggerException
    {
        this.threshold = threshold;

        switch (threshold) {
        case 1:
            cfgId = 30002;
            srcId = TriggerCollection.ICETOP_TRIGGER;
            timeWindow = 1000;
            domSetId = 9;
            readoutSet = 0;
            break;
        case 3:
            cfgId = 1011;
            srcId = TriggerCollection.INICE_TRIGGER;
            timeWindow = 2500;
            domSetId = 6;
            readoutSet = 2;
            break;
        case 6:
            cfgId = 102;
            srcId = TriggerCollection.ICETOP_TRIGGER;
            timeWindow = 5000;
            domSetId = 3;
            readoutSet = 0;
            break;
        case 8:
            cfgId = 102;
            srcId = TriggerCollection.INICE_TRIGGER;
            timeWindow = 5000;
            domSetId = -1;
            readoutSet = 1;
            break;
        default:
            throw new TriggerException("No configuration for threshold " +
                                       threshold);
        }
    }

    void configure(ITriggerAlgorithm trig)
        throws TriggerException
    {
        trig.setSourceId(srcId);

        trig.addParameter("threshold", Integer.toString(threshold));
        trig.addParameter("timeWindow", Integer.toString(timeWindow));

        if (domSetId >= 0) {
            trig.addParameter("domSet", Integer.toString(domSetId));
        }

        switch (readoutSet) {
        case 0:
            trig.addReadout(TriggerCollection.READOUT_ALL, 0, 10000, 10000);
            break;
        case 1:
            trig.addReadout(TriggerCollection.READOUT_ALL_ICETOP, 0,
                            10000, 10000);
            trig.addReadout(TriggerCollection.READOUT_ALL_INICE, 0,
                            4000, 6000);
            break;
        case 2:
            trig.addReadout(TriggerCollection.READOUT_ALL_ICETOP, 0,
                            10000, 10000);
            trig.addReadout(TriggerCollection.READOUT_ALL_INICE, 0,
                            6000, 6000);
            break;
        default:
            throw new TriggerException("Unknown readout set #" + readoutSet);
        }
    }
}

/**
 * Triggers described in sps-2018-iceact-001.xml
 */
public class SMTConfig
    extends TriggerCollection
{
    private boolean checkSequentialTimes;

    private int threshold;
    private long timeBase;
    private long timeStep;

    private int domSetId;
    private DOMInfo[] doms;

    public SMTConfig(IDOMRegistry registry, int threshold)
        throws DOMRegistryException, TriggerException
    {
        this(registry, threshold, false);
    }

    private SMTConfig(IDOMRegistry registry, int threshold,
                      boolean checkSequentialTimes)
        throws  DOMRegistryException, TriggerException
    {
        this.checkSequentialTimes = checkSequentialTimes;

        SMTParameters params = new SMTParameters(threshold);

        final ITriggerAlgorithm trig =
            createTrigger(params.cfgId, params.srcId, "SMT" + threshold);
        params.configure(trig);
        add(trig);

        this.threshold = params.threshold;
        timeBase = 100000L;
        timeStep = 10000L / (long) (threshold + 1);

        int hubId;
        if (params.srcId == ICETOP_TRIGGER) {
            hubId = 208;
        } else {
            hubId = 81;
        }

        doms = buildDOMArray(registry, params.domSetId, hubId, 60);

        setSourceId(STRINGHUB);
    }

    private static final DOMInfo[] emptyArray = new DOMInfo[0];

    private DOMInfo[] buildDOMArray(IDOMRegistry registry, int domSetId,
                                    int hubId, int number)
        throws DOMRegistryException, TriggerException
    {
        DomSet domSet;
        if (domSetId < 0) {
            domSet = null;
        } else {
            domSet = DomSetFactory.getDomSet(domSetId);
        }

        ArrayList<DOMInfo> allDoms = new ArrayList<DOMInfo>();

        for (DOMInfo dom : registry.getDomsOnHub(hubId)) {
            if (domSet == null || domSet.inSet(dom.getChannelId())) {
                allDoms.add(dom);
            }
        }

        if (allDoms.size() == 0) {
            throw new Error("No DOMs found on hub#" + hubId + " (domset " +
                            domSet + ")");
        }

        return allDoms.toArray(emptyArray);
    }

    @Override
    public BaseValidator getAmandaValidator()
    {
        return null;
    }

    public DOMInfo getDOM(int idx)
    {
        if (idx < 0) {
            throw new Error("DOM index cannot be negative");
        }

        return doms[idx % doms.length];
    }

    @Override
    public int getExpectedNumberOfAmandaPayloads(int numObjs)
    {
        return 0;
    }

    @Override
    public int getExpectedNumberOfInIcePayloads(int numObjs)
    {
        return (numObjs / threshold) - 1;
    }

    @Override
    public BaseValidator getInIceValidator()
    {
        return new InIceValidator();
    }

    public void sendAmandaData(WritableByteChannel[] tails, int numObjs)
        throws IOException
    {
    }

    @Override
    public void sendAmandaStops(WritableByteChannel[] tails)
        throws IOException
    {
        sendStops(tails);
    }

    public void sendInIceData(WritableByteChannel[] tails, int numObjs)
        throws IOException
    {
        for (int i = 0; i < numObjs; i++) {
            final long time;
            if (i == 0) {
                time = timeBase;
            } else {
                time = (timeBase * (((i - 1) / threshold) + 1)) +
                    (timeStep * i) + ((i / 5) * 10000L);
            }

            DOMInfo dom = doms[i % doms.length];
            final int tailIndex = i % tails.length;
            sendHit(tails[tailIndex], time, tailIndex,
                    dom.getNumericMainboardId());
        }
    }

    @Override
    public void sendInIceStops(WritableByteChannel[] tails)
        throws IOException
    {
        sendStops(tails);
    }

    class InIceValidator
        extends BaseValidator
    {
        private long timeSpan;

        private boolean jumpHack = true;

        private long nextStart;
        private long nextEnd;

        InIceValidator()
        {
            timeSpan = timeStep * (long) threshold;

            nextStart = timeBase;
            nextEnd = nextStart + timeSpan;
        }

        public boolean validate(IPayload payload)
        {
            if (!(payload instanceof ITriggerRequestPayload)) {
                throw new Error("Unexpected payload " +
                                payload.getClass().getName());
            }

            //dumpPayloadBytes(payload);

            ITriggerRequestPayload tr = (ITriggerRequestPayload) payload;

            if (!PayloadChecker.validateTriggerRequest(tr, true)) {
                throw new Error("Trigger request is not valid: " + tr);
            }

            long firstTime = getUTC(tr.getFirstTimeUTC());
            long lastTime = getUTC(tr.getLastTimeUTC());

            if (checkSequentialTimes) {
                if (firstTime != nextStart) {
                    throw new Error("Expected first trigger time " + nextStart +
                                    ", not " + firstTime);
                } else if (lastTime != nextEnd) {
                    throw new Error("Expected last trigger time " + nextEnd +
                                    ", not " + lastTime);
                }
            }

            nextStart = firstTime + timeBase + timeSpan;
            if (jumpHack) {
                nextStart += timeStep;
                jumpHack = false;
            }
            nextEnd = lastTime + timeBase + timeSpan;

            return true;
        }
    }
}
