package icecube.daq.trigger.algorithm;

public interface AbstractTriggerMBean
{
    /**
     * Get the earliest event time of interest for this algorithm.
     *
     * @return earliest UTC time
     */
    long getEarliestTime();

    /**
     * Get the input queue size.
     *
     * @return input queue size
     */
    int getInputQueueSize();

    /**
     * Return the difference between the start of the first cached request
     * and the earliest payload of interest (in DAQ ticks).
     *
     * @return latency in DAQ ticks
     */
    long getLatency();

    /**
     * Get number of cached requests.
     *
     * @return number of cached requests
     */
    int getNumberOfCachedRequests();

    /**
     * Get the number of trigger sent to the collector.
     *
     * @return sent count
     */
    long getSentTriggerCount();

    /**
     * Get the ID of the most recent trigger request.
     *
     * @return counter value
     */
    int getTriggerCounter();
}
