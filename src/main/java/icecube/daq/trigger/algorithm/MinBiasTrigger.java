/*
 * class: MinBiasTrigger
 *
 * Version $Id: MinBiasTrigger.java 17207 2018-11-08 16:08:59Z dglo $
 *
 * Date: August 27 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import org.apache.log4j.Logger;

/**
 * This class implements a simple minimum bias trigger. It simply counts hits and
 * applies a prescale for determining when a trigger should be formed.
 *
 * @version $Id: MinBiasTrigger.java 17207 2018-11-08 16:08:59Z dglo $
 * @author pat
 */
public class MinBiasTrigger
    extends AbstractTrigger
{
    /** Log object for this class */
    private static final Logger LOG = Logger.getLogger(MinBiasTrigger.class);

    private static int nextTriggerNumber;
    private int triggerNumber;

    private int prescale;
    private int numberProcessed = 0;

    private boolean configPrescale = false;

    public MinBiasTrigger()
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
        if (name.compareTo("prescale") == 0) {
            prescale = Integer.parseInt(value);
            configPrescale = true;
        } else if (name.compareTo("triggerPrescale") == 0) {
            triggerPrescale = Integer.parseInt(value);
        } else if (name.compareTo("domSet") == 0) {
            domSetId = Integer.parseInt(value);
            try {
                configHitFilter(domSetId);
            } catch (ConfigException ce) {
                throw new IllegalParameterValueException("Bad DomSet #" +
                                                         domSetId, ce);
            }
        } else {
            throw new UnknownParameterException("Unknown parameter: " + name);
        }
        super.addParameter(name, value);
    }

    /**
     * Flush the trigger. Basically indicates that there will be no further
     * payloads to process.
     */
    @Override
    public void flush()
    {
        // nothing to do here
    }

    public int getPrescale()
    {
        return prescale;
    }

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    @Override
    public boolean isConfigured()
    {
        return configPrescale;
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
        if (prescale == -1) {
            throw new TriggerException("Prescale has not been set!");
        }

        int interfaceType = payload.getPayloadInterfaceType();
        if ((interfaceType != PayloadInterfaceRegistry.I_HIT_PAYLOAD) &&
            (interfaceType != PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD))
        {
            throw new TriggerException("Expecting an IHitPayload, got type " +
                                       interfaceType);
        }
        IHitPayload hit = (IHitPayload) payload;

        boolean formedTrigger = false;
        if (hitFilter.useHit(hit)) {
            numberProcessed++;
            if (numberProcessed % prescale == 0) {
                // report this as a trigger
                formTrigger(hit, null, null);
                formedTrigger = true;
            }
        }

        if (!formedTrigger) {
            // just update earliest time of interest
            IUTCTime offsetTime = hit.getHitTimeUTC().getOffsetUTCTime(0.1);
            IPayload earliest = new DummyPayload(offsetTime);
            setEarliestPayloadOfInterest(earliest);
        }
    }

    public void setPrescale(int prescale)
    {
        this.prescale = prescale;
    }

    @Override
    public void setTriggerName(String triggerName)
    {
        super.triggerName = triggerName + triggerNumber;
        if (LOG.isInfoEnabled()) {
            LOG.info("TriggerName set to " + super.triggerName);
        }
    }

    /**
     * Get the monitoring name.
     *
     * @return the name used for monitoring this trigger
     */
    @Override
    public String getMonitoringName()
    {
        return "MIN_BIAS";
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
