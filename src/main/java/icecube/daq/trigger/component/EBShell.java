package icecube.daq.trigger.component;

import icecube.daq.io.DAQComponentObserver;
import icecube.daq.io.IOChannelParent;
import icecube.daq.io.InputChannel;
import icecube.daq.io.PayloadReader;
import icecube.daq.io.SimpleOutputEngine;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;
import icecube.daq.juggler.component.DAQComponent;
import icecube.daq.juggler.component.DAQConnector;
import icecube.daq.juggler.mbean.MemoryStatistics;
import icecube.daq.juggler.mbean.SystemStatistics;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.impl.VitreousBufferCache;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;

class DevNullChannel
    extends InputChannel
{
    private IByteBufferCache bufMgr;

    DevNullChannel(IOChannelParent parent, SelectableChannel channel,
                   String name, IByteBufferCache bufMgr, int bufSize)
        throws IOException
    {
        super(parent, channel, name, bufMgr, bufSize);

        this.bufMgr = bufMgr;
    }

    public void pushPayload(ByteBuffer payBuf)
        throws IOException
    {
        bufMgr.returnBuffer(payBuf);
    }

    public void registerComponentObserver(DAQComponentObserver observer,
                                          String notificationID)
    {
        throw new Error("Unimplemented");
    }
}

class DevNullReader
    extends PayloadReader
{
    DevNullReader(String name)
    {
        super(name);
    }

    public InputChannel createChannel(SelectableChannel channel, String name,
                                      IByteBufferCache bufMgr, int bufSize)
        throws IOException
    {
        return new DevNullChannel(this, channel, name, bufMgr, bufSize);
    }
}

/**
 * Debugging shell which reads from stringHubs and does nothing else.
 */
public class EBShell
    extends DAQComponent
{
    private static final String COMPONENT_NAME = "ebShell";
    private static final int COMPONENT_ID = 0;

    public EBShell(String name, int id)
    {
        super(name, id);

        addMBean("jvm", new MemoryStatistics());
        addMBean("system", new SystemStatistics());

        IByteBufferCache inCache = new VitreousBufferCache("EBIn", 250000000);
        addCache(DAQConnector.TYPE_READOUT_DATA, inCache);

        // Create and register io engines
        PayloadReader rdoutDataIn = new DevNullReader(name);
        addMonitoredEngine(DAQConnector.TYPE_READOUT_DATA, rdoutDataIn);

        // Create the outgoing buffer cache
        IByteBufferCache outCache = new VitreousBufferCache("EBOut");
        addCache(DAQConnector.TYPE_READOUT_REQUEST, outCache);

        SimpleOutputEngine rdoutReqOut =
            new SimpleOutputEngine(name, id, name + "Output");
        addMonitoredEngine(DAQConnector.TYPE_READOUT_REQUEST, rdoutReqOut,
                           true);
    }

    public String getVersionInfo()
    {
        return "$Id$";
    }

    public static void main(String[] args)
        throws DAQCompException
    {
        DAQCompServer srvr;
        try {
            srvr = new DAQCompServer(new EBShell(COMPONENT_NAME, COMPONENT_ID),
                                     args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
            return; // without this, compiler whines about uninitialized 'srvr'
        }
        srvr.startServing();
    }
}
