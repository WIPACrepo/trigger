/*
 * class: ConfigException
 *
 * Version $Id: ConfigException.java 2125 2007-10-12 18:27:05Z ksb $
 *
 * Date: November 13 2004
 *
 * (c) 2004 IceCube Collaboration
 */

package icecube.daq.trigger.exceptions;

/**
 * This class defines a trigger configuration exception
 *
 * @version $Id: ConfigException.java 2125 2007-10-12 18:27:05Z ksb $
 * @author pat
 */
public class ConfigException
    extends TriggerException
{

    /**
     * default constructor
     */
    ConfigException()
    {
    }

    /**
     * constructor taking a message
     * @param message message associated with this exception
     */
    public ConfigException(String message)
    {
        super(message);
    }

    /**
     * constructor taking an exception
     * @param exception the exception
     */
    public ConfigException(Exception exception)
    {
        super(exception);
    }

    /**
     * constructor taking a message and an exception
     * @param message message associated with this exception
     * @param exception the exception
     */
    public ConfigException(String message, Exception exception)
    {
        super(message, exception);
    }
}
