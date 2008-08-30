package icecube.daq.trigger.control;

import icecube.daq.payload.IPayloadDestination;
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
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
        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        CoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        bag.setConditionalTriggerAlgorithm(cTrig);

        MockTriggerRequest tr;

        tr = new MockTriggerRequest(12345L, 20000L, 1, 11);
        tr.setReadoutRequest(new MockReadoutRequest());
        bag.add(tr);
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        tr = new MockTriggerRequest(23456L, 30000L, 2, 22);
        tr.setReadoutRequest(new MockReadoutRequest());
        bag.add(tr);
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
    }

    public void testAddTRAndMerge()
    {
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
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        rr = new MockReadoutRequest();
        rr.add(1, 19999L, 23456L, 11111L, 10);

        tr = new MockTriggerRequest(19999L, 23456L, 2, 11, 2000);
        tr.setReadoutRequest(rr);
        bag.add(tr);
        assertEquals("Unexpected input total",
                     2, bag.getMonitor().getInputCountTotal());

        rr = new MockReadoutRequest();
        rr.add(1, 17777L, 18888L, 111111L, 10);

        tr = new MockTriggerRequest(17777L, 18888L, 3, 33, 3000);
        tr.setReadoutRequest(rr);
        bag.add(tr);
        assertEquals("Unexpected input total",
                     3, bag.getMonitor().getInputCountTotal());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
    }

    public void testSetTimeGate()
    {
        ConditionalTriggerBag bag = new ConditionalTriggerBag();

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());

        final long newTime = 54321L;

        bag.setTimeGate(new MockUTCTime(newTime));
        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate",
                     newTime, bag.getTimeGate().longValue());
    }

    public void testContainsAllGetPayDFException()
    {
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
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        assertFalse("Didn't expect 'next' trigger", bag.hasNext());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Couldn't get payloads", appender.getMessage(0));
        appender.clear();
    }

    public void testContainsAllLoadDFException()
    {
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
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        assertFalse("Didn't expect 'next' trigger", bag.hasNext());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Couldn't load payload", appender.getMessage(0));
        appender.clear();
    }

    public void testContainsAllLoadIOException()
    {
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
        assertEquals("Unexpected input total",
                     1, bag.getMonitor().getInputCountTotal());

        assertFalse("Didn't expect 'next' trigger", bag.hasNext());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Couldn't load payload", appender.getMessage(0));
        appender.clear();
    }

    public void testRemoveUnqualified()
    {
        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        final int expSourceId = GT_SRCID;
        final int trigCfgId = 123;

        MockCoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        cTrig.addConfiguredTriggerID(trigCfgId);
        bag.setConditionalTriggerAlgorithm(cTrig);

        final long[][] times = {
            { 12345L, 19999L },
            { 23456L, 29999L },
            { 34567L, 39999L },
            { 45678L, 49999L },
            { 56789L, 59999L },
            { 67890L, 69999L },
        };

        bag.setTimeGate(new MockUTCTime(1L));

        int numInput = 0;
        for (int i = 0; i < times.length; i++) {
            final int trigType = i + 1;

            final boolean bogusCfgId = (i == (times.length / 2) + 1);
            int cfgId;
            if (bogusCfgId) {
                cfgId = trigCfgId + 10;
            } else {
                cfgId = trigCfgId;
            }

            MockTriggerRequest tr =
                new MockTriggerRequest(times[i][0], times[i][1], trigType,
                                       cfgId, expSourceId, i + 1);
            tr.setReadoutRequest(new MockReadoutRequest());
            tr.addPayload(new MockTriggerRequest(times[i][0] + 111L,
                                                 times[i][1] - 111L, trigType,
                                                 trigCfgId));
            bag.add(tr);

            if (!bogusCfgId) {
                numInput++;
            }

            assertFalse("Didn't expect a 'next' trigger #" + i, bag.hasNext());
            assertEquals("Unexpected input total",
                         numInput, bag.getMonitor().getInputCountTotal());
            assertEquals("Unexpected output total",
                         0, bag.getMonitor().getOutputCountTotal());
        }

        bag.setTimeGate(new MockUTCTime(21000L));
        assertTrue("Expected a 'next' trigger", bag.hasNext());
        ITriggerRequestPayload nextTR = bag.next();
        assertNotNull("Expected next trigger", nextTR);
        assertEquals("Unexpected trigger UID", 1, nextTR.getUID());
        assertEquals("Bad trigger config ID",
                     trigCfgId, nextTR.getTriggerConfigID());
    }

    public void testNext()
    {
        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        final int expSourceId = GT_SRCID;
        final int trigCfgId = 123;

        MockCoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        cTrig.addConfiguredTriggerID(trigCfgId);
        bag.setConditionalTriggerAlgorithm(cTrig);

        final long[][] times = {
            { 12345L, 19999L },
            { 23456L, 29999L },
            { 34567L, 39999L },
            { 45678L, 49999L },
            { 56789L, 59999L },
            { 67890L, 69999L },
        };

        bag.setTimeGate(new MockUTCTime(1L));

        for (int i = 0; i < times.length; i++) {
            final int trigType = i + 1;

            MockTriggerRequest tr =
                new MockTriggerRequest(times[i][0], times[i][1], trigType,
                                       trigCfgId, expSourceId, i + 1);
            tr.setReadoutRequest(new MockReadoutRequest());
            tr.addPayload(new MockTriggerRequest(times[i][0] + 123L,
                                                 times[i][1] - 1L, trigType,
                                                 trigCfgId));

            bag.add(tr);
            assertFalse("Didn't expect a 'next' trigger #" + i, bag.hasNext());
            assertEquals("Unexpected input total",
                         i + 1, bag.getMonitor().getInputCountTotal());
            assertEquals("Unexpected output total",
                         0, bag.getMonitor().getOutputCountTotal());
        }

        for (int i = 0; i < times.length; i++) {
            bag.setTimeGate(new MockUTCTime(times[i][0]));
            assertFalse("Didn't expect to have a 'next' trigger #" + i,
                        bag.hasNext());
            assertNull("Didn't expect to get next trigger #" + i, bag.next());

            bag.setTimeGate(new MockUTCTime(times[i][1]));
            assertFalse("Didn't expect to have a 'next' trigger #" + i,
                        bag.hasNext());
            assertNull("Didn't expect to get trigger #" + i, bag.next());
            assertEquals("Unexpected output total for trigger #" + i,
                         i, bag.getMonitor().getOutputCountTotal());

            bag.setTimeGate(new MockUTCTime(times[i][1] + 1L));
            assertTrue("Expected to have a 'next' trigger #" + i,
                       bag.hasNext());

            ITriggerRequestPayload trp = bag.next();
            assertNotNull("Expected to get trigger #" + i, trp);
            assertEquals("Unexpected output total for trigger #" + i,
                         i + 1, bag.getMonitor().getOutputCountTotal());
            assertEquals("Unexpected trigger#" + i + " UID",
                         i + 1, trp.getUID());
            assertEquals("Unexpected trigger#" + i + " first time",
                         times[i][0], trp.getFirstTimeUTC().longValue());
            assertEquals("Unexpected trigger#" + i + " last time",
                         times[i][1], trp.getLastTimeUTC().longValue());
        }
    }

    class TimeArrayComparator
        implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            long[] i1 = (long[]) o1;
            long[] i2 = (long[]) o2;

            int val = (int) (i1[0] - i2[0]);
            if (val == 0) {
                val = (int) (i1[1] - i2[1]);
            }

            return val;
        }
    }

    public void testNextOrder()
    {
        ConditionalTriggerBag bag = new ConditionalTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.getMonitor().getInputCountTotal());

        final int expSourceId = GT_SRCID;
        final int trigCfgId = 123;

        MockCoincidenceTrigger cTrig = new MockCoincidenceTrigger();
        cTrig.addConfiguredTriggerID(trigCfgId);
        bag.setConditionalTriggerAlgorithm(cTrig);

        final long[][] times = {
            { 23456L, 29999L },
            { 34567L, 39999L },
            { 12345L, 19999L },
        };

        bag.setTimeGate(new MockUTCTime(1L));

        for (int i = 0; i < times.length; i++) {
            final int trigType = i + 1;

            MockTriggerRequest tr =
                new MockTriggerRequest(times[i][0], times[i][1], trigType,
                                       trigCfgId, expSourceId, i + 1);
            tr.setReadoutRequest(new MockReadoutRequest());
            tr.addPayload(new MockTriggerRequest(times[i][0] + 123L,
                                                 times[i][1] - 1L, trigType,
                                                 trigCfgId));

            bag.add(tr);
            assertFalse("Didn't expect a 'next' trigger #" + i, bag.hasNext());
            assertEquals("Unexpected input total",
                         i + 1, bag.getMonitor().getInputCountTotal());
            assertEquals("Unexpected output total",
                         0, bag.getMonitor().getOutputCountTotal());
        }

        Arrays.sort(times, new TimeArrayComparator());

        bag.setTimeGate(new MockUTCTime(times[1][1] + 1L));
        assertTrue("Expected to have a 'next' trigger", bag.hasNext());

        for (int i = 0; i < times.length - 1; i++) {
            ITriggerRequestPayload trp = bag.next();
            assertNotNull("Expected to get trigger #" + i, trp);
            assertEquals("Unexpected output total for trigger #" + i,
                         i + 1, bag.getMonitor().getOutputCountTotal());
            assertEquals("Unexpected trigger#" + i + " UID",
                         i + 1, trp.getUID());
            assertEquals("Unexpected trigger#" + i + " first time",
                         times[i][0], trp.getFirstTimeUTC().longValue());
            assertEquals("Unexpected trigger#" + i + " last time",
                         times[i][1], trp.getLastTimeUTC().longValue());
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
