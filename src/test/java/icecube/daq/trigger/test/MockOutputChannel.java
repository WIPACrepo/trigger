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

    int getNumberWritten()
    {
        return numWritten;
    }

    public boolean isStopped()
    {
        return stopped;
    }

    @Override
    public void receiveByteBuffer(ByteBuffer buf)
    {
        numWritten++;
        if (validator != null) {
            validator.validate(buf);
        }
    }

    @Override
    public void sendLastAndStop()
    {
        stopped = true;
    }

    public void setValidator(BaseValidator validator)
    {
        this.validator = validator;
    }

    @Override
    public String toString()
    {
        return String.format("MockOutChan[wrote %d%s]", numWritten,
                             (stopped ? ",stopped" : ""));
    }
}
