package icecube.daq.trigger.control;

import icecube.daq.payload.PayloadDestination;

import icecube.daq.trigger.ITriggerRequestPayload;

import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockPayload;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockTriggerRequest;
import icecube.daq.trigger.test.MockUTCTime;

import java.nio.ByteBuffer;

import java.io.IOException;

import java.util.zip.DataFormatException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class TriggerBagTest
    extends TestCase
{
    class BogusPayload
        extends MockPayload
    {
        public static final int INTERFACE_TYPE = -999;

        private static final int LENGTH = 1;

        BogusPayload(long timeVal)
        {
            super(timeVal);
        }

        public Object deepCopy()
        {
            throw new Error("Unimplemented");
        }

        public ByteBuffer getPayloadBacking()
        {
            throw new Error("Unimplemented");
        }

        public int getPayloadInterfaceType()
        {
            return INTERFACE_TYPE;
        }

        public int getPayloadLength()
        {
            return LENGTH;
        }

        public int getPayloadType()
        {
            throw new Error("Unimplemented");
        }

        public int writePayload(boolean writeLoaded,
                                PayloadDestination dest)
            throws IOException
        {
            throw new Error("Unimplemented");
        }

        public int writePayload(boolean writeLoaded, int offset,
                                ByteBuffer buf)
            throws IOException
        {
            throw new Error("Unimplemented");
        }
    }

    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public TriggerBagTest(String name)
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
        return new TestSuite(TriggerBagTest.class);
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

        TriggerBag bag = new TriggerBag(type, cfgId, new MockSourceID(srcId));
        assertEquals("Bad size", 0, bag.size());
        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testAddHitLoadException()
    {
        TriggerBag bag = new TriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        MockHit hit = new MockHit(12345L);
        hit.setLoadPayloadException(new IOException("Test"));
        bag.add(hit);
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Error loading payload", appender.getMessage(0));
        appender.clear();
    }

    public void testAddBogus()
    {
        TriggerBag bag = new TriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        bag.add(new BogusPayload(12345L));
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        bag.add(new BogusPayload(23456L));
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Unexpected payload type passed to TriggerBag",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testAddBogusAndHit()
    {
        TriggerBag bag = new TriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        bag.add(new BogusPayload(12345L));
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        bag.add(new MockHit(23456L));
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Unexpected payload type passed to TriggerBag",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testAddHit()
    {
        TriggerBag bag = new TriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        bag.add(new MockHit(12345L));
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        bag.add(new MockHit(23456L));
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testAddTR()
    {
        TriggerBag bag = new TriggerBag();
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

    public void testAddHitAndMerge()
    {
        TriggerBag bag = new TriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        bag.add(new MockHit(12345L));
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        bag.add(new MockHit(12345L));
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testAddTRAndMerge()
    {
        TriggerBag bag = new TriggerBag();
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

        bag.add(new MockTriggerRequest(17777L, 18888L, 3, 33, 3000));
        assertEquals("Unexpected input total",
                     3, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testSetTimeGate()
    {
        TriggerBag bag = new TriggerBag();

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());

        final long newTime = 54321L;

        bag.setTimeGate(new MockUTCTime(newTime));
        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate",
                     newTime, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testNext()
    {
        TriggerBag bag = new TriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        bag.add(new MockTriggerRequest(12345L, 20000L, 1, 11));
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
