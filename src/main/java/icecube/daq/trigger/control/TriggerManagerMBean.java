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

import java.util.Map;

public interface TriggerManagerMBean
{
    Map<String, Long> getTriggerCounts();
}
