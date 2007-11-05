/*
 * class: TriggerManager
 *
 * Version $Id: TriggerManager.java 2243 2007-11-05 22:47:49Z dglo $
 *
 * Date: October 25 2004
 *
 * (c) 2004 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.splicer.SpliceableFactory;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.Spliceable;
import icecube.daq.splicer.SplicerChangedEvent;
import icecube.daq.payload.MasterPayloadFactory;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID4B;
import icecube.daq.payload.impl.UTCTime8B;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.trigger.monitor.Statistic;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides the analysis framework for the inice trigger
 *
 * @version $Id: TriggerManager.java 2243 2007-11-05 22:47:49Z dglo $
 * @author pat
 */
public class TriggerManager
        extends TriggerHandler
        implements ITriggerManager
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(TriggerManager.class);

    /**
     * The factory used to produce IHitPayloads for this object to use.
     */
    private SpliceableFactory inputFactory;

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

    private double totalProcessTime;

    /**
     * size of last input list
     */
    private int lastInputListSize;

    private IUTCTime earliestTime;
    private IUTCTime latestTime;
    private LinkedList wallTimeQueue;
    private Statistic processingTime;

    /**
     * Default constructor.
     */
    public TriggerManager() {
        this(new MasterPayloadFactory());
    }

    /**
     * Constructor
     * @param sourceId SourceId of this TriggerManager
     */
    public TriggerManager(ISourceID sourceId) {
        this(new MasterPayloadFactory(), sourceId);
    }

    /**
     * Constructor
     * @param inputFactory SpliceableFactory used by Splicer
     */
    public TriggerManager(SpliceableFactory inputFactory) {
        this(inputFactory,
             new SourceID4B(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID));
    }

    /**
     * Constructor
     * @param inputFactory SpliceableFactory used by Splicer
     * @param sourceId SourceId of this TriggerManager
     */
    public TriggerManager(SpliceableFactory inputFactory, ISourceID sourceId) {
        super(sourceId, getOutputFactory(inputFactory));
        this.inputFactory = inputFactory;
        init();
    }

    private static TriggerRequestPayloadFactory
        getOutputFactory(SpliceableFactory inputFactory)
    {
        final int id = PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST;

        MasterPayloadFactory factory = (MasterPayloadFactory) inputFactory;

        return (TriggerRequestPayloadFactory) factory.getPayloadFactory(id);
    }

    protected void init() {
        start = 0;
        inputCount = 0;
        recycleCount = 0;
        totalProcessTime = 0.0;
        lastInputListSize = 0;
        earliestTime = new UTCTime8B(0);
        latestTime = new UTCTime8B(0);
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
            log.debug("Splicer contains: [" + lastInputListSize + ":" + numberOfObjectsInSplicer + "]");
        }

        if (lastInputListSize > 0) {
            for (int index = start-decrement; numberOfObjectsInSplicer != index; index++) {

                ILoadablePayload payload =
                    (ILoadablePayload) splicedObjects.get(index);
                wallTimeQueue.addLast(new Long(System.currentTimeMillis()));
                latestTime = payload.getPayloadTimeUTC();

                if (log.isDebugEnabled()) {
                    log.debug("  Processing payload " + inputCount + " with time "
                              + payload.getPayloadTimeUTC());
                }

                process(payload);
                inputCount++;
            }

            // Update splicer
            updateSplicer();

        }

        start = numberOfObjectsInSplicer;

    }

    /**
     * getter for factory
     * @return factory for producing primitives
     */
    public SpliceableFactory getFactory() {
        return inputFactory;
    }

    public void setFactory(SpliceableFactory inputFactory) {
        this.inputFactory = inputFactory;
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
                log.debug("Truncating splicer at " + ((IPayload) update).getPayloadTimeUTC());
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
        try {
            getPayloadDestination().closeAllPayloadDestinations();
        } catch (IOException e) {
            log.error("Error closing PayloadDestination", e);
        }
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
        try {
            getPayloadDestination().closeAllPayloadDestinations();
        } catch (IOException e) {
            log.error("Error closing PayloadDestination", e);
        }
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
        try {
            getPayloadDestination().stopAllPayloadDestinations();
        } catch (IOException e) {
            log.error("Error closing PayloadDestination", e);
        }
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
                log.debug("Recycle payload " + recycleCount + " at " + payload.getPayloadTimeUTC());
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
        double timePerInput = totalProcessTime/inputCount;
        if (log.isInfoEnabled()) {
            log.info("Processed " + inputCount + " hits at " + timePerInput + " ms per hit.");
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
