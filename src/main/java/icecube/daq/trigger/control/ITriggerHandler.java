/*
 * interface: ITriggerManager
 *
 * Version $Id: ITriggerHandler.java,v 1.6 2006/08/08 20:26:29 vav111 Exp $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.IPayload;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.trigger.monitor.TriggerHandlerMonitor;

import java.util.List;

/**
 * This interface defines the behavior of a TriggerHandler
 *
 * @version $Id: ITriggerHandler.java,v 1.6 2006/08/08 20:26:29 vav111 Exp $
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
    void addTrigger(ITriggerControl iTrigger);

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

}
