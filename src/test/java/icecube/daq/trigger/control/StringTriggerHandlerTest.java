package icecube.daq.trigger.control;

import icecube.daq.oldpayload.impl.MasterPayloadFactory;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockOutputChannel;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockPayload;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockTrigger;
import icecube.daq.util.DOMRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class StringTriggerHandlerTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;
    private static final MockSourceID srcId =
        new MockSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);

    private StringTriggerHandler trigMgr;

    private DOMRegistry domRegistry;

    public StringTriggerHandlerTest(String name)
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

        String configDir = getClass().getResource("/config/").getPath();

        String classCfgStr = "/classes/config/";
        if (configDir.endsWith(classCfgStr)) {
            int breakPt = configDir.length() - (classCfgStr.length() - 1);
            configDir = configDir.substring(0, breakPt) + "test-" +
                configDir.substring(breakPt);
        }

        domRegistry = DOMRegistry.loadRegistry(configDir);
        DomSetFactory.setDomRegistry(domRegistry);
    }

    public static Test suite()
    {
        return new TestSuite(StringTriggerHandlerTest.class);
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

        trigMgr = new StringTriggerHandler(srcId, factory);
        assertNotNull("Monitor should not be null", trigMgr.getMonitor());
        assertEquals("Unexpected count difference",
                     0, trigMgr.getMonitor().getTriggerBagCountDifference());
        assertEquals("Count should be zero", 0, trigMgr.getCount());
        assertEquals("Unexpected source ID", srcId, trigMgr.getSourceID());

        List trigList = trigMgr.getTriggerList();
        assertNotNull("Trigger list should be initialized", trigList);
        assertEquals("Trigger list should be contain default trigger",
                     1, trigList.size());
    }

    public void testAddTrigger()
    {
        trigMgr = new StringTriggerHandler(srcId);
        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     2, trigMgr.getTriggerList().size());
    }

    public void testAddDuplicateTrigger()
    {
        trigMgr = new StringTriggerHandler(srcId);
        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     2, trigMgr.getTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     2, trigMgr.getTriggerList().size());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Attempt to add duplicate trigger to trigger list!",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testAddMultipleTriggers()
    {
        trigMgr = new StringTriggerHandler(srcId);
        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     2, trigMgr.getTriggerList().size());

        MockTrigger diffTrig = new MockTrigger();
        diffTrig.setTriggerType(2);

        trigMgr.addTrigger(diffTrig);
        assertEquals("Bad triggerList length",
                     3, trigMgr.getTriggerList().size());
    }

    public void testAddTriggers()
        throws Exception
    {
        trigMgr = new StringTriggerHandler(srcId);
        trigMgr.setDOMRegistry(domRegistry);

        ArrayList list = new ArrayList();
        list.add(new MockTrigger());
        list.add(new MockTrigger());

        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());
        trigMgr.addTriggers(list);
        assertEquals("Bad triggerList length",
                     3, trigMgr.getTriggerList().size());
    }

    public void testClearTriggers()
        throws Exception
    {
        trigMgr = new StringTriggerHandler(srcId);
        trigMgr.setDOMRegistry(domRegistry);

        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());
        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     2, trigMgr.getTriggerList().size());

        trigMgr.clearTriggers();
        assertEquals("Bad triggerList length",
                     1, trigMgr.getTriggerList().size());
    }

    public void testSetFactory()
    {
        trigMgr = new StringTriggerHandler(srcId);
        trigMgr.setMasterPayloadFactory(new MasterPayloadFactory());

        assertEquals("Expected to see a log message",
                     appender.getNumberOfMessages(), 1);
        assertEquals("Bad log message",
                     "This ITriggerBag does not use a PayloadFactory",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testIssueNoDest()
    {
        trigMgr = new StringTriggerHandler(srcId);

        try {
            trigMgr.issueTriggers();
            fail("issueTriggers() should not work without PayloadDestination");
        } catch (RuntimeException rte) {
            // expect this to fail
        }
    }

    public void testIssueEmpty()
    {
        trigMgr = new StringTriggerHandler(srcId);

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        trigMgr.issueTriggers();

        waitForOutput(trigMgr);
        waitForOutputThread(trigMgr);

        assertEquals("Bad number of payloads written",
                    0, outProc.getNumberWritten());
    }

    public void testIssueOne()
    {
        trigMgr = new StringTriggerHandler(srcId);

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        MockHit hit = new MockHit(234567L);

        MockTrigger trig = new MockTrigger();
        trig.setEarliestPayloadOfInterest(hit);

        trigMgr.addTrigger(trig);
        trigMgr.addToTriggerBag(new MockHit(100000L));

        trigMgr.issueTriggers();

        waitForOutput(trigMgr);
        waitForOutputThread(trigMgr);

        assertEquals("Bad number of payloads written",
                     1, outProc.getNumberWritten());
    }

    public void testFlushEmpty()
    {
        trigMgr = new StringTriggerHandler(srcId);

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        trigMgr.flush();
    }

    public void testFlushOne()
    {
        trigMgr = new StringTriggerHandler(srcId);

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        trigMgr.addToTriggerBag(new MockHit(100000L));

        trigMgr.flush();
    }

    public void testProcessHit()
    {
        trigMgr = new StringTriggerHandler(srcId);

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
                     0, trigMgr.getInputHandler().size());

        waitForOutput(trigMgr);
        waitForOutputThread(trigMgr);

        assertEquals("Bad number of triggers written",
                     1, outProc.getNumberWritten());
    }

    public void testProcessManyHitsAndReset()
        throws Exception
    {
        trigMgr = new StringTriggerHandler(srcId);
        trigMgr.setDOMRegistry(domRegistry);

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        final int numHitsPerTrigger = 6;

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getInputHandler().size());

        for (int i = 0; i < numHitsPerTrigger * 4; i++) {
            MockHit hit = new MockHit(100000L + (10000 * i), 1111L * i);

            trigMgr.process(hit);

            waitForProcessedPayloads(trigMgr);
            waitForMainThread(trigMgr);

            waitForOutput(trigMgr);
            waitForOutputThread(trigMgr);

            assertEquals("Bad number of input payloads",
                         0, trigMgr.getInputHandler().size());
            assertEquals("Bad number of triggers written (" + i + " hits)",
                         i + 1, outProc.getNumberWritten());
        }

        trigMgr.reset();
        assertEquals("Bad number of input payloads",
                     0, trigMgr.getInputHandler().size());
    }

    private static void waitForInput(TriggerHandler trigMgr)
    {
        ITriggerInput trigIn = trigMgr.getInputHandler();

        boolean success = false;
        for (int i = 0; i < 10; i++) {
            if (trigIn.size() > 0) {
                success = true;
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // ignore interrupts
            }
        }

        if (!success) {
            fail("No input");
        }
    }

    private static void waitForMainThread(TriggerHandler trigMgr)
    {
        boolean success = false;
        for (int i = 0; i < 10; i++) {
            if (trigMgr.isMainThreadWaiting()) {
                success = true;
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // ignore interrupts
            }
        }

        if (!success) {
            fail("Main thread is not waiting");
        }
    }

    private static void waitForOutput(TriggerHandler trigMgr)
    {
        boolean success = false;
        for (int i = 0; i < 10; i++) {
            final int numQueued = trigMgr.getNumOutputsQueued();
            if (numQueued == 0) {
                success = true;
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // ignore interrupts
            }
        }

        if (!success) {
            fail("No output");
        }
    }

    private static void waitForOutputThread(TriggerHandler trigMgr)
    {
        boolean success = false;
        for (int i = 0; i < 10; i++) {
            if (trigMgr.isOutputThreadWaiting()) {
                success = true;
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // ignore interrupts
            }
        }

        if (!success) {
            fail("Output thread is not waiting");
        }
    }

    private static void waitForProcessedPayloads(TriggerHandler trigMgr)
    {
        boolean success = false;
        for (int i = 0; i < 10; i++) {
            if (!trigMgr.hasUnprocessedPayloads()) {
                success = true;
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // ignore interrupts
            }
        }

        if (!success) {
            fail("There are still unprocessed payloads");
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
