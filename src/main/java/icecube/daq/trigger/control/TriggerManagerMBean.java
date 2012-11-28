package icecube.daq.trigger.control;

import java.util.Map;

public interface TriggerManagerMBean
{
    int getNumInputsQueued();
    int getNumOutputsQueued();
    Map<String, Long> getTriggerCounts();
    Map<String, Object> getTriggerMonitorMap();
}
