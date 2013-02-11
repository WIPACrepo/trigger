package icecube.daq.trigger.control;

import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.impl.ReadoutRequestElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for accumulated request element data.
 */
class ElementData
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
        this.firstTime = Long.MAX_VALUE;
        this.lastTime = Long.MIN_VALUE;

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

    void addTo(IReadoutRequest rReq)
    {
        rReq.addElement(type, srcId, firstTime, lastTime, domId);
    }

    void addToRange(ElementData data)
    {
        addToRange(data.firstTime, data.lastTime);
    }

    void addToRange(IReadoutRequestElement rre)
    {
        IUTCTime firstUTC = rre.getFirstTimeUTC();
        IUTCTime lastUTC = rre.getLastTimeUTC();
        addToRange((firstUTC == null ? firstTime : firstUTC.longValue()),
                   (lastUTC == null ? lastTime : lastUTC.longValue()));
    }

    private void addToRange(long firstNew, long lastNew)
    {
        if (firstNew < firstTime) {
            firstTime = firstNew;
        }

        if (lastNew > lastTime) {
            lastTime = lastNew;
        }
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

    boolean matches(IReadoutRequestElement rre)
    {
        // if types don't match we're done
        if (type != rre.getReadoutType()) {
            return false;
        }

        // if sources don't match, we're done
        if (srcId < 0) {
            if (rre.getSourceID() != null &&
                rre.getSourceID().getSourceID() >= 0)
            {
                return false;
            }
        } else if (rre.getSourceID() == null) {
            return false;
        } else if (srcId != rre.getSourceID().getSourceID()) {
            return false;
        }

        // if DOMs don't match, we're done
        if (domId == IReadoutRequestElement.NO_DOM) {
            if (rre.getDomID() != null && rre.getDomID().longValue() >= 0) {
                return false;
            }
        } else if (rre.getDomID() == null) {
            return false;
        } else if (domId != rre.getDomID().longValue()) {
            return false;
        }

        // this is the correct element data
        return true;
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder("[");
        buf.append(ReadoutRequestElement.getTypeString(type));

        if (srcId >= 0) {
            buf.append(" src ").append(srcId);
        }

        buf.append('[').append(firstTime).append('-').append(lastTime).
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
    private static void addData(IReadoutRequestElement rre,
                         List<ElementData> dataList)
    {
        ElementData data = null;
        for (ElementData ed : dataList) {
            if (ed.matches(rre)) {
                data = ed;
                break;
            }
        }

        if (data == null) {
            data = new ElementData(rre);
            dataList.add(data);
        }

        data.addToRange(rre);
    }

    private static void collapseGlobal(List<ElementData> dataList)
    {
        ElementData global = null;
        ElementData iiGlobal = null;
        ElementData itGlobal = null;

        for (ElementData d : dataList) {
            switch (d.getType()) {
            case IReadoutRequestElement.READOUT_TYPE_GLOBAL:
                global = d;
                break;
            case IReadoutRequestElement.READOUT_TYPE_II_GLOBAL:
                iiGlobal = d;
                break;
            case IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL:
                itGlobal = d;
                break;
            default:
                break;
            }
        }

        if (global != null) {
            final int noString = IReadoutRequestElement.NO_STRING;
            final long noDOM = IReadoutRequestElement.NO_DOM;

            if (itGlobal != null) {
                itGlobal.addToRange(global);
            } else {
                final int type =
                    IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL;
                dataList.add(new ElementData(type, global.getFirstTime(),
                                             global.getLastTime(), noString,
                                             noDOM));
            }

            if (iiGlobal != null) {
                iiGlobal.addToRange(global);
            } else {
                final int type =
                    IReadoutRequestElement.READOUT_TYPE_II_GLOBAL;
                dataList.add(new ElementData(type, global.getFirstTime(),
                                             global.getLastTime(), noString,
                                             noDOM));
            }

            dataList.remove(global);
        }
    }

    /**
     * Merge all ReadoutRequestElement ranges into one or two global elements
     * and add the new elements to the IReadoutRequest.
     *
     * @param rReq ReadoutRequest which holds the new elements
     * @param reqList list of trigger requests to be merged
     */
    public static void merge(IReadoutRequest rReq,
                             List<ITriggerRequestPayload> reqList)
    {
        ArrayList<ElementData> dataList = new ArrayList<ElementData>();

        for (ITriggerRequestPayload tr : reqList) {
            IReadoutRequest rr = tr.getReadoutRequest();
            if (rr != null) {
                for (Object obj : rr.getReadoutRequestElements()) {
                    IReadoutRequestElement rre = (IReadoutRequestElement) obj;

                    addData(rre, dataList);
                }
            }
        }

        collapseGlobal(dataList);

        for (ElementData ed : dataList) {
            ed.addTo(rReq);
        }
    }
}
