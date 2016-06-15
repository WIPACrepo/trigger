package icecube.daq.trigger.test;

import icecube.daq.payload.ISourceID;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID;
import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.StrandTail;
import icecube.daq.trigger.algorithm.AbstractTrigger;
import icecube.daq.trigger.control.ITriggerManager;
import icecube.daq.trigger.config.TriggerReadout;

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
    static final int SIMHUB = SourceIdRegistry.SIMULATION_HUB_SOURCE_ID;
    static final int STRINGHUB = SourceIdRegistry.STRING_HUB_SOURCE_ID;

    static final int RECORD_TYPE_TRIGGER_REQUEST = 4;

    private static final String PACKAGE_NAME =
        "icecube.daq.trigger.algorithm";

    private static ByteBuffer hitBuf;
    private static ByteBuffer stopMsg;
    private static ByteBuffer trigBuf;

    private ArrayList<AbstractTrigger> list = new ArrayList<AbstractTrigger>();

    private int hitSrcId = SIMHUB;

    void add(AbstractTrigger trig)
    {
        list.add(trig);
    }

    public void addToHandler(ITriggerManager trigHandler)
    {
        int srcId = trigHandler.getSourceId();

        int numAdded = 0;
        for (AbstractTrigger t : list) {
            if (srcId == t.getSourceId()) {
                trigHandler.addTrigger(t);
                numAdded++;
            }
        }

        if (numAdded == 0) {
            throw new Error("No algorithms added for " + new SourceID(srcId));
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
        String className = PACKAGE_NAME + "." + name;

        Class trigClass;
        try {
            trigClass = Class.forName(className);
        } catch (ClassNotFoundException cnfe) {
            throw new Error("Cannot find " + PACKAGE_NAME + "." + name);
        }

        AbstractTrigger trig;
        try {
            trig = (AbstractTrigger) trigClass.newInstance();
        } catch (Exception ex) {
            throw new Error("Cannot create " + name + " trigger");
        }

        trig.setTriggerType(type);
        trig.setTriggerConfigId(cfgId);
        trig.setSourceId(srcId);
        trig.setTriggerName(name);

        return trig;
    }

    public List<AbstractTrigger> get()
    {
        return list;
    }

    static int getAmandaConfigId(int trigType)
    {
        trigType -= 7;
        if (trigType < 0 || trigType > 8) {
            return -1;
        }

        int bit = 1;
        for (int i = 1; i <= trigType; i++) {
            bit <<= 1;
        }

        return bit;
    }

    public abstract int getExpectedNumberOfAmandaPayloads(int numObjs);

    public abstract int getExpectedNumberOfInIcePayloads(int numObjs);

    public abstract BaseValidator getAmandaValidator();

    public abstract BaseValidator getInIceValidator();

    private static int trigUID = 1;

    public abstract void sendAmandaData(WritableByteChannel[] tails,
                                        int numObjs)
        throws IOException;

    public abstract void sendAmandaStops(WritableByteChannel[] tails)
        throws IOException;

    void sendHit(WritableByteChannel chan, long time, int tailIndex, long domId)
        throws IOException
    {
        final int bufLen = 38;

        if (hitBuf == null) {
            hitBuf = ByteBuffer.allocate(bufLen);
        }

        synchronized (hitBuf) {
            final int recType = AbstractTrigger.SPE_HIT;
            final int cfgId = 2;
            final int srcId = hitSrcId + tailIndex;
            final short mode = 0;

            hitBuf.putInt(0, bufLen);
            hitBuf.putInt(4, PayloadRegistry.PAYLOAD_ID_SIMPLE_HIT);
            hitBuf.putLong(8, time);

            hitBuf.putInt(16, recType);
            hitBuf.putInt(20, cfgId);
            hitBuf.putInt(24, srcId);
            hitBuf.putLong(28, domId);
            hitBuf.putShort(36, mode);

            hitBuf.position(0);
            chan.write(hitBuf);
        }
    }

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

    public void sendStops(WritableByteChannel[] tails)
        throws IOException
    {
        for (int i = 0; i < tails.length; i++) {
            sendStopMsg(tails[i]);
        }
    }

    static void sendTrigger(WritableByteChannel chan, long firstTime,
                            long lastTime, int trigType, int srcId)
        throws IOException
    {
        final int bufLen = 72;

        if (trigBuf == null) {
            trigBuf = ByteBuffer.allocate(bufLen);
        }

        synchronized (trigBuf) {
            final int recType = RECORD_TYPE_TRIGGER_REQUEST;
            final int uid = trigUID++;

            int amCfgId = getAmandaConfigId(trigType);
            if (amCfgId < 0) {
                amCfgId = 0;
            }

            trigBuf.putInt(0, bufLen);
            trigBuf.putInt(4, PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST);
            trigBuf.putLong(8, firstTime);

            trigBuf.putShort(16, (short) recType);
            trigBuf.putInt(18, uid);
            trigBuf.putInt(22, trigType);
            trigBuf.putInt(26, amCfgId);
            trigBuf.putInt(30, srcId);
            trigBuf.putLong(34, firstTime);
            trigBuf.putLong(42, lastTime);

            trigBuf.putShort(50, (short) 0xff);
            trigBuf.putInt(52, uid);
            trigBuf.putInt(56, srcId);
            trigBuf.putInt(60, 0);

            trigBuf.putInt(64, 8);
            trigBuf.putShort(68, (short) 1);
            trigBuf.putShort(70, (short) 0);

            trigBuf.position(0);
            chan.write(trigBuf);
        }
    }

    void setSourceId(int srcId)
    {
        this.hitSrcId = srcId;
    }
}
