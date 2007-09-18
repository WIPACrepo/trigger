package icecube.daq.trigger.test;

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.PayloadDestination;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.PayloadRegistry;

import icecube.daq.splicer.Spliceable;

import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.ICompositePayload;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.ITriggerRequestPayload;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Vector;

import java.util.zip.DataFormatException;

public class MockTriggerRequest
    extends MockPayload
    implements Comparable, ITriggerRequestPayload, Spliceable
{
    public static final int LENGTH = 33;

    private IUTCTime firstTime;
    private IUTCTime lastTime;
    private int type;
    private int cfgId;

    private ISourceID srcId;
    private IReadoutRequest rdoutReq;

    private ArrayList payloadList = new ArrayList();

    public MockTriggerRequest(long firstVal, long lastVal)
    {
        this(firstVal, lastVal, -1, -1);
    }

    public MockTriggerRequest(long firstVal, long lastVal, int type, int cfgId)
    {
        this(firstVal, lastVal, type, cfgId, -1);
    }

    public MockTriggerRequest(long firstVal, long lastVal, int type, int cfgId,
                              int srcId)
    {
        super(firstVal);

        this.firstTime = new MockUTCTime(firstVal);
        this.lastTime = new MockUTCTime(lastVal);
        this.type = type;
        this.cfgId = cfgId;

        if (srcId >= 0) {
            setSourceID(srcId);
        }
    }

    public void addPayload(IPayload payload)
    {
        payloadList.add(payload);
    }

    public int compareTo(Object obj)
    {
        if (obj == null) {
            return 1;
        } else if (!(obj instanceof ICompositePayload)) {
            return getClass().getName().compareTo(obj.getClass().getName());
        }

        return compareTo((ICompositePayload) obj);
    }

    public int compareTo(ICompositePayload comp)
    {
        int cmp = (comp.getTriggerType() - type);
        if (cmp == 0) {
            cmp = firstTime.compareTo(comp.getFirstTimeUTC());
            if (cmp == 0) {
                cmp = lastTime.compareTo(comp.getLastTimeUTC());
            }
        }
        return cmp;
    }

    public Object deepCopy()
    {
        MockTriggerRequest tr =
            new MockTriggerRequest(firstTime.getUTCTimeAsLong(),
                                   lastTime.getUTCTimeAsLong(), type, cfgId);
        if (srcId != null) {
            tr.srcId = (ISourceID) srcId.deepCopy();
        }

        if (rdoutReq != null) {
            tr.rdoutReq = new MockReadoutRequest(rdoutReq);
        }

        return tr;
    }

    public IUTCTime getFirstTimeUTC()
    {
        return firstTime;
    }

    public Vector getHitList()
    {
        throw new Error("Unimplemented");
    }

    public IUTCTime getLastTimeUTC()
    {
        return lastTime;
    }

    public Vector getPayloads()
        throws IOException, DataFormatException
    {
        return new Vector(payloadList);
    }

    public int getPayloadInterfaceType()
    {
        return PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD;
    }

    public int getPayloadLength()
    {
        return LENGTH;
    }

    public int getPayloadType()
    {
        return PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST;
    }

    public IReadoutRequest getReadoutRequest()
    {
        return rdoutReq;
    }

    public ISourceID getSourceID()
    {
        return srcId;
    }

    public int getTriggerConfigID()
    {
        return cfgId;
    }

    public int getTriggerType()
    {
        return type;
    }

    public int getUID()
    {
        throw new Error("Unimplemented");
    }

    public void setReadoutRequest(IReadoutRequest rdoutReq)
    {
        this.rdoutReq = rdoutReq;
    }

    public void setSourceID(int srcVal)
    {
        this.srcId = new MockSourceID(srcVal);
    }

    public int writePayload(boolean writeLoaded, PayloadDestination dest)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    public int writePayload(boolean writeLoaded, int offset, ByteBuffer buf)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    public String toString()
    {
        return "MockTriggerRequest:Type#" + type + ",srcId#" + srcId +
            "[" + firstTime + "," + lastTime + "]";
    }
}
