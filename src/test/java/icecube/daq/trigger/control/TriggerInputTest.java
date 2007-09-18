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

    public TriggerInputTest(String name)
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
        final long firstTime0 = 10000L;
        final long lastTime0 = 19999L;

        TriggerInput input = new TriggerInput();
        input.addPayload(new MockTriggerRequest(firstTime0, lastTime0));
        assertEquals("Bad size", 1, input.size());
        assertFalse("Didn't expect to have 'next' trigger", input.hasNext());
        assertNull("Expected next trigger to be null", input.next());

        final long firstTime1 = firstTime0 + 10000L;
        final long lastTime1 = firstTime0 + 12000L;

        input.addPayload(new MockTriggerRequest(firstTime1,
                                                lastTime1));
        assertEquals("Bad size", 2, input.size());
        assertTrue("Expected 'next' trigger", input.hasNext());

        MockTriggerRequest req;

        req = (MockTriggerRequest) input.next();
        assertEquals("Unexpected first time from returned trigger",
                     firstTime0, req.getFirstTimeUTC().getUTCTimeAsLong());
        assertEquals("Unexpected last time from returned trigger",
                     lastTime0, req.getLastTimeUTC().getUTCTimeAsLong());

        assertFalse("Didn't expect to have 'next' trigger", input.hasNext());
        assertNull("Expected next trigger to be null", input.next());

        input.flush();

        assertTrue("Expected 'next' trigger", input.hasNext());

        req = (MockTriggerRequest) input.next();
        assertEquals("Unexpected first time from returned trigger",
                     firstTime1, req.getFirstTimeUTC().getUTCTimeAsLong());
        assertEquals("Unexpected last time from returned trigger",
                     lastTime1, req.getLastTimeUTC().getUTCTimeAsLong());

        assertFalse("Didn't expect to have 'next' trigger", input.hasNext());
        assertNull("Expected next trigger to be null", input.next());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
