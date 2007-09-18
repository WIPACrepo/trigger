package icecube.daq.trigger.test;

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.PayloadDestination;
import icecube.daq.payload.PayloadInterfaceRegistry;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.zip.DataFormatException;

public abstract class MockPayload
    implements Comparable, ILoadablePayload, IWriteablePayload
{
    private IUTCTime time;
    private DataFormatException loadDFException;
    private IOException loadIOException;

    public MockPayload(long timeVal)
    {
        time = new MockUTCTime(timeVal);
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
        if (time == null) {
            if (mp.time == null) {
                return 0;
            }

            return 1;
        } else if (mp.time == null) {
            return -1;
        }

        return (int) (mp.time.getUTCTimeAsLong() - time.getUTCTimeAsLong());
    }

    public abstract Object deepCopy();

    public void dispose()
    {
        throw new Error("Unimplemented");
    }

    public boolean equals(Object obj)
    {
        return compareTo(obj) == 0;
    }

    public abstract int getPayloadInterfaceType();

    public abstract int getPayloadLength();

    public IUTCTime getPayloadTimeUTC()
    {
        return time;
    }

    public abstract int getPayloadType();

    public int hashCode()
    {
        if (time == null) {
            return 0;
        }

        final long timeVal = time.getUTCTimeAsLong();

        final long modValue = Integer.MAX_VALUE / 256;

        final long topTwo = timeVal / modValue;

        return (int) (topTwo / modValue) + (int) (topTwo % modValue) +
            (int) (timeVal % modValue);
    }

    public void loadPayload()
        throws IOException, DataFormatException
    {
        if (loadDFException != null) {
            throw loadDFException;
        } else if (loadIOException != null) {
            throw loadIOException;
        }

        // do nothing
    }

    public void recycle()
    {
        // do nothing
    }

    public void setLoadPayloadException(Exception ex)
    {
        if (ex instanceof DataFormatException) {
            loadDFException = (DataFormatException) ex;
        } else if (ex instanceof IOException) {
            loadIOException = (IOException) ex;
        } else {
            throw new Error("Unknown exception type " +
                            ex.getClass().getName() + ": " + ex);
        }
    }

    public abstract int writePayload(boolean writeLoaded,
                                     PayloadDestination dest)
        throws IOException;

    public abstract int writePayload(boolean writeLoaded, int offset,
                                     ByteBuffer buf)
        throws IOException;
}
