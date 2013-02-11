package icecube.daq.trigger.control;

import icecube.daq.trigger.algorithm.FlushRequest;

/**
 * Time interval.
 */
public class Interval
    implements Comparable
{
    /** starting time */
    public long start;
    /** ending time */
    public long end;

    /**
     * Create an empty interval.
     */
    public Interval()
    {
        this(Long.MIN_VALUE, Long.MIN_VALUE);
    }

    /**
     * Create an interval.
     *
     * @param start starting time for interval
     * @param end ending time for interval
     */
    public Interval(long start, long end)
    {
        if (start == -1 || end == -1) {
            throw new Error("Cannot create invalid interval [" + start +
                            "-" + end + "]");
        }

        this.start = start;
        this.end = end;
    }

    /**
     * Compare the objects.
     *
     * @param obj object being compared
     *
     * @return the usual values
     */
    public int compareTo(Object obj)
    {
        if (obj == null) {
            return 1;
        } else if (!(obj instanceof Interval)) {
            return getClass().getName().compareTo(obj.getClass().getName());
        }

        return compareTo((Interval) obj);
    }

    /**
     * Compare the intervals.
     *
     * @param ival interval being compared
     *
     * @return the usual values
     */
    public int compareTo(Interval ival)
    {
        if (ival == null) {
            return 1;
        }

        // non-overlapping
        if (ival.end < start) {
            return -1;
        } else if (ival.start > end) {
            return 1;
        }

        if (ival.end < end) {
            return -1;
        } else if (ival.start > start) {
            return 1;
        }

        return 0;
    }

    /**
     * Has this interval been set to a value?
     *
     * @return <tt>true</tt> if the interval has been set
     */
    public boolean isEmpty()
    {
        return start == Long.MIN_VALUE || end == Long.MIN_VALUE;
    }

    /**
     * Are the objects equal?
     *
     * @param obj object being compared
     *
     * @return <tt>true</tt> if the objects are equal
     */
    public boolean equals(Object obj)
    {
        return compareTo(obj) == 0;
    }

    /**
     * Hash code for this object.
     *
     * @return hash code
     */
    public int hashCode()
    {
        final long modValue = Integer.MAX_VALUE / 256;

        return (int) (start / modValue) + (int) (start % modValue) +
            (int) (end / modValue) + (int) (end % modValue);
    }

    /**
     * Return a debugging string.
     *
     * @return debugging string
     */
    public String toString()
    {
        if (isEmpty()) {
            return "[]";
        }

        if (start == FlushRequest.FLUSH_TIME &&
            end == FlushRequest.FLUSH_TIME)
        {
            return "[FLUSH]";
        }

        return String.format("[%d-%d]", start, end);
    }
}
