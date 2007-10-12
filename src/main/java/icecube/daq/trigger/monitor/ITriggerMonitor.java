/*
 * interface: ITriggerMonitor
 *
 * Version $Id: ITriggerMonitor.java 2125 2007-10-12 18:27:05Z ksb $
 *
 * Date: August 22 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.monitor;

/**
 * This interface defines the control aspect of a trigger.
 *
 * @version $Id: ITriggerMonitor.java 2125 2007-10-12 18:27:05Z ksb $
 * @author pat
 */
public interface ITriggerMonitor
{

    String toString();

    int getTriggerCounter();

    boolean isOnTrigger();

    TriggerMonitor getTriggerMonitor();

}
