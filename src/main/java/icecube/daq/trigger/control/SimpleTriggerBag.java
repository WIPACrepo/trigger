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

import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.monitor.PayloadBagMonitor;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.splicer.PayloadFactory;
import icecube.daq.payload.impl.UTCTime8B;

import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

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

    /**
     * internal list of triggers
     */
    private List payloadList = new ArrayList();

    /**
     * triggers that occur earlier than this time are free to be released
     */
    private IUTCTime timeGate = new UTCTime8B(-1);

    /**
     * flag to indicate we are flushing
     */
    private boolean flushing = false;

    /**
     * Payload monitor object.
     */
    private PayloadBagMonitor monitor;

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
        }

        // show this input to the monitor
        monitor.recordInput(payload);

        // add to internal list
        payloadList.add(payload);
        Collections.sort(payloadList);

        if (log.isDebugEnabled()) {
            log.debug("TriggerList has " + payloadList.size() + " payloads");
            log.debug("   TimeGate at " + timeGate.getUTCTimeAsLong());
        }

    }

    /**
     * method to flush the bag, allow all payloads to go free
     */
    public void flush() {
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
    public synchronized boolean hasNext() {

        // iterate over triggerList and check against timeGate
        for (Object aPayloadList : payloadList) {
            ILoadablePayload payload = (ILoadablePayload) aPayloadList;
            if ((flushing) ||
                    (0 < timeGate.compareTo(getPayloadTime(payload)))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get next releasable trigger
     *
     * @return the next trigger
     */
    public synchronized ILoadablePayload next() {

        // iterate over triggerList and check against timeGate
        Iterator iter = payloadList.iterator();
        while (iter.hasNext()) {
            ILoadablePayload payload = (ILoadablePayload) iter.next();
            double timeDiff = timeGate.timeDiff_ns(getPayloadTime(payload));
            if ( (flushing) ||
                 (0 < timeGate.compareTo(getPayloadTime(payload))) ) {
                iter.remove();
                if (log.isDebugEnabled()) {
                    log.debug("Releasing payload at " + getPayloadTime(payload).getUTCTimeAsLong()
                             + " with timeDiff = " + timeDiff);
                }
                // show this output to the monitor
                monitor.recordOutput(payload);

                return payload;
            }
        }

        return null;
    }

    /**
     * Set the timeGate
     *
     * @param time time to set it to
     */
    public void setTimeGate(IUTCTime time) {
        if (log.isDebugEnabled()) {
            log.debug("Updating timeGate to " + time.getUTCTimeAsLong());
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
