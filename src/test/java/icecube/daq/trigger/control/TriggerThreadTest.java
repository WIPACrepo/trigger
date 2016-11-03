package icecube.daq.trigger.control;

import icecube.daq.common.MockAppender;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.control.ITriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.test.MockAlgorithm;
import icecube.daq.trigger.test.MockSubscriber;

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

    public int length()
    {
        throw new Error("Unimplemented");
    }

    public void setCache(IByteBufferCache x0)
    {
        throw new Error("Unimplemented");
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
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);
    }

    @After
    public void tearDown()
        throws Exception
    {
        appender.assertNoLogMessages();
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

        appender.assertNoLogMessages();
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

        final String exMsg = "Trigger " + algo + " failed for " + pay;
        appender.assertLogMessage(exMsg);
        appender.assertNoLogMessages();
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
    }

    @Test
    public void testJoinNoThread()
    {
        MockAlgorithm algo = new MockAlgorithm("foo");

        TriggerThread thrd = new TriggerThread(1, algo);
        thrd.join();
    }
}
