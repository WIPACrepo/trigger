/*
 * class: TriggerParameter
 *
 * Version $Id: TriggerParameter.java,v 1.1 2005/11/23 16:37:37 toale Exp $
 *
 * Date: November 23 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.config;

/**
 * This class encapsulates a name/value pair that is a trigger parameter.
 *
 * @version $Id: TriggerParameter.java,v 1.1 2005/11/23 16:37:37 toale Exp $
 * @author pat
 */
public class TriggerParameter
{

    /**
     * Parameter name.
     */
    private String name;

    /**
     * Parameter value.
     */
    private String value;

    /**
     * Default constructor.
     * Name/value pair are set to null.
     */
    public TriggerParameter() {
        this(null, null);
    }

    /**
     * Constructor that takes a known name/value pair.
     * @param name parameter name
     * @param value parameter value
     */
    public TriggerParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Get parameter name.
     * @return name of parameter
     */
    public String getName() {
        return name;
    }

    /**
     * Set parameter name.
     * @param name name of parameter
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get parameter value.
     * @return value of parameter
     */
    public String getValue() {
        return value;
    }

    /**
     * Set parameter value.
     * @param value value of parameter
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Override to print out name/value pair.
     * @return parameter as a string
     */
    public String toString() {
        return (name + " = " + value);
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object object) {
        if (object == null) {
            return false;
        } else if (!(object instanceof TriggerParameter)) {
            return false;
        } else if (object.hashCode() != this.hashCode()) {
            return false;
        } else {
            return true;
        }
    }

}
