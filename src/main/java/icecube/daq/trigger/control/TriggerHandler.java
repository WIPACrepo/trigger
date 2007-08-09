/*
 * class: TriggerHandler
 *
 * Version $Id: TriggerHandler.java,v 1.19 2006/08/08 20:26:29 vav111 Exp $
 *
 * Date: October 25 2004
 *
 * (c) 2004 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.config.ITriggerConfig;
import icecube.daq.trigger.monitor.ITriggerMonitor;
import icecube.daq.trigger.monitor.PayloadBagMonitor;
import icecube.daq.trigger.monitor.TriggerHandlerMonitor;
import icecube.daq.trigger.impl.TriggerRequestPayload;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.*;
import icecube.daq.payload.splicer.Payload;
import icecube.daq.payload.impl.UTCTime8B;
import icecube.daq.payload.impl.SourceID4B;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides the analysis framework for the inice trigger.
 *
 * @version $Id: TriggerHandler.java,v 1.19 2006/08/08 20:26:29 vav111 Exp $
 * @author pat
 */
public class TriggerHandler
        implements ITriggerHandler
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(TriggerHandler.class);

    /**
     * List of defined triggers
     */
    protected List triggerList = null;

    /**
     * Bag of triggers to issue
     */
    protected ITriggerBag triggerBag = null;

    /**
     * counts the number of processed primitives
     */
    private int count = 0;

    /**
     * output destination
     */
    protected IPayloadDestinationCollection payloadDestination = null;

    /**
     * earliest thing of interest to the analysis
     */
    private IPayload earliestPayloadOfInterest = null;

    /**
     * time of last hit, used for monitoring
     */
    private IUTCTime timeOfLastHit = null;

    /**
     * input handler
     */
    private ITriggerInput inputHandler = null;

    /**
     * Default output factory
     */
    protected TriggerRequestPayloadFactory outputFactory = null;

    /**
     * SourceId of this TriggerHandler.
     */
    private ISourceID sourceId;

    /**
     * Monitor object.
     */
    protected TriggerHandlerMonitor monitor;

    /**
     * Default constructor
     */
    public TriggerHandler() {
        this(new SourceID4B(4000));
    }

    public TriggerHandler(ISourceID sourceId) {
        this(sourceId, new TriggerRequestPayloadFactory());
    }

    public TriggerHandler(ISourceID sourceId, TriggerRequestPayloadFactory outputFactory) {
        this.sourceId = sourceId;
        this.outputFactory = outputFactory;
        init();
    }

    protected void init() {

        count = 0;
        earliestPayloadOfInterest = null;
        timeOfLastHit = null;
        inputHandler = new TriggerInput();
        triggerList = new ArrayList();
        triggerBag = new TriggerBag(sourceId);
        triggerBag.setPayloadFactory(outputFactory);

        monitor = new TriggerHandlerMonitor();
        PayloadBagMonitor triggerBagMonitor = new PayloadBagMonitor();
        triggerBag.setMonitor(triggerBagMonitor);
        monitor.setTriggerBagMonitor(triggerBagMonitor);

    }

    /**
     * add a new trigger payload to the bag
     * @param triggerPayload new trigger to add
     */
    public void addToTriggerBag(ILoadablePayload triggerPayload) {
        triggerBag.add(triggerPayload);
    }

    /**
     * method for adding triggers to the trigger list
     * @param trigger trigger to be added
     */
    public void addTrigger(ITriggerControl trigger) {

        // check for duplicates
        boolean good = true;
        ITriggerConfig config = (ITriggerConfig) trigger;
        Iterator iter = triggerList.iterator();
        while (iter.hasNext()) {
            ITriggerConfig existing = (ITriggerConfig) iter.next();
            if ( (config.getTriggerType() == existing.getTriggerType()) &&
                 (config.getTriggerConfigId() == existing.getTriggerConfigId()) &&
                 (config.getSourceId().getSourceID() == existing.getSourceId().getSourceID()) ) {
                log.error("Attempt to add duplicate trigger to trigger list!");
                good = false;
            }
        }

        if (good) {
            log.info("Setting OutputFactory of Trigger");
            trigger.setTriggerFactory(outputFactory);
            log.info("Adding Trigger to TriggerList");
            triggerList.add(trigger);
            trigger.setTriggerHandler(this);
        }
    }

    /**
     * add a list of triggers
     *
     * @param triggers
     */
    public void addTriggers(List triggers) {
        clearTriggers();
        triggerList.addAll(triggers);
    }

    /**
     * clear list of triggers
     */
    public void clearTriggers() {
        triggerList.clear();
    }

    /**
     * flush the handler
     * including the input buffer, all triggers, and the output bag
     */
    public void flush() {

        // flush the input handler
        if (log.isInfoEnabled()) {
            log.info("Flushing InputHandler: size = " + inputHandler.size());
        }
        inputHandler.flush();

        // then call process with a null payload to suck the life out of the input handler
        process(null);

        // now flush the triggers, this should prompt them to send any known triggers to the bag
        if (log.isInfoEnabled()) {
            log.info("Flushing Triggers");
        }
        Iterator triggerIterator = triggerList.iterator();
        while (triggerIterator.hasNext()) {
            ITriggerControl trigger = (ITriggerControl) triggerIterator.next();
            trigger.flush();
            if (log.isInfoEnabled()) {
                log.info("Trigger count for " + ((ITriggerConfig) trigger).getTriggerName() + " is "
                         + ((ITriggerMonitor) trigger).getTriggerCounter());
            }
        }

        // finally flush the trigger bag
        if (log.isInfoEnabled()) {
            log.info("Flushing TriggerBag");
        }
        triggerBag.flush();

        // one last call to process to check the bag
        process(null);

    }

    /**
     * sets payload destination
     * @param payloadDestination destination of payloads
     */
    public void setPayloadDestinationCollection(IPayloadDestinationCollection payloadDestination) {
        this.payloadDestination = payloadDestination;
    }

    /**
     * Method to process payloads, assumes that they are time ordered.
     * @param payload payload to process
     */
    public void process(ILoadablePayload payload) {

        // add payload to input handler
        if (null != payload) {
            inputHandler.addPayload(payload);
        }

        // now loop over payloads available from input handler
        while (inputHandler.hasNext()) {
            IPayload nextPayload = inputHandler.next();

            int interfaceType = nextPayload.getPayloadInterfaceType();

            // make sure we have hit payloads (or hit data payloads)
            if ((interfaceType == PayloadInterfaceRegistry.I_HIT_PAYLOAD) ||
                (interfaceType == PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD)) {

                IHitPayload hit = (IHitPayload) nextPayload;
                if (hit.getHitTimeUTC() == null) {
                    Payload pay = (Payload) hit;
                    log.error("Bad hit buf " + pay.getPayloadBacking() +
                              " off " + pay.getPayloadOffset() + " len " +
                              pay.getPayloadLength() + " type " +
                              pay.getPayloadType() + " utc " +
                              pay.getPayloadTimeUTC());
                    continue;
                }

                // Calculate time since last hit
                double timeDiff;
                if (timeOfLastHit == null) {
                    timeDiff = 0.0;
                } else {
                    timeDiff = hit.getHitTimeUTC().timeDiff_ns(timeOfLastHit);
                }

                // check to see if timeDiff is reasonable, if not ignore it
                if (timeDiff < 0.0) {
                    log.error("Hit out of order! This time - Last time = " + timeDiff);
                    return;
                } else {
                    timeOfLastHit = hit.getHitTimeUTC();
                    count++;
                }

                // loop over triggers
                Iterator triggerIterator = triggerList.iterator();
                while (triggerIterator.hasNext()) {
                    ITriggerControl trigger = (ITriggerControl) triggerIterator.next();
                    try {
                        trigger.runTrigger(hit);
                    } catch (TriggerException e) {
                        log.error("Exception while running trigger: " + e);
                    }
                }

            } else if(interfaceType == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD){
                try {
                    ((ILoadablePayload) nextPayload).loadPayload();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (DataFormatException e) {
                    e.printStackTrace();
                }
                ITriggerRequestPayload tPayload = (ITriggerRequestPayload) nextPayload;
                int sourceId;
                if (tPayload.getSourceID() != null) {
                    sourceId = tPayload.getSourceID().getSourceID();
                } else {
                    if (tPayload.getPayloadLength() == 0 &&
                        tPayload.getPayloadTimeUTC() == null &&
                        ((Payload) tPayload).getPayloadBacking() == null)
                    {
                        log.error("Ignoring recycled payload");
                    } else {
                        log.error("Unexpected null SourceID in payload (len=" +
                                  tPayload.getPayloadLength() + ", time=" +
                                  (tPayload.getPayloadTimeUTC() == null ?
                                   "null" : "" + tPayload.getPayloadTimeUTC().getUTCTimeAsLong()) +
                                   ", buf=" + ((Payload) tPayload).getPayloadBacking());
                    }

                    sourceId = -1;
                }
                if(sourceId == SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID
                    || sourceId == SourceIdRegistry.STRINGPROCESSOR_SOURCE_ID){
                    // loop over triggers
                    Iterator triggerIterator = triggerList.iterator();
                    while (triggerIterator.hasNext()) {
                        ITriggerControl trigger = (ITriggerControl) triggerIterator.next();
                        try {
                            trigger.runTrigger(tPayload);
                        } catch (TriggerException e) {
                            log.error("Exception while running trigger: " + e);
                        }
                    }
                }else if (sourceId != -1) {

                    log.error("SourceID " + sourceId + " should not send TriggerRequestPayloads!");
                }
            }else{
                    log.warn("TriggerHandler only knows about either hitPayloads or TriggerRequestPayloads!");
            }

        }

        // Check triggerBag and issue triggers
        issueTriggers();

    }

    /**
     * Reset the handler for a new run.
     */
    public void reset() {
        init();
    }

    /**
     * Get the monitor object.
     *
     * @return a TriggerHandlerMonitor
     */
    public TriggerHandlerMonitor getMonitor() {
        return monitor;
    }

    /**
     * getter for count
     * @return count
     */
    public int getCount() {
        return count;
    }

    /**
     * getter for triggerList
     * @return trigger list
     */
    public List getTriggerList() {
        return triggerList;
    }

    /**
     * getter for SourceID
     * @return sourceID
     */
    public ISourceID getSourceID() {
        return sourceId;
    }

    /**
     * check triggerBag and issue triggers if possible
     *   any triggers that are earlier than the earliestPayloadOfInterest are selected
     *   if any of those overlap, they are merged
     */
    public void issueTriggers() {

        if (log.isDebugEnabled()) {
            log.debug("Trigger Bag contains " + triggerBag.size() + " triggers");
        }

        // update earliest time of interest
        setEarliestTime();

        while (triggerBag.hasNext()) {
            IWriteablePayload payload = (IWriteablePayload) triggerBag.next();

            if (payload.getPayloadInterfaceType() == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) {
                ITriggerRequestPayload trigger = (ITriggerRequestPayload) payload;
                if (log.isDebugEnabled()) {
                    IUTCTime firstTime = trigger.getFirstTimeUTC();
                    IUTCTime lastTime = trigger.getLastTimeUTC();

                    int nSubPayloads = 0;
                    try {
                        nSubPayloads = trigger.getPayloads().size();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (log.isDebugEnabled()) {
                        if (0 > trigger.getTriggerType()) {
                            log.debug("Issue trigger: extended event time = " + firstTime.getUTCTimeAsLong() + " to "
                                      + lastTime.getUTCTimeAsLong() + " and contains " + nSubPayloads + " triggers");
                        } else {
                            log.debug("Issue trigger: extended event time = " + firstTime.getUTCTimeAsLong() + " to "
                                      + lastTime.getUTCTimeAsLong() + " and contains " + nSubPayloads + " hits");
                        }
                    }
                }
            }

            // issue the trigger
            if (null == payloadDestination) {
                log.error("PayloadDestination has not been set!");
                throw new RuntimeException("PayloadDestination has not been set!");
            } else {
                try {
                    payloadDestination.writePayload(payload);
                } catch (IOException e) {
                    log.error("Failed to write triggers");
                    throw new RuntimeException(e);
                }
                // now recycle it
                payload.recycle();
            }

        }

    }

    protected IPayload getEarliestPayloadOfInterest() {
        return earliestPayloadOfInterest;
    }

    protected void setEarliestPayloadOfInterest(IPayload earliest) {
        earliestPayloadOfInterest = earliest;
    }

    /**
     * sets the earliest overall time of interest by inspecting each trigger
     */
    protected void setEarliestTime() {

        IUTCTime earliestTimeOverall = new UTCTime8B(Long.MAX_VALUE);
        IPayload earliestPayloadOverall = null;

        // loop over triggers and find earliest time of interest
        Iterator triggerListIterator = triggerList.iterator();
        while (triggerListIterator.hasNext()) {
            IPayload earliestPayload
                    = ((ITriggerControl) triggerListIterator.next()).getEarliestPayloadOfInterest();
            if (earliestPayload != null) {
                // if payload < earliest
                if (earliestTimeOverall.compareTo(earliestPayload.getPayloadTimeUTC()) > 0) {
                    earliestTimeOverall = earliestPayload.getPayloadTimeUTC();
                    earliestPayloadOverall = earliestPayload;
                }
            }
        }

        if (earliestPayloadOverall != null) {
            earliestPayloadOfInterest = earliestPayloadOverall;

            // set timeGate in triggerBag
            triggerBag.setTimeGate(earliestPayloadOfInterest.getPayloadTimeUTC());
            // set earliestPayload
            setEarliestPayloadOfInterest(earliestPayloadOfInterest);
        }

    }

}
