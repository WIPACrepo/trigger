package icecube.daq.trigger.control;

import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.impl.ReadoutRequestElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Container for accumulated request element data.
 */
class ElementData
    implements Comparable<ElementData>
{
    private int type;
    private long firstTime;
    private long lastTime;
    private int srcId;
    private long domId;

    ElementData(int type, long firstTime, long lastTime, int srcId,
                long domId)
    {
        this.type = type;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
        this.srcId = srcId;
        this.domId = domId;
    }

    ElementData(IReadoutRequestElement rre)
    {
        this.type = rre.getReadoutType();
        this.firstTime = getTime(rre.getFirstTimeUTC(), Long.MAX_VALUE);
        this.lastTime = getTime(rre.getLastTimeUTC(), Long.MIN_VALUE);

        if (rre.getSourceID() == null) {
            srcId = IReadoutRequestElement.NO_STRING;
        } else {
            srcId = rre.getSourceID().getSourceID();
        }

        if (rre.getDomID() == null) {
            domId = IReadoutRequestElement.NO_DOM;
        } else {
            domId = rre.getDomID().longValue();
        }
    }

    private static long getTime(IUTCTime utcTime, long defaultTime)
    {
        if (utcTime == null) {
            return defaultTime;
        }

        return utcTime.longValue();
    }

    boolean addToRange(ElementData data)
    {
        return addToRange(data.firstTime, data.lastTime);
    }

    private boolean addToRange(long firstNew, long lastNew)
    {
        if (lastNew < firstTime || firstNew > lastTime) {
            return false;
        }

        if (firstNew < firstTime) {
            firstTime = firstNew;
        }

        if (lastNew > lastTime) {
            lastTime = lastNew;
        }

        return true;
    }

    @Override
    public int compareTo(ElementData ed)
    {
        int val = type - ed.type;
        if (val == 0) {
            val = srcId - ed.srcId;
            if (val == 0) {
                long lval = domId - ed.domId;
                if (lval == 0) {
                    lval = firstTime - ed.firstTime;
                    if (lval == 0) {
                        lval = lastTime - ed.lastTime;
                    }
                }

                if (lval == 0) {
                    val = 0;
                } else if (lval < 0) {
                    val = -1;
                } else {
                    val = 1;
                }
            }
        }

        return val;
    }

    void convertToElement(IReadoutRequest rReq)
    {
        rReq.addElement(type, srcId, firstTime, lastTime, domId);
    }

    /**
     * Compare this DomSet with another object.
     *
     * @param other object being compared
     *
     * @return <tt>true</tt> if both sets contain the same DOM IDs
     */
    @Override
    public boolean equals(Object other)
    {
        if (other == null) {
            return false;
        }

        if (!(other instanceof ElementData)) {
            return getClass().equals(other.getClass());
        }

        return equals((ElementData) other);
    }

    /**
     * Is the specified object equal to this object?
     * @param obj object being compared
     * @return <tt>true</tt> if the objects are equal
     */
    public boolean equals(ElementData ed)
    {
        return compareTo(ed) == 0;
    }

    public long getFirstTime()
    {
        return firstTime;
    }

    public long getLastTime()
    {
        return lastTime;
    }

    public int getType()
    {
        return type;
    }

    /**
     * Return this object's hash code
     * @return hash code
     */
    @Override
    public int hashCode()
    {
        int high = type;
        if (srcId >= 0) {
            high += srcId;
        }
        if (domId >= 0) {
            high += (int)(domId & 0x3fffffff);
        }

        return ((high & 0xffff) << 16) + (int)(firstTime & 0xffff);
    }

    boolean matches(ElementData ed)
    {
        return type == ed.type && srcId == ed.srcId && domId == ed.domId;
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder("[");
        buf.append(ReadoutRequestElement.getTypeString(type));

        if (srcId >= 0) {
            buf.append(" src ").append(srcId);
        }

        buf.append(" [").append(firstTime).append('-').append(lastTime).
            append(']');

        if (domId >= 0) {
            buf.append(" dom ").append(domId);
        }

        return buf.toString();
    }
}

/**
 * Merge readout request elements from a group of trigger requests.
 */
public abstract class ElementMerger
{
    private static final Logger LOG = Logger.getLogger(ElementMerger.class);

    private static List<ElementData> addToList(List<ElementData> list,
                                               int type,
                                               ElementData glbl)
    {
        final int noString = IReadoutRequestElement.NO_STRING;
        final long noDOM = IReadoutRequestElement.NO_DOM;

        boolean added = false;
        if (list != null) {
            for (ElementData ed : list) {
                if (ed.addToRange(glbl)) {
                    added = true;
                    break;
                }
            }
        }

        if (!added) {
            if (list == null) {
                list = new ArrayList<ElementData>();
            }

            list.add(new ElementData(type, glbl.getFirstTime(),
                                     glbl.getLastTime(), noString,
                                     noDOM));
        }

        return list;
    }

    private static void collapseGlobal(List<ElementData> dataList)
    {
        List<ElementData> global = null;
        List<ElementData> iiGlobal = null;
        List<ElementData> itGlobal = null;
        List<ElementData> other = null;

        for (ElementData d : dataList) {
            switch (d.getType()) {
            case IReadoutRequestElement.READOUT_TYPE_GLOBAL:
                if (global == null) {
                    global = new ArrayList<ElementData>();
                }
                global.add(d);
                break;
            case IReadoutRequestElement.READOUT_TYPE_II_GLOBAL:
                if (iiGlobal == null) {
                    iiGlobal = new ArrayList<ElementData>();
                }
                iiGlobal.add(d);
                break;
            case IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL:
                if (itGlobal == null) {
                    itGlobal = new ArrayList<ElementData>();
                }
                itGlobal.add(d);
                break;
            default:
                if (other == null) {
                    other = new ArrayList<ElementData>();
                }

                final String errMsg =
                    String.format("Not merging ReadoutRequestElement type#%d" +
                                  " (range [%d-%d])", d.getType(),
                                  d.getFirstTime(), d.getLastTime());
                LOG.error(errMsg);

                other.add(d);
                break;
            }
        }

        // always split GLOBAL requests into localized GLOBAL requests
        if (global != null) {
            final int iiType = IReadoutRequestElement.READOUT_TYPE_II_GLOBAL;
            final int itType = IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL;

            for (ElementData ed : global) {
                iiGlobal = addToList(iiGlobal, iiType, ed);
                itGlobal = addToList(itGlobal, itType, ed);
            }

            dataList.clear();
            if (iiGlobal != null) {
                dataList.addAll(iiGlobal);
            }
            if (itGlobal != null) {
                dataList.addAll(itGlobal);
            }
            if (other != null) {
                dataList.addAll(other);
            }
        }
    }

    /**
     * Merge all ReadoutRequestElement ranges into non-overlapping elements
     * and add the new elements to the IReadoutRequest.
     *
     * @param rReq ReadoutRequest which holds the new elements
     * @param reqList list of trigger requests to be merged
     */
    public static void merge(IReadoutRequest rReq,
                             List<ITriggerRequestPayload> reqList)
    {
        ArrayList<ElementData> initialList = new ArrayList<ElementData>();

        for (ITriggerRequestPayload tr : reqList) {
            IReadoutRequest rr = tr.getReadoutRequest();
            if (rr == null) {
                LOG.warn("No readout requests found in " + tr);
            } else {
                for (Object obj : rr.getReadoutRequestElements()) {
                    IReadoutRequestElement rre = (IReadoutRequestElement) obj;

                    initialList.add(new ElementData(rre));
                }
            }
        }

        collapseGlobal(initialList);

        Collections.sort(initialList);

        ArrayList<ElementData> dataList = new ArrayList<ElementData>();

        for (ElementData ed : initialList) {
            boolean merged = false;
            for (ElementData data : dataList) {
                if (data.matches(ed)) {
                    if (data.addToRange(ed)) {
                        merged = true;
                        break;
                    }
                }
            }

            if (!merged) {
                dataList.add(ed);
            }
        }

        // add all ranges to the ReadoutRequest as ReadoutRequestElements
        for (ElementData ed : dataList) {
            ed.convertToElement(rReq);
        }
    }
}
