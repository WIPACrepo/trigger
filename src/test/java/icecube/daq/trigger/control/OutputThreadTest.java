package icecube.daq.trigger.control;

import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.Spliceable;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockBufferCache;
import icecube.daq.trigger.test.MockOutputChannel;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockReadoutRequest;
import icecube.daq.trigger.test.MockSplicer;
import icecube.daq.trigger.test.MockTriggerRequest;

import java.util.ArrayList;

import org.junit.*;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

class MockTruncateThread
    implements ITruncateThread
{
    private boolean started;
    private boolean stopped;

    public boolean isStarted()
    {
        return started;
    }

    public boolean isStopped()
    {
        return stopped;
    }

    public void start(Splicer splicer)
    {
        started = true;
    }

    public void stop()
    {
        stopped = true;
    }

    public void truncate(Spliceable spl)
    {
        // do nothing
    }
}

public class OutputThreadTest
{
    private static final int INICE_ID =
        SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;
    private static final int GLOBAL_ID =
        SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID;
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
    public void testMakeBackCompatNotMerged()
    {
        for (int i = 0; i < 3; i++) {
            final int type;
            if ((i & 0x1) == 0x1) {
                type = -1;
            } else {
                type = 2;
            }

            final int cfgId;
            if ((i & 0x2) == 0x2) {
                cfgId = -1;
            } else {
                cfgId = 3;
            }

            MockTriggerRequest req =
                new MockTriggerRequest(1, type, cfgId, 1L, 2L);
            assertTrue(String.format("makeBackwardCompatible(type %d," +
                                     " cfgId %d) failed", type, cfgId),
                       OutputThread.makeBackwardCompatible(req));
        }
    }

    @Test
    public void testMakeBackCompatEmpty()
        throws Exception
    {
        final long first = 123456789L;
        final long last = 123457890L;

        MockTriggerRequest glblReq =
            new MockTriggerRequest(1, -1, -1, first, last);

        assertFalse("makeBackwardCompatible should fail",
                   OutputThread.makeBackwardCompatible(glblReq));
    }

    @Test
    public void testMakeBackCompatIgnoredReq()
        throws Exception
    {
        final long first = 123456789L;
        final long last = 123457890L;

        MockTriggerRequest glblReq =
            new MockTriggerRequest(1, -1, -1, first, last);

        final long step = (last - first) / 3;

        ArrayList<String> logMsgs = new ArrayList<String>();

        int uid = 0;
        for (long t = first; t < last - step; t += step) {
            MockTriggerRequest req =
                new MockTriggerRequest(uid++, 1, 1, t, t + step);

            if (t > first) {
                final long substep = step / 3;
                for (long t2 = t; t2 < (t + step) - substep; t2 += substep) {
                    req.addPayload(new MockTriggerRequest(uid++, 1, 1, t2,
                                                          t2 + substep));
                }
            }

            final int num;
            if (req.getPayloads() == null) {
                num = 0;
            } else {
                num = req.getPayloads().size();
            }

            if (num != 1) {
                logMsgs.add(String.format("Not fixing %s; found %d enclosed" +
                                          " requests", req, num));
            }

            glblReq.addPayload(req);
        }

        assertFalse("makeBackwardCompatible should fail",
                   OutputThread.makeBackwardCompatible(glblReq));

        assertEquals("Bad number of log messages",
                     logMsgs.size(), appender.getNumberOfMessages());

        for (int i = 0; i < logMsgs.size(); i++) {
            assertEquals("Bad log message",
                         logMsgs.get(i), appender.getMessage(i));
        }

        appender.clear();
    }

    @Test
    public void testMakeBackCompatNoRReq()
        throws Exception
    {
        final long first = 123456789L;
        final long last = 123457890L;

        MockTriggerRequest req =
            new MockTriggerRequest(2, 1, 1, first, last);
        req.addPayload(new MockTriggerRequest(3, 1, 1, first + 100,
                                              last - 100));

        MockTriggerRequest glblReq =
            new MockTriggerRequest(1, -1, -1, first, last);
        glblReq.addPayload(req);

        assertFalse("makeBackwardCompatible should fail",
                    OutputThread.makeBackwardCompatible(glblReq));

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());

        final String errMsg =
            "Cannot find readout request for request " + glblReq;
        assertEquals("Bad log message", errMsg, appender.getMessage(0));

        appender.clear();
    }

    @Test
    public void testMakeBackCompat()
        throws Exception
    {
        final long first = 123456789L;
        final long last = 123457890L;

        MockTriggerRequest subReq =
            new MockTriggerRequest(1, 1, 1, first + 100, last - 100);

        MockTriggerRequest req =
            new MockTriggerRequest(2, 1, 1, first, last);
        req.addPayload(subReq);

        MockTriggerRequest glblReq =
            new MockTriggerRequest(1, -1, -1, first, last);
        glblReq.addPayload(req);

        req.setReadoutRequest(new MockReadoutRequest(1, INICE_ID));

        assertNotNull("Could not fetch subrequest readout request",
                      req.getReadoutRequest());
        assertEquals("Bad subrequest readout srcId",
                     INICE_ID,
                     req.getReadoutRequest().getSourceID().getSourceID());

        assertTrue("makeBackwardCompatible failed",
                    OutputThread.makeBackwardCompatible(glblReq));

        assertNotNull("Could not fetch subrequest readout request",
                      req.getReadoutRequest());
        assertEquals("Bad subrequest readout srcId",
                     GLOBAL_ID,
                     req.getReadoutRequest().getSourceID().getSourceID());
    }

    @Test
    public void testRun()
    {
        MockOutputProcess outProc = new MockOutputProcess("Run");
        MockBufferCache bufCache = new MockBufferCache("foo");
        MockTruncateThread trunc = new MockTruncateThread();
        OutputThread thrd =
            new OutputThread("foo", INICE_ID, outProc, bufCache, trunc);

        assertEquals("Bad initial number queued", 0L, thrd.getNumQueued());
        assertFalse("Bad initial 'stopped' state", thrd.isStopped());
        assertFalse("Bad initial 'waiting' state", thrd.isWaiting());

        MockSplicer spl = new MockSplicer();

        assertFalse("TruncateThread should not be started", trunc.isStarted());
        assertFalse("TruncateThread should not be stopped", trunc.isStopped());

        thrd.start(spl);

        for (int i = 0; !thrd.isWaiting() && i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // ignore interrupts
            }
        }

        assertFalse("OutputThread should not be stopped", thrd.isStopped());
        assertTrue("OutputThread should be waiting", thrd.isWaiting());
        assertTrue("TruncateThread should be started", trunc.isStarted());
        assertFalse("TruncateThread should not be stopped", trunc.isStopped());

        MockOutputChannel outChan = new MockOutputChannel();
        outProc.setOutputChannel(outChan);

        assertEquals("Found unexpected output payloads",
                     0, outProc.getNumberWritten());

        MockTriggerRequest req = new MockTriggerRequest(1, 2, 3, 4, 5);
        thrd.push(req);

        for (int i = 0; outProc.getNumberWritten() == 0 && i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // ignore interrupts
            }
        }

        assertEquals("Found queued data", 0L, thrd.getNumQueued());
        assertEquals("No data written", 1, outProc.getNumberWritten());

        thrd.stop();

        for (int i = 0; !thrd.isStopped() && i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // ignore interrupts
            }
        }

        assertTrue("OutputThread should be stopped", thrd.isStopped());
        assertFalse("OutputThread should not be waiting", thrd.isWaiting());
        assertTrue("TruncateThread should be started", trunc.isStarted());
        assertTrue("TruncateThread should be stopped", trunc.isStopped());
    }

    @Test
    public void testRunGT()
    {
        MockOutputProcess outProc = new MockOutputProcess("RunGT");
        MockBufferCache bufCache = new MockBufferCache("foo");
        MockTruncateThread trunc = new MockTruncateThread();
        OutputThread thrd =
            new OutputThread("foo", GLOBAL_ID, outProc, bufCache, trunc);

        assertEquals("Bad initial number queued", 0L, thrd.getNumQueued());
        assertFalse("Bad initial 'stopped' state", thrd.isStopped());
        assertFalse("Bad initial 'waiting' state", thrd.isWaiting());

        MockSplicer spl = new MockSplicer();

        assertFalse("TruncateThread should not be started", trunc.isStarted());
        assertFalse("TruncateThread should not be stopped", trunc.isStopped());

        thrd.start(spl);

        for (int i = 0; !thrd.isWaiting() && i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // ignore interrupts
            }
        }

        assertFalse("OutputThread should not be stopped", thrd.isStopped());
        assertTrue("OutputThread should be waiting", thrd.isWaiting());
        assertTrue("TruncateThread should be started", trunc.isStarted());
        assertFalse("TruncateThread should not be stopped", trunc.isStopped());

        MockOutputChannel outChan = new MockOutputChannel();
        outProc.setOutputChannel(outChan);

        assertEquals("Found unexpected output payloads",
                     0, outProc.getNumberWritten());

        MockTriggerRequest req = new MockTriggerRequest(1, 2, 3, 4, 5);
        thrd.push(req);

        for (int i = 0; outProc.getNumberWritten() == 1 && i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // ignore interrupts
            }
        }

        assertEquals("Found queued data", 0L, thrd.getNumQueued());
        assertEquals("No data written", 1, outProc.getNumberWritten());

        thrd.stop();

        for (int i = 0; !thrd.isStopped() && i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // ignore interrupts
            }
        }

        assertTrue("OutputThread should be stopped", thrd.isStopped());
        assertFalse("OutputThread should not be waiting", thrd.isWaiting());
        assertTrue("TruncateThread should be started", trunc.isStarted());
        assertTrue("TruncateThread should be stopped", trunc.isStopped());
    }

    @Test
    public void testRunNoOutput()
    {
        MockOutputProcess outProc = new MockOutputProcess("RunNoOutput");
        MockBufferCache bufCache = new MockBufferCache("foo");
        MockTruncateThread trunc = new MockTruncateThread();
        OutputThread thrd =
            new OutputThread("foo", 1, outProc, bufCache, trunc);

        assertEquals("Bad initial number queued", 0L, thrd.getNumQueued());
        assertFalse("Bad initial 'stopped' state", thrd.isStopped());
        assertFalse("Bad initial 'waiting' state", thrd.isWaiting());

        MockSplicer spl = new MockSplicer();

        assertFalse("TruncateThread should not be started", trunc.isStarted());
        assertFalse("TruncateThread should not be stopped", trunc.isStopped());

        thrd.start(spl);

        for (int i = 0; !thrd.isWaiting() && i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // ignore interrupts
            }
        }

        assertFalse("OutputThread should not be stopped", thrd.isStopped());
        assertTrue("OutputThread should be waiting", thrd.isWaiting());
        assertTrue("TruncateThread should be started", trunc.isStarted());
        assertFalse("TruncateThread should not be stopped", trunc.isStopped());

/*
        MockOutputChannel outChan = new MockOutputChannel();
        outProc.setOutputChannel(outChan);

        assertEquals("Found unexpected output payloads",
                     0, outProc.getNumberWritten());

        MockTriggerRequest req = new MockTriggerRequest(1, 2, 3, 4, 5);
        thrd.push(req);

        for (int i = 0; thrd.getNumQueued() == 1 && i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // ignore interrupts
            }
        }

        assertEquals("Found queued data", 0L, thrd.getNumQueued());
        assertEquals("No data written", 1, outProc.getNumberWritten());
*/

        thrd.stop();

        for (int i = 0; !thrd.isStopped() && i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // ignore interrupts
            }
        }

        assertTrue("OutputThread should be stopped", thrd.isStopped());
        assertFalse("OutputThread should not be waiting", thrd.isWaiting());
        assertTrue("TruncateThread should be started", trunc.isStarted());
        assertTrue("TruncateThread should be stopped", trunc.isStopped());
    }

    @Test
    public void testRunNoChannel()
    {
        MockOutputProcess outProc = new MockOutputProcess("RunNoChannel");
        MockBufferCache bufCache = new MockBufferCache("foo");
        MockTruncateThread trunc = new MockTruncateThread();
        OutputThread thrd =
            new OutputThread("foo", 1, outProc, bufCache, trunc);

        assertEquals("Bad initial number queued", 0L, thrd.getNumQueued());
        assertFalse("Bad initial 'stopped' state", thrd.isStopped());
        assertFalse("Bad initial 'waiting' state", thrd.isWaiting());

        MockSplicer spl = new MockSplicer();

        assertFalse("TruncateThread should not be started", trunc.isStarted());
        assertFalse("TruncateThread should not be stopped", trunc.isStopped());

        thrd.start(spl);

        for (int i = 0; !thrd.isWaiting() && i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // ignore interrupts
            }
        }

        assertFalse("OutputThread should not be stopped", thrd.isStopped());
        assertTrue("OutputThread should be waiting", thrd.isWaiting());
        assertTrue("TruncateThread should be started", trunc.isStarted());
        assertFalse("TruncateThread should not be stopped", trunc.isStopped());

        assertEquals("Found unexpected output payloads",
                     0, outProc.getNumberWritten());

        System.err.println("*** Expect output channel error ***");
        MockTriggerRequest req = new MockTriggerRequest(1, 2, 3, 4, 5);
        thrd.push(req);

        for (int i = 0; thrd.isAlive() && i < 10; i++) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // ignore interrupts
            }
        }

        assertFalse("Thread is still alive", thrd.isAlive());
        assertEquals("Found queued data", 0L, thrd.getNumQueued());
        assertEquals("Data was written", 0, outProc.getNumberWritten());
    }
}
