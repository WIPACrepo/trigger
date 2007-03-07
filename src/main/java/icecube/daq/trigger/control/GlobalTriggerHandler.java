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

import icecube.daq.payload.*;
import icecube.daq.payload.splicer.Payload;
import icecube.daq.payload.impl.UTCTime8B;
import icecube.daq.payload.impl.SourceID4B;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.trigger.impl.TriggerRequestPayload;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.monitor.ITriggerMonitor;
import icecube.daq.trigger.monitor.TriggerHandlerMonitor;
import icecube.daq.trigger.monitor.PayloadBagMonitor;
import icecube.daq.trigger.config.ITriggerConfig;
import icecube.daq.trigger.config.TriggerReadout;
import icecube.daq.trigger.exceptions.TriggerException;
//import icecube.daq.globalTrig.util.TriggerTestUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Vector;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.io.IOException;

/**
 * This class ...does what?
 *
 * @version $Id: GlobalTrigHandler.java,v 1.44 2006/08/08 20:01:24 vav111 Exp $
 * @author shseo
 */
public class GlobalTriggerHandler
        implements ITriggerHandler
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(GlobalTriggerHandler.class);

    /**
     * interval at which messages are printed
     */
    private static final int messageInterval = 1000;

    /**
     * maximum allowable time difference between hits (in ns)
     */
    private static final double MAX_TIME_DIFF = 30*1e9;

    /**
     * List of defined triggers
     */
    private List configuredTriggerList = null;

    /**
     * Bag of triggers to issue
     */
    protected ITriggerBag triggerBag = null;

    /**
     * The factory used to create triggers to issue
     */
    protected TriggerRequestPayloadFactory outputFactory;

    /**
     * counts the number of processed primitives
     */
    private int count;

    protected IPayloadDestinationCollection payloadDestination = null;
    /**
     * output destination
     */
    // private PayloadDestination payloadDestination;

    /**
     * earliest thing of interest to the analysis
     */
    private IPayload earliestPayloadOfInterest;

    /**
     * sourceID of the iniceTrigger
     */
    protected ISourceID sourceID;

    /**
     * time of last hit, used for monitoring
     */
    private IUTCTime timeOfLastHit = null;

    /**
     * input handler
     */
    protected ITriggerInput inputHandler;
    /**
     * This list is used for JUnitTest purpose.
     */
    private List mListAvailableTriggersToRelease;

    private int miNumberAvailableTriggerToRelease;
    private int miCount;

    protected static final int DEFAULT_MAX_TIMEGATE_WINDOW = 0;
    protected static final int miTimeGap_No = 1;
    protected static final int miTimeGap_Yes = 2;
    protected static final int DEFAULT_TIMEGAP_OPTION = miTimeGap_Yes;

    //--assign Default value.
    private int miMaxTimeGateWindow = DEFAULT_MAX_TIMEGATE_WINDOW;
    private int miTimeGap_option = DEFAULT_TIMEGAP_OPTION;

    public int miTotalInputTriggers;
    public int miTotalNullInputTriggers;
    public int miTotalNonTRPInputTriggers;
    public int miTotalMergedInputTriggers;
    public int miTotalOutputGlobalTriggers;
    public int miTotalOutputMergedGlobalTriggers;
    int miMaxNum = 20;

    private int miInfoPrintoutFrequency = 100000;

    /**
     * Monitor object.
     */
    protected TriggerHandlerMonitor monitor;

    private double longestTrigger;

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public GlobalTriggerHandler()
    {
        this(new SourceID4B(6000), DEFAULT_TIMEGAP_OPTION);
    }

    public GlobalTriggerHandler(ISourceID sourceID)
    {
        this(sourceID, DEFAULT_TIMEGAP_OPTION);
    }

    public GlobalTriggerHandler(TriggerRequestPayloadFactory outputFactory) {
        this(new SourceID4B(6000), DEFAULT_TIMEGAP_OPTION, outputFactory);
    }

    public GlobalTriggerHandler(ISourceID sourceID, int iTimeGap_Option)
    {
        this(sourceID, iTimeGap_Option, new TriggerRequestPayloadFactory());
    }

    public GlobalTriggerHandler(ISourceID sourceID, int iTimeGap_Option, TriggerRequestPayloadFactory outputFactory) {
        this.sourceID = sourceID;
        this.outputFactory = outputFactory;

        this.init();
        ((GlobalTriggerBag) triggerBag).setTimeGap_option(iTimeGap_Option);
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
    /**
     * This method sends input payload to its destinantion algorithm
     * where filtering of payloads may occur if necessary.
     * This method will be removed once automatice routing for a payload is implemented.
     *
     * //todo: finish this method.
     *
     */
 /*   public void sendPayloadToFilterDestinantion(IPayload iPayload)
    {
        switch(iPayload.getPayloadInterfaceType()){
            case PayloadInterfaceRegistry.I_STOP_PAYLOAD:
                tStropTrigger.;
                break;
            case PayloadInterfaceRegistry.I_BEACON_PAYLOAD:

                break;
            case PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD:

                break;
            default:

                break;

        }
    }*/
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

        Iterator triggerIterator = configuredTriggerList.iterator();
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
        count = 0;
        miNumberAvailableTriggerToRelease = 0;
        miCount = 0;
        miTotalInputTriggers = 0;
        miTotalOutputGlobalTriggers = 0;
        miTotalOutputMergedGlobalTriggers = 0;
        earliestPayloadOfInterest = null;
        mListAvailableTriggersToRelease = new ArrayList();
        configuredTriggerList = new ArrayList();

        inputHandler = new TriggerInput();
        triggerBag = new GlobalTriggerBag();
        triggerBag.setPayloadFactory(outputFactory);

        //--following two values need to be reset anytime we want to change the values.
        //  Inside JBoss DAQ framework, those values are reset in enterRunning() stage.
        //  perhaps they need to be set in enterReady() stage, just right place to be.
       // ((GlobalTriggerBag) triggerBag).setMaxTimeGateWindow((int) getMaxTimeGateWindow());
       // ((GlobalTriggerBag) triggerBag).setTimeGap_option(getTimeGap_option());

        this.setMaxTimeGateWindow((int) getMaxTimeGateWindow());
        this.setTimeGap_option(getTimeGap_option());
        
        monitor = new TriggerHandlerMonitor();
        PayloadBagMonitor triggerBagMonitor = new PayloadBagMonitor();
        triggerBag.setMonitor(triggerBagMonitor);
        monitor.setTriggerBagMonitor(triggerBagMonitor);

        longestTrigger = 0.0;
    }
    /**
     * This is the main mehtod.
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
                    Vector vecSubPayloads = new Vector();
                    try {
                        vecSubPayloads = ((TriggerRequestPayload) tInputTrigger).getPayloads();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (DataFormatException e) {
                        e.printStackTrace();
                    }
                    //--Each component payload of the MergedPayload needs to be sent to its filter.
                    for(int i=0; i<vecSubPayloads.size(); i++)
                    {
                        ITriggerRequestPayload subPayload = (ITriggerRequestPayload) vecSubPayloads.get(i);
                        try {
                            ((ILoadablePayload) subPayload).loadPayload();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (DataFormatException e) {
                            e.printStackTrace();
                        }

                        //sendPayloadToFilterDestinantion((IPayload) vecSubPayloads.get(i));
                        Iterator triggerIterator = configuredTriggerList.iterator();
                        while (triggerIterator.hasNext()) {
                            ITriggerControl configuredTrigger = (ITriggerControl) triggerIterator.next();
                            try {
                                configuredTrigger.runTrigger(subPayload);
                                int triggerCounter = ((ITriggerMonitor) configuredTrigger).getTriggerCounter();
                                if(triggerCounter % miInfoPrintoutFrequency == 0 && triggerCounter >= miInfoPrintoutFrequency){
                                    log.info(((ITriggerConfig) configuredTrigger).
                                            getTriggerName() + ":  #  " + triggerCounter);

                                }

                            } catch (TriggerException e) {
                                log.error("Exception while running configuredTrigger: " + e);
                            }
                        }
                    }
                }else{
                    log.debug("Now start processing single trigger input");
                    //sendPayloadToFilterDestinantion(tInputTrigger);
                    Iterator triggerIterator = configuredTriggerList.iterator();
                    while (triggerIterator.hasNext()) {
                        ITriggerControl configuredTrigger = (ITriggerControl) triggerIterator.next();
                        try {
                            configuredTrigger.runTrigger(tInputTrigger);
                        } catch (TriggerException e) {
                            log.error("Exception while running configuredTrigger: " + e);
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
     * check triggerBag and issue/ship GTEventPayload if possible
     *   any triggers that are earlier than the earliestPayloadOfInterest are selected
     *   if any of those overlap, they are merged
     */
    public void issueTriggers() {

        if (log.isDebugEnabled()) {
            log.debug("GlobalTrig Bag contains " + triggerBag.size() + " triggers");
        }

        //--update the most earliest time of interest. determined by the slowest trigger.
        // this updates the timeGate in GlobalTrigBag.
        setEarliestTime();

        while (triggerBag.hasNext()) {
            TriggerRequestPayload GTEventPayload = triggerBag.next();
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
                e.printStackTrace();
            }

            if (log.isInfoEnabled()) {
                if(miTotalOutputGlobalTriggers%miInfoPrintoutFrequency == 0){
                    log.info("Issue # " + miTotalOutputGlobalTriggers + " GTEventPayload (trigType = " + GT_trigType + " ) : "
                             + " extended event time = " + firstTime.getUTCTimeAsLong() + " to "
                             + lastTime.getUTCTimeAsLong() + " and contains " + nSubPayloads + " subTriggers"
                             + ", payload length = " + GTEventPayload.getPayloadLength() + "bytes");
                    if(-1 == GT_trigType){
                        log.info("Merged GT # " + miTotalOutputMergedGlobalTriggers);
                    }
                }
                if(nSubPayloads > miMaxNum){
                    miMaxNum = nSubPayloads;
                    log.info("paylaod length = " + GTEventPayload.getPayloadLength() + "bytes");
                    //TriggerTestUtil testUtil = new TriggerTestUtil();
                    //testUtil.show_trigger_Info("Final GT ", miTotalOutputGlobalTriggers, GTEventPayload);
                }
            }

            //--ship the GTEventPayload to its destinantion (i.e., EB).
            if (null == payloadDestination) {
                log.error("PayloadDestination has not been set!");
            } else {
                try {
                    payloadDestination.writePayload(GTEventPayload);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                GTEventPayload.recycle();
                //sendTrigger(GTEventPayload);
            }
        }
    }
    /**
     * send trigger to output destination.
     * @param trigger
     */
    public void sendTrigger(Payload trigger) {
        // issue the trigger
        try {
            payloadDestination.writePayload(trigger);
        } catch (IOException e) {
            e.printStackTrace();
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
                log.debug("There is earliestPayload: Time = " + earliestPayload.getPayloadTimeUTC().getUTCTimeAsLong());
                // if payload < earliest
                if (earliestTimeOverall.compareTo(earliestPayload.getPayloadTimeUTC()) > 0) {
                    earliestTimeOverall = earliestPayload.getPayloadTimeUTC();
                    earliestPayloadOverall = earliestPayload;
                }
                log.debug("There is earliestPayloadOverall: Time = "
                         + earliestPayloadOverall.getPayloadTimeUTC().getUTCTimeAsLong());
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
     * iTimeGap_option = 1 --> No_TimeGap
     *                 = 2 --> Yes_TimeGap
     *
     * @param iTimeGap_option
     */
    public void setTimeGap_option(int iTimeGap_option)
    {
        miTimeGap_option = iTimeGap_option;
        ((GlobalTriggerBag) triggerBag).setTimeGap_option(iTimeGap_option);
    }

    public int getTimeGap_option()
    {
        return miTimeGap_option;
    }

    public void setPayloadDestinationCollection(IPayloadDestinationCollection payloadDestination)
    {
        this.payloadDestination = payloadDestination;
    }

    public List getListAvailableTriggerToRelease()
    {
        return mListAvailableTriggersToRelease;
    }
}
