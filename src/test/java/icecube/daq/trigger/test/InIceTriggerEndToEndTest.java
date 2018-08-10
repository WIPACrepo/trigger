package icecube.daq.trigger.test;

import icecube.daq.common.MockAppender;
import icecube.daq.io.DAQComponentIOProcess;
import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.SpliceablePayloadReader;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.PayloadFactory;
import icecube.daq.payload.impl.TriggerRequestFactory;
import icecube.daq.payload.impl.VitreousBufferCache;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Spliceable;
import icecube.daq.splicer.SpliceableComparator;
import icecube.daq.splicer.SpliceableFactory;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerException;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.control.SNDAQAlerter;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.util.DOMRegistryException;
import icecube.daq.util.DOMRegistryFactory;
import icecube.daq.util.IDOMRegistry;
import icecube.daq.util.LocatePDAQ;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

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

    private static final Spliceable LAST_SPLICEABLE =
        SpliceableFactory.LAST_POSSIBLE_SPLICEABLE;

    public InIceTriggerEndToEndTest(String name)
    {
        super(name);
    }

    private void checkLogMessages()
    {
        try {
            for (int i = 0; i < appender.getNumberOfMessages(); i++) {
                String msg = (String) appender.getMessage(i);

                if (!(msg.startsWith("Clearing ") &&
                      msg.endsWith(" rope entries")) &&
                    !msg.startsWith("Resetting counter ") &&
                    !msg.startsWith("Resetting decrement ") &&
                    !msg.startsWith("No match for timegate ") &&
                    !msg.startsWith("Using buggy SMT algorithm") &&
                    !msg.startsWith("Using fixed SMT algorithm"))
                {
                    fail("Bad log message#" + i + ": " +
                         appender.getMessage(i));
                }
            }
        } finally {
            appender.clear();
        }
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);

        // initialize SNDAQ ZMQ address to nonsense
        System.getProperties().setProperty(SNDAQAlerter.PROPERTY, ":12345");

        // ensure LocatePDAQ uses the test version of the config directory
        File configDir =
            new File(getClass().getResource("/config").getPath());
        if (!configDir.exists()) {
            throw new IllegalArgumentException("Cannot find config" +
                                               " directory under " +
                                               getClass().getResource("/"));
        }

        System.setProperty(LocatePDAQ.CONFIG_DIR_PROPERTY,
                           configDir.getAbsolutePath());
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

        System.clearProperty(LocatePDAQ.CONFIG_DIR_PROPERTY);

        appender.assertNoLogMessages();

        super.tearDown();
    }

    public void testEndToEnd()
        throws DOMRegistryException, IOException, SplicerException,
               TriggerException
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

        IDOMRegistry domReg;
        try {
            domReg = DOMRegistryFactory.load(configDir);
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

        SpliceableComparator splCmp =
            new SpliceableComparator(LAST_SPLICEABLE);
        HKN1Splicer<Spliceable> splicer =
            new HKN1Splicer<Spliceable>(trigMgr, splCmp, LAST_SPLICEABLE);
        trigMgr.setSplicer(splicer);

        ComponentObserver observer = new ComponentObserver();

        SpliceablePayloadReader rdr =
            new SpliceablePayloadReader("hitReader", splicer,
                                        new PayloadFactory(cache));
        rdr.registerComponentObserver(observer);

        rdr.start();
        DAQTestUtil.waitUntilStopped(rdr, splicer, "creation");
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
        DAQTestUtil.waitUntilRunning(rdr);

        // load data into input channels
        trigCfg.sendInIceData(tails, numObjs);
        trigCfg.sendInIceStops(tails);

        waitForStasis(rdr, trigMgr, outProc, false);
        DAQTestUtil.waitUntilStopped(rdr, splicer, "StopMsg");

        // wait for all collection threads to stop
        for (int i = 0; i < REPS && !trigMgr.isStopped(); i++) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException ie) {
                // ignore interrupts
            }
        }

        assertTrue("Collection thread(s) not stopped: " + trigMgr,
                   trigMgr.isStopped());

        assertEquals("Bad number of payloads written",
                     trigCfg.getExpectedNumberOfInIcePayloads(numObjs),
                     outProc.getNumberWritten());

        if (appender.getLevel().equals(org.apache.log4j.Level.ALL)) {
            appender.clear();
        } else {
            checkLogMessages();
        }
    }

    private static final int REPS = 1000;
    private static final int SLEEP_TIME = 100;

    private static long getNumInputsQueued(TriggerManager mgr)
    {
        Map<String, Integer> map = mgr.getQueuedInputs();

        long total = 0;
        for (Integer val : map.values()) {
            total += val;
        }

        return total;
    }

    public static final void waitForStasis(SpliceablePayloadReader rdr,
                                           TriggerManager mgr,
                                           DAQComponentOutputProcess out,
                                           boolean debug)
    {
        final int maxStatic = 10;

        // track data progress through the system
        long received = 0;
        long queuedIn = 0;
        long processed = 0;
        long queuedReq = 0;
        long queuedOut = 0;
        long sent = 0;

        int numStatic = 0;
        boolean changed = false;
        for (int i = 0; i < REPS; i++) {
            if (received != rdr.getTotalRecordsReceived()) {
                received = rdr.getTotalRecordsReceived();
                changed = true;
            }
            final long mgrQueued = getNumInputsQueued(mgr);
            if (queuedIn != mgrQueued) {
                queuedIn = getNumInputsQueued(mgr);
                changed = true;
            }
            if (processed != mgr.getTotalProcessed()) {
                processed = mgr.getTotalProcessed();
                changed = true;
            }
            if (queuedOut != mgr.getNumOutputsQueued()) {
                queuedOut = mgr.getNumOutputsQueued();
                changed = true;
            }
            if (sent != out.getRecordsSent()) {
                sent = out.getRecordsSent();
                changed = true;
            }

            // if nothing's changed, remember how many reps were static
            if (!changed || sent == 0) {
                numStatic = 0;
            } else {
                numStatic++;
            }
            if (numStatic > maxStatic) {
                break;
            }

            if (debug) {
                System.err.printf("#%d: r%d->i%d->p%d->q%d->o%d->s%d\n",
                                  i, received, queuedIn, processed, queuedReq,
                                  queuedOut, sent);
            }

            try {
                Thread.sleep(SLEEP_TIME);
            } catch (Throwable thr) {
                // ignore interrupts
            }
        }

        assertTrue("Nothing read while waiting for processing",
                   rdr.getTotalRecordsReceived() > 0);
        assertTrue(String.format("Total processed (%d) should be more than " +
                                 " total received (%d)",
                                 mgr.getTotalProcessed(),
                                 rdr.getTotalRecordsReceived()),
                   mgr.getTotalProcessed() >= rdr.getTotalRecordsReceived());

        assertEquals("Input queue is not empty", 0, getNumInputsQueued(mgr));
        assertEquals("Output queue is not empty", 0,
                     mgr.getNumOutputsQueued());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
