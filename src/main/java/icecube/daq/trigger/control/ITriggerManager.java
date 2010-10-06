/*
 * interface: ITriggerManager
 *
 * Version $Id: ITriggerManager.java 12315 2010-10-06 21:27:41Z dglo $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.IUTCTime;
import icecube.daq.splicer.Splicer;

/**
 * This interface defines the behavior of a TriggerManager
 *
 * @version $Id: ITriggerManager.java 12315 2010-10-06 21:27:41Z dglo $
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

    void setOutputFactory(TriggerRequestPayloadFactory factory);
}
