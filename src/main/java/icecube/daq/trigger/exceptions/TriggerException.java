/*
 * class: TriggerException
 *
 * Version $Id: TriggerException.java,v 1.1 2005/12/06 22:29:54 toale Exp $
 *
 * Date: November 13 2004
 *
 * (c) 2004 IceCube Collaboration
 */

package icecube.daq.trigger.exceptions;

/**
 * This class defines a general trigger exception
 *
 * @version $Id: TriggerException.java,v 1.1 2005/12/06 22:29:54 toale Exp $
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

}
