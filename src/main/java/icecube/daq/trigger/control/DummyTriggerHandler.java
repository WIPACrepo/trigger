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

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.OutputChannel;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID;
import icecube.daq.trigger.algorithm.ITrigger;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.monitor.TriggerHandlerMonitor;
import icecube.daq.util.DOMRegistry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.TreeMap;
import java.util.TreeSet;

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
    private ITriggerBag triggerBag;

    /**
     * output process
     */
    private DAQComponentOutputProcess payloadOutput;

    /**
     * output channel
     */
    private OutputChannel outChan;

    /**
     * Default output factory
     */
    private TriggerRequestPayloadFactory outputFactory;

    /**
     * SourceId of this TriggerHandler.
     */
    private ISourceID sourceId;

    /**
     * earliest thing of interest to the analysis
     */
    private IPayload earliestPayloadOfInterest;

    private int numHitsPerTrigger = 1000;
    private int count;

    private DOMRegistry domRegistry;

    /**
     * String map
     */
    private TreeMap<Integer, TreeSet<Integer> > stringMap;

    /**
     * Default constructor
     */
    public DummyTriggerHandler() {
        this(new SourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID));
    }

    private DummyTriggerHandler(ISourceID sourceId) {
        this(sourceId, new TriggerRequestPayloadFactory());
    }

    protected DummyTriggerHandler(ISourceID sourceId, TriggerRequestPayloadFactory outputFactory) {
        this.sourceId = sourceId;
        this.outputFactory = outputFactory;
        init();
    }

    private void init() {
        triggerBag = new DummyTriggerBag(sourceId);
        if (outputFactory != null) {
            triggerBag.setPayloadFactory(outputFactory);
        }
        count = 0;

        outChan = null;
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
        log.info("Triggers added to DummyTriggerBag are ignored");
    }

    /**
     * add a list of triggers
     *
     * @param triggers
     */
    public void addTriggers(List<ITrigger> triggers) {
        log.info("Triggers added to DummyTriggerBag are ignored");
    }

    /**
     * clear list of triggers
     */
    public void clearTriggers() {
    }

    public Map<String, Long> getTriggerCounts()
    {
        HashMap<String, Long> map = new HashMap<String, Long>();

        map.put("DummyTrigger", new Long(count));

        return map;
    }

    /**
     * flush the handler
     * including the input buffer, all triggers, and the output bag
     */
    public void flush() {
    }

    /**
     * Is the main thread waiting for input?
     *
     * @return <tt>true</tt> if the main thread is waiting for input
     */
    public boolean isMainThreadWaiting()
    {
        return false;
    }

    /**
     * Is the output thread waiting for data?
     *
     * @return <tt>true</tt> if the main thread is waiting for data
     */
    public boolean isOutputThreadWaiting()
    {
        return false;
    }

    /**
     * sets payload output
     * @param payloadOutput destination of payloads
     */
    public void setPayloadOutput(DAQComponentOutputProcess payloadOutput) {
        this.payloadOutput = payloadOutput;
    }

    /**
     * Get number of triggers queued for output.
     *
     * @return number of triggers queued
     */
    public int getNumOutputsQueued()
    {
        return 0;
    }

    DAQComponentOutputProcess getPayloadOutput()
    {
        return payloadOutput;
    }

    public void setNumHitsPerTrigger(int numHitsPerTrigger)
    {
        this.numHitsPerTrigger = numHitsPerTrigger;
    }

    /**
     * Method to process payloads, assumes that they are time ordered.
     * @param payload payload to process
     */
    public void process(ILoadablePayload payload) {
        IHitPayload hit = (IHitPayload) payload;

        // need to make TRP and add to trigger bag
        if (count % numHitsPerTrigger == 0) {

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
     * Stop the threads
     */
    public void stopThread()
    {
        // do nothing
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
     * getter for count
     * @return count
     */
    public int getCount() {
        return count;
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

        while (triggerBag.hasNext()) {
            ITriggerRequestPayload trigger = (ITriggerRequestPayload) triggerBag.next();

            // write trigger to a ByteBuffer
            ByteBuffer trigBuf =
                ByteBuffer.allocate(trigger.getPayloadLength());
            try {
                ((IWriteablePayload) trigger).writePayload(false, 0, trigBuf);
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
                }
            }

            //--ship the trigger to its destination
            if (trigBuf != null) {
                outChan.receiveByteBuffer(trigBuf);
            }

            // now recycle it
            trigger.recycle();

        }

    }

    public IPayload getEarliestPayloadOfInterest() {
        return earliestPayloadOfInterest;
    }

    private void setEarliestPayloadOfInterest(IPayload earliest) {
        earliestPayloadOfInterest = earliest;
    }

    public void setDOMRegistry(DOMRegistry registry) {
        domRegistry = registry;
        DomSetFactory.setDomRegistry(registry);
    }

    public DOMRegistry getDOMRegistry() {
        return domRegistry;
    }

    public void createStringMap(String stringMapFileName) {


    }

    public TreeMap<Integer, TreeSet<Integer> > getStringMap() {
	return stringMap;
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
        throw new Error("Unimplemented");
        //outCache = cache;
    }

    public void switchToNewRun()
    {
        // does nothing
    }
}
