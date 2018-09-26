/*
 * class: TriggerParameter
 *
 * Version $Id: TriggerParameter.java 17114 2018-09-26 09:51:56Z dglo $
 *
 * Date: November 23 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.config;

/**
 * This class encapsulates a name/value pair that is a trigger parameter.
 *
 * @version $Id: TriggerParameter.java 17114 2018-09-26 09:51:56Z dglo $
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
    public TriggerParameter()
    {
        this(null, null);
    }

    /**
     * Constructor that takes a known name/value pair.
     * @param name parameter name
     * @param value parameter value
     */
    public TriggerParameter(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    /**
     * Compare this object against <tt>object</tt>
     *
     * @param object object being checked
     *
     * @return <tt>true</tt> if the objects are equal
     */
    @Override
    public boolean equals(Object object)
    {
        return object != null && (object instanceof TriggerParameter) &&
            object.hashCode() == hashCode();
    }

    /**
     * Get parameter name.
     * @return name of parameter
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get parameter value.
     * @return value of parameter
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Hashcode for this parameter.
     *
     * @return hash code
     */
    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }

    /**
     * Set parameter name.
     * @param name name of parameter
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Set parameter value.
     * @param value value of parameter
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * Override to print out name/value pair.
     * @return parameter as a string
     */
    @Override
    public String toString()
    {
        return (name + " = " + value);
    }
}
