package icecube.daq.trigger.exceptions;

/**
 * Used to mark unimplemented code.
 */
public class UnimplementedError
    extends Error
{
    /**
     * Create an unimplemented code error.
     */
    public UnimplementedError()
    {
        super("Unimplemented");
    }

    /**
     * Create an unimplemented code error.
     *
     * @param msg error message
     */
    public UnimplementedError(String msg)
    {
        super(msg);
    }
}
