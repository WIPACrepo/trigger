package icecube.daq.trigger.test;

import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.SourceIdRegistry;

import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.StrandTail;

import icecube.daq.trigger.IReadoutRequestElement;
import icecube.daq.trigger.ITriggerRequestPayload;

import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;

import icecube.daq.trigger.algorithm.AmandaCalibLaserTrigger;
import icecube.daq.trigger.algorithm.AmandaCalibT0Trigger;
import icecube.daq.trigger.algorithm.AmandaM18Trigger;
import icecube.daq.trigger.algorithm.AmandaM24Trigger;
import icecube.daq.trigger.algorithm.AmandaMFrag20Trigger;
import icecube.daq.trigger.algorithm.AmandaRandomTrigger;
import icecube.daq.trigger.algorithm.AmandaSpaseTrigger;
import icecube.daq.trigger.algorithm.AmandaStringTrigger;
import icecube.daq.trigger.algorithm.AmandaTrigger;
import icecube.daq.trigger.algorithm.AmandaVolumeTrigger;

import icecube.daq.trigger.config.TriggerReadout;

import icecube.daq.trigger.control.TriggerManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import junit.textui.TestRunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.log4j.BasicConfigurator;

class AmandaValidator
    implements PayloadValidator
{
    private long timeInc;
    private long nextStart;

    /**
     * Validate Amanda triggers.
     *
     * @param timeInc amount by which trigger times are incremented
     */
    AmandaValidator(long timeInc)
    {
        this.timeInc = timeInc;
        nextStart = timeInc * 2L;
    }

    private static long getUTC(IUTCTime time)
    {
        if (time == null) {
            return -1L;
        }

        return time.getUTCTimeAsLong();
    }

    public void validate(IWriteablePayload payload)
    {
        if (!(payload instanceof ITriggerRequestPayload)) {
            throw new Error("Unexpected payload " +
                            payload.getClass().getName());
        }

        ITriggerRequestPayload tr = (ITriggerRequestPayload) payload;

        long firstTime = getUTC(tr.getFirstTimeUTC());
        long lastTime = getUTC(tr.getLastTimeUTC());

        if (firstTime != nextStart) {
            throw new Error("Expected first trigger time " + nextStart +
                            ", not " + firstTime);
        } else if (lastTime != nextStart) {
            throw new Error("Expected last trigger time " + nextStart +
                            ", not " + lastTime);
        }

        nextStart = firstTime + timeInc;
    }
}

public class AmandaTriggerEndToEndTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    private static final MockSourceID srcId =
        new MockSourceID(SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID);

    public AmandaTriggerEndToEndTest(String name)
    {
        super(name);
    }

    private void checkLogMessages()
    {
        for (int i = 0; i < appender.getNumberOfMessages(); i++) {
            String msg = (String) appender.getMessage(i);

            if (!(msg.startsWith("Clearing ") &&
                  msg.endsWith(" rope entries")) &&
                !msg.startsWith("Resetting counter ") &&
                !msg.startsWith("Resetting decrement ") &&
                !msg.startsWith("No match for timegate "))
            {
                fail("Bad log message#" + i + ": " + appender.getMessage(i));
            }
        }
        appender.clear();
    }

    private AmandaTrigger makeAmandaTrigger(int typeBit)
    {
        AmandaTrigger trig;
        int type;
        String name;

        switch (typeBit) {
        case AmandaTrigger.MULT_FRAG_20:
            trig = new AmandaMFrag20Trigger();
            name = "MFrag20";
            type = 7;
            break;
        case AmandaTrigger.VOLUME:
            trig = new AmandaVolumeTrigger();
            name = "Volume";
            type = 8;
            break;
        case AmandaTrigger.M18:
            trig = new AmandaM18Trigger();
            name = "M18";
            type = 9;
            break;
        case AmandaTrigger.M24:
            trig = new AmandaM24Trigger();
            type = 10;
            name = "M24";
            break;
        case AmandaTrigger.STRING:
            trig = new AmandaStringTrigger();
            type = 11;
            name = "String";
            break;
        case AmandaTrigger.RANDOM:
            trig = new AmandaRandomTrigger();
            type = 12;
            name = "Random";
            break;
        case AmandaTrigger.SPASE:
            trig = new AmandaSpaseTrigger();
            type = 13;
            name = "Spase";
            break;
        case AmandaTrigger.CALIB_T0:
            trig = new AmandaCalibT0Trigger();
            type = 14;
            name = "CalibT0";
            break;
        case AmandaTrigger.CALIB_LASER:
            trig = new AmandaCalibLaserTrigger();
            type = 15;
            name = "CalibLaser"; 
           break;
        default:
            throw new Error("Unknown trigger type #" + typeBit);
        }

        trig.setTriggerType(type);
        trig.setTriggerName(name);

        final int cfgId = 104;

        trig.setSourceId(srcId);
        trig.setTriggerConfigId(cfgId);

        int rdoutType = IReadoutRequestElement.READOUT_TYPE_GLOBAL;

        trig.addReadout(new TriggerReadout(0, -6000, 25000, 25000));

        return trig;
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        appender.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);
    }

    public static Test suite()
    {
        return new TestSuite(AmandaTriggerEndToEndTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testEndToEnd()
        throws SplicerException
    {
        final int numTails = 1;
        final int numObjs = numTails * 10;

        final long multiplier = 10000L;

        TriggerManager trigMgr = new TriggerManager(srcId);
        trigMgr.setOutputFactory(new TriggerRequestPayloadFactory());

        MockPayloadDestination dest = new MockPayloadDestination();
        dest.setValidator(new AmandaValidator(multiplier));
        trigMgr.setPayloadOutput(dest);

        HKN1Splicer splicer = new HKN1Splicer(trigMgr);
        trigMgr.setSplicer(splicer);

        trigMgr.addTrigger(makeAmandaTrigger(AmandaTrigger.M18));
        trigMgr.addTrigger(makeAmandaTrigger(AmandaTrigger.M24));
        trigMgr.addTrigger(makeAmandaTrigger(AmandaTrigger.MULT_FRAG_20));
        trigMgr.addTrigger(makeAmandaTrigger(AmandaTrigger.VOLUME));
        trigMgr.addTrigger(makeAmandaTrigger(AmandaTrigger.STRING));
        //trigMgr.addTrigger(makeAmandaTrigger(AmandaTrigger.RANDOM));

        StrandTail[] tails = new StrandTail[numTails];
        for (int i = 0; i < tails.length; i++) {
            tails[i] = splicer.beginStrand();
        }

        splicer.start();

        int mask = 0x0;
        for (int i = 0; i < numObjs; i++) {
            long first = (long) (i + 1) * multiplier;
            long last = ((long) (i + 2) * multiplier) - 1L;
            MockTriggerRequest tr =
                new MockTriggerRequest(first, last, -1, mask);
            tr.setSourceID(SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID);
            tails[ i % numTails].push(tr);

            mask <<= 1;
            if (mask == 0x0 || mask > 0x10) {
                mask = 0x1;
            }
        }

        for (int i = 0; i < tails.length; i++) {
            tails[i].push(StrandTail.LAST_POSSIBLE_SPLICEABLE);
        }

        trigMgr.flush();

        for (int i = 0; i < 100 && splicer.getState() != Splicer.STOPPED; i++) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // do nothing
            }
        }

        splicer.stop();

        assertEquals("Bad number of payloads written",
                     numObjs - 1, dest.getNumberWritten());

        if (appender.getLevel().equals(org.apache.log4j.Level.ALL)) {
            appender.clear();
        } else {
            checkLogMessages();
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
