package icecube.daq.trigger.test;

import icecube.daq.io.PayloadReader;
import icecube.daq.io.SpliceablePayloadReader;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.MasterPayloadFactory;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.VitreousBufferCache;

import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.StrandTail;

import icecube.daq.trigger.IReadoutRequestElement;
import icecube.daq.trigger.ITriggerRequestPayload;

import icecube.daq.trigger.config.TriggerReadout;

import icecube.daq.trigger.control.GlobalTriggerManager;
import icecube.daq.trigger.control.TriggerManager;

import icecube.daq.trigger.exceptions.TriggerException;

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

import org.apache.log4j.BasicConfigurator;

class TotalValidator
    implements PayloadValidator
{
    /**
     * Validate global triggers.
     */
    TotalValidator()
    {
    }

    public void validate(IWriteablePayload payload)
    {
        if (!(payload instanceof ITriggerRequestPayload)) {
            throw new Error("Unexpected payload " +
                            payload.getClass().getName());
        }

        ITriggerRequestPayload tr = (ITriggerRequestPayload) payload;

        if (!PayloadChecker.validateTriggerRequest(tr, true)) {
            throw new Error("Trigger request is not valid");
        }

        System.err.println("GOT " + payload);
    }
}

class PayloadBridge
    extends MockPayloadDestination
{
    private static ByteBuffer stopMsg;

    private WritableByteChannel chan;

    public PayloadBridge(WritableByteChannel chan)
    {
        this.chan = chan;
    }

    public void stopAllPayloadDestinations()
        throws IOException
    {
        if (stopMsg == null) {
            stopMsg = ByteBuffer.allocate(4);
            stopMsg.putInt(0, 4);
            stopMsg.limit(4);
        }

        synchronized (stopMsg) {
            stopMsg.position(0);
            chan.write(stopMsg);
        }
    }

    public int writePayload(IWriteablePayload pay)
        throws IOException
    {
        super.writePayload(pay);

        int len;

        if (chan == null) {
            len = pay.getPayloadLength();
        } else {
            ByteBuffer buf = ByteBuffer.allocate(pay.getPayloadLength());

            len = pay.writePayload(false, 0, buf);

            chan.write(buf);
        }

        return len;
    }
}

public class TotalTriggerEndToEndTest
    extends TestCase
{
    private static final MockAppender appender =
//        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;
        new MockAppender(/*org.apache.log4j.Level.ALL*/).setVerbose(true);
//        new MockAppender(org.apache.log4j.Level.ALL).setVerbose(true);

    private static final MockSourceID amandaSrcId =
        new MockSourceID(SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID);
    private static final MockSourceID inIceSrcId =
        new MockSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);

    public TotalTriggerEndToEndTest(String name)
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

    PayloadBridge connectToComponent(SpliceablePayloadReader rdr,
                                     IByteBufferCache cache)
        throws IOException
    {
        // create a pipe for use in testing
        Pipe testPipe = Pipe.open();
        Pipe.SinkChannel sinkChannel = testPipe.sink();
        sinkChannel.configureBlocking(false);

        Pipe.SourceChannel sourceChannel = testPipe.source();
        sourceChannel.configureBlocking(false);

        rdr.addDataChannel(sourceChannel, cache, 1024);

        return new PayloadBridge(sinkChannel);
    }

    private static TriggerRequestPayloadFactory
        getTriggerRequestFactory(MasterPayloadFactory factory)
    {
        final int payloadId =
            PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST;

        return (TriggerRequestPayloadFactory)
            factory.getPayloadFactory(payloadId);
    }

    private WritableByteChannel[] openPipes(int numTails, PayloadReader rdr,
                                            IByteBufferCache cache)
        throws IOException
    {
        Pipe.SinkChannel[] chanList = new Pipe.SinkChannel[numTails];

        for (int i = 0; i < chanList.length; i++) {
            // create a pipe for use in testing
            Pipe testPipe = Pipe.open();
            chanList[i] = testPipe.sink();
            chanList[i].configureBlocking(false);

            Pipe.SourceChannel sourceChannel = testPipe.source();
            sourceChannel.configureBlocking(false);

            rdr.addDataChannel(sourceChannel, cache, 1024);
        }

        return chanList;
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
        return new TestSuite(TotalTriggerEndToEndTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testEndToEnd()
        throws IOException, SplicerException, TriggerException
    {
        final int numTails = 10;
        final int numObjs = numTails * 10;

        final long timeBase = 100000L;
        final long timeStep = 5000L / 9L;
        final int numHitsPerTrigger = 8;

        // load all triggers
        TriggerCollection trigCfg = new SPSIcecubeAmanda008Triggers();

        // set up global trigger
        VitreousBufferCache gtCache = new VitreousBufferCache();

        MasterPayloadFactory gtFactory = new MasterPayloadFactory(gtCache);

        GlobalTriggerManager gtMgr = new GlobalTriggerManager();
        gtMgr.setOutputFactory(getTriggerRequestFactory(gtFactory));

        trigCfg.addToHandler(gtMgr);

        MockPayloadDestination gtDest = new MockPayloadDestination();
        gtDest.setValidator(new TotalValidator());
        gtMgr.setPayloadOutput(gtDest);

        HKN1Splicer gtSplicer = new HKN1Splicer(gtMgr);
        gtMgr.setSplicer(gtSplicer);

        ComponentObserver gtObserver = new ComponentObserver();

        SpliceablePayloadReader gtRdr =
            new SpliceablePayloadReader("trigReader", gtSplicer, gtFactory);
        gtRdr.registerComponentObserver(gtObserver);

        gtRdr.start();
        waitUntilStopped(gtRdr, gtSplicer, "creation");
        assertTrue("PayloadReader in " + gtRdr.getPresentState() +
                   ", not Idle after creation", gtRdr.isStopped());

        // set up in-ice trigger
        VitreousBufferCache iiCache = new VitreousBufferCache();

        MasterPayloadFactory iiFactory = new MasterPayloadFactory(iiCache);

        TriggerManager iiMgr = new TriggerManager(inIceSrcId);
        iiMgr.setOutputFactory(getTriggerRequestFactory(iiFactory));

        trigCfg.addToHandler(iiMgr);

        MockPayloadDestination iiDest = connectToComponent(gtRdr, gtCache);
        iiDest.setValidator(trigCfg.getInIceValidator());
        iiMgr.setPayloadOutput(iiDest);

        HKN1Splicer iiSplicer = new HKN1Splicer(iiMgr);
        iiMgr.setSplicer(iiSplicer);

        ComponentObserver iiObserver = new ComponentObserver();

        SpliceablePayloadReader iiRdr =
            new SpliceablePayloadReader("hitReader", iiSplicer, iiFactory);
        iiRdr.registerComponentObserver(iiObserver);

        iiRdr.start();
        waitUntilStopped(iiRdr, iiSplicer, "creation");
        assertTrue("PayloadReader in " + iiRdr.getPresentState() +
                   ", not Idle after creation", iiRdr.isStopped());

        WritableByteChannel[] iiTails = openPipes(numTails, iiRdr, iiCache);

        iiRdr.startProcessing();
        waitUntilRunning(iiRdr);

        // set up amanda trigger
        VitreousBufferCache amCache = new VitreousBufferCache();

        MasterPayloadFactory amFactory = new MasterPayloadFactory(amCache);

        TriggerManager amMgr = new TriggerManager(amandaSrcId);
        amMgr.setOutputFactory(getTriggerRequestFactory(amFactory));

        trigCfg.addToHandler(amMgr);

        MockPayloadDestination amDest = connectToComponent(gtRdr, gtCache);
        amDest.setValidator(trigCfg.getAmandaValidator());
        amMgr.setPayloadOutput(amDest);

        HKN1Splicer amSplicer = new HKN1Splicer(amMgr);
        amMgr.setSplicer(amSplicer);

        ComponentObserver amObserver = new ComponentObserver();

        SpliceablePayloadReader amRdr =
            new SpliceablePayloadReader("amReader", amSplicer, amFactory);
        amRdr.registerComponentObserver(amObserver);

        amRdr.start();
        waitUntilStopped(amRdr, amSplicer, "creation");
        assertTrue("PayloadReader in " + amRdr.getPresentState() +
                   ", not Idle after creation", amRdr.isStopped());

        WritableByteChannel[] amTails = openPipes(1, amRdr, amCache);

        amRdr.startProcessing();
        waitUntilRunning(amRdr);

        // finish global trigger setup
        gtRdr.startProcessing();
        waitUntilRunning(gtRdr);

        // load data into input channels
        trigCfg.sendAmandaData(amTails, numObjs);
        trigCfg.sendInIceData(iiTails, numObjs);

        trigCfg.sendAmandaStops(amTails);
        trigCfg.sendInIceStops(iiTails);

        waitUntilStopped(iiRdr, iiSplicer, "IIStopMsg");
        waitUntilStopped(amRdr, amSplicer, "AMStopMsg");
        waitUntilStopped(gtRdr, gtSplicer, "GTStopMsg");

        amMgr.flush();
        iiMgr.flush();
        gtMgr.flush();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            // ignore interrupts
        }

        System.err.println("Output: II " + iiDest.getNumberWritten() +
                           " AM " + amDest.getNumberWritten() +
                           " GT " + gtDest.getNumberWritten() +
                           "");
        assertEquals("Bad number of in-ice payloads written",
                     trigCfg.getExpectedNumberOfInIcePayloads(numObjs),
                     iiDest.getNumberWritten());
        assertEquals("Bad number of amanda payloads written",
                     trigCfg.getExpectedNumberOfAmandaPayloads(numObjs),
                     amDest.getNumberWritten());

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
                 ((proc != null && !proc.isStopped()) ||
                  splicer.getState() != Splicer.STOPPED);
             i++)
        {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException ie) {
                // ignore interrupts
            }
        }

        if (proc != null) {
            assertTrue("IOProcess in " + proc.getPresentState() +
                       ", not Idle after " + action + extra, proc.isStopped());
        }
        assertTrue("Splicer in " + splicer.getStateString() +
                   ", not STOPPED after " + action + extra,
                   splicer.getState() == Splicer.STOPPED);
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
