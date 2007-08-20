/*
 * class: RegularTriggers
 *
 * Version $Id: RegularTriggers.java, shseo
 *
 * Date: August 2 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.payload.IPayload;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.config.TriggerParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * This class receives unconditional-triggers,
 *  makes a new TriggerRequestPayload for each input TriggerRequestPayload
 *  and then pass them to GlobalTrigBag.java.
 *
 * @version $Id: ThroughputTrigger.java,v 1.21 2006/01/31 14:01:43 toale Exp $
 * @author shseo
 */
public class ThroughputTrigger
        extends AbstractGlobalTrigger
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(ThroughputTrigger.class);

    //private GlobalTrigEventWrapper mtGlobalTrigEventWrapper;
   // private IPayload mtGlobalTrigEventPayload;
    private List mListSelectedPayload = new ArrayList();
    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public ThroughputTrigger()
    {
        super();
    }
    /**
     * This is the main method and called in GlobalTrigHandler.java.
     * Since this is ThroughputTrigger, this method do nothing but wrapping
     * an inputTrigger to GlobalTrigEvent (not the final GlobalTrigEvent yet.....).
     *
     * @param payload
     * @throws TriggerException
     */
    public void runTrigger(IPayload payload) throws TriggerException
    {
        log.debug("inside runTrigger in ThroughputTrigger");
        //DummyPayload dummy = new DummyPayload(mtGlobalTrigEventPayload.getFirstTimeUTC());
        //setEarliestPayloadOfInterest(dummy);
        try {
            wrapTrigger((ITriggerRequestPayload) payload);
        } catch (Exception e) {
            log.error("Couldn't wrap trigger", e);
        }

    }

    /**
      * method to flush the trigger
      * basically indicates that there will be no further payloads to process
      */
     public void flush() {
        //--nothing needs to be done in this ThroughputTrigger algorithm!
     }

    public boolean isConfigured() {
        return true;
    }

    public void addParameter(TriggerParameter parameter) throws UnknownParameterException
    {
        throw new UnknownParameterException("This trigger needs no parameter");
    }
}
