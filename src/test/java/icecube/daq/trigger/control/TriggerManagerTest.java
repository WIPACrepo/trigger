package icecube.daq.trigger.control;

import icecube.daq.common.MockAppender;
import icecube.daq.juggler.alert.AlertQueue;
import icecube.daq.juggler.alert.Alerter.Priority;
import icecube.daq.payload.IDOMID;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.Spliceable;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerChangedEvent;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.control.ITriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.test.MockAlerter;
import icecube.daq.trigger.test.MockAlgorithm;
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

import org.apache.log4j.BasicConfigurator;

public class TriggerManagerTest
{
    private static final int INICE_ID =
        SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;
    private static final int GLOBAL_ID =
        SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID;

    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    private void flushQueue(AlertQueue aq)
    {
        if (!aq.isStopped()) {
            for (int i = 0; i < 1000; i++) {
                if (aq.isIdle() && aq.getNumQueued() == 0) {
                    break;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                    break;
                }
            }

            if (aq.getNumQueued() > 0) {
                throw new Error("Cannot flush " + aq + "; " +
                                aq.getNumQueued() + " alerts queued");
            }
        }
    }

    private static long getNumInputsQueued(TriggerManager mgr)
    {
        Map<String, Integer> map = mgr.getQueuedInputs();

        long total = 0;
        for (Integer val : map.values()) {
            total += val;
        }

        return total;
    }

    @Before
    public void setUp()
        throws Exception
    {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);
    }

    @After
    public void tearDown()
        throws Exception
    {
        appender.assertNoLogMessages();
    }

    @Test
    public void testSimple()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);
        assertEquals("Bad source ID", src.getSourceID(), mgr.getSourceId());

        assertNull("Registry should be null", mgr.getDOMRegistry());
        assertEquals("Bad count", 0L, mgr.getTotalProcessed());
        assertEquals("Bad number of inputs queued",
                     0, getNumInputsQueued(mgr));
        assertEquals("Bad number of outputs queued",
                     0, mgr.getNumOutputsQueued());
        assertEquals("Bad total processed",
                     0L, mgr.getTotalProcessed());
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

        final String msg =
            String.format("Attempt to add duplicate trigger with type %d" +
                          " cfgId %d srcId %d (old %s, new %s)",
                          algo.getTriggerType(), algo.getTriggerConfigId(),
                          algo.getSourceId(), algo.getTriggerName(),
                          algo.getTriggerName());
        appender.assertLogMessage(msg);
        appender.assertNoLogMessages();
    }

    @Test
    public void testAnalyzeEmpty()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.startInputThread();
        try {
            mgr.analyze(new ArrayList());
        } catch (Error err) {
            assertNotNull("Message should not be null", err.getMessage());

            final String msg = "No consumers for 0 hits";
            assertEquals("Unexpected error message", msg, err.getMessage());
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);
    }

    @Test
    public void testAnalyzeNoSub()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        List splObjs = new ArrayList();
        splObjs.add(new MyHit(123, 456));

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
            fail("Should not succeed");
        } catch (Error err) {
            assertNotNull("Message should not be null", err.getMessage());

            final String msg = "No consumers for 1 hits";
            assertEquals("Unexpected error message", msg, err.getMessage());
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);
    }

    @Test
    public void testAnalyzeBad()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAll();

        List splObjs = new ArrayList();
        splObjs.add(new NonHit(123, 456));

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();

        final String msg1 = "TriggerHandler only knows about either" +
            " HitPayloads or TriggerRequestPayloads!";
        appender.assertLogMessage(msg1);

        final String msg2 = "Ignoring invalid payload ";
        appender.assertLogMessage(msg2);

        appender.assertNoLogMessages();
    }

    @Test
    public void testAnalyzeHitOutOfOrder()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAll();

        List splObjs = new ArrayList();
        splObjs.add(new MyHit(123, 234));

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();

        MyHit goodOrder = new MyHit(123, 345);
        splObjs.add(goodOrder);

        MyHit badOrder = new MyHit(321, 246);
        splObjs.add(badOrder);

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();

        final long diff = (badOrder.getUTCTime() - goodOrder.getUTCTime());
        final String msg1 = "Hit " + badOrder.getUTCTime() + " from " +
            badOrder.getSourceID() +
            " out of order! This time - Last time = " + diff +
            ", src of last hit = " + goodOrder.getSourceID();
        appender.assertLogMessage(msg1);

        final String msg2 = "Ignoring invalid payload ";
        appender.assertLogMessage(msg2);

        appender.assertNoLogMessages();
    }

    @Test
    public void testAnalyzeHits()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAll();

        List splObjs = new ArrayList();
        splObjs.add(new MyHit(123, 234));

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();

        splObjs.add(new MyHit(123, 246));
        splObjs.add(new MyHit(321, 345));

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();
    }

    @Test
    public void testAnalyzeTrigReqBadComp()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAll();

        List splObjs = new ArrayList();
        splObjs.add(new MockTriggerRequest(1, 2, 3, 4, 5));

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();

        final String msg1 = "Source #" + INICE_ID +
            " cannot process trigger requests";
        appender.assertLogMessage(msg1);

        final String msg2 = "Ignoring invalid payload ";
        appender.assertLogMessage(msg2);

        appender.assertNoLogMessages();
    }

    @Test
    public void testAnalyzeTrigReqsNoMerged()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAll();

        List splObjs = new ArrayList();
        splObjs.add(new MockTriggerRequest(1, 2, 3, 4, 5));

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();

        splObjs.add(new MockTriggerRequest(2, 2, 2, 6, 7));
        splObjs.add(new MockTriggerRequest(4, 1, 7, 14, 16));

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();
    }

    @Test
    public void testAnalyzeTrigReqsNoSublist()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAll();

        List splObjs = new ArrayList();
        splObjs.add(new MockTriggerRequest(1, 2, 3, 4, 5));

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();

        splObjs.add(new MockTriggerRequest(2, 2, 2, 6, 7));

        MockTriggerRequest merged = new MockTriggerRequest(4, 1, 7, 14, 16);
        merged.setMerged();
        splObjs.add(merged);

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();

        final String msg = "No subtriggers found in " + merged;
        appender.assertLogMessage(msg);
        appender.assertNoLogMessages();
    }

    @Test
    public void testAnalyzeTrigReqs()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAll();

        List splObjs = new ArrayList();
        splObjs.add(new MockTriggerRequest(1, 2, 3, 4, 5));

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();

        splObjs.add(new MockTriggerRequest(2, 2, 2, 6, 7));

        MockTriggerRequest merged = new MockTriggerRequest(4, 1, 7, 14, 16);
        merged.setMerged();
        merged.addPayload(new MockTriggerRequest(6, 2, 2, 14, 16));

        splObjs.add(merged);

        mgr.startInputThread();
        try {
            mgr.analyze(splObjs);
        } finally {
            mgr.stopInputThread();
        }
        waitForStopped(mgr);

        splObjs.clear();
    }

/*
    @Test
    public void testMoniCountsUnstarted()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        List list = mgr.getMoniCounts();

        final String msg = "Cannot get trigger counts for monitoring";
        appender.assertLogMessage(msg);
        appender.assertNoLogMessages();
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
*/

    @Test
    public void testSendHistoUnstarted()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        mgr.sendFinalMoni();

        final String msg = "Cannot send multiplicity data";
        appender.assertLogMessage(msg);
        appender.assertNoLogMessages();
    }

    @Test
    public void testSendHisto()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        MockAlerter alerter = new MockAlerter();

        TriggerManager mgr = new TriggerManager(src, bufCache);
        mgr.setAlertQueue(new AlertQueue(alerter));
        mgr.setRunNumber(123);

        mgr.sendFinalMoni();
    }

    @Test
    public void testNoSwitchRun()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);
        mgr.setRunNumber(123);

        mgr.switchToNewRun(456);

        final String msg = "Collector has not been created before run switch";
        appender.assertLogMessage(msg);
        appender.assertNoLogMessages();
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

        final int runNum = 123;
        mgr.setRunNumber(runNum);

        SplicerChangedEvent<Spliceable> evt =
            new SplicerChangedEvent<Spliceable>(spl, Splicer.State.STARTING,
                                                null, new ArrayList());
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

        mgr.setRunNumber(123);

        SplicerChangedEvent<Spliceable> evt =
            new SplicerChangedEvent<Spliceable>(spl, Splicer.State.STARTING,
                                                null, new ArrayList());
        mgr.starting(evt);

        mgr.switchToNewRun(456);

        appender.assertNoLogMessages();

        mgr.switchToNewRun(789);

        final String msg = "Cannot set next run number";
        appender.assertLogMessage(msg);
        appender.assertNoLogMessages();
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
        alerter.addExpected("trigger_multiplicity", Priority.SCP, 3);
        alerter.addExpected("trigger_rate", Priority.EMAIL, 3);

        TriggerManager mgr = new TriggerManager(src, bufCache);
        mgr.setAlertQueue(new AlertQueue(alerter));
        mgr.setRunNumber(123);

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

        final int runNum = 123;
        mgr.setRunNumber(runNum);

        SplicerChangedEvent<Spliceable> evt =
            new SplicerChangedEvent<Spliceable>(spl, Splicer.State.STARTING,
                                                null, new ArrayList());
        mgr.starting(evt);

        mgr.setFirstGoodTime(0);

        final long intvl = 400000000000L;

        for (int i = 0; i < 6; i++) {
            for (MockAlgorithm a : algo) {
                a.addInterval(i * intvl, (i + 1) * intvl - 10);
            }
            try { Thread.sleep(100); } catch (Exception ex) { }
        }

/*
        List<Map<String, Object>> counts = mgr.getMoniCounts();
        for (Map<String, Object> map : counts) {
            if (!map.containsKey("runNumber")) {
                fail("Count map " + map + " does not contain run number");
            } else if (((Integer) map.get("runNumber")) != runNum) {
                fail("Expected run#" + runNum + " in " + map);
            }
        }
*/

        final int newNum = 456;

        mgr.switchToNewRun(newNum);

        for (int i = 10; i < 16; i++) {
            for (MockAlgorithm a : algo) {
                a.addInterval(i * intvl, (i + 1) * intvl - 10);
            }
            try { Thread.sleep(100); } catch (Exception ex) { }
        }

        for (int i = 0; i < 100; i++) {
            boolean waiting = false;
            for (MockAlgorithm a : algo) {
                if (a.hasCachedRequests()) {
                    waiting = true;
                }
            }
            if (!waiting) {
                break;
            }
            try { Thread.sleep(100); } catch (Exception ex) { }
        }

/*
        boolean pastOldNum = false;
        List<Map<String, Object>> counts2 = mgr.getMoniCounts();
        for (Map<String, Object> map : counts2) {
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
*/

        alerter.waitForAlerts(100);
    }

/*
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

        final int runNum = 123;
        mgr.setRunNumber(runNum);

        SplicerChangedEvent<Spliceable> evt =
            new SplicerChangedEvent<Spliceable>(spl, Splicer.State.STARTING,
                                                null, new ArrayList());
        mgr.starting(evt);

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
*/

    @Test
    public void testSendTriplets()
        throws TriggerException
    {
        final int srcId = INICE_ID;

        MockSourceID src = new MockSourceID(srcId);
        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerManager mgr = new TriggerManager(src, bufCache);

        List<ITriggerAlgorithm> list = new ArrayList<ITriggerAlgorithm>();
        for (int i = 0; i < 10; i++) {
            list.add(new MockAlgorithm("algo" + i, 10 + i, 20 + i, 30 + i));
        }
        mgr.addTriggers(list);

        final int runNum = 12543;

        MockAlerter alerter = new MockAlerter();
        alerter.addExpected("trigger_triplets", Priority.EMAIL, 1);
        mgr.setAlertQueue(new AlertQueue(alerter));

        mgr.sendTriplets(runNum);

        flushQueue(mgr.getAlertQueue());
        mgr.getAlertQueue().stopAndWait();

        // XXX this doesn't validate the body of the alert
        alerter.waitForAlerts(100);
    }

    private void waitForStopped(TriggerManager mgr)
    {
        for (int i = 0; i < 100; i++) {
            if (mgr.isStopped()) break;

            try {
                Thread.sleep(10);
            } catch (InterruptedException ie) {
                break;
            }
        }
    }

    class MyHit
        extends MockPayload
        implements IHitPayload, Spliceable
    {
        private int srcId;

        private IUTCTime timeObj;
        private ISourceID srcObj;

        MyHit(int srcId, long timeVal)
        {
            super(timeVal);

            this.srcId = srcId;
        }

        @Override
        public int compareSpliceable(Spliceable spl)
        {
            throw new Error("Unimplemented");
        }

        @Override
        public Object deepCopy()
        {
            return new MyHit(srcId, getUTCTime());
        }

        @Override
        public short getChannelID()
        {
            throw new Error("Unimplemented");
        }

        @Override
        public IDOMID getDOMID()
        {
            throw new Error("Unimplemented");
        }

        @Override
        public IUTCTime getHitTimeUTC()
        {
            if (timeObj == null) {
                timeObj = new MockUTCTime(getUTCTime());
            }

            return timeObj;
        }

        @Override
        public double getIntegratedCharge()
        {
            throw new Error("Unimplemented");
        }

        @Override
        public int getPayloadInterfaceType()
        {
            return PayloadInterfaceRegistry.I_HIT_PAYLOAD;
        }

        @Override
        public ISourceID getSourceID()
        {
            if (srcObj == null) {
                srcObj = new MockSourceID(srcId);
            }

            return srcObj;
        }

        @Override
        public int getTriggerConfigID()
        {
            throw new Error("Unimplemented");
        }

        @Override
        public int getTriggerType()
        {
            throw new Error("Unimplemented");
        }

        @Override
        public boolean hasChannelID()
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

        @Override
        public int getPayloadInterfaceType()
        {
            return Integer.MIN_VALUE;
        }
    }
}
