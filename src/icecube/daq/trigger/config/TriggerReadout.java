/*
 * class: TriggerReadout
 *
 * Version $Id: TriggerReadout.java,v 1.1 2005/11/23 16:38:06 toale Exp $
 *
 * Date: November 23 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import icecube.daq.trigger.IReadoutRequestElement;

/**
 * This class represents a trigger readout.
 *
 * @version $Id: TriggerReadout.java,v 1.1 2005/11/23 16:38:06 toale Exp $
 * @author pat
 */
public class TriggerReadout
{

    /**
     * Log object for this class.
     */
    private static final Log log = LogFactory.getLog(TriggerReadout.class);

    /**
     * Default readout type.
     */
    private static final int DEFAULT_READOUT_TYPE = IReadoutRequestElement.READOUT_TYPE_IIIT_GLOBAL;

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
     * Type of readout (see {@link icecube.daq.trigger.IReadoutRequestElement IReadoutRequestElement}).
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
    public TriggerReadout() {
        this(DEFAULT_READOUT_TYPE, 0, 0, 0);
    }

    /**
     * Constructor.
     * @param type type of readout
     * @param offset offset of readout
     * @param minus time extenstion into past
     * @param plus time extenstion into future
     */
    public TriggerReadout(int type, int offset, int minus, int plus) {
        this.type = checkType(type);
        this.offset = offset;
        if (minus < 0) {
            log.warn("Readout time minus should be non-negative");
            this.minus = -minus;
        } else {
            this.minus = minus;
        }
        if (plus < 0) {
            log.warn("Readout time plus should be non-negative");
            this.plus = -plus;
        } else {
            this.plus = plus;
        }
    }

    /**
     * Get readout type.
     * @return type
     */
    public int getType() {
        return type;
    }

    /**
     * Set readout type.
     * Type is checked against known readout types and is set to DEFAULT_READOUT_TYPE if unknown.
     * @param type type
     */
    public void setType(int type) {
        this.type = checkType(type);
    }

    /**
     * Get readout offset.
     * @return offset in nanoseconds
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Set readout offset.
     * @param offset offset in nanoseconds
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Get readout time minus.
     * @return time minus in nanoseconds
     */
    public int getMinus() {
        return minus;
    }

    /**
     * Set readout time minus. This should be a non-negative value.
     * @param minus time minus in nanoseconds
     */
    public void setMinus(int minus) {
        if (minus < 0) {
            log.warn("Readout time minus should be non-negative");
            this.minus = -minus;
        } else {
            this.minus = minus;
        }
    }

    /**
     * Get readout time plus.
     * @return time plus in nanoseconds
     */
    public int getPlus() {
        return plus;
    }

    /**
     * Set readout time plus. This should be a non-negative value.
     * @param plus time plus in nanoseconds
     */
    public void setPlus(int plus) {
        if (plus < 0) {
            log.warn("Readout time plus should be non-negative");
            this.plus = -plus;
        } else {
            this.plus = plus;
        }
    }

    /**
     * Get readout type as a string.
     * @return string value
     */
    public String getTypeAsString() {
        switch (type) {
            case IReadoutRequestElement.READOUT_TYPE_IIIT_GLOBAL :
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
                log.error("Unknown readout type");
                return null;
        }
    }

    /**
     * Method to check readout type against known types.
     * @param type type to check
     * @return input type or DEFAULT_READOUT_TYPE if input is unknown
     */
    private int checkType(int type) {
        switch (type) {
            case IReadoutRequestElement.READOUT_TYPE_IIIT_GLOBAL :
            case IReadoutRequestElement.READOUT_TYPE_II_GLOBAL :
            case IReadoutRequestElement.READOUT_TYPE_II_STRING :
            case IReadoutRequestElement.READOUT_TYPE_II_MODULE :
            case IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL :
            case IReadoutRequestElement.READOUT_TYPE_IT_MODULE :
                return type;
            default :
                log.warn("Unknown readout type " + type);
                return DEFAULT_READOUT_TYPE;
        }
    }

    /**
     * Print out the readout as a string.
     * @return string dump of readout
     */
    public String toString() {
        return (getTypeAsString() + " : " + offset + " - " + minus + " + " + plus);
    }

    /**
     * Calculates the maximum extentent of the readout into the past.
     * @param readout TriggerReadout to use
     * @return maximum reach into past, in nanoseconds
     */
    public static int getMaxReadoutPast(TriggerReadout readout) {
        return (readout.getOffset() - readout.getMinus());
    }

    /**
     * Calculates the maximum extentent of the readout into the future.
     * @param readout TriggerReadout to use
     * @return maximum reach into future, in nanoseconds
     */
    public static int getMaxReadoutFuture(TriggerReadout readout) {
        return (readout.getOffset() + readout.getPlus());
    }

}
