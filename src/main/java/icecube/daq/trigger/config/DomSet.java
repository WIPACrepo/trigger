package icecube.daq.trigger.config;

import icecube.daq.payload.IDOMID;

import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: pat
 * Date: Sep 13, 2006
 * Time: 10:43:45 AM
 *
 * This class is a container of DOM IDs used to filter hits in the triggers.
 * The object is created with a name (like 'Inice doms') and a list of
 * mainboard IDs (must be lowercase hex string). Then one can check that a
 * given DOM ID is or isn't contained in the set.
 *
 */
public class DomSet
{
    private static final Logger LOG = Logger.getLogger(DomSet.class);

    /**
     * Name of this DomSet
     */
    private final String name;

    /**
     * List of doms in this DomSet
     */
    private final HashSet<Long> set;

    /**
     * Constructor, takes the name of the set and the list of domid's
     * @param name name of domset
     * @param set list of domIds, must be lowercase hex
     */
    public DomSet(String name, Collection<Long> set)
    {
        this.name = name;
        this.set = new HashSet<Long>(set);
    }

    /**
     * Compare this DomSet with another object.
     *
     * @param other object being compared
     *
     * @return <tt>true</tt> if both sets contain the same DOM IDs
     */
    public boolean equals(Object other)
    {
        if (other == null) {
            return false;
        }

        if (!(other instanceof DomSet)) {
            return getClass().equals(other.getClass());
        }

        return equals((DomSet) other);
    }

    /**
     * Compare this DomSet with another DomSet.
     *
     * @param other DomSet being compared
     *
     * @return <tt>true</tt> if both sets contain the same DOM IDs
     */
    public boolean equals(DomSet other)
    {
        return other != null && set.size() == other.set.size() &&
            set.containsAll(other.set);
    }

    /**
     * Get the name of this DomSet
     * @return name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the hash code.
     *
     * @return hash code
     */
    public int hashCode()
    {
        return name.hashCode() + set.hashCode();
    }

    /**
     * Check if a given domId is in the DomSet
     * @param dom IDOMID to check
     * @return true if dom is in set, false otherwise
     */
    public boolean inSet(IDOMID dom)
    {
        if (dom == null) {
            return false;
        }

        return set.contains(dom.longValue());
    }

    /**
     * Return a debugging string.
     *
     * @return debugging string
     */
    public String toString()
    {
        return name + "*" + set.size();
    }
}
