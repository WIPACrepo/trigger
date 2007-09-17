/*
 * class: TwoPayloadsMerger
 *
 * Version $Id: TwoPayloadsMerger.java, shseo
 *
 * Date: August 3 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.*;
import icecube.daq.payload.splicer.PayloadFactory;
import icecube.daq.payload.splicer.Payload;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.trigger.impl.TriggerRequestPayload;
import icecube.daq.common.DAQCmdInterface;
//import icecube.daq.globalTrig.util.TriggerTestUtil;

import java.util.*;
import java.util.zip.DataFormatException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class merges two TriggerRequestPayloads, which was guaranteed to be overlap in time,
 *  and put in the payloadList.
 * This is called in GlobalTrigBag.java.
 *
 * @version $Id: GlobalTrigEventWrapper.java,v 1.19 2005/10/26 18:31:44 toale Exp $
 * @author shseo
 */
public class GlobalTrigEventWrapper
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(GlobalTrigEventWrapper.class);

    private Sorter tSorter = new Sorter();
    private GlobalTrigEventReadoutElements  mtGlobalTrigEventReadoutElements;
    private PayloadDestination asciiFileOutPayloadDestination;

    /**
     * The factory used to create triggers
     */

    private TriggerRequestPayloadFactory triggerFactory;
    private TriggerRequestPayloadFactory DEFAULT_TRIGGER_FACTORY = new TriggerRequestPayloadFactory();

    private TriggerRequestPayload mtGlobalTrigEventPayload_single;
    private TriggerRequestPayload mtGlobalTrigEventPayload_merged;
    private TriggerRequestPayload mtGlobalTrigEventPayload_final;
    private TriggerRequestPayload mtPayload_onlyTimeWrapped;

 //   SortedSet mergeSet = new TreeSet();

    private int miTriggerUID;
    private boolean mbIsCalled_Wrap_single;
    public final ISourceID mtGlobalTriggerSourceID = SourceIdRegistry.
            getISourceIDFromNameAndId(DAQCmdInterface.DAQ_GLOBAL_TRIGGER, 0);

    private Vector mVecGlobalReadoutRequestElements = new Vector();
    public int miNumMergedGTevent;

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     *
     * Use this constructor for JUnit test.
     *
     */
    public GlobalTrigEventWrapper()
    {
        this(1); // No_TimeGap configuration
        setPayloadFactory(DEFAULT_TRIGGER_FACTORY);
    }

    public GlobalTrigEventWrapper(int iTimeGap_option)
    {
        miTriggerUID = 0;
        miNumMergedGTevent = 0;
        mtGlobalTrigEventReadoutElements = new GlobalTrigEventReadoutElements();
        mtGlobalTrigEventReadoutElements.setTimeGap_option(iTimeGap_option);//No_TimeGap
    }
    /**
     * This method is to provide corrected time using readoutTimes,
     * so that LowThresholdTrigger can use it for checking overalp.
     *
     * @param tTriggerRequestPayload
     */
    public void wrapTime(ITriggerRequestPayload tTriggerRequestPayload)
    {
        IReadoutRequest tReadoutRequest = tTriggerRequestPayload.getReadoutRequest();
        Vector vecReadoutElement = new Vector();

        IUTCTime tUTCTime_earliest = null;
        IUTCTime tUTCTime_latest = null;

        if(null != tReadoutRequest){

            vecReadoutElement = tReadoutRequest.getReadoutRequestElements();
            tUTCTime_earliest = tSorter.getUTCTimeEarliest((List) vecReadoutElement, false);
            tUTCTime_latest = tSorter.getUTCTimeLatest((List) vecReadoutElement, false);

        } else {//--Make sure ReadoutReqeust is null for Beacon, Stop triggers.

            tUTCTime_earliest = tTriggerRequestPayload.getFirstTimeUTC();
            tUTCTime_latest = tTriggerRequestPayload.getLastTimeUTC();
        }

        try {
            mtPayload_onlyTimeWrapped = (TriggerRequestPayload) triggerFactory.
                                   createPayload(tTriggerRequestPayload.getUID(),
                                                 tTriggerRequestPayload.getTriggerType(),
                                                 tTriggerRequestPayload.getTriggerConfigID(),
                                                 tTriggerRequestPayload.getSourceID(),
                                                 tUTCTime_earliest,
                                                 tUTCTime_latest,
                                                 tTriggerRequestPayload.getPayloads(),
                                                 tTriggerRequestPayload.getReadoutRequest());
        } catch (IOException e) {
            log.error("Couldn't wrap trigger time", e);
        } catch (DataFormatException e) {
            log.error("Couldn't wrap trigger time", e);
        }

        ((ILoadablePayload) tTriggerRequestPayload).recycle();

    }
    /**
     * Collects all readout elements from input list of payloads into a vector.
     *
     * @param mergeList
     * @return
     */
    private Vector collectReadoutElements(List mergeList)
    {
        Vector vecGlobalReadoutRequestElements_Raw = new Vector();
        Iterator iterMergeList = mergeList.iterator();
        while(iterMergeList.hasNext())
        {
            ITriggerRequestPayload tPayload = (ITriggerRequestPayload) iterMergeList.next();

            IReadoutRequest tReadoutRequest = tPayload.getReadoutRequest();
            if(tReadoutRequest != null)
            {
                vecGlobalReadoutRequestElements_Raw.addAll(
                                tReadoutRequest.getReadoutRequestElements());
            }
        }
        return vecGlobalReadoutRequestElements_Raw;
    }
    /**
     * Collects all subPayloads from input list of paylaods into a vector.
     *
     * @param mergeList
     * @return
     */
    private Vector collectSubPayloads(List mergeList, boolean bIsFinalGTstage)
    {
        Vector vecGlobalSubPayload = new Vector();
        Vector vecLocalSubPayload = new Vector();
        Iterator iterMergeList = mergeList.iterator();

        while(iterMergeList.hasNext())
        {
            TriggerRequestPayload tPayload = (TriggerRequestPayload) iterMergeList.next();
            //testUtil.show_trigger_Info("inside collect subpayload up: ", miTriggerUID, tPayload);

            //--if subPayload is NOT a mergedTirgger
            if(tPayload.getSourceID().getSourceID() != mtGlobalTriggerSourceID.getSourceID())
            {
                vecLocalSubPayload.add(tPayload);

            //--if subPayload IS already mergedTrigger
            }else
            {
                //testUtil.show_trigger_Info("inside collect subpayload: ", miTriggerUID, tPayload);
                if(bIsFinalGTstage && tPayload.getTriggerType() != -1)
                {
                    vecLocalSubPayload.add(tPayload);
                }else{
                    try {
                        vecLocalSubPayload.addAll(((ITriggerRequestPayload) tPayload).getPayloads());
                        //log.debug("size of the local subPayload = " + vecLocalSubPayload.size());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (DataFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        vecGlobalSubPayload.addAll(vecLocalSubPayload);
        return vecGlobalSubPayload;
    }
    //--todo: readoutRequest is null for special triggers....Beacon, stop...
    /**
     * This method is to make GlobalTrigEvent for a single TriggerRequestPayload.
     * This can be called in ThroughputTrigger, StopTrigger, or any other trigger algorithm
     * where a GTEvent needs to be made for a single TriggerReqeustPayload (i.e., unMergedPayload).
     * ReadoutRequest will be the same.
     */
    public void wrap(ITriggerRequestPayload tTriggerRequestPayload, int iGTrigType, int iGTrigConfigID)
    {
        IUTCTime tUTCTime_earliest = null;
        IUTCTime tUTCTime_latest = null;

        // tReadoutRequest is the same for a single payload.
        IReadoutRequest tReadoutRequest = tTriggerRequestPayload.getReadoutRequest();

        Vector vecReadoutElement = new Vector();

        if(null != tReadoutRequest){

            vecReadoutElement = tReadoutRequest.getReadoutRequestElements();
            tUTCTime_earliest = tSorter.getUTCTimeEarliest((List) vecReadoutElement, false);
            tUTCTime_latest = tSorter.getUTCTimeLatest((List) vecReadoutElement, false);

        } else {//--Make sure ReadoutReqeust is null for Beacon, Stop triggers.

            tUTCTime_earliest = tTriggerRequestPayload.getFirstTimeUTC();
            tUTCTime_latest = tTriggerRequestPayload.getLastTimeUTC();
        }

        Vector vecSubpayloads = new Vector();
        vecSubpayloads.add(tTriggerRequestPayload);

        miTriggerUID++;
        mtGlobalTrigEventPayload_single = (TriggerRequestPayload) triggerFactory.
                                createPayload(miTriggerUID,
                                              iGTrigType,
                                              iGTrigConfigID,
                                              mtGlobalTriggerSourceID,
                                              tUTCTime_earliest,
                                              tUTCTime_latest,
                                              vecSubpayloads,
                                              tReadoutRequest);

        //no need to recycle vecSubpayloads

        mbIsCalled_Wrap_single = true;
    }
    /**
     * This method will create TriggerRequestPayload using input list of payload
     * selected from conditionalTriggers.
     * Since this is not the final event, readoutRequest elements are not managed here
     * but just collected.
     *
     * @param mergeList
     * @param iGTrigType
     * @param iGTrigConfigID
     */
    public void wrapMergingEvent(List mergeList, int iGTrigType, int iGTrigConfigID)
    {
        //--in case wrap_single method was called, mtTriggerUID needs to be reset.
        if(mbIsCalled_Wrap_single)
        {
            miTriggerUID = 0;
        }

        log.debug("miTriggerUID (ConditionalTrigger counter #) = " + miTriggerUID);

        Vector vecGlobalSubPayload = collectSubPayloads(mergeList, false);

        Vector vecGlobalReadoutRequestElements_Raw = collectReadoutElements(mergeList);

        if (log.isDebugEnabled()) {
            log.debug("We have " + mergeList.size() + " selected conditionalTriggers to wrap.");
        }
        //--------------------------------------------------------------------------------------------------------/
        //--create a readout request for the new trigger
        IReadoutRequest tReadoutRequest = TriggerRequestPayloadFactory.createReadoutRequest(mtGlobalTriggerSourceID,
                                                                                            miTriggerUID,
                                                                              vecGlobalReadoutRequestElements_Raw);
        miTriggerUID++;
        //--create the MergedGlobalTriggerEventPayload
        mtGlobalTrigEventPayload_merged = (TriggerRequestPayload) triggerFactory.createPayload(miTriggerUID,
                                                                                               iGTrigType,
                                                                                               iGTrigConfigID,
                                                                                                mtGlobalTriggerSourceID,
                                                                                                tSorter.getUTCTimeEarliest((List) vecGlobalReadoutRequestElements_Raw),
                                                                                                tSorter.getUTCTimeLatest((List) vecGlobalReadoutRequestElements_Raw),
                                                                                                vecGlobalSubPayload,
                                                                                                tReadoutRequest);

        Iterator iter = vecGlobalSubPayload.iterator();
        while(iter.hasNext())
        {
            ITriggerRequestPayload subPayload = (ITriggerRequestPayload) iter.next();
            try {
                ((ILoadablePayload) subPayload).loadPayload();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (DataFormatException e) {
                e.printStackTrace();
            }
            if(subPayload.getSourceID().getSourceID() == mtGlobalTriggerSourceID.getSourceID()){
                ((ILoadablePayload) subPayload).recycle();
            }
        }
    }
    /**
     * This method creates TriggerRequestPayload for final GT event.
     *
     * @param mergeList
     */
    public void wrapMergingEvent(List mergeList)
    {
        //--in case wrap_single method was called, mtTriggerUID needs to be reset.
        //todo: following may not be needed....
        if(mbIsCalled_Wrap_single)
        {
            miTriggerUID = 0;
        }

        Vector vecGlobalSubPayload = collectSubPayloads(mergeList, true);
        Vector vecGlobalReadoutRequestElements_Raw = collectReadoutElements(mergeList);

        if (log.isDebugEnabled()) {
            log.debug("We have " + mergeList.size() + " triggers to wrapMergingEvent");
        }

        Vector vecGlobalReadoutRequestElements_Final = new Vector();

        if(vecGlobalReadoutRequestElements_Raw.size() > 1)
        {
            vecGlobalReadoutRequestElements_Final
                = mtGlobalTrigEventReadoutElements.getManagedFinalReadoutRequestElements(vecGlobalReadoutRequestElements_Raw);

        }else if(vecGlobalReadoutRequestElements_Raw.size() == 1){

            vecGlobalReadoutRequestElements_Final = vecGlobalReadoutRequestElements_Raw;

        }else if(vecGlobalReadoutRequestElements_Raw.size() < 1){

            vecGlobalReadoutRequestElements_Final = null;
        }

        log.debug("size Final readoutElements in wrapMergingEvent()_merged= " + vecGlobalReadoutRequestElements_Final.size());

//---------------------------------------------------------------------------------------------------------------
        // create a readout request for the new trigger
        IReadoutRequest tReadoutRequest = TriggerRequestPayloadFactory.createReadoutRequest(mtGlobalTriggerSourceID,
                                                                              miTriggerUID,
                                                                              vecGlobalReadoutRequestElements_Final);

        //For a mergedGlobalTrigEvent, iMergedTriggerType = -1, iMergedTriggerConfigID = -1.
        //does mergeList can contain one element? check GlobalTrigBag.java --> No!
        int iMergedTriggerType = -1;
        int iMergedTriggerConfigID = -1;

        miTriggerUID++;
        // create the MergedGlobalTriggerEventPayload
        mtGlobalTrigEventPayload_merged = (TriggerRequestPayload) triggerFactory.createPayload(miTriggerUID,
                                                                                                iMergedTriggerType,
                                                                                                iMergedTriggerConfigID,
                                                                                                mtGlobalTriggerSourceID,
                                                                                                tSorter.getUTCTimeEarliest(mergeList,true),
                                                                                                tSorter.getUTCTimeLatest(mergeList,true),
                                                                                                vecGlobalSubPayload,
                                                                                                tReadoutRequest);
        Iterator iter = vecGlobalSubPayload.iterator();
        while(iter.hasNext())
        {
            ((ILoadablePayload) iter.next()).recycle();
        }
    }

    public ITriggerRequestPayload getGlobalTrigEventPayload_single()
    {
        return mtGlobalTrigEventPayload_single;
    }

    public ITriggerRequestPayload getGlobalTrigEventPayload_merged()
    {
        return mtGlobalTrigEventPayload_merged;
    }

    public ITriggerRequestPayload getGlobalTrigEventPayload_final()
    {
        return mtGlobalTrigEventPayload_final;
    }
    /**
     * This method sets final GlobalTrigEventNumber only.
     * Thus other content of the input GTEvent should not be changed.
     *  Note: ReadoutRequest also contains EventNumber. Thus its eventNumber will be set, too.
     *
     * @param tGTEvent
     * @param iEvtNumber
     */
    public void wrapFinalEvent(TriggerRequestPayload tGTEvent, int iEvtNumber)
    {
        Vector vecReadoutRequestElements = ((IReadoutRequest) tGTEvent.getReadoutRequest()).getReadoutRequestElements();

        IReadoutRequest tReadoutRequest_final = TriggerRequestPayloadFactory.createReadoutRequest(tGTEvent.getSourceID(),
                                                                                    iEvtNumber,
                                                                                    vecReadoutRequestElements);
        try {

            mtGlobalTrigEventPayload_final = (TriggerRequestPayload) triggerFactory.createPayload(iEvtNumber,
                                                                                                   tGTEvent.getTriggerType(),
                                                                                                   tGTEvent.getTriggerConfigID(),
                                                                                                   tGTEvent.getSourceID(),
                                                                                                   tGTEvent.getFirstTimeUTC(),
                                                                                                   tGTEvent.getLastTimeUTC(),
                                                                                                   tGTEvent.getPayloads(),
                                                                                                   tReadoutRequest_final);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (DataFormatException e) {
            e.printStackTrace();
        }

        //--count merged GT event.
        if(tGTEvent.getTriggerType() == -1)
        {
            miNumMergedGTevent++;
        }
        log.debug("Total # of Final GT Event so far = " + iEvtNumber);
        log.debug("Total # of Final Merged-GT event so far = " + miNumMergedGTevent);

        //--recycle
        ((ILoadablePayload) tGTEvent).recycle();

    }
    /**
     * Sets PayloadFactory.
     * @param payloadFactory
     */
    public void setPayloadFactory(PayloadFactory payloadFactory) {

        triggerFactory = (TriggerRequestPayloadFactory) payloadFactory;
        mtGlobalTrigEventReadoutElements.setPayloadFactory(triggerFactory);
    }
 /*   public void setPayloadFactory(TriggerRequestPayloadFactory triggerFactory) {
        this.triggerFactory = triggerFactory;
    }
    */
    /**
     * Sets timeGateOption. This should be set in configuration.
     * @param iTimeGap_option
     */
    public void setTimeGap_option(int iTimeGap_option)
    {
        mtGlobalTrigEventReadoutElements.setTimeGap_option(iTimeGap_option);
    }

}
