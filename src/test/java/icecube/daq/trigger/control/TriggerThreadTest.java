package icecube.daq.trigger.control;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.common.ITriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.test.MockAlgorithm;
import icecube.daq.trigger.test.MockAppender;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.*;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

class MyPayload
    implements IPayload
{
    public MyPayload()
    {
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
        throw new Error("Unimplemented");
    }

    public int getPayloadLength()
    {
        throw new Error("Unimplemented");
    }

    public IUTCTime getPayloadTimeUTC()
    {
        throw new Error("Unimplemented");
    }

    public int getPayloadType()
    {
        throw new Error("Unimplemented");
    }

    public long getUTCTime()
    {
        throw new Error("Unimplemented");
    }

    public void setCache(IByteBufferCache x0)
    {
        throw new Error("Unimplemented");
    }
}

class MockSubscriber
    implements PayloadSubscriber
{
    private ArrayList<IPayload> payloads = new ArrayList<IPayload>();

    private TriggerThread thrd;

    public MockSubscriber()
    {
        this.thrd = thrd;
    }

    public void add(IPayload pay)
    {
        payloads.add(pay);
    }

    public boolean hasData()
    {
        return payloads.size() > 0;
    }

    public IPayload pop()
    {
        if (payloads.size() <= 1 && thrd != null) {
            thrd.stop();
        }

        if (payloads.size() < 1) {
            return null;
        }

        return payloads.remove(0);
    }

    public int size()
    {
        throw new Error("Unimplemented");
    }

    public void setThread(TriggerThread thrd)
    {
        this.thrd = thrd;
    }

    public void stop()
    {
        // do nothing
    }
}

public class TriggerThreadTest
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    @Before
    public void setUp()
        throws Exception
    {
        appender.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);
    }

    @After
    public void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());
    }

    @Test
    public void testRunEmpty()
    {
        MockSubscriber sub = new MockSubscriber();
        MockAlgorithm algo = new MockAlgorithm("foo");
        algo.setSubscriber(sub);

        TriggerThread thrd = new TriggerThread(1, algo);
        sub.setThread(thrd);

        assertFalse("Thread is stopped", thrd.isStopped());
        thrd.run();
        assertTrue("Thread is not stopped", thrd.isStopped());
    }

    @Test
    public void testRunData()
    {
        MockSubscriber sub = new MockSubscriber();
        MockAlgorithm algo = new MockAlgorithm("foo");
        algo.setSubscriber(sub);

        TriggerThread thrd = new TriggerThread(1, algo);
        sub.setThread(thrd);

        sub.add(new MyPayload());
        sub.add(new MyPayload());
        sub.add(TriggerManager.FLUSH_PAYLOAD);

        assertFalse("Thread is stopped", thrd.isStopped());
        thrd.run();
        assertTrue("Thread is not stopped", thrd.isStopped());

        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        appender.clear();
    }

    @Test
    public void testRunException()
    {
        MockSubscriber sub = new MockSubscriber();
        MockAlgorithm algo = new MockAlgorithm("foo");
        algo.setSubscriber(sub);

        TriggerThread thrd = new TriggerThread(1, algo);
        sub.setThread(thrd);

        MyPayload pay = new MyPayload();
        sub.add(pay);
        algo.setRunException(new TriggerException("FAIL"));

        assertFalse("Thread is stopped", thrd.isStopped());
        thrd.run();
        assertTrue("Thread is not stopped", thrd.isStopped());

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String exMsg = "Trigger " + algo + " failed for " + pay;
        assertEquals("Bad log message", exMsg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testRunReal()
    {
        MockSubscriber sub = new MockSubscriber();
        MockAlgorithm algo = new MockAlgorithm("foo");
        algo.setSubscriber(sub);

        TriggerThread thrd = new TriggerThread(1, algo);
        sub.setThread(thrd);

        sub.add(new MyPayload());
        sub.add(new MyPayload());
        sub.add(TriggerManager.FLUSH_PAYLOAD);

        assertFalse("Thread is stopped", thrd.isStopped());
        thrd.start();
        thrd.join();
        assertTrue("Thread is not stopped", thrd.isStopped());

        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        appender.clear();
    }

    @Test
    public void testJoinNoThread()
    {
        MockAlgorithm algo = new MockAlgorithm("foo");

        TriggerThread thrd = new TriggerThread(1, algo);
        thrd.join();
    }
}
