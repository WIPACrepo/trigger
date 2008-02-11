/*
 * class: TimeOutOfOrderException
 *
 * Version $Id: UnknownParameterException.java 2629 2008-02-11 05:48:36Z dglo $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.exceptions;


/**
 * This class provides a specific exception
 *
 * @version $Id: UnknownParameterException.java 2629 2008-02-11 05:48:36Z dglo $
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
