package icecube.daq.trigger.test;

import icecube.daq.payload.ISourceID;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.IReadoutRequestElement;

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

    public void addElement(int type, long firstTime, long lastTime, long domId,
                           int srcId)
    {
        addElement(new MockReadoutRequestElement(type, firstTime, lastTime,
                                                 domId, srcId));
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
}
