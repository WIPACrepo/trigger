/*
 * class: MinBiasTrigger
 *
 * Version $Id: MinBiasTrigger.java,v 1.20 2006/09/14 20:35:13 toale Exp $
 *
 * Date: August 27 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements a simple minimum bias trigger that only cares about
 * the AMANDA sync mainboard. It simply counts hits and
 * applies a prescale for determining when a trigger should be formed.
 *
 * @version $Id: MinBiasTrigger.java,v 1.20 2006/09/14 20:35:13 toale Exp $
 * @author pat
 */
public class SyncBoardTrigger extends AbstractTrigger
{

    /**
     * Log object for this class
     */
    private static final Log LOG =
        LogFactory.getLog(SyncBoardTrigger.class);

    private static int triggerNumber = 0;

    private int prescale;
    private int numberProcessed = 0;

    private boolean configPrescale = false;

    public SyncBoardTrigger()
        throws IllegalParameterValueException
    {
        triggerNumber++;
        try {
            configHitFilter(0);
        } catch (ConfigException ce) {
            throw new IllegalParameterValueException("Bad DomSet #0", ce);
        }
    }

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    public boolean isConfigured()
    {
        return configPrescale;
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
    public void addParameter(String name, String value)
        throws UnknownParameterException, IllegalParameterValueException
    {
        if (name.compareTo("prescale") == 0) {
            prescale = Integer.parseInt(value);
            configPrescale = true;
        } else if (name.compareTo("triggerPrescale") == 0) {
            triggerPrescale = Integer.parseInt(value);
        } else {
            throw new UnknownParameterException("Unknown parameter: " + name);
        }
        super.addParameter(name, value);
    }

    public void setTriggerName(String triggerName)
    {
        super.triggerName = triggerName + triggerNumber;
        if (LOG.isInfoEnabled()) {
            LOG.info("TriggerName set to " + super.triggerName);
        }
    }

    /**
     * Run the trigger algorithm on a payload.
     *
     * @param payload payload to process
     *
     * @throws icecube.daq.trigger.exceptions.TriggerException
     *          if the algorithm doesn't like this payload
     */
    public void runTrigger(IPayload payload)
        throws TriggerException
    {
        int interfaceType = payload.getPayloadInterfaceType();
        if ((interfaceType != PayloadInterfaceRegistry.I_HIT_PAYLOAD) &&
            (interfaceType != PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD)) {
            throw new TriggerException("Expecting an IHitPayload, got type " + interfaceType);
        }
        IHitPayload hit = (IHitPayload) payload;

        // check hit filter
        if (!hitFilter.useHit(hit)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Hit from DOM " + hit.getDOMID() + " not in DomSet");
            }
            return;
        }

        if (prescale == -1) {
            throw new TriggerException("Prescale has not been set!");
        }

        numberProcessed++;
        if (numberProcessed % prescale == 0) {
            // report this as a trigger
            formTrigger(hit, null, null);

        } else {
            // just update earliest time of interest
            IPayload earliest = new DummyPayload(hit.getHitTimeUTC().getOffsetUTCTime(0.1));
            setEarliestPayloadOfInterest(earliest);
        }
    }

    /**
     * Flush the trigger. Basically indicates that there will be no further payloads to process.
     */
    public void flush()
    {
        // nothing to do here
    }

    public int getPrescale()
    {
        return prescale;
    }

    public void setPrescale(int prescale)
    {
        this.prescale = prescale;
    }

    /**
     * Get the monitoring name.
     *
     * @return the name used for monitoring this trigger
     */
    public String getMonitoringName()
    {
        return "SYNC_BOARD";
    }
}
