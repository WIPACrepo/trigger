package icecube.daq.trigger.control;

import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.control.ITriggerInput;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.monitor.TriggerHandlerMonitor;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockOutputChannel;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockPayload;
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

    private TriggerHandler trigMgr;

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

        if (trigMgr != null) {
            trigMgr.stopThread();
        }

        super.tearDown();
    }

    public void testCreate()
    {
        TriggerRequestPayloadFactory factory =
            new TriggerRequestPayloadFactory();

        MockSourceID srcId =
            new MockSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);

        trigMgr = new TriggerHandler(srcId, factory);
        assertNotNull("Monitor should not be null", trigMgr.getMonitor());
        assertEquals("Unexpected count difference",
                     0, trigMgr.getMonitor().getTriggerBagCountDifference());
        assertEquals("Count should be zero", 0, trigMgr.getCount());
        assertEquals("Unexpected source ID", srcId, trigMgr.getSourceID());

        List trigList = trigMgr.getTriggerList();
        assertNotNull("Trigger list should be initialized", trigList);
        assertEquals("Trigger list should be empty", 0, trigList.size());

        trigMgr.stopThread();
    }

    public void testAddTrigger()
    {
        trigMgr = new TriggerHandler();
        assertEquals("Bad triggerList length",
                     0, trigMgr.getTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());
    }

    public void testAddDuplicateTrigger()
    {
        trigMgr = new TriggerHandler();
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
        trigMgr = new TriggerHandler();
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
        trigMgr = new TriggerHandler();

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
        trigMgr = new TriggerHandler();

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
        trigMgr = new TriggerHandler();

        try {
            trigMgr.issueTriggers();
            fail("issueTriggers() should not work without PayloadOutput");
        } catch (RuntimeException rte) {
            // expect this to fail
        }
    }

    public void testIssueEmpty()
    {
        trigMgr = new TriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        trigMgr.setPayloadOutput(outProc);

        trigMgr.issueTriggers();
        assertEquals("Bad number of payloads written",
                    0, outProc.getNumberWritten());
    }

    public void testIssueOne()
    {
        trigMgr = new TriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        MockHit hit = new MockHit(123456L);

        MockTrigger trig = new MockTrigger();
        trig.setEarliestPayloadOfInterest(hit);

        trigMgr.addTrigger(trig);
        trigMgr.addToTriggerBag(new MockTriggerRequest(100000L, 111111L, 0, 0));

        trigMgr.issueTriggers();

        waitForOutput(trigMgr);
        waitForOutputThread(trigMgr);

        assertEquals("Bad number of payloads written",
                     1, outProc.getNumberWritten());
    }

    public void testFlushEmpty()
    {
        trigMgr = new TriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        trigMgr.flush();
    }

    public void testFlushOne()
    {
        trigMgr = new TriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        MockHit hit = new MockHit(234567L);

        MockTrigger trig = new MockTrigger();
        trig.setEarliestPayloadOfInterest(hit);

        trigMgr.addTrigger(trig);
        trigMgr.addToTriggerBag(new MockTriggerRequest(100000L, 111111L, 0, 0));

        trigMgr.flush();

        waitForOutput(trigMgr);
        waitForOutputThread(trigMgr);

        assertEquals("Bad number of payloads written",
                     1, outProc.getNumberWritten());
    }

    public void testProcessHit()
    {
        trigMgr = new TriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        MockHit hit = new MockHit(345678L);

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getInputHandler().size());

        trigMgr.process(hit);

        waitForProcessedPayloads(trigMgr);
        waitForMainThread(trigMgr);

        assertEquals("Bad number of input payloads",
                     1, trigMgr.getInputHandler().size());
        assertEquals("Bad number of triggers written",
                     0, outProc.getNumberWritten());

        trigMgr.stopThread();
    }

    public void testProcessManyHitsAndReset()
    {
        trigMgr = new TriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        final int numHitsPerTrigger = 6;

        MockTrigger trig = new MockTrigger(numHitsPerTrigger);
        trigMgr.addTrigger(trig);

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getInputHandler().size());

        for (int i = 0; i < numHitsPerTrigger * 4; i++) {
            MockHit hit = new MockHit(100000L + (10000 * i));

            trigMgr.process(hit);

            waitForProcessedPayloads(trigMgr);
            waitForMainThread(trigMgr);

            assertEquals("Bad number of input payloads",
                         1, trigMgr.getInputHandler().size());
        }

        trigMgr.flush();

        waitForOutput(trigMgr);
        waitForOutputThread(trigMgr);

        assertEquals("Bad number of triggers written",
                     4, outProc.getNumberWritten());

        trigMgr.reset();
        assertEquals("Bad number of input payloads",
                     0, trigMgr.getInputHandler().size());
    }

    public void testProcessTrigger()
    {
        trigMgr = new TriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        MockTriggerRequest trigReq =
            new MockTriggerRequest(10000L, 20000L, 1, 11);

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getInputHandler().size());

        trigMgr.process(trigReq);

        waitForProcessedPayloads(trigMgr);
        waitForMainThread(trigMgr);

        assertEquals("Bad number of input payloads",
                     1, trigMgr.getInputHandler().size());
    }

    public void testProcessManyTriggers()
    {
        trigMgr = new TriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        TriggerTrigger trig = new TriggerTrigger();
        trig.setEarliestPayloadOfInterest(new MockHit(234567L));

        trigMgr.addTrigger(trig);
        trigMgr.addToTriggerBag(new MockTriggerRequest(100000L, 111111L, 0, 0));

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getCount());
        assertEquals("Unexpected count difference",
                     1, trigMgr.getMonitor().getTriggerBagCountDifference());

        final int numTriggers = 10;

        for (int i = 0; i < numTriggers; i++) {
            MockTriggerRequest trigReq =
                new MockTriggerRequest(10000L * (long) (i + 1),
                                       (10000L * (long) (i + 1)) + 9999L,
                                       1, 11);
            trigReq.setSourceID(SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID);
            trigMgr.process(trigReq);

            waitForCount(trigMgr, i);

            assertEquals("Bad number of input payloads (" + (i + 1) + " hits)",
                         i, trigMgr.getCount());

            waitForProcessedPayloads(trigMgr);
            waitForMainThread(trigMgr);

            assertEquals("Unexpected count difference (" + (i + 1) + " hits)",
                         0, trigMgr.getMonitor().getTriggerBagCountDifference());
        }

        trigMgr.flush();

        waitForOutput(trigMgr);
        waitForOutputThread(trigMgr);

        assertEquals("Unexpected count total",
                     numTriggers + 1,
                     trigMgr.getMonitor().getTriggerBagCountTotal());
        assertEquals("Bad number of triggers written",
                     numTriggers + 1, outProc.getNumberWritten());
    }

    private static void waitForCount(TriggerHandler trigMgr, int count)
    {
        for (int i = 0; i < 10; i++) {
            if (trigMgr.getCount() >= count) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // ignore interrupts
            }
        }
    }

    private static void waitForMainThread(TriggerHandler trigMgr)
    {
        for (int i = 0; i < 10; i++) {
            if (trigMgr.isMainThreadWaiting()) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // ignore interrupts
            }
        }
    }

    private static void waitForOutput(TriggerHandler trigMgr)
    {
        for (int i = 0; i < 10; i++) {
            if (trigMgr.getNumOutputsQueued() == 0) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // ignore interrupts
            }
        }
    }

    private static void waitForOutputThread(TriggerHandler trigMgr)
    {
        for (int i = 0; i < 10; i++) {
            if (trigMgr.isOutputThreadWaiting()) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // ignore interrupts
            }
        }
    }

    private static void waitForProcessedPayloads(TriggerHandler trigMgr)
    {
        for (int i = 0; i < 10; i++) {
            if (!trigMgr.hasUnprocessedPayloads()) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // ignore interrupts
            }
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
