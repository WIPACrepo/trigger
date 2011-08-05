/*
 * class: GlobalTrigHandler
 *
 * Version $Id: GlobalTrigHandler.java, shseo
 *
 * Date: August 1 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.OutputChannel;
import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.PayloadException;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.trigger.algorithm.ITrigger;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.config.TriggerReadout;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.monitor.PayloadBagMonitor;
import icecube.daq.trigger.monitor.TriggerHandlerMonitor;
import icecube.daq.util.DOMRegistry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class ...does what?
 *
 * @version $Id: GlobalTriggerHandler.java 13231 2011-08-05 22:45:36Z dglo $
 * @author shseo
 */
public class GlobalTriggerHandler
        implements ITriggerHandler
{
    public static final int DEFAULT_MAX_TIMEGATE_WINDOW = 0;
    public static final boolean DEFAULT_TIMEGAP_OPTION = true;

    private static final int PRINTOUT_FREQUENCY = 100000;

    private static final boolean USE_THREAD = true;

    /**
     * Log object for this class
     */
    private static final Log log =
        LogFactory.getLog(GlobalTriggerHandler.class);

    static final DummyPayload FLUSH_PAYLOAD = new DummyPayload(new UTCTime(0));

    /**
     * List of defined triggers
     */
    private List<ITrigger> configuredTriggerList;

    /**
     * Bag of triggers to issue
     */
    private ITriggerBag triggerBag;

    /**
     * The factory used to create triggers to issue
     */
    private TriggerRequestPayloadFactory outputFactory;

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
     * SourceId of the global trigger
     */
    private ISourceID sourceId;

    /**
     * input handler
     */
    private ITriggerInput inputHandler;

    //--assign Default value.
    private int miMaxTimeGateWindow = DEFAULT_MAX_TIMEGATE_WINDOW;
    private boolean allowTimeGap = DEFAULT_TIMEGAP_OPTION;

    private int miTotalInputTriggers;
    private int miTotalNullInputTriggers;
    private int miTotalNonTRPInputTriggers;
    private int miTotalMergedInputTriggers;
    private int miTotalOutputGlobalTriggers;
    private int miTotalOutputMergedGlobalTriggers;
    /** only log payloads which exceed this size */
    private int miMaxNumLogged = 20;

    /**
     * Monitor object.
     */
    private TriggerHandlerMonitor monitor;

    private double longestTrigger;

    private DOMRegistry domRegistry;

    /**
     * String map
     */
    private TreeMap<Integer, TreeSet<Integer> > stringMap;

    /** Outgoing byte buffer cache. */
    private IByteBufferCache outCache;

    /** Main trigger thread */
    private MainThread mainThread;

    /** Splicer adds payloads to this queue, input thread removes them */
    private LinkedList<ILoadablePayload> inputQueue =
        new LinkedList<ILoadablePayload>();

    /** Output thread. */
    private OutputThread outputThread;

    /** Output queue  -- ACCESS MUST BE SYNCHRONIZED. */
    private List<ByteBuffer> outputQueue =
        new LinkedList<ByteBuffer>();

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public GlobalTriggerHandler()
    {
        this(new SourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID));
    }

    public GlobalTriggerHandler(ISourceID sourceId)
    {
        this(sourceId, DEFAULT_TIMEGAP_OPTION);
    }

    public GlobalTriggerHandler(TriggerRequestPayloadFactory outputFactory) {
        this(new SourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID),
             DEFAULT_TIMEGAP_OPTION, outputFactory);
    }

    public GlobalTriggerHandler(ISourceID sourceId, boolean allowTimeGap)
    {
        this(sourceId, allowTimeGap, new TriggerRequestPayloadFactory());
    }

    public GlobalTriggerHandler(ISourceID sourceId, boolean allowTimeGap, TriggerRequestPayloadFactory outputFactory) {
        this.sourceId = sourceId;
        this.outputFactory = outputFactory;

        init();
        ((GlobalTriggerBag) triggerBag).setAllowTimeGap(allowTimeGap);
    }

    public void addToTriggerBag(ILoadablePayload triggerPayload)
    {
        triggerBag.add(triggerPayload);
    }

    public void addTrigger(ITrigger iTrigger) {

        // check for duplicates
        boolean good = true;
        for (ITrigger existing : configuredTriggerList) {
            if ( (iTrigger.getTriggerType() == existing.getTriggerType()) &&
                 (iTrigger.getTriggerConfigId() == existing.getTriggerConfigId()) &&
                 (iTrigger.getSourceId().getSourceID() == existing.getSourceId().getSourceID()) ) {
                log.error("Attempt to add duplicate trigger to trigger list!");
                good = false;
            }
        }

        if (good) {
            log.info("Setting OutputFactory of Trigger");
            iTrigger.setTriggerFactory(outputFactory);
            log.info("Adding Trigger to configuredTriggerList");
            configuredTriggerList.add(iTrigger);
            iTrigger.setTriggerHandler(this);
        }
    }

    /**
     * add a list of triggers
     *
     * @param triggers
     */
    public void addTriggers(List<ITrigger> triggers) {
        clearTriggers();
        configuredTriggerList.addAll(triggers);
    }

    /**
     * clear list of triggers
     */
    public void clearTriggers() {
        configuredTriggerList.clear();
    }

    public Map<String, Long> getTriggerCounts()
    {
        HashMap<String, Long> map = new HashMap<String, Long>();

        for (ITrigger trigger : configuredTriggerList) {
            map.put(trigger.getTriggerName(),
                    new Long(trigger.getTriggerCounter()));
        }

        return map;
    }

    public List<ITrigger> getConfiguredTriggerList() {
        return configuredTriggerList;
    }

    public void finalFlush()
    {
        while (hasUnprocessedPayloads()) {
            Thread.yield();
        }

        flush();
    }

    public void flush()
    {
        // flush the input handler
        if (log.isInfoEnabled()) {
            log.info("Flushing InputHandler in GlobalTrigger: size = " + inputHandler.size());
        }
        inputHandler.flush();

        // than call process with a null payload to suck the life out of the input handler
        process(null);
        while (mainThread != null && !mainThread.sawFlush()) {
            Thread.yield();
        }

        // now flush the triggers, this should prompt them to send any known triggers to the bag
        if (log.isInfoEnabled()) {
            log.info("Flushing GlobalTriggers");
        }

        for (ITrigger trigger : configuredTriggerList) {
            trigger.flush();
            if (log.isInfoEnabled()) {
                log.info("GlobalTrigger count for " + trigger.getTriggerName() +
                         " is " + trigger.getTriggerCounter());
            }
        }

        // finally flush the trigger bag
        if (log.isInfoEnabled()) {
            log.info("Flushing GlobalTriggerBag");
        }
        triggerBag.flush();

        //-- one last call to process to check the bag
        process(null);
        while (mainThread != null && !mainThread.sawFlush()) {
            Thread.yield();
        }

        String nl = System.getProperty("line.separator");
        String hdrLine = "===================================" +
            "===================================";

        StringBuilder buf = new StringBuilder();
        buf.append(hdrLine).append(nl);
        buf.append("I3 GlobalTrigger Run Summary:").append(nl);
        buf.append(nl);
        buf.append("Total # of GT events = " +
                   miTotalOutputGlobalTriggers).append(nl);
        buf.append("Total # of merged GT events = " +
                  miTotalOutputMergedGlobalTriggers).append(nl);
        buf.append(nl);
        for (ITrigger trigger : configuredTriggerList) {
            buf.append("Total # of " + trigger.getTriggerName() + "= " +
                       trigger.getTriggerCounter());
            buf.append(nl);
        }
        buf.append(hdrLine).append(nl);
        log.warn(buf.toString());
    }

    public IPayload getEarliestPayloadOfInterest()
    {
        return earliestPayloadOfInterest;
    }

    protected void init() {
        miTotalInputTriggers = 0;
        miTotalOutputGlobalTriggers = 0;
        miTotalOutputMergedGlobalTriggers = 0;
        earliestPayloadOfInterest = null;
        configuredTriggerList = new ArrayList<ITrigger>();
        longestTrigger = 0.0;

        inputHandler = new TriggerInput();

        //--following two values need to be reset anytime we want to change the values.
        //  Inside JBoss DAQ framework, those values are reset in enterRunning() stage.
        //  perhaps they need to be set in enterReady() stage, just right place to be.
       // ((GlobalTriggerBag) triggerBag).setMaxTimeGateWindow((int) getMaxTimeGateWindow());
       // ((GlobalTriggerBag) triggerBag).setAllowTimeGap(allowetTimeGap());

        triggerBag = new GlobalTriggerBag();
        if (outputFactory != null) {
            triggerBag.setPayloadFactory(outputFactory);
        }

        monitor = new TriggerHandlerMonitor();
        PayloadBagMonitor triggerBagMonitor = new PayloadBagMonitor();
        triggerBag.setMonitor(triggerBagMonitor);
        monitor.setTriggerBagMonitor(triggerBagMonitor);

        setMaxTimeGateWindow((int) getMaxTimeGateWindow());
        setAllowTimeGap(allowTimeGap());

        outChan = null;

        if (inputQueue.size() > 0) {
            if (log.isErrorEnabled()) {
                log.error("Unhandled input payloads queued at init");
            }

            synchronized (inputQueue) {
                inputQueue.clear();
            }
        }

        if (mainThread == null) {
            mainThread = new MainThread("Main-" + sourceId);
            mainThread.start();
        }

        if (outputQueue.size() > 0) {
            if (log.isErrorEnabled()) {
                log.error("Unwritten triggers queued at reset");
            }

            synchronized (outputQueue) {
                outputQueue.clear();
            }
        }

        if (outputThread == null) {
            outputThread = new OutputThread("Output-" + sourceId);
            outputThread.start();
        }
    }

    public boolean hasUnprocessedPayloads()
    {
        return (inputQueue.size() > 0) || !isMainThreadWaiting();
    }

    /**
     * This is the main method.
     *
     * @param payload
     */
    public void process(ILoadablePayload payload) {
        if (!USE_THREAD) {
            if (payload != null) {
                reprocess(payload);
            } else {
                reprocess(FLUSH_PAYLOAD);
            }
        } else {
            synchronized (inputQueue) {
                if (mainThread == null) {
                    log.error("Attempting to queue input without input thread");
                } else if (payload != null) {
                    inputQueue.add(payload);
                } else {
                    mainThread.addedFlush();
                    inputQueue.add(FLUSH_PAYLOAD);
                }
                inputQueue.notify();
            }
        }
    }

    void reprocess(ILoadablePayload payload) {
        miTotalInputTriggers++;
        if (log.isDebugEnabled()) {
            log.debug("Total # of Input Triggers so far = " +
                      miTotalInputTriggers);
        }

        if (miTotalInputTriggers == 1 && log.isDebugEnabled()) {
            log.debug("MaxTimeGateWindow at GlobalTrigHandler = " +
                      miMaxTimeGateWindow);
        }

        //--add payload to input handler which supplements funtionality of the splicer.
        if (payload != FLUSH_PAYLOAD) {
            inputHandler.addPayload(payload);
        }else
        {
            miTotalNullInputTriggers++;
            if (log.isDebugEnabled()) {
                log.debug("Total # of Null Input Triggers so far = " +
                          miTotalNullInputTriggers);
            }
        }

        //--now loop over payloads available from input handler.
        while (inputHandler.hasNext()) {
            IPayload tInputTrigger = inputHandler.next();
            int interfaceType = tInputTrigger.getPayloadInterfaceType();

            //--MergedPayload should be separated first! and then sent to each GT algorithm.
            if(interfaceType == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) {
                if (((ITriggerRequestPayload) tInputTrigger).getTriggerType() == -1) {
                    miTotalMergedInputTriggers++;
                    if (log.isDebugEnabled()) {
                        log.debug("Total # of Merged Input Triggers so far = " + miTotalMergedInputTriggers);
                        log.debug("Now start processing merged trigger input");
                    }
                    boolean failedLoad = false;
                    List subPayloads;
                    try {
                        subPayloads = ((ITriggerRequestPayload) tInputTrigger).getPayloads();
                    } catch (DataFormatException e) {
                        log.error("Couldn't load payload", e);
                        subPayloads = null;
                        failedLoad = true;
                    }
                    if (subPayloads == null) {
                        if (!failedLoad) {
                            ITriggerRequestPayload trigReq =
                                (ITriggerRequestPayload) tInputTrigger;

                            log.error("Bad merged trigger: uid " +
                                      trigReq.getUID() + " configId " +
                                      trigReq.getTriggerConfigID() + " src "+
                                      trigReq.getSourceID() + " times [" +
                                      trigReq.getFirstTimeUTC() + "-" +
                                      trigReq.getLastTimeUTC() + "]");
                        }
                        continue;
                    }
                    //--Each component payload of the MergedPayload needs to be sent to its filter.
                    for(int i=0; i<subPayloads.size(); i++)
                    {
                        ITriggerRequestPayload subPayload = (ITriggerRequestPayload) subPayloads.get(i);
                        try {
                            ((ILoadablePayload) subPayload).loadPayload();
                        } catch (IOException e) {
                            log.error("Couldn't load payload", e);
                        } catch (DataFormatException e) {
                            log.error("Couldn't load payload", e);
                        }

                        //sendPayloadToFilterDestinantion((IPayload) subPayloads.get(i));
                        for (ITrigger configuredTrigger : configuredTriggerList) {
                            try {
                                configuredTrigger.runTrigger(subPayload);
                                int triggerCounter = configuredTrigger.getTriggerCounter();
                                if(log.isDebugEnabled() &&
                                   triggerCounter % PRINTOUT_FREQUENCY == 0 &&
                                   triggerCounter >= PRINTOUT_FREQUENCY)
                                {
                                    log.debug(configuredTrigger.getTriggerName() +
                                              ":  #  " + triggerCounter);
                                }

                            } catch (TriggerException e) {
                                log.error("Exception while running configuredTrigger", e);
                            }
                        }
                    }
                }else{
                    log.debug("Now start processing single trigger input");
                    for (ITrigger configuredTrigger : configuredTriggerList) {
                        try {
                            configuredTrigger.runTrigger(tInputTrigger);
                        } catch (TriggerException e) {
                            log.error("Exception while running configuredTrigger", e);
                        }
                    }
                }
            } else {
                miTotalNonTRPInputTriggers++;
                if (log.isDebugEnabled()) {
                    log.debug("Found non-TriggerRequestPayload #" +
                              miTotalNonTRPInputTriggers + " (ifaceType#" +
                              interfaceType + "=" +
                              tInputTrigger.getClass().getName() + ")");
                }
                return;
            }

            Thread.yield();
        }
        //--Check triggerBag and issue/ship GTEventPayload
        issueTriggers();
    }

    /**
     * Reset the handler for a new run.
     */
    public void reset() {
        log.info("Reseting GlobalTrigHandler");
        init();
    }

    /**
     * getter for SourceID
     * @return sourceId
     */
    public ISourceID getSourceID() {
        return sourceId;
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
     * check triggerBag and issue/ship GTEventPayload if possible
     *   any triggers that are earlier than the earliestPayloadOfInterest are selected
     *   if any of those overlap, they are merged
     */
    public void issueTriggers() {

        if (null == payloadOutput) {
            throw new RuntimeException("PayloadOutput has not been set!");
        }

        if (log.isDebugEnabled()) {
            log.debug("GlobalTrig Bag contains " + triggerBag.size() + " triggers");
        }

        //--update the most earliest time of interest. determined by the slowest trigger.
        // this updates the timeGate in GlobalTrigBag.
        setEarliestTime();

        while (triggerBag.hasNext()) {
            ITriggerRequestPayload GTEventPayload = (ITriggerRequestPayload) triggerBag.next();
            miTotalOutputGlobalTriggers++;
            int GT_trigType = GTEventPayload.getTriggerType();

            if(-1 == GT_trigType)
            {
                miTotalOutputMergedGlobalTriggers++;
            }

            //setAvailableTriggerToRelease();
            // check first and last time of GTEventPayload to issue
            IUTCTime firstTime = GTEventPayload.getFirstTimeUTC();
            IUTCTime lastTime = GTEventPayload.getLastTimeUTC();
            double triggerLength = lastTime.timeDiff_ns(firstTime);
            if (triggerLength > longestTrigger) {
                longestTrigger = triggerLength;
                if (log.isInfoEnabled()) {
                    log.info("We have a new longest GT: " + longestTrigger);
                }
            }

            int nSubPayloads = 0;
            try {
                nSubPayloads = GTEventPayload.getPayloads().size();
            } catch (Exception e) {
                log.error("Couldn't get payloads", e);
            }

            if (log.isInfoEnabled()) {
                if(miTotalOutputGlobalTriggers % PRINTOUT_FREQUENCY == 0){
                    log.info("Issue # " + miTotalOutputGlobalTriggers + " GTEventPayload (trigType = " + GT_trigType + " ) : "
                             + " extended event time = " + firstTime + " to "
                             + lastTime + " and contains " + nSubPayloads + " subTriggers"
                             + ", payload length = " + GTEventPayload.getPayloadLength() + " bytes");
                    if(-1 == GT_trigType){
                        log.info("Merged GT # " + miTotalOutputMergedGlobalTriggers);
                    }
                }
                if(log.isInfoEnabled() && nSubPayloads > miMaxNumLogged){
                    miMaxNumLogged = nSubPayloads;
                    log.info("new max subpayloads " + nSubPayloads +
                             ", payload length = " +
                             GTEventPayload.getPayloadLength() + " bytes");
                }
            }

            int bufLen = GTEventPayload.getPayloadLength();

            ByteBuffer trigBuf;
            if (outCache != null) {
                trigBuf = outCache.acquireBuffer(bufLen);
//System.err.println("Alloc "+trigBuf.capacity()+" bytes from "+outCache);
            } else {
                trigBuf = ByteBuffer.allocate(bufLen);
//System.err.println("GTrig unattached "+trigBuf.capacity()+" bytes");
            }

            // write trigger to a ByteBuffer
            try {
                ((IWriteablePayload) GTEventPayload).writePayload(false, 0,
                                                                  trigBuf);
            } catch (IOException ioe) {
                log.error("Couldn't create payload", ioe);
                trigBuf = null;
            } catch (PayloadException pe) {
                log.error("Couldn't create payload", pe);
                trigBuf = null;
            }

            if (trigBuf != null) {
                synchronized (outputQueue) {
                    outputQueue.add(trigBuf);
                    outputQueue.notify();
                }
            }

            GTEventPayload.recycle();
        }
    }

    /**
     * This method finds the most earliest time among currently running trigger algorithms
     * and sets it in the triggerBag to set timeGate.
     */
    private void setEarliestTime() {
        IUTCTime earliestTimeOverall = new UTCTime(Long.MAX_VALUE);
        IPayload earliestPayloadOverall = null;

        log.debug("SET EARLIEST TIME:");
        //--loop over triggers and find earliest time of interest
        for (ITrigger trigger : configuredTriggerList) {
            IPayload earliestPayload = trigger.getEarliestPayloadOfInterest();

            if (earliestPayload != null) {
                // if payload < earliest
                if (earliestTimeOverall.compareTo(earliestPayload.getPayloadTimeUTC()) > 0) {
                    earliestTimeOverall = earliestPayload.getPayloadTimeUTC();
                    earliestPayloadOverall = earliestPayload;
                    if (log.isDebugEnabled()) {
                        log.debug("There is earliestPayloadOverall: Time = "
                                  + earliestPayloadOverall.getPayloadTimeUTC());
                    }
                } else if (log.isDebugEnabled()) {
                    log.debug("There is earliestPayload: Time = "
                              + earliestPayload.getPayloadTimeUTC());
                }
            }
        }

        if (earliestPayloadOverall != null) {
            earliestPayloadOfInterest = earliestPayloadOverall;

            //--set timeGate in triggerBag
            /*triggerBag.setTimeGate(((IPayload) earliestPayloadOfInterest).getPayloadTimeUTC().
                                            getOffsetUTCTime(-getMaxTimeGateWindow()));*/
            triggerBag.setTimeGate(((IPayload) earliestPayloadOfInterest).getPayloadTimeUTC());

            //--set earliestPayload
            setEarliestPayloadOfInterest(earliestPayloadOfInterest);
        }else if (log.isDebugEnabled())
        {
            log.debug("There is no earliestPayloadOverall.....");
        }

    }

    public double getMaxTimeGateWindow()
    {
        return (double) miMaxTimeGateWindow;
    }

    public void setMaxTimeGateWindow(int iMaxTimeGateWindow)
    {
        miMaxTimeGateWindow = iMaxTimeGateWindow;
        ((GlobalTriggerBag) triggerBag).setMaxTimeGateWindow(miMaxTimeGateWindow);
    }

    public int getMaxReadoutTimeEarliest(List readoutList)
    {
        int maxPastOverall = Integer.MAX_VALUE;

        // loop over triggers
        for (Object obj : readoutList) {
            TriggerReadout readout = (TriggerReadout) obj;

            // check this readout against the overall earliest
            int maxPast = TriggerReadout.getMaxReadoutPast(readout);
            if (maxPast < maxPastOverall) {
                maxPastOverall = maxPast;
            }
        }

        // todo: Agree on the sign of the returned int.
        return maxPastOverall;
    }

    public void setEarliestPayloadOfInterest(IPayload earliestPayload)
    {
        earliestPayloadOfInterest = earliestPayload;
    }

    /**
     * This method sets TimeGap_option from configuration table.
     * todo: Thus this method should be called when GlobalTriggerHandler object is created
     * when GT is configured.
     *
     * @param allowTimeGap <tt>true</tt> to allow time gaps
     */
    public void setAllowTimeGap(boolean allowTimeGap)
    {
        this.allowTimeGap = allowTimeGap;
        ((GlobalTriggerBag) triggerBag).setAllowTimeGap(allowTimeGap);
    }

    public boolean allowTimeGap()
    {
        return allowTimeGap;
    }

    public void setPayloadOutput(DAQComponentOutputProcess payloadOutput)
    {
        this.payloadOutput = payloadOutput;
    }

    public DAQComponentOutputProcess getPayloadOutput()
    {
        return payloadOutput;
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

    public int getCount()
    {
        return miTotalInputTriggers;
    }

    public int getTotalInputTriggers()
    {
        return miTotalInputTriggers;
    }

    public int getTotalNullInputTriggers()
    {
        return miTotalNullInputTriggers;
    }

    public int getTotalNonTRPInputTriggers()
    {
        return miTotalNonTRPInputTriggers;
    }

    public int getTotalMergedInputTriggers()
    {
        return miTotalMergedInputTriggers;
    }

    public int getTotalOutputGlobalTriggers()
    {
        return miTotalOutputGlobalTriggers;
    }

    public int getTotalOutputMergedGlobalTriggers()
    {
        return miTotalOutputMergedGlobalTriggers;
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

    public void stopThread()
    {
        if (mainThread != null) {
            mainThread = null;

            synchronized (inputQueue) {
                inputQueue.notify();
            }
        }

        if (outputThread != null) {
            outputThread = null;

            synchronized (outputQueue) {
                outputQueue.notify();
            }
        }
    }

    /**
     * Is the main thread waiting for input?
     *
     * @return <tt>true</tt> if the main thread is waiting for input
     */
    public boolean isMainThreadWaiting()
    {
        if (mainThread == null) {
            return false;
        }

        return mainThread.isWaiting();
    }

    /**
     * Is the output thread waiting for data?
     *
     * @return <tt>true</tt> if the main thread is waiting for data
     */
    public boolean isOutputThreadWaiting()
    {
        if (outputThread == null) {
            return false;
        }

        return outputThread.isWaiting();
    }

    /**
     * Get number of triggers queued for input.
     *
     * @return number of triggers queued
     */
    public int getNumInputsQueued()
    {
        return inputQueue.size();
    }

    /**
     * Get number of triggers queued for output.
     *
     * @return number of triggers queued
     */
    public int getNumOutputsQueued()
    {
        return outputQueue.size();
    }

    class MainThread
        implements Runnable
    {
        private Thread thread;
        private boolean sawFlush;
        private boolean waiting;

        MainThread(String name)
        {
            thread = new Thread(this);
            thread.setName(name);
        }

        void addedFlush()
        {
            sawFlush = false;
        }

        boolean isWaiting()
        {
            return waiting;
        }

        /**
         * Main payload processing loop.
         */
        public void run()
        {
            // wait for initial assignment to complete
            while (mainThread == null) {
                Thread.yield();
            }

            ILoadablePayload payload;
            while (mainThread != null) {
                synchronized (inputQueue) {
                    if (inputQueue.size() == 0) {
                        try {
                            waiting = true;
                            inputQueue.wait();
                        } catch (InterruptedException ie) {
                            log.error("Interrupt while waiting for input queue",
                                      ie);
                        }
                        waiting = false;
                    }

                    if (inputQueue.size() == 0) {
                        payload = null;
                    } else {
                        payload = inputQueue.remove(0);
                    }
                }

                if (payload != null) {
                    try {
                        reprocess(payload);
                    } catch (Throwable thr) {
                        log.error("Caught exception while processing " +
                                  payload, thr);
                        // should probably break out of the thread after
                        //  some number of consecutive failures
                    }

                    if (payload == FLUSH_PAYLOAD) {
                        sawFlush = true;
                    }
                }
            }
        }

        boolean sawFlush()
        {
            return sawFlush;
        }

        void start()
        {
            thread.start();
        }
    }

    /**
     * Class which writes triggers to output channel.
     */
    class OutputThread
        implements Runnable
    {
        private Thread thread;
        private boolean waiting;

        /**
         * Create and start output thread.
         *
         * @param name thread name
         */
        OutputThread(String name)
        {
            thread = new Thread(this);
            thread.setName(name);
        }

        boolean isWaiting()
        {
            return waiting;
        }

        /**
         * Main output loop.
         */
        public void run()
        {
            ByteBuffer trigBuf;
            while (outputThread != null) {
                synchronized (outputQueue) {
                    if (outputQueue.size() == 0) {
                        try {
                            waiting = true;
                            outputQueue.wait();
                        } catch (InterruptedException ie) {
                            log.error("Interrupt while waiting for output queue",
                                      ie);
                        }
                        waiting = false;
                    }

                    if (outputQueue.size() == 0) {
                        trigBuf = null;
                    } else {
                        trigBuf = outputQueue.remove(0);
                    }
                }

                if (trigBuf == null) {
                    continue;
                }

                // if we haven't already, get the output channel
                if (outChan == null) {
                    if (payloadOutput == null) {
                        log.error("Trigger destination has not been set");
                    } else {
                        outChan = payloadOutput.getChannel();
                        if (outChan == null) {
                            throw new Error("Output channel has not been set" +
                                            " in " + payloadOutput);
                        }
                    }
                }

                //--ship the trigger to its destination
                if (trigBuf != null) {
                    outChan.receiveByteBuffer(trigBuf);
                }
            }
        }

        void start()
        {
            thread.start();
        }
    }
}
