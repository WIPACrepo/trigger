/*
 * class: TimeOutOfOrderException
 *
 * Version $Id: IllegalParameterValueException.java,v 1.1 2005/12/06 22:29:54 toale Exp $
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
 * @version $Id: IllegalParameterValueException.java,v 1.1 2005/12/06 22:29:54 toale Exp $
 * @author pat
 */
public class IllegalParameterValueException
        extends TriggerException
{

    /**
     * default constructor
     */
    IllegalParameterValueException() {
    }

    /**
     * constructor taking a message
     * @param message message associated with this exception
     */
    public IllegalParameterValueException(String message) {
        super(message);
    }

    /**
     * constructor taking an exception
     * @param exception the exception
     */
    public IllegalParameterValueException(Exception exception) {
        super(exception);
    }

}
