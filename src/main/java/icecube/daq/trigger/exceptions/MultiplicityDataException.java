package icecube.daq.trigger.exceptions;

public class MultiplicityDataException
    extends TriggerException
{
    /**
     * Report a problem with the MultiplicityDataManager.
     *
     * @param msg error message
     */
    public MultiplicityDataException(String msg)
    {
        super(msg);
    }

    /**
     * Report a problem with the MultiplicityDataManager.
     *
     * @param msg error message
     * @param ex encapsulated exception
     */
    public MultiplicityDataException(String msg, Exception ex)
    {
        super(msg, ex);
    }
}
