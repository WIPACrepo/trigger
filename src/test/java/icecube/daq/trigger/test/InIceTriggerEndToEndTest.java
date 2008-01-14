package icecube.daq.trigger.test;

import icecube.daq.common.DAQCmdInterface;

import icecube.daq.io.DAQComponentObserver;
import icecube.daq.io.ErrorState;
import icecube.daq.io.NormalState;
import icecube.daq.io.PayloadReader;
import icecube.daq.io.SpliceablePayloadReader;

import icecube.daq.payload.MasterPayloadFactory;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.VitreousBufferCache;

import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerException;

import icecube.daq.trigger.control.TriggerManager;

import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;

import icecube.daq.trigger.test.MockPayloadDestination;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.nio.channels.Pipe;
import java.nio.channels.WritableByteChannel;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import junit.textui.TestRunner;

import icecube.daq.io.DAQComponentIOProcess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.log4j.BasicConfigurator;

class Observer
    implements DAQComponentObserver
{
    private boolean sinkStopNotificationCalled;
    private boolean sinkErrorNotificationCalled;

    boolean gotError()
    {
        return sinkErrorNotificationCalled;
    }

    boolean gotStop()
    {
        return sinkStopNotificationCalled;
    }

    public synchronized void update(Object object, String notificationID)
    {
        if (object instanceof NormalState){
            NormalState state = (NormalState)object;
            if (state == NormalState.STOPPED){
                if (notificationID.equals(DAQCmdInterface.SINK)){
                    sinkStopNotificationCalled = true;
                } else {
                    throw new Error("Unexpected notification update");
                }
            }
        } else if (object instanceof ErrorState){
            ErrorState state = (ErrorState)object;
            if (state == ErrorState.UNKNOWN_ERROR){
                if (notificationID.equals(DAQCmdInterface.SINK)){
                    sinkErrorNotificationCalled = true;
                } else {
                    throw new Error("Unexpected notification update");
                }
            }
        }
    }
}

public class InIceTriggerEndToEndTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    private static ByteBuffer hitBuf;
    private static ByteBuffer stopMsg;

    public InIceTriggerEndToEndTest(String name)
    {
        super(name);
    }

    private void checkLogMessages()
    {
        for (int i = 0; i < appender.getNumberOfMessages(); i++) {
            String msg = (String) appender.getMessage(i);

            if (!(msg.startsWith("Clearing ") &&
                  msg.endsWith(" rope entries")) &&
                !msg.startsWith("Resetting counter ") &&
                !msg.startsWith("Resetting decrement ") &&
                !msg.startsWith("No match for timegate "))
            {
                fail("Bad log message#" + i + ": " + appender.getMessage(i));
            }
        }
        appender.clear();
    }

    private static TriggerRequestPayloadFactory
        getTriggerRequestFactory(MasterPayloadFactory factory)
    {
        final int payloadId =
            PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST;

        return (TriggerRequestPayloadFactory)
            factory.getPayloadFactory(payloadId);
    }

    void sendHit(WritableByteChannel chan, int num)
        throws IOException
    {
        final int bufLen = 38;

        if (hitBuf == null) {
            hitBuf = ByteBuffer.allocate(bufLen);
        }

        long time = (long) num * 12345678L;
        int type = 1;
        int cfgId = 2;
        int srcId = SourceIdRegistry.SIMULATION_HUB_SOURCE_ID;
        long domId = (long) num * 987654321L;
        short mode = 0;

        hitBuf.putInt(0, bufLen);
        hitBuf.putInt(4, PayloadRegistry.PAYLOAD_ID_SIMPLE_HIT);
        hitBuf.putLong(8, time);

        hitBuf.putInt(16, type);
        hitBuf.putInt(20, cfgId);
        hitBuf.putInt(24, srcId);
        hitBuf.putLong(28, domId);
        hitBuf.putShort(36, mode);

        hitBuf.position(0);
        chan.write(hitBuf);
    }

    private static final void sendStopMsg(WritableByteChannel chan)
        throws IOException
    {
        if (stopMsg == null) {
            stopMsg = ByteBuffer.allocate(4);
            stopMsg.putInt(0, 4);
            stopMsg.limit(4);
        }

        stopMsg.position(0);
        chan.write(stopMsg);
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
        return new TestSuite(InIceTriggerEndToEndTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testEndToEnd()
        throws IOException, SplicerException
    {
        final int numTails = 10;
        final int numObjs = numTails * 10;
        final int numHitsPerTrigger = 8;

        VitreousBufferCache cache = new VitreousBufferCache();

        MasterPayloadFactory factory = new MasterPayloadFactory(cache);

        TriggerManager trigMgr = new TriggerManager();
        trigMgr.setOutputFactory(getTriggerRequestFactory(factory));

        MockTrigger trig = new MockTrigger(numHitsPerTrigger);
        trigMgr.addTrigger(trig);

        MockPayloadDestination dest = new MockPayloadDestination();
        trigMgr.setPayloadOutput(dest);

        HKN1Splicer splicer = new HKN1Splicer(trigMgr);
        trigMgr.setSplicer(splicer);

        Observer observer = new Observer();

        SpliceablePayloadReader rdr =
            new SpliceablePayloadReader("hitReader", splicer, factory);
        rdr.registerComponentObserver(observer);

        rdr.start();
        waitUntilStopped(rdr, splicer, "creation");
        assertTrue("PayloadReader in " + rdr.getPresentState() +
                   ", not Idle after creation", rdr.isStopped());

        Pipe.SinkChannel[] tails = new Pipe.SinkChannel[numTails];
        for (int i = 0; i < tails.length; i++) {
            // create a pipe for use in testing
            Pipe testPipe = Pipe.open();
            tails[i] = testPipe.sink();
            tails[i].configureBlocking(false);

            Pipe.SourceChannel sourceChannel = testPipe.source();
            sourceChannel.configureBlocking(false);

            rdr.addDataChannel(sourceChannel, cache, 1024);
        }

        rdr.startProcessing();
        waitUntilRunning(rdr);

        for (int i = 0; i < numObjs; i++) {
            sendHit(tails[i % numTails], i + 1);
        }

        for (int i = 0; i < tails.length; i++) {
            sendStopMsg(tails[i]);
        }

        waitUntilStopped(rdr, splicer, "StopMsg");

        trigMgr.flush();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            // ignore interrupts
        }

        assertEquals("Bad number of payloads written",
                     numObjs / numHitsPerTrigger, dest.getNumberWritten());

        if (appender.getLevel().equals(org.apache.log4j.Level.ALL)) {
            appender.clear();
        } else {
            checkLogMessages();
        }
    }

    private static final int REPS = 100;
    private static final int SLEEP_TIME = 100;

    public static final void waitUntilRunning(DAQComponentIOProcess proc)
    {
        waitUntilRunning(proc, "");
    }

    public static final void waitUntilRunning(DAQComponentIOProcess proc,
                                              String extra)
    {
        for (int i = 0; i < REPS && !proc.isRunning(); i++) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException ie) {
                // ignore interrupts
            }
        }

        assertTrue("IOProcess in " + proc.getPresentState() +
                   ", not Running after StartSig" + extra, proc.isRunning());
    }

    private static final void waitUntilStopped(DAQComponentIOProcess proc,
                                               Splicer splicer,
                                               String action)
    {
        waitUntilStopped(proc, splicer, action, "");
    }

    private static final void waitUntilStopped(DAQComponentIOProcess proc,
                                               Splicer splicer,
                                               String action,
                                               String extra)
    {
        for (int i = 0; i < REPS &&
                 (!proc.isStopped() || splicer.getState() != Splicer.STOPPED);
             i++)
        {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException ie) {
                // ignore interrupts
            }
        }

        assertTrue("IOProcess in " + proc.getPresentState() +
                   ", not Idle after " + action + extra, proc.isStopped());
        assertTrue("Splicer in " + splicer.getStateString() +
                   ", not STOPPED after " + action + extra,
                   splicer.getState() == Splicer.STOPPED);
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
