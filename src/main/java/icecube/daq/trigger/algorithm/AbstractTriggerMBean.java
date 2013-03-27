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
     * Get number of cached requests.
     *
     * @return number of cached requests
     */
    int getNumberOfCachedRequests();

    /**
     * Get the ID of the most recent trigger request.
     *
     * @return counter value
     */
    int getTriggerCounter();
}
