/*
 * class: TriggerManager
 *
 * Version $Id: TriggerManager.java 12691 2011-02-21 20:22:03Z dglo $
 *
 * Date: October 25 2004
 *
 * (c) 2004 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.oldpayload.impl.MasterPayloadFactory;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.splicer.Spliceable;
import icecube.daq.splicer.SpliceableFactory;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerChangedEvent;
import icecube.daq.trigger.monitor.Statistic;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides the analysis framework for the inice trigger
 *
 * @version $Id: TriggerManager.java 12691 2011-02-21 20:22:03Z dglo $
 * @author pat
 */
public class TriggerManager
        extends TriggerHandler
        implements ITriggerManager, TriggerManagerMBean
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(TriggerManager.class);

    /**
     * splicer associated with this manager
     */
    private Splicer splicer;

    /**
     * marks the begining position of each new spliced buffer, modulo the decrement due to shifting
     */
    private int start;

    /**
     * spliceable inputCount
     */
    private long inputCount;

    private long recycleCount;

    /**
     * size of last input list
     */
    private int lastInputListSize;

    private IUTCTime earliestTime;
    private IUTCTime latestTime;
    private LinkedList wallTimeQueue;
    private Statistic processingTime;

    /**
     * Constructor
     * @param sourceId SourceId of this TriggerManager
     * @param outputFactory factory used to build triggers
     */
    public TriggerManager(ISourceID sourceId,
                          TriggerRequestPayloadFactory outputFactory)
    {
        super(sourceId, outputFactory);
        init();
    }

    protected void init() {
        start = 0;
        inputCount = 0;
        recycleCount = 0;
        lastInputListSize = 0;
        earliestTime = new UTCTime(0);
        latestTime = new UTCTime(0);
        wallTimeQueue = new LinkedList();
        processingTime = new Statistic();
        super.init();
    }

    /**
     * Reset the handler for a new run.
     */
    public void reset() {
        super.reset();
        init();
    }

    /**
     * method which is called by splicer to process primitives
     * assumes that all splicedObjects are IHitPayload's
     * @param splicedObjects list of primitives from splicer
     * @param decrement amount by which buffer was shifted since last call
     */
    public synchronized void execute(List splicedObjects, int decrement) {

        // Loop over the new objects in the splicer
        int numberOfObjectsInSplicer = splicedObjects.size();
        lastInputListSize = numberOfObjectsInSplicer - (start - decrement);

        if (log.isDebugEnabled()) {
            log.debug("Splicer contains: [" + lastInputListSize + ":" +
                      numberOfObjectsInSplicer + "]");
        }

        if (lastInputListSize > 0) {
            for (int index = start-decrement; numberOfObjectsInSplicer != index; index++) {

                ILoadablePayload payload =
                    (ILoadablePayload) splicedObjects.get(index);
                wallTimeQueue.addLast(new Long(System.currentTimeMillis()));
                latestTime = payload.getPayloadTimeUTC();

                if (log.isDebugEnabled()) {
                    log.debug("  Processing payload " + inputCount +
                              " with time " + payload.getPayloadTimeUTC());
                }

                process(payload);
                inputCount++;
            }

            // Update splicer
            updateSplicer();

        }

        start = numberOfObjectsInSplicer;

    }

    public int getLastInputListSize() {
        return lastInputListSize;
    }

    /**
     * setter for splicer
     * @param splicer splicer associated with this object
     */
    public void setSplicer(Splicer splicer) {
        this.splicer = splicer;
        this.splicer.addSplicerListener(this);
    }

    /**
     * update splicer to earliest time of interest
     */
    public void updateSplicer() {
        Spliceable update = (Spliceable) getEarliestPayloadOfInterest();
        if (null != update) {
            if (log.isDebugEnabled()) {
                log.debug("Truncating splicer at " +
                          ((IPayload) update).getPayloadTimeUTC());
            }
            splicer.truncate(update);
        }
    }

    /**
     * Called when the {@link icecube.daq.splicer.Splicer Splicer} enters the disposed state.
     *
     * @param event the event encapsulating this state change.
     */
    public void disposed(SplicerChangedEvent event) {
        if (log.isInfoEnabled()) {
            log.info("Received Splicer DISPOSED");
        }
        getPayloadOutput().destroyProcessor();
    }

    /**
     * Called when the {@link icecube.daq.splicer.Splicer Splicer} enters the failed state.
     *
     * @param event the event encapsulating this state change.
     */
    public void failed(SplicerChangedEvent event) {
        if (log.isErrorEnabled()) {
            log.error("Received Splicer FAILED");
        }
        getPayloadOutput().destroyProcessor();
    }

    /**
     * Called when the {@link icecube.daq.splicer.Splicer Splicer} enters the starting state.
     *
     * @param event the event encapsulating this state change.
     */
    public void starting(SplicerChangedEvent event) {
        // do nothing
    }

    /**
     * Called when the {@link icecube.daq.splicer.Splicer Splicer} enters the started state.
     *
     * @param event the event encapsulating this state change.
     */
    public void started(SplicerChangedEvent event) {
        // do nothing
    }

    /**
     * Called when the {@link icecube.daq.splicer.Splicer Splicer} enters the stopped state.
     *
     * @param event the event encapsulating this state change.
     */
    public void stopped(SplicerChangedEvent event) {
        if (log.isInfoEnabled()) {
            log.info("Received Splicer STOPPED");
        }
        getPayloadOutput().forcedStopProcessing();
    }

    /**
     * Called when the {@link icecube.daq.splicer.Splicer Splicer} enters the stopping state.
     *
     * @param event the event encapsulating this state change.
     */
    public void stopping(SplicerChangedEvent event) {
        // do nothing
    }

    /**
     * Called when the {@link icecube.daq.splicer.Splicer Splicer} has truncated its "rope". This method is called
     * whenever the "rope" is cut, for example to make a clean start from the frayed beginning of a "rope", and not jsut
     * the the {@link Splicer#truncate(Spliceable)} method is invoked. This enables the client to be notified as to
     * which Spliceable are nover going to be accessed again by the Splicer.
     *
     * @param event the event encapsulating this truncation.
     */
    public void truncated(SplicerChangedEvent event) {

        if (log.isDebugEnabled()) {
            log.debug("Splicer truncated: " + event.getSpliceable());
        }

        if (event.getSpliceable() == Splicer.LAST_POSSIBLE_SPLICEABLE) {
            if (log.isDebugEnabled()) {
                log.debug("This is the LAST POSSIBLE SPLICEABLE!");
            }
            flush();
        }

        Iterator iter = event.getAllSpliceables().iterator();
        while (iter.hasNext()) {
            ILoadablePayload payload = (ILoadablePayload) iter.next();
            if (log.isDebugEnabled()) {
                log.debug("Recycle payload " + recycleCount + " at " +
                          payload.getPayloadTimeUTC());
            }

            earliestTime = payload.getPayloadTimeUTC();
            long startTime = ((Long) wallTimeQueue.removeFirst()).longValue();
            long endTime = System.currentTimeMillis();
            processingTime.add(endTime - startTime);
            payload.recycle();
            recycleCount++;
        }

    }

    public void flush() {
        super.flush();
        if (log.isInfoEnabled()) {
            log.info("Processed " + inputCount + " hits");
        }
    }

    public Splicer getSplicer() {
        return splicer;
    }

    public IUTCTime getEarliestTime() {
        return earliestTime;
    }

    public IUTCTime getLatestTime() {
        return latestTime;
    }

    public long getAverageProcessingTime() {
        return (long) processingTime.getAverage();
    }

    public long getMinProcessingTime() {
        return (long) processingTime.getMin();
    }

    public long getMaxProcessingTime() {
        return (long) processingTime.getMax();
    }

    public int getProcessingCount() {
        return wallTimeQueue.size();
    }

}
