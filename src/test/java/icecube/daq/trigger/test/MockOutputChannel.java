package icecube.daq.trigger.test;

import icecube.daq.io.DAQComponentObserver;
import icecube.daq.io.OutputChannel;
import java.nio.ByteBuffer;

public class MockOutputChannel
    implements OutputChannel
{
    private int numWritten;
    private boolean stopped;
    private BaseValidator validator;

    public MockOutputChannel()
    {
    }

    public void destinationClosed()
    {
        throw new Error("Unimplemented");
    }

    public void flushOutQueue()
    {
        throw new Error("Unimplemented");
    }

    int getNumberWritten()
    {
        return numWritten;
    }

    public boolean isOutputQueued()
    {
        throw new Error("Unimplemented");
    }

    public boolean isStopped()
    {
        return stopped;
    }

    public void receiveByteBuffer(ByteBuffer buf)
    {
        numWritten++;
    }

    public void registerComponentObserver(DAQComponentObserver observer,
                                          String id)
    {
        throw new Error("Unimplemented");
    }

    public void sendLastAndStop()
    {
        stopped = true;
    }

    public void setValidator(BaseValidator validator)
    {
        this.validator = validator;
    }
}
