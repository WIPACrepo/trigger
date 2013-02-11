package icecube.daq.trigger.control;

import icecube.daq.payload.IPayload;

/**
 * Subscriber to a shared list of Payloads
 */
public interface PayloadSubscriber
{
    /**
     * Is there data available?
     *
     * @return <tt>true</tt> if there are more payloads available
     */
    boolean hasData();

    /**
     * Return the next available payload.  Note that this may block if there
     * are no payloads queued.
     *
     * @return next available payload.
     */
    IPayload pop();

    /**
     * No more payloads will be collected
     */
    void stop();
}
