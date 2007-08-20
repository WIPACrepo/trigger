/*
 * class: CoincidenceTriggerBag
 *
 * Version $Id: ConditionalTriggerBag.java,v 1.14 2005/12/22 14:01:37 shseo Exp $
 *
 * Date: September 2 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.splicer.Payload;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.algorithm.CoincidenceTrigger;
import icecube.daq.trigger.impl.TriggerRequestPayload;

import java.util.*;
import java.util.zip.DataFormatException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is to provide some methods in GlobalTrigBag to CoincidenceTrigger algorithms.
 * This bag is handled by CoincidenceTrigger.
 * (cf. GlobalTrigBag is handled by GlobalTrigHandler.)
 *
 * @version $Id: ConditionalTriggerBag.java,v 1.14 2005/12/22 14:01:37 shseo Exp $
 * @author shseo
 */
public class ConditionalTriggerBag
        extends GlobalTriggerBag
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(ConditionalTriggerBag.class);
    private CoincidenceTrigger mtCoincidenceTriggerAlgorithm;
    public List mListConfiguredTriggerIDs;
    public boolean mbContainAllTriggerIDsRequired = false;
    public String msCoincidenceTriggerAlgorithmName;

    public Vector payloadListInConditionalBag = new Vector();

    /**
     * set of overlapping triggers to merge
     */
    protected static List mergeListForConditionalBag = new ArrayList();

    public boolean flushing = false;

    /**
     * UID for newly merged triggers
     */
    public int triggerUID;

    private int miCount;
    private boolean mbNeedUpdate = false;
    private DummyPayload mtUpdater = null;
    public List mListUnqualifiedTriggers = new ArrayList();
    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public ConditionalTriggerBag()
    {
        super();
    }

    public ConditionalTriggerBag(int iTrigType, int iTrigconfigId, ISourceID tSourceId)
    {
        super(iTrigType, iTrigconfigId, tSourceId);
    }
    /**
     * Overrided mothod in super class.
     * This method cotains specific conditional trigger algorithm.
     *
     * @param newPayload
     */
    public void add(ILoadablePayload newPayload)
    {
        try {
            newPayload.loadPayload();
        } catch (Exception e) {
            log.error("Error loading newPayload", e);
        }

        //--accept only configured triggers.
        if(!mtCoincidenceTriggerAlgorithm.isConfiguredTrigger((ITriggerRequestPayload)newPayload)){
            return;
        }

        //--add to internal list
        if (payloadListInConditionalBag.isEmpty()) {

            if (log.isDebugEnabled()) {
                log.debug("Adding trigger to empty bag");
            }

            payloadListInConditionalBag.add(newPayload);

        } else {

            if (log.isDebugEnabled())
            {
                log.debug("Adding newPayload to a full bag");
            }

            //--mergeList should be emptied before adding.
            mergeListForConditionalBag.clear();

            //--loop over existing triggers to check timeOverlap w/ currentTrigger.
            Iterator iter = payloadListInConditionalBag.iterator();
            while (iter.hasNext())
            {
                TriggerRequestPayload existingPayload = (TriggerRequestPayload) iter.next();

                //--check if CoincidenceTrigger
                if (mtCoincidenceTriggerAlgorithm.isCoincidentTrigger((TriggerRequestPayload) existingPayload,
                                                             (TriggerRequestPayload) newPayload))
                {
                    if (log.isDebugEnabled()) {
                        log.debug("Two payloads are coincident");
                    }
                    if(!mergeListForConditionalBag.contains(existingPayload))
                    {
                        mergeListForConditionalBag.add(existingPayload);
                    }
                    if(!mergeListForConditionalBag.contains(newPayload))
                    {
                        mergeListForConditionalBag.add(newPayload);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Two payloads are not coincident");
                    }
                }
            }

            //--merge if neccessary, else add newPayload to list
            if (!mergeListForConditionalBag.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Lets merge " + mergeListForConditionalBag.size() + " payloads for "
                            + msCoincidenceTriggerAlgorithmName);
                }
                for(int i=0; i<mergeListForConditionalBag.size(); i++)
                {
                    ITriggerRequestPayload trigger = (ITriggerRequestPayload) mergeListForConditionalBag.get(i);
                    log.debug("Trigger in mergeList: FirstTime = " + trigger.getFirstTimeUTC().getUTCTimeAsLong());
                    log.debug("Trigger in mergeList: LastTime = " + trigger.getLastTimeUTC().getUTCTimeAsLong());
                }

                Collections.sort(mergeListForConditionalBag);
                //-- performed prevention of multiple wrapping in this stage: only single wrap !!!
                mtGlobalTrigEventWrapper.wrapMergingEvent(mergeListForConditionalBag,
                        mtCoincidenceTriggerAlgorithm.getTriggerType(),
                        mtCoincidenceTriggerAlgorithm.getTriggerConfigId());

                //-- remove individual triggers from triggerList and add new merged trigger
                payloadListInConditionalBag.removeAll(mergeListForConditionalBag);
                payloadListInConditionalBag.add(mtGlobalTrigEventWrapper.getGlobalTrigEventPayload_merged());

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No need to merge");
                }

                payloadListInConditionalBag.add(newPayload);
            }

            Collections.sort(payloadListInConditionalBag);
        }

        if (log.isDebugEnabled()) {
            log.debug("Selected CoincidenceTriggerList has " + payloadListInConditionalBag.size() + " payloads.");
            log.debug("   TimeGate at " + timeGate.getUTCTimeAsLong());
        }

    }
    /**
     * This method checks a selected coincidenceTrigger (which was merged already)
     * contains all triggerIDs required by a specipic coincidenceTriggerAlgorithm.
     *
     * @return
     */
    public boolean containAllTriggerIDsRequired(ITriggerRequestPayload tTrigger)
    {
        if(tTrigger.getSourceID().getSourceID() != mtGlobalTrigEventWrapper.mtGlobalTriggerSourceID.getSourceID()){
            return false;
        }else{
            Vector vecTriggers = new Vector();
            try {
                vecTriggers = tTrigger.getPayloads();
            } catch (IOException e) {
                log.error("Couldn't get payloads", e);
            } catch (DataFormatException e) {
                log.error("Couldn't get payloads", e);
            }

            miCount++;

            //--find triggerIDs
            List listTriggerIDs = new ArrayList();
            Iterator iterTriggers = vecTriggers.iterator();
            while(iterTriggers.hasNext())
            {
                ITriggerRequestPayload tPayload = (ITriggerRequestPayload) iterTriggers.next();
                try {
                    ((ILoadablePayload) tPayload).loadPayload();
                } catch (IOException e) {
                    log.error("Couldn't load payload", e);
                } catch (DataFormatException e) {
                    log.error("Couldn't load payload", e);
                }

                Integer tTriggerId = new Integer(mtCoincidenceTriggerAlgorithm.getTriggerId(tPayload));
                if(!listTriggerIDs.contains(tTriggerId))
                {
                    listTriggerIDs.add(tTriggerId);
                }
            }

            //--compare w/ getTriggerIDs from triggerAlgorithm
            return listTriggerIDs.containsAll(mListConfiguredTriggerIDs);
        }
    }
    /**
     * This is to remove any unqualified triggers before checking hasNext().
     * unqualified trigger == not-contain all trigger IDs required && lastTime < timeGate
     */
    public void removeUnqualifiedTriggers()
    {
        int iTriggersInBag = payloadListInConditionalBag.size();
        boolean bRemoved = false;
        if(iTriggersInBag > 0){
            for(int i=0; i < iTriggersInBag; i++){
                if(bRemoved){
                    i--;
                    bRemoved = false;
                }else{
                    ITriggerRequestPayload tPayload = (ITriggerRequestPayload) payloadListInConditionalBag.get(i);
                    IUTCTime lastTime = tPayload.getLastTimeUTC();
                    if((!containAllTriggerIDsRequired(tPayload) && timeGate.compareTo(lastTime) > 0)
                    || (!containAllTriggerIDsRequired(tPayload) && flushing)){
                        payloadListInConditionalBag.remove(i);
                        setUpdateInfo(new DummyPayload(tPayload.getFirstTimeUTC()));
                        //((ILoadablePayload)tPayload).recycle();
                      //  mListUnqualifiedTriggers.add(tPayload);
                        iTriggersInBag = payloadListInConditionalBag.size();
                        bRemoved = true;
                    }
                }
            }

        }
    }

    public void flush() {
        flushing = true;
    }

    /**
     * this method checks whether triggers in the payload list are releasable or not
     * by comparing against timeGate.
     * @return
     */
    public boolean hasNext() {
        //--remove invalid triggers before processing further.
        initUpdateInfo();
        removeUnqualifiedTriggers();
        //--iterate over triggerList and check against timeGate
        Iterator iter = payloadListInConditionalBag.iterator();

        while (iter.hasNext())
        {
            ITriggerRequestPayload trigger = (ITriggerRequestPayload) iter.next();

            if (flushing || 0 < timeGate.compareTo(trigger.getLastTimeUTC())) {
           /*     mbContainAllTriggerIDsRequired = containAllTriggerIDsRequired(trigger);

                if(!mbContainAllTriggerIDsRequired){
                    ((ILoadablePayload)trigger).recycle();
                }
                return mbContainAllTriggerIDsRequired;*/
                return true;
            }
        }
        return false;
    }

    /**
     * This method returns releasable trigger.
     *
     * @return
     */
    public TriggerRequestPayload next() {
        //-- iterate over triggerList and check against timeGate
        Iterator iter = payloadListInConditionalBag.iterator();
        while (iter.hasNext())
        {
            TriggerRequestPayload trigger = (TriggerRequestPayload) iter.next();
            double timeDiff = timeGate.timeDiff_ns(trigger.getLastTimeUTC());
            if ( flushing || 0 < timeGate.compareTo(trigger.getLastTimeUTC()) )
            {
                iter.remove();
                if (log.isDebugEnabled()) {
                    log.debug("Releasing trigger with timeDiff = " + timeDiff);
                }
                //--GTEventNumber should be assigned here.
                triggerUID++;
                mtGlobalTrigEventWrapper.wrapFinalEvent(trigger, triggerUID);
                trigger = (TriggerRequestPayload) mtGlobalTrigEventWrapper.getGlobalTrigEventPayload_final();

                return trigger;
            }
        }
        return null;
    }

    /**
     * This is to flush splicer when return = true
     * @return
     */
    public boolean needUpdate()
    {
        return mbNeedUpdate;
    }

    public DummyPayload getUpdater()
    {
        return mtUpdater;
    }

    public void initUpdateInfo(){
        mbNeedUpdate = false;
        mtUpdater = null;
    }
    public void setUpdateInfo(DummyPayload trigger)
    {
        mbNeedUpdate = true;
        mtUpdater = trigger;
    }
    /**
     * This method sets which CoincidenceTriggerAlgorithm being used in this ConditionalTriggerBag.
     * That information is absolutely necessary here.
     *
     * @param tCoincidenceTrigger
     */
    public void setConditionalTriggerAlgorithm(CoincidenceTrigger tCoincidenceTrigger)
    {
        mtCoincidenceTriggerAlgorithm = tCoincidenceTrigger;
        mListConfiguredTriggerIDs = mtCoincidenceTriggerAlgorithm.getConfiguredTriggerIDs();

        msCoincidenceTriggerAlgorithmName = mtCoincidenceTriggerAlgorithm.getClass().getName();
        log.info("A specific ConditionalTriggerAlgorithm was set in ConditionalTriggerBag: "
                    + msCoincidenceTriggerAlgorithmName);
    }

    public List getListUnqualifiedTriggers()
    {
        return mListUnqualifiedTriggers;
    }

    public Vector getVectorPayloadsInConditonalTriggerBag()
    {
        return payloadListInConditionalBag;
    }
    public void setVectorPayloadsInConditonalTriggerBag(Vector vecPayloads)
    {
        payloadListInConditionalBag.removeAllElements();
        payloadListInConditionalBag.add(vecPayloads);
    }
}
