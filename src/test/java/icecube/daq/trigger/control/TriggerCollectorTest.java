package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.algorithm.FlushRequest;
import icecube.daq.trigger.control.Interval;
import icecube.daq.trigger.test.MockAlgorithm;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockBufferCache;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockReadoutRequest;
import icecube.daq.trigger.test.MockSplicer;
import icecube.daq.trigger.test.MockTriggerRequest;

import java.util.ArrayList;
import java.util.List;

import org.junit.*;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

class MockCollectorThread
    implements ICollectorThread
{
    private boolean changed;
    private boolean reset;
    private boolean started;
    private boolean stopped;

    public long getCollectorLoopCount()
    {
        throw new Error("Unimplemented");
    }

    public long getIntervalSearchCount()
    {
        throw new Error("Unimplemented");
    }

    public long getFoundIntervalCount()
    {
        throw new Error("Unimplemented");
    }

    public long getNumQueued()
    {
        throw new Error("Unimplemented");
    }

    public boolean isOutputStopped()
    {
        throw new Error("Unimplemented");
    }

    public void resetUID()
    {
        reset = true;
    }

    public void setChanged()
    {
        changed = true;
    }

    public void start(Splicer splicer)
    {
        started = true;
    }

    public void stop()
    {
        stopped = true;
    }

    public boolean wasChanged()
    {
        return changed;
    }

    public boolean wasStarted()
    {
        return started;
    }

    public boolean wasStopped()
    {
        return stopped;
    }

    public boolean wasUIDReset()
    {
        return reset;
    }
}

class MyCollector
    extends TriggerCollector
{
    private MockCollectorThread thrd;

    MyCollector(int srcId, List<INewAlgorithm> algorithms,
                DAQComponentOutputProcess outputEngine,
                IByteBufferCache outCache,
                IMonitoringDataManager multiDataMgr)
    {
        super(srcId, algorithms, outputEngine, outCache, multiDataMgr);
    }

    public ICollectorThread createCollectorThread(String name, int srcId,
                                                  List<INewAlgorithm> algo,
                                                  IMonitoringDataManager mdm,
                                                  IOutputThread outThrd)
    {
        if (thrd == null) {
            thrd = new MockCollectorThread();
        }

        return thrd;
    }

    public boolean wasChanged()
    {
        return thrd.wasChanged();
    }

    public boolean wasStarted()
    {
        return thrd.wasStarted();
    }

    public boolean wasStopped()
    {
        return thrd.wasStopped();
    }

    public boolean wasUIDReset()
    {
        return thrd.wasUIDReset();
    }
}

public class TriggerCollectorTest
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
    public void testCreate()
    {
        try {
            new TriggerCollector(INICE_ID, null, null, null, null);
            fail("Constructor should fail with null algorithm list");
        } catch (Error err) {
            // expect this to fail
        }

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();

        try {
            new TriggerCollector(INICE_ID, algorithms, null, null, null);
            fail("Constructor should fail with empty algorithm list");
        } catch (Error err) {
            // expect this to fail
        }

        algorithms.add(new MockAlgorithm("foo"));

        try {
            new TriggerCollector(INICE_ID, algorithms, null, null, null);
            fail("Constructor should fail with null output process");
        } catch (Error err) {
            // expect this to fail
        }

        MockOutputProcess out = new MockOutputProcess();

        try {
            new TriggerCollector(INICE_ID, algorithms, out, null, null);
            fail("Constructor should fail with null output cache");
        } catch (Error err) {
            // expect this to fail
        }

        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerCollector tc =
            new TriggerCollector(INICE_ID, algorithms, out, bufCache, null);
        assertFalse("New collector is stopped", tc.isStopped());
        assertEquals("New collector queue should be empty",
                     0L, tc.getNumQueued());
    }

    @Test
    public void testNullSplicer()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("foo");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockOutputProcess out = new MockOutputProcess();

        MyCollector tc =
            new MyCollector(INICE_ID, algorithms, out, bufCache, null);

        tc.startThreads(null);

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String nullMsg = "Splicer cannot be null";
        assertEquals("Bad log message", nullMsg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testAPI()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("foo");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockOutputProcess out = new MockOutputProcess();

        MyCollector tc =
            new MyCollector(INICE_ID, algorithms, out, bufCache, null);

        assertFalse("Collector thread UID should not be reset",
                    tc.wasUIDReset());
        tc.resetUID();
        assertTrue("Collector thread UID should be reset", tc.wasUIDReset());

        assertFalse("Collector thread should not be changed",
                    tc.wasChanged());
        tc.setChanged();
        assertTrue("Collector thread should be changed", tc.wasChanged());

        MockSplicer spl = new MockSplicer();
        assertFalse("Collector thread should not be started", tc.wasStarted());
        tc.startThreads(spl);
        assertTrue("Collector thread was not started", tc.wasStarted());

        assertFalse("Collector thread should not be stopped", tc.wasStopped());
        tc.stop();
        assertTrue("Collector thread was not stopped", tc.wasStopped());
    }

    @Test
    public void testMakeBackCompatNotMerged()
    {
        for (int i = 0; i < 3; i++) {
            final int type;
            if ((i & 0x1) == 0x1) {
                type = -1;
            } else {
                type = 2;
            }

            final int cfgId;
            if ((i & 0x2) == 0x2) {
                cfgId = -1;
            } else {
                cfgId = 3;
            }

            MockTriggerRequest req =
                new MockTriggerRequest(1, type, cfgId, 1L, 2L);
            assertTrue(String.format("makeBackwardCompatible(type %d," +
                                     " cfgId %d) failed", type, cfgId),
                       OutputThread.makeBackwardCompatible(req));
        }
    }

    @Test
    public void testMakeBackCompatEmpty()
        throws Exception
    {
        final long first = 123456789L;
        final long last = 123457890L;

        MockTriggerRequest glblReq =
            new MockTriggerRequest(1, -1, -1, first, last);

        assertFalse("makeBackwardCompatible should fail",
                   OutputThread.makeBackwardCompatible(glblReq));
    }

    @Test
    public void testMakeBackCompatIgnoredReq()
        throws Exception
    {
        final long first = 123456789L;
        final long last = 123457890L;

        MockTriggerRequest glblReq =
            new MockTriggerRequest(1, -1, -1, first, last);

        final long step = (last - first) / 3;

        ArrayList<String> logMsgs = new ArrayList<String>();

        int uid = 0;
        for (long t = first; t < last - step; t += step) {
            MockTriggerRequest req =
                new MockTriggerRequest(uid++, 1, 1, t, t + step);

            if (t > first) {
                final long substep = step / 3;
                for (long t2 = t; t2 < (t + step) - substep; t2 += substep) {
                    req.addPayload(new MockTriggerRequest(uid++, 1, 1, t2,
                                                          t2 + substep));
                }
            }

            final int num;
            if (req.getPayloads() == null) {
                num = 0;
            } else {
                num = req.getPayloads().size();
            }

            if (num != 1) {
                logMsgs.add(String.format("Not fixing %s; found %d enclosed" +
                                          " requests", req, num));
            }

            glblReq.addPayload(req);
        }

        assertFalse("makeBackwardCompatible should fail",
                   OutputThread.makeBackwardCompatible(glblReq));

        assertEquals("Bad number of log messages",
                     logMsgs.size(), appender.getNumberOfMessages());

        for (int i = 0; i < logMsgs.size(); i++) {
            assertEquals("Bad log message",
                         logMsgs.get(i), appender.getMessage(i));
        }

        appender.clear();
    }

    @Test
    public void testMakeBackCompatNoRReq()
        throws Exception
    {
        final long first = 123456789L;
        final long last = 123457890L;

        MockTriggerRequest req =
            new MockTriggerRequest(2, 1, 1, first, last);
        req.addPayload(new MockTriggerRequest(3, 1, 1, first + 100,
                                              last - 100));

        MockTriggerRequest glblReq =
            new MockTriggerRequest(1, -1, -1, first, last);
        glblReq.addPayload(req);

        assertFalse("makeBackwardCompatible should fail",
                    OutputThread.makeBackwardCompatible(glblReq));

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String errMsg =
            "Cannot find readout request for request " + glblReq;
        assertEquals("Bad log message", errMsg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testMakeBackCompat()
        throws Exception
    {
        final long first = 123456789L;
        final long last = 123457890L;

        MockTriggerRequest subReq =
            new MockTriggerRequest(1, 1, 1, first + 100, last - 100);

        MockTriggerRequest req =
            new MockTriggerRequest(2, 1, 1, first, last);
        req.addPayload(subReq);

        MockTriggerRequest glblReq =
            new MockTriggerRequest(1, -1, -1, first, last);
        glblReq.addPayload(req);

        req.setReadoutRequest(new MockReadoutRequest(1, INICE_ID));

        assertNotNull("Could not fetch subrequest readout request",
                      req.getReadoutRequest());
        assertEquals("Bad subrequest readout srcId",
                     INICE_ID,
                     req.getReadoutRequest().getSourceID().getSourceID());

        assertTrue("makeBackwardCompatible failed",
                    OutputThread.makeBackwardCompatible(glblReq));

        assertNotNull("Could not fetch subrequest readout request",
                      req.getReadoutRequest());
        assertEquals("Bad subrequest readout srcId",
                     GLOBAL_ID,
                     req.getReadoutRequest().getSourceID().getSourceID());
    }
}
