/*
 * class: AMajorityTrigger
 *
 * Version $Id: AMajorityTrigger.java 11-02-2011 alb $
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
 * This class implements a multiplicty trigger.
 *
 * @version $Id: AMajorityTrigger.java $
 * @author alb
 */
public final class AMajorityTrigger extends AbstractTrigger
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(AMajorityTrigger.class);

    private static int triggerNumber = 0;

    /**
     * Trigger parameters
     */
    private int threshold;
    private int timeWindow;
    private int maxNumberOfHitsInTW;
    private int numberOfHitsProcessed = 0;

    /**
     * list of hits currently within slidingTimeWindow
     */
    private BasketTimeWindow basketTimeWindow = new BasketTimeWindow();

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
    private boolean configMaxNumberOfHitsInTW = false;

    public AMajorityTrigger() {
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
        } else if (parameter.getName().compareTo("maxNumberOfHitsInTW") == 0) {  
            maxNumberOfHitsInTW = Integer.parseInt(parameter.getValue());
            configMaxNumberOfHitsInTW = true;                                   
	} else if (parameter.getName().compareTo("triggerPrescale") == 0) {
            triggerPrescale = Integer.parseInt(parameter.getValue());
        } else if (parameter.getName().compareTo("domSet") == 0) {
            domSetId = Integer.parseInt(parameter.getValue());
            configHitFilter(domSetId);
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

            basketTimeWindow.add(hit);
	    setEarliestPayloadOfInterest(hit);

            if (log.isDebugEnabled()) {
                log.debug("This is the first hit, initializing...");
                log.debug("basketTimeWindowStart set to " + basketTimeWindow.startTime());
            }

        }
        /*
         * Add another hit
         */
        else {

            if (log.isDebugEnabled()){log.debug("Processing hit at time " + hitTimeUTC);}

            /*
             * Check for out-of-time hits
             * hitTime < basketTimeWindowStart
             */
            if (hitTimeUTC == null) {
                log.error("hitTimeUTC is null!!!");
            } else if (basketTimeWindow.startTime() == null) {
                log.error("BasektTimeWindow startTime is null!!!");
                int i = 0;
                Iterator it = basketTimeWindow.hits.iterator();

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

            if (hitTimeUTC.compareTo(basketTimeWindow.startTime()) < 0)
                throw new TimeOutOfOrderException(
                        "Hit comes before start of basket time window: Window is at "
                        + basketTimeWindow.startTime() + " Hit is at "
                        + hitTimeUTC + " DOMId = "
                        + hit.getDOMID());


            /*
             * Hit falls within the basketTimeWindow
             */

	    //////////////////////////////////////////////////////////////////////////////////////////2.1/////////start
            if (basketTimeWindow.inTimeWindow(hitTimeUTC)) {

                basketTimeWindow.addt(hit);

                // If onTrigger, add hit to list
                if (onTrigger) {
                    addHitToTriggerWindow(hit);
                } else {
		    if(basketTimeWindow.aboveThreshold()) {
			hitsWithinTriggerWindow.addAll(basketTimeWindow.copy());
			numberOfHitsInTriggerWindow = hitsWithinTriggerWindow.size();
			onTrigger=true;
		    }
		}

                if (log.isDebugEnabled()) {
                    log.debug("Hit falls within basketTimeWindow numberOfHitsInBasketTimeWindow now equals "
                              + basketTimeWindow.size());
                }

            }
	    //////////////////////////////////////////////////////////////////////////////////////////2.1///////////end

            /*
             * Hits falls outside the basketTimeWindow
             *   First see if we have a trigger, then slide the window
             */
            else {

                if (log.isDebugEnabled()) {
                    log.debug("Hit falls outside basketgTimeWindow, numberOfHitsInBasketTimeWindow is "
                              + basketTimeWindow.size());
                }

		if (log.isDebugEnabled()) {
                    log.debug("Now slide the window...");
                }

                // Now advance the basketTimeWindow until the hit is inside or there is only 1 hit left in window
                while ( (!basketTimeWindow.inTimeWindow(hitTimeUTC)) &&
                        (basketTimeWindow.size() > 1) ) {

                    IHitPayload oldHit = basketTimeWindow.slide();

                    // if this hit is not part of the trigger, update the earliest time of interest
                    if (!onTrigger) {
                        IPayload oldHitPlus = new DummyPayload(oldHit.getHitTimeUTC().getOffsetUTCTime(0.1));
                        setEarliestPayloadOfInterest(oldHitPlus);
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("numberOfHitsInBasketTimeWindow is now " + basketTimeWindow.size()
                                  + " basketTimeWindowStart is now " + basketTimeWindow.startTime());
                    }

                }

                if (log.isDebugEnabled()) {
                    log.debug("Done sliding");
                }

                /*
                 * Does it fall in new basketTimeWindow?
                 *  if not, it defines the start of a new basketTimeWindow
                 */
                if (!basketTimeWindow.inTimeWindow(hitTimeUTC)) {

                    IHitPayload oldHit = basketTimeWindow.slide();

                    if ( !onTrigger ) {
                        IPayload oldHitPlus = new DummyPayload(oldHit.getHitTimeUTC().getOffsetUTCTime(0.1));
                        setEarliestPayloadOfInterest(oldHitPlus);
                    }

                    basketTimeWindow.addt(hit);

                    if (log.isDebugEnabled()) {
                        log.debug("Hit still outside basketTimeWindow, start a new one "
                                  + "numberOfHitsInBasketTimeWindow = " + basketTimeWindow.size()
                                  + " basketTimeWindowStart = " + basketTimeWindow.startTime());
                    }

                }
                // if so, add it to window
                else {
                    basketTimeWindow.addt(hit);

                    if (log.isDebugEnabled()) {
                        log.debug("Hit is now in basketTimeWindow numberOfHitsInBasketTimeWindow = "
                                  + basketTimeWindow.size());
                    }

                }

                /*
                 * If onTrigger, check for finished trigger
                 */
                if (onTrigger) {
		    
                    if (log.isDebugEnabled()) { log.debug("Trigger is on, numberOfHitsInBasketTimeWindow = " + basketTimeWindow.size()); }

                    if ( ( (!basketTimeWindow.aboveThreshold()) && (!basketTimeWindow.overlaps(hitsWithinTriggerWindow)) ) ||
                         ( (basketTimeWindow.size() == 1) && (threshold == 1) ) ) {
			
                        if (log.isDebugEnabled()) { log.debug("Report trigger and reset"); }
			
                        // form trigger and reset
                        for (int i=0; i<hitsWithinTriggerWindow.size(); i++) {
                            IHitPayload h = (IHitPayload) hitsWithinTriggerWindow.get(i);
                            if (basketTimeWindow.contains(h)) {
                                log.error("Hit at time " + h.getPayloadTimeUTC() + " is part of new trigger but is still in BasketTimeWindow");
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
        else if(basketTimeWindow.aboveThreshold()) {

            hitsWithinTriggerWindow.addAll(basketTimeWindow.copy());
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
        numberOfHitsInTriggerWindow = hitsWithinTriggerWindow.size();
	if(numberOfHitsInTriggerWindow>maxNumberOfHitsInTW){
	    hitsWithinTriggerWindow.removeLast();
	}
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

    public int getNumberOfHitsWithinBasketTimeWindow() {
        return basketTimeWindow.size();
    }

    public int getNumberOfHitsWithinTriggerWindow() {
        return hitsWithinTriggerWindow.size();
    }

    // todo: Pat is this right?
    private void reset(){
        basketTimeWindow = new BasketTimeWindow();
        threshold = 0;
        timeWindow = 0;
        numberOfHitsProcessed = 0;
        hitsWithinTriggerWindow.clear();
        numberOfHitsInTriggerWindow = 0;
        configThreshold = false;
        configTimeWindow = false;
    }

    private final class BasketTimeWindow {

        private LinkedList hits;

        private BasketTimeWindow() {
            hits = new LinkedList();
        }

        private void add(IHitPayload hit) {
            hits.addLast(hit);
        }

	private void addt(IHitPayload hit) {
            hits.addLast(hit);
	    if(hits.size() > threshold) hits.removeFirst();
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

	private boolean gThreshold() {
            return (hits.size() > threshold);
        }


    }

}
