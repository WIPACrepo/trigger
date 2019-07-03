package icecube.daq.trigger.test;

import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.PayloadChecker;
import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.StrandTail;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.config.TriggerReadout;
import icecube.daq.trigger.exceptions.TriggerException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Triggers described in sps-icecube-amanda-008.xml
 */
public class CylinderTriggerConfig
    extends TriggerCollection
{
    private static long domIds[] = new long[] {
        0x555bc8be648aL,
        0xe35b214bc073L,
        0x6dac20fd0621L,
        0x6f340c2abb03L,
        0x68c12b5b59e9L,
        0x555bc8be648aL,
        0x25024c2f6aabL,
        0xe26b3232c31eL,
        0x6f340c2abb03L,
        0x15051df110e1L,
        0x73aa84286532L,
        0x25024c2f6aabL,
        0x022663eb69d9L,
        0x4abe48b5d75eL,
        0x15051df110e1L,
        0x740e5e3dbbf5L,
        0x5f5ad6750b1aL,
        0x022663eb69d9L,
        0x23567996b16fL,
        0xd656756eb743L,
        0x740e5e3dbbf5L,
        0x3406f0c821d0L,
        0x2387fad22e74L,
        0x23567996b16fL,
        0x7ff781326bbdL,
        0x4abca28ac534L,
        0x3406f0c821d0L,
        0x8df51c30e388L,
        0xce38a9c852aeL,
        0x7ff781326bbdL,
        0x3ce7dc443f2aL,
    };

    private boolean checkSequentialTimes;
    private int numHitsPerTrigger;
    private long timeBase;
    private long timeStep;

    public CylinderTriggerConfig()
        throws TriggerException
    {
        this(false);
    }

    public CylinderTriggerConfig(boolean checkSequentialTimes)
        throws TriggerException
    {
        this.checkSequentialTimes = checkSequentialTimes;

        ITriggerAlgorithm trig;

        numHitsPerTrigger = 8;
        timeBase = 100000L;
        timeStep = 10000L / (long) (numHitsPerTrigger + 1);

        trig = createTrigger(21001, INICE_TRIGGER, "CylinderTrigger");
        trig.addParameter("multiplicity", "4");
        trig.addParameter("simpleMultiplicity",
                          Integer.toString(numHitsPerTrigger));
        trig.addParameter("radius", "175");
        trig.addParameter("height", "75");
        trig.addParameter("timeWindow", "1000");
        trig.addParameter("domSet", "2");
        trig.addReadout(READOUT_ALL_ICETOP, 0, 10000, 10000);
        trig.addReadout(READOUT_ALL_INICE, 0, 4000, 6000);
        add(trig);

        setSourceId(STRINGHUB);
    }

    @Override
    public BaseValidator getAmandaValidator()
    {
        return null;
    }

    @Override
    public int getExpectedNumberOfAmandaPayloads(int numObjs)
    {
        return 0;
    }

    @Override
    public int getExpectedNumberOfInIcePayloads(int numObjs)
    {
        return numObjs / numHitsPerTrigger;
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
                time = (timeBase * (((i - 1) / numHitsPerTrigger) + 1)) +
                    (timeStep * i) + ((i / 5) * 10000L);
            }

            long domId = domIds[i % domIds.length];
            final int tailIndex = i % tails.length;
            sendHit(tails[tailIndex], time, tailIndex, domId);
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
            timeSpan = timeStep * (long) numHitsPerTrigger;

            nextStart = timeBase;
            nextEnd = nextStart + timeSpan;
        }

        @Override
        public boolean validate(IWriteablePayload payload)
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

            return true;
        }
    }
}
