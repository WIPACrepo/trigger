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

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.oldpayload.impl.PayloadFactory;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.SourceIdRegistry;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class merges two TriggerRequestPayloads, which was guaranteed to be overlap in time,
 *  and put in the payloadList.
 * This is called in GlobalTrigBag.java.
 *
 * @version $Id: GlobalTrigEventWrapper.java 4574 2009-08-28 21:32:32Z dglo $
 * @author shseo
 */
public class GlobalTrigEventWrapper
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(GlobalTrigEventWrapper.class);

    private static final String GLOBAL_TRIGGER_NAME =
        DAQCmdInterface.DAQ_GLOBAL_TRIGGER;
    public static final ISourceID GLOBAL_TRIGGER_SOURCE_ID =
        SourceIdRegistry.getISourceIDFromNameAndId(GLOBAL_TRIGGER_NAME, 0);

    private static final TriggerRequestPayloadFactory DEFAULT_TRIGGER_FACTORY =
        new TriggerRequestPayloadFactory();

    private Sorter tSorter = new Sorter();
    private GlobalTrigEventReadoutElements  mtGlobalTrigEventReadoutElements;

    /**
     * The factory used to create triggers
     */
    private TriggerRequestPayloadFactory triggerFactory;

    private ITriggerRequestPayload mtGlobalTrigEventPayload_single;
    private ITriggerRequestPayload mtGlobalTrigEventPayload_merged;
    private ITriggerRequestPayload mtGlobalTrigEventPayload_final;

    private int miTriggerUID;
    private boolean mbIsCalled_Wrap_single;

    private int miNumMergedGTevent;

    public GlobalTrigEventWrapper()
    {
        miTriggerUID = 0;
        miNumMergedGTevent = 0;
        mtGlobalTrigEventReadoutElements = new GlobalTrigEventReadoutElements();
        setPayloadFactory(DEFAULT_TRIGGER_FACTORY);
    }
    /**
     * Collects all readout elements from input list of payloads into a vector.
     *
     * @param mergeList
     * @return
     */
    private List collectReadoutElements(List mergeList)
    {
        List vecGlobalReadoutRequestElements_Raw = new Vector();
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
     * Collects all subPayloads from input list of payloads into a vector.
     *
     * @param mergeList
     * @return
     */
    private List collectSubPayloads(List mergeList, boolean bIsFinalGTstage)
    {
        List vecGlobalSubPayload = new Vector();
        List vecLocalSubPayload = new Vector();
        Iterator iterMergeList = mergeList.iterator();

        while(iterMergeList.hasNext())
        {
            ITriggerRequestPayload tPayload = (ITriggerRequestPayload) iterMergeList.next();
            //testUtil.show_trigger_Info("inside collect subpayload up: ", miTriggerUID, tPayload);

            //--if subPayload is NOT a mergedTrigger
            if(tPayload.getSourceID().getSourceID() != GLOBAL_TRIGGER_SOURCE_ID.getSourceID())
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
                        //if (log.isDebugEnabled()) log.debug("size of the local subPayload = " + vecLocalSubPayload.size());
                    } catch (DataFormatException e) {
                        log.error("Couldn't get payloads", e);
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

        List elems;

        if(null != tReadoutRequest){

            elems = tReadoutRequest.getReadoutRequestElements();
            tUTCTime_earliest = tSorter.getUTCTimeEarliest(elems, false);
            tUTCTime_latest = tSorter.getUTCTimeLatest(elems, false);

        } else {//--Make sure ReadoutReqeust is null for Beacon, Stop triggers.

            elems = new Vector();
            tUTCTime_earliest = tTriggerRequestPayload.getFirstTimeUTC();
            tUTCTime_latest = tTriggerRequestPayload.getLastTimeUTC();
        }

        List vecSubpayloads = new Vector();
        vecSubpayloads.add(tTriggerRequestPayload);

        miTriggerUID++;
        mtGlobalTrigEventPayload_single = (ITriggerRequestPayload) triggerFactory.
                                createPayload(miTriggerUID,
                                              iGTrigType,
                                              iGTrigConfigID,
                                              GLOBAL_TRIGGER_SOURCE_ID,
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

        if (log.isDebugEnabled()) {
            log.debug("miTriggerUID (ConditionalTrigger counter #) = " + miTriggerUID);
        }

        List vecGlobalSubPayload = collectSubPayloads(mergeList, false);

        List vecGlobalReadoutRequestElements_Raw = collectReadoutElements(mergeList);

        if (log.isDebugEnabled()) {
            log.debug("We have " + mergeList.size() + " selected conditionalTriggers to wrap.");
        }
        //--------------------------------------------------------------------------------------------------------/
        //--create a readout request for the new trigger
        IReadoutRequest tReadoutRequest = TriggerRequestPayloadFactory.createReadoutRequest(GLOBAL_TRIGGER_SOURCE_ID,
                                                                                            miTriggerUID,
                                                                              vecGlobalReadoutRequestElements_Raw);
        miTriggerUID++;
        //--create the MergedGlobalTriggerEventPayload
        mtGlobalTrigEventPayload_merged = (ITriggerRequestPayload) triggerFactory.createPayload(miTriggerUID,
                                                                                               iGTrigType,
                                                                                               iGTrigConfigID,
                                                                                                GLOBAL_TRIGGER_SOURCE_ID,
                                                                                                tSorter.getUTCTimeEarliest((List) vecGlobalReadoutRequestElements_Raw),
                                                                                                tSorter.getUTCTimeLatest((List) vecGlobalReadoutRequestElements_Raw),
                                                                                                vecGlobalSubPayload,
                                                                                                tReadoutRequest);

        Iterator iter = vecGlobalSubPayload.iterator();
        while(iter.hasNext())
        {
            ITriggerRequestPayload subPayload =
                (ITriggerRequestPayload) iter.next();
            // XXX shouldn't need to load payload before recycling it
            try {
                ((ILoadablePayload) subPayload).loadPayload();
            } catch (IOException e) {
                log.error("Couldn't load payload", e);
            } catch (DataFormatException e) {
                log.error("Couldn't load payload", e);
            }
            if(subPayload.getSourceID().getSourceID() ==
               GLOBAL_TRIGGER_SOURCE_ID.getSourceID())
            {
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

        List vecGlobalSubPayload = collectSubPayloads(mergeList, true);
        List vecGlobalReadoutRequestElements_Raw = collectReadoutElements(mergeList);

        if (log.isDebugEnabled()) {
            log.debug("We have " + mergeList.size() + " triggers to wrapMergingEvent");
        }

        List vecGlobalReadoutRequestElements_Final;

        if(vecGlobalReadoutRequestElements_Raw.size() > 1)
        {
            vecGlobalReadoutRequestElements_Final
                = new Vector(mtGlobalTrigEventReadoutElements.getManagedFinalReadoutRequestElements(vecGlobalReadoutRequestElements_Raw));

        }else if(vecGlobalReadoutRequestElements_Raw.size() == 1){

            vecGlobalReadoutRequestElements_Final = vecGlobalReadoutRequestElements_Raw;

        }else{

            vecGlobalReadoutRequestElements_Final = new Vector();
        }

        if (log.isDebugEnabled()) {
            log.debug("size Final readoutElements in wrapMergingEvent()_merged= " + vecGlobalReadoutRequestElements_Final.size());
        }

//---------------------------------------------------------------------------------------------------------------
        // create a readout request for the new trigger
        IReadoutRequest tReadoutRequest = TriggerRequestPayloadFactory.createReadoutRequest(GLOBAL_TRIGGER_SOURCE_ID,
                                                                              miTriggerUID,
                                                                              vecGlobalReadoutRequestElements_Final);

        //For a mergedGlobalTrigEvent, iMergedTriggerType = -1, iMergedTriggerConfigID = -1.
        //does mergeList can contain one element? check GlobalTrigBag.java --> No!
        int iMergedTriggerType = -1;
        int iMergedTriggerConfigID = -1;

        miTriggerUID++;
        // create the MergedGlobalTriggerEventPayload
        mtGlobalTrigEventPayload_merged = (ITriggerRequestPayload) triggerFactory.createPayload(miTriggerUID,
                                                                                                iMergedTriggerType,
                                                                                                iMergedTriggerConfigID,
                                                                                                GLOBAL_TRIGGER_SOURCE_ID,
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
    public void wrapFinalEvent(ITriggerRequestPayload tGTEvent, int iEvtNumber)
    {
        List elems = ((IReadoutRequest) tGTEvent.getReadoutRequest()).getReadoutRequestElements();

        IReadoutRequest tReadoutRequest_final = TriggerRequestPayloadFactory.createReadoutRequest(tGTEvent.getSourceID(),
                                                                                    iEvtNumber,
                                                                                    elems);
        try {

            mtGlobalTrigEventPayload_final = (ITriggerRequestPayload) triggerFactory.createPayload(iEvtNumber,
                                                                                                   tGTEvent.getTriggerType(),
                                                                                                   tGTEvent.getTriggerConfigID(),
                                                                                                   tGTEvent.getSourceID(),
                                                                                                   tGTEvent.getFirstTimeUTC(),
                                                                                                   tGTEvent.getLastTimeUTC(),
                                                                                                   tGTEvent.getPayloads(),
                                                                                                   tReadoutRequest_final);

        } catch (DataFormatException e) {
            log.error("Couldn't create payload", e);
        }

        //--count merged GT event.
        if(tGTEvent.getTriggerType() == -1)
        {
            miNumMergedGTevent++;
        }
        if (log.isDebugEnabled()) {
            log.debug("Total # of Final GT Event so far = " + iEvtNumber);
            log.debug("Total # of Final Merged-GT event so far = " +
                      miNumMergedGTevent);
        }

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
    /**
     * Sets timeGateOption. This should be set in configuration.
     * @param iTimeGap_option
     */
    protected void setAllowTimeGap(boolean allowTimeGap)
    {
        mtGlobalTrigEventReadoutElements.setAllowTimeGap(allowTimeGap);
    }

}
