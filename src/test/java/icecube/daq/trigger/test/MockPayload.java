package icecube.daq.trigger.test;

import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.PayloadFormatException;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class MockPayload
    implements Comparable, ILoadablePayload, IWriteablePayload
{
    private long time;
    private PayloadFormatException loadPFException;
    private IOException loadIOException;

    private IUTCTime timeObj;

    public MockPayload(long timeVal)
    {
        time = timeVal;
    }

    public int compareTo(Object obj)
    {
        if (obj == null) {
            return 1;
        }

        if (!(obj instanceof MockPayload)) {
            return getClass().getName().compareTo(obj.getClass().getName());
        }

        MockPayload mp = (MockPayload) obj;
        return (int) (mp.time - time);
    }

    public Object deepCopy()
    {
        throw new Error("Unimplemented");
    }

    public void dispose()
    {
        throw new Error("Unimplemented");
    }

    public boolean equals(Object obj)
    {
        return compareTo(obj) == 0;
    }

    public ByteBuffer getPayloadBacking()
    {
        throw new Error("Unimplemented");
    }

    public int getPayloadLength()
    {
        return length();
    }

    public IUTCTime getPayloadTimeUTC()
    {
        if (timeObj == null) {
            timeObj = new MockUTCTime(time);
        }

        return timeObj;
    }

    public int getPayloadType()
    {
        throw new Error("Unimplemented");
    }

    public long getUTCTime()
    {
        return time;
    }

    public int hashCode()
    {
        final long modValue = Integer.MAX_VALUE / 256;

        final long topTwo = time / modValue;

        return (int) (topTwo / modValue) + (int) (topTwo % modValue) +
            (int) (time % modValue);
    }

    public int length()
    {
        throw new Error("Unimplemented");
    }

    public void loadPayload()
        throws IOException, PayloadFormatException
    {
        if (loadPFException != null) {
            throw loadPFException;
        } else if (loadIOException != null) {
            throw loadIOException;
        }

        // do nothing
    }

    public void recycle()
    {
        // do nothing
    }

    public void setCache(IByteBufferCache bufCache)
    {
        throw new Error("Unimplemented");
    }

    public void setLoadPayloadException(Exception ex)
    {
        if (ex instanceof PayloadFormatException) {
            loadPFException = (PayloadFormatException) ex;
        } else if (ex instanceof IOException) {
            loadIOException = (IOException) ex;
        } else {
            throw new Error("Unknown exception type " +
                            ex.getClass().getName() + ": " + ex);
        }
    }

    public int writePayload(boolean writeLoaded, int offset, ByteBuffer buf)
        throws IOException
    {
        throw new Error("Unimplemented");
    }
}
