package icecube.daq.trigger.component;

import icecube.daq.common.MockAppender;
import icecube.daq.io.DAQComponentIOProcess;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.alert.Alerter.Priority;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.PayloadChecker;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.VitreousBufferCache;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerException;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.control.SNDAQAlerter;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.test.ActivityMonitor;
import icecube.daq.trigger.test.BaseValidator;
import icecube.daq.trigger.test.DAQTestUtil;
import icecube.daq.trigger.test.MockAlerter;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.util.DOMInfo;
import icecube.daq.util.DOMRegistryException;
import icecube.daq.util.DOMRegistryFactory;
import icecube.daq.util.IDOMRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.WritableByteChannel;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

public class InIceTriggerIntegrationTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    private static final MockSourceID TRIGGER_SOURCE_ID =
        new MockSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);

    private static final int NUM_HITS_PER_TRIGGER = 8;
    private static final long TIME_BASE = 100000L;
    private static final long TIME_STEP =
        5000L / (long) (NUM_HITS_PER_TRIGGER + 1);

    private IniceTriggerComponent comp;
    private Pipe[] tails;

    private ByteBuffer hitBuf;

    public InIceTriggerIntegrationTest(String name)
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
                    !msg.startsWith("No match for timegate ") &&
                    !msg.startsWith("Using slow SMT algorithm") &&
                    !msg.startsWith("Using quick SMT algorithm"))
                {
                    fail("Bad log message#" + i + ": " +
                         appender.getMessage(i));
                }
            }
        } finally {
            appender.clear();
        }
    }

    private void sendHit(WritableByteChannel chan, long time, int tailIndex,
                         long domId)
        throws IOException
    {
        final int bufLen = 38;

        if (hitBuf == null) {
            hitBuf = ByteBuffer.allocate(bufLen);
        }

        synchronized (hitBuf) {
            final int recType = ITriggerAlgorithm.SPE_HIT;
            final int cfgId = 2;
            final int srcId = SourceIdRegistry.SIMULATION_HUB_SOURCE_ID;
            final short mode = 0;

            hitBuf.putInt(0, bufLen);
            hitBuf.putInt(4, PayloadRegistry.PAYLOAD_ID_SIMPLE_HIT);
            hitBuf.putLong(8, time);

            hitBuf.putInt(16, recType);
            hitBuf.putInt(20, cfgId);
            hitBuf.putInt(24, srcId + tailIndex);
            hitBuf.putLong(28, domId);
            hitBuf.putShort(36, mode);

            hitBuf.position(0);
            chan.write(hitBuf);
        }
    }

    private void sendInIceData(Pipe[] tails, int numObjs,
                               IDOMRegistry registry)
        throws DOMRegistryException, IOException
    {
        int numSent = 0;
        for (DOMInfo dom : registry.allDOMs()) {
            if (!dom.isRealDOM()) {
                continue;
            }

            final long time;
            if (numSent == 0) {
                time = TIME_BASE;
            } else {
                time = (TIME_BASE * (((numSent - 1) /
                                      NUM_HITS_PER_TRIGGER) + 1)) +
                    (TIME_STEP * numSent);
            }

            final int tailIndex = numSent % tails.length;
            sendHit(tails[tailIndex].sink(), time, tailIndex,
                    dom.getNumericMainboardId());

            if (++numSent == numObjs) {
                break;
            }
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

    private void startAndRun(IniceTriggerComponent comp, IDOMRegistry domReg,
                             int runInstance)
        throws DAQCompException, DOMRegistryException, IOException
    {
        final int numTails = 10;
        final int numObjs = numTails * 10;

        tails = DAQTestUtil.connectToReader(comp.getReader(),
                                            comp.getInputCache(), numTails);

        InIceValidator validator = new InIceValidator();
        DAQTestUtil.connectToSink("iiOut", comp.getWriter(),
                                  comp.getOutputCache(), validator);

        comp.starting(12345, 0);

        DAQTestUtil.startComponentIO(comp);

        ActivityMonitor activity = new ActivityMonitor(comp, "II");

        // load data into input channels
        sendInIceData(tails, numObjs, domReg);

        final int expTriggers = numObjs / NUM_HITS_PER_TRIGGER;

        final boolean dumpActivity = false;
        final boolean dumpSplicers = false;
        final boolean dumpBEStats = false;

        activity.waitForStasis(10, 1000, expTriggers, dumpActivity,
                               dumpSplicers);

        DAQTestUtil.sendStops(tails);

        activity.waitForStasis(10, 1000, expTriggers, dumpActivity,
                               dumpSplicers);

        //assertEquals("Bad number of payloads written",
        //             expTriggers, comp.getPayloadsSent() - 1);

        assertFalse("Found invalid payload(s)", validator.foundInvalid());

        comp.stopped();

        DAQTestUtil.checkCaches(comp, runInstance);
    }

    public static Test suite()
    {
        return new TestSuite(InIceTriggerIntegrationTest.class);
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        // remove SNDAQ ZMQ address
        System.clearProperty(SNDAQAlerter.PROPERTY);

        if (comp != null) {
            try {
                comp.closeAll();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        appender.assertNoLogMessages();

        if (tails != null) {
            DAQTestUtil.closePipeList(tails);
        }

        super.tearDown();
    }

    public void testIntegration()
        throws DAQCompException, DOMRegistryException, IOException,
               SplicerException, TriggerException
    {
        File cfgFile =
            DAQTestUtil.buildConfigFile(getClass().getResource("/").getPath(),
                                        "sps-2012-013");

        IDOMRegistry domReg;
        try {
            domReg = DOMRegistryFactory.load(cfgFile.getParent());
        } catch (Exception ex) {
            throw new Error("Cannot load DOM registry", ex);
        }

        MockAlerter alerter = new MockAlerter();
        alerter.addExpected("trigger_triplets", Priority.EMAIL, 2);

        // set up in-ice trigger
        comp = new IniceTriggerComponent();
        comp.setGlobalConfigurationDir(cfgFile.getParent());
        comp.setAlerter(alerter);
        comp.initialize();
        comp.start(false);

        comp.configuring(cfgFile.getName());

        startAndRun(comp, domReg, 1);

        startAndRun(comp, domReg, 2);

        DAQTestUtil.destroyComponentIO(comp);

        alerter.waitForAlerts(100);

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

    class InIceValidator
        extends BaseValidator
    {
        private Logger LOG = Logger.getLogger(InIceValidator.class);

        private long timeSpan;

        private boolean jumpHack = true;

        private long nextStart;
        private long nextEnd;

        InIceValidator()
        {
            timeSpan = TIME_STEP * (long) NUM_HITS_PER_TRIGGER;

            nextStart = TIME_BASE;
            nextEnd = nextStart + timeSpan;
        }

        @Override
        public boolean validate(IPayload payload)
        {
            if (!(payload instanceof ITriggerRequestPayload)) {
                LOG.error("Unexpected payload " + payload.getClass().getName());
                return false;
            }

            //dumpPayloadBytes(payload);

            ITriggerRequestPayload tr = (ITriggerRequestPayload) payload;

            if (!PayloadChecker.validateTriggerRequest(tr, true)) {
                LOG.error("Trigger request is not valid");
                return false;
            }

            long firstTime = getUTC(tr.getFirstTimeUTC());
            long lastTime = getUTC(tr.getLastTimeUTC());

            if (firstTime != nextStart) {
                LOG.error("Expected first trigger time " + nextStart +
                          ", not " + firstTime);
                return false;
            } else if (lastTime != nextEnd) {
                LOG.error("Expected last trigger time " + nextEnd +
                          ", not " + lastTime);
                return false;
            }

            nextStart = firstTime + TIME_BASE + timeSpan;
            if (jumpHack) {
                nextStart += TIME_STEP;
                jumpHack = false;
            }
            nextEnd = lastTime + TIME_BASE + timeSpan;

            return true;
        }
    }
}
