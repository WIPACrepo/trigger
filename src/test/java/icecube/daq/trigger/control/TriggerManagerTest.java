package icecube.daq.trigger.control;

import icecube.daq.oldpayload.impl.MasterPayloadFactory;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.StrandTail;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockOutputChannel;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockSplicer;
import icecube.daq.trigger.test.MockTrigger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class TriggerManagerTest
    extends TestCase
{
    private static final MockSourceID SOURCE_ID =
        new MockSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);

    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public TriggerManagerTest(String name)
    {
        super(name);
    }

    private void loadAndRun(TriggerManager trigMgr)
        throws SplicerException
    {
        Splicer splicer = trigMgr.getSplicer();

        final int numTails = 10;
        final int numObjs = numTails * 10;
        final int numHitsPerTrigger = 8;

        trigMgr.addTrigger(new MockTrigger(numHitsPerTrigger));

        StrandTail[] tails = new StrandTail[numTails];
        for (int i = 0; i < tails.length; i++) {
            tails[i] = splicer.beginStrand();
        }

        splicer.start();

        for (int i = 0; i < numObjs; i++) {
            tails[ i % numTails].push(new MockHit((long) (i + 1) * 10000L));
        }

        waitForRecordsSent(trigMgr);

        for (int i = 0; i < tails.length; i++) {
            tails[i].push(StrandTail.LAST_POSSIBLE_SPLICEABLE);
        }

        for (int i = 0; i < 100 && splicer.getState() != Splicer.STOPPED; i++) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // do nothing
            }
        }

        splicer.stop();
    }

    public void runWithRealSplicer(TriggerManager trigMgr)
        throws SplicerException
    {
        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        HKN1Splicer splicer = new HKN1Splicer(trigMgr);
        trigMgr.setSplicer(splicer);

        loadAndRun(trigMgr);

        for (int i = 0; i < 100 && !outProc.isStopped(); i++) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // do nothing
            }
        }

        waitForRecordsSent(trigMgr);

        assertTrue("Output process has not stopped", outProc.isStopped());

        assertEquals("Bad number of payloads written",
                     12, outProc.getNumberWritten());

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
        return new TestSuite(TriggerManagerTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testRealSplicer()
        throws SplicerException
    {
        MasterPayloadFactory factory = new MasterPayloadFactory();

        TriggerManager trigMgr =
            new TriggerManager(factory, SOURCE_ID,
                               new TriggerRequestPayloadFactory());

        runWithRealSplicer(trigMgr);
    }

    public void testMockSplicer()
        throws SplicerException
    {
        MasterPayloadFactory factory = new MasterPayloadFactory();

        TriggerManager trigMgr =
            new TriggerManager(factory, SOURCE_ID,
                               new TriggerRequestPayloadFactory());

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        MockSplicer splicer = new MockSplicer(trigMgr);
        trigMgr.setSplicer(splicer);

        loadAndRun(trigMgr);

        for (int i = 0; i < 100 && !outProc.isStopped(); i++) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // do nothing
            }
        }

        assertEquals("Bad number of payloads written",
                     12, outProc.getNumberWritten());

        trigMgr.reset();
    }

    private void waitForRecordsSent(TriggerManager trigMgr)
    {
        long prevSent = 0L;
        int prevSame = 0;
        for (int i = 0; i < 10; i++) {
            long[] recsSent = trigMgr.getPayloadOutput().getRecordsSent();

            long curSent;
            if (recsSent == null) {
                curSent = 0L;
            } else {
                curSent = recsSent[0];
            }

            if (curSent > 0L) {
                if (curSent != prevSent) {
                    prevSame = 0;
                } else {
                    prevSame++;
                    if (prevSame == 3) {
                        break;
                    }
                }
            }

            prevSent = curSent;

            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // do nothing
            }
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
