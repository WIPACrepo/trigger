/*
 * class: SimpleMajorityTrigger
 *
 * Version $Id: SimpleMajorityTrigger.java 13679 2012-05-02 15:12:38Z dglo $
 *
 * Date: August 19 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TimeOutOfOrderException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements a simple multiplicty trigger.
 *
 * @version $Id: SimpleMajorityTrigger.java 13679 2012-05-02 15:12:38Z dglo $
 * @author pat
 */
public final class SimpleMajorityTrigger extends AbstractTrigger
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(SimpleMajorityTrigger.class);

    private static int triggerNumber = 0;

    /**
     * Trigger parameters
     */
    private int threshold;
    private int timeWindow;

    private int numberOfHitsProcessed = 0;

    /**
     * list of hits currently within slidingTimeWindow
     */
    private SlidingTimeWindow slidingTimeWindow = new SlidingTimeWindow();

    /**
     * list of hits in current trigger
     */
    private LinkedList hitsWithinTriggerWindow = new LinkedList();

    /**
     * number of hits in hitsWithinTriggerWindow
     */
    private int numberOfHitsInTriggerWindow;

    private boolean configThreshold = false;
    private boolean configTimeWindow = false;

    public SimpleMajorityTrigger() {
        triggerNumber++;
    }

    /*
     *
     * Methods of ITriggerConfig
     *
     */

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    public boolean isConfigured() {
        return (configThreshold && configTimeWindow);
    }

    /**
     * Add a parameter.
     *
     * @param parameter TriggerParameter object.
     *
     * @throws icecube.daq.trigger.exceptions.UnknownParameterException
     *
     */
    public void addParameter(TriggerParameter parameter) throws UnknownParameterException, IllegalParameterValueException {
        if (parameter.getName().compareTo("threshold") == 0) {
            threshold = Integer.parseInt(parameter.getValue());
            configThreshold = true;
        } else if (parameter.getName().compareTo("timeWindow") == 0) {
            timeWindow = Integer.parseInt(parameter.getValue());
            configTimeWindow = true;
        } else if (parameter.getName().compareTo("triggerPrescale") == 0) {
            triggerPrescale = Integer.parseInt(parameter.getValue());
        } else if (parameter.getName().compareTo("domSet") == 0) {
            domSetId = Integer.parseInt(parameter.getValue());
            try {
                configHitFilter(domSetId);
            } catch (ConfigException ce) {
                throw new IllegalParameterValueException("Bad DomSet #" +
                                                         domSetId);
            }
        } else {
            throw new UnknownParameterException("Unknown parameter: " + parameter.getName());
        }
        super.addParameter(parameter);
    }

    public void setTriggerName(String triggerName) {
        super.triggerName = triggerName + triggerNumber;
        if (log.isInfoEnabled()) {
            log.info("TriggerName set to " + super.triggerName);
        }
    }

    /*
    *
    * Methods of ITriggerControl
    *
    */

    /**
     * Run the trigger algorithm on a payload.
     *
     * @param payload payload to process
     *
     * @throws icecube.daq.trigger.exceptions.TriggerException
     *          if the algorithm doesn't like this payload
     */
    public void runTrigger(IPayload payload)
        throws TriggerException {

        // check that this is a hit
        int interfaceType = payload.getPayloadInterfaceType();
        if ((interfaceType != PayloadInterfaceRegistry.I_HIT_PAYLOAD) &&
            (interfaceType != PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD)) {
            throw new TriggerException("Expecting an IHitPayload");
        }
        IHitPayload hit = (IHitPayload) payload;

        // make sure spe bit is on for this hit (must have 0x02)
        int type = AbstractTrigger.getHitType(hit);
        if (type != AbstractTrigger.SPE_HIT) {
            if (log.isDebugEnabled()) {
                log.debug("Hit type is " + hit.getTriggerType() + ", returning.");
            }
            return;
        }

        // check hit filter
        if (!hitFilter.useHit(hit)) {
            if (log.isDebugEnabled()) {
                log.debug("Hit from DOM " + hit.getDOMID() + " not in DomSet");
            }
            return;
        }

        IUTCTime hitTimeUTC = hit.getHitTimeUTC();

        /*
         * Initialization for first hit
         */
        if (numberOfHitsProcessed == 0) {

            // initialize slidingTimeWindow
            slidingTimeWindow.add(hit);
            // initialize triggerWindow
            //addHitToTriggerWindow(hit);
            // initialize earliest time of interest
            setEarliestPayloadOfInterest(hit);

            if (log.isDebugEnabled()) {
                log.debug("This is the first hit, initializing...");
                log.debug("slidingTimeWindowStart set to " + slidingTimeWindow.startTime());
            }

        }
        /*
         * Add another hit
         */
        else {

            if (log.isDebugEnabled()) {
                log.debug("Processing hit at time " + hitTimeUTC);
            }

            /*
             * Check for out-of-time hits
             * hitTime < slidingTimeWindowStart
             */
            if (hitTimeUTC == null) {
                log.error("hitTimeUTC is null!!!");
            } else if (slidingTimeWindow.startTime() == null) {
                log.error("SlidingTimeWindow startTime is null!!!");
                int i = 0;
                Iterator it = slidingTimeWindow.hits.iterator();

                while ( it.hasNext() )
                {
                    i++;
                    IHitPayload h = (IHitPayload) it.next();
                    if (h == null)
                    {
                        log.error("  Hit " + i + " is null");
                    }
                    else
                    {
                        if (h.getPayloadTimeUTC() == null)
                        {
                            log.error("  Hit " + i + " has a null time");
                        }
                        else
                        {
                            log.error("  Hit " + i + " has time = " + h.getPayloadTimeUTC());
                        }
                    }
                }
            }

            if (hitTimeUTC.compareTo(slidingTimeWindow.startTime()) < 0)
                throw new TimeOutOfOrderException(
                        "Hit comes before start of sliding time window: Window is at "
                        + slidingTimeWindow.startTime() + " Hit is at "
                        + hitTimeUTC + " DOMId = "
                        + hit.getDOMID());


            /*
             * Hit falls within the slidingTimeWindow
             */
            if (slidingTimeWindow.inTimeWindow(hitTimeUTC)) {
                slidingTimeWindow.add(hit);

                // If onTrigger, add hit to list
                if (onTrigger) {
                    addHitToTriggerWindow(hit);
                }

                if (log.isDebugEnabled()) {
                    log.debug("Hit falls within slidingTimeWindow numberOfHitsInSlidingTimeWindow now equals "
                              + slidingTimeWindow.size());
                }

            }
            /*
             * Hits falls outside the slidingTimeWindow
             *   First see if we have a trigger, then slide the window
             */
            else {

                if (log.isDebugEnabled()) {
                    log.debug("Hit falls outside slidingTimeWindow, numberOfHitsInSlidingTimeWindow is "
                              + slidingTimeWindow.size());
                }

                // Do we have a trigger?
                if (slidingTimeWindow.aboveThreshold()) {

                    // onTrigger?
                    if (onTrigger) {

                        if (log.isDebugEnabled()) {
                            log.debug("Trigger is already on. Changing triggerWindowStop to "
                                      + getTriggerWindowStop());
                        }

                    } else {

                        // We now have the start of a new trigger
                        hitsWithinTriggerWindow.addAll(slidingTimeWindow.copy());
                        numberOfHitsInTriggerWindow = hitsWithinTriggerWindow.size();

                        if (log.isDebugEnabled()) {
                            log.debug("Trigger is now on, numberOfHitsInTriggerWindow = "
                                      + numberOfHitsInTriggerWindow + " triggerWindowStart = "
                                      + getTriggerWindowStart() + " triggerWindowStop = "
                                      + getTriggerWindowStop());
                        }

                    }

                    // turn trigger on
                    onTrigger = true;

                }

                if (log.isDebugEnabled()) {
                    log.debug("Now slide the window...");
                }

                // Now advance the slidingTimeWindow until the hit is inside or there is only 1 hit left in window
                while ( (!slidingTimeWindow.inTimeWindow(hitTimeUTC)) &&
                        (slidingTimeWindow.size() > 1) ) {

                    IHitPayload oldHit = slidingTimeWindow.slide();

                    // if this hit is not part of the trigger, update the earliest time of interest
                    if ( (hitsWithinTriggerWindow == null) ||
                         ((hitsWithinTriggerWindow != null) && (!hitsWithinTriggerWindow.contains(oldHit))) ) {
                        IPayload oldHitPlus = new DummyPayload(oldHit.getHitTimeUTC().getOffsetUTCTime(0.1));
                        setEarliestPayloadOfInterest(oldHitPlus);
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("numberOfHitsInSlidingTimeWindow is now " + slidingTimeWindow.size()
                                  + " slidingTimeWindowStart is now " + slidingTimeWindow.startTime());
                    }

                }

                if (log.isDebugEnabled()) {
                    log.debug("Done sliding");
                }

                /*
                 * Does it fall in new slidingTimeWindow?
                 *  if not, it defines the start of a new slidingTimeWindow
                 */
                if (!slidingTimeWindow.inTimeWindow(hitTimeUTC)) {

                    IHitPayload oldHit = slidingTimeWindow.slide();

                    if ( (hitsWithinTriggerWindow == null) ||
                         ((hitsWithinTriggerWindow != null) && (!hitsWithinTriggerWindow.contains(oldHit))) ) {
                        IPayload oldHitPlus = new DummyPayload(oldHit.getHitTimeUTC().getOffsetUTCTime(0.1));
                        setEarliestPayloadOfInterest(oldHitPlus);
                    }

                    slidingTimeWindow.add(hit);

                    if (log.isDebugEnabled()) {
                        log.debug("Hit still outside slidingTimeWindow, start a new one "
                                  + "numberOfHitsInSlidingTimeWindow = " + slidingTimeWindow.size()
                                  + " slidingTimeWindowStart = " + slidingTimeWindow.startTime());
                    }

                }
                // if so, add it to window
                else {
                    slidingTimeWindow.add(hit);

                    if (log.isDebugEnabled()) {
                        log.debug("Hit is now in slidingTimeWindow numberOfHitsInSlidingTimeWindow = "
                                  + slidingTimeWindow.size());
                    }

                }

                /*
                 * If onTrigger, check for finished trigger
                 */
                if (onTrigger) {

                    if (log.isDebugEnabled()) {
                        log.debug("Trigger is on, numberOfHitsInSlidingTimeWindow = "
                                  + slidingTimeWindow.size());
                    }

                    // todo - also need to check if any hits still in STW are also in TW
                    if ( ( (!slidingTimeWindow.aboveThreshold()) &&
                           (!slidingTimeWindow.overlaps(hitsWithinTriggerWindow)) ) ||
                         ( (slidingTimeWindow.size() == 1) && (threshold == 1) ) ) {

                        if (log.isDebugEnabled()) {
                            log.debug("Report trigger and reset");
                        }

                        // form trigger and reset
                        for (int i=0; i<hitsWithinTriggerWindow.size(); i++) {
                            IHitPayload h = (IHitPayload) hitsWithinTriggerWindow.get(i);
                            if (slidingTimeWindow.contains(h)) {
                                log.error("Hit at time " + h.getPayloadTimeUTC()
                                          + " is part of new trigger but is still in SlidingTimeWindow");
                            }
                        }
                        formTrigger(hitsWithinTriggerWindow, null, null);

                        onTrigger = false;
                        hitsWithinTriggerWindow.clear();
                        numberOfHitsInTriggerWindow = 0;

                    } else {
                        addHitToTriggerWindow(hit);

                        if (log.isDebugEnabled()) {
                            log.debug("Still on trigger, numberOfHitsInTriggerWindow = "
                                      + numberOfHitsInTriggerWindow);
                        }


                    }

                }

            }

        }

        numberOfHitsProcessed++;

    }

    /**
     * Flush the trigger. Basically indicates that there will be no further payloads to process
     * and no further calls to runTrigger.
     */
    public void flush() {
        boolean formLast = true;

        // see if we're above threshold, if so form a trigger
        if (onTrigger) {
            if (log.isDebugEnabled()) {
                log.debug("Last Trigger is on, numberOfHitsInTriggerWindow = "
                          + numberOfHitsInTriggerWindow);
            }
        }
        //see if TimeWindow has enough hits to form a trigger before an out of bounds hit
        else if(slidingTimeWindow.aboveThreshold()) {

            hitsWithinTriggerWindow.addAll(slidingTimeWindow.copy());
            numberOfHitsInTriggerWindow = hitsWithinTriggerWindow.size();

            if (log.isDebugEnabled()) {
                log.debug(" Last Trigger is now on, numberOfHitsInTriggerWindow = "
                + numberOfHitsInTriggerWindow
                + (numberOfHitsInTriggerWindow == 0 ? "" :
                   " triggerWindowStart = "
                   + getTriggerWindowStart() + " triggerWindowStop = "
                   + getTriggerWindowStop()));
            }
        }
        else {
            formLast = false;
        }

        // form last trigger
        if (hitsWithinTriggerWindow.size() > 0 && formLast) {
            formTrigger(hitsWithinTriggerWindow, null, null);
        }

        // todo: Pat is this right?
        reset();
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(int timeWindow) {
        this.timeWindow = timeWindow;
    }

    /**
     * add a hit to the trigger time window
     * @param triggerPrimitive hit to add
     */
    private void addHitToTriggerWindow(IHitPayload triggerPrimitive) {
        hitsWithinTriggerWindow.addLast(triggerPrimitive);
        numberOfHitsInTriggerWindow = hitsWithinTriggerWindow.size();
    }

    /**
     * getter for triggerWindowStart
     * @return start of trigger window
     */
    private IUTCTime getTriggerWindowStart() {
        return ((IHitPayload) hitsWithinTriggerWindow.getFirst()).getHitTimeUTC();
    }

    /**
     * getter for triggerWindowStop
     * @return stop of trigger window
     */
    private IUTCTime getTriggerWindowStop() {
        return ((IHitPayload) hitsWithinTriggerWindow.getLast()).getHitTimeUTC();
    }

    public int getNumberOfHitsWithinSlidingTimeWindow() {
        return slidingTimeWindow.size();
    }

    public int getNumberOfHitsWithinTriggerWindow() {
        return hitsWithinTriggerWindow.size();
    }

    // todo: Pat is this right?
    private void reset(){
        slidingTimeWindow = new SlidingTimeWindow();
        threshold = 0;
        timeWindow = 0;
        numberOfHitsProcessed = 0;
        hitsWithinTriggerWindow.clear();
        numberOfHitsInTriggerWindow = 0;
        configThreshold = false;
        configTimeWindow = false;
    }

    private final class SlidingTimeWindow {

        private LinkedList hits;

        private SlidingTimeWindow() {
            hits = new LinkedList();
        }

        private void add(IHitPayload hit) {
            hits.addLast(hit);
        }

        private int size() {
            return hits.size();
        }

        private IHitPayload slide() {
            return (IHitPayload) hits.removeFirst();
        }

        private IUTCTime startTime() {
            return ((IHitPayload) hits.getFirst()).getHitTimeUTC();
        }

        private IUTCTime endTime() {
            return (startTime().getOffsetUTCTime((double) timeWindow));
        }

        private boolean inTimeWindow(IUTCTime hitTime) {
            return (hitTime.compareTo(startTime()) >= 0) &&
                   (hitTime.compareTo(endTime()) <= 0);
        }

        private LinkedList copy() {
            return (LinkedList) hits.clone();
        }

        private boolean contains(Object object) {
            return hits.contains(object);
        }

        private boolean overlaps(List list) {
            Iterator iter = list.iterator();
            while (iter.hasNext()) {
                if (contains(iter.next())) {
                    return true;
                }
            }
            return false;
        }

        private boolean aboveThreshold() {
            return (hits.size() >= threshold);
        }

        public String toString()
        {
            if (hits.size() == 0) {
                return "Window[]*0";
            }

            return "Window[" + startTime() + "-" + endTime() + "]*" +
                hits.size();
        }
    }

}
