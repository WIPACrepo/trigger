/*
 * class: ITriggerBag
 *
 * Version $Id: ITriggerBag.java,v 1.3 2005/12/29 23:17:35 toale Exp $
 *
 * Date: March 16 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.trigger.impl.TriggerRequestPayload;
import icecube.daq.trigger.monitor.PayloadBagMonitor;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.splicer.PayloadFactory;

/**
 *
 * Interface for trigger bag
 *
 * @version $Id: ITriggerBag.java,v 1.3 2005/12/29 23:17:35 toale Exp $
 * @author pat
 */
public interface ITriggerBag
{

    /**
     * add another payload to the bag
     * @param payload payload to add
     */
    void add(IPayload payload);

    /**
     * method to flush the bag, allow all payloads to go free
     */ 
    void flush();

    /**
     * test to see if there is a trigger available for release
     * @return true if there is a trigger to release
     */
    boolean hasNext();

    /**
     * get next trigger from bag
     * @return next available trigger
     */
    TriggerRequestPayload next();

    /**
     * get the current time of the bag
     * @return current UTC time
     */
    IUTCTime getTimeGate();

    /**
     * set the current time of the bag
     * @param time time to set
     */
    void setTimeGate(IUTCTime time);

    /**
     * get size of bag
     * @return number of triggers in bag
     */
    int size();

    /**
     * set the factory for the bag
     * @param payloadFactory payload factory
     */
    void setPayloadFactory(PayloadFactory payloadFactory);

    /**
     * Get the monitor object.
     * @return a PayloadBagMonitor
     */
    PayloadBagMonitor getMonitor();

    /**
     * Set the monitor object.
     * @param monitor
     */
    void setMonitor(PayloadBagMonitor monitor);

}
