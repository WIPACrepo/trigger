package icecube.daq.trigger.test;

import icecube.daq.payload.ISourceID;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID;
import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.StrandTail;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.config.TriggerReadout;
import icecube.daq.trigger.control.ITriggerManager;
import icecube.daq.util.DOMRegistryException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

public abstract class TriggerCollection
{
    public static final int AMANDA_TRIGGER =
        SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID;
    public static final int GLOBAL_TRIGGER =
        SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID;
    public static final int ICETOP_TRIGGER =
        SourceIdRegistry.ICETOP_TRIGGER_SOURCE_ID;
    public static final int INICE_TRIGGER =
        SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;
    public static final int SIMHUB =
        SourceIdRegistry.SIMULATION_HUB_SOURCE_ID;
    public static final int STRINGHUB =
        SourceIdRegistry.STRING_HUB_SOURCE_ID;

    public static final int READOUT_ALL =
        IReadoutRequestElement.READOUT_TYPE_GLOBAL;
    public static final int READOUT_ALL_ICETOP =
        IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL;
    public static final int READOUT_ALL_INICE =
        IReadoutRequestElement.READOUT_TYPE_II_GLOBAL;

    static final int RECORD_TYPE_TRIGGER_REQUEST = 4;

    private static final String PACKAGE_NAME =
        "icecube.daq.trigger.algorithm";

    private static final int HITBUF_LEN = 38;

    private static ByteBuffer hitBuf = ByteBuffer.allocate(HITBUF_LEN);
    private static ByteBuffer stopMsg;
    private static ByteBuffer trigBuf;

    private ArrayList<ITriggerAlgorithm> list = new ArrayList<ITriggerAlgorithm>();

    private int hitSrcId = SIMHUB;

    void add(ITriggerAlgorithm trig)
    {
        list.add(trig);
    }

    public void addToHandler(ITriggerManager trigHandler)
    {
        int srcId = trigHandler.getSourceId();

        int numAdded = 0;
        for (ITriggerAlgorithm t : list) {
            if (srcId == t.getSourceId()) {
                trigHandler.addTrigger(t);
                numAdded++;
            }
        }

        if (numAdded == 0) {
            throw new Error("No algorithms added for " + new SourceID(srcId));
        }
    }

    public static ByteBuffer createHit(long time, int srcId, long domId)
        throws IOException
    {
        return createHit(ByteBuffer.allocate(HITBUF_LEN), time, srcId, domId);
    }

    public static ByteBuffer createHit(ByteBuffer buf, long time, int srcId,
                                       long domId)
        throws IOException
    {
        final int recType = ITriggerAlgorithm.SPE_HIT;
        final int cfgId = 2;
        final short mode = 0;

        hitBuf.putInt(0, HITBUF_LEN);
        hitBuf.putInt(4, PayloadRegistry.PAYLOAD_ID_SIMPLE_HIT);
        hitBuf.putLong(8, time);

        hitBuf.putInt(16, recType);
        hitBuf.putInt(20, cfgId);
        hitBuf.putInt(24, srcId);
        hitBuf.putLong(28, domId);
        hitBuf.putShort(36, mode);

        hitBuf.position(0);

        return hitBuf;
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
    static ITriggerAlgorithm createTrigger(int type, int cfgId, int srcId,
                                           String name)
    {
        final String className;
        if (name.startsWith("SMT")) {
            className = PACKAGE_NAME + ".SimpleMajorityTrigger";
        } else {
            className = PACKAGE_NAME + "." + name;
        }

        Class trigClass;
        try {
            trigClass = Class.forName(className);
        } catch (ClassNotFoundException cnfe) {
            throw new Error("Cannot find " + className);
        }

        ITriggerAlgorithm trig;
        try {
            trig = (ITriggerAlgorithm) trigClass.newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new Error("Cannot create " + name + " trigger", ex);
        }

        trig.setTriggerType(type);
        trig.setTriggerConfigId(cfgId);
        trig.setSourceId(srcId);
        trig.setTriggerName(name);

        return trig;
    }

    public Iterable<ITriggerAlgorithm> get()
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

    void sendHit(WritableByteChannel chan, long time, int tailIndex,
                 long domId)
        throws IOException
    {
        synchronized (hitBuf) {
            hitBuf = createHit(hitBuf, time, hitSrcId + tailIndex, domId);
            chan.write(hitBuf);
        }
    }

    public abstract void sendInIceData(WritableByteChannel[] tails,
                                       int numObjs)
        throws DOMRegistryException, IOException;

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
