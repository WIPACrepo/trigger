package icecube.daq.trigger.test;

import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IDOMID;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.splicer.Spliceable;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MockHit
    extends MockPayload
    implements IHitPayload, Spliceable
{
    private static final int LENGTH = 38;

    private static final int TRIGTYPE = 0;
    private static final int CFG_ID = 0;
    private static final short TRIGMODE = 0;

    private int srcId;
    private long domId;

    private MockSourceID srcObj;
    private MockDOMID domObj;

    private ByteBuffer backingBuf;

    public MockHit(long time)
    {
        this(time, 123456789L);
    }

    public MockHit(long time, long domId)
    {
        super(time);

        this.srcId = -1;
        this.domId = domId;
    }

    public int compareSpliceable(Spliceable spl)
    {
        if (spl == null) {
            return 1;
        } else if (!(spl instanceof IHitPayload)) {
            return getClass().getName().compareTo(spl.getClass().getName());
        }

        return getHitTimeUTC().compareTo(((IHitPayload) spl).getHitTimeUTC());
    }

    public Object deepCopy()
    {
        return new MockHit(getPayloadTimeUTC().longValue());
    }

    public boolean equals(Object obj)
    {
        return compareTo(obj) == 0;
    }

    public IDOMID getDOMID()
    {
        if (domObj == null) {
            domObj = new MockDOMID(domId);
        }

        return domObj;
    }

    public IUTCTime getHitTimeUTC()
    {
        return getPayloadTimeUTC();
    }

    public double getIntegratedCharge()
    {
        throw new Error("Unimplemented");
    }

    public ByteBuffer getPayloadBacking()
    {
        if (backingBuf == null) {
            backingBuf = ByteBuffer.allocate(LENGTH);

            writePayloadToBuffer(backingBuf, 0, getUTCTime(), TRIGTYPE, CFG_ID,
                                 srcId, domId, TRIGMODE);
        }

        return backingBuf;
    }

    public int getPayloadInterfaceType()
    {
        return PayloadInterfaceRegistry.I_HIT_PAYLOAD;
    }

    public int getPayloadLength()
    {
        return length();
    }

    public int getPayloadType()
    {
        return PayloadRegistry.PAYLOAD_ID_SIMPLE_HIT;
    }

    public ISourceID getSourceID()
    {
        if (srcObj == null) {
            srcObj = new MockSourceID(srcId);
        }

        return srcObj;
    }

    public int getTriggerConfigID()
    {
        throw new Error("Unimplemented");
    }

    public int getTriggerType()
    {
        throw new Error("Unimplemented");
    }

    public int length()
    {
        return LENGTH;
    }

    public void setCache(IByteBufferCache cache)
    {
        throw new Error("Unimplemented");
    }

    public void setSourceID(int srcId)
    {
        this.srcId = srcId;
        srcObj = null;
    }

    public int writePayload(boolean writeLoaded, int offset, ByteBuffer buf)
        throws IOException
    {
        if (writeLoaded) {
            throw new Error("Cannot write loaded payload");
        } else if (buf.limit() < offset + LENGTH) {
            throw new Error("Expected " + (offset + LENGTH) +
                            "-byte buffer, not " + buf.limit());
        }

        writePayloadToBuffer(buf, offset, getUTCTime(), TRIGTYPE, CFG_ID,
                             srcId, domId, TRIGMODE);

        return LENGTH;
    }

    /**
     * Write a simple hit to the byte buffer
     * @param buf byte buffer
     * @param offset index of first byte
     * @param utcTime UTC time
     * @param trigType trigger type
     * @param cfgId trigger configuration ID
     * @param srcId source ID
     * @param domId DOM ID
     * @param trigMode trigger mode
     */
    private static void writePayloadToBuffer(ByteBuffer buf, int offset,
                                             long utcTime, int trigType,
                                             int cfgId, int srcId, long domId,
                                             short trigMode)
    {
        buf.putInt(offset, LENGTH);
        buf.putInt(offset + 4, PayloadRegistry.PAYLOAD_ID_SIMPLE_HIT);
        buf.putLong(offset + 8, utcTime);
        buf.putInt(offset + 16, trigType);
        buf.putInt(offset + 20, cfgId);
        buf.putInt(offset + 24, srcId);
        buf.putLong(offset + 28, domId);
        buf.putShort(offset + 36, trigMode);
    }

    public String toString()
    {
        return "MockHit*" + getHitTimeUTC().longValue();
    }
}
