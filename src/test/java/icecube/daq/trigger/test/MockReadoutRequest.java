package icecube.daq.trigger.test;

import icecube.daq.payload.ISourceID;

import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.IReadoutRequestElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class MockReadoutRequest
    implements IReadoutRequest
{
    private int uid;
    private ISourceID srcId;
    private List elems;

    public MockReadoutRequest()
    {
        this(-1, -1, null);
    }

    public MockReadoutRequest(IReadoutRequest rReq)
    {
        this(rReq.getUID(), rReq.getSourceID(),
             rReq.getReadoutRequestElements());
    }

    public MockReadoutRequest(int uid, ISourceID srcObj, List elems)
    {
        this(uid, srcObj == null ? -1 : srcObj.getSourceID(), elems);
    }

    public MockReadoutRequest(int uid, int srcId, List elems)
    {
        this.uid = uid;
        this.srcId = new MockSourceID(srcId);
        this.elems = elems;
    }

    public void add(IReadoutRequestElement elem)
    {
        if (elems == null) {
            elems = new ArrayList();
        }

        elems.add(elem);
    }

    public void add(int type, long firstTime, long lastTime, long domId,
                    int srcId)
    {
        if (elems == null) {
            elems = new ArrayList();
        }

        elems.add(new MockReadoutRequestElement(type, firstTime, lastTime,
                                                domId, srcId));
    }

    public Vector getReadoutRequestElements()
    {
        Vector vec;

        if (elems == null) {
            vec = new Vector();
        } else {
            vec = new Vector(elems);
        }

        return vec;
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
