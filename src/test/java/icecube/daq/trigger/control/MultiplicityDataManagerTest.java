package icecube.daq.trigger.control;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.exceptions.MultiplicityDataException;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockTriggerRequest;
import icecube.daq.util.Leapseconds;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

class MockAlerter
    implements Alerter
{
    private boolean inactive;

    private int numSent;
    private String expVarName;
    private Priority expPrio;

    public MockAlerter()
    {
    }

    public void close()
    {
        throw new Error("Unimplemented");
    }

    public void deactivate()
    {
        inactive = true;
    }

    public int getNumSent()
    {
        return numSent;
    }

    public String getService()
    {
        throw new Error("Unimplemented");
    }

    public boolean isActive()
    {
        return !inactive;
    }

    public void send(String varname, Priority prio, Calendar dateTime,
                     Map<String, Object> values)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void send(String varname, Priority prio, Map<String, Object> values)
        throws AlertException
    {
        assertEquals("Unexpected varname", varname, expVarName);
        assertEquals("Unexpected priority", prio, expPrio);

        numSent++;
    }

    public void sendAlert(Priority prio, String condition, Map x2)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void sendAlert(Priority prio, String condition, String notify,
                          Map<String, Object> vars)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void sendAlert(Calendar dateTime, Priority prio, String condition,
                          String notify, Map<String, Object> vars)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void sendObject(Object obj)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void setAddress(String host, int port)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void setExpectedPriority(Priority prio)
    {
        expPrio = prio;
    }

    public void setExpectedVarName(String name)
    {
        expVarName = name;
    }
}

public class MultiplicityDataManagerTest
{
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
    public void testAddNoStart()
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlerter(alerter);

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

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlerter(alerter);
        mgr.setFirstGoodTime(1);

        mgr.start(123);

        mgr.add(new MockTriggerRequest(1, 2, 3, 4, 5));

        List<Map> histo = mgr.getCounts();
        assertNotNull("Histogram should not be null", histo);
        assertEquals("Unexpected histogram list " + histo, 0, histo.size());
    }

    @Test
    public void testAddMergedEmpty()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlerter(alerter);
        mgr.setFirstGoodTime(1);

        mgr.start(123);

        final int type = 2;

        int uid = 1;

        MockTriggerRequest req = new MockTriggerRequest(uid++, type, -1, 4, 5);
        req.setMerged();

        mgr.add(req);

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "No subtriggers found in " + req;
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testAddMerged()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlerter(alerter);
        mgr.setFirstGoodTime(1);

        mgr.start(123);

        final long startTime = 1000;
        final long endTime = 1500;

        MockTriggerRequest sub =
            new MockTriggerRequest(17, 17, 17, startTime, endTime);

        final int type = 2;

        int uid = 1;

        MockTriggerRequest req =
            new MockTriggerRequest(uid++, type, -1, startTime, endTime);
        req.setMerged();
        req.addPayload(sub);

        mgr.add(req);
    }

    @Test
    public void testAddMulti()
        throws MultiplicityDataException
    {
        // set the Leapseconds config directory to UTCTime.toDateString() works
        File configDir = new File(getClass().getResource("/config").getPath());
        if (!configDir.exists()) {
            throw new IllegalArgumentException("Cannot find config" +
                                               " directory under " +
                                               getClass().getResource("/"));
        }
        Leapseconds.setConfigDirectory(configDir);

        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlerter(alerter);
        mgr.setFirstGoodTime(1);

        mgr.start(123);

        final int type = 2;
        final int cfgId = 3;

        int uid = 1;

        final long firstBin = 100000;
        mgr.add(new MockTriggerRequest(uid++, type, cfgId,
                                       firstBin + 4, firstBin + 5));

        final long nextBin = firstBin + CountData.DAQ_BIN_WIDTH;
        mgr.add(new MockTriggerRequest(uid++, type, cfgId,
                                       nextBin + 4, nextBin + 5));

        List<Map> histo = mgr.getCounts();
        assertNotNull("Histogram should not be null", histo);
        assertEquals("Unexpected histogram list " + histo, 1, histo.size());

        Map<String, Object> map = histo.get(0);
        assertEquals("Bad type", type, map.get("trigid"));
        assertEquals("Bad config ID", cfgId, map.get("configid"));
        assertEquals("Bad source ID", GLOBAL_ID, map.get("sourceid"));
        assertEquals("Bad count", 1, map.get("value"));
    }

    @Test
    public void testGetCountsNoStart()
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlerter(alerter);

        try {
            mgr.getCounts();
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
        mgr.setAlerter(alerter);

        mgr.start(123);

        List<Map> histo = mgr.getCounts();
        assertNull("Unexpected histogram list " + histo, histo);
    }

    @Test
    public void testResetNoStart()
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlerter(alerter);

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
        mgr.setAlerter(alerter);

        mgr.setNextRunNumber(123);

        mgr.reset();
    }

    @Test
    public void testSendNoStart()
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlerter(alerter);

        try {
            mgr.send();
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
        mgr.setAlerter(alerter);
        mgr.start(123);

        boolean sent = mgr.send();
        assertFalse("Unexpected return value", sent);
        assertEquals("Unexpected send", 0, alerter.getNumSent());
    }

    @Test
    public void testSend()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlerter(alerter);
        mgr.setFirstGoodTime(1);

        mgr.start(123);

        final int type = 2;
        final int cfgId = 3;

        int uid = 1;

        final long firstBin = 100000;
        mgr.add(new MockTriggerRequest(uid++, type, cfgId,
                                       firstBin + 4, firstBin + 5));

        final long nextBin = firstBin + CountData.DAQ_BIN_WIDTH;
        mgr.add(new MockTriggerRequest(uid++, type, cfgId,
                                       nextBin + 4, nextBin + 5));

        alerter.setExpectedVarName("trigger_multiplicity");
        alerter.setExpectedPriority(Alerter.Priority.SCP);

        boolean sent = mgr.send();
        assertTrue("Unexpected return value", sent);
        assertEquals("Unexpected send", 1, alerter.getNumSent());
    }

    @Test
    public void testSetRunNumTwice()
        throws MultiplicityDataException
    {
        MockAlerter alerter = new MockAlerter();

        MultiplicityDataManager mgr = new MultiplicityDataManager();
        mgr.setAlerter(alerter);

        mgr.setNextRunNumber(123);

        try {
            mgr.setNextRunNumber(123);
            fail("Should not succeed");
        } catch (MultiplicityDataException mde) {
            assertNotNull("Null message", mde.getMessage());

            final String msg = "Next run number has already been set";
            assertEquals("Unexpected exception", msg, mde.getMessage());
        }
    }
}
