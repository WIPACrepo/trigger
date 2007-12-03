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

public class DummyTriggerHandlerTest
    extends TestCase
{
    private static final MockAppender appender = new MockAppender();

    public DummyTriggerHandlerTest(String name)
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
        return new TestSuite(DummyTriggerHandlerTest.class);
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

        DummyTriggerHandler trigMgr = new DummyTriggerHandler(srcId, factory);
        assertNull("Monitor should be null", trigMgr.getMonitor());
        assertEquals("Count should be zero", 0, trigMgr.getCount());
        assertEquals("Unexpected source ID", srcId, trigMgr.getSourceID());
    }

    public void testAddTrigger()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();
        trigMgr.addTrigger(new MockTrigger());
    }

    public void testAddTriggers()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        ArrayList list = new ArrayList();
        list.add(new MockTrigger());
        list.add(new MockTrigger());

        trigMgr.addTriggers(list);
    }

    public void testClearTriggers()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        trigMgr.clearTriggers();
    }

    public void testIssueNoDest()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

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
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadOutput(dest);

        trigMgr.issueTriggers();
        assertEquals("Bad number of payloads written",
                    0, dest.getNumberWritten());
    }

    public void testIssueException()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadOutput(dest);

        dest.setWritePayloadException(new IOException("Test"));

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
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadOutput(dest);

        trigMgr.addToTriggerBag(new MockTriggerRequest(100000L, 111111L, 0, 0));

        trigMgr.issueTriggers();
        assertEquals("Bad number of payloads written",
                     1, dest.getNumberWritten());
    }

    public void testFlushEmpty()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        trigMgr.flush();
    }

    public void testFlushOne()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadOutput(dest);

        trigMgr.addToTriggerBag(new MockTriggerRequest(100000L, 111111L, 0, 0));

        trigMgr.flush();
    }

    public void testProcessHit()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadOutput(dest);

        MockHit hit = new MockHit(345678L);

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getCount());

        trigMgr.process(hit);
        assertEquals("Bad number of input payloads",
                     1, trigMgr.getCount());
        assertEquals("Bad number of triggers written",
                     1, dest.getNumberWritten());
    }

    public void testProcessManyHitsAndReset()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadOutput(dest);

        final int numHitsPerTrigger = 6;
        trigMgr.setNumHitsPerTrigger(numHitsPerTrigger);

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getCount());

        for (int i = 0; i < numHitsPerTrigger * 4; i++) {
            MockHit hit = new MockHit(100000L + (10000 * i));

            trigMgr.process(hit);
            assertEquals("Bad number of input payloads",
                         i + 1, trigMgr.getCount());
            assertEquals("Bad number of triggers written (" + (i + 1) +
                         " hits)",
                         (i / numHitsPerTrigger) + 1, dest.getNumberWritten());
        }

        trigMgr.reset();
        assertEquals("Bad number of input payloads",
                     0, trigMgr.getCount());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
