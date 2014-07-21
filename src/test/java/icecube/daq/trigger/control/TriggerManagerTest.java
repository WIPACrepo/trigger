package icecube.daq.trigger.control;

import icecube.daq.juggler.alert.Alerter.Priority;
import icecube.daq.payload.IDOMID;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.SplicerChangedEvent;
import icecube.daq.trigger.common.ITriggerAlgorithm;
import icecube.daq.trigger.common.ITriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.test.MockAlgorithm;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockBufferCache;
import icecube.daq.trigger.test.MockOutputChannel;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockPayload;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockSplicer;
import icecube.daq.trigger.test.MockTriggerRequest;
import icecube.daq.trigger.test.MockUTCTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

class MockOldAlgorithm
    implements ITriggerAlgorithm
{
    private String name;

    public MockOldAlgorithm(String name)
    {
        this.name = name;
    }

    public IPayload getEarliestPayloadOfInterest()
    {
        throw new Error("Unimplemented");
    }

    public int getSourceId()
    {
        throw new Error("Unimplemented");
    }

    public int getTriggerConfigId()
    {
        throw new Error("Unimplemented");
    }

    public int getTriggerCounter()
    {
        throw new Error("Unimplemented");
    }

    public Map getTriggerMonitorMap()
    {
        throw new Error("Unimplemented");
    }

    public String getTriggerName()
    {
        return name;
    }

    public int getTriggerType()
    {
        throw new Error("Unimplemented");
    }

    public boolean isConfigured()
    {
        throw new Error("Unimplemented");
    }

    public void runTrigger(IPayload pay)
        throws TriggerException
    {
        throw new Error("Unimplemented");
    }

    public void setSourceId(int srcId)
    {
        throw new Error("Unimplemented");
    }

    public void setTriggerConfigId(int cfgId)
    {
        throw new Error("Unimplemented");
    }

    public void setTriggerManager(ITriggerManager mgr)
    {
        // do nothing
    }

    public void setTriggerName(String name)
    {
        throw new Error("Unimplemented");
    }

    public void setTriggerType(int type)
    {
        throw new Error("Unimplemented");
    }

    public String toString()
    {
        return "OldAlgo[" + name + "]";
    }
}

public class TriggerManagerTest
{
    private static final int INICE_ID =
        SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;
    private static final int GLOBAL_ID =
        SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID;

    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    @Before
    public void setUp()
        throws Exception
    {
        appender.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);
    }

    @After
    public void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());
    }

    @Test
    public void testSimple()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);
        assertEquals("Bad source ID", src.getSourceID(), mgr.getSourceId());

        assertNull("Registry should be null", mgr.getDOMRegistry());
        assertEquals("Bad count", 0L, mgr.getCount());
        assertEquals("Bad number of inputs queued",
                     0, mgr.getNumInputsQueued());
        assertEquals("Bad number of outputs queued",
                     0, mgr.getNumOutputsQueued());
        assertEquals("Bad total processed",
                     0L, mgr.getTotalProcessed());
    }

    @Test
    public void testAddAlgoBad()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        MockOldAlgorithm bad = new MockOldAlgorithm("addAlgoBad");
        try {
            mgr.addTrigger(bad);
            fail("This should not succeed");
        } catch (Error err) {
            assertNotNull("Error message should not be null",
                          err.getMessage());
            final String msg = "Algorithm " + bad +
                " must implement INewAlgorithm";
            assertEquals("Bad message", msg, err.getMessage());
        }
    }

    @Test
    public void testAddAlgo1()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        List<ITriggerAlgorithm> list = new ArrayList<ITriggerAlgorithm>();
        list.add(new MockAlgorithm("addAlgo1"));

        mgr.addTriggers(list);
    }

    @Test
    public void testAddAlgoMany()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        List<ITriggerAlgorithm> list = new ArrayList<ITriggerAlgorithm>();
        for (int t = 10; t < 40; t += 10) {
            for (int c = 11; c < 40; c += 10) {
                for (int s = 12; s < 40; s += 10) {
                    list.add(new MockAlgorithm("algo1", t, c, s));
                }
            }
        }

        mgr.addTriggers(list);
    }

    @Test
    public void testAddAlgoDup()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        MockAlgorithm algo = new MockAlgorithm("dup", 1, 2, 3);

        List<ITriggerAlgorithm> list = new ArrayList<ITriggerAlgorithm>();
        list.add(algo);
        list.add(algo);

        mgr.addTriggers(list);

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg =
            String.format("Attempt to add duplicate trigger with type %d" +
                          " cfgId %d srcId %d (old %s, new %s)",
                          algo.getTriggerType(), algo.getTriggerConfigId(),
                          algo.getSourceId(), algo.getTriggerName(),
                          algo.getTriggerName());
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testExecuteEmpty()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.execute(new ArrayList(), 0);
    }

    @Test
    public void testExecuteNoSub()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        List splObjs = new ArrayList();
        splObjs.add(new MyHit(123, 456));

        try {
            mgr.execute(splObjs, 0);
            fail("Should not succeed");
        } catch (Error err) {
            assertNotNull("Message should not be null", err.getMessage());

            final String msg = "No subscribers have been added";
            assertEquals("Unexpected error message", msg, err.getMessage());
        }
    }

    @Test
    public void testExecuteBad()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAlgorithms();

        List splObjs = new ArrayList();
        splObjs.add(new NonHit(123, 456));

        mgr.execute(splObjs, 0);

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "TriggerHandler only knows about either" +
            " HitPayloads or TriggerRequestPayloads!";
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testExecuteHitOutOfOrder()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAlgorithms();

        List splObjs = new ArrayList();
        splObjs.add(new MyHit(123, 234));

        mgr.execute(splObjs, 0);

        MyHit goodOrder = new MyHit(123, 345);
        splObjs.add(goodOrder);

        MyHit badOrder = new MyHit(321, 246);
        splObjs.add(badOrder);

        mgr.execute(splObjs, 0);

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final double diff = (badOrder.getUTCTime() - goodOrder.getUTCTime());
        final String msg = "Hit from " + badOrder.getSourceID() +
            " out of order! This time - Last time = " + diff +
            ", src of last hit = " + goodOrder.getSourceID();
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testExecuteHits()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAlgorithms();

        List splObjs = new ArrayList();
        splObjs.add(new MyHit(123, 234));

        mgr.execute(splObjs, 0);

        splObjs.add(new MyHit(123, 246));
        splObjs.add(new MyHit(321, 345));

        mgr.execute(splObjs, 0);
    }

    @Test
    public void testExecuteTrigReqBadComp()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAlgorithms();

        List splObjs = new ArrayList();
        splObjs.add(new MockTriggerRequest(1, 2, 3, 4, 5));

        mgr.execute(splObjs, 0);

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "Source #" + INICE_ID +
            " cannot process trigger requests";
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testExecuteTrigReqsNoMerged()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAlgorithms();

        List splObjs = new ArrayList();
        splObjs.add(new MockTriggerRequest(1, 2, 3, 4, 5));

        mgr.execute(splObjs, 0);

        splObjs.add(new MockTriggerRequest(2, 2, 2, 6, 7));
        splObjs.add(new MockTriggerRequest(4, 1, 7, 14, 16));

        mgr.execute(splObjs, 0);
    }

    @Test
    public void testExecuteTrigReqsNoSublist()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAlgorithms();

        List splObjs = new ArrayList();
        splObjs.add(new MockTriggerRequest(1, 2, 3, 4, 5));

        mgr.execute(splObjs, 0);

        splObjs.add(new MockTriggerRequest(2, 2, 2, 6, 7));

        MockTriggerRequest merged = new MockTriggerRequest(4, 1, 7, 14, 16);
        merged.setMerged();
        splObjs.add(merged);

        mgr.execute(splObjs, 0);

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "No subtriggers found in " + merged;
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testExecuteTrigReqs()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAlgorithms();

        List splObjs = new ArrayList();
        splObjs.add(new MockTriggerRequest(1, 2, 3, 4, 5));

        mgr.execute(splObjs, 0);

        splObjs.add(new MockTriggerRequest(2, 2, 2, 6, 7));

        MockTriggerRequest merged = new MockTriggerRequest(4, 1, 7, 14, 16);
        merged.setMerged();
        merged.addPayload(new MockTriggerRequest(6, 2, 2, 14, 16));

        splObjs.add(merged);

        mgr.execute(splObjs, 0);
    }

    @Test
    public void testMoniCountsUnstarted()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        List list = mgr.getMoniCounts();

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "Cannot get trigger counts for monitoring";
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testMoniCounts()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);
        mgr.setRunNumber(123);

        List list = mgr.getMoniCounts();
        assertNotNull("List should not be null", list);
        assertEquals("Unexpected list entries", 0, list.size());
    }

    @Test
    public void testSendHistoUnstarted()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.sendHistograms();

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "Cannot send multiplicity data";
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testSendHisto()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);
        mgr.setRunNumber(123);

        mgr.sendHistograms();
    }

    @Test
    public void testNoSwitchRun()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);
        mgr.setRunNumber(123);

        mgr.switchToNewRun(456);

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "Collector has not been created before run switch";
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testSwitchRun()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);
        mgr.addTrigger(new MockAlgorithm("dummy"));

        MockOutputProcess out = new MockOutputProcess();
        mgr.setOutputEngine(out);

        MockSplicer spl = new MockSplicer();
        mgr.setSplicer(spl);

        SplicerChangedEvent evt = new SplicerChangedEvent(mgr, 0, null,
                                                          new ArrayList());
        mgr.starting(evt);

        mgr.switchToNewRun(456);
    }

    @Test
    public void testSwitchRunTooMany()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);
        mgr.addTrigger(new MockAlgorithm("dummy"));

        MockOutputProcess out = new MockOutputProcess();
        mgr.setOutputEngine(out);

        MockSplicer spl = new MockSplicer();
        mgr.setSplicer(spl);

        SplicerChangedEvent evt = new SplicerChangedEvent(mgr, 0, null,
                                                          new ArrayList());
        mgr.starting(evt);

        mgr.switchToNewRun(456);

        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        mgr.switchToNewRun(789);

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "Cannot set next run number";
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testGetTrigMoniMap()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        HashMap<String, Object> expMap = new HashMap<String, Object>();
        for (int i = 0; i < 3; i++) {
            final String name;
            if (i == 0) {
                name = "foo";
            } else if (i == 1) {
                name = "bar";
            } else {
                name = "empty";
            }

            final int type = 1 + i;
            final int cfgId = 3 - i;

            MockAlgorithm a = new MockAlgorithm(name, type, cfgId, INICE_ID);

            if (i > 1) {
                final String key = "k" + i;
                final String val = "v" + i;
                a.addTriggerMonitorData(key, val);

                expMap.put(name + "-" + cfgId + "-" + key, val);
            }

            mgr.addTrigger(a);
        }

        Map<String, Object> map = mgr.getTriggerMonitorMap();
        assertNotNull("Trigger monitor map should not be null", map);
        assertEquals("Bad trigger monitor map length",
                     expMap.size(), map.size());

        for (String k : map.keySet()) {
            assertTrue("Unknown key " + k, expMap.containsKey(k));
            assertEquals("Bad value for key " + k, expMap.get(k), map.get(k));
        }
    }

    @Test
    public void testCountsGTSwitchRun()
    {
        final int srcId = GLOBAL_ID;

        MockSourceID src = new MockSourceID(srcId);
        MockBufferCache bufCache = new MockBufferCache("foo");

        MockAlerter alerter = new MockAlerter();
        alerter.setExpectedVarName("trigger_multiplicity");
        alerter.setExpectedPriority(Priority.SCP);

        TriggerManager mgr = new TriggerManager(src, bufCache);
        mgr.setAlerter(alerter);

        ArrayList<MockAlgorithm> algo = new ArrayList<MockAlgorithm>();
        for (int i = 0; i < 3; i++) {
            final String name;
            if (i == 0) {
                name = "foo";
            } else if (i == 1) {
                name = "bar";
            } else {
                name = "empty";
            }

            final int type = 1 + i;
            final int cfgId = 3 - i;

            MockAlgorithm a = new MockAlgorithm(name, type, cfgId, srcId);
            algo.add(a);

            mgr.addTrigger(a);
        }

        MockOutputProcess out = new MockOutputProcess();
        mgr.setOutputEngine(out);

        MockOutputChannel outChan = new MockOutputChannel();
        out.setOutputChannel(outChan);

        MockSplicer spl = new MockSplicer();
        mgr.setSplicer(spl);

        System.setProperty("icecube.sndaq.zmq.address", "localhost:0");

        SplicerChangedEvent evt = new SplicerChangedEvent(mgr, 0, null,
                                                          new ArrayList());
        mgr.starting(evt);

        final int runNum = 123;

        mgr.setRunNumber(runNum);
        mgr.setFirstGoodTime(0);

        final long intvl = 400000000000L;

        for (int i = 0; i < 6; i++) {
            for (MockAlgorithm a : algo) {
                a.addInterval(i * intvl, (i + 1) * intvl - 10);
            }
            try { Thread.sleep(100); } catch (Exception ex) { }
        }

        List<Map<String, Object>> counts;

        counts = mgr.getMoniCounts();
        for (Map<String, Object> map : counts) {
            if (!map.containsKey("runNumber")) {
                fail("Count map " + map + " does not contain run number");
            } else if (((Integer) map.get("runNumber")) != runNum) {
                fail("Expected run#" + runNum + " in " + map);
            }
        }

        final int newNum = 456;

        mgr.switchToNewRun(newNum);

        for (int i = 10; i < 16; i++) {
            for (MockAlgorithm a : algo) {
                a.addInterval(i * intvl, (i + 1) * intvl - 10);
            }
            try { Thread.sleep(100); } catch (Exception ex) { }
        }

        boolean pastOldNum = false;
        counts = mgr.getMoniCounts();
        for (Map<String, Object> map : counts) {
            if (!map.containsKey("runNumber")) {
                fail("Count map " + map + " does not contain run number");
            } else if (((Integer) map.get("runNumber")) == newNum) {
                pastOldNum = true;
            } else if (((Integer) map.get("runNumber")) == runNum) {
                if (pastOldNum) {
                    fail("Expected run#" + newNum + " in " + map);
                }
            }
        }
    }

    @Test
    public void testCountsIISwitchRun()
    {
        final int srcId = INICE_ID;

        MockSourceID src = new MockSourceID(srcId);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        ArrayList<MockAlgorithm> algo = new ArrayList<MockAlgorithm>();
        for (int i = 0; i < 3; i++) {
            final String name;
            if (i == 0) {
                name = "foo";
            } else if (i == 1) {
                name = "bar";
            } else {
                name = "empty";
            }

            final int type = 1 + i;
            final int cfgId = 3 - i;

            MockAlgorithm a = new MockAlgorithm(name, type, cfgId, srcId);
            algo.add(a);

            mgr.addTrigger(a);
        }

        MockOutputProcess out = new MockOutputProcess();
        mgr.setOutputEngine(out);

        MockOutputChannel outChan = new MockOutputChannel();
        out.setOutputChannel(outChan);

        MockSplicer spl = new MockSplicer();
        mgr.setSplicer(spl);

        System.setProperty("icecube.sndaq.zmq.address", "localhost:0");

        SplicerChangedEvent evt = new SplicerChangedEvent(mgr, 0, null,
                                                          new ArrayList());
        mgr.starting(evt);

        final int runNum = 123;

        mgr.setRunNumber(runNum);
        mgr.setFirstGoodTime(0);

        final long intvl = 400000000000L;

        for (int i = 0; i < 6; i++) {
            for (MockAlgorithm a : algo) {
                a.addInterval(i * intvl, (i + 1) * intvl - 10);
            }
            try { Thread.sleep(100); } catch (Exception ex) { }
        }

        List<Map<String, Object>> counts;

        counts = mgr.getMoniCounts();
        for (Map<String, Object> map : counts) {
            if (!map.containsKey("runNumber")) {
                fail("Count map " + map + " does not contain run number");
            } else if (((Integer) map.get("runNumber")) != runNum) {
                fail("Expected run#" + runNum + " in " + map);
            }
        }

        final int newNum = 456;

        mgr.switchToNewRun(newNum);

        for (int i = 10; i < 16; i++) {
            for (MockAlgorithm a : algo) {
                a.addInterval(i * intvl, (i + 1) * intvl - 10);
            }
            try { Thread.sleep(100); } catch (Exception ex) { }
        }

        boolean pastOldNum = false;
        counts = mgr.getMoniCounts();
        for (Map<String, Object> map : counts) {
            if (!map.containsKey("runNumber")) {
                fail("Count map " + map + " does not contain run number");
            } else if (((Integer) map.get("runNumber")) == newNum) {
                pastOldNum = true;
            } else if (((Integer) map.get("runNumber")) == runNum) {
                if (pastOldNum) {
                    fail("Expected run#" + newNum + " in " + map);
                }
            }
        }
    }

    class MyHit
        extends MockPayload
        implements IHitPayload
    {
        private int srcId;

        private IUTCTime timeObj;
        private ISourceID srcObj;

        MyHit(int srcId, long timeVal)
        {
            super(timeVal);
        }

        public IDOMID getDOMID()
        {
            throw new Error("Unimplemented");
        }

        public IUTCTime getHitTimeUTC()
        {
            if (timeObj == null) {
                timeObj = new MockUTCTime(getUTCTime());
            }

            return timeObj;
        }

        public double getIntegratedCharge()
        {
            throw new Error("Unimplemented");
        }

        public int getPayloadInterfaceType()
        {
            return PayloadInterfaceRegistry.I_HIT_PAYLOAD;
        }

        public ISourceID getSourceID()
        {
            if (srcObj == null) {
                srcObj = new MockSourceID(srcId);
            }

            return srcObj;
        }

        public int getTriggerConfigID()
        {
            throw new Error("Unimplemented");
        }

        public int getTriggerType()
        {
            throw new Error("Unimplemented");
        }
    }

    class NonHit
        extends MyHit
    {
        NonHit(int srcId, long timeVal)
        {
            super(srcId, timeVal);
        }

        public int getPayloadInterfaceType()
        {
            return Integer.MIN_VALUE;
        }
    }
}
