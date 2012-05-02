/*
 * class: PhysicsMinBiasTrigger
 *
 * Version $Id: PhysicsMinBiasTrigger.java,v 1.20 2006/09/14 20:35:13 toale Exp $
 *
 * Date: August 27 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements a minimum bias trigger with a deadtime. It simply counts
 * hits that are not in a deadtime window and
 * applies a prescale for determining when a trigger should be formed.
 *
 * @version $Id: PhysicsMinBiasTrigger.java,v 1.20 2006/09/14 20:35:13 toale Exp $
 * @author pat
 */
public class PhysicsMinBiasTrigger extends AbstractTrigger
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(PhysicsMinBiasTrigger.class);

    private static int triggerNumber = 0;

    private int prescale;
    private int deadtime;
    private int numberProcessed = 0;

    private boolean configPrescale = false;
    private boolean configDeadtime = false;

    private IUTCTime deadtimeWindow = new UTCTime(0);

    public PhysicsMinBiasTrigger() {
        triggerNumber++;
    }

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    public boolean isConfigured() {
        return (configPrescale && configDeadtime);
    }

    /**
     * Add a parameter.
     *
     * @param parameter TriggerParameter object.
     *
     * @throws icecube.daq.trigger.exceptions.UnknownParameterException
     *
     */
    public void addParameter(TriggerParameter parameter) throws UnknownParameterException, IllegalParameterValueException {
        if (parameter.getName().compareTo("prescale") == 0) {
            prescale = Integer.parseInt(parameter.getValue());
            configPrescale = true;
        } else if (parameter.getName().compareTo("deadtime") == 0) {
            deadtime = Integer.parseInt(parameter.getValue());
            configDeadtime = true;
        } else if (parameter.getName().compareTo("triggerPrescale") == 0) {
            triggerPrescale = Integer.parseInt(parameter.getValue());
        } else if (parameter.getName().compareTo("domSet") == 0) {
            domSetId = Integer.parseInt(parameter.getValue());
            try {
                configHitFilter(domSetId);
            } catch (ConfigException ce) {
                throw new IllegalParameterValueException("Bad DomSet #" +
                                                         domSetId);
            }
        } else {
            throw new UnknownParameterException("Unknown parameter: " + parameter.getName());
        }
        super.addParameter(parameter);
    }

    public void setTriggerName(String triggerName) {
        super.triggerName = triggerName + triggerNumber;
        if (log.isInfoEnabled()) {
            log.info("TriggerName set to " + super.triggerName);
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
    public void runTrigger(IPayload payload) throws TriggerException {
        int interfaceType = payload.getPayloadInterfaceType();
        if ((interfaceType != PayloadInterfaceRegistry.I_HIT_PAYLOAD) &&
            (interfaceType != PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD)) {
            throw new TriggerException("Expecting an IHitPayload, got type " + interfaceType);
        }
        IHitPayload hit = (IHitPayload) payload;

	// make sure spe bit is on for this hit
	int type = AbstractTrigger.getHitType(hit);
	if (type != AbstractTrigger.SPE_HIT) {
	    if (log.isDebugEnabled()) {
		log.debug("Hit type is " + hit.getTriggerType() + ", returning.");
	    }
	    return;
	}

        // check hit filter
        if (!hitFilter.useHit(hit)) {
            if (log.isDebugEnabled()) {
                log.debug("Hit from DOM " + hit.getDOMID() + " not in DomSet");
            }
            return;
        }

        if (prescale == -1) {
            throw new TriggerException("Prescale has not been set!");
        }

	IUTCTime hitTime = hit.getHitTimeUTC();

	if (hitTime.compareTo(deadtimeWindow) <= 0) {
	    // this hit comes before the end of the deadtime window, do not count it
            IPayload earliest = new DummyPayload(hitTime.getOffsetUTCTime(0.1));
            setEarliestPayloadOfInterest(earliest);
	} else {
	    // this hit comes after the end of the deadtime window, count it
	    numberProcessed++;
	    deadtimeWindow = hitTime.getOffsetUTCTime(deadtime);
	    if (numberProcessed % prescale == 0) {
		// report this as a trigger and update the deadtime window
		formTrigger(hit, null, null);
	    } else {
		// just update earliest time of interest
		IPayload earliest = new DummyPayload(hitTime.getOffsetUTCTime(0.1));
		setEarliestPayloadOfInterest(earliest);
	    }
	}

    }

    /**
     * Flush the trigger. Basically indicates that there will be no further payloads to process.
     */
    public void flush() {
        // nothing to do here
    }

    public int getPrescale() {
        return prescale;
    }

    public void setPrescale(int prescale) {
        this.prescale = prescale;
    }

    public int getDeadtime() {
        return deadtime;
    }

    public void setDeadtime(int deadtime) {
        this.deadtime = deadtime;
    }

}
