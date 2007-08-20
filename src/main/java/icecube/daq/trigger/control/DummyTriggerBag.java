/*
 * class: TriggerBag
 *
 * Version $Id: TriggerBag.java,v 1.8 2005/12/29 23:17:35 toale Exp $
 *
 * Date: March 16 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.trigger.impl.TriggerRequestPayload;
import icecube.daq.trigger.monitor.PayloadBagMonitor;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.splicer.PayloadFactory;
import icecube.daq.payload.impl.SourceID4B;
import icecube.daq.payload.impl.UTCTime8B;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

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
 * @version $Id: TriggerBag.java,v 1.8 2005/12/29 23:17:35 toale Exp $
 * @author pat
 */
public class DummyTriggerBag
        implements ITriggerBag
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(DummyTriggerBag.class);

    /**
     * internal list of triggers
     */
    private List payloadList = new ArrayList();

    /**
     * default constructor
     */
    public DummyTriggerBag() {
        this(-1, -1, new SourceID4B(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID));
    }

    /**
     * constructor
     * @param sourceID sourceID to use
     */
    public DummyTriggerBag(ISourceID sourceID) {
        this(-1, -1, sourceID);
    }

    /**
     * constructor
     *
     * @param type trigger type to use for merged triggers
     * @param configID trigger config ID to use for merged triggers
     * @param sourceID trigger source ID to use for merged triggers
     */
    public DummyTriggerBag(int type, int configID, ISourceID sourceID) {
    }

    /**
     * Add a new payload to the bag
     * The new payload will be inserted in the proper place based on PayloadTime.
     * If it overlaps with any existing payloads, they will be merged.
     * @param payload new payload
     */
    public synchronized void add(ILoadablePayload payload) {
        payloadList.add(payload);
    }

    /**
     * method to flush the bag, allow all payloads to go free
     */
    public void flush() {
    }

    /**
     * Get the timeGate
     *
     * @return timeGate
     */
    public IUTCTime getTimeGate() {
        return new UTCTime8B(-1);
    }

    /**
     * Tests whether there is a trigger that can be released.
     *
     * the last time of the earliest trigger must be earlier than the timeGate
     *
     * @return true if there is a releasable trigger
     */
    public synchronized boolean hasNext() {
        Iterator iter = payloadList.iterator();
        return iter.hasNext();
    }

    /**
     * Get next releasable trigger
     *
     * @return the next trigger
     */
    public synchronized TriggerRequestPayload next() {
        Iterator iter = payloadList.iterator();
        return (TriggerRequestPayload) iter.next();
    }

    /**
     * Set the timeGate
     *
     * @param time time to set it to
     */
    public void setTimeGate(IUTCTime time) {
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
    }

    /**
     * Get the monitor object.
     * @return the PayloadBagMonitor
     */
    public PayloadBagMonitor getMonitor() {
        return null;
    }

    /**
     * Set the monitor object.
     *
     * @param monitor
     */
    public void setMonitor(PayloadBagMonitor monitor) {
    }

}
