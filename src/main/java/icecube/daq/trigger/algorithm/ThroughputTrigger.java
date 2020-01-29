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

import org.apache.log4j.Logger;

/**
 * This class receives unconditional-triggers,
 *  makes a new TriggerRequest for each input TriggerRequest
 *  and then pass them to GlobalTrigBag.java.
 *
 * @version $Id: ThroughputTrigger.java 17683 2020-01-29 17:39:28Z dglo $
 * @author shseo
 */
public class ThroughputTrigger
        extends AbstractGlobalTrigger
{
    /* Log object for this class */
    private static final Logger LOG =
        Logger.getLogger(ThroughputTrigger.class);

    /** I3Live monitoring name for this algorithm */
    private static final String MONITORING_NAME = "THROUGHPUT";

    /** Numeric type for this algorithm */
    public static final int TRIGGER_TYPE = 3;

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
      * method to flush the trigger
      * basically indicates that there will be no further payloads to process
      */
    @Override
    public void flush()
    {
        //--nothing needs to be done in this ThroughputTrigger algorithm!
    }

    /**
     * Get the monitoring name.
     *
     * @return the name used for monitoring this trigger
     */
    @Override
    public String getMonitoringName()
    {
        return MONITORING_NAME;
    }

    /**
     * Get the trigger type.
     *
     * @return trigger type
     */
    @Override
    public int getTriggerType()
    {
        return TRIGGER_TYPE;
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

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    @Override
    public boolean isConfigured()
    {
        return true;
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
}
