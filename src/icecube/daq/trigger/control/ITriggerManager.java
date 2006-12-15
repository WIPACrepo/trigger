/*
 * interface: ITriggerManager
 *
 * Version $Id: ITriggerManager.java,v 1.7 2006/05/07 17:17:41 toale Exp $
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
 * @version $Id: ITriggerManager.java,v 1.7 2006/05/07 17:17:41 toale Exp $
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
