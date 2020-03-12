package icecube.daq.trigger.algorithm;

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.impl.SourceID;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.splicer.Spliceable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FlushRequest
    implements ILoadablePayload, ITriggerRequestPayload, Spliceable
{
    public static final int UID = -12345;
    public static final long FLUSH_TIME = Long.MAX_VALUE;

    private long utcTime;
    private int type;
    private int cfgId;
    private int srcId;
    private long firstTime;
    private long lastTime;
    private List dataList;
    private IReadoutRequest rReq;

    private IUTCTime firstUTC;
    private IUTCTime lastUTC;
    private ISourceID srcObj;

    public FlushRequest()
    {
        this(FLUSH_TIME);
    }

    private FlushRequest(long utcTime)
    {
        this(utcTime, -1, -1, -1, utcTime, utcTime, null, null);
    }

    private FlushRequest(long utcTime, int type, int cfgId, int srcId,
                         long firstTime, long lastTime, List dataList,
                         IReadoutRequest rReq)
    {
        this.utcTime = utcTime;
        this.type = type;
        this.cfgId = cfgId;
        this.srcId = srcId;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
        this.dataList = dataList;
        this.rReq = rReq;
    }

    @Override
    public int compareSpliceable(Spliceable spl)
    {
        if (!(spl instanceof ILoadablePayload)) {
            return getClass().getName().compareTo(spl.getClass().getName());
        }


        long val = ((ILoadablePayload) spl).getUTCTime() - utcTime;
        if (val < 0) {
            return -1;
        } else if (val > 0) {
            return 1;
        }

        return 0;
    }

    @Override
    public Object deepCopy()
    {
        ArrayList newList;
        if (dataList == null) {
            newList = null;
        } else {
            newList = new ArrayList(dataList.size());

            for (Iterator iter = dataList.iterator(); iter.hasNext(); ) {
                newList.add(((ILoadablePayload) iter.next()).deepCopy());
            }
        }

        IReadoutRequest newReq;
        if (rReq == null) {
            newReq = null;
        } else {
            newReq = (IReadoutRequest) ((ILoadablePayload) rReq).deepCopy();
        }

        return new FlushRequest(utcTime, type, cfgId, srcId,
                                firstTime, lastTime, newList, newReq);
    }

    @Override
    public void dispose()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public IUTCTime getFirstTimeUTC()
    {
        if (firstUTC == null) {
            firstUTC = new UTCTime(firstTime);
        }

        return firstUTC;
    }

    @Override
    public List getHitList()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public IUTCTime getLastTimeUTC()
    {
        if (lastUTC == null) {
            lastUTC = new UTCTime(lastTime);
        }

        return lastUTC;
    }

    @Override
    public ByteBuffer getPayloadBacking()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public IUTCTime getPayloadTimeUTC()
    {
        return getFirstTimeUTC();
    }

    @Override
    public int getPayloadType()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public List getPayloads()
    {
        return dataList;
    }

    @Override
    public IReadoutRequest getReadoutRequest()
    {
        return rReq;
    }

    @Override
    public ISourceID getSourceID()
    {
        if (srcObj == null) {
            srcObj = new SourceID(srcId);
        }

        return srcObj;
    }

    @Override
    public int getTriggerConfigID()
    {
        return cfgId;
    }

    @Override
    public String getTriggerName()
    {
        return null;
    }

    @Override
    public int getTriggerType()
    {
        return type;
    }

    @Override
    public int getUID()
    {
        return UID;
    }

    @Override
    public long getUTCTime()
    {
        return utcTime;
    }

    @Override
    public boolean isMerged()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public int length()
    {
        final int hitLen;
        if (dataList == null) {
            hitLen = 0;
        } else {
            hitLen = dataList.size() * 40;
        }

        final int rrLen;
        if (rReq == null) {
            rrLen = 0;
        } else {
            List elems = rReq.getReadoutRequestElements();

            final int numElems;
            if (elems == null) {
                numElems = 0;
            } else {
                numElems = elems.size();
            }

            rrLen = 14 + (32 * numElems);
        }

        return 50 + rrLen + 8 + hitLen;
    }

    /**
     * Initializes Payload from backing so it can be used as an IPayload.
     */
    @Override
    public void loadPayload()
    {
        // do nothing
    }

    /**
     * Object knows how to recycle itself
     */
    @Override
    public void recycle()
    {
        // do nothing
    }

    @Override
    public void setCache(IByteBufferCache cache)
    {
        throw new Error("Unimplemented");
    }

    /**
     * Set the universal ID for global requests which will become events.
     *
     * @param uid new UID
     */
    @Override
    public void setUID(int uid)
    {
        throw new Error("Unimplemented");
    }

    public int writePayload(boolean writeLoaded, int destOffset, ByteBuffer buf)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    @Override
    public String toString()
    {
        return String.format("FlushRequest[%d#%d %d-%d]", type, UID, firstTime,
                             lastTime);
    }
}
