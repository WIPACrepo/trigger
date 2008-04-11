package icecube.daq.trigger.test;

import icecube.daq.io.DAQComponentObserver;
import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.OutputChannel;
import icecube.daq.payload.IByteBufferCache;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class MockOutputProcess
    implements DAQComponentOutputProcess
{
    private MockOutputChannel outChan;
    private BaseValidator validator;

    public MockOutputProcess()
    {
    }

    public OutputChannel addDataChannel(WritableByteChannel chan,
                                        IByteBufferCache bufCache)
    {
        throw new Error("Unimplemented");
    }

    public OutputChannel connect(IByteBufferCache bufCache,
                                 WritableByteChannel chan, int srcId)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    public void destroyProcessor()
    {
        throw new Error("Unimplemented");
    }

    public void disconnect()
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    public void forcedStopProcessing()
    {
        outChan.sendLastAndStop();
    }

    public OutputChannel getChannel()
    {
        return outChan;
    }

    public int getNumberWritten()
    {
        if (outChan == null) {
            return 0;
        }

        return outChan.getNumberWritten();
    }

    public String getPresentState()
    {
        throw new Error("Unimplemented");
    }

    public boolean isConnected()
    {
        throw new Error("Unimplemented");
    }

    public boolean isDestroyed()
    {
        throw new Error("Unimplemented");
    }

    public boolean isRunning()
    {
        throw new Error("Unimplemented");
    }

    public boolean isStopped()
    {
        throw new Error("Unimplemented");
    }

    public void registerComponentObserver(DAQComponentObserver observer)
    {
        throw new Error("Unimplemented");
    }

    public void sendLastAndStop()
    {
        if (outChan == null) {
            throw new Error("Output channel has not been set");
        }

        outChan.sendLastAndStop();
    }

    public void setOutputChannel(MockOutputChannel outChan)
    {
        if (this.outChan != null) {
            throw new Error("OutputChannel is already set");
        }

        this.outChan = outChan;

        if (validator != null) {
            this.outChan.setValidator(validator);
        }
    }

    public void setValidator(BaseValidator validator)
    {
        this.validator = validator;

        if (outChan != null) {
            outChan.setValidator(validator);
        }
    }

    public void start()
    {
        throw new Error("Unimplemented");
    }

    public void startProcessing()
    {
        throw new Error("Unimplemented");
    }
}
