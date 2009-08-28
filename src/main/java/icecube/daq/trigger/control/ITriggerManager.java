/*
 * interface: ITriggerManager
 *
 * Version $Id: ITriggerManager.java 4574 2009-08-28 21:32:32Z dglo $
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
 * @version $Id: ITriggerManager.java 4574 2009-08-28 21:32:32Z dglo $
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

    int getProcessingCount();

    void setOutputFactory(TriggerRequestPayloadFactory factory);
}
