package icecube.daq.trigger.control;

import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockReadoutRequestElement;
import icecube.daq.trigger.test.MockTriggerRequest;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class SorterTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public SorterTest(String name)
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
        return new TestSuite(SorterTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testElements()
    {
        Sorter sorter = new Sorter();

        final long tEarly = 10000L;
        final long tLate = 40000L;

        ArrayList list = new ArrayList();
        list.add(new MockReadoutRequestElement(1, 30000L, tLate, 111L, 123));
        list.add(new MockReadoutRequestElement(2, tEarly, 20000L, 222L, 234));
        list.add(new MockReadoutRequestElement(3, 25000L, 25999L, 333L, 345));
        list.add(new MockReadoutRequestElement(4, 22000L, 22999L, 444L, 456));
        list.add(new MockReadoutRequestElement(5, 27000L, 27999L, 555L, 567));

        assertEquals("Bad earliest time", tEarly,
                     sorter.getUTCTimeEarliest(list).longValue());
        assertEquals("Bad earliest time", tEarly,
                     sorter.getUTCTimeEarliest(list, false).longValue());

        assertEquals("Bad latest time", tLate,
                     sorter.getUTCTimeLatest(list).longValue());
        assertEquals("Bad latest time", tLate,
                     sorter.getUTCTimeLatest(list, false).longValue());

        final int listSize = list.size();

        List sortList = sorter.getReadoutElementsUTCTimeSorted(list);
        assertEquals("Unexpected list size", listSize, sortList.size());

        long prevTime = -1L;
        for (Object obj : sortList) {
            MockReadoutRequestElement elem = (MockReadoutRequestElement) obj;

            final long elemTime = elem.getFirstTimeUTC().longValue();
            if (prevTime > elemTime) {
                fail("Previous time " + prevTime + " > " + elemTime +
                     " from " + elem);
            }

            prevTime = elemTime;
        }
    }

    public void testListOneElement()
    {
        Sorter sorter = new Sorter();

        ArrayList list = new ArrayList();
        list.add(new MockReadoutRequestElement(1, 10000L, 20000L, 111L, 123));

        final int listSize = list.size();

        List sortList = sorter.getReadoutElementsUTCTimeSorted(list);
        assertEquals("Unexpected list size", listSize, sortList.size());
    }

    public void testTriggers()
    {
        Sorter sorter = new Sorter();

        final long tEarly = 10000L;
        final long tLate = 40000L;

        ArrayList list = new ArrayList();
        list.add(new MockTriggerRequest(30000L, tLate));
        list.add(new MockTriggerRequest(tEarly, 20000L));
        list.add(new MockTriggerRequest(25000L, 25999L));
        list.add(new MockTriggerRequest(22000L, 22999L));
        list.add(new MockTriggerRequest(27000L, 27999L));

        assertEquals("Bad earliest time", tEarly,
                     sorter.getUTCTimeEarliest(list, true).longValue());

        assertEquals("Bad latest time", tLate,
                     sorter.getUTCTimeLatest(list, true).longValue());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
