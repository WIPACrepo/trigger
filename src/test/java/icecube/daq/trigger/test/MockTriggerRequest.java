package icecube.daq.trigger.test;

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IPayloadDestination;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.splicer.Spliceable;
import icecube.daq.trigger.ICompositePayload;
import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.ITriggerRequestPayload;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
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
    private int uid;

    private ISourceID srcId;
    private IReadoutRequest rdoutReq;

    private ArrayList payloadList = new ArrayList();

    private DataFormatException getPayDFException;
    private IOException getPayIOException;

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
        this(firstVal, lastVal, type, cfgId, srcId, -1);
    }

    public MockTriggerRequest(long firstVal, long lastVal, int type, int cfgId,
                              int srcId, int uid)
    {
        super(firstVal);

        this.firstTime = new MockUTCTime(firstVal);
        this.lastTime = new MockUTCTime(lastVal);
        this.type = type;
        this.cfgId = cfgId;
        this.uid = uid;

        if (srcId >= 0) {
            setSourceID(srcId);
        }
    }

    public void addPayload(IPayload payload)
    {
        payloadList.add(payload);
    }

    public int compareSpliceable(Spliceable spl)
    {
        if (spl == null) {
            return 1;
        } else if (!(spl instanceof ICompositePayload)) {
            return getClass().getName().compareTo(spl.getClass().getName());
        }

        ICompositePayload comp = (ICompositePayload) spl;

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
            new MockTriggerRequest(firstTime.longValue(),
                                   lastTime.longValue(), type, cfgId);
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

    public List getHitList()
    {
        throw new Error("Unimplemented");
    }

    public IUTCTime getLastTimeUTC()
    {
        return lastTime;
    }

    public List getPayloads()
        throws IOException, DataFormatException
    {
        if (getPayDFException != null) {
            throw getPayDFException;
        } else if (getPayIOException != null) {
            throw getPayIOException;
        }

        return payloadList;
    }

    public ByteBuffer getPayloadBacking()
    {
        return null;
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
        return uid;
    }

    public void setGetPayloadsException(Exception ex)
    {
        if (ex instanceof DataFormatException) {
            getPayDFException = (DataFormatException) ex;
        } else if (ex instanceof IOException) {
            getPayIOException = (IOException) ex;
        } else {
            throw new Error("Unknown exception type " +
                            ex.getClass().getName() + ": " + ex);
        }
    }

    public void setReadoutRequest(IReadoutRequest rdoutReq)
    {
        this.rdoutReq = rdoutReq;
    }

    public void setSourceID(int srcVal)
    {
        this.srcId = new MockSourceID(srcVal);
    }

    public int writePayload(boolean writeLoaded, IPayloadDestination dest)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    public int writePayload(boolean writeLoaded, int offset, ByteBuffer buf)
        throws IOException
    {
        if (writeLoaded) {
            throw new Error("Cannot write loaded payload");
        } else if (offset != 0) {
            throw new Error("Unexpected non-zero offset is " + offset);
        } else if (buf.limit() < offset + LENGTH) {
            throw new Error("Expected " + (offset + LENGTH) +
                            "-byte buffer, not " + buf.limit());
        }

        for (char ch = 1; ch < LENGTH; ch++) {
            buf.putChar(offset + (int) ch - 1, (char) ch);
        }

        return LENGTH;
    }

    public String toString()
    {
        return "MockTriggerRequest:type#" + type + ",cfg#" + cfgId +
            ",srcId#" + srcId + "[" + firstTime + "," + lastTime + "]";
    }
}
