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

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IPayloadOutput;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID4B;
import icecube.daq.payload.impl.UTCTime8B;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.config.ITriggerConfig;
import icecube.daq.trigger.config.TriggerReadout;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.trigger.monitor.ITriggerMonitor;
import icecube.daq.trigger.monitor.PayloadBagMonitor;
import icecube.daq.trigger.monitor.TriggerHandlerMonitor;
import icecube.daq.util.DOMRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class ...does what?
 *
 * @version $Id: GlobalTriggerHandler.java 2629 2008-02-11 05:48:36Z dglo $
 * @author shseo
 */
public class GlobalTriggerHandler
        implements ITriggerHandler
{
    public static final int DEFAULT_MAX_TIMEGATE_WINDOW = 0;
    public static final boolean DEFAULT_TIMEGAP_OPTION = true;

    private static final int PRINTOUT_FREQUENCY = 100000;

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(GlobalTriggerHandler.class);

    /**
     * List of defined triggers
     */
    private List configuredTriggerList;

    /**
     * Bag of triggers to issue
     */
    private ITriggerBag triggerBag;

    /**
     * The factory used to create triggers to issue
     */
    private TriggerRequestPayloadFactory outputFactory;

    /**
     * output destination
     */
    private IPayloadOutput payloadOutput;

    /**
     * earliest thing of interest to the analysis
     */
    private IPayload earliestPayloadOfInterest;

    /**
     * sourceID of the iniceTrigger
     */
    private ISourceID sourceID;

    /**
     * input handler
     */
    private ITriggerInput inputHandler;
    /**
     * This list is used for JUnitTest purpose.
     */
    private List mListAvailableTriggersToRelease;

    //--assign Default value.
    private int miMaxTimeGateWindow = DEFAULT_MAX_TIMEGATE_WINDOW;
    private boolean allowTimeGap = DEFAULT_TIMEGAP_OPTION;

    private int miTotalInputTriggers;
    private int miTotalNullInputTriggers;
    private int miTotalNonTRPInputTriggers;
    private int miTotalMergedInputTriggers;
    private int miTotalOutputGlobalTriggers;
    private int miTotalOutputMergedGlobalTriggers;
    private int miMaxNum = 20;

    /**
     * Monitor object.
     */
    private TriggerHandlerMonitor monitor;

    private double longestTrigger;

    private DOMRegistry domRegistry;

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public GlobalTriggerHandler()
    {
        this(new SourceID4B(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID));
    }

    public GlobalTriggerHandler(ISourceID sourceID)
    {
        this(sourceID, DEFAULT_TIMEGAP_OPTION);
    }

    public GlobalTriggerHandler(TriggerRequestPayloadFactory outputFactory) {
        this(new SourceID4B(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID),
             DEFAULT_TIMEGAP_OPTION, outputFactory);
    }

    public GlobalTriggerHandler(ISourceID sourceID, boolean allowTimeGap)
    {
        this(sourceID, allowTimeGap, new TriggerRequestPayloadFactory());
    }

    public GlobalTriggerHandler(ISourceID sourceID, boolean allowTimeGap, TriggerRequestPayloadFactory outputFactory) {
        this.sourceID = sourceID;
        this.outputFactory = outputFactory;

        this.init();
        ((GlobalTriggerBag) triggerBag).setAllowTimeGap(allowTimeGap);
    }

    public void addToTriggerBag(ILoadablePayload triggerPayload)
    {
        triggerBag.add(triggerPayload);
    }

    public void addTrigger(ITriggerControl iTrigger) {

        // check for duplicates
        boolean good = true;
        ITriggerConfig config = (ITriggerConfig) iTrigger;
        Iterator iter = configuredTriggerList.iterator();
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
    public void addTriggers(List triggers) {
        clearTriggers();
        configuredTriggerList = triggers;
    }

    /**
     * clear list of triggers
     */
    public void clearTriggers() {
        configuredTriggerList.clear();
    }

    public List getConfiguredTriggerList() {
        return configuredTriggerList;
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

        // now flush the triggers, this should prompt them to send any known triggers to the bag
        if (log.isInfoEnabled()) {
            log.info("Flushing GlobalTriggers");
        }

        Iterator triggerIterator;

        triggerIterator = configuredTriggerList.iterator();
        while (triggerIterator.hasNext()) {
            ITriggerControl trigger = (ITriggerControl) triggerIterator.next();
            trigger.flush();
            if (log.isInfoEnabled()) {
                log.info("GlobalTrigger count for " + ((ITriggerConfig) trigger).getTriggerName() + " is "
                         + ((ITriggerMonitor) trigger).getTriggerCounter());
            }
        }

        // finally flush the trigger bag
        if (log.isInfoEnabled()) {
            log.info("Flushing GlobalTriggerBag");
        }
        triggerBag.flush();

        //-- one last call to process to check the bag
        process(null);

        System.out.println("======================================================================");
        System.out.println("I3 GlobalTrigger Run Summary: ");
        System.out.println(" ");
        System.out.println("Total # of GT events = " + miTotalOutputGlobalTriggers);
        System.out.println("Total # of merged GT events = " + miTotalOutputMergedGlobalTriggers);
        System.out.println(" ");
        triggerIterator = configuredTriggerList.iterator();
        while (triggerIterator.hasNext()) {
            ITriggerControl trigger = (ITriggerControl) triggerIterator.next();
            System.out.println("Total # of " + ((ITriggerConfig) trigger).getTriggerName() + "= "
                    + ((ITriggerMonitor) trigger).getTriggerCounter());

        }
        System.out.println("======================================================================");
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
        mListAvailableTriggersToRelease = new ArrayList();
        configuredTriggerList = new ArrayList();
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

        this.setMaxTimeGateWindow((int) getMaxTimeGateWindow());
        this.setAllowTimeGap(allowTimeGap());
    }
    /**
     * This is the main method.
     *
     * @param payload
     */
    public void process(ILoadablePayload payload) {
        miTotalInputTriggers++;
        log.debug("Total # of Input Triggers so far = " + miTotalInputTriggers);

        if(miTotalInputTriggers == 1){
            System.out.println("MaxTimeGateWindow at GlobalTrigHandler = " + miMaxTimeGateWindow);
        }

        //--add payload to input handler which supplements funtionality of the splicer.
        if (null != payload) {
            inputHandler.addPayload(payload);
        }else
        {
            miTotalNullInputTriggers++;
            log.debug("Total # of Null Input Triggers so far = " + miTotalNullInputTriggers);
            //log.fatal("incoming payload is null....");
        }

        //--now loop over payloads available from input handler.
        while (inputHandler.hasNext()) {
            IPayload tInputTrigger = inputHandler.next();
            int interfaceType = tInputTrigger.getPayloadInterfaceType();

            //--MergedPayload should be separated first! and then sent to each GT algorithm.
            if(interfaceType == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) {
                 if (((ITriggerRequestPayload) tInputTrigger).getTriggerType() == -1) {
                     miTotalMergedInputTriggers++;
                     log.debug("Total # of Merged Input Triggers so far = " + miTotalMergedInputTriggers);
                     log.debug("Now start processing merged trigger input");
                    boolean failedLoad = false;
                    Vector vecSubPayloads;
                    try {
                        vecSubPayloads = ((ITriggerRequestPayload) tInputTrigger).getPayloads();
                    } catch (IOException e) {
                        log.error("Couldn't load payload", e);
                        vecSubPayloads = null;
                        failedLoad = true;
                    } catch (DataFormatException e) {
                        log.error("Couldn't load payload", e);
                        vecSubPayloads = null;
                        failedLoad = true;
                    }
                    if (vecSubPayloads == null) {
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
                    for(int i=0; i<vecSubPayloads.size(); i++)
                    {
                        ITriggerRequestPayload subPayload = (ITriggerRequestPayload) vecSubPayloads.get(i);
                        try {
                            ((ILoadablePayload) subPayload).loadPayload();
                        } catch (IOException e) {
                            log.error("Couldn't load payload", e);
                        } catch (DataFormatException e) {
                            log.error("Couldn't load payload", e);
                        }

                        //sendPayloadToFilterDestinantion((IPayload) vecSubPayloads.get(i));
                        Iterator triggerIterator = configuredTriggerList.iterator();
                        while (triggerIterator.hasNext()) {
                            ITriggerControl configuredTrigger = (ITriggerControl) triggerIterator.next();
                            try {
                                configuredTrigger.runTrigger(subPayload);
                                int triggerCounter = ((ITriggerMonitor) configuredTrigger).getTriggerCounter();
                                if(triggerCounter % PRINTOUT_FREQUENCY == 0 && triggerCounter >= PRINTOUT_FREQUENCY){
                                    log.info(((ITriggerConfig) configuredTrigger).
                                            getTriggerName() + ":  #  " + triggerCounter);

                                }

                            } catch (TriggerException e) {
                                log.error("Exception while running configuredTrigger", e);
                            }
                        }
                    }
                }else{
                    log.debug("Now start processing single trigger input");
                    Iterator triggerIterator = configuredTriggerList.iterator();
                    while (triggerIterator.hasNext()) {
                        ITriggerControl configuredTrigger = (ITriggerControl) triggerIterator.next();
                        try {
                            configuredTrigger.runTrigger(tInputTrigger);
                        } catch (TriggerException e) {
                            log.error("Exception while running configuredTrigger", e);
                        }
                    }
                }
            } else {
                miTotalNonTRPInputTriggers++;
                log.debug("Total # of Non-TriggerRequestPayload Input Triggers so far = " + miTotalNonTRPInputTriggers);
                log.info("Input payload was found to be Non-TriggerRequestPayloads: # " + miTotalNonTRPInputTriggers);
                return;
            }
        }
        //--Check triggerBag and issue/ship GTEventPayload
        issueTriggers();
    }

    /**
     * Reset the handler for a new run.
     */
    public void reset() {
        log.info("Reseting GlobalTrigHandler");
        this.init();
    }

    /**
     * getter for SourceID
     * @return sourceID
     */
    public ISourceID getSourceID() {
        return sourceID;
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

            //--add to the list for the first 100 payloads.
            if(mListAvailableTriggersToRelease.size() <= 100)
            {
                mListAvailableTriggersToRelease.add(GTEventPayload);
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
                             + ", payload length = " + GTEventPayload.getPayloadLength() + "bytes");
                    if(-1 == GT_trigType){
                        log.info("Merged GT # " + miTotalOutputMergedGlobalTriggers);
                    }
                }
                if(nSubPayloads > miMaxNum){
                    miMaxNum = nSubPayloads;
                    log.info("payload length = " + GTEventPayload.getPayloadLength() + "bytes");
                    //TriggerTestUtil testUtil = new TriggerTestUtil();
                    //testUtil.show_trigger_Info("Final GT ", miTotalOutputGlobalTriggers, GTEventPayload);
                }
            }

            //--ship the GTEventPayload to its destinantion (i.e., EB).
            try {
                payloadOutput.writePayload(GTEventPayload);
            } catch (IOException e) {
                log.error("Couldn't write payload", e);
            }
            GTEventPayload.recycle();
        }
    }

    /**
     * This method finds the most earliest time among currently running trigger algorithms
     * and sets it in the triggerBag to set timeGate.
     */
    private void setEarliestTime() {
        IUTCTime earliestTimeOverall = new UTCTime8B(Long.MAX_VALUE);
        IPayload earliestPayloadOverall = null;

        log.debug("SET EARLIEST TIME:");
        //--loop over triggers and find earliest time of interest
        Iterator triggerListIterator = configuredTriggerList.iterator();
        while (triggerListIterator.hasNext()) {
            IPayload earliestPayload
                    = (IPayload) ((ITriggerControl) triggerListIterator.next()).getEarliestPayloadOfInterest();

            if (earliestPayload != null) {
                log.debug("There is earliestPayload: Time = " + earliestPayload.getPayloadTimeUTC());
                // if payload < earliest
                if (earliestTimeOverall.compareTo(earliestPayload.getPayloadTimeUTC()) > 0) {
                    earliestTimeOverall = earliestPayload.getPayloadTimeUTC();
                    earliestPayloadOverall = earliestPayload;
                }
                log.debug("There is earliestPayloadOverall: Time = "
                         + earliestPayloadOverall.getPayloadTimeUTC());
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
        }else
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
        Iterator readoutIter = readoutList.iterator();
        while (readoutIter.hasNext()) {
            TriggerReadout readout = (TriggerReadout) readoutIter.next();

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

    public void setPayloadOutput(IPayloadOutput payloadOutput)
    {
        this.payloadOutput = payloadOutput;
    }

    public IPayloadOutput getPayloadOutput()
    {
        return payloadOutput;
    }

    public List getListAvailableTriggerToRelease()
    {
        return mListAvailableTriggersToRelease;
    }

    public void setDOMRegistry(DOMRegistry registry) {
	domRegistry = registry;
    }

    public DOMRegistry getDOMRegistry() {
	return domRegistry;
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

}
