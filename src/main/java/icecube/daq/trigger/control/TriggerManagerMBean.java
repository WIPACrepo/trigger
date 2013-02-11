package icecube.daq.trigger.control;

import java.util.Map;

/**
 * Definition of all methods used for monitoring the trigger manager.
 */
public interface TriggerManagerMBean
{

    /**
     * Get the number of requests queued for writing
     *
     * @return size of output queue
     */
    int getNumOutputsQueued();

    /**
     * Get map of trigger names to number of queued hits
     *
     * @return map of {name : numQueuedHits}
     */
    Map<String, Integer> getQueuedInputsMap();

    /**
     * Get the total number of hits pushed onto the input queue
     *
     * @return total number of hits received from the splicer
     */
    long getTotalProcessed();

    /**
     * Get map of trigger names to number of issued requests
     *
     * @return map of {name : numRequests}
     */
    Map<String, Long> getTriggerCounts();

    /**
     * Get any special monitoring quantities for all algorithms.
     *
     * @return map of {name-configID-quantity: quantityObject}
     */
    Map<String, Object> getTriggerMonitorMap();
}
