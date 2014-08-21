package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.util.DOMRegistry;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;

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
public class ClusterTrigger extends AbstractTrigger
{
    private long timeWindow;
    private boolean configTimeWindow;

    private int  multiplicity;
    private boolean configMultiplicity;

    private int  coherenceLength;
    private int  coherenceUp;
    private int  coherenceDown;
    private boolean configCoherence;

    private LinkedList<IHitPayload> triggerQueue;

    private static final Logger logger = Logger.getLogger(ClusterTrigger.class);

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
            LogicalChannel logical = LogicalChannel.fromHitPayload(
                    hitPayload, getTriggerHandler().getDOMRegistry());
            logger.debug("Received hit at UTC " + hitPayload.getUTCTime() +
                         " - logical channel " + logical +
                         " queue size = " + triggerQueue.size());
        }

        // try to form a request
        boolean formed = false;
        while (triggerQueue.size() > 0 &&
               hitPayload.getUTCTime() - triggerQueue.element().getUTCTime() >
               timeWindow)
        {
            if (triggerQueue.size() >= multiplicity && processHitQueue()) {
                formTrigger(triggerQueue, null, null);
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
                earliest.getPayloadTimeUTC().getOffsetUTCTime(-0.1);
            setEarliestPayloadOfInterest(new DummyPayload(earliestUTC));
        }

        // if new hit is usable, add it to the queue
        if (getHitType(hitPayload) == AbstractTrigger.SPE_HIT &&
            hitFilter.useHit(hitPayload))
        {
            triggerQueue.add(hitPayload);
        }
    }

    private boolean processHitQueue()
    {
        final DOMRegistry domRegistry = getTriggerHandler().getDOMRegistry();
        final TreeMap<LogicalChannel, Integer> coherenceMap = new TreeMap<LogicalChannel, Integer>();
        boolean trigger = false;

        for (IHitPayload hit : triggerQueue)
        {
            LogicalChannel central = LogicalChannel.fromHitPayload(hit, domRegistry);
            int m0 = Math.max( 1, central.module - coherenceUp);
            int m1 = Math.min(60, central.module + coherenceDown);
            for (int m = m0; m <= m1; m++)
            {
                LogicalChannel ch = new LogicalChannel(central.string, m);
                int counter = 0;
                if (coherenceMap.containsKey(ch)) counter = coherenceMap.get(ch);
                counter += 1;
                if (counter >= multiplicity) trigger = true;
                coherenceMap.put(ch, counter);
            }
        }

        if (logger.isDebugEnabled())
        {
            for (Entry<LogicalChannel, Integer> e : coherenceMap.entrySet()) {
                logger.debug("Logical channel " + e.getKey() + " : " +
                             e.getValue());
            }
        }

        // No trigger so skip next operation
        if (!trigger) return false;

        // Remove sites in coherence map less than threshold
        for (Iterator<Integer> it = coherenceMap.values().iterator(); it.hasNext(); )
            if (it.next() < multiplicity) it.remove();

        // Prune hits not in spatial cluster out of queue as these
        // will be built into trigger very soon.
        for (Iterator<IHitPayload> hitIt = triggerQueue.iterator(); hitIt.hasNext(); )
        {
            IHitPayload hit = hitIt.next();
            LogicalChannel testCh = LogicalChannel.fromHitPayload(hit, domRegistry);
            boolean clust = false;
            for (LogicalChannel ch : coherenceMap.keySet())
                if (ch.isNear(testCh, coherenceUp, coherenceDown)) clust = true;
            if (!clust) hitIt.remove();
        }

        return true;
    }

    public boolean isConfigured()
    {
        return configTimeWindow && configMultiplicity && configCoherence;
    }

}

class LogicalChannel implements Comparable<LogicalChannel>
{
    int string;
    int module;
    long numericMBID;
    String mbid;

    LogicalChannel()
    {
        this(0, 0);
    }

    LogicalChannel(int string, int module)
    {
        this.string = string;
        this.module = module;
        this.numericMBID = 0x00000000000L;
        this.mbid   = "000000000000";
    }

    @Override
    public int hashCode()
    {
        return 64 * string + module - 1;
    }

    static LogicalChannel fromHitPayload(IHitPayload hit, DOMRegistry registry)
    {
        LogicalChannel logCh = new LogicalChannel();
        logCh.numericMBID   = hit.getDOMID().longValue();
        logCh.mbid          = String.format("%012x", logCh.numericMBID);
        logCh.string        = registry.getStringMajor(logCh.mbid);
        logCh.module        = registry.getStringMinor(logCh.mbid);
        return logCh;
    }

    /**
     * Determine whether given channel is inside [up,down] radius of this
     * channel.
     * @param ch test channel to compare
     * @param up up radius
     * @param down down radius
     * @return true if within near neighborhood
     */
    boolean isNear(LogicalChannel ch, int up, int down)
    {
        if (string != ch.string) return false;
        int intraStringSeparation = ch.module - module;
        if ((intraStringSeparation < 0 && -intraStringSeparation <= up) ||
                (intraStringSeparation > 0 && intraStringSeparation <= down) ||
                intraStringSeparation == 0) return true;
        return false;
    }

    @Override
    public String toString()
    {
        return String.format("(%d, %d)", string, module);
    }

    public int compareTo(LogicalChannel o)
    {
        if (hashCode() < o.hashCode())
            return -1;
        else if (hashCode() > o.hashCode())
            return 1;
        else
            return 0;
    }

    @Override
    public boolean equals(Object obj)
    {
       if (obj==null) {
           return false;
       }

        return hashCode() == obj.hashCode();
    }
}
