package icecube.daq.trigger.test;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.Spliceable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MockTriggerRequest
    implements Comparable, ITriggerRequestPayload, Spliceable
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
        this(uid, SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID, type, cfgId,
             startVal, endVal);
    }

    public MockTriggerRequest(int uid, int srcId, int type, int cfgId,
                              long startVal, long endVal)
    {
        if (startVal > endVal) {
            throw new Error("Starting time " + startVal +
                            " cannot be greater than ending time " + endVal);
        }
        this.uid = uid;
        this.srcId = srcId;
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

    @Override
    public int compareSpliceable(Spliceable spl)
    {
        throw new Error("Unimplemented");
    }

    @Override
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

    @Override
    public Object deepCopy()
    {
        return new MockTriggerRequest(uid, type, cfgId, startTime.longValue(),
                                      endTime.longValue());
    }

    @Override
    public boolean equals(Object obj)
    {
        return compareTo(obj) == 0;
    }

    @Override
    public IUTCTime getFirstTimeUTC()
    {
        return startTime;
    }

    @Override
    public IUTCTime getLastTimeUTC()
    {
        return endTime;
    }

    @Override
    public ByteBuffer getPayloadBacking()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public IUTCTime getPayloadTimeUTC()
    {
        return startTime;
    }

    @Override
    public int getPayloadType()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public List getPayloads()
    {
        return payloads;
    }

    @Override
    public IReadoutRequest getReadoutRequest()
    {
        return rdoutReq;
    }

    @Override
    public ISourceID getSourceID()
    {
        return new MockSourceID(srcId);
    }

    @Override
    public int getTriggerConfigID()
    {
        return cfgId;
    }

    @Override
    public String getTriggerName()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public int getTriggerType()
    {
        return type;
    }

    @Override
    public int getUID()
    {
        return uid;
    }

    @Override
    public long getUTCTime()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public int hashCode()
    {
        return uid + type +
            (int) (startTime.longValue() % (long) Integer.MAX_VALUE) +
            (int) (endTime.longValue() % (long) Integer.MAX_VALUE);
    }

    @Override
    public boolean isMerged()
    {
        return merged;
    }

    @Override
    public int length()
    {
        return LENGTH;
    }

    @Override
    public void loadPayload()
    {
        // unneeded
    }

    @Override
    public void recycle()
    {
        if (recycled) {
            throw new Error("Payload has already been recycled");
        }

        recycled = true;
    }

    @Override
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
    @Override
    public void setUID(int uid)
    {
        this.uid = uid;
    }

    @Override
    public int writePayload(boolean b0, int i1, ByteBuffer x2)
        throws IOException
    {
        // do nothing
        return LENGTH;
    }

    @Override
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
