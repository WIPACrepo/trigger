package icecube.daq.trigger.algorithm;

import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadException;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.TriggerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: pat
 * Date: Dec 11, 2006
 * Time: 1:09:26 PM
 */
public abstract class AmandaTrigger
    extends AbstractTrigger
{

    private static final Log log = LogFactory.getLog(AmandaTrigger.class);

    public static final int MULT_FRAG_20 = 0x001;
    public static final int VOLUME       = 0x002;
    public static final int M18          = 0x004;
    public static final int M24          = 0x008;
    public static final int STRING       = 0x010;
    public static final int SPASE        = 0x020;
    public static final int RANDOM       = 0x040;
    public static final int CALIB_T0     = 0x080;
    public static final int CALIB_LASER  = 0x100;

    protected int triggerBit = 0x000;

    /**
     * Run the trigger algorithm on a payload.
     *
     * @param payload payload to process
     *
     * @throws icecube.daq.trigger.exceptions.TriggerException
     *     if the algorithm doesn't like this payload
     */
    public void runTrigger(IPayload payload) throws TriggerException 
    {

        int interfaceType = payload.getPayloadInterfaceType();
        if (interfaceType != 
            PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) 
        {
            throw new TriggerException("Expecting an ITriggerRequestPayload");
        }
        ITriggerRequestPayload trigger = (ITriggerRequestPayload) payload;
        IUTCTime triggerTime = trigger.getFirstTimeUTC();

        // check the trigger bit
        int bitmask = trigger.getTriggerConfigID();
        if (log.isDebugEnabled()) {
            log.debug("Received TWR trigger mask: " + bitmask);
        }
        if ((bitmask & triggerBit) == triggerBit) {
            // this is the correct type, report trigger
            try {
                formTrigger(triggerTime);
            } catch (PayloadException pe) {
                throw new TriggerException("Cannot form trigger", pe);
            }
        } else {
            // this is not, update earliest time of interest
            IPayload earliest = new DummyPayload(triggerTime.
                getOffsetUTCTime(0.1));
            setEarliestPayloadOfInterest(earliest);
        }


    }

    /**
     * Flush the trigger. Basically indicates that there will be no further 
     * payloads to process.
     */
    public void flush() 
    {
        // nothing has to be done here since this trigger
        // does not buffer anything.
    }

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    public boolean isConfigured() 
    {
        return true;
    }

    public int getTriggerBit() 
    {
        return triggerBit;
    }

    public void setTriggerBit(int triggerBit) 
    {
        this.triggerBit = triggerBit;
    }

}
