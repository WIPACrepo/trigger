/*
 * class: MultiplicityStringTrigger
 *
 * Version $Id: MultiplicityStringTrigger.java,v 1.2 2007/02/06 20:26:29 vav111 Exp $
 *
 * Date: February 6 2006
 *
 * (c) 2007 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadException;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.control.DummyPayload;
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
 * This class implements a string based trigger.  It will veto a number of doms from the top
 * of the detector.  Then it will search for a given number of hits out of a given number of doms.
 * The trigger assumes all incoming hits are on the same string.
 *
 * @version $Id: MultiplicityStringTrigger.java,v 1.2 2007/02/06 20:26:29 vav111 Exp $
 * @author vince
 */

public class MultiplicityStringTrigger extends AbstractTrigger {

    /**
     * Log object for this class
     */

    private static final Log log = LogFactory.getLog(MultiplicityStringTrigger.class);

    private static int triggerNumber = 0;

    /**
     * Trigger Parameters
     */

    /**
     * Parameters used by SimpleMajorityTrigger
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

    /**
     * Parameters used by StringTrigger algorithm
     */

    private int numberOfVetoTopDoms;
    private int maxLength;
    private int string;

    private boolean configNumberOfVetoTopDoms = false;
    private boolean configMaxLength = false;
    private boolean configString = false;

    public MultiplicityStringTrigger() {
        triggerNumber++;
    }

    /**
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
        return (configThreshold&&configTimeWindow&&configNumberOfVetoTopDoms&&configMaxLength&&configString);
    }

    /**
     * Add a parameter.
     *
     * @param parameter TriggerParameter object.
     *
     * @throws icecube.daq.trigger.exceptions.UnknownParameterException
     * @throws icecube.daq.trigger.exceptions.IllegalParameterValueException
     */

    public void addParameter(TriggerParameter parameter) throws UnknownParameterException, IllegalParameterValueException {
        if (parameter.getName().compareTo("numberOfVetoTopDoms") == 0) {
           if(Integer.parseInt(parameter.getValue())>=0) {
               numberOfVetoTopDoms = Integer.parseInt(parameter.getValue());
               configNumberOfVetoTopDoms = true;
            } else {
                throw new IllegalParameterValueException("Illegal number of exlcuded top doms value: " + parameter.getValue());
            }
        }
        else if (parameter.getName().compareTo("maxLength")==0) {
            if(Integer.parseInt(parameter.getValue())>0) {
                maxLength = Integer.parseInt(parameter.getValue());
                configMaxLength = true;
            } else
                throw new IllegalParameterValueException("Illegal max length value:" +parameter.getValue());
        }
        else if (parameter.getName().compareTo("threshold") == 0) {
            if(Integer.parseInt(parameter.getValue())>=0) {
                threshold = Integer.parseInt(parameter.getValue());
                configThreshold = true;
            }
            else {
                throw new IllegalParameterValueException("Illegal Threshold value: " + Integer.parseInt(parameter.getValue()));
            }
        }
        else if(parameter.getName().compareTo("string")==0) {
	    // Can't have this check on SPTS
	    // if(Integer.parseInt(parameter.getValue())>0&&Integer.parseInt(parameter.getValue())<=80) {
                string = Integer.parseInt(parameter.getValue());
                configString = true;
	    //            }
	    //else {
	    //    throw new IllegalParameterValueException("Illegal String value: " + Integer.parseInt(parameter.getValue()));
	    //}
        }
        else if (parameter.getName().compareTo("timeWindow") == 0) {
            if(Integer.parseInt(parameter.getValue())>=0) {
                timeWindow = Integer.parseInt(parameter.getValue());
                configTimeWindow = true;
            }
            else {
                throw new IllegalParameterValueException("Illegal timeWindow value: " + Integer.parseInt(parameter.getValue()));
            }
        }
        else {
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

    /**
     *
     * Methods of ITriggerControl
     *
     */

    /**
     * Run the trigger algorithm on a payload.
     * *Same as a SimpleMajorityTrigger*
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

	// make sure this hit is on the proper string
	int hitString = getTriggerHandler().getDOMRegistry().getStringMajor(hit.getDOMID().toString());
	if (hitString != string) {
		if(log.isDebugEnabled())
			log.debug("This hit is not on the proper string.");
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
                for (int i=0; i<slidingTimeWindow.size(); i++) {
                    IHitPayload h = (IHitPayload) slidingTimeWindow.hits.get(i);
                    if (h == null) {
                        log.error("  Hit " + i + " is null");
                    } else {
                        if (h.getPayloadTimeUTC() == null) {
                            log.error("  Hit " + i + " has a null time");
                        } else {
                            log.error("  Hit " + i + " has time = " + h.getPayloadTimeUTC());
                        }
                    }
                }
            }
            if (hitTimeUTC.compareTo(slidingTimeWindow.startTime()) < 0) {
                throw new TimeOutOfOrderException("Hit comes before start of sliding time window: Window is at "
                                                                                 + slidingTimeWindow.startTime() + " Hit is at "
                                                                                 + hitTimeUTC + " DOMId = "
                                                                                 + hit.getDOMID());
            }

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

                    if ( ( (!slidingTimeWindow.aboveThreshold()) &&
                           (!slidingTimeWindow.overlaps(hitsWithinTriggerWindow)) ) ||
                         ( (slidingTimeWindow.size() == 1) && (threshold == 1) ) ) {

                        if (log.isDebugEnabled()) {
                            log.debug("Pass trigger to analyzeTopology and reset");
                        }

                        // pass trigger and reset
                        for (int i=0; i<hitsWithinTriggerWindow.size(); i++) {
                            IHitPayload h = (IHitPayload) hitsWithinTriggerWindow.get(i);
                            if (slidingTimeWindow.contains(h)) {
                                log.error("Hit at time " + h.getPayloadTimeUTC()
                                          + " is part of new trigger but is still in SlidingTimeWindow");
                            }
                        }
                        analyzeString(hitsWithinTriggerWindow);

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
     * This method implements the string trigger criteria with a veto on all hits in a veto region.
     * @param hitsWithinTriggerWindow
     */

    public void analyzeString(LinkedList hitsWithinTriggerWindow)
        throws TriggerException
    {

        if(log.isDebugEnabled()) {
            log.debug("Counting hits which meet string trigger criteria");
        }

        Iterator iter = hitsWithinTriggerWindow.iterator();

	// Veto events that have an intime hit in the veto region
        while(iter.hasNext()) {
            IHitPayload hit = (IHitPayload) iter.next();
            int hitPosition = getTriggerHandler().getDOMRegistry().getStringMinor(hit.getDOMID().toString());
            if (hitPosition <= numberOfVetoTopDoms) {
                if (log.isDebugEnabled())
                    log.debug("The event contains a hit in the veto region, vetoing event.");
                return;
            }
        }

        Iterator iter2 = hitsWithinTriggerWindow.iterator();

        while(iter2.hasNext()) {
            IHitPayload topHit = (IHitPayload) iter2.next();
            int topPosition = getTriggerHandler().getDOMRegistry().getStringMinor(topHit.getDOMID().toString());
            int numberOfHits = 0;
            Iterator iter3 = hitsWithinTriggerWindow.iterator();

            while(iter3.hasNext()) {
                IHitPayload hit = (IHitPayload) iter3.next();
                int hitPosition = getTriggerHandler().getDOMRegistry().getStringMinor(hit.getDOMID().toString());
                if(hitPosition>=topPosition && hitPosition<(topPosition+maxLength))
                    numberOfHits++;
            }

            if(log.isDebugEnabled()) {
                log.debug("The number of hits between positions " + topPosition + " and " + (topPosition+maxLength) + " is " + numberOfHits);
            }

            if(numberOfHits>=threshold) {

                if(log.isDebugEnabled()) {
                    log.debug("The number of hits greater than the threshold.");
                    log.debug("Reporting trigger");
                }
                try {
                    formTrigger(hitsWithinTriggerWindow, null, null);
                } catch (PayloadException pe) {
                    throw new TriggerException("Cannot form trigger", pe);
                }
                return;

            } else {
                if(log.isDebugEnabled()) {
                    log.debug("The number of hits is less than the threshold");
                    log.debug("Trying next hit at topPostion");
                }
            }
        }
    }

    /**
     * Implementation of an exlcusion instead of a veto.  An exlcusion will only ignore hits in the top Dom
     * layer instead of vetoing the event.  The remaining hits below the excluded dom region must still meet
     * the remaining trigger criteria (n of m doms hits).
     */

//    public void analyzeString(LinkedList hitsWithinTriggerWindow) {
//
//        if(log.isDebugEnabled()) {
//            log.debug("Counting hits which meet string trigger criteria");
//        }
//
//        Iterator iter = hitsWithinTriggerWindow.iterator();
//
//        while(iter.hasNext()) {
//            IHitPayload topHit = (IHitPayload) iter.next();
//            int topPosition = getTriggerHandler().getDOMRegistry().getStringMinor(topHit.getDOMID().toString());
//            int numberOfHits = 0;
//            if(topPosition>numberOfVetoTopDoms) {
//                Iterator iter2 = hitsWithinTriggerWindow.iterator();
//                while(iter2.hasNext()) {
//                    IHitPayload hit = (IHitPayload) iter2.next();
//                    int hitPosition = getTriggerHandler().getDOMRegistry().getStringMinor(hit.getDOMID().toString());
//                    if(hitPosition>=topPosition && hitPosition<(topPosition+maxLength))
//                        numberOfHits++;
//                }
//
//                if(log.isDebugEnabled()) {
//                    log.debug("The number of hits between positions " + topPosition + " and " + (topPosition+maxLength) + " is " + numberOfHits);
//                }
//
//                if(numberOfHits>=threshold) {
//
//                    if(log.isDebugEnabled()) {
//                        log.debug("The number of hits greater than the threshold.");
//                        log.debug("Reporting trigger");
//                    }
//                    formTrigger(hitsWithinTriggerWindow, null, null);
//                    return;
//
//                } else {
//                    if(log.isDebugEnabled()) {
//                        log.debug("The number of hits is less than the threshold");
//                        log.debug("Trying next hit at topPostion");
//                    }
//                }
//            }
//        }
//    }

    /**
     * Flush the trigger. Basically indicates that there will be no further payloads to process
     * and no further calls to runTrigger.
     */

    public void flush() {
        // see if we're above threshold, if so form a trigger
        if (onTrigger) {
            if (log.isDebugEnabled()) {
                log.debug("Last Trigger is on, numberOfHitsInTriggerWindow = "
                          + numberOfHitsInTriggerWindow);
            }

            // pass last trigger
            try {
                analyzeString(hitsWithinTriggerWindow);
            } catch (TriggerException te) {
                log.error("Cannot evaluate last trigger", te);
            }
        }
        //see if TimeWindow has enough hits to form a trigger before an out of bounds hit
        else if(slidingTimeWindow.aboveThreshold()) {

            hitsWithinTriggerWindow.addAll(slidingTimeWindow.copy());
            numberOfHitsInTriggerWindow = hitsWithinTriggerWindow.size();

            if (log.isDebugEnabled()) {
                log.debug(" Last Trigger is now on, numberOfHitsInTriggerWindow = "
                + numberOfHitsInTriggerWindow + " triggerWindowStart = "
                + getTriggerWindowStart() + " triggerWindowStop = "
                + getTriggerWindowStop());
            }

            // pass last trigger
            try {
                analyzeString(hitsWithinTriggerWindow);
            } catch (TriggerException te) {
                log.error("Cannot evaluate last trigger", te);
            }
        }
        reset();
    }
    /*
     *Get Parameter Methods
     */

    public int getThreshold() {
        return threshold;
    }

    public int getTimeWindow() {
        return timeWindow;
    }

    public int getNumberOfVetoTopDoms() {
        return numberOfVetoTopDoms;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int getString() {
        return string;
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

    /*
     *Set Parameter Methods
     */

    public void setThreshold(int Threshold) {
        this.threshold = Threshold;
    }

    public void setTimeWindow(int timeWindow) {
        this.timeWindow = timeWindow;
    }

    public void setNumberOfVetoTopDoms(int VetoTopDoms) {
        this.numberOfVetoTopDoms = VetoTopDoms;
    }

    public void setMaxLength(int MaxLength) {
        this.maxLength = MaxLength;
    }

    public void setString(int String) {
        this.string = String;
    }

    /**
     * add a hit to the trigger time window
     * @param triggerPrimitive hit to add
     */

    private void addHitToTriggerWindow(IHitPayload triggerPrimitive) {
        hitsWithinTriggerWindow.addLast(triggerPrimitive);
        numberOfHitsInTriggerWindow = hitsWithinTriggerWindow.size();
    }

    private void reset() {

        /**
         *Reseting SimpleMajorityTrigger parameters
         */

        slidingTimeWindow = new SlidingTimeWindow();
        threshold = 0;
        timeWindow = 0;
        numberOfHitsProcessed = 0;
        hitsWithinTriggerWindow.clear();
        numberOfHitsInTriggerWindow = 0;
        configThreshold = false;
        configTimeWindow = false;

        /**
         * Resetting StringTrigger parameters
         */

        numberOfVetoTopDoms = 0;
        maxLength = 0;
        string = -1;
        configNumberOfVetoTopDoms = false;
        configMaxLength = false;
        configString = false;

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
    }

}
