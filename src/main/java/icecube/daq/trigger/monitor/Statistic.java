/*
 * class: Statistic
 *
 * Version $Id: Statistic.java 2125 2007-10-12 18:27:05Z ksb $
 *
 * Date: May 7 2006
 *
 * (c) 2006 IceCube Collaboration
 */

package icecube.daq.trigger.monitor;

/**
 * This class implements a simple statistic, keeping track of the average value as
 * well as min and max values.
 *
 * @version $Id: Statistic.java 2125 2007-10-12 18:27:05Z ksb $
 * @author pat
 */
public class Statistic
{

    double sum;
    long count;

    double min;
    double max;

    boolean newMin;
    boolean newMax;

    public Statistic() {
        sum = 0;
        count = 0;
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        newMin = false;
        newMax = false;
    }

    public void add(double value) {
        sum += value;
        count++;

        if (value < min) {
            min = value;
            newMin = true;
        } else {
            newMin = false;
        }

        if (value > max) {
            max = value;
            newMax = true;
        } else {
            newMax = false;
        }
    }

    public double getAverage() {
        if (count < 1) {
            return 0;
        }
        return sum/((double) count);
    }

    public double getSum() {
        return sum;
    }

    public long getCount() {
        return count;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public boolean isNewMin() {
        return newMin;
    }

    public boolean isNewMax() {
        return newMax;
    }

}
