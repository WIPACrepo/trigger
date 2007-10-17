/*
 * interface: ITriggerManager
 *
 * Version $Id: ITriggerManager.java 2125 2007-10-12 18:27:05Z ksb $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.splicer.Splicer;
import icecube.daq.payload.IUTCTime;

/**
 * This interface defines the behavior of a TriggerManager
 *
 * @version $Id: ITriggerManager.java 2125 2007-10-12 18:27:05Z ksb $
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

}
