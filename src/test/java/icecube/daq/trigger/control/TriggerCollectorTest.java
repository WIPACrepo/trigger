package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.algorithm.FlushRequest;
import icecube.daq.trigger.control.Interval;
import icecube.daq.trigger.test.MockAlgorithm;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockBufferCache;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockSplicer;

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
    private static final int INICE_ID = 4000;

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

        MockBufferCache bufCache = new MockBufferCache();

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

        MockBufferCache bufCache = new MockBufferCache();

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

        MockBufferCache bufCache = new MockBufferCache();

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
}