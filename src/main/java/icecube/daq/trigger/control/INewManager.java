package icecube.daq.trigger.control;

import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.common.ITriggerManager;

import java.util.TreeMap;
import java.util.TreeSet;

/**
 * New trigger manager interface.
 */
public interface INewManager
    extends ITriggerManager
{
    /**
     * XXX Unimplemented.
     *
     * @param trigReq ignored
     */
    void addTriggerRequest(ITriggerRequestPayload trigReq);

    /**
     * Get the number of inputs waiting to be processed.
     *
     * @return size of the input queue
     */
    int getNumInputsQueued();

    /**
     * XXX Unimplemented
     *
     * @return UnimplementedError
     */
    TreeMap<Integer, TreeSet<Integer>> getStringMap();

    /**
     * Set the splicer associated with this trigger manager
     *
     * @param splicer splicer
     * @deprecated
     */
    void setSplicer(Splicer splicer);

    /**
     * Stop all threads.
     */
    void stopThread();
}
