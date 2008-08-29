package icecube.daq.trigger.control;

import icecube.daq.trigger.ITriggerRequestPayload;

import java.util.Comparator;

/**
 * Sort triggers by times.
 */
public class TriggerComparator
    implements Comparator
{
    /**
     * Compare two triggers.
     */
    public int compare(Object o1, Object o2)
    {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }

            return 1;
        } else if (o2 == null) {
            return -1;
        } else if (!(o1 instanceof ITriggerRequestPayload)) {
            if (!(o2 instanceof ITriggerRequestPayload)) {
                final String name1 = o1.getClass().getName();
                return name1.compareTo(o2.getClass().getName());
            }

            return 1;
        } else if (!(o2 instanceof ITriggerRequestPayload)) {
            return -1;
        }

        ITriggerRequestPayload tr1 = (ITriggerRequestPayload) o1;
        ITriggerRequestPayload tr2 = (ITriggerRequestPayload) o2;

        int val = tr1.getFirstTimeUTC().compareTo(tr2.getFirstTimeUTC());
        if (val == 0) {
            val = tr1.getLastTimeUTC().compareTo(tr2.getLastTimeUTC());
            if (val == 0) {
                val = tr2.getUID() - tr1.getUID();
            }
        }

        return val;
    }

    /**
     * Is the specified object a member of the same class?
     *
     * @return <tt>true</tt> if specified object matches this class
     */
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }

        return getClass().equals(obj.getClass());
    }

    /**
     * Get sorter hash code.
     *
     * @return hash code for this class.
     */
    public int hashCode()
    {
        return getClass().hashCode();
    }
}
