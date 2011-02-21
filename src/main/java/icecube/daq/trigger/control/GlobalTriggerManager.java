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

import icecube.daq.oldpayload.impl.MasterPayloadFactory;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
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
 * This class...
 *
 * @version $Id: GlobalTriggerManager.java 12691 2011-02-21 20:22:03Z dglo $
 * @author shseo
 */
public class GlobalTriggerManager
        extends GlobalTriggerHandler
        implements ITriggerManager, GlobalTriggerManagerMBean
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(GlobalTriggerManager.class);
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
    private int recycleCount;
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
     */
    public GlobalTriggerManager(ISourceID sourceID,
                                TriggerRequestPayloadFactory outputFactory)
    {
        this(sourceID, outputFactory, DEFAULT_TIMEGAP_OPTION,
             DEFAULT_MAX_TIMEGATE_WINDOW);
    }

    private GlobalTriggerManager(ISourceID sourceID,
                                 TriggerRequestPayloadFactory outputFactory,
                                 boolean allowTimeGap, int iMax_TimeGate_Window)
    {
        super(sourceID, allowTimeGap, outputFactory);

        setMaxTimeGateWindow(iMax_TimeGate_Window);
        setAllowTimeGap(allowTimeGap);

        initialize();
    }

    public Splicer getSplicer() {
        return splicer;
    }

    public void setSplicer(Splicer splicer) {
        this.splicer = splicer;
        this.splicer.addSplicerListener(this);
    }

    public void setReportingThreshold(int threshold) {
        nThresholdInSplicer = threshold;
    }

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

        if (log.isInfoEnabled() && numberOfObjectsInSplicer >= nThresholdInSplicer){
            log.info("Splicer contains: [" + lastInputListSize + ":" + numberOfObjectsInSplicer + "]");
        }

        if (lastInputListSize > 0) {
            for (int index = start-decrement; numberOfObjectsInSplicer != index; index++) {

                ILoadablePayload payload =
                    (ILoadablePayload) splicedObjects.get(index);
                wallTimeQueue.addLast(new Long(System.currentTimeMillis()));
                latestTime = payload.getPayloadTimeUTC();

                inputCount++;
                if (log.isDebugEnabled()) {
                    log.debug("  Processing payload " + inputCount +
                              " with time " + payload.getPayloadTimeUTC());
                }

                process(payload);
                inputCount++;

            }
            updateSplicer();
        }

        start = numberOfObjectsInSplicer;

        if ((inputCount % nThresholdInSplicer) == (nThresholdInSplicer - 1)) {
            double timeDiff = System.currentTimeMillis() - startTime;
            double timePerHit = timeDiff/nInputs;
            if (log.isDebugEnabled()) {
                log.debug("Process time per input = " +
                          Math.round(timePerHit) + " ms");
            }
        }

    }

    /**
     * update splicer to earliest time of interest
     */
    public void updateSplicer() {
        Spliceable update = (Spliceable) super.getEarliestPayloadOfInterest();
        if (null != update) {
            if (log.isDebugEnabled()) {
                log.debug("Truncating splicer at " +
                          ((IPayload) update).getPayloadTimeUTC());
            }
            splicer.truncate(update);
        }
    }

    public void failed(SplicerChangedEvent event) {
        if (log.isErrorEnabled()) {
            log.error("Received Splicer FAILED");
        }
        getPayloadOutput().sendLastAndStop();
    }

    public void stopped(SplicerChangedEvent event) {
        log.info("Received Splicer STOPPED, flushing...");
        getPayloadOutput().forcedStopProcessing();
    }

    public void starting(SplicerChangedEvent event) {
    }

    public void started(SplicerChangedEvent event) {
    }

    public void stopping(SplicerChangedEvent event) {
    }

    public void truncated(SplicerChangedEvent event) {

        if (log.isDebugEnabled()) {
            log.debug("Splicer truncated: " + event.getSpliceable());
        }

        if (event.getSpliceable() == Splicer.LAST_POSSIBLE_SPLICEABLE) {
            finalFlush();
        }

        Iterator iter = event.getAllSpliceables().iterator();
        while (iter.hasNext()) {
            ILoadablePayload payload = (ILoadablePayload) iter.next();
            recycleCount++;
            if (log.isDebugEnabled()) {
                log.debug("  Recycle payload " + recycleCount + " at time " +
                          payload.getPayloadTimeUTC());
            }
            earliestTime = payload.getPayloadTimeUTC();
            long wallStart = ((Long) wallTimeQueue.removeFirst()).longValue();
            long wallEnd = System.currentTimeMillis();
            processingTime.add(wallEnd - wallStart);
            payload.recycle();
        }
    }

    public void disposed(SplicerChangedEvent event) {
        if (log.isInfoEnabled()) {
            log.info("Received Splicer DISPOSED");
        }
        getPayloadOutput().destroyProcessor();
    }

    public void flush() {
        super.flush();
        if (log.isInfoEnabled()) {
            log.info("Processed " + inputCount + " hits");
        }
    }

    private void initialize() {
        start = 0;
        inputCount = 0;
        recycleCount = 0;
        lastInputListSize = 0;
        earliestTime = new UTCTime(0);
        latestTime = new UTCTime(0);
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
