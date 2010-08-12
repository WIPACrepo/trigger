/*
 * class: PayloadFlowMonitor
 *
 * Version $Id: PayloadFlowMonitor.java 4574 2009-08-28 21:32:32Z dglo $
 *
 * Date: December 29 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.monitor;

import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.impl.UTCTime;
import icecube.icebucket.monitor.ScalarFlowMonitor;
import icecube.icebucket.monitor.simple.ScalarFlowMonitorImpl;

/**
 * This class monitors the flow of payloads. It keeps track of object counts and rates as
 * well as byte counts and rates.
 *
 * @version $Id: PayloadFlowMonitor.java 4574 2009-08-28 21:32:32Z dglo $
 * @author pat
 */
public class PayloadFlowMonitor
{

    /**
     * First time seen.
     */
    private IUTCTime firstTime = null;

    /**
     * Last time seen.
     */
    private IUTCTime lastTime = null;

    /**
     * Flow monitor for payload count.
     */
    private final ScalarFlowMonitor countMonitor;

    /**
     * Flow monitor for payload size.
     */
    private final ScalarFlowMonitor byteMonitor;

    /**
     * Default constructor.
     * Creates two new ScalarFlowMonitors
     */
    public PayloadFlowMonitor() {
        this(new ScalarFlowMonitorImpl(), new ScalarFlowMonitorImpl());
    }

    /**
     * Constructor.
     * @param countMonitor flow monitor for objects
     * @param byteMonitor flow monitor for bytes
     */
    public PayloadFlowMonitor(ScalarFlowMonitor countMonitor, ScalarFlowMonitor byteMonitor) {
        this.countMonitor = countMonitor;
        this.byteMonitor = byteMonitor;
    }

    /**
     * Record a payload.
     * @param payload payload to record
     */
    public void record(IPayload payload) {

        IUTCTime currentTime = payload.getPayloadTimeUTC();
        if (null == firstTime) {
            firstTime = currentTime;
        }
        lastTime = currentTime;

        countMonitor.measure(1);
        byteMonitor.measure(payload.getPayloadLength());

    }

    /**
     * Get first time seen.
     * @return first time
     */
    public IUTCTime getFirstTime() {
        if (null == firstTime) {
            return new UTCTime(0);
        }
        return firstTime;
    }

    /**
     * Get last time seen.
     * @return last time
     */
    public IUTCTime getLastTime() {
        if (null == lastTime) {
            return new UTCTime(0);
        }
        return lastTime;
    }

    /**
     * Get duration (in ns).
     * @return last time minus first time
     */
    public double getDuration() {
        if (null == firstTime) {
            return 0.0;
        }
        return lastTime.timeDiff_ns(firstTime);
    }

    /**
     * Get payload count.
     * @return number of payloads seen
     */
    public long getCountTotal() {
        return countMonitor.getTotal();
    }

    /**
     * Get payload rate (in Hz).
     * @return rate of payloads
     */
    public float getCountRate() {
        return countMonitor.getRate();
    }

    /**
     * Get payload rate history.
     * @return previous rate measurements
     */
    public float[] getCountHistory() {
        return countMonitor.getHistory();
    }

    /**
     * Get byte count.
     * @return number of bytes seen
     */
    public long getByteTotal() {
        return byteMonitor.getTotal();
    }

    /**
     * Get byte rate (in B/s)
     * @return rate of bytes
     */
    public float getByteRate() {
        return byteMonitor.getRate();
    }

    /**
     * Get byte rate history.
     * @return previous rate measurements
     */
    public float[] getByteHistory() {
        return byteMonitor.getHistory();
    }

}
