package icecube.daq.trigger.control;

import icecube.daq.payload.IPayload;

/**
 * Subscriber to a shared list of Payloads
 */
public interface PayloadSubscriber
{
    /**
     * Get subscriber name
     *
     * @return name
     */
    String getName();

    /**
     * Is there data available?
     *
     * @return <tt>true</tt> if there are more payloads available
     */
    boolean hasData();

    /**
     * Has this list been stopped?
     *
     * @return <tt>true</tt> if the list has been stopped
     */
    boolean isStopped();

    /**
     * Return the next available payload.  Note that this may block if there
     * are no payloads queued.
     *
     * @return next available payload.
     */
    IPayload pop();

    /**
     * Add a payload to the queue.
     *
     * @param pay payload
     */
    void push(IPayload pay);

    /**
     * Get the number of queued payloads
     *
     * @return size of internal queue
     */
    int size();

    /**
     * No more payloads will be collected
     */
    void stop();
}
