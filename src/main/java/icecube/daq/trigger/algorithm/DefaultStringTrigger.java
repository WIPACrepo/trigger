package icecube.daq.trigger.algorithm;

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: toale
 * Date: Mar 30, 2007
 * Time: 2:19:52 PM
 */
public class DefaultStringTrigger
        extends AbstractTrigger {


    private static final Log log = LogFactory.getLog(DefaultStringTrigger.class);

    private static int triggerNumber = 0;

    public DefaultStringTrigger() {
        triggerNumber++;
    }

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    public boolean isConfigured() {
        return true;
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
        if (parameter.getName().compareTo("triggerPrescale") == 0) {
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

        // set earliest payload of interest to 1/10 ns after the last hit
        // THIS IS DONE HERE SINCE IT USUALLY HAPPENS IN fromTrigger()
        IPayload earliest = new DummyPayload(hit.getHitTimeUTC().getOffsetUTCTime(0.1));
        setEarliestPayloadOfInterest(earliest);

        reportTrigger((ILoadablePayload) hit.deepCopy());

    }

    /**
     * Flush the trigger.
     * Basically indicates that there will be no further payloads to process.
     */
    public void flush() {
        // nothing to flush
    }

}
