/*
 * class: TimeOutOfOrderException
 *
 * Version $Id: UnknownParameterException.java 2125 2007-10-12 18:27:05Z ksb $
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
 * @version $Id: UnknownParameterException.java 2125 2007-10-12 18:27:05Z ksb $
 * @author pat
 */
public class UnknownParameterException
        extends TriggerException
{

    /**
     * default constructor
     */
    UnknownParameterException() {
    }

    /**
     * constructor taking a message
     * @param message message associated with this exception
     */
    public UnknownParameterException(String message) {
        super(message);
    }

    /**
     * constructor taking an exception
     * @param exception the exception
     */
    public UnknownParameterException(Exception exception) {
        super(exception);
    }

}
