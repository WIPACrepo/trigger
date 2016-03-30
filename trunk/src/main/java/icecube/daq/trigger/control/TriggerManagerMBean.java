package icecube.daq.trigger.control;

import java.util.Map;

/**
 * Definition of all methods used for monitoring the trigger manager.
 */
public interface TriggerManagerMBean
{
    /**
     * Get the number of dropped SNDAQ alerts
     *
     * @return number of dropped SNDAQ alerts
     */
    long getSNDAQAlertsDropped();

    /**
     * Get the number of SNDAQ alerts queued for writing
     *
     * @return number of queued SNDAQ alerts
     */
    int getSNDAQAlertsQueued();

    /**
     * Get the number of SNDAQ alerts sent to SNDAQ
     *
     * @return number of SNDAQ alerts
     */
    long getSNDAQAlertsSent();

    /**
     * Get the number of requests queued for writing
     *
     * @return size of output queue
     */
    int getNumOutputsQueued();

    /**
     * Get the total number of hits pushed onto the input queue
     *
     * @return total number of hits received from the splicer
     */
    long getTotalProcessed();

    /**
     * Get any special monitoring quantities for all algorithms.
     *
     * @return map of {name-configID-quantity: quantityObject}
     */
    Map<String, Object> getTriggerMonitorMap();
}
