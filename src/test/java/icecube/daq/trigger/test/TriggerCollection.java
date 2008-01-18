package icecube.daq.trigger.test;

import icecube.daq.payload.ISourceID;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;

import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.StrandTail;

import icecube.daq.trigger.algorithm.AbstractTrigger;

import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.config.TriggerReadout;

import icecube.daq.trigger.control.ITriggerHandler;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.nio.channels.WritableByteChannel;

import java.util.ArrayList;
import java.util.List;

public abstract class TriggerCollection
{
    static final int AMANDA_TRIGGER = SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID;
    static final int GLOBAL_TRIGGER = SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID;
    static final int ICETOP_TRIGGER = SourceIdRegistry.ICETOP_TRIGGER_SOURCE_ID;
    static final int INICE_TRIGGER = SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;

    private static ByteBuffer hitBuf;
    private static ByteBuffer stopMsg;

    private ArrayList<AbstractTrigger> list = new ArrayList<AbstractTrigger>();

    void add(AbstractTrigger trig)
    {
        list.add(trig);
    }

    public void addToHandler(ITriggerHandler trigHandler)
    {
        ISourceID srcId = trigHandler.getSourceID();

        int numAdded = 0;
        for (AbstractTrigger t : list) {
            if (srcId.equals(t.getSourceId())) {
                trigHandler.addTrigger(t);
                numAdded++;
            }
        }

        if (numAdded == 0) {
            throw new Error("No triggers added for " + srcId);
        }
    }

    /**
     * Create a trigger.
     *
     * @param type trigger type
     * @param cfgId trigger configuration ID
     * @param srcId source ID
     * @param name trigger name
     *
     * @return new trigger
     */
    static AbstractTrigger createTrigger(int type, int cfgId, int srcId,
                                         String name)
    {
        String className = "icecube.daq.trigger.algorithm." + name;

        Class trigClass;
        try {
            trigClass = Class.forName(className);
        } catch (ClassNotFoundException cnfe) {
            throw new Error("Cannot find " + name + " trigger");
        }

        AbstractTrigger trig;
        try {
            trig = (AbstractTrigger) trigClass.newInstance();
        } catch (Exception ex) {
            throw new Error("Cannot create " + name + " trigger");
        }

        trig.setTriggerType(type);
        trig.setTriggerConfigId(cfgId);
        trig.setSourceId(new MockSourceID(srcId));
        trig.setTriggerName(name);

        return trig;
    }

    public List<AbstractTrigger> get()
    {
        return list;
    }

    public abstract int getExpectedNumberOfAmandaPayloads(int numObjs);

    public abstract int getExpectedNumberOfInIcePayloads(int numObjs);

    public abstract BaseValidator getAmandaValidator();

    public abstract BaseValidator getInIceValidator();

    static void sendHit(WritableByteChannel chan, long time, int tailIndex,
                        long domId)
        throws IOException
    {
        final int bufLen = 38;

        if (hitBuf == null) {
            hitBuf = ByteBuffer.allocate(bufLen);
        }

        synchronized (hitBuf) {
            final int type = 0x2;
            final int cfgId = 2;
            final int srcId = SourceIdRegistry.SIMULATION_HUB_SOURCE_ID;
            final short mode = 0;

            hitBuf.putInt(0, bufLen);
            hitBuf.putInt(4, PayloadRegistry.PAYLOAD_ID_SIMPLE_HIT);
            hitBuf.putLong(8, time);

            hitBuf.putInt(16, type);
            hitBuf.putInt(20, cfgId);
            hitBuf.putInt(24, srcId + tailIndex);
            hitBuf.putLong(28, domId);
            hitBuf.putShort(36, mode);

            hitBuf.position(0);
            chan.write(hitBuf);
        }
    }

    public abstract void sendAmandaData(StrandTail[] tails, int numObjs)
        throws SplicerException;

    public abstract void sendAmandaStops(StrandTail[] tails)
        throws SplicerException;

    public abstract void sendInIceData(WritableByteChannel[] tails,
                                       int numObjs)
        throws IOException;

    public abstract void sendInIceStops(WritableByteChannel[] tails)
        throws IOException;

    static final void sendStopMsg(WritableByteChannel chan)
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
}