/*
 * class: FixedRateTrigger
 *
 * Version $Id: FixedRateTrigger.java 13357 2011-09-14 22:24:32Z seshadrivija $
 *
 * Date: May 1 2006
 *
 * (c) 2006 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadException;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements a trigger that is satisfied every N nanoseconds.
 *
 * @version $Id: FixedRateTrigger.java 13357 2011-09-14 22:24:32Z seshadrivija $
 * @author pat
 */
public class FixedRateTrigger extends AbstractTrigger
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(FixedRateTrigger.class);

    /**
     * unique id within this trigger type
     */
    private static int triggerNumber = 0;

    /**
     * Time interval between triggers (in nanoseconds)
     */
    private int interval;

    /**
     * Number of hits processed
     */
    private int numberOfHitsProcessed = 0;

    /**
     * flag to indicate that trigger has been configured
     */
    private boolean configInterval = false;

    /**
     * UTC time of next trigger
     */
    private IUTCTime nextTrigger;

    /**
     * Default constructor
     */
    public FixedRateTrigger() 
    {
        triggerNumber++;
    }

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    public boolean isConfigured()
    {
        return configInterval;
    }

    /**
     * Add a parameter.
     *
     * @param parameter TriggerParameter object.
     *
     * @throws icecube.daq.trigger.exceptions.UnknownParameterException
     *
     */
    public void addParameter(TriggerParameter parameter) 
        throws UnknownParameterException, IllegalParameterValueException 
    {
        if (parameter.getName().compareTo("interval") == 0) {
            interval = Integer.parseInt(parameter.getValue());
            configInterval = true;
        } else if (parameter.getName().compareTo("triggerPrescale") == 0) {
            triggerPrescale = Integer.parseInt(parameter.getValue());
        } else {
            throw new UnknownParameterException("Unknown parameter: " +
                parameter.getName());
        }
        super.addParameter(parameter);
    }

    /**
     * Set name of trigger, include triggerNumber
     * @param triggerName
     */
    public void setTriggerName(String triggerName) 
    {
        super.triggerName = triggerName + triggerNumber;
        if (log.isInfoEnabled()) {
            log.info("TriggerName set to " + super.triggerName);
        }
    }

    /**
     * Get interval.
     * @return time interval between triggers (in nanoseconds)
     */
    public int getInterval()
    {
        return interval;
    }

    /**
     * Set interval.
     * @param interval time interval between triggers (in nanoseconds)
     */
    public void setInterval(int interval) 
    {
        this.interval = interval;
    }

    /**
     * Flush the trigger. Basically indicates that there will be no 
     * further payloads to process.
     */
    public void flush() 
    {
        reset();
    }

    /**
     * reset
     */
    private void reset() 
    {
        numberOfHitsProcessed = 0;
        configInterval = false;
    }

    /**
     * Run the trigger algorithm on a payload.
     *
     * @param payload payload to process
     *
     * @throws icecube.daq.trigger.exceptions.TriggerException
     *          if the algorithm doesn't like this payload
     */
    public void runTrigger(IPayload payload) throws TriggerException 
    {
        // check that this is a hit
        int interfaceType = payload.getPayloadInterfaceType();
        if ((interfaceType != PayloadInterfaceRegistry.I_HIT_PAYLOAD) &&
            (interfaceType != PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD))
        {
            throw new TriggerException("Expecting an IHitPayload");
        }
        IHitPayload hit = (IHitPayload) payload;
        IUTCTime hitTimeUTC = hit.getHitTimeUTC();

        if (numberOfHitsProcessed == 0) {
            // set time of first trigger to be first hit time + interval
            nextTrigger = hitTimeUTC.getOffsetUTCTime(interval);
        } else {
            // issue triggers until one comes after this hit
            while (hitTimeUTC.compareTo(nextTrigger) >= 0) {
                try {
                    formTrigger(nextTrigger);
                } catch (PayloadException pe) {
                    throw new TriggerException("Cannot form trigger", pe);
                }
                nextTrigger = nextTrigger.getOffsetUTCTime(interval);
            }
        }

        IPayload oldHitPlus = new DummyPayload(hitTimeUTC.
            getOffsetUTCTime(0.1));
        setEarliestPayloadOfInterest(oldHitPlus);
        numberOfHitsProcessed++;
    }

}
