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

import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.PayloadException;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements a simple minimum bias trigger that only cares about
 * the AMANDA trig mainboard. It simply counts hits and
 * applies a prescale for determining when a trigger should be formed.
 *
 * @version $Id: MinBiasTrigger.java,v 1.20 2006/09/14 20:35:13 toale Exp $
 * @author pat
 */
public class TrigBoardTrigger extends AbstractTrigger
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(TrigBoardTrigger.class);

    private static int triggerNumber = 0;

    private int prescale;
    private int numberProcessed = 0;

    private boolean configPrescale = false;

    public TrigBoardTrigger() {
        triggerNumber++;
        configHitFilter(1);
    }

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    public boolean isConfigured() {
        return configPrescale;
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
        } else if (parameter.getName().compareTo("triggerPrescale") == 0) {
            triggerPrescale = Integer.parseInt(parameter.getValue());
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

        numberProcessed++;
        if (numberProcessed % prescale == 0) {
            // report this as a trigger
            try {
                formTrigger(hit, null, null);
            } catch (PayloadException pe) {
                throw new TriggerException("Cannot form trigger", pe);
            }
        } else {
            // just update earliest time of interest
            IPayload earliest = new DummyPayload(hit.getHitTimeUTC().getOffsetUTCTime(0.1));
            setEarliestPayloadOfInterest(earliest);
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

}
