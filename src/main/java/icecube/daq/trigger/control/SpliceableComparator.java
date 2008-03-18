package icecube.daq.trigger.control;

import icecube.daq.splicer.Spliceable;

import java.util.Comparator;

/**
 * Sort payloads using the compareSpliceable() method.
 */
public class SpliceableComparator
    implements Comparator
{
    /**
     * Basic constructor.
     */
/*
    public SpliceableComparator()
    {
    }
*/

    /**
     * Compare two spliceables.
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
        } else if (!(o1 instanceof Spliceable)) {
            if (!(o2 instanceof Spliceable)) {
                final String name1 = o1.getClass().getName();
                return name1.compareTo(o2.getClass().getName());
            }

            return 1;
        } else if (!(o2 instanceof Spliceable)) {
            return -1;
        } else {
            return ((Spliceable) o1).compareSpliceable((Spliceable) o2);
        }
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
