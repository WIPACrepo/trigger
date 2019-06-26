/*
 * class: RegularTriggers
 *
 * Version $Id: RegularTriggers.java, shseo
 *
 * Date: August 2 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IPayload;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class receives unconditional-triggers,
 *  makes a new TriggerRequest for each input TriggerRequest
 *  and then pass them to GlobalTrigBag.java.
 *
 * @version $Id: ThroughputTrigger.java 17114 2018-09-26 09:51:56Z dglo $
 * @author shseo
 */
public class ThroughputTrigger
        extends AbstractGlobalTrigger
{
    /**
     * Log object for this class
     */
    private static final Log LOG =
        LogFactory.getLog(ThroughputTrigger.class);

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public ThroughputTrigger()
    {
        super();
    }
    /**
     * This is the main method and called in GlobalTrigHandler.java.
     * Since this is ThroughputTrigger, this method do nothing but wrapping
     * an inputTrigger to GlobalTrigEvent (not the final GlobalTrigEvent yet.....).
     *
     * @param payload
     * @throws TriggerException
     */
    @Override
    public void runTrigger(IPayload payload) throws TriggerException
    {
        LOG.debug("inside runTrigger in ThroughputTrigger");
        //DummyPayload dummy = new DummyPayload(mtGlobalTrigEventPayload.getFirstTimeUTC());
        //setEarliestPayloadOfInterest(dummy);
        try {
            wrapTrigger((ITriggerRequestPayload) payload);
        } catch (Exception e) {
            LOG.error("Couldn't wrap trigger", e);
        }

    }

    /**
      * method to flush the trigger
      * basically indicates that there will be no further payloads to process
      */
    @Override
     public void flush()
     {
        //--nothing needs to be done in this ThroughputTrigger algorithm!
     }

    @Override
    public boolean isConfigured()
    {
        return true;
    }

    /**
     * Add a trigger parameter.
     *
     * @param name parameter name
     * @param value parameter value
     *
     * @throws UnknownParameterException if the parameter is unknown
     */
    @Override
    public void addParameter(String name, String value)
        throws UnknownParameterException
    {
        throw new UnknownParameterException("This trigger needs no parameter");
    }

    /**
     * Get the monitoring name.
     *
     * @return the name used for monitoring this trigger
     */
    @Override
    public String getMonitoringName()
    {
        return "THROUGHPUT";
    }

    /**
     * Does this algorithm include all relevant hits in each request
     * so that it can be used to calculate multiplicity?
     *
     * @return <tt>true</tt> if this algorithm can supply a valid multiplicity
     */
    @Override
    public boolean hasValidMultiplicity()
    {
        return true;
    }
}
