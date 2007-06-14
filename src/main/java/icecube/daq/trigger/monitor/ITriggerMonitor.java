/*
 * interface: ITriggerMonitor
 *
 * Version $Id: ITriggerMonitor.java,v 1.3 2005/11/22 21:30:31 toale Exp $
 *
 * Date: August 22 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.monitor;

/**
 * This interface defines the control aspect of a trigger.
 *
 * @version $Id: ITriggerMonitor.java,v 1.3 2005/11/22 21:30:31 toale Exp $
 * @author pat
 */
public interface ITriggerMonitor
{

    String toString();

    int getTriggerCounter();

    boolean isOnTrigger();

    TriggerMonitor getTriggerMonitor();

}
