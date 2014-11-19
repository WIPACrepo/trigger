/*
 * class: IniceVolumeTrigger
 *
 * Version $Id: IniceVolumeTrigger.java,v 1.2 2007/02/06 20:26:29 vav111 Exp $
 *
 * Date: February 6 2006
 *
 * (c) 2007 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.control.StringMap;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TimeOutOfOrderException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.util.DeployedDOM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements a simple topological trigger.
 * Then it will search for a given number of hits in 1) a configurable time window
 * and 2) in a volume composed of 7 strings with a hieght of some number of doms.
 *
 * @version $Id: IniceVolumeTrigger.java,v 1.2 2007/02/06 20:26:29 vav111 Exp $
 * @author vince
 */

public class IniceVolumeTrigger extends AbstractTrigger {

    /**
     * Log object for this class
     */

    private static final Log LOG =
        LogFactory.getLog(IniceVolumeTrigger.class);

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
     * Parameters used by VolumeTrigger algorithm
     */

    private int volumeHeight;
    private int centerShift;

    private boolean configVolumeHeight = false;
    private boolean configCenterShift = false;

    private StringMap stringMap;

    public IniceVolumeTrigger()
    {
        triggerNumber++;
	stringMap = StringMap.getInstance();
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

    public boolean isConfigured()
    {
        return (configThreshold&&configTimeWindow&&configVolumeHeight&&configCenterShift);
    }

    /**
     * Add a trigger parameter.
     *
     * @param name parameter name
     * @param value parameter value
     *
     * @throws UnknownParameterException if the parameter is unknown
     * @throws IllegalParameterValueException if the parameter value is bad
     */
    public void addParameter(String name, String value)
        throws UnknownParameterException, IllegalParameterValueException
    {
        if (name.compareTo("volumeHeight") == 0) {
           if(Integer.parseInt(value)>=0) {
               volumeHeight = Integer.parseInt(value);
               configVolumeHeight = true;
            } else {
                throw new IllegalParameterValueException("Illegal number of exlcuded top doms value: " + value);
            }
        }
        else if (name.compareTo("centerShift")==0) {
            //if(Integer.parseInt(value)>0) {
                centerShift = Integer.parseInt(value);
                configCenterShift = true;
		//} else
                //throw new IllegalParameterValueException("Illegal max length value:" +value);
        }
        else if (name.compareTo("threshold") == 0) {
            if(Integer.parseInt(value)>=0) {
                threshold = Integer.parseInt(value);
                configThreshold = true;
            }
            else {
                throw new IllegalParameterValueException("Illegal Threshold value: " + Integer.parseInt(value));
            }
        }
        else if (name.compareTo("timeWindow") == 0) {
            if(Integer.parseInt(value)>=0) {
                timeWindow = Integer.parseInt(value);
                configTimeWindow = true;
            }
            else {
                throw new IllegalParameterValueException("Illegal timeWindow value: " + Integer.parseInt(value));
            }
        }
        else {
            throw new UnknownParameterException("Unknown parameter: " + name);
        }
        super.addParameter(name, value);
    }

    public void setTriggerName(String triggerName)
    {
        super.triggerName = triggerName + triggerNumber;
        if (LOG.isInfoEnabled()) {
            LOG.info("TriggerName set to " + super.triggerName);
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
            throws TriggerException
        {

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
            if (LOG.isDebugEnabled()) {
                LOG.debug("Hit type is " + hit.getTriggerType() + ", returning.");
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

            if (LOG.isDebugEnabled()) {
                LOG.debug("This is the first hit, initializing...");
                LOG.debug("slidingTimeWindowStart set to " + slidingTimeWindow.startTime());
            }

        }
        /*
         * Add another hit
         */
        else {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Processing hit at time " + hitTimeUTC);
            }

            /*
             * Check for out-of-time hits
             * hitTime < slidingTimeWindowStart
             */
            if (hitTimeUTC == null) {
                throw new TriggerException("hitTimeUTC was null");
            } else if (slidingTimeWindow.startTime() == null) {
                LOG.error("SlidingTimeWindow startTime is null!!!");
                for (int i=0; i<slidingTimeWindow.size(); i++) {
                    IHitPayload h = (IHitPayload) slidingTimeWindow.hits.get(i);
                    if (h == null) {
                        LOG.error("  Hit " + i + " is null");
                    } else {
                        if (h.getPayloadTimeUTC() == null) {
                            LOG.error("  Hit " + i + " has a null time");
                        } else {
                            LOG.error("  Hit " + i + " has time = " + h.getPayloadTimeUTC());
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

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Hit falls within slidingTimeWindow numberOfHitsInSlidingTimeWindow now equals "
                              + slidingTimeWindow.size());
                }

            }
            /*
             * Hits falls outside the slidingTimeWindow
             *   First see if we have a trigger, then slide the window
             */
            else {

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Hit falls outside slidingTimeWindow, numberOfHitsInSlidingTimeWindow is "
                              + slidingTimeWindow.size());
                }

                // Do we have a trigger?
                if (slidingTimeWindow.aboveThreshold()) {

                    // onTrigger?
                    if (onTrigger) {

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Trigger is already on. Changing triggerWindowStop to "
                                      + getTriggerWindowStop());
                        }

                    } else {

                        // We now have the start of a new trigger
                        hitsWithinTriggerWindow.addAll(slidingTimeWindow.copy());
                        numberOfHitsInTriggerWindow = hitsWithinTriggerWindow.size();

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Trigger is now on, numberOfHitsInTriggerWindow = "
                                      + numberOfHitsInTriggerWindow + " triggerWindowStart = "
                                      + getTriggerWindowStart() + " triggerWindowStop = "
                                      + getTriggerWindowStop());
                        }

                    }

                    // turn trigger on
                    onTrigger = true;

                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Now slide the window...");
                }

                // Now advance the slidingTimeWindow until the hit is inside or there is only 1 hit left in window
                while ( (!slidingTimeWindow.inTimeWindow(hitTimeUTC)) &&
                        (slidingTimeWindow.size() > 1) ) {

                    IHitPayload oldHit = slidingTimeWindow.slide();

                    // if this hit is not part of the trigger, update the earliest time of interest
                    if ( (hitsWithinTriggerWindow == null) ||
                         (!hitsWithinTriggerWindow.contains(oldHit))) {
                        IPayload oldHitPlus = new DummyPayload(oldHit.getHitTimeUTC().getOffsetUTCTime(0.1));
                        setEarliestPayloadOfInterest(oldHitPlus);
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("numberOfHitsInSlidingTimeWindow is now " + slidingTimeWindow.size()
                                  + " slidingTimeWindowStart is now " + slidingTimeWindow.startTime());
                    }

                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Done sliding");
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

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Hit still outside slidingTimeWindow, start a new one "
                                  + "numberOfHitsInSlidingTimeWindow = " + slidingTimeWindow.size()
                                  + " slidingTimeWindowStart = " + slidingTimeWindow.startTime());
                    }

                }
                // if so, add it to window
                else {
                    slidingTimeWindow.add(hit);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Hit is now in slidingTimeWindow numberOfHitsInSlidingTimeWindow = "
                                  + slidingTimeWindow.size());
                    }

                }

                /*
                 * If onTrigger, check for finished trigger
                 */
                if (onTrigger) {

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Trigger is on, numberOfHitsInSlidingTimeWindow = "
                                  + slidingTimeWindow.size());
                    }

                    if ( ( (!slidingTimeWindow.aboveThreshold()) &&
                           (!slidingTimeWindow.overlaps(hitsWithinTriggerWindow)) ) ||
                         ( (slidingTimeWindow.size() == 1) && (threshold == 1) ) ) {

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Pass trigger to analyzeTopology and reset");
                        }

                        // pass trigger and reset
                        for (int i=0; i<hitsWithinTriggerWindow.size(); i++) {
                            IHitPayload h = (IHitPayload) hitsWithinTriggerWindow.get(i);
                            if (slidingTimeWindow.contains(h)) {
                                LOG.error("Hit at time " + h.getPayloadTimeUTC()
                                          + " is part of new trigger but is still in SlidingTimeWindow");
                            }
                        }
                        analyzeHits(hitsWithinTriggerWindow);

                        onTrigger = false;
                        hitsWithinTriggerWindow.clear();
                        numberOfHitsInTriggerWindow = 0;

                    } else {
                        addHitToTriggerWindow(hit);

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Still on trigger, numberOfHitsInTriggerWindow = "
                                      + numberOfHitsInTriggerWindow);
                        }


                    }

                }

            }

        }

        numberOfHitsProcessed++;

    }

    /**
     * This method implements the volume trigger criteria.
     * @param hitsWithinTriggerWindow
     */

    public void analyzeHits(LinkedList hitsWithinTriggerWindow)
    {

        if(LOG.isDebugEnabled()) {
            LOG.debug("Counting hits which meet volume trigger criteria");
        }

	// loop over all hits and use each as the center of a volume element
	Iterator iter1 = hitsWithinTriggerWindow.iterator();
	while (iter1.hasNext()) {
	    IHitPayload center = (IHitPayload) iter1.next();
	    int numberOfHits = 1;

	    // get neighboring doms
	    String centerId = center.getDOMID().toString();
	    ArrayList<String> neighbors = getNeighboringDoms(centerId);

	    // loop over all other hits and check to see if they are in the volume element
	    Iterator iter2 = hitsWithinTriggerWindow.iterator();
	    while (iter2.hasNext()) {
		if (iter2 == iter1) continue;
		IHitPayload neighbor = (IHitPayload) iter2.next();
		String neighborId = neighbor.getDOMID().toString();

		if (neighbors.contains(neighborId)) {
		    // we have a hit in the volume element
		    numberOfHits++;

		    if (numberOfHits >= threshold) {
			formTrigger(hitsWithinTriggerWindow, null, null);
			return;
		    }
		}
	    }
	}
    }

    private ArrayList<String> getNeighboringDoms(String centerDom)
    {
	ArrayList<String> neighbors = new ArrayList<String>();

	// get the string and position of the center dom
	int centerString = getTriggerHandler().getDOMRegistry().getStringMajor(centerDom);
	int centerPosition = getTriggerHandler().getDOMRegistry().getStringMinor(centerDom);

	// get the vertical shift of the center string
	int vShift = stringMap.getVerticalOffset(Integer.valueOf(centerString));

	// calculate the range of DOM positions in this volume element
	int minPos = centerPosition - volumeHeight;
	if (minPos < 1) minPos = 1;
	int maxPos = centerPosition + volumeHeight;
	if (maxPos > 60) maxPos = 60;

	// add doms on center string, skipping center om
	Collection<DeployedDOM> allDoms = getTriggerHandler().getDOMRegistry().getDomsOnHub(centerString);
	for (DeployedDOM dom : allDoms) {
	    int position = dom.getStringMinor();
	    if (position != centerPosition) {
		if ( (position >= minPos) && (position <= maxPos) ) {
		    neighbors.add(dom.getDomId());
		}
	    }
	}

	// get list of neighboring strings
	ArrayList<Integer> neighborStrings = stringMap.getNeighbors(Integer.valueOf(centerString));
	for (Integer string : neighborStrings) {

	    // calculate the range for this string
	    int thisShift = stringMap.getVerticalOffset(string);
	    int omShift = thisShift - vShift;

	    minPos += omShift;
	    if (minPos < 1) minPos = 1;
	    maxPos += omShift;
	    if (maxPos > 60) maxPos = 60;

	    // get the doms on this string
	    allDoms = getTriggerHandler().getDOMRegistry().getDomsOnHub(string.intValue());

	    // loop over the doms and get the ones with positions in the correct range
	    for (DeployedDOM dom : allDoms) {
		int position = dom.getStringMinor();
		if ( (position >= minPos) && (position <= maxPos) ) {
		    neighbors.add(dom.getDomId());
		}

	    }
	}
	return neighbors;
    }

    /**
     * Flush the trigger. Basically indicates that there will be no further payloads to process
     * and no further calls to runTrigger.
     */

    public void flush()
    {
        // see if we're above threshold, if so form a trigger
        if (onTrigger) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Last Trigger is on, numberOfHitsInTriggerWindow = "
                          + numberOfHitsInTriggerWindow);
            }

            // pass last trigger
            analyzeHits(hitsWithinTriggerWindow);
        }
        //see if TimeWindow has enough hits to form a trigger before an out of bounds hit
        else if(slidingTimeWindow.aboveThreshold()) {

            hitsWithinTriggerWindow.addAll(slidingTimeWindow.copy());
            numberOfHitsInTriggerWindow = hitsWithinTriggerWindow.size();

            if (LOG.isDebugEnabled()) {
                LOG.debug(" Last Trigger is now on, numberOfHitsInTriggerWindow = "
                + numberOfHitsInTriggerWindow + " triggerWindowStart = "
                + getTriggerWindowStart() + " triggerWindowStop = "
                + getTriggerWindowStop());
            }

            // pass last trigger
            analyzeHits(hitsWithinTriggerWindow);
        }
        reset();
    }
    /*
     *Get Parameter Methods
     */

    public int getThreshold()
    {
        return threshold;
    }

    public int getTimeWindow()
    {
        return timeWindow;
    }

    public int getVolumeHeight()
    {
        return volumeHeight;
    }

    public int getCenterShift()
    {
        return centerShift;
    }

    /**
     * getter for triggerWindowStart
     * @return start of trigger window
     */

    private IUTCTime getTriggerWindowStart()
    {
        return ((IHitPayload) hitsWithinTriggerWindow.getFirst()).getHitTimeUTC();
    }

    /**
     * getter for triggerWindowStop
     * @return stop of trigger window
     */

    private IUTCTime getTriggerWindowStop()
    {
        return ((IHitPayload) hitsWithinTriggerWindow.getLast()).getHitTimeUTC();
    }

    public int getNumberOfHitsWithinSlidingTimeWindow()
    {
        return slidingTimeWindow.size();
    }

    public int getNumberOfHitsWithinTriggerWindow()
    {
        return hitsWithinTriggerWindow.size();
    }

    /*
     *Set Parameter Methods
     */

    public void setThreshold(int Threshold)
    {
        this.threshold = Threshold;
    }

    public void setTimeWindow(int timeWindow)
    {
        this.timeWindow = timeWindow;
    }

    public void setVolumeHeight(int volumeHeight)
    {
        this.volumeHeight = volumeHeight;
    }

    public void setCenterShift(int centerShift)
    {
        this.centerShift = centerShift;
    }

    /**
     * add a hit to the trigger time window
     * @param triggerPrimitive hit to add
     */

    private void addHitToTriggerWindow(IHitPayload triggerPrimitive)
    {
        hitsWithinTriggerWindow.addLast(triggerPrimitive);
        numberOfHitsInTriggerWindow = hitsWithinTriggerWindow.size();
    }

    private void reset()
    {

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
         * Resetting VolumeTrigger parameters
         */

        volumeHeight = 0;
        centerShift = 0;
        configVolumeHeight = false;
        configCenterShift = false;

    }

    private final class SlidingTimeWindow {

        private LinkedList hits;

        private SlidingTimeWindow()
        {
            hits = new LinkedList();
        }

        private void add(IHitPayload hit)
        {
            hits.addLast(hit);
        }

        private int size()
        {
            return hits.size();
        }

        private IHitPayload slide()
        {
            return (IHitPayload) hits.removeFirst();
        }

        private IUTCTime startTime()
        {
            return ((IHitPayload) hits.getFirst()).getHitTimeUTC();
        }

        private IUTCTime endTime()
        {
            return (startTime().getOffsetUTCTime((double) timeWindow));
        }

        private boolean inTimeWindow(IUTCTime hitTime)
        {
            return (hitTime.compareTo(startTime()) >= 0) &&
                   (hitTime.compareTo(endTime()) <= 0);
        }

        private LinkedList copy()
        {
            return (LinkedList) hits.clone();
        }

        private boolean contains(Object object)
        {
            return hits.contains(object);
        }

        private boolean overlaps(List list)
        {
            Iterator iter = list.iterator();
            while (iter.hasNext()) {
                if (contains(iter.next())) {
                    return true;
                }
            }
            return false;
        }

        private boolean aboveThreshold()
        {
            return (hits.size() >= threshold);
        }
    }

    /**
     * Get the monitoring name.
     *
     * @return the name used for monitoring this trigger
     */
    public String getMonitoringName()
    {
        return "II_VOLUME";
    }

    /**
     * Does this algorithm include all relevant hits in each request
     * so that it can be used to calculate multiplicity?
     *
     * @return <tt>true</tt> if this algorithm can supply a valid multiplicity
     */
    public boolean hasValidMultiplicity()
    {
        return true;
    }
}
