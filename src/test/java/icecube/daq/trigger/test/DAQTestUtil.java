package icecube.daq.trigger.test;

import icecube.daq.io.DAQComponentIOProcess;
import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.PayloadReader;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.trigger.component.TriggerComponent;
import icecube.daq.trigger.exceptions.UnimplementedError;
import icecube.daq.payload.IByteBufferCache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ChannelData
{
    private static final Log LOG = LogFactory.getLog(ChannelData.class);

    private String name;
    private java.nio.channels.Channel chan;
    private Error stack;

    ChannelData(String name, java.nio.channels.Channel chan)
    {
        this.name = name;
        this.chan = chan;

        try {
            throw new Error("StackTrace");
        } catch (Error err) {
            stack = err;
        }
    }

    void logOpen()
    {
        if (chan.isOpen()) {
            LOG.error(toString() + " has not been closed");
            try {
                chan.close();
            } catch (IOException ioe) {
                // ignore errors
            }
        }
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder("Channel[");
        buf.append(name).append('/').append(chan.toString());

        if (stack != null) {
            buf.append("/ opened at ");
            buf.append(stack.getStackTrace()[1].toString());
        }

        return buf.append(']').toString();
    }
}

public final class DAQTestUtil
    extends Assert
{
    private static final int REPS = 100;
    private static final int SLEEP_TIME = 100;

    public static ArrayList<ChannelData> chanData =
        new ArrayList<ChannelData>();

    private static ByteBuffer stopMsg;

    public static final File buildConfigFile(String rsrcDirName,
                                             String trigConfigName)
        throws IOException
    {
        File configDir = new File(rsrcDirName, "config");
        if (!configDir.isDirectory()) {
            throw new Error("Config directory \"" + configDir +
                            "\" does not exist");
        }

        File trigCfgDir = new File(configDir, "trigger");
        if (!trigCfgDir.isDirectory()) {
            throw new Error("Trigger config directory \"" + trigCfgDir +
                            "\" does not exist");
        }

        if (trigConfigName.endsWith(".xml")) {
            trigConfigName =
                trigConfigName.substring(0, trigConfigName.length() - 4);
        }

        File tempFile = File.createTempFile("tmpconfig-", ".xml", configDir);
        tempFile.deleteOnExit();

        FileWriter out = new FileWriter(tempFile);
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out.write("<runConfig>\n");
        out.write("<triggerConfig>" + trigConfigName + "</triggerConfig>\n");
        out.write("</runConfig>\n");
        out.close();

        return tempFile;
    }

    public static void checkCaches(TriggerComponent comp)
        throws DAQCompException
    {
        checkCaches(comp, comp.getName(), false);
    }

    public static void checkCaches(TriggerComponent comp, String name,
                                   boolean debug)
        throws DAQCompException
    {
        IByteBufferCache inCache = comp.getInputCache();
        if (debug) System.err.println(name+" INcache " + inCache);
        assertTrue(name + " input buffer cache is unbalanced (" + inCache + ")",
                   inCache.isBalanced());
        assertTrue(name + " input buffer cache was unused (" + inCache + ")",
                   inCache.getTotalBuffersAcquired() > 0);

        IByteBufferCache outCache = comp.getOutputCache();
        if (debug) System.err.println(name+" OUTcache " + outCache);
        assertTrue(name + " output buffer cache is unbalanced (" + outCache +
                   ")", outCache.isBalanced());
        assertTrue(name + " output buffer cache was unused (" + outCache + ")",
                   outCache.getTotalBuffersAcquired() > 0);

        assertEquals(name + " mismatch between triggers allocated and sent",
                     outCache.getTotalBuffersAcquired(),
                     comp.getPayloadsSent() - 1);
    }

    public static final void closePipeList(Pipe[] list)
    {
        for (int i = 0; i < list.length; i++) {
            try {
                list[i].sink().close();
            } catch (IOException ioe) {
                // ignore errors on close
            }
            try {
                list[i].source().close();
            } catch (IOException ioe) {
                // ignore errors on close
            }
        }
    }

    public static Pipe[] connectToReader(PayloadReader rdr,
                                         IByteBufferCache cache,
                                         int numTails)
        throws IOException
    {
        Pipe[] chanList = new Pipe[numTails];

        for (int i = 0; i < chanList.length; i++) {
            chanList[i] = connectToReader(rdr, cache);
        }

        return chanList;
    }

    public static Pipe connectToReader(PayloadReader rdr,
                                       IByteBufferCache cache)
        throws IOException
    {
        Pipe testPipe = Pipe.open();

        WritableByteChannel sinkChannel = testPipe.sink();
        chanData.add(new ChannelData("rdrSink", sinkChannel));
        testPipe.sink().configureBlocking(true);

        Pipe.SourceChannel sourceChannel = testPipe.source();
        chanData.add(new ChannelData("rdrSrc", sourceChannel));
        sourceChannel.configureBlocking(false);

        rdr.addDataChannel(sourceChannel, "rdrSink", cache, 1024);

        return testPipe;
    }

    public static PayloadSink connectToSink(String name,
                                            DAQComponentOutputProcess out,
                                            IByteBufferCache outCache,
                                            PayloadValidator validator)
        throws IOException
    {
        final boolean startOut = false;
        final boolean startIn = false;

        Pipe outPipe = Pipe.open();

        Pipe.SinkChannel sinkOut = outPipe.sink();
        chanData.add(new ChannelData(name, sinkOut));
        sinkOut.configureBlocking(false);

        Pipe.SourceChannel srcOut = outPipe.source();
        chanData.add(new ChannelData(name, srcOut));
        srcOut.configureBlocking(true);

        out.addDataChannel(sinkOut, outCache);

        if (startOut) {
            startIOProcess(out);
        }

        PayloadSink consumer = new PayloadSink(name, srcOut);
        consumer.setValidator(validator);
        consumer.start();

        return consumer;
    }

    public static void destroyComponentIO(TriggerComponent comp)
    {
        comp.getReader().destroyProcessor();
        comp.getWriter().destroyProcessor();
    }

    public static void sendStopMsg(WritableByteChannel chan)
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

    public static void sendStops(Pipe[] tails)
        throws IOException
    {
        for (int i = 0; i < tails.length; i++) {
            sendStopMsg(tails[i].sink());
        }
    }

    public static void startComponentIO(TriggerComponent comp)
        throws IOException
    {
        ArrayList<DAQComponentIOProcess> procList =
            new ArrayList<DAQComponentIOProcess>();

        procList.add(comp.getReader());
        procList.add(comp.getWriter());

        for (DAQComponentIOProcess proc : procList) {
            if (!proc.isRunning()) {
                proc.startProcessing();
            }
        }

        for (DAQComponentIOProcess proc : procList) {
            if (!proc.isRunning()) {
                waitUntilRunning(proc);
            }
        }
    }

    private static void startIOProcess(DAQComponentIOProcess proc)
    {
        if (!proc.isRunning()) {
            proc.startProcessing();
            waitUntilRunning(proc);
        }
    }

    private static final void waitUntilRunning(DAQComponentIOProcess proc)
    {
        for (int i = 0; i < REPS && !proc.isRunning(); i++) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException ie) {
                // ignore interrupts
            }
        }

        assertTrue("IOProcess in " + proc.getPresentState() +
                   ", not Running after StartSig", proc.isRunning());
    }
}
