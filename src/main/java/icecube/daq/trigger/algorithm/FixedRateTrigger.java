/*
 * class: FixedRateTrigger
 *
 * Version $Id: FixedRateTrigger.java 17449 2019-07-03 18:47:17Z dglo $
 *
 * Date: May 1 2006
 *
 * (c) 2006 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import org.apache.log4j.Logger;

/**
 * This class implements a trigger that is satisfied every N nanoseconds.
 *
 * @version $Id: FixedRateTrigger.java 17449 2019-07-03 18:47:17Z dglo $
 * @author pat
 */
public class FixedRateTrigger
    extends AbstractTrigger
{
    /** Numeric type for this algorithm */
    public static final int TRIGGER_TYPE = 23;

    /** Log object for this class */
    private static final Logger LOG =
        Logger.getLogger(FixedRateTrigger.class);

    /** I3Live monitoring name for this algorithm */
    private static final String MONITORING_NAME = "UNBIASED";

    /**
     * unique id within this trigger type
     */
    private static int nextTriggerNumber;
    private int triggerNumber;

    /**
     * Time interval between triggers (in integral number of nanoseconds)
     */
    private long interval;

    /**
     * Number of hits processed
     */
    private long numberOfHitsProcessed = 0;

    /**
     * flag to indicate that trigger has been configured
     */
    private boolean configInterval;

    /**
     * UTC time of next trigger
     */
    private IUTCTime nextTrigger;

    /**
     * Default constructor
     */
    public FixedRateTrigger()
    {
        triggerNumber = ++nextTriggerNumber;
    }

    /**
     * Add a trigger parameter.
     *
     * @param name parameter name
     * @param value parameter value
     *
     * @throws UnknownParameterException if the parameter is unknown
     * @throws IllegalParameterValueException if the parameter value is bad
     */
    @Override
    public void addParameter(String name, String value)
        throws UnknownParameterException, IllegalParameterValueException
    {
        if (name.compareTo("interval") == 0) {
            String txt = value.trim();
            if (txt.matches("[0-9]+"))
                interval = Long.parseLong(txt);
            else
                interval = (long) (Double.parseDouble(txt) * 1.0E9);
            configInterval = true;
        } else if (name.compareTo("triggerPrescale") == 0) {
            triggerPrescale = Integer.parseInt(value);
        } else {
            throw new UnknownParameterException("Unknown parameter: " + name);
        }
        super.addParameter(name, value);
    }

    /**
     * Set name of trigger, include triggerNumber
     * @param triggerName
     */
    @Override
    public void setTriggerName(String triggerName)
    {
        super.triggerName = triggerName + triggerNumber;
        if (LOG.isInfoEnabled()) {
            LOG.info("TriggerName set to " + super.triggerName);
        }
    }

    /**
     * Get interval.
     * @return time interval between triggers (in nanoseconds)
     */
    public long getInterval()
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
     * Flush the trigger. Basically indicates that there will be no further payloads to process.
     */
    @Override
    public void flush()
    {
        // nothing to do here
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
        return false;
    }

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    @Override
    public boolean isConfigured()
    {
        return configInterval;
    }

    /**
     * Reset the algorithm to its initial condition.
     */
    @Override
    public void resetAlgorithm()
    {
        numberOfHitsProcessed = 0;

        super.resetAlgorithm();
    }

    /**
     * Run the trigger algorithm on a payload.
     *
     * @param payload payload to process
     *
     * @throws icecube.daq.trigger.exceptions.TriggerException
     *          if the algorithm doesn't like this payload
     */
    @Override
    public void runTrigger(IPayload payload)
        throws TriggerException
    {
        // check that this is a hit
        int interfaceType = payload.getPayloadInterfaceType();
        if ((interfaceType != PayloadInterfaceRegistry.I_HIT_PAYLOAD) &&
            (interfaceType != PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD)) {
            throw new TriggerException("Expecting an IHitPayload");
        }
        IHitPayload hit = (IHitPayload) payload;
        IUTCTime hitTimeUTC = hit.getHitTimeUTC();

        if (numberOfHitsProcessed == 0) {
            // set time of first trigger to be first hit time + interval
            nextTrigger = hitTimeUTC.getOffsetUTCTime(interval * 10L);
        } else {
            // issue triggers until one comes after this hit
            while (hitTimeUTC.compareTo(nextTrigger) >= 0) {
                formTrigger(nextTrigger);
                nextTrigger = nextTrigger.getOffsetUTCTime(interval * 10L);
            }
        }

        IPayload oldHitPlus = new DummyPayload(hitTimeUTC.getOffsetUTCTime(1));
        setEarliestPayloadOfInterest(oldHitPlus);
        numberOfHitsProcessed++;
    }
}
