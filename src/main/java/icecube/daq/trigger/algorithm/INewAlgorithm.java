package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IPayload;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.impl.TriggerRequestFactory;
import icecube.daq.trigger.common.ITriggerAlgorithm;
import icecube.daq.trigger.control.Interval;
import icecube.daq.trigger.control.PayloadSubscriber;
import icecube.daq.trigger.control.TriggerCollector;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import java.util.List;

public interface INewAlgorithm
    extends ITriggerAlgorithm
{
    /**
     * Add a trigger parameter.
     *
     * @param name parameter name
     * @param value parameter value
     *
     * @throws UnknownParameterException if the parameter is unknown
     * @throws IllegalParameterValueException if the parameter value is bad
     */
    void addParameter(String name, String value)
        throws UnknownParameterException, IllegalParameterValueException;

    /**
     * Add a readout entry to the cached list.
     *
     * @param rdoutType readout type
     * @param offset offset value
     * @param minus minus
     * @param plus plus
     */
    void addReadout(int rdoutType, int offset, int minus, int plus);

    /**
     * Get the input queue size.
     *
     * @return input queue size
     */
    int getInputQueueSize();

    /**
     * Get the next interval.
     *
     * @param interval interval being considered
     *
     * @return next interval
     */
    Interval getInterval(Interval interval);

    /**
     * Get the monitoring name.
     *
     * @return the name used for monitoring this trigger
     */
    String getMonitoringName();

    /**
     * Get number of cached requests.
     *
     * @return number of cached requests
     */
    int getNumberOfCachedRequests();

    /**
     * Get the input provider.
     *
     * @return input queue subscription
     */
    PayloadSubscriber getSubscriber();

    /**
     * Get the time of the last released trigger.
     *
     * @return release time
     */
    IPayload getReleaseTime();

    /**
     * Does this algorithm include all relevant hits in each request
     * so that it can be used to calculate multiplicity?
     *
     * @return <tt>true</tt> if this algorithm can supply a valid multiplicity
     */
    boolean hasValidMultiplicity();

    /**
     * Recycle all unused requests still cached in the algorithms.
     */
    void recycleUnusedRequests();

    /**
     * Add all requests in the interval to the list of released requests.
     *
     * @param interval time interval to check
     * @param released list of released requests
     */
    void release(Interval interval,
                 List<ITriggerRequestPayload> released);

    /**
     * Reset the UID to signal a run switch.
     */
    void resetUID();

    /**
     * Clear out all remaining payloads.
     */
    void sendLast();

    /**
     * Set the list subscriber client (for monitoring the input queue).
     *
     * @param subscriber input queue subscriber
     */
    void setSubscriber(PayloadSubscriber subscriber);

    /**
     * Set request collector.
     *
     * @param collector trigger collector
     */
    void setTriggerCollector(TriggerCollector collector);

    /**
     * Set the factory used to create trigger requests.
     *
     * @param triggerFactory trigger factory
     */
    void setTriggerFactory(TriggerRequestFactory triggerFactory);
}
