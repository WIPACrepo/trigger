/*
 * class: TriggerHandlerMonitor
 *
 * Version $Id: TriggerHandlerMonitor.java,v 1.2 2006/05/08 02:44:44 toale Exp $
 *
 * Date: December 29 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.monitor;

import icecube.daq.payload.IUTCTime;

/**
 * This class is a mess.
 *
 * @version $Id: TriggerHandlerMonitor.java,v 1.2 2006/05/08 02:44:44 toale Exp $
 * @author pat
 */
public class TriggerHandlerMonitor
{

    private PayloadBagMonitor triggerBagMonitor;

    public TriggerHandlerMonitor() {
        this(new PayloadBagMonitor());
    }

    public TriggerHandlerMonitor(PayloadBagMonitor triggerBagMonitor) {
        this.triggerBagMonitor = triggerBagMonitor;
    }

    public void setTriggerBagMonitor(PayloadBagMonitor triggerBagMonitor) {
        this.triggerBagMonitor = triggerBagMonitor;
    }

    /**
     * Get first time on output of trigger bag.
     * @return first time seen on output of trigger bag
     */
    public IUTCTime getTriggerBagFirstTime() {
        return triggerBagMonitor.getOutputFirstTime();
    }

    /**
     * Get last time on output of trigger bag.
     * @return last time seen on output of trigger bag
     */
    public IUTCTime getTriggerBagLastTime() {
        return triggerBagMonitor.getOutputLastTime();
    }

    /**
     * Get running time on output (in ns) of trigger bag.
     * @return last time minus first time of trigger bag
     */
    public double getTriggerBagDuration() {
        return triggerBagMonitor.getOutputDuration();
    }

    /**
     * Get latency of trigger bag (in ns).
     * @return last time of input minus last time of output.
     */
    public double getTriggerBagLatency() {
        return triggerBagMonitor.getLatency();
    }

    /**
     * Get the difference in input counts and output counts of trigger bag.
     * @return input count minus output count
     */
    public long getTriggerBagCountDifference() {
        return triggerBagMonitor.getCountDifference();
    }

    /**
     * Get the count rate reduction of trigger bag.
     * @return output count rate divided by input count rate
     */
    public float getTriggerBagCountRateReduction() {
        return triggerBagMonitor.getCountRateReduction();
    }

    /**
     * Get the byte rate reduction of trigger bag.
     * @return output byte rate divided by input byte rate
     */
    public float getTriggerBagByteRateReduction() {
        return triggerBagMonitor.getByteRateReduction();
    }

    /**
     * Get the count total on output of trigger bag.
     * @return number of objects seen on output of trigger bag
     */
    public long getTriggerBagCountTotal() {
        return triggerBagMonitor.getOutputCountTotal();
    }

    /**
     * Get the count rate on output of trigger bag.
     * @return rate of objects seen on output of trigger bag
     */
    public float getTriggerBagCountRate() {
        return triggerBagMonitor.getOutputCountRate();
    }

    /**
     * Get the byte total on output of trigger bag.
     * @return number of bytes seen on output of trigger bag
     */
    public long getTriggerBagByteTotal() {
        return triggerBagMonitor.getOutputByteTotal();
    }

    /**
     * Get the byte rate on output of trigger bag.
     * @return rate of bytes seen on output of trigger bag
     */
    public float getTriggerBagByteRate() {
        return triggerBagMonitor.getOutputByteRate();
    }

}
