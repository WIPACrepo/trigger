package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.exceptions.MultiplicityDataException;
import icecube.daq.trigger.test.MockAlgorithm;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockBufferCache;
import icecube.daq.trigger.test.MockOutputProcess;
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

    public long getNumQueued()
    {
        return pushed.size();
    }

    public boolean isStopped()
    {
        calledIsStopped = true;

        return stopped;
    }

    public void notifyThread()
    {
        throw new Error("Unimplemented");
    }

    public void push(ITriggerRequestPayload req)
    {
        pushed.add(req);
    }

    public void resetUID()
    {
        // do nothing
    }

    public void start(Splicer splicer)
    {
        throw new Error("Unimplemented");
    }

    public void stop()
    {
        throw new Error("Unimplemented");
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

    public void add(ITriggerRequestPayload req)
        throws MultiplicityDataException
    {
        if (throwAddEx) {
            throw new MultiplicityDataException("Bad add");
        }

        wasAdded = true;
    }

    public void reset()
        throws MultiplicityDataException
    {
        if (throwResetEx) {
            throw new MultiplicityDataException("Bad reset");
        }

        wasReset = true;
    }

    public boolean send()
        throws MultiplicityDataException
    {
        if (throwSendEx) {
            throw new MultiplicityDataException("Bad send");
        }

        wasSent = true;

        return doReset;
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

public class CollectorThreadTest
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
        MockAlgorithm fooAlgo = new MockAlgorithm("creAlgo");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        CollectorThread ct =
            new CollectorThread("cre", INICE_ID, algorithms, null, null);
    }

    @Test
    public void testAPI()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("apiAlgo");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockOutputThread outThrd = new MockOutputThread();

        CollectorThread ct =
            new CollectorThread("api", INICE_ID, algorithms, null, outThrd);
        ct.resetUID();
    }

    @Test
    public void testFindInterval()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("findAlgo");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        CollectorThread ct =
            new CollectorThread("find", INICE_ID, algorithms, null, null);
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

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockOutputThread outThrd = new MockOutputThread();

        CollectorThread ct =
            new CollectorThread("pushII", INICE_ID, algorithms, null, outThrd);
        ct.pushTrigger(new MockTriggerRequest(1, 2, 3, 4L, 5L));
    }

    @Test
    public void testPushGTriggerBadReq()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("pushGAlgo");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();
        mgr.throwAddException();

        MockOutputThread outThrd = new MockOutputThread();

        CollectorThread ct =
            new CollectorThread("pushG", GLOBAL_ID, algorithms, mgr, outThrd);
        ct.pushTrigger(new MockTriggerRequest(1, 2, 3, 4L, 5L));
        assertFalse("Request was not added", mgr.wasAdded());
        assertFalse("Request was not sent", mgr.wasSent());
        assertFalse("Request was not reset", mgr.wasReset());

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "Cannot add multiplicity data";
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testPushGTrigger()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("pushGAlgo");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        MockOutputThread outThrd = new MockOutputThread();

        CollectorThread ct =
            new CollectorThread("pushG", GLOBAL_ID, algorithms, mgr, outThrd);
        ct.pushTrigger(new MockTriggerRequest(1, 2, 3, 4L, 5L));
        assertTrue("Request was added", mgr.wasAdded());
        assertFalse("Request was not sent", mgr.wasSent());
        assertFalse("Request was not reset", mgr.wasReset());
    }

    @Test
    public void testPushGTriggerSendBad()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("pushGAlgo");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();
        mgr.throwSendException();

        MockOutputThread outThrd = new MockOutputThread();

        CollectorThread ct =
            new CollectorThread("pushG", GLOBAL_ID, algorithms, mgr, outThrd);
        ct.pushTrigger(new MockTriggerRequest(0, 2, 3, 4L, 5L));
        assertTrue("Request was added", mgr.wasAdded());
        assertFalse("Request was not sent", mgr.wasSent());
        assertTrue("Request was reset", mgr.wasReset());

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "Failed to send multiplicity data";
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testPushGTriggerSend()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("pushGAlgo");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        MockOutputThread outThrd = new MockOutputThread();

        CollectorThread ct =
            new CollectorThread("pushG", GLOBAL_ID, algorithms, mgr, outThrd);
        ct.pushTrigger(new MockTriggerRequest(0, 2, 3, 4L, 5L));
        assertTrue("Request was added", mgr.wasAdded());
        assertTrue("Request was sent", mgr.wasSent());
        assertFalse("Request was not reset", mgr.wasReset());
    }

    @Test
    public void testPushGTriggerResetBad()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("pushGAlgo");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();
        mgr.setDoReset();
        mgr.throwResetException();

        MockOutputThread outThrd = new MockOutputThread();

        CollectorThread ct =
            new CollectorThread("pushG", GLOBAL_ID, algorithms, mgr, outThrd);
        ct.pushTrigger(new MockTriggerRequest(0, 2, 3, 4L, 5L));
        assertTrue("Request was added", mgr.wasAdded());
        assertTrue("Request was sent", mgr.wasSent());
        assertFalse("Request was not reset", mgr.wasReset());

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "Failed to reset multiplicity data";
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testSendRequestsEmpty()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("sendReqEmpty");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("sendReqEmpty", INICE_ID, algorithms, mgr,
                                null);

        Interval ival = new Interval(10, 30);

        List<ITriggerRequestPayload> requests =
            new ArrayList<ITriggerRequestPayload>();
        ct.addRequests(ival, requests);
        ct.sendRequests(ival, requests);

        assertFalse("Should not have added anything to data manager",
                    mgr.wasAdded());

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "No requests found for interval " + ival;
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testSendRequestsIIOne()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("sendReqII1");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        MockOutputThread outThrd = new MockOutputThread();

        CollectorThread ct =
            new CollectorThread("sendReqII1", INICE_ID, algorithms, mgr,
                                outThrd);

        fooAlgo.addInterval(11, 15);

        Interval ival = new Interval(10, 30);

        List<ITriggerRequestPayload> requests =
            new ArrayList<ITriggerRequestPayload>();
        ct.addRequests(ival, requests);
        ct.sendRequests(ival, requests);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        assertFalse("Should not have added request to data manager",
                   mgr.wasAdded());
    }

    @Test
    public void testSendRequestsGTOne()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("sendReqGT1");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        MockOutputThread outThrd = new MockOutputThread();

        CollectorThread ct =
            new CollectorThread("sendReqGT1", GLOBAL_ID, algorithms, mgr,
                                outThrd);

        fooAlgo.addInterval(11, 15);

        Interval ival = new Interval(10, 30);

        List<ITriggerRequestPayload> requests =
            new ArrayList<ITriggerRequestPayload>();
        ct.addRequests(ival, requests);
        ct.sendRequests(ival, requests);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        assertTrue("Should have added request to data manager",
                   mgr.wasAdded());
        assertTrue("Should have sent data", mgr.wasSent());
    }

    @Test
    public void testSendRequestsIIMany()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("sendReqIIMany");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        MockOutputThread outThrd = new MockOutputThread();

        CollectorThread ct =
            new CollectorThread("sendReqIIMany", INICE_ID, algorithms, mgr,
                                outThrd);

        fooAlgo.addInterval(11, 15);
        fooAlgo.addInterval(18, 25);

        Interval ival = new Interval(10, 30);

        List<ITriggerRequestPayload> requests =
            new ArrayList<ITriggerRequestPayload>();
        ct.addRequests(ival, requests);
        ct.sendRequests(ival, requests);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        assertFalse("Should not have added request to data manager",
                   mgr.wasAdded());
    }

    @Test
    public void testSendRequestsGTMany()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("sendReqGTMany");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        MockOutputThread outThrd = new MockOutputThread();

        CollectorThread ct =
            new CollectorThread("sendReqMany", GLOBAL_ID, algorithms, mgr,
                                outThrd);

        fooAlgo.addInterval(11, 15);
        fooAlgo.addInterval(18, 25);

        Interval ival = new Interval(10, 30);

        List<ITriggerRequestPayload> requests =
            new ArrayList<ITriggerRequestPayload>();
        ct.addRequests(ival, requests);
        ct.sendRequests(ival, requests);

        assertEquals("Should be one request queued",
                     1L, outThrd.getNumQueued());
        assertTrue("Should have added request to data manager",
                   mgr.wasAdded());
        assertFalse("Should not have sent data", mgr.wasSent());

        assertEquals("Bad number of log messages",
                     2, appender.getNumberOfMessages());
        for (int i = 0; i < 2; i++) {
            final String msg = (String) appender.getMessage(i);
            assertTrue("Bad log message \"" + msg + "\"",
                       msg.startsWith("No readout requests found in "));
        }
        appender.clear();

    }

    @Test
    public void testSetChangedNoAlgo()
    {
        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("setChgNoAlgo", INICE_ID, algorithms, mgr,
                                null);

        ct.setChanged();
    }

    @Test
    public void testSetChangedNoFlush()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("setChgNoFlush");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("setChgNoFlush", INICE_ID, algorithms, mgr,
                                null);

        ct.setChanged();
    }

    @Test
    public void testSetChangedFlush()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("setChgFlush");

        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();
        algorithms.add(fooAlgo);

        MockOutputProcess out = new MockOutputProcess();

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockDataManager mgr = new MockDataManager();

        CollectorThread ct =
            new CollectorThread("setChgFlush", INICE_ID, algorithms, mgr,
                                null);

        fooAlgo.setSawFlush();

        ct.setChanged();
    }
}
