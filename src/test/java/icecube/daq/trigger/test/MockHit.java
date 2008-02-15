package icecube.daq.trigger.test;

import icecube.daq.payload.IDOMID;
import icecube.daq.payload.IPayloadDestination;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.splicer.Spliceable;
import icecube.daq.trigger.IHitPayload;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MockHit
    extends MockPayload
    implements IHitPayload, Spliceable
{
    private static final int LENGTH = 17;

    private MockSourceID srcId;
    private long domId;

    public MockHit(long time)
    {
        this(time, 123456789L);
    }

    public MockHit(long time, long domId)
    {
        super(time);

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
        return new MockDOMID(domId);
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
        throw new Error("Unimplemented");
    }

    public int getPayloadInterfaceType()
    {
        return PayloadInterfaceRegistry.I_HIT_PAYLOAD;
    }

    public int getPayloadLength()
    {
        return LENGTH;
    }

    public int getPayloadType()
    {
        return PayloadRegistry.PAYLOAD_ID_SIMPLE_HIT;
    }

    public ISourceID getSourceID()
    {
        if (srcId == null) {
            srcId = new MockSourceID(-1);
        }

        return srcId;
    }

    public int getTriggerConfigID()
    {
        throw new Error("Unimplemented");
    }

    public int getTriggerType()
    {
        throw new Error("Unimplemented");
    }

    public void setSourceID(int srcId)
    {
        this.srcId = new MockSourceID(srcId);
    }

    public int writePayload(boolean writeLoaded, IPayloadDestination dest)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    public int writePayload(boolean writeLoaded, int offset, ByteBuffer buf)
        throws IOException
    {
        System.err.println("Not writing MockHit to ByteBuffer");
        return LENGTH;
    }

    public String toString()
    {
        return "MockHit*" + getHitTimeUTC().longValue();
    }
}
