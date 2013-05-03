package icecube.daq.trigger.control;

import icecube.daq.payload.IDOMID;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.common.ITriggerAlgorithm;
import icecube.daq.trigger.common.ITriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.test.MockAlgorithm;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockBufferCache;
import icecube.daq.trigger.test.MockPayload;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockTriggerRequest;
import icecube.daq.trigger.test.MockUTCTime;

import java.util.ArrayList;
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
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);
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
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);

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
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);

        List<ITriggerAlgorithm> list = new ArrayList<ITriggerAlgorithm>();
        list.add(new MockAlgorithm("addAlgo1"));

        mgr.addTriggers(list);
    }

    @Test
    public void testAddAlgoMany()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);

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
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);

        MockAlgorithm algo = new MockAlgorithm("dup", 1, 2, 3);

        List<ITriggerAlgorithm> list = new ArrayList<ITriggerAlgorithm>();
        list.add(algo);
        list.add(algo);

        mgr.addTriggers(list);

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String msg = "Attempt to add duplicate trigger \"" +
            algo.getTriggerName() + "-" + algo.getTriggerConfigId() +
            "\" to trigger list!";
        assertEquals("Bad log message", msg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testExecuteEmpty()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);

        mgr.execute(new ArrayList(), 0);
    }

    @Test
    public void testExecuteNoSub()
    {
        MockSourceID src = new MockSourceID(INICE_ID);
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);

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
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);

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
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);

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
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);

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
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);

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
    public void testExecuteTrigReqs()
    {
        MockSourceID src = new MockSourceID(GLOBAL_ID);
        MockBufferCache bufCache = new MockBufferCache();

        TriggerManager mgr = new TriggerManager(src, bufCache, null);

        mgr.addTrigger(new MockAlgorithm("foo"));
        mgr.subscribeAlgorithms();

        List splObjs = new ArrayList();
        splObjs.add(new MockTriggerRequest(1, 2, 3, 4, 5));

        mgr.execute(splObjs, 0);

        splObjs.add(new MockTriggerRequest(2, 2, 2, 6, 7));
        splObjs.add(new MockTriggerRequest(4, 1, 7, 14, 16));

        mgr.execute(splObjs, 0);
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
