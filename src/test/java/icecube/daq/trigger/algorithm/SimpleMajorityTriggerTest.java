package icecube.daq.trigger.algorithm;

import icecube.daq.common.MockAppender;
import icecube.daq.io.SpliceablePayloadReader;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.PayloadFactory;
import icecube.daq.payload.impl.VitreousBufferCache;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Spliceable;
import icecube.daq.splicer.SpliceableComparator;
import icecube.daq.splicer.SpliceableFactory;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.control.SNDAQAlerter;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.test.ComponentObserver;
import icecube.daq.trigger.test.DAQTestUtil;
import icecube.daq.trigger.test.MockOutputChannel;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.SMTConfig;
import icecube.daq.trigger.test.TriggerCollection;
import icecube.daq.util.DOMInfo;
import icecube.daq.util.DOMRegistryException;
import icecube.daq.util.DOMRegistryFactory;
import icecube.daq.util.IDOMRegistry;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

public class SimpleMajorityTriggerTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;
        //new MockAppender(org.apache.log4j.Level.ALL).setVerbose(true);

    private static final Spliceable LAST_SPLICEABLE =
        SpliceableFactory.LAST_POSSIBLE_SPLICEABLE;

    public SimpleMajorityTriggerTest(String name)
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
                    !msg.startsWith("Using fixed SMT algorithm") &&
                    !(msg.startsWith("Recycled ") &&
                      msg.contains(" unused ") &&
                      msg.endsWith(" requests")))
                {
                    fail("Bad log message#" + i + ": " +
                         appender.getMessage(i));
                }
            }
        } finally {
            appender.clear();
        }
    }

    private int countRecycled()
    {
        int count = 0;
        for (int i = 0; i < appender.getNumberOfMessages(); i++) {
            Matcher match =
                RECYCLE_PAT.matcher((String) appender.getMessage(i));

            if (match.find()) {
                count += Integer.parseInt(match.group(1));
            }
        }
        return count;
    }

    private static final Pattern RECYCLE_PAT =
        Pattern.compile("^Recycled (\\d+) unused .* requests\\s*$");

    private String getConfigurationDirectory()
    {
        String configDir = getClass().getResource("/config/").getPath();

        final String classCfgStr = "/classes/config/";
        if (configDir.endsWith(classCfgStr)) {
            int breakPt = configDir.length() - (classCfgStr.length() - 1);
            configDir = configDir.substring(0, breakPt) + "test-" +
                configDir.substring(breakPt);
        }

        return configDir;
    }

    private void run(int threshold, boolean allowRerun, int numTails,
                     int numPerTail)
        throws DOMRegistryException, IOException, TriggerException
    {
        if (allowRerun) {
            System.setProperty("allowSMTRerun", "1");
        }
        SimpleMajorityTrigger.setRerunProperty();

        final int srcNum;
        if (threshold == 1 || threshold == 6) {
            srcNum = TriggerCollection.ICETOP_TRIGGER;
        } else {
            srcNum = TriggerCollection.INICE_TRIGGER;
        }

        final int numObjs = numTails * numPerTail;

        final MockSourceID srcId = new MockSourceID(srcNum);

        // set up in-ice trigger
        VitreousBufferCache cache = new VitreousBufferCache("IITrig");

        PayloadFactory factory = new PayloadFactory(cache);

        TriggerManager trigMgr = new TriggerManager(srcId, cache);

        final String configDir = getConfigurationDirectory();

        final IDOMRegistry registry = DOMRegistryFactory.load(configDir);

        DomSetFactory.setConfigurationDirectory(configDir);
        try {
            trigMgr.setDOMRegistry(registry);
        } catch (Exception ex) {
            throw new Error("Cannot set DOM registry", ex);
        }

        // load all triggers
        TriggerCollection trigCfg = new SMTConfig(registry, threshold);
        trigCfg.addToHandler(trigMgr);

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());
        outProc.setValidator(trigCfg.getInIceValidator());

        trigMgr.setOutputEngine(outProc);

        SpliceableComparator splCmp =
            new SpliceableComparator(LAST_SPLICEABLE);
        HKN1Splicer<Spliceable> splicer =
            new HKN1Splicer<Spliceable>(trigMgr, splCmp, LAST_SPLICEABLE);
        trigMgr.setSplicer(splicer);
        trigMgr.setRunNumber(1);

        ComponentObserver observer = new ComponentObserver();

        SpliceablePayloadReader rdr =
            new SpliceablePayloadReader("hitReader", splicer, factory);
        rdr.registerComponentObserver(observer);

        try {
            writeHits(cache, trigCfg, trigMgr, rdr, splicer, numTails,
                      numObjs);
        } finally {
            rdr.destroyProcessor();
        }

        final int recycled = countRecycled();
        final int totReq = outProc.getNumberWritten() + recycled;

        try {
            int expected = numObjs / threshold;
            if (!allowRerun && threshold == 1) {
                expected /= 2;
            }

            int diff = Math.abs(totReq - expected);
            assertTrue("Expected " + expected + " requests, not " + totReq,
                       diff == 0 || diff == 1);
        } finally {
            if (appender.getLevel().equals(org.apache.log4j.Level.ALL)) {
                appender.clear();
            } else {
                checkLogMessages();
            }
        }
    }

    private void writeHits(VitreousBufferCache cache,
                           TriggerCollection trigCfg,
                           TriggerManager trigMgr,
                           SpliceablePayloadReader rdr,
                           HKN1Splicer<Spliceable> splicer, int numTails,
                           int numObjs)
        throws DOMRegistryException, IOException, TriggerException
    {
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

            rdr.addDataChannel(sourceChannel, "CylTrig", cache, 1024);
        }

        rdr.startProcessing();
        DAQTestUtil.waitUntilRunning(rdr);

        // load data into input channels
        trigCfg.sendInIceData(tails, numObjs);
        trigCfg.sendInIceStops(tails);

        DAQTestUtil.waitUntilStopped(rdr, splicer, "StopMsg");

        // wait for all collection threads to stop
        for (int i = 0; i < DAQTestUtil.WAIT_REPS && !trigMgr.isStopped();
             i++)
        {
            try {
                Thread.sleep(DAQTestUtil.WAIT_TIME);
            } catch (InterruptedException ie) {
                // ignore interrupts
            }
        }

        assertTrue("Collection thread(s) not stopped: " + trigMgr,
                   trigMgr.isStopped());
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);

        Properties props = System.getProperties();

        // initialize SNDAQ ZMQ address to nonsense
        props.setProperty(SNDAQAlerter.PROPERTY, ":12345");

        // clear SMTRerun property
        props.remove("allowSMTRerun");
    }

    public static Test suite()
    {
        return new TestSuite(SimpleMajorityTriggerTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        // remove properties
        System.clearProperty(SNDAQAlerter.PROPERTY);
        System.clearProperty("allowSMTRerun");

        appender.assertNoLogMessages();

        super.tearDown();
    }

    public void testComboOldSMT1()
        throws DOMRegistryException, IOException, TriggerException
    {
        run(1, false, 24, 1234);
    }

    public void testComboNewSMT1()
        throws DOMRegistryException, IOException, TriggerException
    {
        run(1, true, 24, 1234);
    }

    public void testComboOldSMT3()
        throws DOMRegistryException, IOException, TriggerException
    {
        run(3, false, 24, 1234);
    }

    public void testComboNewSMT3()
        throws DOMRegistryException, IOException, TriggerException
    {
        run(3, true, 24, 1234);
    }

    public void testComboOldSMT6()
        throws DOMRegistryException, IOException, TriggerException
    {
        run(6, false, 24, 1234);
    }

    public void testComboNewSMT6()
        throws DOMRegistryException, IOException, TriggerException
    {
        run(6, true, 24, 1234);
    }

    public void testComboOldSMT8()
        throws DOMRegistryException, IOException, TriggerException
    {
        run(8, false, 24, 1234);
    }

    public void testComboNewSMT8()
        throws DOMRegistryException, IOException, TriggerException
    {
        run(8, true, 24, 1234);
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
