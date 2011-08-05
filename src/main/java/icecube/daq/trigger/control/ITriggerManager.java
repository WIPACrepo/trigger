/*
 * interface: ITriggerManager
 *
 * Version $Id: ITriggerManager.java 12777 2011-03-14 22:32:59Z dglo $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.splicer.Splicer;

/**
 * This interface defines the behavior of a TriggerManager
 *
 * @version $Id: ITriggerManager.java 12777 2011-03-14 22:32:59Z dglo $
 * @author pat
 */
public interface ITriggerManager
    extends ITriggerHandler, AdvancedSplicedAnalysis
{

    Splicer getSplicer();

    IUTCTime getEarliestTime();

    IUTCTime getLatestTime();

    long getAverageProcessingTime();

    long getMinProcessingTime();

    long getMaxProcessingTime();

    int getNumOutputsQueued();

    int getProcessingCount();

    IPayload getEarliestPayloadOfInterest();

    void setOutputFactory(TriggerRequestPayloadFactory factory);
}
