/*
 * class: TriggerReadout
 *
 * Version $Id: TriggerReadout.java 17207 2018-11-08 16:08:59Z dglo $
 *
 * Date: November 23 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.config;

import icecube.daq.payload.IReadoutRequestElement;

import org.apache.log4j.Logger;

/**
 * This class represents a trigger readout.
 *
 * @version $Id: TriggerReadout.java 17207 2018-11-08 16:08:59Z dglo $
 * @author pat
 */
public class TriggerReadout
{

    /**
     * Log object for this class.
     */
    private static final Logger LOG = Logger.getLogger(TriggerReadout.class);

    /**
     * Default readout type.
     */
    public static final int DEFAULT_READOUT_TYPE =
        IReadoutRequestElement.READOUT_TYPE_GLOBAL;

    /**
     * String value of readout type.
     */
    private static final String IIIT_GLOBAL = "IIIT_GLOBAL";
    private static final String II_GLOBAL   = "II_GLOBAL";
    private static final String II_STRING   = "II_STRING";
    private static final String II_MODULE   = "II_MODULE";
    private static final String IT_GLOBAL   = "IT_GLOBAL";
    private static final String IT_MODULE   = "IT_MODULE";

    /**
     * Type of readout (see {@link icecube.daq.payload.IReadoutRequestElement
     * IReadoutRequestElement}).
     */
    private int type;

    /**
     * Offset of readout in nanoseconds.
     */
    private int offset;

    /**
     * Time extension into past from offset, in nanoseconds.
     * This should be a non-negative number.
     */
    private int minus;

    /**
     * Time extension into future from offset, in nanoseconds.
     * This should be a non-negative number.
     */
    private int plus;

    /**
     * Default constructor.
     */
    public TriggerReadout()
    {
        this(DEFAULT_READOUT_TYPE, 0, 0, 0);
    }

    /**
     * Constructor.
     * @param type type of readout
     * @param offset offset of readout
     * @param minus time extenstion into past
     * @param plus time extenstion into future
     */
    public TriggerReadout(int type, int offset, int minus, int plus)
    {
        this.type = checkType(type);
        this.offset = offset;
        if (minus < 0) {
            LOG.warn("Readout time minus should be non-negative");
            this.minus = -minus;
        } else {
            this.minus = minus;
        }
        if (plus < 0) {
            LOG.warn("Readout time plus should be non-negative");
            this.plus = -plus;
        } else {
            this.plus = plus;
        }
    }

    /**
     * Method to check readout type against known types.
     * @param type type to check
     * @return input type or DEFAULT_READOUT_TYPE if input is unknown
     */
    private int checkType(int type)
    {
        switch (type) {
        case IReadoutRequestElement.READOUT_TYPE_GLOBAL :
        case IReadoutRequestElement.READOUT_TYPE_II_GLOBAL :
        case IReadoutRequestElement.READOUT_TYPE_II_STRING :
        case IReadoutRequestElement.READOUT_TYPE_II_MODULE :
        case IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL :
        case IReadoutRequestElement.READOUT_TYPE_IT_MODULE :
            return type;
        default :
            LOG.warn("Unknown readout type " + type);
            return DEFAULT_READOUT_TYPE;
        }
    }

    /**
     * Compare this object against <tt>object</tt>
     *
     * @param object object being checked
     *
     * @return <tt>true</tt> if the objects are equal
     */
    @Override
    public boolean equals(Object object)
    {
        return object != null && (object instanceof TriggerReadout) &&
            object.hashCode() == hashCode();
    }

    /**
     * Calculates the maximum extentent of the readout into the future.
     * @param readout TriggerReadout to use
     * @return maximum reach into future, in nanoseconds
     */
    public static int getMaxReadoutFuture(TriggerReadout readout)
    {
        return (readout.getOffset() + readout.getPlus());
    }

    /**
     * Calculates the maximum extentent of the readout into the past.
     * @param readout TriggerReadout to use
     * @return maximum reach into past, in nanoseconds
     */
    public static int getMaxReadoutPast(TriggerReadout readout)
    {
        return (readout.getOffset() - readout.getMinus());
    }

    /**
     * Get readout time minus.
     * @return time minus in nanoseconds
     */
    public int getMinus()
    {
        return minus;
    }

    /**
     * Get readout offset.
     * @return offset in nanoseconds
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * Get readout time plus.
     * @return time plus in nanoseconds
     */
    public int getPlus()
    {
        return plus;
    }

    /**
     * Get readout type.
     * @return type
     */
    public int getType()
    {
        return type;
    }

    /**
     * Get readout type as a string.
     * @return string value
     */
    public String getTypeAsString()
    {
        switch (type) {
        case IReadoutRequestElement.READOUT_TYPE_GLOBAL :
            return IIIT_GLOBAL;
        case IReadoutRequestElement.READOUT_TYPE_II_GLOBAL :
            return II_GLOBAL;
        case IReadoutRequestElement.READOUT_TYPE_II_STRING :
            return II_STRING;
        case IReadoutRequestElement.READOUT_TYPE_II_MODULE :
            return II_MODULE;
        case IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL :
            return IT_GLOBAL;
        case IReadoutRequestElement.READOUT_TYPE_IT_MODULE :
            return IT_MODULE;
        default :
            LOG.error("Unknown readout type");
            return null;
        }
    }

    /**
     * Hashcode for this readout.
     *
     * @return hash code
     */
    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }

    /**
     * Set readout time minus. This should be a non-negative value.
     * @param minus time minus in nanoseconds
     */
    public void setMinus(int minus)
    {
        if (minus < 0) {
            LOG.warn("Readout time minus should be non-negative");
            this.minus = -minus;
        } else {
            this.minus = minus;
        }
    }

    /**
     * Set readout offset.
     * @param offset offset in nanoseconds
     */
    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    /**
     * Set readout time plus. This should be a non-negative value.
     * @param plus time plus in nanoseconds
     */
    public void setPlus(int plus)
    {
        if (plus < 0) {
            LOG.warn("Readout time plus should be non-negative");
            this.plus = -plus;
        } else {
            this.plus = plus;
        }
    }

    /**
     * Set readout type.
     * Type is checked against known readout types and is set to
     * DEFAULT_READOUT_TYPE if unknown.
     * @param type type
     */
    public void setType(int type)
    {
        this.type = checkType(type);
    }

    /**
     * Print out the readout as a string.
     * @return string dump of readout
     */
    @Override
    public String toString()
    {
        return getTypeAsString() + " : " + offset + " - " + minus + " + " +
            plus;
    }
}
