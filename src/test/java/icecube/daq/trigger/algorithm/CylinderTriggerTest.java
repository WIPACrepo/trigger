package icecube.daq.trigger.algorithm;

import icecube.daq.common.MockAppender;
import icecube.daq.io.DAQComponentIOProcess;
import icecube.daq.io.SpliceablePayloadReader;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.PayloadFactory;
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
import icecube.daq.trigger.test.ComponentObserver;
import icecube.daq.trigger.test.CylinderTriggerConfig;
import icecube.daq.trigger.test.DAQTestUtil;
import icecube.daq.trigger.test.MockOutputChannel;
import icecube.daq.trigger.test.MockOutputProcess;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.TriggerCollection;
import icecube.daq.util.DOMRegistryException;
import icecube.daq.util.DOMRegistryFactory;

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

public class CylinderTriggerTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    private static final MockSourceID srcId =
        new MockSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);

    private static final Spliceable LAST_SPLICEABLE =
        SpliceableFactory.LAST_POSSIBLE_SPLICEABLE;

    public CylinderTriggerTest(String name)
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
                    !msg.startsWith("No match for timegate "))
                {
                    fail("Bad log message#" + i + ": " +
                         appender.getMessage(i));
                }
            }
        } finally {
            appender.clear();
        }
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);

        // initialize SNDAQ ZMQ address to nonsense
        System.getProperties().setProperty(SNDAQAlerter.PROPERTY, ":12345");
    }

    public static Test suite()
    {
        return new TestSuite(CylinderTriggerTest.class);
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        // remove SNDAQ ZMQ address
        System.clearProperty(SNDAQAlerter.PROPERTY);

        appender.assertNoLogMessages();

        super.tearDown();
    }

    public void testEndToEnd()
        throws DOMRegistryException, IOException, SplicerException,
               TriggerException
    {
        final int numTails = 4;
        final int numObjs = numTails * 10;

        // set up in-ice trigger
        VitreousBufferCache cache = new VitreousBufferCache("IITrig");

        PayloadFactory factory = new PayloadFactory(cache);

        TriggerManager trigMgr =
            new TriggerManager(srcId, cache);

        String configDir =
            getClass().getResource("/config/").getPath();

        String classCfgStr = "/classes/config/";
        if (configDir.endsWith(classCfgStr)) {
            int breakPt = configDir.length() - (classCfgStr.length() - 1);
            configDir = configDir.substring(0, breakPt) + "test-" +
                configDir.substring(breakPt);
        }

        DomSetFactory.setConfigurationDirectory(configDir);

        try {
            trigMgr.setDOMRegistry(DOMRegistryFactory.load(configDir));
        } catch (Exception ex) {
            throw new Error("Cannot set DOM registry", ex);
        }

        // load all triggers
        TriggerCollection trigCfg = new CylinderTriggerConfig();
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

        assertEquals("Bad number of payloads written",
                     trigCfg.getExpectedNumberOfInIcePayloads(numObjs),
                     outProc.getNumberWritten());

        if (appender.getLevel().equals(org.apache.log4j.Level.ALL)) {
            appender.clear();
        } else {
            checkLogMessages();
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
