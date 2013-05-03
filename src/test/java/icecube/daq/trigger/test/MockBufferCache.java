package icecube.daq.trigger.test;

import icecube.daq.payload.IByteBufferCache;

import java.nio.ByteBuffer;

public class MockBufferCache
    implements IByteBufferCache
{
    public MockBufferCache()
    {
    }

    public ByteBuffer acquireBuffer(int bytes)
    {
        throw new Error("Unimplemented");
    }

    public void destinationClosed()
    {
        throw new Error("Unimplemented");
    }

    public void flush()
    {
        throw new Error("Unimplemented");
    }

    public int getCurrentAquiredBuffers()
    {
        throw new Error("Unimplemented");
    }

    public long getCurrentAquiredBytes()
    {
        throw new Error("Unimplemented");
    }


    public boolean getIsCacheBounded()
    {
        throw new Error("Unimplemented");
    }

    public long getMaxAquiredBytes()
    {
        throw new Error("Unimplemented");
    }

    public String getName()
    {
        throw new Error("Unimplemented");
    }

    public int getTotalBuffersAcquired()
    {
        throw new Error("Unimplemented");
    }

    public int getTotalBuffersCreated()
    {
        throw new Error("Unimplemented");
    }

    public int getTotalBuffersReturned()
    {
        throw new Error("Unimplemented");
    }

    public long getTotalBytesInCache()
    {
        throw new Error("Unimplemented");
    }

    public boolean isBalanced()
    {
        throw new Error("Unimplemented");
    }

    public void receiveByteBuffer(ByteBuffer buf)
    {
        throw new Error("Unimplemented");
    }

    public void returnBuffer(ByteBuffer buf)
    {
        throw new Error("Unimplemented");
    }

    public void returnBuffer(int bytes)
    {
        throw new Error("Unimplemented");
    }
}
