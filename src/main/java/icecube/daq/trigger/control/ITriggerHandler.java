/*
 * interface: ITriggerManager
 *
 * Version $Id: ITriggerHandler.java 13798 2012-07-16 19:44:30Z dglo $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.trigger.algorithm.ITrigger;
import icecube.daq.trigger.monitor.TriggerHandlerMonitor;
import icecube.daq.util.DOMRegistry;

import java.io.File;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This interface defines the behavior of a TriggerHandler
 *
 * @version $Id: ITriggerHandler.java 13798 2012-07-16 19:44:30Z dglo $
 * @author pat
 */
public interface ITriggerHandler extends IPayloadProducer
{

    /**
     * add a new trigger request to the trigger bag
     * @param payload new trigger request
     */
    void addToTriggerBag(ILoadablePayload payload);

    /**
     * add a trigger to the list of managed triggers
     * @param iTrigger trigger to add
     */
    void addTrigger(ITrigger iTrigger);

    /**
     * add a list of triggers
     * @param triggers
     */
    void addTriggers(List<ITrigger> triggers);

    /**
     * clear list of triggers
     */
    void clearTriggers();

    /**
     * flush the handler
     * including the input buffer, all triggers, and the output bag
     */
    void flush();

    /**
     * process next payload
     * @param payload payload to process
     */
    void process(ILoadablePayload payload);

    /**
     * Reset the handler for a new run.
     */
    void reset();

    /**
     * Stop the threads
     */
    void stopThread();

    /**
     * Get the SourceID
     * @return a ISourceID
     */
    ISourceID getSourceID();

    /**
     * Get the monitor object.
     * @return a TriggerHandlerMonitor
     */
    TriggerHandlerMonitor getMonitor();

    /**
     * Is the main thread waiting for input?
     *
     * @return <tt>true</tt> if the main thread is waiting for input
     */
    boolean isMainThreadWaiting();

    /**
     * Is the output thread waiting for data?
     *
     * @return <tt>true</tt> if the main thread is waiting for data
     */
    boolean isOutputThreadWaiting();

    /**
     * Set the DOMRegistry that should be used.
     * @param registry A configured DOMRegistry
     */
    void setDOMRegistry(DOMRegistry registry);

    /**
     * Get the DOMRegistry.
     * @return the DOMRegistry to use
     */
    DOMRegistry getDOMRegistry();

    /**
     * Get the number of payloads processed.
     * @return number of payloads processed
     */
    int getCount();

    /**
     * Set the outgoing payload buffer cache.
     * @param byte buffer cache manager
     */
    void setOutgoingBufferCache(IByteBufferCache cache);

    /**
     * Switch to a new run.
     */
    void switchToNewRun();
}
