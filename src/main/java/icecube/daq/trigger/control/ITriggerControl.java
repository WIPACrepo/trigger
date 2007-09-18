/*
 * interface: ITriggerControl
 *
 * Version $Id: ITriggerControl.java,v 1.4 2005/12/06 22:29:54 toale Exp $
 *
 * Date: August 22 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.IPayload;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;

/**
 * This interface defines the control aspect of a trigger.
 *
 * @version $Id: ITriggerControl.java,v 1.4 2005/12/06 22:29:54 toale Exp $
 * @author pat
 */
public interface ITriggerControl
{

    /**
     * Get the earliest payload still of interest to this trigger.
     * @return earliest payload of interest
     */
    IPayload getEarliestPayloadOfInterest();

    /**
     * Set the trigger handler of this trigger.
     * @param triggerHandler trigger handler
     */
    void setTriggerHandler(ITriggerHandler triggerHandler);

    /**
     * Get the trigger handler of this trigger.
     * @return thr trigger handler
     */
    ITriggerHandler getTriggerHandler();

    /**
     * Set the TriggerRequestPayloadFactory for this trigger.
     * @param triggerFactory payload factory
     */
    void setTriggerFactory(TriggerRequestPayloadFactory triggerFactory);

    /**
     * Run the trigger algorithm on a payload.
     * @param payload payload to process
     * @throws icecube.daq.trigger.exceptions.TriggerException if the algorithm doesn't like this payload
     */
    void runTrigger(IPayload payload) throws TriggerException;

    /**
     * Flush the trigger.
     * Basically indicates that there will be no further payloads to process.
     */
    void flush();

}
