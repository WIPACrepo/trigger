package icecube.daq.trigger.control;

import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockReadoutRequest;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockTriggerRequest;
import icecube.daq.trigger.test.MockUTCTime;

import java.io.IOException;
import java.util.Vector;
import java.util.zip.DataFormatException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

class MockHitRequest
    extends MockHit
    implements ITriggerRequestPayload
{
    private IReadoutRequest rdoutReq;

    public MockHitRequest(long time)
    {
        super(time);
    }

    public IUTCTime getFirstTimeUTC()
    {
        return getHitTimeUTC();
    }

    public Vector getHitList()
    {
        throw new Error("Unimplemented");
    }

    public IUTCTime getLastTimeUTC()
    {
        return getHitTimeUTC();
    }

    public Vector getPayloads()
        throws IOException, DataFormatException
    {
        throw new Error("Unimplemented");
    }

    public IReadoutRequest getReadoutRequest()
    {
        return rdoutReq;
    }

    public int getUID()
    {
        throw new Error("Unimplemented");
    }

    public void setReadoutRequest(IReadoutRequest rdoutReq)
    {
        this.rdoutReq = rdoutReq;
    }
}

public class GlobalTriggerBagTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public GlobalTriggerBagTest(String name)
    {
        super(name);
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        appender.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);
    }

    public static Test suite()
    {
        return new TestSuite(GlobalTriggerBagTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testCreateAllParams()
    {
        final int type = 123;
        final int cfgId = 456;
        final int srcId = 789;

        GlobalTriggerBag bag =
            new GlobalTriggerBag(type, cfgId, new MockSourceID(srcId));
        assertEquals("Bad size", 0, bag.size());
        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testAddLoadException()
    {
        GlobalTriggerBag bag = new GlobalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        MockTriggerRequest tr = new MockTriggerRequest(12345L, 23456L);
        tr.setLoadPayloadException(new IOException("Test"));
        bag.add(tr);
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Error loading currentPayload", appender.getMessage(0));
        appender.clear();
    }

    public void testAdd()
    {
        GlobalTriggerBag bag = new GlobalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        bag.add(new MockTriggerRequest(12345L, 20000L, 1, 11));
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        bag.add(new MockTriggerRequest(23456L, 30000L, 2, 22));
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testAddHits()
    {
        GlobalTriggerBag bag = new GlobalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        MockHitRequest hReq;

        hReq = new MockHitRequest(12345L);
        hReq.setSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);

        bag.add(hReq);
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        hReq = new MockHitRequest(12345L);
        hReq.setSourceID(SourceIdRegistry.ICETOP_TRIGGER_SOURCE_ID);

        bag.add(hReq);
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testAddTRAndMerge()
    {
        GlobalTriggerBag bag = new GlobalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        MockTriggerRequest tr =
            new MockTriggerRequest(12345L, 20000L, 1, 11, 1000);
        tr.addPayload(new MockHit(13579L));

        bag.add(tr);
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        bag.add(new MockTriggerRequest(19999L, 23456L, 2, 22, 2000));
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        bag.add(new MockTriggerRequest(21000L, 22000L, 3, 33, 3000));
        assertEquals("Unexpected input total",
                     3, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testSetTimeGate()
    {
        GlobalTriggerBag bag = new GlobalTriggerBag();

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());

        bag.setTimeGate(new MockUTCTime(12345L));
        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate",
                     12345L, bag.getTimeGate().getUTCTimeAsLong());

        final int window = 1000;

        bag.setMaxTimeGateWindow(window);
        bag.setTimeGate(new MockUTCTime(12345L));
        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate",
                     12345L + (window * 10),
                     bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testNext()
    {
        GlobalTriggerBag bag = new GlobalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.size());

        MockTriggerRequest tr = new MockTriggerRequest(12345L, 20000L, 1, 11);
        tr.setSourceID(666);
        tr.setReadoutRequest(new MockReadoutRequest());

        bag.add(tr);
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        bag.add(new MockTriggerRequest(23456L, 30000L, 2, 22));
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());
        assertEquals("Unexpected output total",
                     0, bag.getMonitor().getOutputCountTotal());

        bag.setTimeGate(new MockUTCTime(10000L));
        assertFalse("Didn't expect to have a 'next' trigger", bag.hasNext());
        assertNull("Didn't expect to get next trigger", bag.next());

        bag.setTimeGate(new MockUTCTime(16666L));
        assertFalse("Didn't expect to have a 'next' trigger", bag.hasNext());
        assertNull("Didn't expect to get next trigger", bag.next());
        assertEquals("Unexpected output total",
                     0, bag.getMonitor().getOutputCountTotal());

        bag.setTimeGate(new MockUTCTime(20001L));
        assertTrue("Expected to have a 'next' trigger", bag.hasNext());

        ITriggerRequestPayload trp = bag.next();
        assertNotNull("Expected to get next trigger", trp);
        assertEquals("Unexpected output total",
                     1, bag.getMonitor().getOutputCountTotal());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
