/*
 * class: TimeOutOfOrderException
 *
 * Version $Id: TimeOutOfOrderException.java 2125 2007-10-12 18:27:05Z ksb $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.exceptions;

import icecube.daq.trigger.exceptions.TriggerException;

/**
 * This class provides a specific exception
 *
 * @version $Id: TimeOutOfOrderException.java 2125 2007-10-12 18:27:05Z ksb $
 * @author pat
 */
public class TimeOutOfOrderException
        extends TriggerException
{

    /**
     * default constructor
     */
    TimeOutOfOrderException() {
    }

    /**
     * constructor taking a message
     * @param message message associated with this exception
     */
    public TimeOutOfOrderException(String message) {
        super(message);
    }

    /**
     * constructor taking an exception
     * @param exception the exception
     */
    public TimeOutOfOrderException(Exception exception) {
        super(exception);
    }

}
