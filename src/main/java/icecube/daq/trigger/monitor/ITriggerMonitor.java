/*
 * interface: ITriggerMonitor
 *
 * Version $Id: ITriggerMonitor.java 14073 2012-11-28 18:56:56Z dglo $
 *
 * Date: August 22 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.monitor;

import java.util.Map;

/**
 * This interface defines the control aspect of a trigger.
 *
 * @version $Id: ITriggerMonitor.java 14073 2012-11-28 18:56:56Z dglo $
 * @author pat
 */
public interface ITriggerMonitor
{

    String toString();

    int getTriggerCounter();

    boolean isOnTrigger();

    TriggerMonitor getTriggerMonitor();

    Map<String, Object> getTriggerMonitorMap();
}
