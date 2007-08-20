/*
 * class: TriggerManager
 *
 * Version $Id: TriggerManager.java,v 1.24 2006/05/09 19:55:29 toale Exp $
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
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID4B;
import icecube.daq.payload.splicer.Payload;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;

import java.util.List;
import java.util.Iterator;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides the analysis framework for the inice trigger
 *
 * @version $Id: TriggerManager.java,v 1.24 2006/05/09 19:55:29 toale Exp $
 * @author pat
 */
public class DummyTriggerManager
        extends DummyTriggerHandler
        implements ITriggerManager
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(DummyTriggerManager.class);

    /**
     * The factory used to produce IHitPayloads for this object to use.
     */
    private SpliceableFactory inputFactory = null;

    /**
     * splicer associated with this manager
     */
    private Splicer splicer;

    /**
     * marks the begining position of each new spliced buffer, modulo the decrement due to shifting
     */
    private int start;

    /**
     * size of last input list
     */
    private int lastInputListSize;

    /**
     * Default constructor.
     */
    public DummyTriggerManager() {
        this(new MasterPayloadFactory());
    }

    /**
     * Constructor
     * @param sourceId SourceId of this TriggerManager
     */
    public DummyTriggerManager(ISourceID sourceId) {
        this(new MasterPayloadFactory(), sourceId);
    }

    /**
     * Constructor
     * @param inputFactory SpiceableFactory used by Splicer
     */
    public DummyTriggerManager(SpliceableFactory inputFactory) {
        this(inputFactory,
             new SourceID4B(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID));
    }

    /**
     * Constructor
     * @param inputFactory SpiceableFactory used by Splicer
     * @param sourceId SourceId of this TriggerManager
     */
    public DummyTriggerManager(SpliceableFactory inputFactory, ISourceID sourceId) {
        super(sourceId);
        this.inputFactory = inputFactory;
        this.outputFactory = (TriggerRequestPayloadFactory)
                ((MasterPayloadFactory) inputFactory).getPayloadFactory(PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST);
        init();
    }

    private void init() {
        start = 0;
        lastInputListSize = 0;
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

        if (lastInputListSize > 0) {
            for (int index = start-decrement; numberOfObjectsInSplicer != index; index++) {

                ILoadablePayload payload =
                    (ILoadablePayload) splicedObjects.get(index);

                process(payload);
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
            log.info("Recieved Splicer DISPOSED");
        }
        try {
            payloadDestination.closeAllPayloadDestinations();
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
            log.error("Recieved Splicer FAILED");
        }
        try {
            payloadDestination.closeAllPayloadDestinations();
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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Called when the {@link icecube.daq.splicer.Splicer Splicer} enters the started state.
     *
     * @param event the event encapsulating this state change.
     */
    public void started(SplicerChangedEvent event) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Called when the {@link icecube.daq.splicer.Splicer Splicer} enters the stopped state.
     *
     * @param event the event encapsulating this state change.
     */
    public void stopped(SplicerChangedEvent event) {
        if (log.isInfoEnabled()) {
            log.info("Recieved Splicer STOPPED");
        }
        try {
            //payloadDestination.closeAllPayloadDestinations();
            payloadDestination.stopAllPayloadDestinations();
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
        //To change body of implemented methods use File | Settings | File Templates.
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

        if (event.getSpliceable() == Splicer.LAST_POSSIBLE_SPLICEABLE) {
            flush();
        }

        Iterator iter = event.getAllSpliceables().iterator();
        while (iter.hasNext()) {
            Payload payload = (Payload) iter.next();
            payload.recycle();
        }

    }

    public void flush() {
        super.flush();
    }

    public Splicer getSplicer() {
        return splicer;
    }

    public IUTCTime getEarliestTime() {
        return null;
    }

    public IUTCTime getLatestTime() {
        return null;
    }

    public long getAverageProcessingTime() {
        return 0;
    }

    public long getMinProcessingTime() {
        return 0;
    }

    public long getMaxProcessingTime() {
        return 0;
    }

    public int getProcessingCount() {
        return 0;
    }

}
