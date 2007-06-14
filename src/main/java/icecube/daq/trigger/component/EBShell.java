package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;

import icecube.daq.io.InputChannel;
import icecube.daq.io.InputChannelParent;
import icecube.daq.io.PayloadDestinationOutputEngine;
import icecube.daq.io.PayloadReader;

import icecube.daq.juggler.component.DAQCompServer;
import icecube.daq.juggler.component.DAQComponent;
import icecube.daq.juggler.component.DAQConnector;
import icecube.daq.juggler.component.DAQCompException;

import icecube.daq.juggler.mbean.MemoryStatistics;
import icecube.daq.juggler.mbean.SystemStatistics;

import icecube.daq.payload.ByteBufferCache;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayloadDestinationCollection;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.MasterPayloadFactory;
import icecube.daq.payload.SourceIdRegistry;

import icecube.daq.trigger.control.ITriggerControl;
import icecube.daq.trigger.control.ITriggerManager;

import icecube.daq.trigger.monitor.TriggerHandlerMonitor;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.nio.channels.SelectableChannel;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


class DevNullChannel
    extends InputChannel
{
    private IByteBufferCache bufMgr;

    DevNullChannel(InputChannelParent parent, SelectableChannel channel,
                   IByteBufferCache bufMgr, int bufSize)
        throws IOException
    {
        super(parent, channel, bufMgr, bufSize);

        this.bufMgr = bufMgr;
    }

    public void pushPayload(ByteBuffer payBuf)
        throws IOException
    {
        bufMgr.returnBuffer(payBuf);
    }
}

class DevNullReader
    extends PayloadReader
{
    DevNullReader(String name)
    {
        super(name);
    }

    public InputChannel createChannel(SelectableChannel channel,
                                      IByteBufferCache bufMgr, int bufSize)
        throws IOException
    {
        return new DevNullChannel(this, channel, bufMgr, bufSize);
    }
}

/**
 * Debugging shell which reads from stringHubs and does nothing else.
 */
public class EBShell
    extends DAQComponent
{
    private static final Log log = LogFactory.getLog(EBShell.class);

    private static final String COMPONENT_NAME = "ebShell";
    private static final int COMPONENT_ID = 0;

    public EBShell(String name, int id)
    {
        super(name, id);

        // Create the buffer cache
        IByteBufferCache bufferCache =
            new ByteBufferCache(256, 250000000L, 225000000L, name);
        addCache(bufferCache);

        addMBean("jvm", new MemoryStatistics());
        addMBean("system", new SystemStatistics());

        // Create and register io engines
        PayloadReader rdoutDataIn = new DevNullReader(name);
        addMonitoredEngine(DAQConnector.TYPE_READOUT_DATA, rdoutDataIn);

        PayloadDestinationOutputEngine rdoutReqOut =
            new PayloadDestinationOutputEngine(name, id, name + "Output");
        rdoutReqOut.registerBufferManager(bufferCache);
        addMonitoredEngine(DAQConnector.TYPE_READOUT_REQUEST, rdoutReqOut,
                           true);
    }

    public static void main(String[] args)
        throws DAQCompException
    {
        new DAQCompServer(new EBShell(COMPONENT_NAME, COMPONENT_ID), args);
    }
}
