package icecube.daq.trigger.control;

public interface ITriggerCollector
{
    /**
     * Notify the collector thread that one or more lists has changed.
     */
    void setChanged();
}
