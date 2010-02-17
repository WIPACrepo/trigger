package icecube.daq.trigger.control;

import java.util.Map;

public interface TriggerManagerMBean
{
    Map<String, Long> getTriggerCounts();
}
