package icecube.daq.trigger.control;

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.SourceIdRegistry;

import icecube.daq.trigger.exceptions.TriggerException;

import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;

import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockPayload;
import icecube.daq.trigger.test.MockPayloadDestination;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockTrigger;
import icecube.daq.trigger.test.MockTriggerRequest;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

class TriggerTrigger
    extends MockTrigger
{
    public void runTrigger(IPayload payload)
        throws TriggerException
    {
        reportTrigger((ILoadablePayload) payload);
    }
}

public class TriggerHandlerTest
    extends TestCase
{
    private static final MockAppender appender = new MockAppender();

    public TriggerHandlerTest(String name)
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
        return new TestSuite(TriggerHandlerTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testCreate()
    {
        TriggerRequestPayloadFactory factory =
            new TriggerRequestPayloadFactory();

        MockSourceID srcId =
            new MockSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);

        TriggerHandler trigMgr = new TriggerHandler(srcId, factory);
        assertNotNull("Monitor should not be null", trigMgr.getMonitor());
        assertEquals("Unexpected count difference",
                     0, trigMgr.getMonitor().getTriggerBagCountDifference());
        assertEquals("Count should be zero", 0, trigMgr.getCount());
        assertEquals("Unexpected source ID", srcId, trigMgr.getSourceID());

        List trigList = trigMgr.getTriggerList();
        assertNotNull("Trigger list should be initialized", trigList);
        assertEquals("Trigger list should be empty", 0, trigList.size());
    }

    public void testAddTrigger()
    {
        TriggerHandler trigMgr = new TriggerHandler();
        assertEquals("Bad triggerList length",
                     0, trigMgr.getTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());
    }

    public void testAddDuplicateTrigger()
    {
        TriggerHandler trigMgr = new TriggerHandler();
        assertEquals("Bad triggerList length",
                     0, trigMgr.getTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Attempt to add duplicate trigger to trigger list!",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testAddMultipleTriggers()
    {
        TriggerHandler trigMgr = new TriggerHandler();
        assertEquals("Bad triggerList length",
                     0, trigMgr.getTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());

        MockTrigger diffTrig = new MockTrigger();
        diffTrig.setTriggerType(2);

        trigMgr.addTrigger(diffTrig);
        assertEquals("Bad triggerList length",
                     2, trigMgr.getTriggerList().size());
    }

    public void testAddTriggers()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        ArrayList list = new ArrayList();
        list.add(new MockTrigger());
        list.add(new MockTrigger());

        assertEquals("Bad triggerList length",
                     0, trigMgr.getTriggerList().size());
        trigMgr.addTriggers(list);
        assertEquals("Bad triggerList length",
                     2, trigMgr.getTriggerList().size());
    }

    public void testClearTriggers()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        assertEquals("Bad triggerList length",
                     0, trigMgr.getTriggerList().size());
        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());

        trigMgr.clearTriggers();
        assertEquals("Bad triggerList length",
                     0, trigMgr.getTriggerList().size());
    }

    public void testIssueNoDest()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        try {
            trigMgr.issueTriggers();
            fail("issueTriggers() should not work without PayloadDestination");
        } catch (RuntimeException rte) {
            // expect this to fail
        }

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        appender.clear();
    }

    public void testIssueEmpty()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadDestinationCollection(dest);

        trigMgr.issueTriggers();
        assertEquals("Bad number of payloads written",
                    0, dest.getNumberWritten());
    }

    public void testIssueException()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadDestinationCollection(dest);

        dest.setWritePayloadException(new IOException("Test"));

        MockHit hit = new MockHit(123456L);

        MockTrigger trig = new MockTrigger();
        trig.setEarliestPayloadOfInterest(hit);

        trigMgr.addTrigger(trig);
        trigMgr.addToTriggerBag(new MockTriggerRequest(100000L, 111111L, 0, 0));

        try {
            trigMgr.issueTriggers();
            fail("issueTriggers() should have gotten an exception");
        } catch (RuntimeException rte) {
            // expect this to fail
        }
        assertEquals("Bad number of payloads written",
                     0, dest.getNumberWritten());

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        appender.clear();
    }

    public void testIssueOne()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadDestinationCollection(dest);

        MockHit hit = new MockHit(123456L);

        MockTrigger trig = new MockTrigger();
        trig.setEarliestPayloadOfInterest(hit);

        trigMgr.addTrigger(trig);
        trigMgr.addToTriggerBag(new MockTriggerRequest(100000L, 111111L, 0, 0));

        trigMgr.issueTriggers();
        assertEquals("Bad number of payloads written",
                     1, dest.getNumberWritten());
    }

    public void testFlushEmpty()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadDestinationCollection(dest);

        trigMgr.flush();
    }

    public void testFlushOne()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadDestinationCollection(dest);

        MockHit hit = new MockHit(234567L);

        MockTrigger trig = new MockTrigger();
        trig.setEarliestPayloadOfInterest(hit);

        trigMgr.addTrigger(trig);
        trigMgr.addToTriggerBag(new MockTriggerRequest(100000L, 111111L, 0, 0));

        trigMgr.flush();
    }

    public void testProcessHit()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadDestinationCollection(dest);

        MockHit hit = new MockHit(345678L);

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getInputHandler().size());

        trigMgr.process(hit);
        assertEquals("Bad number of input payloads",
                     1, trigMgr.getInputHandler().size());
        assertEquals("Bad number of triggers written",
                     0, dest.getNumberWritten());
    }

    public void testProcessManyHitsAndReset()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadDestinationCollection(dest);

        final int numHitsPerTrigger = 6;

        MockTrigger trig = new MockTrigger(numHitsPerTrigger);
        trigMgr.addTrigger(trig);

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getInputHandler().size());

        for (int i = 0; i < numHitsPerTrigger * 4; i++) {
            MockHit hit = new MockHit(100000L + (10000 * i));

            trigMgr.process(hit);
            assertEquals("Bad number of input payloads",
                         1, trigMgr.getInputHandler().size());
            assertEquals("Bad number of triggers written (" + i + " hits)",
                         (i / numHitsPerTrigger), dest.getNumberWritten());
        }

        trigMgr.reset();
        assertEquals("Bad number of input payloads",
                     0, trigMgr.getInputHandler().size());
    }

    public void testProcessTrigger()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadDestinationCollection(dest);

        MockTriggerRequest trigReq =
            new MockTriggerRequest(10000L, 20000L, 1, 11);

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getInputHandler().size());

        trigMgr.process(trigReq);
        assertEquals("Bad number of input payloads",
                     1, trigMgr.getInputHandler().size());
    }

    public void testProcessManyTriggers()
    {
        TriggerHandler trigMgr = new TriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadDestinationCollection(dest);

        TriggerTrigger trig = new TriggerTrigger();
        trig.setEarliestPayloadOfInterest(new MockHit(234567L));

        trigMgr.addTrigger(trig);
        trigMgr.addToTriggerBag(new MockTriggerRequest(100000L, 111111L, 0, 0));

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getCount());
        assertEquals("Unexpected count difference",
                     1, trigMgr.getMonitor().getTriggerBagCountDifference());

        for (int i = 0; i < 10; i++) {
            MockTriggerRequest trigReq =
                new MockTriggerRequest(10000L * (long) (i + 1),
                                       (10000L * (long) (i + 1)) + 9999L,
                                       1, 11);
            trigReq.setSourceID(SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID);
            trigMgr.process(trigReq);

            assertEquals("Bad number of input payloads (" + (i + 1) + " hits)",
                         i, trigMgr.getCount());
            assertEquals("Unexpected count difference (" + (i + 1) + " hits)",
                         0, trigMgr.getMonitor().getTriggerBagCountDifference());
            assertEquals("Unexpected count total (" + (i + 1) + " hits)",
                         i + 1, trigMgr.getMonitor().getTriggerBagCountTotal());
            assertEquals("Bad number of triggers written (" + i + " hits)",
                         i + 1, dest.getNumberWritten());
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
