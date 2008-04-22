package icecube.daq.trigger.control;

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockOutputChannel;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockPayload;
import icecube.daq.trigger.test.MockReadoutRequest;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockTrigger;
import icecube.daq.trigger.test.MockTriggerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

class GlobalTriggerTrigger
    extends MockTrigger
{
    public void runTrigger(IPayload payload)
        throws TriggerException
    {
        reportTrigger((ILoadablePayload) payload);
    }
}

class BadTriggerRequest
    extends MockTriggerRequest
{
    private int uid;

    public BadTriggerRequest(long firstVal, long lastVal, int type, int cfgId,
                             int uid)
    {
        super(firstVal, lastVal, type, cfgId);

        this.uid = uid;
    }

    public List getPayloads()
        throws IOException, DataFormatException
    {
        return null;
    }

    public int getUID()
    {
        return uid;
    }
}

public class GlobalTriggerHandlerTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public GlobalTriggerHandlerTest(String name)
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
        return new TestSuite(GlobalTriggerHandlerTest.class);
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

        GlobalTriggerHandler trigMgr =
            new GlobalTriggerHandler(srcId, false, factory);
        assertNotNull("Monitor should not be null", trigMgr.getMonitor());
        assertEquals("Unexpected count difference",
                     0, trigMgr.getMonitor().getTriggerBagCountDifference());
        assertEquals("Unexpected source ID", srcId, trigMgr.getSourceID());

        List trigList = trigMgr.getConfiguredTriggerList();
        assertNotNull("Trigger list should be initialized", trigList);
        assertEquals("Trigger list should be empty", 0, trigList.size());
    }

    public void testAddTrigger()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();
        assertEquals("Bad triggerList length",
                     0, trigMgr.getConfiguredTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     1, trigMgr.getConfiguredTriggerList().size());
    }

    public void testAddDuplicateTrigger()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();
        assertEquals("Bad triggerList length",
                     0, trigMgr.getConfiguredTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     1, trigMgr.getConfiguredTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     1, trigMgr.getConfiguredTriggerList().size());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Attempt to add duplicate trigger to trigger list!",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testAddMultipleTriggers()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();
        assertEquals("Bad triggerList length",
                     0, trigMgr.getConfiguredTriggerList().size());

        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     1, trigMgr.getConfiguredTriggerList().size());

        MockTrigger diffTrig = new MockTrigger();
        diffTrig.setTriggerType(2);

        trigMgr.addTrigger(diffTrig);
        assertEquals("Bad triggerList length",
                     2, trigMgr.getConfiguredTriggerList().size());
    }

    public void testAddTriggers()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();

        ArrayList list = new ArrayList();
        list.add(new MockTrigger());
        list.add(new MockTrigger());

        assertEquals("Bad triggerList length",
                     0, trigMgr.getConfiguredTriggerList().size());
        trigMgr.addTriggers(list);
        assertEquals("Bad triggerList length",
                     2, trigMgr.getConfiguredTriggerList().size());
    }

    public void testClearTriggers()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();

        assertEquals("Bad triggerList length",
                     0, trigMgr.getConfiguredTriggerList().size());
        trigMgr.addTrigger(new MockTrigger());
        assertEquals("Bad triggerList length",
                     1, trigMgr.getConfiguredTriggerList().size());

        trigMgr.clearTriggers();
        assertEquals("Bad triggerList length",
                     0, trigMgr.getConfiguredTriggerList().size());
    }

    public void testIssueNoDest()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();

        try {
            trigMgr.issueTriggers();
            fail("issueTriggers() should not work without PayloadDestination");
        } catch (RuntimeException rte) {
            // expect this to fail
        }
    }

    public void testIssueEmpty()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        trigMgr.issueTriggers();
        assertEquals("Bad number of payloads written",
                    0, outProc.getNumberWritten());
    }

    public void testIssueOne()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        MockHit hit = new MockHit(123456L);

        MockTrigger trig = new MockTrigger();
        trig.setEarliestPayloadOfInterest(hit);
        trigMgr.addTrigger(trig);

        MockTriggerRequest tr = new MockTriggerRequest(10000L, 11111L, 0, 0);
        tr.setSourceID(666);
        tr.setReadoutRequest(new MockReadoutRequest());
        trigMgr.addToTriggerBag(tr);

        trigMgr.issueTriggers();
        assertEquals("Bad number of payloads written",
                     1, outProc.getNumberWritten());
    }

    public void testFlushEmpty()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        trigMgr.flush();
    }

    public void testFlush()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        MockTrigger trig = new MockTrigger();
        //trig.setEarliestPayloadOfInterest(hit);
        trigMgr.addTrigger(trig);

        MockTriggerRequest tr = new MockTriggerRequest(10000L, 11111L, 0, 0);
        tr.setSourceID(666);
        tr.setReadoutRequest(new MockReadoutRequest());
        trigMgr.addToTriggerBag(tr);

        trigMgr.flush();
        assertEquals("Bad number of payloads written",
                     1, outProc.getNumberWritten());
    }

    public void testProcessNonTrigger()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        GlobalTriggerTrigger trig = new GlobalTriggerTrigger();
        trig.setEarliestPayloadOfInterest(new MockHit(999L));
        trigMgr.addTrigger(trig);

        MockTriggerRequest bagReq =
            new MockTriggerRequest(10000L, 11111L, 0, 0);
        bagReq.setSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);
        bagReq.setReadoutRequest(new MockReadoutRequest());
        trigMgr.addToTriggerBag(bagReq);
        assertEquals("Unexpected count difference",
                     1, trigMgr.getMonitor().getTriggerBagCountDifference());

        trigMgr.process(new MockHit(12345L));
        assertEquals("Unexpected non-trigger payloads",
                     0, trigMgr.getTotalNonTRPInputTriggers());

        MockTriggerRequest trigReq;

        trigReq = new MockTriggerRequest(20000L, 29999L, 1, 11);
        trigReq.setSourceID(SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID);
        trigReq.setReadoutRequest(new MockReadoutRequest());
        trigMgr.process(trigReq);
        assertEquals("Expected non-trigger payload",
                     1, trigMgr.getTotalNonTRPInputTriggers());
    }

    public void testProcessSimpleTrigger()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        GlobalTriggerTrigger trig = new GlobalTriggerTrigger();
        trig.setEarliestPayloadOfInterest(new MockHit(999L));
        trigMgr.addTrigger(trig);

        MockTriggerRequest bagReq =
            new MockTriggerRequest(10000L, 11111L, 0, 0);
        bagReq.setSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);
        bagReq.setReadoutRequest(new MockReadoutRequest());
        trigMgr.addToTriggerBag(bagReq);
        assertEquals("Unexpected count difference",
                     1, trigMgr.getMonitor().getTriggerBagCountDifference());

        MockTriggerRequest trigReq;

        trigReq = new MockTriggerRequest(20000L, 29999L, 1, 11);
        trigReq.setSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);
        trigReq.setReadoutRequest(new MockReadoutRequest());
        trigReq.addPayload(new MockTriggerRequest(20001L, 21000L));
        trigMgr.process(trigReq);

        assertEquals("Unexpected count difference",
                     1, trigMgr.getMonitor().getTriggerBagCountDifference());
        assertEquals("Unexpected count total",
                     0, trigMgr.getMonitor().getTriggerBagCountTotal());
        assertEquals("Bad number of triggers written",
                     0, outProc.getNumberWritten());

        trigReq = new MockTriggerRequest(30000L, 39999L, 2, 22);
        trigReq.setSourceID(SourceIdRegistry.ICETOP_TRIGGER_SOURCE_ID);
        trigReq.setReadoutRequest(new MockReadoutRequest());
        trigMgr.process(trigReq);

        assertEquals("Unexpected count difference",
                     2, trigMgr.getMonitor().getTriggerBagCountDifference());
        assertEquals("Unexpected count total",
                     0, trigMgr.getMonitor().getTriggerBagCountTotal());
        assertEquals("Bad number of triggers written",
                     0, outProc.getNumberWritten());
    }

    public void testProcessBadMergedTrigger()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        GlobalTriggerTrigger trig = new GlobalTriggerTrigger();
        trig.setEarliestPayloadOfInterest(new MockHit(999L));
        trigMgr.addTrigger(trig);

        MockTriggerRequest bagReq =
            new MockTriggerRequest(10000L, 11111L, 0, 0);
        bagReq.setSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);
        bagReq.setReadoutRequest(new MockReadoutRequest());
        trigMgr.addToTriggerBag(bagReq);
        assertEquals("Unexpected count difference",
                     1, trigMgr.getMonitor().getTriggerBagCountDifference());

        MockTriggerRequest trigReq;

        final long badFirstTime = 20000L;
        final long badLastTime = 29999L;
        final int badCfgId = 11;
        final int badUID = 123;
        final int badSrcId = SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID;
        final String badSrcName =
            SourceIdRegistry.getDAQNameFromSourceID(badSrcId) + "#" +
            SourceIdRegistry.getDAQIdFromSourceID(badSrcId);

        trigReq = new BadTriggerRequest(badFirstTime, badLastTime, -1,
                                        badCfgId, badUID);
        trigReq.setSourceID(badSrcId);
        trigReq.setReadoutRequest(new MockReadoutRequest());
        trigMgr.process(trigReq);

        assertEquals("Unexpected count difference",
                     1, trigMgr.getMonitor().getTriggerBagCountDifference());
        assertEquals("Unexpected count total",
                     0, trigMgr.getMonitor().getTriggerBagCountTotal());
        assertEquals("Bad number of triggers written",
                     0, outProc.getNumberWritten());

        trigReq = new MockTriggerRequest(30000L, 39999L, -1, 11);
        trigReq.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        trigReq.setReadoutRequest(new MockReadoutRequest());
        trigMgr.process(trigReq);

        assertEquals("Unexpected count difference",
                     1, trigMgr.getMonitor().getTriggerBagCountDifference());
        assertEquals("Unexpected count total",
                     0, trigMgr.getMonitor().getTriggerBagCountTotal());
        assertEquals("Bad number of triggers written",
                     0, outProc.getNumberWritten());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Bad merged trigger: uid " + badUID + " configId " +
                     badCfgId + " src " + badSrcName + " times [" +
                     badFirstTime + "-" + badLastTime + "]",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testProcessMergedTrigger()
    {
        GlobalTriggerHandler trigMgr = new GlobalTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setPayloadOutput(outProc);

        GlobalTriggerTrigger trig = new GlobalTriggerTrigger();
        trig.setEarliestPayloadOfInterest(new MockHit(999L));
        trigMgr.addTrigger(trig);

        MockTriggerRequest bagReq =
            new MockTriggerRequest(10000L, 11111L, 0, 0);
        bagReq.setSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);
        bagReq.setReadoutRequest(new MockReadoutRequest());
        trigMgr.addToTriggerBag(bagReq);
        assertEquals("Unexpected count difference",
                     1, trigMgr.getMonitor().getTriggerBagCountDifference());

        MockTriggerRequest trigReq;

        trigReq = new MockTriggerRequest(20000L, 29999L, -1, 11);
        trigReq.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        trigReq.setReadoutRequest(new MockReadoutRequest());
        trigReq.addPayload(new MockTriggerRequest(20001L, 21000L));
        trigMgr.process(trigReq);

        assertEquals("Unexpected count difference",
                     1, trigMgr.getMonitor().getTriggerBagCountDifference());
        assertEquals("Unexpected count total",
                     0, trigMgr.getMonitor().getTriggerBagCountTotal());
        assertEquals("Bad number of triggers written",
                     0, outProc.getNumberWritten());

        trigReq = new MockTriggerRequest(30000L, 39999L, -1, 11);
        trigReq.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        trigReq.setReadoutRequest(new MockReadoutRequest());
        trigMgr.process(trigReq);

        assertEquals("Unexpected count difference",
                     2, trigMgr.getMonitor().getTriggerBagCountDifference());
        assertEquals("Unexpected count total",
                     0, trigMgr.getMonitor().getTriggerBagCountTotal());
        assertEquals("Bad number of triggers written",
                     0, outProc.getNumberWritten());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
