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
import icecube.daq.util.DeployedDOM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

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
public class CylinderTrigger extends AbstractTrigger
{
    private long timeWindow;
    private int multiplicity;
    private int simpleMultiplicity;
    private double radius, radius2;
    private double height;

    private LinkedList<IHitPayload> triggerQueue;

    private Comparator hitComparator = new HitComparator();

    public CylinderTrigger()
    {
        triggerQueue    = new LinkedList<IHitPayload>();

        setMultiplicity(5);
        setSimpleMultiplicity(10);
        setTimeWindow(1500L);
        setRadius(150.0);
        setHeight(100.0);
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
        else if (name.equals("simpleMultiplicity"))
            setSimpleMultiplicity(Integer.parseInt(value));
        else if (name.equals("radius"))
            setRadius(Double.parseDouble(value));
        else if (name.equals("height"))
            setHeight(Double.parseDouble(value));
        else if (name.equals("domSet"))
        {
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

    public int getSimpleMultiplicity()
    {
        return simpleMultiplicity;
    }

    public void setSimpleMultiplicity(int simpleMultiplicity)
    {
        this.simpleMultiplicity = simpleMultiplicity;
    }

    public double getRadius()
    {
        return radius;
    }

    public void setRadius(double radius)
    {
        this.radius = radius;
        this.radius2 = radius * radius;
    }

    public double getHeight()
    {
        return height;
    }

    public void setHeight(double height)
    {
        this.height = height;
    }

    public long getTimeWindow()
    {
        return timeWindow / 10L;
    }

    public void setTimeWindow(long lval)
    {
        timeWindow = lval * 10L;
    }

    public int getMultiplicity()
    {
        return multiplicity;
    }

    public void setMultiplicity(int val)
    {
        multiplicity = val;
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
        if (triggerQueue.size() >= simpleMultiplicity) return true;
        IHitPayload[] q = triggerQueue.toArray(new IHitPayload[0]);

        final DOMRegistry domRegistry = getTriggerHandler().getDOMRegistry();

        ArrayList<IHitPayload> hitsInCylinder =
            new ArrayList<IHitPayload>(q.length*(q.length-1));

        // Loop over hit pairs
        for (int ihit = 0; ihit < q.length; ihit++)
        {
            String mbid0 = String.format("%012x", q[ihit].getDOMID().longValue());
            DeployedDOM d0 = domRegistry.getDom(mbid0);
            if (d0 == null) {
                throw new Error("Cannot find DOM " + mbid0);
            }

            hitsInCylinder.clear();
            hitsInCylinder.add(q[ihit]);
            for (int jhit = 0; jhit < q.length; jhit++)
            {
                if (ihit == jhit) continue;
                String mbid1 = String.format("%012x", q[jhit].getDOMID().longValue());
                DeployedDOM d1 = domRegistry.getDom(mbid1);
                if (d1 == null) {
                    throw new Error("Cannot find DOM " + mbid1);
                }

                double dx = d1.getX() - d0.getX();
                double dy = d1.getY() - d0.getY();
                double dz = d1.getZ() - d0.getZ();
                double r  = dx * dx + dy * dy;
                if (r < radius2 && Math.abs(dz) < 0.5*height) hitsInCylinder.add(q[jhit]);
            }
            if (hitsInCylinder.size() >= multiplicity)
            {
                Collections.sort((ArrayList) hitsInCylinder, hitComparator);
                triggerQueue = new LinkedList<IHitPayload>(hitsInCylinder);
                return true;
            }
        }

        return false;
    }

    public boolean isConfigured()
    {
        return true;
    }

}

class HitComparator
    implements Comparator
{
    public int compare(Object o1, Object o2)
    {
        if (o1 == null || !(o1 instanceof IHitPayload)) {
            if (o2 == null || !(o2 instanceof IHitPayload)) {
                return 0;
            }

            return 1;
        } else if (o2 == null || !(o2 instanceof IHitPayload)) {
            return -1;
        }

        IHitPayload h1 = (IHitPayload) o1;
        IHitPayload h2 = (IHitPayload) o2;

        if (h1.getHitTimeUTC() == null) {
            if (h2.getHitTimeUTC() == null) {
                return 0;
            }

            return 1;
        } else if (h2.getHitTimeUTC() == null) {
            return -1;
        }

        long val = h1.getHitTimeUTC().longValue() -
            h2.getHitTimeUTC().longValue();
        if (val > 0) {
            return 1;
        } else if (val < 0) {
            return -1;
        }

        return 0;
    }

    public boolean equals(Object obj)
    {
        return obj instanceof HitComparator;
    }
}
