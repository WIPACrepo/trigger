package icecube.daq.trigger.control;

import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.exceptions.UnimplementedError;

import java.util.List;

/**
 * This class provides the earliest/latest UTC-timeStamp.
 */
public class Sorter
{
    /**
     * Create a sorter object.
     */
    public Sorter()
    {
    }

    /**
     * XXX Unimplemented
     *
     * @param list unused
     *
     * @return UnimplementedError
     */
    public IUTCTime getUTCTimeEarliest(List list)
    {
        return getUTCTimeEarliest(list, false);
    }

    /**
     * XXX Unimplemented
     *
     * @param list unused
     * @param isPayloadObject unused
     *
     * @return UnimplementedError
     */
    public IUTCTime getUTCTimeEarliest(List list, boolean isPayloadObject)
    {
        throw new UnimplementedError();
    }

    /**
     * XXX Unimplemented
     *
     * @param list unused
     *
     * @return UnimplementedError
     */
    public IUTCTime getUTCTimeLatest(List list)
    {
        return getUTCTimeLatest(list, false);
    }

    /**
     * XXX Unimplemented
     *
     * @param list unused
     * @param isPayloadObject unused
     *
     * @return UnimplementedError
     */
    public IUTCTime getUTCTimeLatest(List list, boolean isPayloadObject)
    {
        throw new UnimplementedError();
    }
}
