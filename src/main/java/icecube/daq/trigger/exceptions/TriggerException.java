/*
 * class: TriggerException
 *
 * Version $Id: TriggerException.java 13231 2011-08-05 22:45:36Z dglo $
 *
 * Date: November 13 2004
 *
 * (c) 2004 IceCube Collaboration
 */

package icecube.daq.trigger.exceptions;

/**
 * This class defines a general trigger exception
 *
 * @version $Id: TriggerException.java 13231 2011-08-05 22:45:36Z dglo $
 * @author pat
 */
public class TriggerException extends Exception
{

    /**
     * default constructor
     */
    TriggerException() {
    }

    /**
     * constructor taking a message
     * @param message message associated with this exception
     */
    public TriggerException(String message) {
        super(message);
    }

    /**
     * constructor taking an exception
     * @param exception the exception
     */
    public TriggerException(Exception exception) {
        super(exception);
    }

    /**
     * constructor taking a message
     * @param message message associated with this exception
     * @param exception the exception
     */
    public TriggerException(String message, Exception exception) {
        super(message, exception);
    }

}
