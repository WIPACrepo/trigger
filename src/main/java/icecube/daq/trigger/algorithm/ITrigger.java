package icecube.daq.trigger.algorithm;

import icecube.daq.trigger.config.ITriggerConfig;
import icecube.daq.trigger.control.ITriggerControl;
import icecube.daq.trigger.monitor.ITriggerMonitor;

public interface ITrigger
    extends ITriggerConfig, ITriggerControl, ITriggerMonitor
{
}
