package icecube.daq.trigger.control;

/**
 * Ugly hack to allow control to ping-pong between TriggerManager and
 * TriggerCollector during the end of the run
 */
public interface SubscriptionManager
{
    void subscribeAll();
    void unsubscribeAll();
}
