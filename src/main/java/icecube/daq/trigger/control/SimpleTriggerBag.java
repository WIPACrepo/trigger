/*
 * class: SimpleTriggerBag
 *
 * Version $Id: SimpleTriggerBag.java,v 1.8 2005/12/29 23:17:35 toale Exp $
 *
 * Date: March 16 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.oldpayload.impl.PayloadFactory;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.trigger.monitor.PayloadBagMonitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements a collection specific to IPayload's. It is similar to
 * the original TriggerBag, except there is no merging.
 *
 *
 *   <---- t                      TimeGate
 *                                   +
 *   Newest        |----|            +
 *                         |--|      +
 *                           |-------+|
 *                              |---|+        Safe to Release
 *                                   +       |------|
 *   Oldest                          +             |---------|
 *                                   +
 *
 * @version $Id: SimpleTriggerBag.java,v 1.8 2005/12/29 23:17:35 toale Exp $
 * @author pat
 */
public class SimpleTriggerBag
        implements ITriggerBag
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(SimpleTriggerBag.class);

    /** No 'next' value is known. */
    private static final int NEXT_UNKNOWN = -1;
    /** There is no 'next' value. */
    private static final int NEXT_NONE = Integer.MIN_VALUE;

    /**
     * internal list of triggers
     */
    private List<ILoadablePayload> payloadList =
        new ArrayList<ILoadablePayload>();

    /**
     * triggers that occur earlier than this time are free to be released
     */
    private IUTCTime timeGate = new UTCTime(-1);

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
    public SimpleTriggerBag() {
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
        payloadList.add(payload);
        Collections.sort((List) payloadList, new SpliceableComparator());

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
            ILoadablePayload payload = payloadList.get(i);

            // if flushing, just return true
            // otherwise check if it can be released
            if (flushing ||
                0 < timeGate.compareTo(getPayloadTime(payload)))
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
    public synchronized ILoadablePayload next()
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

        ILoadablePayload payload = payloadList.remove(curIndex);

        if (log.isDebugEnabled()) {
            IUTCTime payTime = getPayloadTime(payload);
            double timeDiff = timeGate.timeDiff_ns(payTime);
            log.debug("Releasing payload from " + payTime +
                      " with timeDiff = " + timeDiff);
        }

        // show this output to the monitor
        monitor.recordOutput(payload);

        return payload;
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
        log.warn("This ITriggerBag does not use a PayloadFactory");
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
     * @param monitor bag monitor
     */
    public void setMonitor(PayloadBagMonitor monitor) {
        this.monitor = monitor;
    }

    private IUTCTime getPayloadTime(ILoadablePayload payload) {

        int ifType = payload.getPayloadInterfaceType();
        if (ifType == PayloadInterfaceRegistry.I_HIT_PAYLOAD) {
            return ((IHitPayload) payload).getHitTimeUTC();
        } else if (ifType == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) {
            return ((ITriggerRequestPayload) payload).getLastTimeUTC();
        } else {
            log.error("Unknown payload type: " + ifType);
            return null;
        }

    }

}
