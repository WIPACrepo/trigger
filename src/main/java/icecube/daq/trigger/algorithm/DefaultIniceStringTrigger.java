package icecube.daq.trigger.algorithm;

import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.oldpayload.impl.TriggerRequestPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadException;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Vector;

/**
 *
 *
 *
 *
 * @author toale
 *
 */
public class DefaultIniceStringTrigger extends AbstractTrigger
{

    private static final Log log = LogFactory.getLog(DefaultIniceStringTrigger.class);

    private static int triggerNumber = 0;

    private int stringTriggerType;
    private boolean configStringTriggerType = false;

    public DefaultIniceStringTrigger() {
	triggerNumber++;
    }

    public boolean isConfigured() {
	return configStringTriggerType;
    }

    public void addParameter(TriggerParameter parameter)
	throws UnknownParameterException, IllegalParameterValueException
    {
	if (parameter.getName().compareTo("stringTriggerType") == 0) {
	    stringTriggerType = Integer.parseInt(parameter.getValue());
	    configStringTriggerType = true;
	} else {
	    throw new UnknownParameterException("Unknown parameter: " + parameter.getName());
	}
	super.addParameter(parameter);
    }

    public void setTriggerName(String triggerName) {
	super.triggerName = triggerName + triggerNumber;
    }

    public void runTrigger(IPayload payload)
	throws TriggerException
    {

	// check to see what kind of payload this is
	int interfaceType = payload.getPayloadInterfaceType();
	if (interfaceType != PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) {
	    // set earliest time of interest
	    IUTCTime earliestTime = payload.getPayloadTimeUTC();
	    IPayload earliestPayload = new DummyPayload(earliestTime.getOffsetUTCTime(0.1));
	    setEarliestPayloadOfInterest(earliestPayload);
	}
	ITriggerRequestPayload trigger = (ITriggerRequestPayload) payload;

	// now we probably need to load it
	try {
	    trigger.loadPayload();
	} catch (Exception e) {
	    log.error("Error loading trigger request payload", e);
	    return;
	}

	// next get the source and trigger type
	int source = trigger.getSourceID().getSourceID();
	int type = trigger.getTriggerType();

	// check if source is a stringhub and see if the type matches the configured type
	if ( (SourceIdRegistry.isAnyHubSourceID(source)) &&
	     (type == stringTriggerType) ) {
	    // this is a trigger, need to:
	    //  0) get trigger times
	    //  1) get readout instructions
	    //  2) create new trigger request for inice, containing the string trigger
	    //  3) report it

	    IUTCTime firstTime = trigger.getFirstTimeUTC();
	    IUTCTime lastTime = trigger.getLastTimeUTC();
	    IReadoutRequest readoutRequest = trigger.getReadoutRequest();
	    Vector str = new Vector();
	    str.add(trigger);

	    TriggerRequestPayload newTrigger;
            try {
                newTrigger =
                    (TriggerRequestPayload)
                    triggerFactory.createPayload(triggerCounter, triggerType,
                                                 triggerConfigId, sourceId,
                                                 firstTime, lastTime, str,
                                                 readoutRequest);
            } catch (PayloadException pe) {
                log.error("Cannot create trigger", pe);
                newTrigger = null;
            }

	    if (newTrigger != null) reportTrigger(newTrigger);

	} else {
	    // set earliest time of interest
	    IUTCTime earliestTime = trigger.getPayloadTimeUTC();
	    IPayload earliestPayload = new DummyPayload(earliestTime.getOffsetUTCTime(0.1));
	    setEarliestPayloadOfInterest(earliestPayload);
	}
    }

    public void flush() {
	// nothing to flush
    }
}

