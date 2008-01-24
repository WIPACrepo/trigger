package icecube.daq.trigger.test;

import icecube.daq.payload.IWriteablePayload;

import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.StrandTail;

import icecube.daq.trigger.ITriggerRequestPayload;

import icecube.daq.trigger.algorithm.AbstractTrigger;

import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.config.TriggerReadout;

import icecube.daq.trigger.control.PayloadChecker;

import icecube.daq.trigger.exceptions.TriggerException;

import java.io.IOException;

import java.nio.channels.WritableByteChannel;

/**
 * Triggers described in sps-icecube-amanda-008.xml
 */
public class SPSIcecubeAmanda008Triggers
    extends TriggerCollection
{
    private boolean checkSequentialTimes;
    private int numHitsPerTrigger;
    private long timeBase;
    private long timeStep;

    public SPSIcecubeAmanda008Triggers()
        throws TriggerException
    {
        this(true);
    }

    public SPSIcecubeAmanda008Triggers(boolean checkSequentialTimes)
        throws TriggerException
    {
        this.checkSequentialTimes = checkSequentialTimes;

        AbstractTrigger trig;

        trig = createTrigger(3, -1, GLOBAL_TRIGGER, "ThroughputTrigger");
        add(trig);

        trig = createTrigger(2, 101, ICETOP_TRIGGER, "MinBiasTrigger");
        trig.addParameter(new TriggerParameter("prescale", "10000"));
        trig.addReadout(new TriggerReadout(0, 0, 25000, 25000));
        add(trig);

        trig = createTrigger(0, 102, ICETOP_TRIGGER, "SimpleMajorityTrigger");
        trig.addParameter(new TriggerParameter("threshold", "6"));
        trig.addParameter(new TriggerParameter("timeWindow", "5000"));
        trig.addReadout(new TriggerReadout(0, 0, 10000, 10000));
        add(trig);

        trig = createTrigger(2, 101, INICE_TRIGGER, "MinBiasTrigger");
        trig.addParameter(new TriggerParameter("prescale", "10000"));
        trig.addReadout(new TriggerReadout(0, 0, 25000, 25000));
        add(trig);

        trig = createTrigger(0, 103, INICE_TRIGGER, "SimpleMajorityTrigger");
        trig.addParameter(new TriggerParameter("threshold", "8"));
        trig.addParameter(new TriggerParameter("timeWindow", "5000"));
        trig.addReadout(new TriggerReadout(0, 0, 10000, 10000));
        add(trig);

        trig = createTrigger(7, 104, AMANDA_TRIGGER, "AmandaMFrag20Trigger");
        trig.addReadout(new TriggerReadout(0, -6000, 25000, 25000));
        add(trig);

        trig = createTrigger(8, 104, AMANDA_TRIGGER, "AmandaVolumeTrigger");
        trig.addReadout(new TriggerReadout(0, -6000, 25000, 25000));
        add(trig);

        trig = createTrigger(9, 104, AMANDA_TRIGGER, "AmandaM18Trigger");
        trig.addReadout(new TriggerReadout(0, -6000, 25000, 25000));
        add(trig);

        trig = createTrigger(10, 104, AMANDA_TRIGGER, "AmandaM24Trigger");
        trig.addReadout(new TriggerReadout(0, -6000, 25000, 25000));
        add(trig);

        trig = createTrigger(11, 104, AMANDA_TRIGGER, "AmandaStringTrigger");
        trig.addReadout(new TriggerReadout(0, -6000, 25000, 25000));
        add(trig);

        trig = createTrigger(12, 104, AMANDA_TRIGGER, "AmandaRandomTrigger");
        trig.addReadout(new TriggerReadout(0, -6000, 25000, 25000));
        add(trig);

        trig = createTrigger(13, 105, INICE_TRIGGER, "PhysicsMinBiasTrigger");
        trig.addParameter(new TriggerParameter("deadtime", "5000"));
        trig.addParameter(new TriggerParameter("prescale", "200"));
        trig.addReadout(new TriggerReadout(0, 0, 10000, 10000));
        add(trig);

        numHitsPerTrigger = 8;
        timeBase = 100000L;
        timeStep = 5000L / (long) (numHitsPerTrigger + 1);
    }

    public int getExpectedNumberOfAmandaPayloads(int numObjs)
    {
        return numObjs - 1;
    }

    public int getExpectedNumberOfInIcePayloads(int numObjs)
    {
        return numObjs / numHitsPerTrigger;
    }

    public BaseValidator getAmandaValidator()
    {
        return new AmandaValidator();
    }

    public BaseValidator getInIceValidator()
    {
        return new InIceValidator();
    }

    public void sendAmandaData(WritableByteChannel[] tails, int numObjs)
        throws IOException
    {
        int trigType = 0;
        for (int i = 0; i < numObjs; i++) {
            long first = timeBase + (long) (i + 1) * timeStep;
            long last = timeBase + ((long) (i + 2) * timeStep) - 1L;

            final int tailIndex = i % tails.length;
            sendTrigger(tails[tailIndex], first, last, trigType,
                        AMANDA_TRIGGER);

            trigType++;
            if (trigType < 7 || trigType > 11) {
                trigType = 7;
            }
        }
    }

    public void sendAmandaStops(WritableByteChannel[] tails)
        throws IOException
    {
        for (int i = 0; i < tails.length; i++) {
            sendStopMsg(tails[i]);
        }
    }

    public void sendInIceData(WritableByteChannel[] tails, int numObjs)
        throws IOException
    {
        for (int i = 0; i < numObjs; i++) {
            final long time;
            if (i == 0) {
                time = timeBase;
            } else {
                time = (timeBase * (((i - 1) / numHitsPerTrigger) + 1)) +
                    (timeStep * i);
            }

            final int tailIndex = i % tails.length;
            sendHit(tails[tailIndex], time, tailIndex, 987654321L * i);
        }
    }

    public void sendInIceStops(WritableByteChannel[] tails)
        throws IOException
    {
        for (int i = 0; i < tails.length; i++) {
            sendStopMsg(tails[i]);
        }
    }

    class AmandaValidator
        extends BaseValidator
    {
        private long nextStart;
        private long nextEnd;

        /**
         * Validate Amanda triggers.
         */
        AmandaValidator()
        {
            nextStart = timeBase + timeStep * 2L;
            nextEnd = nextStart;
        }

        public void validate(IWriteablePayload payload)
        {
            if (!(payload instanceof ITriggerRequestPayload)) {
                throw new Error("Unexpected payload " +
                                payload.getClass().getName());
            }

            //dumpPayloadBytes(payload);

            ITriggerRequestPayload tr = (ITriggerRequestPayload) payload;

            if (!PayloadChecker.validateTriggerRequest(tr, true)) {
                throw new Error("Trigger request is not valid");
            }

            long firstTime = getUTC(tr.getFirstTimeUTC());
            long lastTime = getUTC(tr.getLastTimeUTC());

            if (checkSequentialTimes) {
                if (firstTime != nextStart) {
                    throw new Error("Expected first trigger time " + nextStart +
                                    ", not " + firstTime);
                } else if (lastTime != nextStart) {
                    throw new Error("Expected last trigger time " + nextStart +
                                    ", not " + lastTime);
                }
            }
/*
System.err.println("First: EXP "+nextStart+" ACT "+firstTime+" DIFF "+(firstTime-nextStart));
System.err.println(" Last: EXP "+nextEnd+" ACT "+lastTime+" DIFF "+(lastTime-nextEnd));
*/

            nextStart = firstTime + timeStep;
            nextEnd = nextStart;
        }
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
            timeSpan = timeStep * (long) numHitsPerTrigger;

            nextStart = timeBase;
            nextEnd = nextStart + timeSpan;
        }

        public void validate(IWriteablePayload payload)
        {
            if (!(payload instanceof ITriggerRequestPayload)) {
                throw new Error("Unexpected payload " +
                                payload.getClass().getName());
            }

            //dumpPayloadBytes(payload);

            ITriggerRequestPayload tr = (ITriggerRequestPayload) payload;

            if (!PayloadChecker.validateTriggerRequest(tr, true)) {
                throw new Error("Trigger request is not valid");
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
/*
System.err.println("First: EXP "+nextStart+" ACT "+firstTime+" DIFF "+(firstTime-nextStart));
System.err.println(" Last: EXP "+nextEnd+" ACT "+lastTime+" DIFF "+(lastTime-nextEnd));
*/

            nextStart = firstTime + timeBase + timeSpan;
            if (jumpHack) {
                nextStart += timeStep;
                jumpHack = false;
            }
            nextEnd = lastTime + timeBase + timeSpan;
        }
    }
}
