/*
 * class: CalibrationTrigger
 *
 * Version $Id: CalibrationTrigger.java,v 1.19 2006/09/14 20:35:13 toale Exp $
 *
 * Date: August 27 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.PayloadInterfaceRegistry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is an implementation of a calibration trigger. It checks each hit
 * to see if it is the correct type (flasher hit for instance), and if so it
 * forms a TriggerRequestPayload.
 *
 * This trigger is an example of an 'instantaneous trigger' since it is capable
 * of making a decision based only on the current hit.
 *
 * @version $Id: CalibrationTrigger.java,v 1.19 2006/09/14 20:35:13 toale Exp $
 * @author pat
 */
public class CalibrationTrigger extends AbstractTrigger
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(CalibrationTrigger.class);

    private static int triggerNumber = 0;

    /**
     * Type of hit to trigger on.
     */
    private int hitType;

    private boolean configHitType = false;

    /**
     * Constructor.
     */
    public CalibrationTrigger() {
        triggerNumber++;
    }

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    public boolean isConfigured() {
        return configHitType;
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
        if (parameter.getName().compareTo("hitType") == 0) {
            hitType = Integer.parseInt(parameter.getValue());
            configHitType = true;
        } else if (parameter.getName().compareTo("triggerPrescale") == 0) {
            triggerPrescale = Integer.parseInt(parameter.getValue());
        } else if (parameter.getName().compareTo("domSet") == 0) {
            domSetId = Integer.parseInt(parameter.getValue());
            configHitFilter(domSetId);
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
            throw new TriggerException("Expecting an IHitPayload");
        }
        IHitPayload hit = (IHitPayload) payload;

        // check hit filter
        if (!hitFilter.useHit(hit)) {
            if (log.isDebugEnabled()) {
                log.debug("Hit from DOM " + hit.getDOMID().getDomIDAsString() + " not in DomSet");
            }
            return;
        }

        // check the hit type
        int type = AbstractTrigger.getHitType(hit);
        if (type == hitType) {
            // this is the correct type, report trigger
            formTrigger(hit, hit.getDOMID(), hit.getSourceID());
        } else {
            // this is not, update earliest time of interest
            IPayload earliest = new DummyPayload(hit.getHitTimeUTC().getOffsetUTCTime(0.1));
            setEarliestPayloadOfInterest(earliest);
        }


    }

    /**
     * Flush the trigger. Basically indicates that there will be no further payloads to process.
     */
    public void flush() {
        // nothing has to be done here since this trigger does not buffer anything.
    }

    public int getHitType() {
        return hitType;
    }

    public void setHitType(int hitType) {
        this.hitType = hitType;
    }

}
