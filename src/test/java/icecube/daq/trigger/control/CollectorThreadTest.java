package icecube.daq.trigger.control;

import icecube.daq.common.MockAppender;
import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.FlushRequest;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.exceptions.MultiplicityDataException;
import icecube.daq.trigger.test.MockAlgorithm;
import icecube.daq.trigger.test.MockBufferCache;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockSubscriber;
import icecube.daq.trigger.test.MockTriggerRequest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.junit.*;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

class MockOutputThread
    implements IOutputThread
{
    private boolean stopped;

    private boolean calledIsStopped;

    private List<ITriggerRequestPayload> pushed =
        new ArrayList<ITriggerRequestPayload>();

    public boolean calledIsStopped()
    {
        return calledIsStopped;
    }

    public void clear()
    {
        stopped = false;
        calledIsStopped = false;
        pushed.clear();
    }

    @Override
    public long getNumQueued()
    {
        return pushed.size();
    }

    public ITriggerRequestPayload getPushed(int idx)
    {
        if (pushed.size() <= idx) {
            throw new Error("Cannot return request #" + idx + ", only " +
                            pushed.size() + " available");
        }

        return pushed.get(idx);
    }

    @Override
    public boolean isStopped()
    {
        calledIsStopped = true;

        return stopped;
    }

    @Override
    public void notifyThread()
    {
        // do nothing
    }

    @Override
    public void push(ITriggerRequestPayload req)
    {
        pushed.add(req);
    }

    @Override
    public void resetUID()
    {
        // do nothing
    }

    @Override
    public void start(Splicer splicer)
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void stop()
    {
        stopped = true;
    }
}

class MockDataManager
    implements IMonitoringDataManager
{
    private boolean throwAddEx;
    private boolean throwResetEx;
    private boolean throwSendEx;
    private boolean doReset;

    private boolean wasAdded;
    private boolean wasReset;
    private boolean wasSent;

    @Override
    public void add(ITriggerRequestPayload req)
        throws MultiplicityDataException
    {
        if (throwAddEx) {
            throw new MultiplicityDataException("Bad add");
        }

        wasAdded = true;
    }

    @Override
    public void reset()
        throws MultiplicityDataException
    {
        if (throwResetEx) {
            throw new MultiplicityDataException("Bad reset");
        }

        wasReset = true;
    }

    @Override
    public boolean sendFinal()
        throws MultiplicityDataException
    {
        if (throwSendEx) {
            throw new MultiplicityDataException("Bad send");
        }

        wasSent = true;

        return doReset;
    }

    @Override
    public boolean sendSingleBin(boolean isFinal)
        throws MultiplicityDataException
    {
        return false;
    }

    public void setDoReset()
    {
        doReset = true;
    }

    public void throwResetException()
    {
        throwResetEx = true;
    }

    public void throwSendException()
    {
        throwSendEx = true;
    }

    public void throwAddException()
    {
        throwAddEx = true;
    }

    public boolean wasAdded()
    {
        return wasAdded;
    }

    public boolean wasReset()
    {
        return wasReset;
    }

    public boolean wasSent()
    {
        return wasSent;
    }
}

class MockSubscriptionManager
    implements SubscriptionManager
{
    @Override
    public void subscribeAll()
    {
        // do nothing
    }

    @Override
    public void unsubscribeAll()
    {
        // do nothing
    }
}

public class CollectorThreadTest
{
    private static final int INICE_ID =
        SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;
    private static final int GLOBAL_ID =
        SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID;
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    private MockOutputThread outThrd = new MockOutputThread();

    private void checkDataMgr(MockDataManager mgr, boolean added, boolean sent,
                              boolean reset)
    {
        checkDMValue("added", added, mgr.wasAdded());
        checkDMValue("sent", sent, mgr.wasSent());
        checkDMValue("reset", reset, mgr.wasReset());
    }

    private void checkDMValue(String name, boolean expVal, boolean val)
    {
        final String prefix = "MonitoringData was ";
        if (expVal) {
            assertTrue(prefix + "not " + name, val);
        } else {
            assertFalse(prefix + name, val);
        }
    }

    @Before
    public void setUp()
        throws Exception
    {
        outThrd.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);

        // initialize SNDAQ ZMQ address to nonsense
        System.getProperties().setProperty(SNDAQAlerter.PROPERTY, ":12345");
    }

    @After
    public void tearDown()
        throws Exception
    {
        // remove SNDAQ ZMQ address
        System.clearProperty(SNDAQAlerter.PROPERTY);

        appender.assertNoLogMessages();

        assertEquals("Found unexpected output requests", 0L,
                     outThrd.getNumQueued());
    }

    @Test
    public void testCreate()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("creAlgo");
        fooAlgo.setSubscriber(new MockSubscriber());

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        CollectorThread ct =
            new CollectorThread("cre", INICE_ID, algorithms, null, outThrd,
                                null);
    }

    @Test
    public void testAPI()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("apiAlgo");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        CollectorThread ct =
            new CollectorThread("api", INICE_ID, algorithms, null, outThrd,
                                null);
        ct.resetUID();
    }

    @Test
    public void testFindInterval()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("findAlgo");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        CollectorThread ct =
            new CollectorThread("find", INICE_ID, algorithms, null, outThrd,
                                null);
        assertNull("Found unexpected interval", ct.findInterval());

        final long start = 1;
        final long end = 2;
        fooAlgo.addInterval(start, end);

        Interval ival = ct.findInterval();
        assertNotNull("Should not have null interval", ival);
        assertEquals("Bad interval start", start, ival.start);
        assertEquals("Bad interval start", end, ival.end);
    }

    @Test
    public void testPushIITrigger()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("pushIIAlgo");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        Interval ival = new Interval(4L, 5L);

        CollectorThread ct =
            new CollectorThread("pushII", INICE_ID, algorithms, null, outThrd,
                                null);
        ct.pushTrigger(new MockTriggerRequest(1, 2, 3, ival.start, ival.end));

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        ITriggerRequestPayload req = outThrd.getPushed(0);
        if (req.getFirstTimeUTC().longValue() != ival.start ||
            req.getLastTimeUTC().longValue() != ival.end)
        {
            fail("Bad " + req + " from " + ival);
        }
        outThrd.clear();
    }

    @Test
    public void testPushGTriggerBadReq()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("pushGAlgo");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();
        mgr.throwAddException();

        Interval ival = new Interval(4L, 5L);

        CollectorThread ct =
            new CollectorThread("pushG", GLOBAL_ID, algorithms, mgr, outThrd,
                                null);
        ct.pushTrigger(new MockTriggerRequest(1, 2, 3, ival.start, ival.end));
        checkDataMgr(mgr, false, false, false);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        ITriggerRequestPayload req = outThrd.getPushed(0);
        if (req.getFirstTimeUTC().longValue() != ival.start ||
            req.getLastTimeUTC().longValue() != ival.end)
        {
            fail("Bad " + req + " from " + ival);
        }
        outThrd.clear();

        final String msg = "Cannot add multiplicity data";
        appender.assertLogMessage(msg);
        appender.assertNoLogMessages();
    }

    @Test
    public void testPushGTrigger()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("pushGAlgo");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        Interval ival = new Interval(4L, 5L);

        CollectorThread ct =
            new CollectorThread("pushG", GLOBAL_ID, algorithms, mgr, outThrd,
                                null);
        ct.pushTrigger(new MockTriggerRequest(1, 2, 3, ival.start, ival.end));
        checkDataMgr(mgr, true, false, false);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        ITriggerRequestPayload req = outThrd.getPushed(0);
        if (req.getFirstTimeUTC().longValue() != ival.start ||
            req.getLastTimeUTC().longValue() != ival.end)
        {
            fail("Bad " + req + " from " + ival);
        }
        outThrd.clear();
    }

    @Test
    public void testPushGTriggerSendFirst()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("pushGAlgo");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();
        mgr.throwSendException();

        Interval ival = new Interval(4L, 5L);

        CollectorThread ct =
            new CollectorThread("pushG", GLOBAL_ID, algorithms, mgr, outThrd,
                                null);
        ct.pushTrigger(new MockTriggerRequest(0, 2, 3, ival.start, ival.end));
        checkDataMgr(mgr, true, false, false);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        ITriggerRequestPayload req = outThrd.getPushed(0);
        if (req.getFirstTimeUTC().longValue() != ival.start ||
            req.getLastTimeUTC().longValue() != ival.end)
        {
            fail("Bad " + req + " from " + ival);
        }
        outThrd.clear();

        appender.clear();
    }

    @Test
    public void testPushGTriggerSend()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("pushGAlgo");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        Interval ival = new Interval(4L, 5L);

        CollectorThread ct =
            new CollectorThread("pushG", GLOBAL_ID, algorithms, mgr, outThrd,
                                null);
        ct.setRunNumber(12345, true);

        ct.pushTrigger(new MockTriggerRequest(0, 2, 3, ival.start, ival.end));
        checkDataMgr(mgr, true, true, true);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        ITriggerRequestPayload req = outThrd.getPushed(0);
        if (req.getFirstTimeUTC().longValue() != ival.start ||
            req.getLastTimeUTC().longValue() != ival.end)
        {
            fail("Bad " + req + " from " + ival);
        }
        outThrd.clear();
    }

    @Test
    public void testPushGTriggerResetBad()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("pushGAlgo");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();
        mgr.setDoReset();
        mgr.throwResetException();

        Interval ival = new Interval(4L, 5L);

        CollectorThread ct =
            new CollectorThread("pushG", GLOBAL_ID, algorithms, mgr, outThrd,
                                null);
        ct.setRunNumber(12345, true);

        ct.pushTrigger(new MockTriggerRequest(0, 2, 3, ival.start, ival.end));
        checkDataMgr(mgr, true, true, false);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        ITriggerRequestPayload req = outThrd.getPushed(0);
        if (req.getFirstTimeUTC().longValue() != ival.start ||
            req.getLastTimeUTC().longValue() != ival.end)
        {
            fail("Bad " + req + " from " + ival);
        }
        outThrd.clear();

        final String msg = "Failed to reset multiplicity data";
        appender.assertLogMessage(msg);
        appender.assertNoLogMessages();
    }

    @Test
    public void testSendRequestsEmpty()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("sendReqEmpty");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("sendReqEmpty", INICE_ID, algorithms, mgr,
                                outThrd, null);

        Interval ival = new Interval(10, 30);

        List<ITriggerRequestPayload> requests =
            new ArrayList<ITriggerRequestPayload>();
        ct.addRequests(ival, requests);
        ct.sendRequests(ival, requests);

        assertFalse("Should not have added anything to data manager",
                    mgr.wasAdded());

        final String msg = "No requests found for interval " + ival;
        appender.assertLogMessage(msg);
        appender.assertNoLogMessages();
    }

    @Test
    public void testSendRequestsIIOne()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("sendReqII1");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("sendReqII1", INICE_ID, algorithms, mgr,
                                outThrd, null);

        Interval ival0 = new Interval(11, 15);
        fooAlgo.addInterval(ival0);

        Interval rval = new Interval(10, 30);

        List<ITriggerRequestPayload> requests =
            new ArrayList<ITriggerRequestPayload>();
        ct.addRequests(rval, requests);
        ct.sendRequests(rval, requests);

        assertFalse("Should not have added request to data manager",
                   mgr.wasAdded());

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        ITriggerRequestPayload req = outThrd.getPushed(0);
        if (req.getFirstTimeUTC().longValue() != ival0.start ||
            req.getLastTimeUTC().longValue() != ival0.end)
        {
            fail("Bad " + req + " from " + ival0);
        }
        outThrd.clear();
    }

    @Test
    public void testSendRequestsGTOne()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("sendReqGT1");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("sendReqGT1", GLOBAL_ID, algorithms, mgr,
                                outThrd, null);

        Interval ival0 = new Interval(11, 15);
        fooAlgo.addInterval(ival0);

        Interval rval = new Interval(10, 30);

        List<ITriggerRequestPayload> requests =
            new ArrayList<ITriggerRequestPayload>();
        ct.addRequests(rval, requests);
        ct.sendRequests(rval, requests);
        checkDataMgr(mgr, true, false, false);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        ITriggerRequestPayload req = outThrd.getPushed(0);
        if (req.getFirstTimeUTC().longValue() != ival0.start ||
            req.getLastTimeUTC().longValue() != ival0.end)
        {
            fail("Bad " + req + " from " + ival0);
        }
        outThrd.clear();
    }

    @Test
    public void testSendRequestsIIMany()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("sendReqIIMany");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("sendReqIIMany", INICE_ID, algorithms, mgr,
                                outThrd, null);

        Interval ival0 = new Interval(11, 15);
        fooAlgo.addInterval(ival0);

        Interval ival1 = new Interval(18, 25);
        fooAlgo.addInterval(ival1);

        Interval rval = new Interval(10, 30);

        List<ITriggerRequestPayload> requests =
            new ArrayList<ITriggerRequestPayload>();
        ct.addRequests(rval, requests);
        ct.sendRequests(rval, requests);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        assertFalse("Should not have added request to data manager",
                   mgr.wasAdded());

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        ITriggerRequestPayload req = outThrd.getPushed(0);
        if (req.getFirstTimeUTC().longValue() != rval.start ||
            req.getLastTimeUTC().longValue() != rval.end)
        {
            fail("Bad " + req + " from " + ival0);
        }
        outThrd.clear();
    }

    @Test
    public void testSendRequestsGTMany()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("sendReqGTMany");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("sendReqMany", GLOBAL_ID, algorithms, mgr,
                                outThrd, null);

        Interval ival0 = new Interval(11, 15);
        fooAlgo.addInterval(ival0);

        Interval ival1 = new Interval(18, 25);
        fooAlgo.addInterval(ival1);

        Interval rval = new Interval(10, 30);

        List<ITriggerRequestPayload> requests =
            new ArrayList<ITriggerRequestPayload>();
        ct.addRequests(rval, requests);
        ct.sendRequests(rval, requests);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        assertTrue("Should have added request to data manager",
                   mgr.wasAdded());
        assertFalse("Should not have sent data", mgr.wasSent());

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        ITriggerRequestPayload req = outThrd.getPushed(0);
        if (req.getFirstTimeUTC().longValue() != rval.start ||
            req.getLastTimeUTC().longValue() != rval.end)
        {
            fail("Bad " + req + " from " + ival0);
        }
        outThrd.clear();
    }

    @Test
    public void testSetChangedNoAlgo()
    {
        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("setChgNoAlgo", INICE_ID, algorithms, mgr,
                                outThrd, null);

        ct.setChanged();
    }

    @Test
    public void testSetChangedNoFlush()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("setChgNoFlush");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("setChgNoFlush", INICE_ID, algorithms, mgr,
                                outThrd, null);

        ct.setChanged();
    }

    @Test
    public void testSetChangedFlush()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("setChgFlush");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("setChgFlush", INICE_ID, algorithms, mgr,
                                outThrd, null);

        fooAlgo.setSawFlush();

        ct.setChanged();
    }

    public void runOne(String name, long oldStart, long oldEnd,
                       long newStart, long newEnd,
                       Interval[] reqList)
    {
        MockRunAlgorithm fooAlgo = new MockRunAlgorithm(name + "Algo");
        fooAlgo.setSubscriber(new MockSubscriber());

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        fooAlgo.addInterval(oldStart, oldEnd);
        fooAlgo.addInterval(newStart, newEnd);

        fooAlgo.setFetchAll(false);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache(name + "BufCache");

        MockDataManager mgr = new MockDataManager();

        MockSubscriptionManager subMgr = new MockSubscriptionManager();

        CollectorThread ct =
            new CollectorThread(name, INICE_ID, algorithms, mgr, outThrd,
                                subMgr);
        ct.setRunNumber(1234, false);

        fooAlgo.setSawFlush();

        ct.setChanged();
        ct.run();

        assertEquals("Bad number of " + name + " requests queued",
                     (long) reqList.length, outThrd.getNumQueued());
        for (int i = 0; i < reqList.length; i++) {
            ITriggerRequestPayload req = outThrd.getPushed(i);
            if (req.getFirstTimeUTC().longValue() != reqList[i].start ||
                req.getLastTimeUTC().longValue() != reqList[i].end)
            {
                fail("Bad " + name + " " + req + " from " + reqList[i]);
            }
        }
        outThrd.clear();

        if (reqList.length == 0) {
            final String expMsg = "New interval [" + newStart + "-" + newEnd +
                "] precedes old interval [" + oldStart + "-" + oldEnd + "]";
            appender.assertLogMessage(expMsg);
            appender.assertNoLogMessages();
        }
    }

    // This test broke when SVN rev 15465 fixed an end-of-run race condition
    // @Test
    public void testRun()
    {
        long os = 400;
        long oe = 600;

        for (int s = 0; s < 6; s++) {
            String sd;
            long ns;
            switch (s) {
            case 0:
                sd = "ns < os";
                ns = os - 50;
                break;
            case 1:
                sd = "ns == os";
                ns = os;
                break;
            case 2:
                sd = "ns > os";
                ns = os + 50;
                break;
            case 3:
                sd = "ns < oe";
                ns = oe - 50;
                break;
            case 4:
                sd = "ns == oe";
                ns = oe;
                break;
            case 5:
                sd = "ns > oe";
                ns = oe + 50;
                break;
            default:
                throw new Error("s#" + s + " not handled");
            }

            for (int e = 0; e < 6; e++) {
                boolean invalid = false;
                String ed;
                long ne;
                switch (e) {
                case 0:
                    ed = "ne < os";
                    if (s == 1 || s == 2 || s == 4 || s == 5) {
                        ne = Long.MAX_VALUE;
                        invalid = true;
                    } else {
                        ne = os - 25;
                        if (s == 3) {
                            ns = os - 125;
                        }
                    }
                    break;
                case 1:
                    ed = "ne == os";
                    if (s == 2 || s == 4 || s == 5) {
                        ne = Long.MAX_VALUE;
                        invalid = true;
                    } else {
                        ne = os;
                        if (s == 3) {
                            ns = os - 25;
                        }
                    }
                    break;
                case 2:
                    ed = "ne > os";
                    ne = os + 75;
                    if (s == 3) {
                        ns = ne - 50;
                    } else if (s == 4 || s == 5) {
                        ne = ns + 25;
                    }
                    break;
                case 3:
                    ed = "ne < oe";
                    if (s == 4 || s == 5) {
                        ne = Long.MAX_VALUE;
                        invalid = true;
                    } else {
                        ne = oe - 25;
                    }
                    break;
                case 4:
                    ed = "ne == oe";
                    if (s == 5) {
                        ne = Long.MAX_VALUE;
                        invalid = true;
                    } else {
                        ne = oe;
                    }
                    break;
                case 5:
                    ed = "ne > oe";
                    ne = oe + 75;
                    break;
                default:
                    throw new Error("e#" + e + " not handled");
                }

                if (invalid) {
                    continue;
                }

                Interval[] ilist = buildIntervalList(os, oe, ns, ne);

                //System.err.println("================= " + sd + ", " + ed +
                //                   " :: old " + os + "-" + oe + " new " + ns +
                //                   "-" + ne + " :: " + dumpIntervals(ilist));
                runOne(sd + ", " + ed, os, oe, ns, ne, ilist);
            }
        }
    }

    private static String dumpIntervals(Interval[] list)
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < list.length; i++) {
            if (buf.length() > 0) {
                buf.append(' ');
            }

            buf.append('[').append(list[i].start).append('-').
                append(list[i].end).append(']');
        }

        return buf.toString();
    }

    private static Interval[] buildIntervalList(long os, long oe,
                                                long ns, long ne)
    {
        if (ne < os) {
            return new Interval[0];
        } else if (oe < ns) {
            return new Interval[] {
                new Interval(os, oe),
                new Interval(ns, ne),
            };
        }

        long ms;
        if (os < ns) {
            ms = os;
        } else {
            ms = ns;
        }

        long me;
        if (oe > ne) {
            me = oe;
        } else {
            me = ne;
        }

        return new Interval[] { new Interval(ms, me), };
    }
}

class MockRunAlgorithm
    extends MockAlgorithm
{
    private boolean flushed;

    public MockRunAlgorithm(String name)
    {
        super(name);
    }

    public int release(Interval interval,
                       List<ITriggerRequestPayload> released)
    {
        final int len = released.size();

        int rtnval = super.release(interval, released);

        if (!flushed && getNumberOfIntervals() == 0) {
            addInterval(FlushRequest.FLUSH_TIME, FlushRequest.FLUSH_TIME);
            flushed = true;
        }

        return rtnval;
    }
}
