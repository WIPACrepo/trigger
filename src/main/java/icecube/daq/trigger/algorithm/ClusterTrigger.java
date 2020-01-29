package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.util.DOMInfo;
import icecube.daq.util.IDOMRegistry;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

/**
 * The ClusterTrigger is a variant on the <i>StringTrigger</i> theme.  The ClusterTrigger
 * is intended for use on a central trigger module which may have inputs from multiple
 * strings.  The trigger searches for N hits clustered in a "coherence" length of M
 * adjacent modules all within a time window of &Delta;t.
 *
 * The trigger is configured via the standard trigger config XML.  It will respond to
 * the following configuration parameters ...
 * <dl>
 * <dt>timeWindow</dt>
 * <dd>The time window, &Delta;t, expressed in units of ns.</dd>
 * <dt>coherenceLength</dt>
 * <dd>The parameter M above - the cluster spatial search window.</dd>
 * <dt>multiplicity</dt>
 * <dd>The parameter M above - the multiplicity threshold.</dd>
 * </dl>
 *
 * The implementation is straightforward.  First the overall multiplicty requirement must
 * be satisfied: hits are collected into a queue until the head and tail fall outside
 * the time window.  If the queue size is &ge; N then that forms the 'first-level trigger.'
 * Upon reaching this state, it must be checked whether the hits are clustered in space.
 * This is done by incrementing counters a length M/2 in either direction from the
 * <i>logical channel</i> taking care not to cross string boundaries.  A space cluster will
 * also maintain counter[i] &ge; N for one or more <i>logical channel</i> locations.
 * <p>
 * The trigger that is formed should <i>only</i> contain those hits which are part of a
 * space cluster.  That is, hits are not part of the trigger hit list unless they are
 * clustered both in time and in space.  Simultaneous, multiple clusters will count toward
 * a single single trigger and will not produce multiple triggers.
 *
 * @author kael
 *
 */
public class ClusterTrigger
    extends AbstractTrigger
{
    /** Log object for this class */
    private static final Logger logger =
        Logger.getLogger(ClusterTrigger.class);

    /** I3Live monitoring name for this algorithm */
    private static final String MONITORING_NAME = "CLUSTER";

    /** Numeric type for this algorithm */
    public static final int TRIGGER_TYPE = 14;

    private long timeWindow;
    private boolean configTimeWindow;

    private int  multiplicity;
    private boolean configMultiplicity;

    private int  coherenceLength;
    private int  coherenceUp;
    private int  coherenceDown;
    private boolean configCoherence;

    private LinkedList<IHitPayload> triggerQueue;

    public ClusterTrigger()
    {
        triggerQueue    = new LinkedList<IHitPayload>();

        setMultiplicity(5);
        configMultiplicity = false;

        setTimeWindow(1500L);
        configTimeWindow = false;

        setCoherenceLength(7);
        configCoherence = false;
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
        if (name.equals("timeWindow"))
            setTimeWindow(Long.parseLong(value));
        else if (name.equals("multiplicity"))
            setMultiplicity(Integer.parseInt(value));
        else if (name.equals("coherenceLength"))
            setCoherenceLength(Integer.parseInt(value));
        else if (name.equals("domSet")) {
            domSetId = Integer.parseInt(value);
            try {
                configHitFilter(domSetId);
            } catch (ConfigException ce) {
                throw new IllegalParameterValueException("Bad DomSet #" +
                                                         domSetId, ce);
            }
        }
        super.addParameter(name, value);
    }

    public long getTimeWindow()
    {
        return timeWindow / 10L;
    }

    public void setTimeWindow(long lval)
    {
        timeWindow = lval * 10L;
        configTimeWindow = true;
    }

    public int getMultiplicity()
    {
        return multiplicity;
    }

    public void setMultiplicity(int val)
    {
        multiplicity = val;
        configMultiplicity = true;
    }

    public int getCoherenceLength()
    {
        return coherenceLength;
    }

    public void setCoherenceLength(int val)
    {
        coherenceLength = val;
        coherenceUp     = (coherenceLength - 1) / 2;
        coherenceDown   = coherenceLength / 2;
        configCoherence = true;
    }

    @Override
    public void flush()
    {
        triggerQueue.clear();
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
     * Get the trigger type.
     *
     * @return trigger type
     */
    @Override
    public int getTriggerType()
    {
        return TRIGGER_TYPE;
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
        return configTimeWindow && configMultiplicity && configCoherence;
    }

    @Override
    public void runTrigger(IPayload payload) throws TriggerException
    {
        if (!(payload instanceof IHitPayload)) {
            throw new TriggerException("Payload object " + payload +
                                       " cannot be upcast to IHitPayload.");
        }

        IHitPayload hitPayload = (IHitPayload) payload;

        if (logger.isDebugEnabled())
        {
            IDOMRegistry domRegistry = getTriggerHandler().getDOMRegistry();

            DOMInfo dom = getDOMFromHit(domRegistry, hitPayload);

            String chanStr;
            if (dom == null) {
                chanStr = "<null>";
            } else {
                chanStr = dom.getDeploymentLocation();
            }
            logger.debug("Received hit at UTC " + hitPayload.getUTCTime() +
                         " - logical channel " + chanStr +
                         " queue size = " + triggerQueue.size());
        }

        // try to form a request
        boolean formed = false;
        while (true) {
            boolean stoploop = (triggerQueue.size() > 0 &&
                                hitPayload.getUTCTime() -
                                triggerQueue.element().getUTCTime() >
                                timeWindow);

            if (!stoploop) {
                break;
            }

            boolean found = (triggerQueue.size() >= multiplicity &&
                             processHitQueue());
            if (found) {
                formTrigger(triggerQueue);
                triggerQueue.clear();
                formed = true;
                break;
            }

            triggerQueue.removeFirst();
        }

        // if earliest time wasn't set by formTrigger(), set it now
        if (!formed) {
            IHitPayload earliest = null;
            if (triggerQueue.size() > 0) {
                earliest = triggerQueue.peek();
            } else {
                earliest = hitPayload;
            }

            // set earliest time to just before this time
            IUTCTime earliestUTC =
                earliest.getPayloadTimeUTC().getOffsetUTCTime(-1);
            setEarliestPayloadOfInterest(new DummyPayload(earliestUTC));
        }

        // if new hit is usable, add it to the queue
        boolean use1 = getHitType(hitPayload) == SPE_HIT;
        boolean use2 = hitFilter.useHit(hitPayload);
        boolean usable = use1 && use2;
        if (usable)
        {
            triggerQueue.add(hitPayload);
        }
    }

    // preallocate coherence array
    // (this is faster than reallocating for every hit)
    private int[][] coherence = new int[86][60];

    private boolean processHitQueue()
    {
        final IDOMRegistry domRegistry = getTriggerHandler().getDOMRegistry();
        boolean trigger = false;

        // clear coherence array
        for (int[] row : coherence) {
            java.util.Arrays.fill(row, 0);
        }

        for (IHitPayload hit : triggerQueue) {
            DOMInfo dom = getDOMFromHit(domRegistry, hit);
            if (dom == null) {
                logger.error("Cannot find DOM for " + hit.toString());
                continue;
            }

            int m0 = Math.max( 1, dom.getStringMinor() - coherenceUp);
            int m1 = Math.min(60, dom.getStringMinor() + coherenceDown);
            for (int m = m0; m <= m1; m++) {
                // if one site exceeds multiplicity, we've got a trigger!
                final int site =
                    ++coherence[dom.getStringMajor() - 1][m - 1];
                if (site >= multiplicity) {
                    trigger = true;
                }
            }
        }

        if (logger.isDebugEnabled()) {
            for (int s = 0; s < coherence.length; s++) {
                for (int p = 0; p < coherence[s].length; p++) {
                    if (coherence[s][p] > 0) {
                        final String str =
                            String.format("%d-%d: %s", s + 1, p + 1,
                                          coherence[s][p]);
                        logger.debug(str);
                    }
                }
            }
        }

        // No trigger so skip next operation
        if (!trigger) return false;

        // Prune hits not in spatial cluster out of queue;
        // remaining hits will be built into a trigger very soon.
        for (Iterator<IHitPayload> hitIt = triggerQueue.iterator();
             hitIt.hasNext(); )
        {
            IHitPayload hit = hitIt.next();
            DOMInfo dom = getDOMFromHit(domRegistry, hit);
            int[] string = coherence[dom.getStringMajor() - 1];

            int top = Math.max( 1, dom.getStringMinor() - coherenceUp) - 1;
            int bottom =
                Math.min(60, dom.getStringMinor() + coherenceDown) - 1;

            boolean clust = false;
            for (int i = top; i <= bottom; i++) {
                if (string[i] >= multiplicity) {
                    clust = true;
                    break;
                }
            }

            if (!clust) hitIt.remove();
        }

        return true;
    }
}
