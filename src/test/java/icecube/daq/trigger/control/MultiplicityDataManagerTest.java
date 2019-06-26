package icecube.daq.trigger.control;

import icecube.daq.common.MockAppender;
import icecube.daq.juggler.alert.AlertQueue;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.exceptions.MultiplicityDataException;
import icecube.daq.trigger.test.MockAlerter;
import icecube.daq.trigger.test.MockAlgorithm;
import icecube.daq.trigger.test.MockTriggerRequest;
import icecube.daq.util.Leapseconds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

public class MultiplicityDataManagerTest
{
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

    @Before
    public void setUp()
        throws Exception
    {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);

        // set the Leapseconds config directory so UTCTime.toDateString() works
        File configDir = new File(getClass().getResource("/config").getPath());
        if (!configDir.exists()) {
            throw new IllegalArgumentException("Cannot find config" +
                                               " directory under " +
                                               getClass().getResource("/"));
        }
        Leapseconds.setConfigDirectory(configDir);
    }

    @After
    public void tearDown()
        throws Exception
    {
        appender.assertNoLogMessages();
    }

    @Test
    public void testAddNoStart()
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));

        try {
            mgr.add(new MockTriggerRequest(1, 2, 3, 4, 5));
            fail("Should not succeed");
        } catch (MultiplicityDataException mde) {
            assertNotNull("Null message", mde.getMessage());

            final String msg = "MultiplicityDataManager has not been started";
            assertEquals("Unexpected exception", msg, mde.getMessage());
        }
    }

    @Test
    public void testAddOne()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        final int srcId = SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;
        final int cfgId = 2;
        final int type = 3;

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));
        mgr.setFirstGoodTime(1);

        mgr.addAlgorithm(new MockAlgorithm("TstAddOne", type, cfgId, srcId));

        mgr.start(123);

        mgr.add(new MockTriggerRequest(1, srcId, type, cfgId, 4, 5));

        Iterable<Map<String, Object>> histo = mgr.getSummary(10, true, true);
        assertNotNull("Histogram should not be null", histo);
        assertFalse("Unexpected histogram list " + histo,
                    histo.iterator().hasNext());
    }

    @Test
    public void testAddMergedEmpty()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));
        mgr.setFirstGoodTime(1);

        mgr.start(123);

        final int srcId = SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;
        final int type = 3;

        int uid = 1;

        MockTriggerRequest req = new MockTriggerRequest(uid++, srcId, type, -1,
                                                        4, 5);
        req.setMerged();

        mgr.add(req);

        final String msg = "No subtriggers found in " + req;
        appender.assertLogMessage(msg);
        appender.assertNoLogMessages();
    }

    @Test
    public void testAddMerged()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));
        mgr.setFirstGoodTime(1);

        final int srcId = SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;

        final int type = 16;
        final int cfg = 17;

        mgr.addAlgorithm(new MockAlgorithm("TstAddOne", type, cfg, srcId));

        mgr.start(123);

        final long startTime = 1000;
        final long endTime = 1500;

        MockTriggerRequest sub =
            new MockTriggerRequest(17, srcId, type, cfg, startTime, endTime);

        final int gtype = 2;

        int uid = 1;

        MockTriggerRequest req =
            new MockTriggerRequest(uid++, GLOBAL_ID, gtype, -1, startTime,
                                   endTime);
        req.setMerged();
        req.addPayload(sub);

        mgr.add(req);
    }

    @Test
    public void testAddMulti()
        throws MultiplicityDataException
    {
        final int srcId = SourceIdRegistry.ICETOP_TRIGGER_SOURCE_ID;
        final int cfgId = 2;
        final int type = 3;

        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));
        mgr.setFirstGoodTime(1);

        mgr.addAlgorithm(new MockAlgorithm("TstAddMulti", type, cfgId, srcId));

        mgr.start(123);

        int uid = 1;

        final long firstBin = 100000;
        mgr.add(new MockTriggerRequest(uid++, srcId, type, cfgId,
                                       firstBin + 4, firstBin + 5));

        final long nextBin = firstBin + Bins.WIDTH;
        mgr.add(new MockTriggerRequest(uid++, srcId, type, cfgId,
                                       nextBin + 4, nextBin + 5));

        Iterable<Map<String, Object>> histo = mgr.getSummary(10, true, true);
        assertNotNull("Histogram should not be null", histo);

        boolean found = false;
        for (Map<String, Object> map : histo) {
            if (found) {
                fail("Found more than one histogram");
            }
            found = true;

            assertEquals("Bad type", type, map.get("trigid"));
            assertEquals("Bad config ID", cfgId, map.get("configid"));
            assertEquals("Bad source ID", srcId, map.get("sourceid"));
            assertEquals("Bad count", 1, map.get("value"));
        }
        assertTrue("Didn't find any histograms", found);
    }

    @Test
    public void testMultiSummary()
        throws MultiplicityDataException
    {
        final int srcId = SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;
        final int cfgId = 2;
        final int type = 3;

        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));
        mgr.setFirstGoodTime(1);

        mgr.addAlgorithm(new MockAlgorithm("TstMultiSummary", type, cfgId,
                                           srcId));

        mgr.start(123);

        final int numBins = 10;
        final int extraBins = 2;
        int uid = 1;
        long binTime = 100000;

        for (int i = 0; i < numBins + extraBins; i++) {
            mgr.add(new MockTriggerRequest(uid++, srcId, type, cfgId,
                                           binTime + 4, binTime + 5));
            binTime += Bins.WIDTH;
        }

        Iterable<Map<String, Object>> histo = mgr.getSummary(10, true, true);
        assertNotNull("Histogram should not be null", histo);

        int count = 0;
        for (Map<String, Object> map : histo) {
            if (count >= 2) {
                fail("Too many histograms!");
            }
            count++;

            assertEquals("Bad type", type, map.get("trigid"));
            assertEquals("Bad config ID", cfgId, map.get("configid"));
            assertEquals("Bad source ID", srcId, map.get("sourceid"));
            assertEquals("Bad count", (count == 1 ? numBins : extraBins - 1),
                         map.get("value"));
        }
        assertEquals("Expected 2 histograms, only got " + count, 2, count);
    }

    @Test
    public void testGetCountsNoStart()
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));

        try {
            mgr.getSummary(10, true, true);
            fail("Should not succeed");
        } catch (MultiplicityDataException mde) {
            assertNotNull("Null message", mde.getMessage());

            final String msg = "MultiplicityDataManager has not been started";
            assertEquals("Unexpected exception", msg, mde.getMessage());
        }
    }

    @Test
    public void testGetCountsEmpty()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));

        mgr.start(123);

        Iterable<Map<String, Object>> histo = mgr.getSummary(10, true, true);
        assertNotNull("Unexpected null histogram list " + histo, histo);

        for (Map<String, Object> map : histo) {
            fail("Histogram list " + histo + " should be empty");
        }
    }

    @Test
    public void testResetNoStart()
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));

        try {
            mgr.reset();
            fail("Should not succeed");
        } catch (MultiplicityDataException mde) {
            assertNotNull("Null message", mde.getMessage());

            final String msg = "Next run number has not been set";
            assertEquals("Unexpected exception", msg, mde.getMessage());
        }
    }

    @Test
    public void testReset()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));

        mgr.start(123);

        mgr.setNextRunNumber(456);

        mgr.reset();
    }

    @Test
    public void testSendNoStart()
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));

        try {
            mgr.sendFinal();
            fail("Should not succeed");
        } catch (MultiplicityDataException mde) {
            assertNotNull("Null message", mde.getMessage());

            final String msg = "MultiplicityDataManager has not been started";
            assertEquals("Unexpected exception", msg, mde.getMessage());
        }
    }

    @Test
    public void testSendEmpty()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));
        mgr.start(123);

        boolean sent = mgr.sendFinal();
        assertFalse("Unexpected return value", sent);
        assertEquals("Unexpected send", 0, alerter.getNumSent());
    }

    @Test
    public void testSend()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        final int srcId = SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;
        final int cfgId = 2;
        final int type = 3;

        AlertQueue aq = new AlertQueue(alerter);

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(aq);
        mgr.setFirstGoodTime(1);

        mgr.addAlgorithm(new MockAlgorithm("TstSend", type, cfgId, srcId));

        mgr.start(123);

        int uid = 1;

        final long firstBin = 100000;
        mgr.add(new MockTriggerRequest(uid++, srcId, type, cfgId,
                                       firstBin + 4, firstBin + 5));

        final long nextBin = firstBin + Bins.WIDTH;
        mgr.add(new MockTriggerRequest(uid++, srcId, type, cfgId,
                                       nextBin + 4, nextBin + 5));

        alerter.addExpected("trigger_multiplicity", Alerter.Priority.SCP, 1);
        alerter.addExpected("trigger_rate", Alerter.Priority.EMAIL, 1);

        boolean sent = mgr.sendFinal();
        assertTrue("Unexpected return value", sent);

        flushQueue(aq);
        aq.stopAndWait();

        alerter.waitForAlerts(100);
        assertEquals("Unexpected send", 2, alerter.getNumSent());
    }

    @Test
    public void testSetRunNumTwice()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlertQueue(new AlertQueue(alerter));

        final int runNum = 12345;

        mgr.setNextRunNumber(runNum);

        final int nextNum = 67890;
        try {
            mgr.setNextRunNumber(nextNum);
            fail("Should not succeed");
        } catch (MultiplicityDataException mde) {
            assertNotNull("Null message", mde.getMessage());

            final String msg = "Cannot set next run number to " + nextNum +
                "; already set to " + runNum;
            assertEquals("Unexpected exception", msg, mde.getMessage());
        }
    }
}
