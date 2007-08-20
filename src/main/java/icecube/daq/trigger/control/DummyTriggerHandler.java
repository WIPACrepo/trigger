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

import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.IReadoutRequestElement;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.monitor.TriggerHandlerMonitor;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.*;
import icecube.daq.payload.impl.SourceID4B;

import java.util.List;
import java.util.Vector;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides the analysis framework for the inice trigger.
 *
 * @version $Id: TriggerHandler.java,v 1.19 2006/08/08 20:26:29 vav111 Exp $
 * @author pat
 */
public class DummyTriggerHandler
        implements ITriggerHandler
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(DummyTriggerHandler.class);

    /**
     * Bag of triggers to issue
     */
    protected ITriggerBag triggerBag = null;

    /**
     * output destination
     */
    protected IPayloadDestinationCollection payloadDestination = null;

    /**
     * Default output factory
     */
    protected TriggerRequestPayloadFactory outputFactory = null;

    /**
     * SourceId of this TriggerHandler.
     */
    private ISourceID sourceId;

    /**
     * earliest thing of interest to the analysis
     */
    private IPayload earliestPayloadOfInterest = null;

    private int count;

    /**
     * Default constructor
     */
    public DummyTriggerHandler() {
        this(new SourceID4B(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID));
    }

    public DummyTriggerHandler(ISourceID sourceId) {
        this(sourceId, new TriggerRequestPayloadFactory());
    }

    public DummyTriggerHandler(ISourceID sourceId, TriggerRequestPayloadFactory outputFactory) {
        this.sourceId = sourceId;
        this.outputFactory = outputFactory;
        init();
    }

    private void init() {
        triggerBag = new DummyTriggerBag(sourceId);
        triggerBag.setPayloadFactory(outputFactory);
        count = 0;
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
    }

    /**
     * add a list of triggers
     *
     * @param triggers
     */
    public void addTriggers(List triggers) {
    }

    /**
     * clear list of triggers
     */
    public void clearTriggers() {
    }

    /**
     * flush the handler
     * including the input buffer, all triggers, and the output bag
     */
    public void flush() {
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
        IHitPayload hit = (IHitPayload) payload;

        // need to make TRP and add to trigger bag
        if (count % 1000 == 0) {

            log.info("Creating Trigger...");

            IUTCTime hitTime = hit.getHitTimeUTC();

            // create readout
            Vector readouts = new Vector();
            IUTCTime timeMinus = hitTime.getOffsetUTCTime(-8000);
            IUTCTime timePlus = hitTime.getOffsetUTCTime(8000);
            readouts.add(TriggerRequestPayloadFactory.createReadoutRequestElement(IReadoutRequestElement.READOUT_TYPE_GLOBAL,
                                                                                  timeMinus, timePlus, null, null));
            IReadoutRequest readout = TriggerRequestPayloadFactory.createReadoutRequest(sourceId, count, readouts);

            // create trigger
            ITriggerRequestPayload triggerPayload
                    = (ITriggerRequestPayload) outputFactory.createPayload(count,
                                                                          0,
                                                                          0,
                                                                          sourceId,
                                                                          hitTime,
                                                                          hitTime,
                                                                          new Vector(),
                                                                          readout);
            addToTriggerBag(triggerPayload);

        }
        count++;
        
        setEarliestPayloadOfInterest(payload);

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
        return null;
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
        while (triggerBag.hasNext()) {
            ITriggerRequestPayload trigger = (ITriggerRequestPayload) triggerBag.next();

            // issue the trigger
            if (null == payloadDestination) {
                log.error("PayloadDestination has not been set!");
                throw new RuntimeException("PayloadDestination has not been set!");
            } else {
                try {
		    log.info("Writing Trigger...");
                    payloadDestination.writePayload(trigger);
                } catch (IOException e) {
                    log.error("Failed to write triggers");
                    throw new RuntimeException(e);
                }
                // now recycle it
                trigger.recycle();
            }

        }

    }

    protected IPayload getEarliestPayloadOfInterest() {
        return earliestPayloadOfInterest;
    }

    protected void setEarliestPayloadOfInterest(IPayload earliest) {
        earliestPayloadOfInterest = earliest;
    }

}
