/*
 * interface: ITriggerManager
 *
 * Version $Id: ITriggerManager.java 2247 2007-11-06 16:57:04Z dglo $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.IUTCTime;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;

/**
 * This interface defines the behavior of a TriggerManager
 *
 * @version $Id: ITriggerManager.java 2247 2007-11-06 16:57:04Z dglo $
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
