package icecube.daq.trigger.test;

import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.ISourceID;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MockReadoutRequest
    implements IReadoutRequest
{
    private int uid;
    private ISourceID srcId;
    private List elemList;

    public MockReadoutRequest()
    {
        this(-1, -1, null);
    }

    public MockReadoutRequest(IReadoutRequest rReq)
    {
        this(rReq.getUID(), rReq.getSourceID(),
             rReq.getReadoutRequestElements());
    }

    public MockReadoutRequest(int uid, int srcId)
    {
        this(uid, new MockSourceID(srcId), null);
    }

    public MockReadoutRequest(int uid, int srcId, List elemList)
    {
        this(uid, new MockSourceID(srcId), elemList);
    }

    public MockReadoutRequest(int uid, ISourceID srcId, List elemList)
    {
        this.uid = uid;
        this.srcId = srcId;
        this.elemList = elemList;
    }

    public void addElement(IReadoutRequestElement elem)
    {
        if (elemList == null) {
            elemList = new ArrayList();
        }

        elemList.add(elem);
    }

    public void addElement(int type, int srcId, long firstTime, long lastTime,
                           long domId)
    {
        addElement(new MockReadoutRequestElement(type, firstTime, lastTime,
                                                 domId, srcId));
    }

    public void addElement(int type, long firstTime, long lastTime, long domId,
                           int srcId)
    {
        addElement(new MockReadoutRequestElement(type, firstTime, lastTime,
                                                 domId, srcId));
    }

    public int getEmbeddedLength()
    {
        throw new Error("Unimplemented");
    }

    public List getReadoutRequestElements()
    {
        if (elemList == null) {
            elemList = new ArrayList();
        }

        return elemList;
    }

    public ISourceID getSourceID()
    {
        return srcId;
    }

    public int getUID()
    {
        return uid;
    }

    public int length()
    {
        throw new Error("Unimplemented");
    }

    public int putBody(ByteBuffer buf, int offset)
    {
        throw new Error("Unimplemented");
    }

    public void recycle()
    {
        // do nothing
    }
}
