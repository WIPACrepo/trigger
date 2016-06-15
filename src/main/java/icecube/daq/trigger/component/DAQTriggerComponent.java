package icecube.daq.trigger.component;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.SpliceablePayloadReader;
import icecube.daq.juggler.component.IComponent;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.control.ITriggerManager;

import java.util.List;

/**
 * Expose common trigger methods for trigger rewrite.
 */
public interface DAQTriggerComponent
    extends IComponent
{
    /**
     * Attempt to send any cached trigger requests.
     */
    void flushTriggers();

    /**
     * Get the list of configured algorithms.
     *
     * @return list of algorithms
     */
    Iterable<ITriggerAlgorithm> getAlgorithms();

    /**
     * Get the ByteBufferCache used to track the incoming hit payloads
     *
     * @return input cache
     */
    IByteBufferCache getInputCache();

    /**
     * Get the component name.
     *
     * @return component name (without component ID)
     */
    String getName();

    /**
     * Get the ByteBufferCache used to track the outgoing request payloads
     *
     * @return output cache
     */
    IByteBufferCache getOutputCache();

    /**
     * Get the current number of hits/requests received
     *
     * @return hits/requests received
     */
    long getPayloadsReceived();

    /**
     * Get the current number of trigger requests written out
     *
     * @return requests sent
     */
    long getPayloadsSent();

    /**
     * Get the input reader for this component.
     *
     * @return input reader
     */
    SpliceablePayloadReader getReader();

    /**
     * Get the input splicer for this component.
     *
     * @return splicer
     */
    Splicer getSplicer();

    /**
     * Get the trigger manager for this component.
     *
     * @return trigger manager
     */
    ITriggerManager getTriggerManager();

    /**
     * Get the output process for this component.
     *
     * @return output process
     */
    DAQComponentOutputProcess getWriter();

    //Set<String> listMBeans();
}
