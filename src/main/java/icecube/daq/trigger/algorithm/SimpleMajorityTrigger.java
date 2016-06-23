/*
 * class: SimpleMajorityTrigger
 *
 * Version $Id: SimpleMajorityTrigger.java 16158 2016-06-23 20:35:31Z dglo $
 *
 * Date: August 19 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TimeOutOfOrderException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class HitCollection
    implements Iterable<IHitPayload>
{
    private static final HitComparator COMPARATOR = new HitComparator();

    private ArrayList<IHitPayload> hits = new ArrayList<IHitPayload>();

    public void addAll(List<IHitPayload> list) {
        hits.addAll(list);
    }

    public void add(IHitPayload hit)
    {
        hits.add(hit);
    }

    public void clear()
    {
        hits.clear();
    }

    public boolean contains(IHitPayload hit)
    {
        if (hits.size() < 100) {
            return hits.contains(hit);
        }

        return Collections.binarySearch(hits, hit, COMPARATOR) >= 0;
    }

    public List<IHitPayload> copy()
    {
        return (List<IHitPayload>) hits.clone();
    }

    public IHitPayload getFirst()
    {
        return hits.get(0);
    }

    public IHitPayload getLast()
    {
        return hits.get(hits.size() - 1);
    }

    public Iterator<IHitPayload> iterator()
    {
        return hits.iterator();
    }

    public List<IHitPayload> list()
    {
        return hits;
    }

    public boolean overlaps(HitCollection coll)
    {
        for (IHitPayload hit : hits) {
            if (coll.contains(hit)) {
                return true;
            }
        }
        return false;
    }

    public IHitPayload removeFirst()
    {
        return hits.remove(0);
    }

    public int size()
    {
        return hits.size();
    }
}

/**
 * This class implements a simple multiplicty trigger.
 *
 * @version $Id: SimpleMajorityTrigger.java 16158 2016-06-23 20:35:31Z dglo $
 * @author pat
 */
public final class SimpleMajorityTrigger extends AbstractTrigger
{
    /**
     * Log object for this class
     */
    private static final Log LOG =
        LogFactory.getLog(SimpleMajorityTrigger.class);

    private static int nextTriggerNumber;
    private int triggerNumber;

    /**
     * Trigger parameters
     */
    private int threshold;
    private int timeWindow;

    /**
     * list of hits currently within slidingTimeWindow
     */
    private SlidingTimeWindow slidingTimeWindow = new SlidingTimeWindow();

    /**
     * list of hits in current trigger
     */
    private HitCollection hitsWithinTriggerWindow = new HitCollection();

    private boolean configThreshold = false;
    private boolean configTimeWindow = false;

    public SimpleMajorityTrigger()
    {
        triggerNumber = ++nextTriggerNumber;
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
    public boolean isConfigured()
    {
        return (configThreshold && configTimeWindow);
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
        if (name.compareTo("threshold") == 0) {
            threshold = Integer.parseInt(value);
            configThreshold = true;
        } else if (name.compareTo("timeWindow") == 0) {
            timeWindow = Integer.parseInt(value);
            configTimeWindow = true;
        } else if (name.compareTo("triggerPrescale") == 0) {
            triggerPrescale = Integer.parseInt(value);
        } else if (name.compareTo("domSet") == 0) {
            domSetId = Integer.parseInt(value);
            try {
                configHitFilter(domSetId);
            } catch (ConfigException ce) {
                throw new IllegalParameterValueException("Bad DomSet #" +
                                                         domSetId, ce);
            }
        } else {
            throw new UnknownParameterException("Unknown parameter: " +
                                                name);
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
        throws TriggerException
    {
        // check that this is a hit
        int interfaceType = payload.getPayloadInterfaceType();
        if ((interfaceType != PayloadInterfaceRegistry.I_HIT_PAYLOAD) &&
            (interfaceType != PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD)) {
            throw new TriggerException("Expecting an IHitPayload");
        }
        IHitPayload hit = (IHitPayload) payload;

        // Check hit type and perhaps pre-screen DOMs based on channel
        boolean usableHit =
            getHitType(hit) == AbstractTrigger.SPE_HIT &&
            hitFilter.useHit(hit);

        IUTCTime hitTimeUTC = hit.getHitTimeUTC();
        if (hitTimeUTC == null) {
            throw new TriggerException("Hit time was null");
        }

        /*
         * Initialization for first hit
         */
        if (slidingTimeWindow.size() == 0) {

            if (usableHit) {
                // initialize slidingTimeWindow
                slidingTimeWindow.add(hit);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("This is the first hit, initializing...");
                    LOG.debug("slidingTimeWindowStart set to " +
                              slidingTimeWindow.startTime());
                }
            }

            // initialize earliest time of interest
            setEarliestPayloadOfInterest(hit);
        }
        /*
         * Try to add another hit
         */
        else {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Processing hit at time " + hitTimeUTC);
            }

            /*
             * Check for out-of-time hits
             * hitTime < slidingTimeWindowStart
             */
            if (hitTimeUTC.compareTo(slidingTimeWindow.startTime()) < 0) {
                throw new TimeOutOfOrderException(
                        "Hit comes before start of sliding time window:" +
                        " Window is at " + slidingTimeWindow.startTime() +
                        " Hit is at " + hitTimeUTC + " DOMId = " +
                        hit.getDOMID());
            }

            /*
             * Hit falls within the slidingTimeWindow
             */
            if (slidingTimeWindow.inTimeWindow(hitTimeUTC)) {
                if (usableHit) {
                    slidingTimeWindow.add(hit);

                    // If onTrigger, add hit to list
                    if (onTrigger) {
                        addHitToTriggerWindow(hit);
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Hit falls within slidingTimeWindow" +
                                  " numberOfHitsInSlidingTimeWindow now" +
                                  " equals " + slidingTimeWindow.size());
                    }
                }
            }
            /*
             * Hits falls outside the slidingTimeWindow
             *   First see if we have a trigger, then slide the window
             */
            else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Hit falls outside slidingTimeWindow," +
                              " numberOfHitsInSlidingTimeWindow is "
                              + slidingTimeWindow.size());
                }

                // Do we have a trigger?
                if (slidingTimeWindow.aboveThreshold()) {

                    // onTrigger?
                    if (onTrigger) {

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Trigger is already on. Changing" +
                                      " triggerWindowStop to "
                                      + getTriggerWindowStop());
                        }

                    } else {

                        // We now have the start of a new trigger
                        hitsWithinTriggerWindow.addAll(slidingTimeWindow.copy());
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Trigger is now on," +
                                      " numberOfHitsInTriggerWindow = "
                                      + hitsWithinTriggerWindow.size() +
                                      " triggerWindowStart = "
                                      + getTriggerWindowStart() +
                                      " triggerWindowStop = "
                                      + getTriggerWindowStop());
                        }

                    }

                    // turn trigger on
                    onTrigger = true;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Now slide the window...");
                }

                // Now advance the slidingTimeWindow until the hit is inside
                // or there is only 1 hit left in window
                while ( (!slidingTimeWindow.inTimeWindow(hitTimeUTC)) &&
                        (slidingTimeWindow.size() > 1) ) {
                    IHitPayload oldHit = slidingTimeWindow.slide();

                    // if this hit is not part of the trigger, update the
                    // earliest time of interest
                    if (hitsWithinTriggerWindow == null ||
                        !hitsWithinTriggerWindow.contains(oldHit))
                    {
                        IPayload oldHitPlus =
                            new DummyPayload(oldHit.getHitTimeUTC().getOffsetUTCTime(0.1));
                        setEarliestPayloadOfInterest(oldHitPlus);
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("numberOfHitsInSlidingTimeWindow is now " +
                                  slidingTimeWindow.size()
                                  + " slidingTimeWindowStart is now " +
                                  slidingTimeWindow.startTime());
                    }
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Done sliding");
                }

                /*
                 * Does it fall in new slidingTimeWindow?
                 */
                if (!slidingTimeWindow.inTimeWindow(hitTimeUTC)) {

                    IHitPayload oldHit = slidingTimeWindow.slide();

                    if (hitsWithinTriggerWindow == null ||
                        !hitsWithinTriggerWindow.contains(oldHit))
                    {
                        IPayload oldHitPlus =
                            new DummyPayload(oldHit.getHitTimeUTC().getOffsetUTCTime(0.1));
                        setEarliestPayloadOfInterest(oldHitPlus);
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Hit still outside slidingTimeWindow," +
                                  " start a new one");
                    }

                }

                if (usableHit) {
                    slidingTimeWindow.add(hit);
                }

                if (LOG.isDebugEnabled()) {
                    if (slidingTimeWindow.size() == 0) {
                        LOG.debug("Empty sliding time window");
                    } else {
                        LOG.debug("NumberOfHitsInSlidingTimeWindow = " +
                                  slidingTimeWindow.size() +
                                  " slidingTimeWindowStart = " +
                                  slidingTimeWindow.startTime());
                    }
                }

                /*
                 * If onTrigger, check for finished trigger
                 */
                if (onTrigger) {

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Trigger is on," +
                                  " numberOfHitsInSlidingTimeWindow = "
                                  + slidingTimeWindow.size());
                    }

                    // todo - also need to check if any hits still in STW are
                    // also in TW
                    if ( ( (!slidingTimeWindow.aboveThreshold()) &&
                           (!slidingTimeWindow.overlaps(hitsWithinTriggerWindow)) ) ||
                         ( (slidingTimeWindow.size() == 1) &&
                           (threshold == 1) ) )
                    {

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Report trigger and reset");
                        }

                        // form trigger and reset
                        for (IHitPayload h2 : hitsWithinTriggerWindow) {
                            if (slidingTimeWindow.contains(h2)) {
                                LOG.error("Hit at time " +
                                          h2.getPayloadTimeUTC()
                                          + " is part of new trigger but is" +
                                          " still in SlidingTimeWindow");
                            }
                        }
                        formTrigger(hitsWithinTriggerWindow.list(), null,
                                    null);

                        onTrigger = false;
                        hitsWithinTriggerWindow.clear();

                    } else if (usableHit) {
                        addHitToTriggerWindow(hit);

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Still on trigger," +
                                      " numberOfHitsInTriggerWindow = "
                                      + hitsWithinTriggerWindow.size());
                        }
                    }
                }
            }
        }
    }

    /**
     * Flush the trigger. Basically indicates that there will be no further
     * payloads to process and no further calls to runTrigger.
     */
    public void flush()
    {
        boolean formLast = true;

        // see if we're above threshold, if so form a trigger
        if (onTrigger) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Last Trigger is on, numberOfHitsInTriggerWindow = "
                          + hitsWithinTriggerWindow.size());
            }
        }
        //see if TimeWindow has enough hits to form a trigger before
        // an out of bounds hit
        else if(slidingTimeWindow.aboveThreshold()) {

            hitsWithinTriggerWindow.addAll(slidingTimeWindow.copy());

            if (LOG.isDebugEnabled()) {
                LOG.debug(" Last Trigger is now on," +
                          " numberOfHitsInTriggerWindow = " +
                          hitsWithinTriggerWindow.size() +
                          (hitsWithinTriggerWindow.size() == 0 ? "" :
                           " triggerWindowStart = " +
                           getTriggerWindowStart() + " triggerWindowStop = "+
                           getTriggerWindowStop()));
            }
        }
        else {
            formLast = false;
        }

        // form last trigger
        if (hitsWithinTriggerWindow.size() > 0 && formLast) {
            formTrigger(hitsWithinTriggerWindow.list(), null, null);
        }

        reset();
    }

    public int getThreshold()
    {
        return threshold;
    }

    public void setThreshold(int threshold)
    {
        this.threshold = threshold;
    }

    public int getTimeWindow()
    {
        return timeWindow;
    }

    public void setTimeWindow(int timeWindow)
    {
        this.timeWindow = timeWindow;
    }

    /**
     * add a hit to the trigger time window
     * @param triggerPrimitive hit to add
     */
    private void addHitToTriggerWindow(IHitPayload triggerPrimitive)
    {
        hitsWithinTriggerWindow.add(triggerPrimitive);
    }

    /**
     * getter for triggerWindowStart
     * @return start of trigger window
     */
    private IUTCTime getTriggerWindowStart()
    {
        return hitsWithinTriggerWindow.getFirst().getHitTimeUTC();
    }

    /**
     * getter for triggerWindowStop
     * @return stop of trigger window
     */
    private IUTCTime getTriggerWindowStop()
    {
        return hitsWithinTriggerWindow.getLast().getHitTimeUTC();
    }

    public int getNumberOfHitsWithinSlidingTimeWindow()
    {
        return slidingTimeWindow.size();
    }

    public int getNumberOfHitsWithinTriggerWindow()
    {
        return hitsWithinTriggerWindow.size();
    }

    private void reset()
    {
        slidingTimeWindow.clear();
        hitsWithinTriggerWindow.clear();
    }

    /**
     * Reset the algorithm to its initial condition.
     */
    public void resetAlgorithm()
    {
        reset();

        super.resetAlgorithm();
    }

    final class SlidingTimeWindow
        extends HitCollection
    {
        private boolean aboveThreshold()
        {
            return (size() >= threshold);
        }

        private IUTCTime endTime()
        {
            return (startTime().getOffsetUTCTime((double) timeWindow));
        }

        private boolean inTimeWindow(IUTCTime hitTime)
        {
            return hitTime.compareTo(startTime()) >= 0 &&
                hitTime.compareTo(endTime()) <= 0;
        }

        private IHitPayload slide()
        {
            return removeFirst();
        }

        private IUTCTime startTime()
        {
            return getFirst().getHitTimeUTC();
        }

        public String toString()
        {
            if (size() == 0) {
                return "Window[]*0";
            }

            return "Window[" + startTime() + "-" + endTime() + "]*" + size();
        }
    }

    /**
     * Get the monitoring name.
     *
     * @return the name used for monitoring this trigger
     */
    public String getMonitoringName()
    {
        return "SIMPLE_MULTIPLICITY";
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
