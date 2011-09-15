package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.PayloadException;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.util.DOMRegistry;
import icecube.daq.util.DeployedDOM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
 * The VolumeTrigger is based on the ClusterTrigger, with a slight
 * modifications to allow clusters to span neighboring strings.  The
 * VolumeTrigger is intended for use on a central trigger module which may have
 * inputs from multiple strings.  The trigger searches for N hits clustered in
 * a "coherence" length of M adjacent modules all within a time window of
 * &Delta;t.
 * The trigger is configured via the standard trigger config XML.  It will
 * respond to the following configuration parameters ...
 * <dl>
 * <dt>timeWindow</dt>
 * <dd>The time window, &Delta;t, expressed in units of ns.</dd>
 * <dt>coherenceLength</dt>
 * <dd>The parameter M above - the cluster spatial search window.</dd>
 * <dt>multiplicity</dt>
 * <dd>The parameter M above - the multiplicity threshold.</dd>
 * </dl>
 *
 * The implementation is straightforward.  First the overall multiplicty
 * requirement must be satisfied: hits are collected into a queue until the
 * head and tail fall outside the time window.  If the queue size is &ge; N
 * then that forms the 'first-level trigger.' Upon reaching this state, it must
 * be checked whether the hits are clustered in space. This is done by
 * incrementing counters a length M/2 in either direction from the
 * <i>logical channel</i>, allowing for counts on neighboring strings.  A space
 * cluster will also maintain counter[i] &ge; N for one or more
 * <i>logical channel</i> locations. <p>
 * The trigger that is formed should <i>only</i> contain those hits which are
 * part of a space cluster.  That is, hits are not part of the trigger hit list
 * unless they are clustered both in time and in space.  Simultaneous, multiple
 * clusters will count toward a single single trigger and will not produce
 * multiple triggers.
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

    private static final Logger logger =
        Logger.getLogger(CylinderTrigger.class);

    public CylinderTrigger()
    {
        triggerQueue    = new LinkedList<IHitPayload>();

        setMultiplicity(5);
        setSimpleMultiplicity(10);
        setTimeWindow(1500L);
        setRadius(150.0);
        setHeight(100.0);
    }

    @Override
    public void addParameter(TriggerParameter parameter)
        throws UnknownParameterException,
            IllegalParameterValueException
    {
        if (parameter.getName().equals("timeWindow")) {
            setTimeWindow(Long.parseLong(parameter.getValue()));
        } else if (parameter.getName().equals("multiplicity")) {
            setMultiplicity(Integer.parseInt(parameter.getValue()));
        } else if (parameter.getName().equals("simpleMultiplicity")) {
            setSimpleMultiplicity(Integer.parseInt(parameter.getValue()));
        } else if (parameter.getName().equals("radius")) {
            setRadius(Double.parseDouble(parameter.getValue()));
        } else if (parameter.getName().equals("height")) {
            setHeight(Double.parseDouble(parameter.getValue()));
        } else if (parameter.getName().equals("domSet")) {
            domSetId = Integer.parseInt(parameter.getValue());
            configHitFilter(domSetId);
        }
        super.addParameter(parameter);
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
            throw new TriggerException(
                "Payload object " + payload +
                " cannot be upcast to IHitPayload." );
        }
        // This upcast should be safe now
        IHitPayload hitPayload = (IHitPayload) payload;

        // Check hit type and perhaps pre-screen DOMs based on channel
        // (HitFilter)
        if (getHitType(hitPayload) != AbstractTrigger.SPE_HIT) {
            return;
        }
        if (!hitFilter.useHit(hitPayload)) {
            return;
        }

        while (triggerQueue.size() > 0 &&
            hitPayload.getHitTimeUTC().longValue() -
            triggerQueue.element().getHitTimeUTC().longValue() > timeWindow)
        {
            if (triggerQueue.size() >= multiplicity && processHitQueue()) {
                if (triggerQueue.size() > 0) {
                    try {
                        formTrigger(triggerQueue, null, null);
                    } catch (PayloadException pe) {
                        throw new TriggerException("Cannot form trigger", pe);
                    }
                }
                triggerQueue.clear();
                setEarliestPayloadOfInterest(hitPayload);
                break;
            } else {
                triggerQueue.removeFirst();
                IHitPayload firstHitInQueue = triggerQueue.peek();
                if (firstHitInQueue == null) {
                    firstHitInQueue = hitPayload;
                }
                setEarliestPayloadOfInterest(firstHitInQueue);
            }
        }
        triggerQueue.add(hitPayload);
    }

    private boolean processHitQueue()
    {
        if (triggerQueue.size() >= simpleMultiplicity) {
            return true;
        }
        IHitPayload[] q = triggerQueue.toArray(new IHitPayload[0]);

        final DOMRegistry domRegistry = getTriggerHandler().getDOMRegistry();

        ArrayList<IHitPayload> hitsInCylinder =
            new ArrayList<IHitPayload>(q.length*(q.length-1));

        // Loop over hit pairs
        for (int ihit = 0; ihit < q.length; ihit++) {
            String mbid0 = String.format("%012x", q[ihit].getDOMID().
                longValue());
            DeployedDOM d0 = domRegistry.getDom(mbid0);
            hitsInCylinder.clear();
            hitsInCylinder.add(q[ihit]);
            for (int jhit = 0; jhit < q.length; jhit++) {
                if (ihit == jhit) {
                    continue;
                }
                String mbid1 = String.format("%012x", q[jhit].getDOMID().
                    longValue());
                DeployedDOM d1 = domRegistry.getDom(mbid1);
                double dx = d1.getX() - d0.getX();
                double dy = d1.getY() - d0.getY();
                double dz = d1.getZ() - d0.getZ();
                double r  = dx * dx + dy * dy;
                if (r < radius2 && Math.abs(dz) < 0.5*height) {
                    hitsInCylinder.add(q[jhit]);
                }
            }
            if (hitsInCylinder.size() >= multiplicity) {
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
