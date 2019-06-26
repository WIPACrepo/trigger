package icecube.daq.trigger.test;

import icecube.daq.io.DAQComponentObserver;
import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.OutputChannel;
import icecube.daq.io.QueuedOutputChannel;
import icecube.daq.payload.IByteBufferCache;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class MockOutputProcess
    implements DAQComponentOutputProcess
{
    private String name;
    private MockOutputChannel outChan;
    private BaseValidator validator;

    public MockOutputProcess()
    {
        this(null);
    }

    public MockOutputProcess(String name)
    {
        this.name = name;
    }

    @Override
    public QueuedOutputChannel addDataChannel(WritableByteChannel chan,
                                              IByteBufferCache bufCache,
                                              String name)
    {
        throw new Error("Unimplemented");
    }

    @Override
    public QueuedOutputChannel connect(IByteBufferCache bufCache,
                                       WritableByteChannel chan, int srcId)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void destroyProcessor()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void disconnect()
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void forcedStopProcessing()
    {
        outChan.sendLastAndStop();
    }

    @Override
    public OutputChannel getChannel()
    {
        return outChan;
    }

    @Override
    public int getNumberOfChannels()
    {
        throw new Error("Unimplemented");
    }

    public int getNumberWritten()
    {
        if (outChan == null) {
            return 0;
        }

        return outChan.getNumberWritten();
    }

    @Override
    public String getPresentState()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public long getRecordsSent()
    {
        return outChan.getNumberWritten();
    }

    @Override
    public long getTotalRecordsSent()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public boolean isConnected()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public boolean isDestroyed()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public boolean isRunning()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public boolean isStopped()
    {
        return outChan.isStopped();
    }

    @Override
    public void registerComponentObserver(DAQComponentObserver observer)
    {
        throw new Error("Unimplemented");
    }

    @Override
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

    @Override
    public void start()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public void startProcessing()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public String toString()
    {
        if (name == null) {
            return "MockOutProc=>" + outChan;
        }

        return "MockOutProc[" + name + "]=>" + outChan;
    }
}
