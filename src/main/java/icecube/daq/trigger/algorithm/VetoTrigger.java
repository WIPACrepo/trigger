/*
 * class: VetoTrigger
 *
 * Version $Id: VetoTrigger.java 15271 2014-11-19 18:46:22Z dglo $
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
 * @version $Id: VetoTrigger.java 15271 2014-11-19 18:46:22Z dglo $
 * @author shseo
 */
public abstract class VetoTrigger
        extends AbstractGlobalTrigger
{
    /**
    * Log object for this class
    */
    private static final Log LOG =
        LogFactory.getLog(VetoTrigger.class);

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
        LOG.debug("inside runTrigger in VetoTrigger");

        //--Configured trigger needs to be vetoed.
         if(!isConfiguredTrigger((ITriggerRequestPayload) payload))
         {
             miNumIncomingSelectedTriggers++;
             if (LOG.isDebugEnabled()) {
                 LOG.debug("Total number of incoming Unvetoed triggers so far = " + miNumIncomingSelectedTriggers);
             }
             try {
                 wrapTrigger((ITriggerRequestPayload) payload);
             } catch (Exception e) {
                 LOG.error("Couldn't wrap trigger", e);
             }
         }else
         {
             LOG.debug("This Trigger is being vetoed.");
             //DummyPayload dummy = new DummyPayload(((ITriggerRequestPayload) payload).getFirstTimeUTC());
             //setEarliestPayloadOfInterest(dummy);
         }
    }

    public void flush()
    {
        //--nothing needs to be done in this VetoTrigger algorithm!
    }

    /**
     * Get the monitoring name.
     *
     * @return the name used for monitoring this trigger
     */
    public String getMonitoringName()
    {
        return "VETO";
    }

    /**
     * Does this algorithm include all relevant hits in each request
     * so that it can be used to calculate multiplicity?
     *
     * @return <tt>true</tt> if this algorithm can supply a valid multiplicity
     */
    public boolean hasValidMultiplicity()
    {
        return true;
    }
}
