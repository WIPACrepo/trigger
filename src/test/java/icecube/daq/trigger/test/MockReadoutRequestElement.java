package icecube.daq.trigger.test;

import icecube.daq.payload.IDOMID;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;

import icecube.daq.trigger.IReadoutRequestElement;

public class MockReadoutRequestElement
    implements IReadoutRequestElement
{
    private int type;
    private IUTCTime firstTime;
    private IUTCTime lastTime;
    private IDOMID domId;
    private ISourceID srcId;

    public MockReadoutRequestElement(int type, long firstTime, long lastTime,
                                     long domId, int srcId)
    {
        this(type, new MockUTCTime(firstTime), new MockUTCTime(lastTime),
             new MockDOMID(domId), new MockSourceID(srcId));
    }

    public MockReadoutRequestElement(int type, IUTCTime firstTime,
                                     IUTCTime lastTime, IDOMID domId,
                                     ISourceID srcId)
    {
        this.type = type;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
        this.domId = domId;
        this.srcId = srcId;
    }

    public IDOMID getDomID()
    {
        return domId;
    }

    public IUTCTime getFirstTimeUTC()
    {
        return firstTime;
    }

    public IUTCTime getLastTimeUTC()
    {
        return lastTime;
    }

    public int getReadoutType()
    {
        return type;
    }

    public ISourceID getSourceID()
    {
        return srcId;
    }

    public String toString()
    {
        String typeStr;
        switch (type) {
        case READOUT_TYPE_GLOBAL:
            typeStr = "GLOBAL";
            break;
        case READOUT_TYPE_II_GLOBAL:
            typeStr = "II_GLOBAL";
            break;
        case READOUT_TYPE_IT_GLOBAL:
            typeStr = "IT_GLOBAL";
            break;
        case READOUT_TYPE_II_STRING:
            typeStr = "II_STRING";
            break;
        case READOUT_TYPE_II_MODULE:
            typeStr = "II_MODULE";
            break;
        case READOUT_TYPE_IT_MODULE:
            typeStr = "IT_MODULE";
            break;
        default:
            typeStr = "UNKNOWN";
            break;
        }

        return "MockReadoutRequestElement[" + typeStr + " [" + firstTime +
            "-" + lastTime + "] dom " + domId + " src " + srcId + "]";
    }
}
