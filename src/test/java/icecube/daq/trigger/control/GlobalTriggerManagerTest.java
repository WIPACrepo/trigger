package icecube.daq.trigger.control;

import icecube.daq.oldpayload.impl.MasterPayloadFactory;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.StrandTail;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockOutputChannel;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockReadoutRequest;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockSplicer;
import icecube.daq.trigger.test.MockTrigger;
import icecube.daq.trigger.test.MockTrigger;
import icecube.daq.trigger.test.MockTriggerRequest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

class GTriggerTrigger
    extends MockTrigger
{
    public void runTrigger(IPayload payload)
        throws TriggerException
    {
        reportTrigger((ILoadablePayload) payload);
    }
}

public class GlobalTriggerManagerTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    private static final MockSourceID SOURCE_ID =
        new MockSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);

    public GlobalTriggerManagerTest(String name)
    {
        super(name);
    }

    private void checkLogMessages()
    {
        for (int i = 0; i < appender.getNumberOfMessages(); i++) {
            String msg = (String) appender.getMessage(i);

            if (!(msg.contains("I3 GlobalTrigger Run Summary")) &&
                !msg.startsWith("Resetting counter ") &&
                true)
            {
                fail("Bad log message#" + i + ": " + appender.getMessage(i));
            }
        }
        appender.clear();
    }

    private void loadAndRun(GlobalTriggerManager trigMgr, int numTails,
                            int numSteps)
        throws SplicerException
    {
        MockHit hit = new MockHit(123456789L);

        GTriggerTrigger trig = new GTriggerTrigger();
        trig.setEarliestPayloadOfInterest(hit);
        trigMgr.addTrigger(trig);

        Splicer splicer = trigMgr.getSplicer();

        StrandTail[] tails = new StrandTail[numTails];
        for (int i = 0; i < tails.length; i++) {
            tails[i] = splicer.beginStrand();
        }

        splicer.start();

        for (int i = 0; i < numTails; i++) {
            final long timeStep = 9000L / (long) numSteps;
            for (int j = 0; j < numSteps; j++) {
                final long firstTime = (i + 1) * 10000L + (j * timeStep);
                final long lastTime = firstTime + (timeStep - 1);

                MockTriggerRequest tr =
                    new MockTriggerRequest(firstTime, lastTime, 0, 0);
                tr.setSourceID(666);
                tr.setReadoutRequest(new MockReadoutRequest());

                final int num = (i * numSteps) + j;
                tails[num % numTails].push(tr);
            }

            try {
                Thread.sleep(10);
            } catch (Exception ex) {
                // ignore interrupts
            }
        }

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

        try {
            Thread.sleep(100);
        } catch (Exception ex) {
            // ignore interrupts
        }
    }

    public void runWithRealSplicer(GlobalTriggerManager trigMgr)
        throws SplicerException
    {
        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        HKN1Splicer splicer = new HKN1Splicer(trigMgr);
        trigMgr.setSplicer(splicer);

        loadAndRun(trigMgr, 10, 10);

        assertEquals("Bad number of payloads written",
                     100, outProc.getNumberWritten());
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
        return new TestSuite(GlobalTriggerManagerTest.class);
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

        GlobalTriggerManager trigMgr =
            new GlobalTriggerManager(factory, SOURCE_ID,
                                     new TriggerRequestPayloadFactory());

        runWithRealSplicer(trigMgr);

        checkLogMessages();
    }

    public void testMockSplicer()
        throws SplicerException
    {
        MasterPayloadFactory factory = new MasterPayloadFactory();

        GlobalTriggerManager trigMgr =
            new GlobalTriggerManager(factory, SOURCE_ID,
                                     new TriggerRequestPayloadFactory());

        trigMgr.setReportingThreshold(10);

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        MockSplicer splicer = new MockSplicer(trigMgr);
        trigMgr.setSplicer(splicer);

        loadAndRun(trigMgr, 10, 10);

        assertEquals("Bad number of payloads written",
                     100, outProc.getNumberWritten());

        trigMgr.reset();

        checkLogMessages();
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
