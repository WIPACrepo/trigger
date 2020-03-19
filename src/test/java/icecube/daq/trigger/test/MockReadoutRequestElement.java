package icecube.daq.trigger.test;

import icecube.daq.payload.IDOMID;
import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayloadRecord;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MockReadoutRequestElement
    implements IReadoutRequestElement, IWriteablePayloadRecord
{
    private int type;
    private long firstTime;
    private long lastTime;
    private long domId;
    private int srcId;

    private IDOMID domObj;
    private ISourceID srcObj;
    private IUTCTime firstObj;
    private IUTCTime lastObj;

    public MockReadoutRequestElement(int type, long firstTime, long lastTime,
                                     long domId, int srcId)
    {
        if (type == READOUT_TYPE_GLOBAL ||
            type == READOUT_TYPE_II_GLOBAL ||
            type == READOUT_TYPE_IT_GLOBAL)
        {
            if (srcId != NO_STRING) {
                final String errStr =
                    String.format("Cannot specify source #%d" +
                                  " with global element type #%d",
                                  srcId, type);
                throw new Error(errStr);
            }
            if (domId != NO_DOM) {
                final String errStr =
                    String.format("Cannot specify DOM %012x" +
                                  " with global element type #%d",
                                  domId, type);
                throw new Error(errStr);
            }
        }

        this.type = type;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
        this.domId = domId;
        this.srcId = srcId;
    }

    public MockReadoutRequestElement(IReadoutRequestElement elem)
    {
        if (elem == null) {
            throw new Error("Cannot copy null readout request element");
        }

        type = elem.getReadoutType();
        firstTime = elem.getFirstTime();
        lastTime = elem.getLastTime();

        final boolean isGlobal =
            type == READOUT_TYPE_GLOBAL ||
            type == READOUT_TYPE_II_GLOBAL ||
            type == READOUT_TYPE_IT_GLOBAL;

        if (isGlobal || elem.getSourceID() == null) {
            srcId = IReadoutRequestElement.NO_STRING;
        } else {
            srcId = elem.getSourceID().getSourceID();
        }

        if (isGlobal || elem.getDOMID() == null) {
            domId = IReadoutRequestElement.NO_DOM;
        } else {
            domId = elem.getDOMID().longValue();
        }
    }

    @Override
    public Object deepCopy()
    {
        throw new Error("Unimplemented");
    }

    @Override
    public IDOMID getDOMID()
    {
        if (domObj == null && domId != IReadoutRequestElement.NO_DOM) {
            domObj = new MockDOMID(domId);
        }

        return domObj;
    }

    @Override
    public long getFirstTime()
    {
        return firstTime;
    }

    @Override
    public IUTCTime getFirstTimeUTC()
    {
        if (firstObj == null && firstTime != 0) {
            firstObj = new MockUTCTime(firstTime);
        }

        return firstObj;
    }

    @Override
    public long getLastTime()
    {
        return lastTime;
    }

    @Override
    public IUTCTime getLastTimeUTC()
    {
        if (lastObj == null && lastTime != 0) {
            lastObj = new MockUTCTime(lastTime);
        }

        return lastObj;
    }

    @Override
    public int getReadoutType()
    {
        return type;
    }

    @Override
    public ISourceID getSourceID()
    {
        if (srcObj == null && srcId != IReadoutRequestElement.NO_STRING) {
            srcObj = new MockSourceID(srcId);
        }

        return srcObj;
    }

    /**
     * Determines if this record is loaded with valid data.
     * @return <tt>true</tt> if data is loaded, <tt>false</tt> otherwise.
     */
    @Override
    public boolean isDataLoaded()
    {
        throw new Error("Unimplemented");
    }

    /**
     * Loads the data from the buffer into the container record.
     *
     * @param offset the offset into the byte buffer
     * @param buffer ByteBuffer from which to construct the record.
     */
    @Override
    public void loadData(int offset, ByteBuffer buffer)
    {
        throw new Error("Unimplemented");
    }

    /**
     * Write this element to the byte buffer
     * @param buf byte buffer
     * @param offset index of first byte
     */
    @Override
    public void put(ByteBuffer buf, int offset)
    {
        throw new Error("Unimplemented");
    }

    /**
     * Set readout type
     *
     * @param type new readout type
     */
    public void setReadoutType(int type)
    {
        this.type = type;
    }

    /**
     * Write this record to the bute buffer.
     *
     * @param iOffset the offset at which to start writing the object.
     * @param tBuffer the ByteBuffer into which to write this payload-record
.
     * @return the number of bytes written to this byte buffer.
     */
    @Override
    public int writeData(int iOffset, ByteBuffer tBuffer)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    @Override
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

        return "MockReadoutRequestElement[" + typeStr +
            " [" + firstTime + "-" + lastTime + "]" +
            " dom " + (domId == IReadoutRequestElement.NO_DOM ? "NO_DOM" :
                       String.format("%012x", domId)) +
            " src " + MockSourceID.toString(srcId) +
            "]";
    }
}
