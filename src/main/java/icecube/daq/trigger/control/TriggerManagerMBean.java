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
    Map<String, Integer> getQueuedInputs();

    /**
     * Return a map of algorithm names to the time of their most recently
     * released request.  This can be useful for determining which algorithm
     * is causing the trigger output stream to stall.
     *
     * @return map of names to times
     */
    Map<String, Long> getReleaseTimes();

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
     * Get the total number of hits pushed onto the input queue
     *
     * @return total number of hits received from the splicer
     */
    long getTotalProcessed();

    /**
     * Get the number of requests collected from all algorithms
     *
     * @return total number of collected requests
     */
    int getTotalRequestsCollected();

    /**
     * Get the number of requests released for collection
     *
     * @return total number of released requests
     */
    int getTotalRequestsReleased();

    /**
     * Get any special monitoring quantities for all algorithms.
     *
     * @return map of {name-configID-quantity: quantityObject}
     */
    Map<String, Object> getTriggerMonitorMap();
}
