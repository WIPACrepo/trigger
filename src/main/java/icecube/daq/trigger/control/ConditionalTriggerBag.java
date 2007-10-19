/*
 * class: ConditionalTriggerBag
 *
 * Version $Id: ConditionalTriggerBag.java 2164 2007-10-19 17:21:58Z dglo $
 *
 * Date: September 2 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.algorithm.CoincidenceTrigger;

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
 * @version $Id: ConditionalTriggerBag.java 2164 2007-10-19 17:21:58Z dglo $
 * @author shseo
 */
public class ConditionalTriggerBag
        extends GlobalTriggerBag
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(ConditionalTriggerBag.class);

    /** No 'next' value is known. */
    private static final int NEXT_UNKNOWN = -1;
    /** There is no 'next' value. */
    private static final int NEXT_NONE = Integer.MIN_VALUE;

    private CoincidenceTrigger mtCoincidenceTriggerAlgorithm;
    private List mListConfiguredTriggerIDs;
    private boolean mbContainAllTriggerIDsRequired;
    private String msCoincidenceTriggerAlgorithmName;

    private List<ITriggerRequestPayload> payloadListInConditionalBag =
        new ArrayList<ITriggerRequestPayload>();

    private boolean flushing;

    /**
     * UID for newly merged triggers
     */
    private int triggerUID;

    private boolean mbNeedUpdate;
    private DummyPayload mtUpdater;
    private List mListUnqualifiedTriggers = new ArrayList();

    /** The index of the 'next' value (can be NEXT_UNKNOWN or NEXT_NONE). */
    private int nextIndex = NEXT_UNKNOWN;

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
            return;
        }

        ITriggerRequestPayload trigger = (ITriggerRequestPayload) newPayload;

        //--accept only configured triggers.
        if(!mtCoincidenceTriggerAlgorithm.isConfiguredTrigger(trigger)){
            return;
        }

        // reset 'next' index
        nextIndex = NEXT_UNKNOWN;

        //--add to internal list
        if (payloadListInConditionalBag.isEmpty()) {

            if (log.isDebugEnabled()) {
                log.debug("Adding trigger to empty bag");
            }

            payloadListInConditionalBag.add(trigger);

        } else {

            if (log.isDebugEnabled())
            {
                log.debug("Adding newPayload to a full bag");
            }

            List<ITriggerRequestPayload> mergeListForConditionalBag = null;

            //--loop over existing triggers to check timeOverlap w/ currentTrigger.
            boolean addedNewPayload = false;
            for (ITriggerRequestPayload existingPayload : payloadListInConditionalBag)
            {
                //--check if CoincidenceTrigger
                if (mtCoincidenceTriggerAlgorithm.isCoincidentTrigger(existingPayload,
                                                             trigger))
                {
                    if (log.isDebugEnabled()) {
                        log.debug("Two payloads are coincident");
                    }
                    if(mergeListForConditionalBag == null)
                    {
                        mergeListForConditionalBag =
                            new ArrayList<ITriggerRequestPayload>();
                    }
                    if(!mergeListForConditionalBag.contains(existingPayload))
                    {
                        mergeListForConditionalBag.add(existingPayload);
                    }
                    if(!addedNewPayload)
                    {
                        mergeListForConditionalBag.add(trigger);
                        addedNewPayload = true;
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Two payloads are not coincident");
                    }
                }
            }

            //--merge if neccessary, else add newPayload to list
            if (mergeListForConditionalBag != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Lets merge " + mergeListForConditionalBag.size() + " payloads for "
                            + msCoincidenceTriggerAlgorithmName);
                    for (ITriggerRequestPayload tr : mergeListForConditionalBag) {
                        log.debug("Trigger in mergeList: FirstTime = " + tr.getFirstTimeUTC());
                        log.debug("Trigger in mergeList: LastTime = " + tr.getLastTimeUTC());
                    }
                }

                Collections.sort((List) mergeListForConditionalBag);
                //-- performed prevention of multiple wrapping in this stage: only single wrap !!!
                getGlobalTrigEventWrapper().wrapMergingEvent(mergeListForConditionalBag,
                        mtCoincidenceTriggerAlgorithm.getTriggerType(),
                        mtCoincidenceTriggerAlgorithm.getTriggerConfigId());

                //-- remove individual triggers from triggerList and add new merged trigger
                payloadListInConditionalBag.removeAll(mergeListForConditionalBag);
                payloadListInConditionalBag.add(getGlobalTrigEventWrapper().getGlobalTrigEventPayload_merged());

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No need to merge");
                }

                payloadListInConditionalBag.add(trigger);
            }

            Collections.sort((List) payloadListInConditionalBag);
        }

        if (log.isDebugEnabled()) {
            log.debug("Selected CoincidenceTriggerList has " + payloadListInConditionalBag.size() + " payloads.");
            log.debug("   TimeGate at " + getTimeGate());
        }

    }
    /**
     * This method checks a selected coincidenceTrigger (which was merged already)
     * contains all triggerIDs required by a specipic coincidenceTriggerAlgorithm.
     *
     * @return
     */
    private boolean containAllTriggerIDsRequired(ITriggerRequestPayload tTrigger)
    {
        if(tTrigger.getSourceID().getSourceID() != GlobalTrigEventWrapper.GLOBAL_TRIGGER_SOURCE_ID.getSourceID()){
            return false;
        }

        Iterator iterTriggers;
        try {
            iterTriggers = tTrigger.getPayloads().iterator();
        } catch (IOException e) {
            log.error("Couldn't get payloads", e);
            return false;
        } catch (DataFormatException e) {
            log.error("Couldn't get payloads", e);
            return false;
        }

        //--find triggerIDs
        List listTriggerIDs = new ArrayList();
        while(iterTriggers.hasNext())
        {
            ITriggerRequestPayload tPayload = (ITriggerRequestPayload) iterTriggers.next();
            try {
                ((ILoadablePayload) tPayload).loadPayload();
            } catch (IOException e) {
                log.error("Couldn't load payload", e);
                continue;
            } catch (DataFormatException e) {
                log.error("Couldn't load payload", e);
                continue;
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
    /**
     * This is to remove any unqualified triggers before checking hasNext().
     * unqualified trigger == not-contain all trigger IDs required && lastTime < timeGate
     *
     * NOTE: This code is currently broken.  If the last two triggers in the
     * list are unqualified, the last one will not be removed.  To fix this,
     * delete the 'if (bRemoved)' block and decrement 'i' instead of
     * setting bRemoved to 'true'.
     */
    private void removeUnqualifiedTriggers()
    {
        int iTriggersInBag = payloadListInConditionalBag.size();
        boolean bRemoved = false;
        if(iTriggersInBag > 0){
            for(int i=0; i < iTriggersInBag; i++){
                if(bRemoved){
                    i--;
                    bRemoved = false;
                }else{
                    ITriggerRequestPayload tPayload = payloadListInConditionalBag.get(i);
                    IUTCTime lastTime = tPayload.getLastTimeUTC();
                    boolean containAll = containAllTriggerIDsRequired(tPayload);
                    if ((!containAll && getTimeGate().compareTo(lastTime) > 0) ||
                        (!containAll && flushing))
                    {
                        payloadListInConditionalBag.remove(i);
                        setUpdateInfo(new DummyPayload(tPayload.getFirstTimeUTC()));
                        iTriggersInBag = payloadListInConditionalBag.size();
                        bRemoved = true;
                    }
                }
            }

        }
    }

    /**
     * Find the index of the 'next' value used by hasNext() and next().
     * NOTE: Sets the internal 'nextIndex' value.
     */
    private void findNextIndex()
    {
        // remove invalid triggers before processing further.
        initUpdateInfo();
        removeUnqualifiedTriggers();

        // assume we won't find anything
        nextIndex = NEXT_NONE;

        for (int i = 0; i < payloadListInConditionalBag.size(); i++) {
            ITriggerRequestPayload trigger =
                payloadListInConditionalBag.get(i);

            // if flushing, just return true
            // otherwise check if it can be released
            if (flushing ||
                0 < getTimeGate().compareTo(trigger.getLastTimeUTC()))
            {
                nextIndex = i;
                break;
            }
        }
    }

    public void flush() {
        nextIndex = NEXT_UNKNOWN;
        flushing = true;
    }

    /**
     * this method checks whether triggers in the payload list are releasable or not
     * by comparing against timeGate.
     * @return
     */
    public synchronized boolean hasNext()
    {
        if (nextIndex == NEXT_UNKNOWN) {
            findNextIndex();
        }

        return (nextIndex != NEXT_NONE);
    }

    /**
     * This method returns releasable trigger.
     *
     * @return
     */
    public synchronized ITriggerRequestPayload next()
    {
        if (nextIndex == NEXT_UNKNOWN) {
            findNextIndex();
        }

        // save and reset next index
        int curIndex = nextIndex;
        nextIndex = NEXT_UNKNOWN;

        // if there isn't one, return null
        if (curIndex == NEXT_NONE) {
            return null;
        }

        ITriggerRequestPayload trigger =
            payloadListInConditionalBag.remove(curIndex);

        if (log.isDebugEnabled()) {
            double timeDiff = getTimeGate().timeDiff_ns(trigger.getLastTimeUTC());
            log.debug("Releasing trigger with timeDiff = " + timeDiff);
        }

        //--GTEventNumber should be assigned here.
        triggerUID++;
        getGlobalTrigEventWrapper().wrapFinalEvent(trigger, triggerUID);
        trigger = (ITriggerRequestPayload) getGlobalTrigEventWrapper().getGlobalTrigEventPayload_final();

        return trigger;
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

    private void initUpdateInfo(){
        mbNeedUpdate = false;
        mtUpdater = null;
    }
    private void setUpdateInfo(DummyPayload trigger)
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

    private List getListUnqualifiedTriggers()
    {
        return mListUnqualifiedTriggers;
    }

    public Vector getVectorPayloadsInConditonalTriggerBag()
    {
        return new Vector(payloadListInConditionalBag);
    }
    private void setVectorPayloadsInConditonalTriggerBag(Vector vecPayloads)
    {
        payloadListInConditionalBag.clear();
        payloadListInConditionalBag.addAll(vecPayloads);
    }
}
