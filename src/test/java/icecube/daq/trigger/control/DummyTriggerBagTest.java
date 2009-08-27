package icecube.daq.trigger.control;

import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockTriggerRequest;
import icecube.daq.trigger.test.MockUTCTime;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class DummyTriggerBagTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public DummyTriggerBagTest(String name)
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
        return new TestSuite(DummyTriggerBagTest.class);
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

        DummyTriggerBag bag =
            new DummyTriggerBag(type, cfgId, new MockSourceID(srcId));
        assertEquals("Bad size", 0, bag.size());
        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
    }

    public void testAdd()
    {
        DummyTriggerBag bag = new DummyTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.size());

        bag.add(new MockHit(12345L));
        assertEquals("Unexpected input total",
                     1, bag.size());

        bag.add(new MockHit(23456L));
        assertEquals("Unexpected input total",
                     2, bag.size());

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
    }

    public void testSetTimeGate()
    {
        DummyTriggerBag bag = new DummyTriggerBag();

        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());

        bag.setTimeGate(new MockUTCTime(12345L));
        assertNotNull("Null timeGate", bag.getTimeGate());
        assertEquals("Bad timeGate", -1L, bag.getTimeGate().longValue());
    }

    public void testNext()
    {
        DummyTriggerBag bag = new DummyTriggerBag();
        assertEquals("Unexpected input total",
                     0, bag.size());

        bag.add(new MockTriggerRequest(12345L, 20000L, 1, 11));
        assertEquals("Unexpected input total",
                     1, bag.size());

        bag.add(new MockTriggerRequest(23456L, 30000L, 2, 22));
        assertEquals("Unexpected input total",
                     2, bag.size());


        while (bag.size() > 0) {
            assertTrue("Expected to have a 'next' trigger", bag.hasNext());

            ITriggerRequestPayload trp = bag.next();
            assertNotNull("Expected to get next trigger", trp);
        }

        assertFalse("Didn't expect to have 'next' trigger", bag.hasNext());
        assertNull("Expected to get null trigger", bag.next());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
