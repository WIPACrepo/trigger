package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.util.DOMRegistry;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * The VolumeTrigger is based on the ClusterTrigger, with a slight modifications to
 * allow clusters to span neighboring strings.  The VolumeTrigger
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
 * <i>logical channel</i>, allowing for counts on neighboring strings.  A space cluster will
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
public class VolumeTrigger extends AbstractTrigger
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

    private boolean needStringMap;
    private TreeMap<Integer, TreeSet<Integer>> stringMap;

    private static final Logger logger = Logger.getLogger(VolumeTrigger.class);

    public VolumeTrigger()
    {
        triggerQueue    = new LinkedList<IHitPayload>();

        setMultiplicity(5);
        configMultiplicity = false;

        setTimeWindow(1500L);
        configTimeWindow = false;

        setCoherenceLength(7);
        configCoherence = false;

	needStringMap = true;
    }

    @Override
    public void addParameter(TriggerParameter parameter) throws UnknownParameterException,
            IllegalParameterValueException
    {
        if (parameter.getName().equals("timeWindow"))
            setTimeWindow(Long.parseLong(parameter.getValue()));
        else if (parameter.getName().equals("multiplicity"))
            setMultiplicity(Integer.parseInt(parameter.getValue()));
        else if (parameter.getName().equals("coherenceLength"))
            setCoherenceLength(Integer.parseInt(parameter.getValue()));
        else if (parameter.getName().equals("domSet")) {
            domSetId = Integer.parseInt(parameter.getValue());
            try {
                configHitFilter(domSetId);
            } catch (ConfigException ce) {
                throw new IllegalParameterValueException("Bad DomSet #" +
                                                         domSetId, ce);
            }
        }
        super.addParameter(parameter);
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

	// Get the string map if we haven't already
	if (needStringMap) {
	    stringMap = getTriggerHandler().getStringMap();
	    needStringMap = false;
	}

        if (!(payload instanceof IHitPayload))
            throw new TriggerException(
                    "Payload object " + payload + " cannot be upcast to IHitPayload."
                    );
        // This upcast should be safe now
        IHitPayload hitPayload = (IHitPayload) payload;

        // Check hit type and perhaps pre-screen DOMs based on channel (HitFilter)
        if (getHitType(hitPayload) != AbstractTrigger.SPE_HIT) return;
        if (!hitFilter.useHit(hitPayload)) return;

        if (logger.isDebugEnabled())
        {
            LogicalChannelVT logical = LogicalChannelVT.fromHitPayload(
                    hitPayload, getTriggerHandler().getDOMRegistry());
            logger.debug("Received hit at UTC " +
                    hitPayload.getHitTimeUTC() +
                    " - logical channel " + logical +
                    " queue size = " + triggerQueue.size());
        }

        while (triggerQueue.size() > 0 &&
                hitPayload.getHitTimeUTC().longValue() -
                triggerQueue.element().getHitTimeUTC().longValue() > timeWindow)
        {
            if (triggerQueue.size() >= multiplicity && processHitQueue())
            {
                if (triggerQueue.size() > 0) formTrigger(triggerQueue, null, null);
                triggerQueue.clear();
                setEarliestPayloadOfInterest(hitPayload);
                break;
            }
            else
            {
                triggerQueue.removeFirst();
                IHitPayload firstHitInQueue = triggerQueue.peek();
                if (firstHitInQueue == null) firstHitInQueue = hitPayload;
                setEarliestPayloadOfInterest(firstHitInQueue);
            }
        }
        triggerQueue.add(hitPayload);
    }

    private boolean processHitQueue()
    {
    	final DOMRegistry domRegistry = getTriggerHandler().getDOMRegistry();

        final TreeMap<LogicalChannelVT, Integer> coherenceMap = new TreeMap<LogicalChannelVT, Integer>();
        boolean trigger = false;

        for (IHitPayload hit : triggerQueue)
        {
            LogicalChannelVT central = LogicalChannelVT.fromHitPayload(hit, domRegistry);

	    // Get the neighboring strings
	    Integer stringNum = new Integer(central.string);
	    TreeSet<Integer> strings = stringMap.get(stringNum);

	    // Iterate over the set of neighbors
	    for (Integer st : strings ) {

		int m0 = Math.max( 1, central.module - coherenceUp);
		int m1 = Math.min(60, central.module + coherenceDown);
		for (int m = m0; m <= m1; m++)
		    {
			LogicalChannelVT ch = new LogicalChannelVT(st.intValue(), m);
			int counter = 0;
			if (coherenceMap.containsKey(ch)) counter = coherenceMap.get(ch);
			counter += 1;
			if (counter >= multiplicity) trigger = true;
			coherenceMap.put(ch, counter);
		    }
	    }
        }

        if (logger.isDebugEnabled())
        {
            for (LogicalChannelVT ch : coherenceMap.keySet())
            {
                logger.debug("Logical channel " + ch + " : " + coherenceMap.get(ch));
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
            LogicalChannelVT testCh = LogicalChannelVT.fromHitPayload(hit, domRegistry);

	    // Get the neighboring strings
	    Integer stringNum = new Integer(testCh.string);
	    TreeSet<Integer> strings = stringMap.get(stringNum);

            boolean clust = false;
            for (LogicalChannelVT ch : coherenceMap.keySet())
                if (ch.isNear(testCh, coherenceUp, coherenceDown, strings)) clust = true;
            if (!clust) hitIt.remove();
        }

        return true;
    }

    public boolean isConfigured()
    {
        return configTimeWindow && configMultiplicity && configCoherence;
    }

}

class LogicalChannelVT implements Comparable<LogicalChannelVT>
{
    int string;
    int module;
    long numericMBID;
    String mbid;

    LogicalChannelVT()
    {
        this(0, 0);
    }

    LogicalChannelVT(int string, int module)
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

    static LogicalChannelVT fromHitPayload(IHitPayload hit, DOMRegistry registry)
    {
        LogicalChannelVT logCh = new LogicalChannelVT();
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
    boolean isNear(LogicalChannelVT ch, int up, int down, TreeSet<Integer> neighbors)
    {
	// First check if this string is near the hit string
	boolean closeString = neighbors.contains(new Integer(string));

	// Next check if this module is near the hit module
	boolean closeModule = false;
        int intraStringSeparation = ch.module - module;
        if ((intraStringSeparation < 0 && -intraStringSeparation <= up) ||
                (intraStringSeparation > 0 && intraStringSeparation <= down) ||
	    intraStringSeparation == 0) closeModule =  true;

	if (closeString && closeModule) return true;

        return false;
    }

    @Override
    public String toString()
    {
        return String.format("(%d, %d)", string, module);
    }

    public int compareTo(LogicalChannelVT o)
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
        return hashCode() == obj.hashCode();
    }




}
