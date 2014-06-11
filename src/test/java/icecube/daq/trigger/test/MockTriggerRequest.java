package icecube.daq.trigger.test;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.SourceIdRegistry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MockTriggerRequest
    implements Comparable, ITriggerRequestPayload
{
    private static final int LENGTH = 41;

    private int uid;
    private int type;
    private int cfgId;
    private int srcId = SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID;
    private IUTCTime startTime;
    private IUTCTime endTime;
    private IReadoutRequest rdoutReq;
    private boolean recycled;
    private boolean merged;
    private List<IPayload> payloads;

    public MockTriggerRequest(int uid, int type, int cfgId, long startVal,
                              long endVal)
    {
        if (startVal > endVal) {
            throw new Error("Starting time " + startVal +
                            " cannot be greater than ending time " + endVal);
        }

        this.uid = uid;
        this.type = type;
        this.cfgId = cfgId;
        startTime = new MockUTCTime(startVal);
        endTime = new MockUTCTime(endVal);
    }

    public void addPayload(IPayload pay)
    {
        if (payloads == null) {
            payloads = new ArrayList<IPayload>();
        }

        payloads.add(pay);
    }

    private static int compareTimes(IUTCTime a, IUTCTime b)
    {
        if (a == null) {
            if (b == null) {
                return 0;
            }

            return 1;
        } else if (b == null) {
            return -1;
        }

        return (int) (a.longValue() - b.longValue());
    }

    public int compareTo(Object obj)
    {
        if (!(obj instanceof ITriggerRequestPayload)) {
            return getClass().getName().compareTo(obj.getClass().getName());
        }

        ITriggerRequestPayload req = (ITriggerRequestPayload) obj;
        int val = uid - req.getUID();
        if (val != 0) {
            val = compareTimes(startTime, req.getFirstTimeUTC());
            if (val != 0) {
                val = compareTimes(endTime, req.getLastTimeUTC());
            }
        }

        return val;
    }

    public Object deepCopy()
    {
        return new MockTriggerRequest(uid, type, cfgId, startTime.longValue(),
                                      endTime.longValue());
    }

    public void dispose()
    {
        throw new Error("Unimplemented");
    }

    public boolean equals(Object obj)
    {
        return compareTo(obj) == 0;
    }

    public IUTCTime getFirstTimeUTC()
    {
        return startTime;
    }

    public List getHitList()
    {
        throw new Error("Unimplemented");
    }

    public IUTCTime getLastTimeUTC()
    {
        return endTime;
    }

    public ByteBuffer getPayloadBacking()
    {
        throw new Error("Unimplemented");
    }

    public int getPayloadInterfaceType()
    {
        return PayloadInterfaceRegistry.I_TRIGGER_REQUEST;
    }

    public int getPayloadLength()
    {
        return length();
    }

    public IUTCTime getPayloadTimeUTC()
    {
        return startTime;
    }

    public int getPayloadType()
    {
        throw new Error("Unimplemented");
    }

    public List getPayloads()
    {
        return payloads;
    }

    public IReadoutRequest getReadoutRequest()
    {
        return rdoutReq;
    }

    public ISourceID getSourceID()
    {
        return new MockSourceID(srcId);
    }

    public int getTriggerConfigID()
    {
        return cfgId;
    }

    public String getTriggerName()
    {
        throw new Error("Unimplemented");
    }

    public int getTriggerType()
    {
        return type;
    }

    public int getUID()
    {
        return uid;
    }

    public long getUTCTime()
    {
        throw new Error("Unimplemented");
    }

    public int hashCode()
    {
        return uid + type +
            (int) (startTime.longValue() % (long) Integer.MAX_VALUE) +
            (int) (endTime.longValue() % (long) Integer.MAX_VALUE);
    }

    public boolean isMerged()
    {
        return merged;
    }

    public int length()
    {
        return LENGTH;
    }

    public void loadPayload()
    {
        // unneeded
    }

    public void recycle()
    {
        if (recycled) {
            throw new Error("Payload has already been recycled");
        }

        recycled = true;
    }

    public void setCache(IByteBufferCache cache)
    {
        throw new Error("Unimplemented");
    }

    public void setMerged()
    {
        merged = true;
    }

    public void setReadoutRequest(IReadoutRequest rReq)
    {
        rdoutReq = rReq;
    }

    /**
     * Set the universal ID for global requests which will become events.
     *
     * @param uid new UID
     */
    public void setUID(int uid)
    {
        this.uid = uid;
    }

    public int writePayload(boolean b0, int i1, ByteBuffer x2)
        throws IOException
    {
        // do nothing
        return 0;
    }

    public String toString()
    {
        final int plen;
        if (payloads == null) {
            plen = 0;
        } else {
            plen = payloads.size();
        }

        return String.format("MockTrigReq[%d typ %d cfg %d [%d-%d] pay %d]",
                             uid, type, cfgId, startTime.longValue(),
                             endTime.longValue(), plen);
    }
}
