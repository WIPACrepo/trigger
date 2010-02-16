/*
 * interface: ITriggerManager
 *
 * Version $Id: ITriggerHandler.java 4891 2010-02-16 21:09:34Z dglo $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.trigger.algorithm.ITrigger;
import icecube.daq.trigger.monitor.TriggerHandlerMonitor;
import icecube.daq.util.DOMRegistry;

import java.util.List;

/**
 * This interface defines the behavior of a TriggerHandler
 *
 * @version $Id: ITriggerHandler.java 4891 2010-02-16 21:09:34Z dglo $
 * @author pat
 */
public interface ITriggerHandler extends IPayloadProducer
{

    /**
     * add a new trigger request to the trigger bag
     * @param payload new trigger request
     */
    void addToTriggerBag(ILoadablePayload payload);

    /**
     * add a trigger to the list of managed triggers
     * @param iTrigger trigger to add
     */
    void addTrigger(ITrigger iTrigger);

    /**
     * add a list of triggers
     * @param triggers
     */
    void addTriggers(List triggers);

    /**
     * clear list of triggers
     */
    void clearTriggers();

    /**
     * flush the handler
     * including the input buffer, all triggers, and the output bag
     */
    void flush();

    /**
     * process next payload
     * @param payload payload to process
     */
    void process(ILoadablePayload payload);

    /**
     * Reset the handler for a new run.
     */
    void reset();

    /**
     * Get the SourceID
     * @return a ISourceID
     */
    ISourceID getSourceID();

    /**
     * Get the monitor object.
     * @return a TriggerHandlerMonitor
     */
    TriggerHandlerMonitor getMonitor();

    /**
     * Set the DOMRegistry that should be used.
     * @param registry A configured DOMRegistry
     */
    void setDOMRegistry(DOMRegistry registry);

    /**
     * Get the DOMRegistry.
     * @return the DOMRegistry to use
     */
    DOMRegistry getDOMRegistry();

    /**
     * Get the number of payloads processed.
     * @return number of payloads processed
     */
    int getCount();

    /**
     * Set the outgoing payload buffer cache.
     * @param byte buffer cache manager
     */
    void setOutgoingBufferCache(IByteBufferCache cache);
}
