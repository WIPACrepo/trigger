package icecube.daq.trigger.control;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IPayloadDestination;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.SourceIdRegistry;
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

public class TriggerBagTest
    extends TestCase
{
    private static final int SOURCE_ID =
        SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;

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

        public long getUTCTime()
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
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
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
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Unexpected payload type #-999 passed to TriggerBag",
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
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Unexpected payload type #-999 passed to TriggerBag",
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
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
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
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
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
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
    }

    public void testAddTRAndMerge()
    {
        TriggerBag bag = new TriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        final int baseSrcId = SourceIdRegistry.STRINGPROCESSOR_SOURCE_ID;

        MockTriggerRequest tr =
            new MockTriggerRequest(12345L, 20000L, 1, 11, baseSrcId + 1);
        tr.addPayload(new MockHit(13579L));

        bag.add(tr);
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        bag.add(new MockTriggerRequest(19999L, 23456L, 2, 22, baseSrcId + 2));
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        bag.add(new MockTriggerRequest(17777L, 18888L, 3, 33, baseSrcId + 3));
        assertEquals("Unexpected input total",
                     3, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
    }

    public void testSetTimeGate()
    {
        TriggerBag bag = new TriggerBag();

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

        assertEquals("Unexpected UID for first request", 0, trp.getUID());
    }

    public void testMerge()
    {
        TriggerBag bag = new TriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        long timeStep = 10000L;

        long firstTime = timeStep + 1234L;
        long lastTime = timeStep * 2;
        int type = 1;
        int cfgId = 11;
        long nextGate = lastTime + 1;
        int nextUID = 1;

        int numAdded = 0;
        int numIssued = 0;

        int expUID = 1;

        for (int i = 0; i < 3; i++) {
            if (i == 2) {
                bag.resetUID();
                expUID = 1;
            }

            bag.add(new MockTriggerRequest(firstTime,
                                           lastTime - (timeStep / 2),
                                           type, cfgId, SOURCE_ID,
                                           nextUID++));
            numAdded++;
            assertEquals("Unexpected input total",
                         numAdded, bag.getMonitor().getInputCountTotal());
            assertEquals("Unexpected output total",
                         numIssued, bag.getMonitor().getOutputCountTotal());

            bag.add(new MockTriggerRequest((lastTime - (timeStep / 2)) - 1,
                                           lastTime, type, cfgId, SOURCE_ID,
                                           nextUID++));
            numAdded++;
            assertEquals("Unexpected input total",
                         numAdded, bag.getMonitor().getInputCountTotal());
            assertEquals("Unexpected output total",
                         numIssued, bag.getMonitor().getOutputCountTotal());

            long gateTime;
            gateTime = nextGate - (timeStep + 1);
            bag.setTimeGate(new MockUTCTime(gateTime));
            assertFalse("Didn't expect to have a 'next' trigger for" +
                        " gateTime " + gateTime, bag.hasNext());
            assertNull("Didn't expect to get next trigger", bag.next());
            assertEquals("Unexpected output total",
                         numIssued, bag.getMonitor().getOutputCountTotal());

            gateTime = nextGate;
            bag.setTimeGate(new MockUTCTime(gateTime));
            assertTrue("Expected to have a 'next' trigger", bag.hasNext());

            numIssued += 1;

            ITriggerRequestPayload trp = bag.next();
            assertNotNull("Expected to get next trigger", trp);
            assertEquals("Unexpected output total",
                         numIssued, bag.getMonitor().getOutputCountTotal());

            assertEquals("Unexpected UID for request #" + numIssued,
                         expUID, trp.getUID());

            firstTime += timeStep;
            lastTime += timeStep;
            type += 1;
            cfgId += 11;
            nextGate += timeStep;
            expUID += 1;
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
