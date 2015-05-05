package icecube.daq.trigger.control;

import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.trigger.exceptions.MultiplicityDataException;

public interface IMonitoringDataManager
{
    void add(ITriggerRequestPayload req)
        throws MultiplicityDataException;

    void reset()
        throws MultiplicityDataException;

    boolean sendFinal()
        throws MultiplicityDataException;

    /**
     * Send a single bin of data
     *
     * @param isFinal <tt>true</tt> if this is the final bin
     *
     * @throws MultiplicityDataException if there is a problem
     */
    boolean sendSingleBin(boolean isFinal)
        throws MultiplicityDataException;
}
