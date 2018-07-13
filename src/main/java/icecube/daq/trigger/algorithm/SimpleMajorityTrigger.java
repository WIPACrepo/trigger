/*
 * class: SimpleMajorityTrigger
 *
 * Version $Id: SimpleMajorityTrigger.java 17047 2018-07-13 17:28:58Z dglo $
 *
 * Date: August 19 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.payload.PayloadInterfaceRegistry;
import icecube.daq.payload.IDOMID;
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
import java.util.LinkedList;
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
 * @version $Id: SimpleMajorityTrigger.java 17047 2018-07-13 17:28:58Z dglo $
 * @author pat
 */
public final class SimpleMajorityTrigger extends AbstractTrigger
{
    /**
     * Log object for this class
     */
    private static final Log LOG =
        LogFactory.getLog(SimpleMajorityTrigger.class);

    /**
     * If the 'allowSMTRerun' property is set, hits are no longer dropped
     * after a request has been created.
     */
    private static final boolean allowRerun = checkRerunProperty();

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

    /**
     * Time of previous hit, used to ensure strict time ordering
     */
    private IUTCTime lastHitTime = null;

    /** If 'allowSMTRerun' was not set, log a warning */
    private boolean loggedBuggy = false;

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
        if (!allowRerun && !loggedBuggy) {
            loggedBuggy = true;
            // XXX when this is deleted, remove this phrase from all unit tests
            LOG.error("Using buggy SMT algorithm");
        }

        runTrigger(payload, true);
    }

    /**
     * Run the trigger algorithm on a payload.
     *
     * @param payload payload to process
     * @param rerunHit if a request is created, run the hit again
     *
     * @throws icecube.daq.trigger.exceptions.TriggerException
     *          if the algorithm doesn't like this payload
     */
    public void runTrigger(IPayload payload, boolean rerunHit)
        throws TriggerException
    {
        // check that this is a hit
        int interfaceType = payload.getPayloadInterfaceType();
        if ((interfaceType != PayloadInterfaceRegistry.I_HIT_PAYLOAD) &&
            (interfaceType != PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD)) {
            throw new TriggerException("Expecting an IHitPayload");
        }
        IHitPayload hit = (IHitPayload) payload;

        IUTCTime hitTimeUTC = hit.getHitTimeUTC();
        if (hitTimeUTC == null) {
            throw new TriggerException("Hit time was null");
        }

        // verify strict time ordering
        if (lastHitTime != null && hitTimeUTC.compareTo(lastHitTime) < 0) {
            throw new TimeOutOfOrderException(
                    "Hit comes before previous hit:" +
                    " Previous hit is at " + lastHitTime +
                    " Hit is at " + hitTimeUTC + " DOMId = " +
                    hit.getDOMID());
        }
        lastHitTime = hitTimeUTC;

        if (slidingTimeWindow.size() == 0) {

            // Initialize earliest payload of interst
            setEarliestPayloadOfInterest(hit);
        }

        /*
         * Skip hits that we don't use.
         * Check hit type and perhaps pre-screen DOMs based on channel.
         */
        boolean usableHit =
            getHitType(hit) == AbstractTrigger.SPE_HIT &&
            hitFilter.useHit(hit);
        if (!usableHit) {
            return;
        }

        /*
         * Set the window front to the current hit time and slide the window
         * tail forward, removing hits no longer in the window.
         */
        while (slidingTimeWindow.size() > 0 && 
               !slidingTimeWindow.inTimeWindow(hitTimeUTC)) {

            IHitPayload oldHit = slidingTimeWindow.slide();

            /*
             * If this hit is not part of the trigger, update the
             * Earliest time of interest.  If the trigger is on, the
             * hit, by definition, is part of the trigger.
             */
            if (!haveTrigger()) {
                IPayload oldHitPlus =
                    new DummyPayload(oldHit.getHitTimeUTC().getOffsetUTCTime(0.1));
                setEarliestPayloadOfInterest(oldHitPlus);
            }
        }

        // Add hit to the sliding window
        slidingTimeWindow.add(hit);

        // Quit if we're below threshold and we don't currently have a trigger
        if (!slidingTimeWindow.aboveThreshold() && !haveTrigger()) {
            return;
        }

        /*
         * We're either above threshold, currently have a trigger, or both.
         * Check if we have reached the trigger ending condition.
         *
         * N.B. Ending condition is defined as a time period the
         * length of the trigger window with no hits. (i.e.
         * the sliding time window only has one hit).
         *
         * N.B. We also have to explicitly check haveTrigger() to correctly
         * handle the threshold == 1 case, because we may not have a
         * trigger at this point in this case.
         */
        if (slidingTimeWindow.size() == 1 && haveTrigger()) {
            flushTrigger();
            if (allowRerun && rerunHit) {
                runTrigger(hit, false);
            }
            return;
        }

        /* 
         * Trigger condition satisfied.  If trigger is new,
         * we need to copy the trigger window hits
         */
         if (!haveTrigger()) {
             hitsWithinTriggerWindow.addAll(slidingTimeWindow.copy());
         } else {
             // We only need to copy the current hit
             hitsWithinTriggerWindow.add(hit);
         }
    }

    /**
     * Flush the trigger. Basically indicates that there will be no further
     * payloads to process and no further calls to runTrigger.
     */
    public void flush()
    {
        flushTrigger();
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
     * Do we currently have a trigger?
     */
    private boolean haveTrigger()
    {
        return hitsWithinTriggerWindow.size() > 0;
    }

     /**
     * Form any pending triggers if we have them
     */
    private void flushTrigger() {
        if (haveTrigger()) {
            formTrigger(hitsWithinTriggerWindow.list(), null, null);
            hitsWithinTriggerWindow.clear();
        }
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
        lastHitTime = null;
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

    private static final boolean checkRerunProperty()
    {
        final String prop = System.getProperty("allowSMTRerun");
        return prop != null && prop.length() > 0;
    }
}
