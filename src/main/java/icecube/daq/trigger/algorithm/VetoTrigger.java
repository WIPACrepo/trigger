/*
 * class: VetoTrigger
 *
 * Version $Id: VetoTrigger.java 4574 2009-08-28 21:32:32Z dglo $
 *
 * Date: January 25 2006
 *
 * (c) 2006 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IPayload;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.trigger.exceptions.TriggerException;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is to provide commond methods for any N-VetoTrigger.
 *
 * @version $Id: VetoTrigger.java 4574 2009-08-28 21:32:32Z dglo $
 * @author shseo
 */
public abstract class VetoTrigger
        extends AbstractGlobalTrigger
{
    /**
    * Log object for this class
    */
    private static final Log log = LogFactory.getLog(VetoTrigger.class);

    private int miNumIncomingSelectedTriggers;
    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public VetoTrigger()
    {
        super();
    }

    public abstract List getConfiguredTriggerIDs();

    public abstract boolean isConfiguredTrigger(ITriggerRequestPayload tPayload);

    public void runTrigger(IPayload payload) throws TriggerException
    {
        log.debug("inside runTrigger in VetoTrigger");

        //--Configured trigger needs to be vetoed.
         if(!isConfiguredTrigger((ITriggerRequestPayload) payload))
         {
             miNumIncomingSelectedTriggers++;
             if (log.isDebugEnabled()) {
                 log.debug("Total number of incoming Unvetoed triggers so far = " + miNumIncomingSelectedTriggers);
             }
             try {
                 wrapTrigger((ITriggerRequestPayload) payload);
             } catch (Exception e) {
                 log.error("Couldn't wrap trigger", e);
             }
         }else
         {
             log.debug("This Trigger is being vetoed.");
             //DummyPayload dummy = new DummyPayload(((ITriggerRequestPayload) payload).getFirstTimeUTC());
             //setEarliestPayloadOfInterest(dummy);
         }
    }

    public void flush()
    {
        //--nothing needs to be done in this VetoTrigger algorithm!
    }

}
