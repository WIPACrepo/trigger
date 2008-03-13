package icecube.daq.trigger.algorithm;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import icecube.daq.payload.IPayload;
import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.util.DOMRegistry;

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
        
        triggerQueue.add((IHitPayload) payload);
        while (triggerQueue.size() > 1 &&
                triggerQueue.getLast().getHitTimeUTC().longValue() - 
                triggerQueue.getFirst().getHitTimeUTC().longValue() > timeWindow)
        {
            if (triggerQueue.size() > multiplicity && processHitQueue())
            {   
                formTrigger(triggerQueue, null, null);
                triggerQueue.clear();
                setEarliestPayloadOfInterest(new DummyPayload(
                        hitPayload.getPayloadTimeUTC().getOffsetUTCTime(0.1)
                        )
                );
            }
            else
            {
                triggerQueue.removeFirst();
                setEarliestPayloadOfInterest(triggerQueue.peek());
            }
        }

    }
    
    private boolean processHitQueue()
    {
    	final DOMRegistry domRegistry = getTriggerHandler().getDOMRegistry();
        final TreeMap<Integer, Integer> coherenceMap = new TreeMap<Integer, Integer>();
        boolean trigger = false;
        
        for (IHitPayload hit : triggerQueue)
        {
            long numericMBID = hit.getDOMID().longValue();
            String mbid      = String.format("%012x", numericMBID);
            int stringNumber = domRegistry.getStringMajor(mbid);
            int moduleNumber = domRegistry.getStringMinor(mbid);
            int m0 = Math.max( 1, moduleNumber - coherenceUp);
            int m1 = Math.min(60, moduleNumber + coherenceDown);
            for (int m = m0; m < m1; m++) 
            {
                int logicalChannel = 64 * stringNumber + m;
                int counter = 0;
                if (coherenceMap.containsKey(logicalChannel)) counter = coherenceMap.get(logicalChannel);
                counter += 1;
                if (counter >= multiplicity) trigger = true;
                coherenceMap.put(logicalChannel, counter);
            }
        }
        
        // No trigger so skip next operation
        if (!trigger) return false;
        
        // Prune hits not in spatial cluster out of queue as these
        // will be built into trigger very soon.
        for (Iterator<IHitPayload> hitIt = triggerQueue.iterator(); hitIt.hasNext(); )
        {
            IHitPayload hit = hitIt.next();
            long numericMBID = hit.getDOMID().longValue();
            String mbid      = String.format("%012x", numericMBID);
            int stringNumber = domRegistry.getStringMajor(mbid);
            int moduleNumber = domRegistry.getStringMinor(mbid);
            int logicalChannel = 64 * stringNumber + moduleNumber;
            if (coherenceMap.get(logicalChannel) < multiplicity) hitIt.remove();
        }
        
        return true;
    }

    public boolean isConfigured()
    {
        // TODO Auto-generated method stub
        return true;
    }

}
