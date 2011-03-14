/*
 * class: TriggerInput
 *
 * Version $Id: TriggerInput.java 12778 2011-03-14 22:43:46Z dglo $
 *
 * Date: May 2 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.payload.ICompositePayload;
import icecube.daq.payload.IEventPayload;
import icecube.daq.payload.IHitDataPayload;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IReadoutDataPayload;
import icecube.daq.payload.ITriggerPayload;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides a simple implementation of ITriggerInput
 *
 * @version $Id: TriggerInput.java 12778 2011-03-14 22:43:46Z dglo $
 * @author pat
 */
public class TriggerInput
    implements ITriggerInput, Iterable, Iterator
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(TriggerInput.class);

    /** No 'next' value is known. */
    private static final int NEXT_UNKNOWN = -1;
    /** There is no 'next' value. */
    private static final int NEXT_NONE = Integer.MIN_VALUE;

    /**
     * internal list of payloads
     */
    private List<PayloadWindow> inputList;

    private int count;

    /**
     * flag to indicate if we should flush
     */
    private boolean flushing;

    /** The index of the 'next' value (can be NEXT_UNKNOWN or NEXT_NONE). */
    private int nextIndex = NEXT_UNKNOWN;

    /**
     * default constructor
     */
    public TriggerInput() {
        inputList = new ArrayList<PayloadWindow>();
    }

    /**
     * add a new payload to the list
     * @param payload
     */
    public void addPayload(ILoadablePayload payload) {

        // Load the payload
        try {
            payload.loadPayload();
        } catch (IOException ioe) {
            log.error("IO exception while loading payload", ioe);
            return;
        } catch (DataFormatException dfe) {
            log.error("Data format exception while loading payload", dfe);
            return;
        }

        // reset 'next' index
        nextIndex = NEXT_UNKNOWN;

        PayloadWindow newWindow = new PayloadWindow(payload);
        long currentTime = newWindow.firstTime;

        // update containment
        for (PayloadWindow window : inputList) {
            // if window was already contained it is still contained
            if (!window.isContained()) {

                // see if it is now contained
                if (currentTime > window.lastTime) {
                    window.setContained(true);
                }
            }
        }

        // add new window
        inputList.add(newWindow);
        count++;

        // now check for overlaps
        for (int i=0; i<inputList.size(); i++) {
            PayloadWindow window1 = inputList.get(i);
            // for each contained window...
            if (window1.isContained()) {

                window1.setOverlapping(false);

                for (int j=0; j<inputList.size(); j++) {
                    PayloadWindow window2 = inputList.get(j);
                    // check overlaps with all uncontained windows
                    if (!window2.isContained()) {

                        if (window1.firstTime <= window2.lastTime &&
                            window1.lastTime >= window2.firstTime)
                        {
                            window1.setOverlapping(true);
                            break;
                        }
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
        // assume we won't find anything
        nextIndex = NEXT_NONE;

        for (int i=0; i<inputList.size(); i++) {
            PayloadWindow window = inputList.get(i);

            // if flushing, just return true
            // otherwise check if it is free to go
            if (flushing || (window.isContained() && !window.isOverlapping()))
            {
                nextIndex = i;
                break;
            }
        }
    }

    /**
     * allow all payloads to be sucked out
     */
    public void flush() {
        nextIndex = NEXT_UNKNOWN;
        flushing = true;
        if (log.isInfoEnabled()) {
            log.info("Flushing: Total count = " + count);
        }
    }

    /**
     * check if there is a payload which is fully contained and does not
     * overlap with payloads that are not contained
     *
     * @return true if there is, false otherwise
     */
    public synchronized boolean hasNext()
    {
        if (nextIndex == NEXT_UNKNOWN) {
            findNextIndex();
        }

        return (nextIndex != NEXT_NONE);
    }

    /**
     * Return current object to be used as an iterator.
     *
     * @return <tt>this</tt>
     */
    public Iterator<ILoadablePayload> iterator()
    {
        return this;
    }

    /**
     * get next available payload
     * @return next ILoadablePayload
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

        return inputList.remove(curIndex).getPayload();
    }

    public void remove()
    {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     * get size of list
     * @return number of payloads in list
     */
    public int size() {
        return inputList.size();
    }

    /**
     * private inner class to manage time windows and flags
     */
    private final class PayloadWindow {

        private ILoadablePayload payload;
        private long firstTime;
        private long lastTime;
        private boolean overlapping;
        private boolean contained;

        private PayloadWindow(ILoadablePayload payload) {
            this.payload = payload;

            // get firstTime and lastTime from appropriate interface
            IUTCTime firstUTC;
            IUTCTime lastUTC;
            switch (payload.getPayloadInterfaceType()) {
                case PayloadInterfaceRegistry.I_PAYLOAD :
                    firstUTC = payload.getPayloadTimeUTC();
                    lastUTC = firstUTC;
                    break;
                case PayloadInterfaceRegistry.I_TRIGGER_PAYLOAD :
                    firstUTC = ((ITriggerPayload) payload).getPayloadTimeUTC();
                    lastUTC = firstUTC;
                    break;
                case PayloadInterfaceRegistry.I_HIT_PAYLOAD :
                    firstUTC = ((IHitPayload) payload).getHitTimeUTC();
                    lastUTC = firstUTC;
                    break;
                case PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD :
                    firstUTC = ((IHitDataPayload) payload).getHitTimeUTC();
                    lastUTC = firstUTC;
                    break;
                case PayloadInterfaceRegistry.I_COMPOSITE_PAYLOAD :
                    firstUTC = ((ICompositePayload) payload).getFirstTimeUTC();
                    lastUTC = ((ICompositePayload) payload).getLastTimeUTC();
                    break;
                case PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD :
                    firstUTC = ((ITriggerRequestPayload) payload).getFirstTimeUTC();
                    lastUTC = ((ITriggerRequestPayload) payload).getLastTimeUTC();
                    break;
                case PayloadInterfaceRegistry.I_READOUT_REQUEST_PAYLOAD :
                    firstUTC = null;
                    lastUTC = null;
                    break;
                case PayloadInterfaceRegistry.I_READOUT_DATA_PAYLOAD :
                    firstUTC = ((IReadoutDataPayload) payload).getFirstTimeUTC();
                    lastUTC = ((IReadoutDataPayload) payload).getLastTimeUTC();
                    break;
                case PayloadInterfaceRegistry.I_EVENT_PAYLOAD :
                    firstUTC = ((IEventPayload) payload).getFirstTimeUTC();
                    lastUTC = ((IEventPayload) payload).getLastTimeUTC();
                    break;
                default :
                    log.error("Unexpected interface type #" +
                              payload.getPayloadInterfaceType());
                    firstUTC = null;
                    lastUTC = null;
                    break;
            }

            if (firstUTC == null) {
                firstTime = -1L;
            } else {
                firstTime = firstUTC.longValue();
            }
            if (lastUTC == null) {
                lastTime = -1L;
            } else {
                lastTime = lastUTC.longValue();
            }
        }

        private ILoadablePayload getPayload() {
            return payload;
        }

        private boolean isContained() {
            return contained;
        }

        private boolean isOverlapping() {
            return overlapping;
        }

        private void setContained(boolean contained) {
            this.contained = contained;
        }

        private void setOverlapping(boolean overlapping) {
            this.overlapping = overlapping;
        }

        public String toString()
        {
            return "PayloadWindow[" + firstTime + "," + lastTime + "]" +
                (overlapping ? ",overlapping" : "") +
                (contained ? ",contained" : "");
        }
    }

}
