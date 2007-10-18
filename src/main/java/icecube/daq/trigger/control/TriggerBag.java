/*
 * class: TriggerBag
 *
 * Version $Id: TriggerBag.java 2157 2007-10-18 21:42:19Z dglo $
 *
 * Date: March 16 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.monitor.PayloadBagMonitor;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.splicer.PayloadFactory;
import icecube.daq.payload.impl.SourceID4B;
import icecube.daq.payload.impl.UTCTime8B;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.Vector;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements a collection specific to IPayload's.
 *
 *
 *   <---- t                      TimeGate
 *                                   +
 *   Newest        |----|            +
 *                         |--|      +
 *                           |-------+|
 *                              |---|+        Safe to Release
 *                         {=========+}      |------|
 *   Oldest                   Merge  +             |---------|
 *                                   +       {===============}
 *                                   +            Merge
 *
 * @version $Id: TriggerBag.java 2157 2007-10-18 21:42:19Z dglo $
 * @author pat
 */
public class TriggerBag
        implements ITriggerBag
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(TriggerBag.class);

    /** No 'next' value is known. */
    private static final int NEXT_UNKNOWN = -1;
    /** There is no 'next' value. */
    private static final int NEXT_NONE = Integer.MIN_VALUE;

    /**
     * internal list of triggers
     */
    private List payloadList = new ArrayList();

    /**
     * The factory used to create triggers
     */
    private TriggerRequestPayloadFactory triggerFactory = new TriggerRequestPayloadFactory();

    /**
     * triggers that occur earlier than this time are free to be released
     */
    private IUTCTime timeGate = new UTCTime8B(-1);

    /**
     * UID for newly merged triggers
     */
    private int triggerUID;

    /**
     * type for newly merged triggers
     */
    private int triggerType;

    /**
     * config ID for newly merged triggers
     */
    private int triggerConfigID;

    /**
     * source ID for newly merged triggers
     */
    private ISourceID triggerSourceID;

    /**
     * flag to indicate we are flushing
     */
    private boolean flushing;

    /**
     * Payload monitor object.
     */
    private PayloadBagMonitor monitor;

    /** The index of the 'next' value (can be NEXT_UNKNOWN or NEXT_NONE). */
    private int nextIndex = NEXT_UNKNOWN;

    /**
     * default constructor
     */
    public TriggerBag() {
        this(new SourceID4B(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID));
    }

    /**
     * constructor
     * @param sourceID sourceID to use
     */
    public TriggerBag(ISourceID sourceID) {
        this(-1, -1, sourceID);
    }

    /**
     * constructor
     *
     * @param type trigger type to use for merged triggers
     * @param configID trigger config ID to use for merged triggers
     * @param sourceID trigger source ID to use for merged triggers
     */
    public TriggerBag(int type, int configID, ISourceID sourceID) {
        triggerUID = 0;
        triggerType = type;
        triggerConfigID = configID;
        triggerSourceID = sourceID;

        monitor = new PayloadBagMonitor();
    }

    /**
     * Add a new payload to the bag
     * The new payload will be inserted in the proper place based on PayloadTime.
     * If it overlaps with any existing payloads, they will be merged.
     * @param payload new payload
     */
    public synchronized void add(ILoadablePayload payload) {

        try {
            payload.loadPayload();
        } catch (Exception e) {
            log.error("Error loading payload", e);
            return;
        }

        // reset 'next' index
        nextIndex = NEXT_UNKNOWN;

        // show this input to the monitor
        monitor.recordInput(payload);

        // add to internal list
        if (payloadList.isEmpty()) {

            if (log.isDebugEnabled()) {
                log.debug("Adding trigger to empty bag");
            }

            payloadList.add(payload);

        } else {

            if (log.isDebugEnabled()) {
                log.debug("Adding payload to a full bag");
            }

            List mergeList = null;

            // loop over existing triggers
            Iterator iter = payloadList.iterator();
            boolean addedPayload = false;
            while (iter.hasNext()) {
                IPayload next = (IPayload) iter.next();

                // check for overlap
                if (overlap(payload, next)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Payload overlaps with another");
                    }
                    if (mergeList == null) {
                        mergeList = new ArrayList();
                    }
                    if (!addedPayload) {
                        mergeList.add(payload);
                        addedPayload = true;
                    }
                    mergeList.add(next);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Payload does not overlap");
                    }
                }
            }

            // merge if neccessary, else add new payload to list
            if (mergeList != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Lets merge " + mergeList.size() + " payloads");
                }
                // sort to be safe
                Collections.sort(mergeList);
                merge(mergeList);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No need to merge");
                }
                payloadList.add(payload);
            }

            // sort list
            Collections.sort(payloadList);

        }

        if (log.isDebugEnabled()) {
            log.debug("TriggerList has " + payloadList.size() + " payloads");
            log.debug("   TimeGate at " + timeGate);
        }

    }

    /**
     * Find the index of the 'next' value used by hasNext() and next().
     * NOTE: Sets the internal 'nextIndex' value.
     */
    private void findNextIndex()
    {
        // assume we won't find anything
        nextIndex = NEXT_NONE;

        for (int i = 0; i < payloadList.size(); i++) {
            ITriggerRequestPayload trigger =
                (ITriggerRequestPayload) payloadList.get(i);

            // if flushing, just return true
            // otherwise check if it can be released
            if (flushing ||
                0 < timeGate.compareTo(trigger.getLastTimeUTC()))
            {
                nextIndex = i;
                break;
            }
        }
    }

    /**
     * method to flush the bag, allow all payloads to go free
     */
    public void flush() {
        nextIndex = NEXT_UNKNOWN;
        flushing = true;
    }

    /**
     * Get the timeGate
     *
     * @return timeGate
     */
    public IUTCTime getTimeGate() {
        return timeGate;
    }

    /**
     * Tests whether there is a trigger that can be released.
     *
     * the last time of the earliest trigger must be earlier than the timeGate
     *
     * @return true if there is a releasable trigger
     */
    public synchronized boolean hasNext()
    {
        if (nextIndex == NEXT_UNKNOWN) {
            findNextIndex();
        }

        return (nextIndex != NEXT_NONE);
    }

    /**
     * Get next releasable trigger
     *
     * @return the next trigger
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
            (ITriggerRequestPayload) payloadList.remove(curIndex);

        if (log.isDebugEnabled()) {
            double timeDiff = timeGate.timeDiff_ns(trigger.getLastTimeUTC());
            log.debug("Releasing trigger from " + trigger.getFirstTimeUTC() +
                      " to " + trigger.getLastTimeUTC() +
                      " with timeDiff = " + timeDiff);
        }

        // show this output to the monitor
        monitor.recordOutput(trigger);

        return trigger;
    }

    /**
     * Set the timeGate
     *
     * @param time time to set it to
     */
    public void setTimeGate(IUTCTime time) {
        if (log.isDebugEnabled()) {
            log.debug("Updating timeGate to " + time);
        }
        timeGate = time;
    }

    /**
     * Get size of internal list
     *
     * @return size of trigger bag
     */
    public int size() {
        return payloadList.size();
    }

    /**
     * set the factory for the bag
     *
     * @param payloadFactory payload factory
     */
    public void setPayloadFactory(PayloadFactory payloadFactory) {
        triggerFactory = (TriggerRequestPayloadFactory) payloadFactory;
    }

    /**
     * Get the monitor object.
     * @return the PayloadBagMonitor
     */
    public PayloadBagMonitor getMonitor() {
        return monitor;
    }

    /**
     * Set the monitor object.
     *
     * @param monitor
     */
    public void setMonitor(PayloadBagMonitor monitor) {
        this.monitor = monitor;
    }

    /**
     * checks the top-level trigger for sub-triggers
     *
     * @param trigger top-level trigger
     * @return vector of sub-triggers
     */
    private static List getSubTriggers(IPayload trigger) {

        Vector subTriggers = new Vector();
        LinkedList stack = new LinkedList();
        stack.add(trigger);

        while (!stack.isEmpty()) {

            // check trigger type to see if this is a merged trigger
            ITriggerRequestPayload next = (ITriggerRequestPayload) stack.removeFirst();
            if (0 > next.getTriggerType()) {
                // if it is, get the subPayloads and add them to the stack
                if (log.isDebugEnabled()) {
                    log.debug("  Already merged trigger: get sub-triggers");
                }
                try {
                    Vector subs = next.getPayloads();
                    if (log.isDebugEnabled()) {
                        log.debug("   Adding " + subs.size() + " triggers to stack");
                    }
                    Iterator iter = subs.iterator();
                    while (iter.hasNext()) {
                        ILoadablePayload payload =
                            (ILoadablePayload) iter.next();
                        // must load or bad things happen
                        try {
                            payload.loadPayload();
                        } catch (IOException ioe) {
                            log.error("IOException on loadPayload", ioe);
                        } catch (DataFormatException dfe) {
                            log.error("DataFormatException on loadPayload", dfe);
                        }
                        stack.add(payload);
                    }
                } catch (Exception e) {
                    log.error("Error accessing sub-payloads", e);
                }
            } else {
                // if it is not, add it to the list of subTriggers
                if (log.isDebugEnabled()) {
                    log.debug("  SubTrigger from " + next.getSourceID()
                              + " has type " + next.getTriggerType());
                    List hitList;
                    try {
                        hitList = next.getPayloads();
                    } catch (Exception e) {
                        log.error("Error getting list of hits", e);
                        hitList = null;
                    }
                    for (int i=0; hitList != null && i < hitList.size(); i++) {
                        IHitPayload hit = (IHitPayload) hitList.get(i);
                        try {
                            ((ILoadablePayload) hit).loadPayload();
                        } catch (Exception e) {
                            log.error("Error loading hit", e);
                        }
                        log.debug("    Hit " + i + ": " + hit.getHitTimeUTC());
                    }
                }
                subTriggers.add(next);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("  Returning " + subTriggers.size() + " subTriggers");
        }

        return subTriggers;

    }

    /**
     * Merge payloads
     * combine payloads in mergeList and remove them from triggerList
     */
    private void merge(List mergeList) {

        triggerUID++;

        Vector subTriggers = new Vector();

        // loop over payloads in set and find earliest and latest times
        IUTCTime earliestTime = new UTCTime8B(Long.MAX_VALUE);
        IUTCTime latestTime = new UTCTime8B(Long.MIN_VALUE);

        Iterator listIter = mergeList.iterator();
        int listCount = 0;
        while (listIter.hasNext()) {
            IPayload payload = (IPayload) listIter.next();

            int type = payload.getPayloadInterfaceType();
            if (type == PayloadInterfaceRegistry.I_HIT_PAYLOAD) {
                subTriggers.add(payload);

                IHitPayload hit = (IHitPayload) payload;
                if (0 < earliestTime.compareTo(hit.getHitTimeUTC())) {
                    earliestTime = hit.getHitTimeUTC();
                }
                // if lastTime > latestTime
                if (0 > latestTime.compareTo(hit.getHitTimeUTC())) {
                    latestTime = hit.getHitTimeUTC();
                }
            } else if (type == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) {

                if (log.isDebugEnabled()) {
                    log.debug("   Getting sub-triggers of trigger " + listCount + "...");
                }

                List realTriggers = getSubTriggers(payload);
                Iterator iter = realTriggers.iterator();
                while (iter.hasNext()) {
                    ITriggerRequestPayload trigger = (ITriggerRequestPayload) iter.next();
                    subTriggers.add(trigger);

                    if (0 < earliestTime.compareTo(trigger.getFirstTimeUTC())) {
                        earliestTime = trigger.getFirstTimeUTC();
                    }
                    // if lastTime > latestTime
                    if (0 > latestTime.compareTo(trigger.getLastTimeUTC())) {
                        latestTime = trigger.getLastTimeUTC();
                    }
                }
            }
            listCount++;

        }

        if (log.isDebugEnabled()) {
            log.debug("We have " + subTriggers.size() + " triggers to merge");
        }

        // create a readout request for the new trigger
        Vector readoutElements = new Vector();
        IReadoutRequest readoutRequest = TriggerRequestPayloadFactory.createReadoutRequest(triggerSourceID,
                                                                                           triggerUID,
                                                                                           readoutElements);

        // create the new trigger
        ITriggerRequestPayload newTrigger = (ITriggerRequestPayload) triggerFactory.createPayload(triggerUID,
                                                                                                triggerType,
                                                                                                triggerConfigID,
                                                                                                triggerSourceID,
                                                                                                earliestTime,
                                                                                                latestTime,
                                                                                                subTriggers,
                                                                                                readoutRequest);

        // remove individual triggers from triggerList and add new merged trigger
        payloadList.removeAll(mergeList);
        payloadList.add(newTrigger);
        nextIndex = NEXT_UNKNOWN;

        // recycle old subTriggers
        Iterator iter = subTriggers.iterator();
        while (iter.hasNext()) {
            ((ILoadablePayload) iter.next()).recycle();
        }
    }

    /**
     * Check for overlap between two payloads
     *
     * @param payload1 first payload
     * @param payload2 second payload
     * @return true if they overlap, false otherwise
     */
    private static boolean overlap(IPayload payload1, IPayload payload2) {

        // set times for first payload based on its type
        int type1 = payload1.getPayloadInterfaceType();
        IUTCTime startOfPayload1;
        IUTCTime endOfPayload1;

        if (type1 == PayloadInterfaceRegistry.I_HIT_PAYLOAD) {
            startOfPayload1 = ((IHitPayload) payload1).getHitTimeUTC();
            endOfPayload1   = startOfPayload1;
        } else if (type1 == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) {
            startOfPayload1 = ((ITriggerRequestPayload) payload1).getFirstTimeUTC();
            endOfPayload1   = ((ITriggerRequestPayload) payload1).getLastTimeUTC();
        } else {
            log.error("Unexpected payload type passed to TriggerBag");
            return false;
        }

        // set times for second payload based on its type
        int type2 = payload2.getPayloadInterfaceType();
        IUTCTime startOfPayload2;
        IUTCTime endOfPayload2;

        if (type2 == PayloadInterfaceRegistry.I_HIT_PAYLOAD) {
            startOfPayload2 = ((IHitPayload) payload2).getHitTimeUTC();
            endOfPayload2   = startOfPayload2;
        } else if (type2 == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) {
            startOfPayload2 = ((ITriggerRequestPayload) payload2).getFirstTimeUTC();
            endOfPayload2   = ((ITriggerRequestPayload) payload2).getLastTimeUTC();
        } else {
            log.error("Unexpected payload type passed to TriggerBag");
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug("Payload1: FirstTime = " + startOfPayload1
                      + " LastTime = " + endOfPayload1);
            log.debug("Payload2: FirstTime = " + startOfPayload2
                      + " LastTime = " + endOfPayload2);
        }

        if ( (0 < startOfPayload1.compareTo(endOfPayload2)) ||
             (0 < startOfPayload2.compareTo(endOfPayload1)) ) {
            if (log.isDebugEnabled()) {
                log.debug("  NO OVERLAP");
            }
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug("  OVERLAP!!!");
        }
        return true;

    }

}
