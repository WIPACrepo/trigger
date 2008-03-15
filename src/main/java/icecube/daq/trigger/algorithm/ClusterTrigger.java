package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IPayload;
import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.util.DOMRegistry;

import java.util.Iterator;
import java.util.LinkedList;
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
    private int  multiplicity;
    private int  coherenceLength;
    private int  coherenceUp;
    private int  coherenceDown;
    private boolean configured;
    
    private LinkedList<IHitPayload> triggerQueue;
    
    private static final Logger logger = Logger.getLogger(ClusterTrigger.class);
    
    public ClusterTrigger()
    {
        triggerQueue    = new LinkedList<IHitPayload>();
        multiplicity    = 5;
        timeWindow      = 15000L;
        coherenceLength = 7;
        coherenceUp     = (coherenceLength - 1) / 2;
        coherenceDown   = coherenceLength / 2;
        configured      = false;
    }
    
    @Override
    public void addParameter(TriggerParameter parameter) throws UnknownParameterException,
            IllegalParameterValueException
    {
        if (parameter.getName().equals("timeWindow"))
            timeWindow = Long.parseLong(parameter.getValue()) * 10L;
        else if (parameter.getName().equals("multiplicity"))
            multiplicity = Integer.parseInt(parameter.getValue());
        else if (parameter.getName().equals("coherenceLength"))
            coherenceLength = Integer.parseInt(parameter.getValue());
        super.addParameter(parameter);
        configured = true;
    }

    @Override
    public void flush()
    {
        triggerQueue.clear();
    }

    @Override
    public void runTrigger(IPayload payload) throws TriggerException
    {
        if (!(payload instanceof IHitPayload)) 
            throw new TriggerException(
                    "Payload object " + payload + " cannot be upcast to IHitPayload."
                    );
        // This upcast should be safe now
        IHitPayload hitPayload = (IHitPayload) payload;

        // Check hit type and perhaps pre-screen DOMs based on channel (HitFilter)
        if (getHitType(hitPayload) != AbstractTrigger.SPE_HIT) return;
        
        if (logger.isDebugEnabled()) 
        {
            logger.debug("Received eligible hit at UTC " + hitPayload.getHitTimeUTC() +
                    " queue size = " + triggerQueue.size());
        }
        
        while (triggerQueue.size() > 0 &&
                hitPayload.getHitTimeUTC().longValue() -
                triggerQueue.element().getHitTimeUTC().longValue() > timeWindow)
        {
            if (triggerQueue.size() >= multiplicity && processHitQueue())
            {   
                formTrigger(triggerQueue, null, null);
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
        final TreeMap<LogicalChannel, Integer> coherenceMap = new TreeMap<LogicalChannel, Integer>();
        boolean trigger = false;
        
        for (IHitPayload hit : triggerQueue)
        {
            LogicalChannel ch = LogicalChannel.fromHitPayload(hit, domRegistry);
            int m0 = Math.max( 1, ch.module - coherenceUp);
            int m1 = Math.min(60, ch.module + coherenceDown);
            for (int m = m0; m <= m1; m++) 
            {
                int counter = 0;
                if (coherenceMap.containsKey(ch)) counter = coherenceMap.get(ch);
                counter += 1;
                if (counter >= multiplicity) trigger = true;
                coherenceMap.put(ch, counter);
            }
        }
        
        // No trigger so skip next operation
        if (!trigger) return false;
        
        // Prune hits not in spatial cluster out of queue as these
        // will be built into trigger very soon.
        for (Iterator<IHitPayload> hitIt = triggerQueue.iterator(); hitIt.hasNext(); )
        {
            IHitPayload hit = hitIt.next();
            LogicalChannel logicalChannel = LogicalChannel.fromHitPayload(hit, domRegistry);
            if (coherenceMap.containsKey(logicalChannel))
            {
                if (coherenceMap.get(logicalChannel) < multiplicity) hitIt.remove();
            }
            else
            {
                logger.warn("Logical channel not in coherenceMap: " + logicalChannel); 
            }
        }
        
        return true;
    }

    public boolean isConfigured()
    {
        return configured;
    }

}

class LogicalChannel
{
    int string;
    int module;
    long numericMBID;
    String mbid;
    
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

    @Override
    public String toString()
    {
        return String.format("(%d, %d) [%s]", string, module, mbid);
    }
    
    
    
    
}
