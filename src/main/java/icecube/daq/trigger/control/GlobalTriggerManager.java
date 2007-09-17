/*
 * class: GlobalTrigManager
 *
 * Version $Id: GlobalTrigManager.java, shseo
 *
 * Date: August 1 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SpliceableFactory;
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
import icecube.daq.payload.splicer.Payload;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.trigger.monitor.Statistic;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class...
 *
 * @version $Id: GlobalTriggerManager.java,v 1.31 2006/05/09 19:57:57 toale Exp $
 * @author shseo
 */
public class GlobalTriggerManager
        extends GlobalTriggerHandler
        implements ITriggerManager
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(GlobalTriggerManager.class);
    /**
     * The factory used to produce IHitPayloads for this object to use.
     */
    //private final MasterPayloadFactory inputFactory = new MasterPayloadFactory();
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
     * spliceable inputCount
     */
    private int inputCount;
    private int recycleCount = 0;
    private double totalProcessTime;
    /**
     * size of last input list
     */
    private int lastInputListSize;

    private int nThresholdInSplicer = 2000;
    private long startTime;
    private int nInputs;

    private IUTCTime earliestTime;
    private IUTCTime latestTime;
    private LinkedList wallTimeQueue;
    private Statistic processingTime;

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public GlobalTriggerManager()
    {
        this(new MasterPayloadFactory(),
             new SourceID4B(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID),
             DEFAULT_TIMEGAP_OPTION);
    }

    public GlobalTriggerManager(SpliceableFactory inputFactory)
    {
        this(inputFactory,
             new SourceID4B(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID),
             DEFAULT_TIMEGAP_OPTION);
    }

    public GlobalTriggerManager(SpliceableFactory inputFactory, ISourceID sourceID)
    {
        this(inputFactory, sourceID, DEFAULT_TIMEGAP_OPTION);
    }

    public GlobalTriggerManager(SpliceableFactory inputFactory, ISourceID sourceID, int iTimeGap_option)
    {
        super.sourceID = sourceID;
        this.inputFactory = inputFactory;
        outputFactory = (TriggerRequestPayloadFactory)
                ((MasterPayloadFactory) inputFactory).getPayloadFactory(PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST);

        super.init();
        this.initialize();
        super.setMaxTimeGateWindow(DEFAULT_MAX_TIMEGATE_WINDOW);
        super.setTimeGap_option(iTimeGap_option);
    }

    public GlobalTriggerManager(SpliceableFactory inputFactory, ISourceID sourceID,
                                int iTimeGap_option, int iMax_TimeGate_Window)
    {
        super.sourceID = sourceID;
        this.inputFactory = inputFactory;
        outputFactory = (TriggerRequestPayloadFactory)
                ((MasterPayloadFactory) inputFactory).getPayloadFactory(PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST);
        super.setMaxTimeGateWindow(iMax_TimeGate_Window);
        super.setTimeGap_option(iTimeGap_option);
        super.init();
        this.initialize();
    }

    // public static void main(String args[]) {}
    public Splicer getSplicer() {
        return splicer;
    }

    public void setSplicer(Splicer splicer) {
        this.splicer = splicer;
        this.splicer.addSplicerListener(this);
    }

    public void setFactory(SpliceableFactory inputFactory) {
        this.inputFactory = inputFactory;
    }

  /*  public void setMaxTimeGateWindow(int iMaxTimeGateWindow)
    {
        super.setMaxTimeGateWindow(iMaxTimeGateWindow);
    }*/

    public void execute(List splicedObjects, int decrement) {
        // Loop over the new objects in the splicer
//-----------------------------------------------------
        if ((inputCount % nThresholdInSplicer) == 0) {
            nInputs = 0;
            startTime = System.currentTimeMillis();
        } else {
            nInputs++;
        }

        // Loop over the new objects in the splicer
        int numberOfObjectsInSplicer = splicedObjects.size();
        lastInputListSize = numberOfObjectsInSplicer - (start - decrement);

        if (log.isInfoEnabled()) {
            if(numberOfObjectsInSplicer >= nThresholdInSplicer){
                log.info("Splicer contains: [" + lastInputListSize + ":" + numberOfObjectsInSplicer + "]");
            }
        }

        if (lastInputListSize > 0) {
            for (int index = start-decrement; numberOfObjectsInSplicer != index; index++) {

                ILoadablePayload payload =
                    (ILoadablePayload) splicedObjects.get(index);
                wallTimeQueue.addLast(new Long(System.currentTimeMillis()));
                latestTime = payload.getPayloadTimeUTC();

                inputCount++;
                if (log.isDebugEnabled()) {
                    log.debug("  Processing payload " + inputCount + " with time "
                         + payload.getPayloadTimeUTC());
                }

                process(payload);
                inputCount++;

            }
            updateSplicer();
        }

        start = numberOfObjectsInSplicer;

        totalProcessTime += (System.currentTimeMillis() - startTime);
        if(lastInputListSize > nThresholdInSplicer){
            if (log.isInfoEnabled()) {
                log.info("Total time = " + totalProcessTime);
            }
        }

        if ((inputCount % nThresholdInSplicer) == (nThresholdInSplicer - 1)) {
            double timeDiff = System.currentTimeMillis() - startTime;
            double timePerHit = timeDiff/nInputs;
            log.debug("Process time per input = " + Math.round(timePerHit)+ " ms");
        }

    }

    public SpliceableFactory getFactory() {
        return inputFactory;
    }
    /**
     * update splicer to earliest time of interest
     */
    public void updateSplicer() {
        Spliceable update = (Spliceable) super.getEarliestPayloadOfInterest();
        if (null != update) {
            log.debug("Truncating splicer at " + ((IPayload) update).getPayloadTimeUTC());
            splicer.truncate(update);
        }
    }

    public void failed(SplicerChangedEvent event) {
        if (log.isErrorEnabled()) {
            log.error("Received Splicer FAILED");
        }
        try {
            payloadDestination.closeAllPayloadDestinations();
        } catch (IOException e) {
            log.error("Error closing PayloadDestination", e);
        }
    }

    public void stopped(SplicerChangedEvent event) {
        log.info("Received Splicer STOPPED, flushing...");
        try {
            payloadDestination.stopAllPayloadDestinations();
        } catch (IOException e) {
            log.error("Error closing PayloadDestinations");
        }
    }

    public void starting(SplicerChangedEvent event) {
    }

    public void started(SplicerChangedEvent event) {
    }

    public void stopping(SplicerChangedEvent event) {
    }

    public void truncated(SplicerChangedEvent event) {

        log.debug("Splicer truncated: " + event.getSpliceable());
        if (event.getSpliceable() == Splicer.LAST_POSSIBLE_SPLICEABLE) {
            flush();
        }
        Iterator iter = event.getAllSpliceables().iterator();
        while (iter.hasNext()) {
            Payload payload = (Payload) iter.next();
            recycleCount++;
            log.debug("  Recycle payload " + recycleCount + " at time " + payload.getPayloadTimeUTC());
            earliestTime = payload.getPayloadTimeUTC();
            long startTime = ((Long) wallTimeQueue.removeFirst()).longValue();
            long endTime = System.currentTimeMillis();
            processingTime.add(endTime - startTime);
            payload.recycle();
        }
    }

    public void disposed(SplicerChangedEvent event) {
        if (log.isInfoEnabled()) {
            log.info("Received Splicer DISPOSED");
        }
        try {
            payloadDestination.closeAllPayloadDestinations();
        } catch (IOException e) {
            log.error("Error closing PayloadDestination", e);
        }
    }

    public void flush() {
        super.flush();
        double timePerInput = totalProcessTime/inputCount;
        if (log.isInfoEnabled()) {
            log.info("Processed " + inputCount + " hits at " + timePerInput + " ms per hit.");
        }
    }

    private void initialize() {
        start = 0;
        inputCount = 0;
        recycleCount = 0;
        totalProcessTime = 0.0;
        lastInputListSize = 0;
        earliestTime = new UTCTime8B(0);
        latestTime = new UTCTime8B(0);
        wallTimeQueue = new LinkedList();
        processingTime = new Statistic();
    }

    /**
     * Reset the handler for a new run.
     */
    public void reset() {
        log.info("Reseting GlobalTrigManager");
        super.reset();
        this.initialize();
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
