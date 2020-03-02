/*
 * class: SimpleMajorityTrigger
 *
 * Version $Id: SimpleMajorityTrigger.java 17732 2020-03-02 17:49:07Z dglo $
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

    @Override
    public String toString()
    {
        if (hits.size() < 1) {
            return "HitCollection[]";
        } else if (hits.size() == 1) {
            return "HitCollection[" + getFirst().getUTCTime() + "]";
        }

        return "HitCollection*" + hits.size() + "[" + getFirst().getUTCTime() +
            "-" + getLast().getUTCTime() + "]";
    }
}

/**
 * This class implements a simple multiplicty trigger.
 *
 * @version $Id: SimpleMajorityTrigger.java 17732 2020-03-02 17:49:07Z dglo $
 * @author pat
 */
public final class SimpleMajorityTrigger
    extends AbstractTrigger
{
    /** Log object for this class */
    private static final Log LOG =
        LogFactory.getLog(SimpleMajorityTrigger.class);

    /** I3Live monitoring name for this algorithm */
    private static final String MONITORING_NAME = "SIMPLE_MULTIPLICITY";

    /**
     * If the 'disableQuickPush' property is set, unused hits will not be
     * used to determine when an interval can be released
     */
    private static boolean allowQuickPush;

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

    /** On the first trip through runTrigger(), log mode */
    private boolean loggedQuick = false;

    public SimpleMajorityTrigger()
    {
        triggerNumber = ++nextTriggerNumber;

        checkQuickPushProperty();
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
    @Override
    public void addParameter(String name, String value)
        throws UnknownParameterException, IllegalParameterValueException
    {
        if (name.compareTo("threshold") == 0) {
            threshold = Integer.parseInt(value);
            configThreshold = true;
        } else if (name.compareTo("timeWindow") == 0) {
            setTimeWindow(Integer.parseInt(value));
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

    @Override
    public void setTriggerName(String triggerName)
    {
        super.triggerName = triggerName + triggerNumber;
        if (LOG.isInfoEnabled()) {
            LOG.info("TriggerName set to " + super.triggerName);
        }
    }

    /**
     * Get the monitoring name.
     *
     * @return the name used for monitoring this trigger
     */
    @Override
    public String getMonitoringName()
    {
        return MONITORING_NAME;
    }

    /**
     * Does this algorithm include all relevant hits in each request
     * so that it can be used to calculate multiplicity?
     *
     * @return <tt>true</tt> if this algorithm can supply a valid multiplicity
     */
    @Override
    public boolean hasValidMultiplicity()
    {
        return true;
    }

    /**
     * Is the trigger configured?
     *
     * @return true if it is
     */
    @Override
    public boolean isConfigured()
    {
        return (configThreshold && configTimeWindow);
    }

    /**
     * Run the trigger algorithm on a payload.
     *
     * @param payload payload to process
     *
     * @throws icecube.daq.trigger.exceptions.TriggerException
     *          if the algorithm doesn't like this payload
     */
    @Override
    public void runTrigger(IPayload payload)
        throws TriggerException
    {
        // XXX when this is deleted, remove these phrases from all unit tests
        if (!loggedQuick) {
            loggedQuick = true;
            if (!allowQuickPush) {
                LOG.error("Using slow SMT algorithm");
            } else {
                LOG.error("Using quick SMT algorithm");
            }
        }

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
            // Initialize earliest payload of interest
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
            if (LOG.isDebugEnabled()) {
                LOG.debug("Hit " + hit + " isn't usable");
            }

            if (allowQuickPush) {
                // if this hit is outside the window, flush cached interval
                if (hitsWithinTriggerWindow.size() > 0) {
                    long trigTime =
                        hitsWithinTriggerWindow.getLast().getUTCTime();
                    if (trigTime + timeWindow < hit.getUTCTime()) {
                        flushTrigger();
                        slidingTimeWindow.clear();
                    }
                }

                // if this hit is outside the window, slide it
                if (slidingTimeWindow.size() > 0 &&
                    slidingTimeWindow.getFirst().getUTCTime() + timeWindow <
                    hit.getUTCTime())
                {
                    slidingTimeWindow.slide();
                }
            }

            return;
        }

        analyzeWindow(hit, true);
    }

    private void analyzeWindow(IHitPayload hit, boolean rerunHit)
    {
        /*
         * Set the window front to the current hit time and slide the window
         * tail forward, removing hits no longer in the window.
         */
        updateSlidingWindow(hit.getHitTimeUTC());

        // Add hit to the sliding window
        if (!slidingTimeWindow.contains(hit)) {
            slidingTimeWindow.add(hit);
        }

        /*
         * Trigger condition satisfied.  If trigger is new,
         */
         if (!haveTrigger()) {
             // save the hits if we're above threshold and don't have a trigger
             if (slidingTimeWindow.aboveThreshold()) {
                 if (LOG.isDebugEnabled()) {
                     LOG.debug("Add " + slidingTimeWindow.size() +
                               " hit(s) to trigger window");
                 }
                 hitsWithinTriggerWindow.addAll(slidingTimeWindow.copy());
             } else if (LOG.isDebugEnabled()) {
                 if (LOG.isDebugEnabled()) {
                     LOG.debug("Sliding window is below threshold " +
                               threshold);
                 }
             }
         } else {
             /*
              * We're either above threshold, currently have a trigger, or
              * both. Check if we have reached the trigger ending condition.
              *
              * N.B. Ending condition is defined as a time period the
              * length of the trigger window with no hits. (i.e.
              * the sliding time window only has one hit).
              *
              * N.B. We have to explicitly check haveTrigger() to
              * correctly handle the threshold == 1 case, because we may not
              * have a trigger at this point in this case.
              */
             if (slidingTimeWindow.size() != 1) {
                 // save the current hit
                 if (LOG.isDebugEnabled()) {
                     LOG.debug("Add " + slidingTimeWindow.size() +
                               " hit(s) to trigger window");
                 }
                 hitsWithinTriggerWindow.add(hit);
             } else {
                 if (LOG.isDebugEnabled()) {
                     LOG.debug("Create request from " +
                               hitsWithinTriggerWindow.size() + " hits");
                 }
                 flushTrigger();
                 if (rerunHit) {
                     if (LOG.isDebugEnabled()) {
                         LOG.debug("Rerun analysis");
                     }
                     analyzeWindow(hit, false);
                 }
             }
         }
    }

    /**
     * Flush the trigger. Basically indicates that there will be no further
     * payloads to process and no further calls to runTrigger.
     */
    @Override
    public void flush()
    {
        if (haveTrigger()) {
            flushTrigger();
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
        return timeWindow / 10;
    }

    public void setTimeWindow(int timeWindow)
    {
        this.timeWindow = timeWindow * 10;
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
        formTrigger(hitsWithinTriggerWindow.list(), null, null);
        hitsWithinTriggerWindow.clear();
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
    @Override
    public void resetAlgorithm()
    {
        reset();

        super.resetAlgorithm();
    }

    /*
     * Set the window front to the current hit time and slide the window
     * tail forward, removing hits no longer in the window.
     */
    private void updateSlidingWindow(IUTCTime hitTimeUTC)
    {
        IUTCTime hitTime = null;
        while (slidingTimeWindow.size() > 0 &&
               !slidingTimeWindow.inTimeWindow(hitTimeUTC))
        {
            IHitPayload oldHit = slidingTimeWindow.slide();

            /*
             * If this hit is not part of the trigger, update the
             * Earliest time of interest.  If the trigger is on, the
             * hit, by definition, is part of the trigger.
             */
            if (!haveTrigger()) {
                hitTime = oldHit.getHitTimeUTC();
            }
        }

        if (hitTime != null) {
            final IUTCTime offsetTime = hitTime.getOffsetUTCTime(0.1);
            setEarliestPayloadOfInterest(new DummyPayload(offsetTime));
        }
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
            return (startTime().getOffsetUTCTime((double) (timeWindow / 10)));
        }

        private boolean inTimeWindow(IUTCTime hitTime)
        {
            if (size() == 0) {
                return false;
            }

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
                return "Window[]";
            }

            return "Window*" + size() + "[" + startTime() + "-" + endTime() +
                "]";
        }
    }

    public static final void checkQuickPushProperty()
    {
        final String prop = System.getProperty("disableQuickPush");
        allowQuickPush = prop == null;
    }
}
