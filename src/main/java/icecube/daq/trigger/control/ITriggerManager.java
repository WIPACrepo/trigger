/*
 * interface: ITriggerManager
 *
 * Version $Id: ITriggerManager.java 12777 2011-03-14 22:32:59Z dglo $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.juggler.alert.AlertQueue;
import icecube.daq.payload.IPayload;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.AlgorithmStatistics;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.util.IDOMRegistry;

import java.util.Map;

/**
 * This interface defines the behavior of a TriggerManager
 *
 * @version $Id: ITriggerManager.java 12777 2011-03-14 22:32:59Z dglo $
 * @author pat
 */
public interface ITriggerManager
{
    /**
     * Add a trigger algorithm.
     *
     * @param trig algorithm being added
     */
    void addTrigger(ITriggerAlgorithm trig);

    /**
     * Add a list of algorithms.
     *
     * @param list list of trigger algorithms to add
     */
    void addTriggers(Iterable<ITriggerAlgorithm> list);

    /**
     * Old method used to flush algorithms.
     * @deprecated
     */
    void flush();

    /**
     * Get the alert queue for messages sent to I3Live
     *
     * @return alert queue
     */
    AlertQueue getAlertQueue();

    /**
     * Get list of objects which can return trigger algorithm statistics
     *
     * @return list of trigger statistics objects
     */
    Iterable<AlgorithmStatistics> getAlgorithmStatistics();

    /**
     * Get the DOM registry.
     *
     * @return DOM registry
     */
    IDOMRegistry getDOMRegistry();

    /**
     * Get number of triggers queued for input.
     *
     * @return number of triggers queued
     */
    Map<String, Integer> getQueuedInputs();

    /**
     * Get the number of requests queued for writing
     *
     * @return size of output queue
     */
    int getNumOutputsQueued();

    /**
     * Get this component's source ID.
     *
     * @return source ID
     */
    int getSourceId();

    /**
     * Get the total number of hits pushed onto the input queue
     *
     * @return total number of hits received from the splicer
     */
    long getTotalProcessed();

    /**
     * Are all data collection threads stopped?
     *
     * @return <tt>true</tt> if the data collection threads are stopped
     */
    boolean isStopped();

    /**
     * Set the DOM registry.
     *
     * @param registry DOM registry
     * @deprecated
     */
    void setDOMRegistry(IDOMRegistry registry);

    /**
     * Notify the thread of the earliest payload of interest
     *
     * @param payload earliest payload
     */
    void setEarliestPayloadOfInterest(IPayload payload);

    /**
     * Set the output engine.
     *
     * @param payloadOutput output engine
     */
    void setOutputEngine(DAQComponentOutputProcess payloadOutput);

    /**
     * Set the splicer associated with this trigger manager
     *
     * @param splicer splicer
     * @deprecated
     */
    void setSplicer(Splicer splicer);

    /**
     * Stop the threads.
     */
    void stopThread();

    /**
     * Switch to a new run.
     */
    void switchToNewRun(int runNumber);
}
