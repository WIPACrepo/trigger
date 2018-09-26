package icecube.daq.trigger.config;

import icecube.daq.payload.IDOMID;
import icecube.daq.util.DOMInfo;

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
    /**
     * Name of this DomSet
     */
    private final String name;

    /**
     * List of dom IDs in this DomSet
     */
    private final HashSet<Long> domIds;

    /**
     * List of channel IDs in this DomSet
     */
    private final HashSet<Short> chanIds;

    /**
     * Constructor, takes the name of the set and the list of domid's
     * @param name name of domset
     * @param set list of domIds, must be lowercase hex
     */
    public DomSet(String name)
    {
        this.name = name;
        this.domIds = new HashSet<Long>();
        this.chanIds = new HashSet<Short>();
    }

    public void add(DOMInfo dom)
    {
        domIds.add(dom.getNumericMainboardId());
        chanIds.add(dom.getChannelId());
    }

    /**
     * Compare this DomSet with another object.
     *
     * @param other object being compared
     *
     * @return <tt>true</tt> if both sets contain the same DOM IDs
     */
    @Override
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
        return other != null &&
            domIds.size() == other.domIds.size() &&
            domIds.containsAll(other.domIds) &&
            chanIds.size() == other.chanIds.size() &&
            chanIds.containsAll(other.chanIds);
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
    @Override
    public int hashCode()
    {
        return name.hashCode() + domIds.hashCode() + chanIds.hashCode();
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

        return domIds.contains(dom.longValue());
    }

    /**
     * Check if a given channel ID is in the DomSet
     * @param chanId channel ID to check
     * @return true if dom is in set, false otherwise
     */
    public boolean inSet(short chanId)
    {
        return chanIds.contains(chanId);
    }

    /**
     * Return the number of DOMs in this set.
     *
     * @return number of DOMs
     */
    public int size()
    {
        return chanIds.size();
    }

    /**
     * Return a debugging string.
     *
     * @return debugging string
     */
    @Override
    public String toString()
    {
        return name + "*" + chanIds.size();
    }
}
