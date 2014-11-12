package icecube.daq.trigger.test;

import icecube.daq.io.DAQComponentIOProcess;
import icecube.daq.io.SpliceablePayloadReader;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.PayloadFactory;
import icecube.daq.payload.impl.TriggerRequestFactory;
import icecube.daq.payload.impl.VitreousBufferCache;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerException;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.control.SNDAQAlerter;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.util.DOMRegistry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.WritableByteChannel;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

public class InIceTriggerEndToEndTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    private static final MockSourceID srcId =
        new MockSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);

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

    protected void setUp()
        throws Exception
    {
        super.setUp();

        appender.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);

        // initialize SNDAQ ZMQ address to nonsense
        System.getProperties().setProperty(SNDAQAlerter.PROPERTY, ":12345");
    }

    public static Test suite()
    {
        return new TestSuite(InIceTriggerEndToEndTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        // remove SNDAQ ZMQ address
        System.clearProperty(SNDAQAlerter.PROPERTY);

        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testEndToEnd()
        throws IOException, SplicerException, TriggerException
    {
        final int numTails = 10;
        final int numObjs = numTails * 10;

        // set up in-ice trigger
        VitreousBufferCache cache = new VitreousBufferCache("IITrig");

        TriggerManager trigMgr = new TriggerManager(srcId, cache);

        String configDir =
            getClass().getResource("/config/").getPath();

        String classCfgStr = "/classes/config/";
        if (configDir.endsWith(classCfgStr)) {
            int breakPt = configDir.length() - (classCfgStr.length() - 1);
            configDir = configDir.substring(0, breakPt) + "test-" +
                configDir.substring(breakPt);
        }

        DOMRegistry domReg;
        try {
            domReg = DOMRegistry.loadRegistry(configDir);
            trigMgr.setDOMRegistry(domReg);
        } catch (Exception ex) {
            throw new Error("Cannot set DOM registry", ex);
        }

        DomSetFactory.setConfigurationDirectory(configDir);

        // load all triggers
        TriggerCollection trigCfg = new SPS2012Triggers(domReg);
        trigCfg.addToHandler(trigMgr);

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());
        outProc.setValidator(trigCfg.getInIceValidator());

        trigMgr.setOutputEngine(outProc);

        trigMgr.setRunNumber(12345);

        HKN1Splicer splicer = new HKN1Splicer(trigMgr);
        trigMgr.setSplicer(splicer);

        ComponentObserver observer = new ComponentObserver();

        SpliceablePayloadReader rdr =
            new SpliceablePayloadReader("hitReader", splicer,
                                        new PayloadFactory(cache));
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

            rdr.addDataChannel(sourceChannel, "Chan", cache, 1024);
        }

        rdr.startProcessing();
        waitUntilRunning(rdr);

        // load data into input channels
        trigCfg.sendInIceData(tails, numObjs);
        trigCfg.sendInIceStops(tails);

        waitUntilProcessed(rdr, trigMgr);
        waitUntilStopped(rdr, splicer, "StopMsg");

        assertEquals("Bad number of payloads written",
                     trigCfg.getExpectedNumberOfInIcePayloads(numObjs),
                     outProc.getNumberWritten());

        if (appender.getLevel().equals(org.apache.log4j.Level.ALL)) {
            appender.clear();
        } else {
            checkLogMessages();
        }
    }

    private static final int REPS = 100;
    private static final int SLEEP_TIME = 100;

    public static final void waitUntilProcessed(SpliceablePayloadReader rdr,
                                                TriggerManager mgr)
    {
        for (int i = 0; i < REPS &&
                 (rdr.getTotalRecordsReceived() == 0 ||
                  mgr.getTotalProcessed() < rdr.getTotalRecordsReceived());
             i++)
        {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException ie) {
                // ignore interrupts
            }
        }

        for (int i = 0; i < REPS &&
                 (mgr.getNumInputsQueued() > 0 ||
                  mgr.getNumOutputsQueued() > 0);
             i++)
        {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException ie) {
                // ignore interrupts
            }
        }
    }

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
