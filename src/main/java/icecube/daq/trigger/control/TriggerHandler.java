/*
 * class: TriggerHandler
 *
 * Version $Id: TriggerHandler.java 4892 2010-02-16 21:26:15Z dglo $
 *
 * Date: October 25 2004
 *
 * (c) 2004 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.OutputChannel;
import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.oldpayload.impl.Payload;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.trigger.algorithm.ITrigger;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.monitor.PayloadBagMonitor;
import icecube.daq.trigger.monitor.TriggerHandlerMonitor;
import icecube.daq.util.DOMRegistry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides the analysis framework for the inice trigger.
 *
 * @version $Id: TriggerHandler.java 4892 2010-02-16 21:26:15Z dglo $
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
    private List<ITrigger> triggerList;

    /**
     * Bag of triggers to issue
     */
    private ITriggerBag triggerBag;

    /**
     * counts the number of processed primitives
     */
    private int count;

    /**
     * output process
     */
    private DAQComponentOutputProcess payloadOutput;

    /**
     * output channel
     */
    private OutputChannel outChan;

    /**
     * earliest thing of interest to the analysis
     */
    private IPayload earliestPayloadOfInterest;

    /**
     * time of last hit, used for monitoring
     */
    private IUTCTime timeOfLastHit;

    /**
     * input handler
     */
    private ITriggerInput inputHandler;

    /**
     * Default output factory
     */
    private TriggerRequestPayloadFactory outputFactory;

    /**
     * SourceId of this TriggerHandler.
     */
    private ISourceID sourceId;

    /**
     * Monitor object.
     */
    private TriggerHandlerMonitor monitor;

    /**
     * DOMRegistry
     */
    private DOMRegistry domRegistry;

    /** Outgoing byte buffer cache. */
    private IByteBufferCache outCache;

    /**
     * Default constructor
     */
    public TriggerHandler() {
        this(new SourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID));
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
        triggerList = new ArrayList<ITrigger>();

        triggerBag = createTriggerBag();

        monitor = new TriggerHandlerMonitor();
        PayloadBagMonitor triggerBagMonitor = new PayloadBagMonitor();
        triggerBag.setMonitor(triggerBagMonitor);
        monitor.setTriggerBagMonitor(triggerBagMonitor);
    }

    protected ITriggerBag createTriggerBag()
    {
        ITriggerBag bag = new TriggerBag(sourceId);
        if (outputFactory != null) {
            bag.setPayloadFactory(outputFactory);
        }
        return bag;
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
    public void addTrigger(ITrigger trigger) {

        // check for duplicates
        boolean good = true;
        for (ITrigger existing : triggerList) {
            if ( (trigger.getTriggerType() == existing.getTriggerType()) &&
                 (trigger.getTriggerConfigId() == existing.getTriggerConfigId()) &&
                 (trigger.getSourceId().getSourceID() == existing.getSourceId().getSourceID()) ) {
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
    public void addTriggers(List<ITrigger> triggers) {
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
        for (ITrigger trigger : triggerList) {
            trigger.flush();
            if (log.isInfoEnabled()) {
                log.info("Trigger count for " + trigger.getTriggerName() +
                         " is " + trigger.getTriggerCounter());
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
     * sets payload output
     * @param payloadOutput destination of payloads
     */
    public void setPayloadOutput(DAQComponentOutputProcess payloadOutput) {
        this.payloadOutput = payloadOutput;
    }

    public DAQComponentOutputProcess getPayloadOutput()
    {
        return payloadOutput;
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
                for (ITrigger trigger : triggerList) {
                    try {
                        trigger.runTrigger(hit);
                    } catch (TriggerException e) {
                        log.error("Exception while running trigger", e);
                    }
                }

            } else if(interfaceType == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD){
                try {
                    ((ILoadablePayload) nextPayload).loadPayload();
                } catch (IOException e) {
                    log.error("Couldn't load payload", e);
                } catch (DataFormatException e) {
                    log.error("Couldn't load payload", e);
                }
                ITriggerRequestPayload tPayload = (ITriggerRequestPayload) nextPayload;
                int srcId;
                if (tPayload.getSourceID() != null) {
                    srcId = tPayload.getSourceID().getSourceID();
                } else {
                    if (tPayload.getPayloadLength() == 0 &&
                        tPayload.getPayloadTimeUTC() == null &&
                        ((IPayload) tPayload).getPayloadBacking() == null)
                    {
                        log.error("Ignoring recycled payload");
                    } else {
                        log.error("Unexpected null SourceID in payload (len=" +
                                  tPayload.getPayloadLength() + ", time=" +
                                  (tPayload.getPayloadTimeUTC() == null ?
                                   "null" : "" + tPayload.getPayloadTimeUTC()) +
                                   ", buf=" +
                                  ((IPayload) tPayload).getPayloadBacking() +
                                  ")");
                    }

                    srcId = -1;
                }
                if(srcId == SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID
                    || srcId == SourceIdRegistry.STRINGPROCESSOR_SOURCE_ID){
                    count++;

                    // loop over triggers
                    for (ITrigger trigger : triggerList) {
                        try {
                            trigger.runTrigger(tPayload);
                        } catch (TriggerException e) {
                            log.error("Exception while running trigger", e);
                        }
                    }
                }else if (srcId != -1) {

                    log.error("SourceID " + srcId + " should not send TriggerRequestPayloads!");
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
     * Get the input handler
     *
     * @return a trigger input handler
     */
    public ITriggerInput getInputHandler()
    {
        return inputHandler;
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
    public List<ITrigger> getTriggerList() {
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
        if (null == payloadOutput) {
            throw new RuntimeException("PayloadOutput has not been set!");
        }


        if (log.isDebugEnabled()) {
            log.debug("Trigger Bag contains " + triggerBag.size() +
                      " triggers");
        }

        // update earliest time of interest
        setEarliestTime();

        while (triggerBag.hasNext()) {
            IWriteablePayload payload = (IWriteablePayload) triggerBag.next();

            if (payload == null) {
                log.error("TriggerBag returned null next payload");
                break;
            }

            if (payload.getPayloadInterfaceType() == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) {
                if (log.isDebugEnabled()) {
                    ITriggerRequestPayload trigger = (ITriggerRequestPayload) payload;

                    IUTCTime firstTime = trigger.getFirstTimeUTC();
                    IUTCTime lastTime = trigger.getLastTimeUTC();

                    int nSubPayloads = 0;
                    try {
                        nSubPayloads = trigger.getPayloads().size();
                    } catch (Exception e) {
                        log.error("Couldn't get number of subpayloads", e);
                    }

                    if (log.isDebugEnabled()) {
                        String trType;
                        if (0 > trigger.getTriggerType()) {
                            trType = "triggers";
                        } else {
                            trType = "hits";
                        }

                        log.debug("Issue trigger: extended event time = " +
                                  firstTime + " to " + lastTime +
                                  " and contains " + nSubPayloads + " " +
                                  trType);
                    }
                }
            }

            int bufLen = payload.getPayloadLength();

            // allocate ByteBuffer
            ByteBuffer trigBuf;
            if (outCache != null) {
                trigBuf = outCache.acquireBuffer(bufLen);
//System.err.println("Alloc "+trigBuf.capacity()+" bytes from "+outCache);
            } else {
                trigBuf = ByteBuffer.allocate(bufLen);
System.err.println("Unattached "+SourceIdRegistry.getDAQNameFromISourceID(sourceId)+" "+trigBuf.capacity()+" bytes");
            }

            // write trigger to a ByteBuffer
            try {
                ((IWriteablePayload) payload).writePayload(false, 0, trigBuf);
            } catch (IOException ioe) {
                log.error("Couldn't create payload", ioe);
                trigBuf = null;
            }

            // if we haven't already, get the output channel
            if (outChan == null) {
                if (payloadOutput == null) {
                    log.error("Trigger destination has not been set");
                } else {
                    outChan = payloadOutput.getChannel();
                    if (outChan == null) {
                        throw new Error("Output channel has not been set in " +
                                        payloadOutput);
                    }
                }
            }

            //--ship the trigger to its destination
            if (trigBuf != null) {
                outChan.receiveByteBuffer(trigBuf);
            }

            // now recycle it
            payload.recycle();

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
    private void setEarliestTime() {

        IUTCTime earliestTimeOverall = new UTCTime(Long.MAX_VALUE);
        IPayload earliestPayloadOverall = null;

        // loop over triggers and find earliest time of interest
        for (ITrigger trigger : triggerList) {
            IPayload earliestPayload = trigger.getEarliestPayloadOfInterest();
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

    public void setDOMRegistry(DOMRegistry registry) {
	domRegistry = registry;
        DomSetFactory.setDomRegistry(registry);
    }

    public DOMRegistry getDOMRegistry() {
	return domRegistry;
    }

    public void setOutputFactory(TriggerRequestPayloadFactory factory)
    {
        outputFactory = factory;
        if (outputFactory != null) {
            triggerBag.setPayloadFactory(outputFactory);
        }
    }

    /**
     * Set the outgoing payload buffer cache.
     * @param byte buffer cache manager
     */
    public void setOutgoingBufferCache(IByteBufferCache cache)
    {
        outCache = cache;
    }
}
