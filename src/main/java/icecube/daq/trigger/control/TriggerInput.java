/*
 * class: TriggerInput
 *
 * Version $Id: TriggerInput.java,v 1.4 2005/10/18 19:18:06 toale Exp $
 *
 * Date: May 2 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.IHitDataPayload;
import icecube.daq.trigger.ICompositePayload;
import icecube.daq.trigger.ITriggerPayload;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.eventbuilder.IEventPayload;
import icecube.daq.eventbuilder.IReadoutDataPayload;

import java.util.List;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides a simple implementation of ITriggerInput
 *
 * @version $Id: TriggerInput.java,v 1.4 2005/10/18 19:18:06 toale Exp $
 * @author pat
 */
public class TriggerInput
        implements ITriggerInput
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(TriggerInput.class);

    /**
     * internal list of payloads
     */
    private List inputList;

    private int count;

    /**
     * flag to indicate if we should flush
     */
    private boolean flushing;

    /**
     * default constructor
     */
    public TriggerInput() {
        inputList = new ArrayList();
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

        PayloadWindow newWindow = new PayloadWindow(payload);
        IUTCTime currentTime = newWindow.firstTime;
        inputList.add(newWindow);
        count++;

        // update containment
        for (int i=0; i<inputList.size(); i++) {
            PayloadWindow window = (PayloadWindow) inputList.get(i);

            // if window was already contained it is still contained
            if (!window.isContained()) {

                // see if it is now contained
                if (0 < currentTime.compareTo(window.lastTime)) {
                    window.setContained(true);
                }

            }
        }

        // now check for overlaps
        for (int i=0; i<inputList.size(); i++) {
            PayloadWindow window1 = (PayloadWindow) inputList.get(i);
            // for each contained window...
            if (window1.isContained()) {

                window1.setOverlapping(false);

                for (int j=i+1; j<inputList.size(); j++) {
                    PayloadWindow window2 = (PayloadWindow) inputList.get(j);
                    // check ovelaps with all uncontained windows
                    if (!window2.isContained()) {

                        if ( (0 >= window1.firstTime.compareTo(window2.lastTime)) &&
                             (0 <= window1.lastTime.compareTo(window2.firstTime)) ) {
                            window1.setOverlapping(true);
                        }

                    }

                }
            }
        }

    }

    /**
     * allow all payloads to be sucked out
     */
    public void flush() {
        flushing = true;
        log.info("Flushing: Total count = " + count);
    }

    /**
     * check if there is a payload which is fully contained and does not overlap with
     * payloads that are not contained
     *
     * @return true if there is, false otherwise
     */
    public boolean hasNext() {

        // loop over all payloads in list
        for (int i=0; i<inputList.size(); i++) {
            PayloadWindow window = (PayloadWindow) inputList.get(i);
            // if flushing, just return true
            // otherwise check if it is free to go
            if ( (flushing) ||
                 (window.isContained() && !window.isOverlapping()) ) {
                return true;
            }
        }

        return false;
    }

    /**
     * get next available payload
     * @return next ILoadablePayload
     */
    public ILoadablePayload next() {

        // loop over all payloads in list
        for (int i=0; i<inputList.size(); i++) {
            PayloadWindow window = (PayloadWindow) inputList.get(i);
            // if flushing, just return the payload
            // otherwise check if it is free to go
            if ( (flushing) ||
                 (window.isContained() && !window.isOverlapping()) ) {
                inputList.remove(i);
                return window.getPayload();
            }
        }

        return null;
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
    private class PayloadWindow {

        private ILoadablePayload payload;
        public IUTCTime firstTime;
        private IUTCTime lastTime;
        private boolean overlapping;
        private boolean contained;

        private PayloadWindow(ILoadablePayload payload) {
            this.payload = payload;

            // get firstTime and lastTime from appropriate interface
            switch (payload.getPayloadInterfaceType()) {
                case PayloadInterfaceRegistry.I_PAYLOAD :
                    firstTime = payload.getPayloadTimeUTC();
                    lastTime = firstTime;
                    break;
                case PayloadInterfaceRegistry.I_TRIGGER_PAYLOAD :
                    firstTime = ((ITriggerPayload) payload).getPayloadTimeUTC();
                    lastTime = firstTime;
                    break;
                case PayloadInterfaceRegistry.I_HIT_PAYLOAD :
                    firstTime = ((IHitPayload) payload).getHitTimeUTC();
                    lastTime = firstTime;
                    break;
                case PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD :
                    firstTime = ((IHitDataPayload) payload).getHitTimeUTC();
                    lastTime = firstTime;
                    break;
                case PayloadInterfaceRegistry.I_COMPOSITE_PAYLOAD :
                    firstTime = ((ICompositePayload) payload).getFirstTimeUTC();
                    lastTime = ((ICompositePayload) payload).getLastTimeUTC();
                    break;
                case PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD :
                    firstTime = ((ITriggerRequestPayload) payload).getFirstTimeUTC();
                    lastTime = ((ITriggerRequestPayload) payload).getLastTimeUTC();
                    break;
                case PayloadInterfaceRegistry.I_READOUT_REQUEST_PAYLOAD :
                    firstTime = null;
                    lastTime = null;
                    break;
                case PayloadInterfaceRegistry.I_READOUT_DATA_PAYLOAD :
                    firstTime = ((IReadoutDataPayload) payload).getFirstTimeUTC();
                    lastTime = ((IReadoutDataPayload) payload).getLastTimeUTC();
                    break;
                case PayloadInterfaceRegistry.I_EVENT_PAYLOAD :
                    firstTime = ((IEventPayload) payload).getFirstTimeUTC();
                    lastTime = ((IEventPayload) payload).getLastTimeUTC();
                    break;
                default :
                    firstTime = null;
                    lastTime = null;
                    break;
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

    }

}
