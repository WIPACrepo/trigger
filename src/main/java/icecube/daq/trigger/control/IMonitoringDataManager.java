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

    boolean sendSingleBin()
        throws MultiplicityDataException;
}
