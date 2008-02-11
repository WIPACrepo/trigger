package icecube.daq.trigger.control;

import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockTriggerRequest;

import java.io.IOException;
import java.util.zip.DataFormatException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class TriggerInputTest
    extends TestCase
{
    private static final MockAppender appender = new MockAppender();

    private static final long TIME_STEP = 10000L;

    private static final long TIME_BEFORE = 100000L;
    private static final long TIME_FIRST = TIME_BEFORE - TIME_STEP;
    private static final long TIME_LAST = TIME_FIRST + TIME_STEP;
    private static final long TIME_AFTER = TIME_LAST + TIME_STEP;

    public TriggerInputTest(String name)
    {
        super(name);
    }

    public void checkWithMark(long first1, long last1, long first2,
                              long last2, boolean expNext)
    {
        if (first1 > last1) {
            throw new Error("Bad first range [" + first1 + "-" + last1 + "]");
        }
        if (first2 > last2) {
            throw new Error("Bad second range [" + first1 + "-" + last1 +
                            "]");
        }

        final long firstMark = TIME_FIRST / 2;
        if (first1 <= firstMark || first2 <= firstMark) {
            throw new Error("Bad first mark " + firstMark);
        }

        final long lastMark = first1;

        final String timeStr = "[" + first1 + "-" + last1 + "] <=> [" +
            first2 + "-" + last2 + "] #" + (expNext ? "" : "!") + "expNext";

        TriggerInput input = new TriggerInput();

        input.addPayload(new MockTriggerRequest(firstMark, lastMark));
        assertEquals("Bad size for " + timeStr, 1, input.size());
        assertFalse("Didn't expect to have 'next' trigger for " + timeStr,
                    input.hasNext());
        assertNull("Expected next trigger to be null for " + timeStr,
                   input.next());

        input.addPayload(new MockTriggerRequest(first1, last1));
        assertEquals("Bad size for " + timeStr, 2, input.size());
        assertFalse("Didn't expect to have 'next' trigger for " + timeStr,
                    input.hasNext());
        assertNull("Expected next trigger to be null for " + timeStr,
                   input.next());

        input.addPayload(new MockTriggerRequest(first2, last2));
        assertEquals("Bad size for " + timeStr, 3, input.size());

        MockTriggerRequest req;

        if (expNext) {
            assertTrue("Expected 'next' trigger for " + timeStr,
                       input.hasNext());

            req = (MockTriggerRequest) input.next();
            assertEquals("Unexpected first time from returned trigger " +
                         timeStr,
                         firstMark, req.getFirstTimeUTC().getUTCTimeAsLong());
            assertEquals("Unexpected last time from returned trigger " +
                         timeStr,
                         lastMark, req.getLastTimeUTC().getUTCTimeAsLong());

            req = (MockTriggerRequest) input.next();
            assertEquals("Unexpected first time from returned trigger " +
                         timeStr,
                         first1, req.getFirstTimeUTC().getUTCTimeAsLong());
            assertEquals("Unexpected last time from returned trigger " +
                         timeStr,
                         last1, req.getLastTimeUTC().getUTCTimeAsLong());
            assertEquals("Bad size for " + timeStr, 1, input.size());
        }

        assertFalse("Didn't expect to have 'next' trigger " + timeStr,
                    input.hasNext());
        assertNull("Expected next trigger to be null for " + timeStr,
                   input.next());

        input.flush();

        if (!expNext) {
            req = (MockTriggerRequest) input.next();
            assertEquals("Unexpected first time from returned trigger " +
                         timeStr,
                         firstMark, req.getFirstTimeUTC().getUTCTimeAsLong());
            assertEquals("Unexpected last time from returned trigger " +
                         timeStr,
                         lastMark, req.getLastTimeUTC().getUTCTimeAsLong());

            req = (MockTriggerRequest) input.next();
            assertEquals("Unexpected first time from returned trigger " +
                         timeStr,
                         first1, req.getFirstTimeUTC().getUTCTimeAsLong());
            assertEquals("Unexpected last time from returned trigger " +
                         timeStr,
                         last1, req.getLastTimeUTC().getUTCTimeAsLong());
        }

        assertEquals("Bad size for " + timeStr, 1, input.size());
        assertTrue("Expected 'next' trigger for " + timeStr, input.hasNext());

        req = (MockTriggerRequest) input.next();
        assertEquals("Unexpected first time from returned trigger " + timeStr,
                     first2, req.getFirstTimeUTC().getUTCTimeAsLong());
        assertEquals("Unexpected last time from returned trigger " + timeStr,
                     last2, req.getLastTimeUTC().getUTCTimeAsLong());

        assertFalse("Didn't expect to have 'next' trigger for " + timeStr,
                    input.hasNext());
        assertNull("Expected next trigger to be null for " + timeStr,
                   input.next());
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
        return new TestSuite(TriggerInputTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testAddPayloadDFExc()
    {
        TriggerInput input = new TriggerInput();

        MockHit hit = new MockHit(12345L);
        hit.setLoadPayloadException(new DataFormatException("Test"));
        input.addPayload(hit);
        assertEquals("Bad size", 0, input.size());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Data format exception while loading payload",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testAddPayloadIOExc()
    {
        TriggerInput input = new TriggerInput();

        MockHit hit = new MockHit(12345L);
        hit.setLoadPayloadException(new IOException("Test"));
        input.addPayload(hit);
        assertEquals("Bad size", 0, input.size());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "IO exception while loading payload",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testAddBasic()
    {
        TriggerInput input = new TriggerInput();
        input.addPayload(new MockHit(12345L));
        assertEquals("Bad size", 1, input.size());
    }

    public void testAddOverlapping()
    {
        TriggerInput input = new TriggerInput();
        input.addPayload(new MockTriggerRequest(10000L, 19999L));
        assertEquals("Bad size", 1, input.size());

        input.addPayload(new MockTriggerRequest(15000L, 25000L));
        assertEquals("Bad size", 2, input.size());

        input.addPayload(new MockTriggerRequest(20000L, 22000L));
        assertEquals("Bad size", 3, input.size());
    }

    public void testHasNext()
    {
        final long firstTime1 = TIME_LAST + TIME_STEP;
        final long lastTime1 = firstTime1 + TIME_STEP;

        checkWithMark(TIME_FIRST, TIME_LAST, firstTime1, lastTime1, true);
    }

    public void testCheckPointLT()
    {
        checkWithMark(TIME_FIRST, TIME_FIRST, TIME_BEFORE, TIME_BEFORE, true);
    }

    public void testCheckPointEQ()
    {
        checkWithMark(TIME_FIRST, TIME_FIRST, TIME_FIRST, TIME_FIRST, false);
    }

    public void testCheckPointGT()
    {
        checkWithMark(TIME_FIRST, TIME_FIRST, TIME_AFTER, TIME_AFTER, true);
    }

    public void testCheckOverlap()
    {
        //       |-- over --|
        //    |-- wide -------------|
        //                     |-- new --|

        final long firstWide = 111111L;
        final long firstOver = firstWide + TIME_STEP;
        final long lastOver = firstOver + TIME_STEP;
        final long firstNew = lastOver + TIME_STEP;
        final long lastWide = firstNew + TIME_STEP;
        final long lastNew = lastWide + TIME_STEP;

        TriggerInput input = new TriggerInput();

        input.addPayload(new MockTriggerRequest(firstOver, lastOver));
        assertEquals("Bad size", 1, input.size());
        assertFalse("Didn't expect to have 'next' trigger",
                    input.hasNext());
        assertNull("Expected next trigger to be null",
                   input.next());

        input.addPayload(new MockTriggerRequest(firstWide, lastWide));
        assertEquals("Bad size", 2, input.size());
        assertFalse("Didn't expect to have 'next' trigger",
                    input.hasNext());
        assertNull("Expected next trigger to be null",
                   input.next());

        input.addPayload(new MockTriggerRequest(firstNew, lastNew));
        assertEquals("Bad size", 3, input.size());
        assertFalse("Didn't expect to have 'next' trigger",
                    input.hasNext());
        assertNull("Expected next trigger to be null",
                   input.next());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
