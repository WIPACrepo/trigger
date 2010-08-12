package icecube.daq.trigger.control;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayloadDestination;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockPayload;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockTriggerRequest;
import icecube.daq.trigger.test.MockUTCTime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class SimpleTriggerBagTest
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

        public void setCache(IByteBufferCache cache)
        {
            throw new Error("Unimplemented");
        }

        public int writePayload(boolean writeLoaded,
                                IPayloadDestination dest)
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

    public SimpleTriggerBagTest(String name)
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
        return new TestSuite(SimpleTriggerBagTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testAddHitLoadException()
    {
        SimpleTriggerBag bag = new SimpleTriggerBag();
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

    public void testAddHit()
    {
        SimpleTriggerBag bag = new SimpleTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        bag.add(new MockHit(12345L));
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        bag.add(new MockHit(23456L));
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
    }

    public void testAddTR()
    {
        SimpleTriggerBag bag = new SimpleTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        bag.add(new MockTriggerRequest(12345L, 20000L, 1, 11));
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        bag.add(new MockTriggerRequest(23456L, 30000L, 2, 22));
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
    }

    public void testAddHitAndMerge()
    {
        SimpleTriggerBag bag = new SimpleTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        bag.add(new MockHit(12345L));
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        bag.add(new MockHit(12345L));
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
    }

    public void testAddTRAndMerge()
    {
        SimpleTriggerBag bag = new SimpleTriggerBag();
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
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
    }

    public void testSetTimeGate()
    {
        SimpleTriggerBag bag = new SimpleTriggerBag();

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());

        final long newTime = 54321L;

        bag.setTimeGate(new MockUTCTime(newTime));
        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate",
                     newTime, bag.getTimeGate().longValue());
    }

    public void testNext()
    {
        SimpleTriggerBag bag = new SimpleTriggerBag();
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

        ILoadablePayload trp = bag.next();
        assertNotNull("Expected to get next trigger", trp);
        assertEquals("Unexpected output total",
                     1, bag.getMonitor().getOutputCountTotal());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
