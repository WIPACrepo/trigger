package icecube.daq.trigger.config;

import icecube.daq.payload.IDOMID;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: pat
 * Date: Sep 13, 2006
 * Time: 10:43:45 AM
 *
 *
 * This class is a container of domId's used to filter hits in the triggers.
 * The object is created with a name (like 'Inice doms') and a list of mainboard
 * id's (must be lowercase hex string). Then one can check that a given IDOMID is
 * or isn't contained in the set.
 *
 */
public class DomSet
{

    /**
     * Name of this DomSet
     */
    private final String name;

    /**
     * List of doms in this DomSet
     */
    private final HashSet<String> set;

    /**
     * Constructor, takes the name of the set and the list of domid's
     * @param name name of domset
     * @param set list of domIds, must be lowercase hex
     */
    public DomSet(String name, Collection<String> set) {
        this.name = name;
        this.set = new HashSet<String>(7500);
        this.set.addAll(set);
    }

    /**
     * Compare this DomSet with another DomSet.
     *
     * @param other DomSet being compared
     *
     * @return <tt>true</tt> if both sets contain the same DOM IDs
     */
    public boolean equals(Object other)
    {
	if (this==other) {
	    return true;
	}
	
	if ((other==null) || (!(other instanceof DomSet))) {
	    return false;
	}

	DomSet other_d = (DomSet)other;
	
        return set.size() == other_d.set.size() &&
            set.containsAll(other_d.set);
    }

    /**
     * Get the name of this DomSet
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Check if a given domId is in the DomSet
     * @param dom IDOMID to check
     * @return true if dom is in set, false otherwise
     */
    public boolean inSet(IDOMID dom) {
        String domId = dom.toString().toLowerCase();
        return set.contains(domId);
    }

    public String toString()
    {
        return name + "*" + set.size();
    }
}
