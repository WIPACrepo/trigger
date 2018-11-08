package icecube.daq.trigger.control;

import icecube.daq.common.MockAppender;
import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.FlushRequest;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.control.Interval;
import icecube.daq.trigger.test.MockAlgorithm;
import icecube.daq.trigger.test.MockBufferCache;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockReadoutRequest;
import icecube.daq.trigger.test.MockSplicer;
import icecube.daq.trigger.test.MockTriggerRequest;

import java.util.ArrayList;
import java.util.List;

import org.junit.*;
import static org.junit.Assert.*;

import org.apache.log4j.BasicConfigurator;

class MockCollectorThread
    implements ICollectorThread
{
    private boolean changed;
    private boolean reset;
    private boolean started;
    private boolean stopped;

    @Override
    public long getSNDAQAlertsDropped()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public int getSNDAQAlertsQueued()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public long getSNDAQAlertsSent()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public long getTotalCollected()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public long getTotalReleased()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void resetUID()
    {
        reset = true;
    }

    @Override
    public void setChanged()
    {
        changed = true;
    }

    @Override
    public void setRunNumber(int runNumber, boolean isSwitched)
    {
        // do nothing
    }

    @Override
    public void start(Splicer splicer)
    {
        started = true;
    }

    @Override
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

    MyCollector(int srcId, List<ITriggerAlgorithm> algorithms,
                DAQComponentOutputProcess outputEngine,
                IByteBufferCache outCache,
                IMonitoringDataManager multiDataMgr,
                SubscriptionManager subMgr)
    {
        super(srcId, algorithms, outputEngine, outCache, multiDataMgr, subMgr);
    }

    @Override
    public ICollectorThread createCollectorThread(String name, int srcId,
                                                  List<ITriggerAlgorithm> algo,
                                                  IMonitoringDataManager mdm,
                                                  IOutputThread outThrd,
                                                  SubscriptionManager subMgr)
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
    }

    @Test
    public void testCreate()
    {
        try {
            new TriggerCollector(INICE_ID, null, null, null, null, null);
            fail("Constructor should fail with null algorithm list");
        } catch (Error err) {
            // expect this to fail
        }

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();

        try {
            new TriggerCollector(INICE_ID, algorithms, null, null, null, null);
            fail("Constructor should fail with empty algorithm list");
        } catch (Error err) {
            // expect this to fail
        }

        algorithms.add(new MockAlgorithm("foo"));

        try {
            new TriggerCollector(INICE_ID, algorithms, null, null, null, null);
            fail("Constructor should fail with null output process");
        } catch (Error err) {
            // expect this to fail
        }

        MockOutputProcess out = new MockOutputProcess();

        try {
            new TriggerCollector(INICE_ID, algorithms, out, null, null, null);
            fail("Constructor should fail with null output cache");
        } catch (Error err) {
            // expect this to fail
        }

        MockBufferCache bufCache = new MockBufferCache("foo");

        TriggerCollector tc =
            new TriggerCollector(INICE_ID, algorithms, out, bufCache, null,
                                 null);
        assertFalse("New collector is stopped", tc.isStopped());
        assertEquals("New collector queue should be empty",
                     0L, tc.getNumQueued());
    }

    @Test
    public void testNullSplicer()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("foo");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockOutputProcess out = new MockOutputProcess();

        MyCollector tc =
            new MyCollector(INICE_ID, algorithms, out, bufCache, null, null);

        tc.startThreads(null);

        final String nullMsg = "Splicer cannot be null";
        appender.assertLogMessage(nullMsg);
        appender.assertNoLogMessages();
    }

    @Test
    public void testAPI()
    {
        MockAlgorithm fooAlgo = new MockAlgorithm("foo");

        ArrayList<ITriggerAlgorithm> algorithms =
            new ArrayList<ITriggerAlgorithm>();
        algorithms.add(fooAlgo);

        MockBufferCache bufCache = new MockBufferCache("foo");

        MockOutputProcess out = new MockOutputProcess();

        MyCollector tc =
            new MyCollector(INICE_ID, algorithms, out, bufCache, null, null);

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
