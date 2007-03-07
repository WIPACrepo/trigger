/*
 * class: PayloadBagMonitor
 *
 * Version $Id: PayloadBagMonitor.java,v 1.1 2005/12/29 23:16:15 toale Exp $
 *
 * Date: December 29 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.monitor;

import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IPayload;

/**
 * This class combines two PayloadFlowMonitors to be used by any class that consumes and
 * produces payloads.
 *
 * @version $Id: PayloadBagMonitor.java,v 1.1 2005/12/29 23:16:15 toale Exp $
 * @author pat
 */
public class PayloadBagMonitor
{

    /**
     * Input payload monitor.
     */
    private final PayloadFlowMonitor inputMonitor;

    /**
     * Output payload monitor.
     */
    private final PayloadFlowMonitor outputMonitor;

    /**
     * Default constructor.
     * Creates two new PayloadFlowMonitors.
     */
    public PayloadBagMonitor() {
        this(new PayloadFlowMonitor(), new PayloadFlowMonitor());
    }

    /**
     * Constructor.
     * @param inputMonitor payload monitor for input
     * @param outputMonitor payload monitor for output
     */
    public PayloadBagMonitor(PayloadFlowMonitor inputMonitor, PayloadFlowMonitor outputMonitor) {
        this.inputMonitor = inputMonitor;
        this.outputMonitor = outputMonitor;
    }

    /**
     * Record an input payload
     * @param payload input payload
     */
    public void recordInput(IPayload payload) {
        inputMonitor.record(payload);
    }

    /**
     * Record an output payload
     * @param payload output payload
     */
    public void recordOutput(IPayload payload) {
        outputMonitor.record(payload);
    }

    /**
     * Get first time of input monitor.
     * @return first time seen on input
     */
    public IUTCTime getInputFirstTime() {
        return inputMonitor.getFirstTime();
    }

    /**
     * Get last time of input monitor.
     * @return last time seen on input
     */
    public IUTCTime getInputLastTime() {
        return inputMonitor.getLastTime();
    }

    /**
     * Get running time on input (in ns).
     * @return last time minus first time
     */
    public double getInputDuration() {
        return inputMonitor.getDuration();
    }

    /**
     * Get first time on output.
     * @return first time seen on output
     */
    public IUTCTime getOutputFirstTime() {
        return outputMonitor.getFirstTime();
    }

    /**
     * Get last time on output.
     * @return last time seen on output
     */
    public IUTCTime getOutputLastTime() {
        return outputMonitor.getLastTime();
    }

    /**
     * Get running time on output (in ns).
     * @return last time minus first time
     */
    public double getOutputDuration() {
        return outputMonitor.getDuration();
    }

    /**
     * Get latency of bag (in ns).
     * @return last time of input minus last time of output.
     */
    public double getLatency() {
        return inputMonitor.getLastTime().timeDiff_ns(outputMonitor.getLastTime());
    }

    /**
     * Get the difference in input counts and output counts.
     * @return input count minus output count
     */
    public long getCountDifference() {
        return (inputMonitor.getCountTotal() - outputMonitor.getCountTotal());
    }

    /**
     * Get the count rate reduction.
     * @return output count rate divided by input count rate
     */
    public float getCountRateReduction() {
        if (0 >= inputMonitor.getCountRate()) {
            return -1;
        }
        return (outputMonitor.getCountRate()/inputMonitor.getCountRate());
    }

    /**
     * Get the byte rate reduction.
     * @return output byte rate divided by input byte rate
     */
    public float getByteRateReduction() {
        if (0 >= inputMonitor.getByteRate()) {
            return -1;
        }
        return (outputMonitor.getByteRate()/inputMonitor.getByteRate());
    }

    /**
     * Get the count total on input.
     * @return number of objects seen on input
     */
    public long getInputCountTotal() {
        return inputMonitor.getCountTotal();
    }

    /**
     * Get the count rate on input.
     * @return rate of objects seen on input
     */
    public float getInputCountRate() {
        return inputMonitor.getCountRate();
    }

    /**
     * Get the rate history on input.
     * @return input count rates
     */
    public float[] getInputCountHistory() {
        return inputMonitor.getCountHistory();
    }

    /**
     * Get the byte total on input.
     * @return number of bytes seen on input
     */
    public long getInputByteTotal() {
        return inputMonitor.getByteTotal();
    }

    /**
     * Get the byte rate on input.
     * @return rate of bytes seen on input
     */
    public float getInputByteRate() {
        return inputMonitor.getByteRate();
    }

    /**
     * Get the rate history on input.
     * @return input byte rates
     */
    public float[] getInputByteHistory() {
        return inputMonitor.getByteHistory();
    }

    /**
     * Get the count total on output.
     * @return number of objects seen on output
     */
    public long getOutputCountTotal() {
        return outputMonitor.getCountTotal();
    }

    /**
     * Get the count rate on output.
     * @return rate of objects seen on output
     */
    public float getOutputCountRate() {
        return outputMonitor.getCountRate();
    }

    /**
     * Get the rate history on output.
     * @return output count rates
     */
    public float[] getOutputCountHistory() {
        return outputMonitor.getCountHistory();
    }

    /**
     * Get the byte total on output.
     * @return number of bytes seen on output
     */
    public long getOutputByteTotal() {
        return outputMonitor.getByteTotal();
    }

    /**
     * Get the byte rate on output.
     * @return rate of bytes seen on output
     */
    public float getOutputByteRate() {
        return outputMonitor.getByteRate();
    }

    /**
     * Get the rate history on output.
     * @return output byte rates
     */
    public float[] getOutputByteHistory() {
        return outputMonitor.getByteHistory();
    }

}
