package icecube.daq.trigger.control;

import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.StrandTail;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockPayloadDestination;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockSplicer;
import icecube.daq.trigger.test.MockTrigger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class DummyTriggerManagerTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public DummyTriggerManagerTest(String name)
    {
        super(name);
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
        return new TestSuite(DummyTriggerManagerTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void runWithRealSplicer(DummyTriggerManager trigMgr)
        throws SplicerException
    {
        final int numTails = 10;
        final int numObjs = numTails * 10;
        final int numHitsPerTrigger = 8;

        trigMgr.setNumHitsPerTrigger(numHitsPerTrigger);

        HKN1Splicer splicer = new HKN1Splicer(trigMgr);
        trigMgr.setSplicer(splicer);

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadOutput(dest);

        StrandTail[] tails = new StrandTail[numTails];
        for (int i = 0; i < tails.length; i++) {
            tails[i] = splicer.beginStrand();
        }

        splicer.start();

        for (int i = 0; i < numObjs; i++) {
            tails[ i % numTails].push(new MockHit((long) (i + 1) * 10000L));
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

        assertEquals("Bad number of payloads written",
                     13, dest.getNumberWritten());

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

    public void testRealSplicer()
        throws SplicerException
    {
        DummyTriggerManager trigMgr = new DummyTriggerManager();

        trigMgr.setOutputFactory(new TriggerRequestPayloadFactory());

        runWithRealSplicer(trigMgr);
    }

/*
    public void testThreadedSplicer()
        throws SplicerException
    {
        DummyTriggerManager trigMgr = new DummyTriggerManager();
        trigMgr.enableThread();
        runWithRealSplicer(trigMgr);
    }
*/

    public void testMockSplicer()
        throws SplicerException
    {
        final int numTails = 10;
        final int numObjs = numTails * 10;
        final int numHitsPerTrigger = 8;

        DummyTriggerManager trigMgr = new DummyTriggerManager();
        trigMgr.setNumHitsPerTrigger(numHitsPerTrigger);

        MockSplicer splicer = new MockSplicer(trigMgr);
        trigMgr.setSplicer(splicer);

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadOutput(dest);

        trigMgr.setOutputFactory(new TriggerRequestPayloadFactory());

        StrandTail[] tails = new StrandTail[numTails];
        for (int i = 0; i < tails.length; i++) {
            tails[i] = splicer.beginStrand();
        }

        splicer.start();

        MockHit hit = null;
        for (int i = 0; i < numObjs; i++) {
            hit = new MockHit((long) i * 10000L);
            tails[ i % numTails].push(hit);
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

        assertEquals("Bad number of payloads written",
                     13, dest.getNumberWritten());

        trigMgr.reset();
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
