package icecube.daq.trigger.control;

import icecube.daq.payload.PayloadDestination;
import icecube.daq.payload.SourceIdRegistry;

import icecube.daq.trigger.ITriggerRequestPayload;

import icecube.daq.trigger.algorithm.CoincidenceTrigger;

import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockPayload;
import icecube.daq.trigger.test.MockReadoutRequest;
import icecube.daq.trigger.test.MockReadoutRequestElement;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockTriggerRequest;
import icecube.daq.trigger.test.MockUTCTime;

import java.nio.ByteBuffer;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import java.util.zip.DataFormatException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

class MockCoincidenceTrigger
    extends CoincidenceTrigger
{
    private ArrayList configuredIDs = new ArrayList();

    MockCoincidenceTrigger()
    {
    }

    public void addConfiguredTriggerID(int id)
    {
        configuredIDs.add(new Integer(id));
    }

    public List getConfiguredTriggerIDs()
    {
        return configuredIDs;
    }

    public int getTriggerId(ITriggerRequestPayload tPayload)
    {
        int id = tPayload.getTriggerConfigID();
        if (id < 0) {
            return -1;
        }

        return id;
    }

    public boolean isConfigured()
    {
        return true;
    }

    public boolean isConfiguredTrigger(ITriggerRequestPayload tPayload)
    {
        if (configuredIDs.size() == 0) {
            return true;
        }

        int id = tPayload.getTriggerConfigID();
        return configuredIDs.contains(new Integer(id));
    }
}

public class ConditionalTriggerBagTest
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

    private static final boolean DO_MONITORING = false;

    private static final int GT_SRCID =
        SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID;
    private static final int DH_SRCID =
        SourceIdRegistry.DOMHUB_SOURCE_ID;

    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public ConditionalTriggerBagTest(String name)
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
        return new TestSuite(ConditionalTriggerBagTest.class);
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

        ConditionalTriggerBag bag =
            new ConditionalTriggerBag(type, cfgId, new MockSourceID(srcId));
        assertEquals("Bad size", 0, bag.size());
        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testAddHitLoadException()
    {
        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        CoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        bag.setConditionalTriggerAlgorithm(cTrig);

        MockTriggerRequest tr = new MockTriggerRequest(12345L, 23456L);
        tr.setLoadPayloadException(new IOException("Test"));
        bag.add(tr);
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Error loading newPayload", appender.getMessage(0));
        appender.clear();
    }

    public void testAddTR()
    {
        if (!DO_MONITORING) {
            System.err.println("XXX - monitoring does not work");
        }

        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        CoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        bag.setConditionalTriggerAlgorithm(cTrig);

        MockTriggerRequest tr;

        tr = new MockTriggerRequest(12345L, 20000L, 1, 11);
        tr.setReadoutRequest(new MockReadoutRequest());
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         1, bag.getMonitor().getInputCountTotal());
        }

        tr = new MockTriggerRequest(23456L, 30000L, 2, 22);
        tr.setReadoutRequest(new MockReadoutRequest());
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         2, bag.getMonitor().getInputCountTotal());
        }

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testAddTRAndMerge()
    {
        if (!DO_MONITORING) {
            System.err.println("XXX - monitoring does not work");
        }

        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        CoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        bag.setConditionalTriggerAlgorithm(cTrig);

        MockReadoutRequest rr;
        MockTriggerRequest tr;

        rr = new MockReadoutRequest();
        rr.add(1, 12345L, 20000L, 1111L, 10);

        tr = new MockTriggerRequest(12345L, 20000L, 1, 11, 1000);
        tr.setReadoutRequest(rr);
        //tr.addPayload(new MockTriggerRequest(13579L, 14567L));

        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         1, bag.getMonitor().getInputCountTotal());
        }

        rr = new MockReadoutRequest();
        rr.add(1, 19999L, 23456L, 11111L, 10);

        tr = new MockTriggerRequest(19999L, 23456L, 2, 11, 2000);
        tr.setReadoutRequest(rr);
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         2, bag.getMonitor().getInputCountTotal());
        }

        rr = new MockReadoutRequest();
        rr.add(1, 17777L, 18888L, 111111L, 10);

        tr = new MockTriggerRequest(17777L, 18888L, 3, 33, 3000);
        tr.setReadoutRequest(rr);
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         3, bag.getMonitor().getInputCountTotal());
        }

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testSetTimeGate()
    {
        ConditionalTriggerBag bag = new ConditionalTriggerBag();

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().getUTCTimeAsLong());

        final long newTime = 54321L;

        bag.setTimeGate(new MockUTCTime(newTime));
        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate",
                     newTime, bag.getTimeGate().getUTCTimeAsLong());
    }

    public void testContainsAllGetPayDFException()
    {
        if (!DO_MONITORING) {
            System.err.println("XXX - monitoring does not work");
        }

        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        MockCoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        cTrig.addConfiguredTriggerID(11);
        bag.setConditionalTriggerAlgorithm(cTrig);

        MockTriggerRequest tr =
            new MockTriggerRequest(12345L, 20000L, 1, 11, GT_SRCID);
        tr.setGetPayloadsException(new DataFormatException("TestException"));
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         1, bag.getMonitor().getInputCountTotal());
        }

        assertFalse("Didn't expect 'next' trigger", bag.hasNext());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Couldn't get payloads", appender.getMessage(0));
        appender.clear();
    }

    public void testContainsAllGetPayIOException()
    {
        if (!DO_MONITORING) {
            System.err.println("XXX - monitoring does not work");
        }

        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        MockCoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        cTrig.addConfiguredTriggerID(11);
        bag.setConditionalTriggerAlgorithm(cTrig);

        MockTriggerRequest tr =
            new MockTriggerRequest(12345L, 20000L, 1, 11, GT_SRCID);
        tr.setGetPayloadsException(new IOException("TestException"));
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         1, bag.getMonitor().getInputCountTotal());
        }

        assertFalse("Didn't expect 'next' trigger", bag.hasNext());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Couldn't get payloads", appender.getMessage(0));
        appender.clear();
    }

    public void testContainsAllLoadDFException()
    {
        if (!DO_MONITORING) {
            System.err.println("XXX - monitoring does not work");
        }

        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        MockCoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        cTrig.addConfiguredTriggerID(11);
        bag.setConditionalTriggerAlgorithm(cTrig);

        MockTriggerRequest subTR = new MockTriggerRequest(24680L, 25000L);
        DataFormatException ex = new DataFormatException("TestException");
        subTR.setLoadPayloadException(ex);

        MockTriggerRequest tr =
            new MockTriggerRequest(12345L, 20000L, 1, 11, GT_SRCID);
        tr.addPayload(subTR);
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         1, bag.getMonitor().getInputCountTotal());
        }

        assertFalse("Didn't expect 'next' trigger", bag.hasNext());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Couldn't load payload", appender.getMessage(0));
        appender.clear();
    }

    public void testContainsAllLoadIOException()
    {
        if (!DO_MONITORING) {
            System.err.println("XXX - monitoring does not work");
        }

        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        MockCoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        cTrig.addConfiguredTriggerID(11);
        bag.setConditionalTriggerAlgorithm(cTrig);

        MockTriggerRequest subTR = new MockTriggerRequest(24680L, 25000L);
        subTR.setLoadPayloadException(new IOException("TestException"));

        MockTriggerRequest tr =
            new MockTriggerRequest(12345L, 20000L, 1, 11, GT_SRCID);
        tr.addPayload(subTR);
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         1, bag.getMonitor().getInputCountTotal());
        }

        assertFalse("Didn't expect 'next' trigger", bag.hasNext());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Couldn't load payload", appender.getMessage(0));
        appender.clear();
    }

    public void testRemoveUnqualified()
    {
        if (!DO_MONITORING) {
            System.err.println("XXX - monitoring does not work");
        }

        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        MockCoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        cTrig.addConfiguredTriggerID(11);
        cTrig.addConfiguredTriggerID(22);
        cTrig.addConfiguredTriggerID(33);
        cTrig.addConfiguredTriggerID(55);
        bag.setConditionalTriggerAlgorithm(cTrig);

        MockTriggerRequest tr;

        tr = new MockTriggerRequest(12345L, 20000L, 1, 11, GT_SRCID);
        tr.setReadoutRequest(new MockReadoutRequest());
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         1, bag.getMonitor().getInputCountTotal());
        } else {
            assertEquals("Unexpected payload total",
                         1,
                         bag.getVectorPayloadsInConditonalTriggerBag().size());
        }

        tr = new MockTriggerRequest(23456L, 30000L, 2, 22, GT_SRCID);
        tr.setReadoutRequest(new MockReadoutRequest());
        tr.addPayload(new MockTriggerRequest(24680L, 25000L));
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         2, bag.getMonitor().getInputCountTotal());
            assertEquals("Unexpected output total",
                         0, bag.getMonitor().getOutputCountTotal());
        } else {
            assertEquals("Unexpected payload total",
                         2,
                         bag.getVectorPayloadsInConditonalTriggerBag().size());
        }

        tr = new MockTriggerRequest(34567L, 40000L, 3, 33, DH_SRCID);
        tr.setReadoutRequest(new MockReadoutRequest());
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         3, bag.getMonitor().getInputCountTotal());
            assertEquals("Unexpected output total",
                         0, bag.getMonitor().getOutputCountTotal());
        } else {
            assertEquals("Unexpected payload total",
                         3,
                         bag.getVectorPayloadsInConditonalTriggerBag().size());
        }

        tr = new MockTriggerRequest(45678L, 50000L, 4, 44, GT_SRCID);
        tr.setReadoutRequest(new MockReadoutRequest());
        tr.addPayload(new MockTriggerRequest(24680L, 25000L));
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         3, bag.getMonitor().getInputCountTotal());
            assertEquals("Unexpected output total",
                         0, bag.getMonitor().getOutputCountTotal());
        } else {
            assertEquals("Unexpected payload total",
                         3,
                         bag.getVectorPayloadsInConditonalTriggerBag().size());
        }

        tr = new MockTriggerRequest(56789L, 60000L, 5, 55, GT_SRCID);
        tr.setReadoutRequest(new MockReadoutRequest());
        tr.addPayload(new MockTriggerRequest(59876L, 59999L));
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         4, bag.getMonitor().getInputCountTotal());
            assertEquals("Unexpected output total",
                         0, bag.getMonitor().getOutputCountTotal());
        } else {
            assertEquals("Unexpected payload total",
                         4,
                         bag.getVectorPayloadsInConditonalTriggerBag().size());
        }

        bag.setTimeGate(new MockUTCTime(31000L));
        assertTrue("Expected a 'next' trigger", bag.hasNext());
        ITriggerRequestPayload nextTR = bag.next();
        assertNotNull("Expected next trigger", nextTR);
        assertEquals("Bad trigger config ID", 11, nextTR.getTriggerConfigID());
    }

    public void testNext()
    {
        if (!DO_MONITORING) {
            System.err.println("XXX - monitoring does not work");
        }

        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        MockCoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        cTrig.addConfiguredTriggerID(11);
        bag.setConditionalTriggerAlgorithm(cTrig);

        MockTriggerRequest tr;

        tr = new MockTriggerRequest(12345L, 20000L, 1, 11, GT_SRCID);
        tr.setReadoutRequest(new MockReadoutRequest());
        tr.addPayload(new MockTriggerRequest(13579L, 19999L, 1, 11));
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         1, bag.getMonitor().getInputCountTotal());
        }

        tr = new MockTriggerRequest(23456L, 30000L, 2, 22, GT_SRCID);
        tr.setReadoutRequest(new MockReadoutRequest());
        tr.addPayload(new MockTriggerRequest(23579L, 29999L, 1, 11));
        bag.add(tr);
        if (DO_MONITORING) {
            assertEquals("Unexpected input total",
                         2, bag.getMonitor().getInputCountTotal());
            assertEquals("Unexpected output total",
                         0, bag.getMonitor().getOutputCountTotal());
        }

        bag.setTimeGate(new MockUTCTime(10000L));
        assertFalse("Didn't expect to have a 'next' trigger", bag.hasNext());
        assertNull("Didn't expect to get next trigger", bag.next());

        bag.setTimeGate(new MockUTCTime(16666L));
        assertFalse("Didn't expect to have a 'next' trigger", bag.hasNext());
        assertNull("Didn't expect to get next trigger", bag.next());
        if (DO_MONITORING) {
            assertEquals("Unexpected output total",
                         0, bag.getMonitor().getOutputCountTotal());
        }

        bag.setTimeGate(new MockUTCTime(20001L));
        assertTrue("Expected to have a 'next' trigger", bag.hasNext());

        ITriggerRequestPayload nextTR = bag.next();
        assertNotNull("Expected to get next trigger", nextTR);
        assertEquals("Bad trigger config ID", 11, nextTR.getTriggerConfigID());
        if (DO_MONITORING) {
            assertEquals("Unexpected output total",
                         1, bag.getMonitor().getOutputCountTotal());
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
