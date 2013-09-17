package icecube.daq.trigger.control;

import icecube.daq.payload.IDOMID;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadException;
import icecube.daq.trigger.test.MockDOMID;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockTriggerRequest;
import icecube.daq.trigger.test.MockUTCTime;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.*;
import static org.junit.Assert.*;

class MockReadoutRequestElement
    implements IReadoutRequestElement
{
    private int type;
    private int srcId;
    private long firstTime;
    private long lastTime;
    private long domId;

    private IDOMID domObj;
    private ISourceID srcObj;
    private IUTCTime firstObj;
    private IUTCTime lastObj;

    public MockReadoutRequestElement(int type, int srcId, long firstTime,
                                     long lastTime, long domId)
    {
        this.type = type;
        this.srcId = srcId;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
        this.domId = domId;
    }

    public Object deepCopy()
    {
        throw new Error("Unimplemented");
    }

    public IDOMID getDomID()
    {
        if (domObj == null && domId != 0) {
            domObj = new MockDOMID(domId);
        }

        return domObj;
    }

    public long getFirstTime()
    {
        return firstTime;
    }

    public IUTCTime getFirstTimeUTC()
    {
        if (firstObj == null && firstTime != 0) {
            firstObj = new MockUTCTime(firstTime);
        }

        return firstObj;
    }

    public long getLastTime()
    {
        return lastTime;
    }

    public IUTCTime getLastTimeUTC()
    {
        if (lastObj == null && lastTime != 0) {
            lastObj = new MockUTCTime(lastTime);
        }

        return lastObj;
    }

    public int getReadoutType()
    {
        return type;
    }

    public ISourceID getSourceID()
    {
        if (srcObj == null && srcId != 0) {
            srcObj = new MockSourceID(srcId);
        }

        return srcObj;
    }

    public void put(ByteBuffer x0, int i1)
        throws PayloadException
    {
        throw new Error("Unimplemented");
    }

    public String toString()
    {
        return String.format("Elem[%d/%d/%d-%d/%d]", type, srcId, firstTime,
                             lastTime, domId);
    }
}

class MockReadoutRequest
    implements IReadoutRequest
{
    private List<IReadoutRequestElement> elems =
        new ArrayList<IReadoutRequestElement>();

    public void addElement(int type, int srcId, long firstTime, long lastTime,
                           long domId)
    {
        elems.add(new MockReadoutRequestElement(type, srcId, firstTime,
                                                lastTime, domId));
    }

    public int getEmbeddedLength()
    {
        throw new Error("Unimplemented");
    }

    public List<IReadoutRequestElement> getReadoutRequestElements()
    {
        return elems;
    }

    public ISourceID getSourceID()
    {
        throw new Error("Unimplemented");
    }

    public int getUID()
    {
        throw new Error("Unimplemented");
    }

    public long getUTCTime()
    {
        throw new Error("Unimplemented");
    }

    public int length()
    {
        throw new Error("Unimplemented");
    }

    public int putBody(ByteBuffer buf, int offset)
        throws PayloadException
    {
        throw new Error("Unimplemented");
    }

    public void recycle()
    {
        throw new Error("Unimplemented");
    }

    public void setSourceID(ISourceID srcId)
    {
        throw new Error("Unimplemented");
    }

    public void setUID(int uid)
    {
        throw new Error("Unimplemented");
    }

    public String toString()
    {
        return "RReq*" + elems.size() + ": " + elems;
    }
}

public class ElementMergerTest
{
    private static final int GLOBAL =
        IReadoutRequestElement.READOUT_TYPE_GLOBAL;
    private static final int II_GLOBAL =
        IReadoutRequestElement.READOUT_TYPE_II_GLOBAL;
    private static final int IT_GLOBAL =
        IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL;
    private static final int OTHER = 100;

    @Test
    public void testMergeMany()
    {
        MockReadoutRequest srcReq = new MockReadoutRequest();
        srcReq.addElement(GLOBAL, 2, 3, 4, 5);
        srcReq.addElement(OTHER, 0, 3, 4, 0);
        srcReq.addElement(GLOBAL, 2, 7, 8, 5);
        srcReq.addElement(II_GLOBAL, 2, 7, 8, 5);
        srcReq.addElement(GLOBAL, 2, 5, 6, 5);
        srcReq.addElement(IT_GLOBAL, 2, 5, 6, 5);

        MockTriggerRequest req = new MockTriggerRequest(1, 2, 3, 4, 5);
        req.setReadoutRequest(srcReq);

        MockReadoutRequest tgtReq = new MockReadoutRequest();

        ArrayList<ITriggerRequestPayload> list =
            new ArrayList<ITriggerRequestPayload>();
        list.add(req);

        ElementMerger.merge(tgtReq, list);
    }

    private void permute(int i0, int i1, int i2, int i3, int i4, int i5,
                         int i6, int i7)
    {
        MockReadoutRequest srcReq = new MockReadoutRequest();

        for (int j = 0; j < 8; j++) {
            if (j == i0) {
                srcReq.addElement(GLOBAL, 2, 5, 6, 5);
            } else if (j == i1) {
                srcReq.addElement(GLOBAL, 2, 7, 8, 5);
            } else if (j == i2) {
                srcReq.addElement(II_GLOBAL, 2, 3, 7, 5);
            } else if (j == i3) {
                srcReq.addElement(II_GLOBAL, 2, 6, 12, 5);
            } else if (j == i4) {
                srcReq.addElement(IT_GLOBAL, 2, 3, 4, 5);
            } else if (j == i5) {
                srcReq.addElement(IT_GLOBAL, 2, 5, 6, 5);
            } else if (j == i6) {
                srcReq.addElement(OTHER, 0, 2, 6, 0);
            } else if (j == i7) {
                srcReq.addElement(OTHER, 0, 8, 9, 0);
            }
        }

        MockTriggerRequest req = new MockTriggerRequest(1, 2, 3, 4, 5);
        req.setReadoutRequest(srcReq);

        MockReadoutRequest tgtReq = new MockReadoutRequest();

        ArrayList<ITriggerRequestPayload> list =
            new ArrayList<ITriggerRequestPayload>();
        list.add(req);

        ElementMerger.merge(tgtReq, list);

        String permName = String.format("Perm[%d/%d/%d/%d/%d/%d/%d/%d]",
                                        i0, i1, i2, i3, i4, i5, i6, i7);

        List<IReadoutRequestElement> elems =
            tgtReq.getReadoutRequestElements();
        assertNotNull(permName + " element list should not be null", elems);
        assertEquals(permName + " bad number of elements", 3, elems.size());

        for (IReadoutRequestElement elem : elems) {
            String type;
            int srcId;
            long domId;
            long firstTime;
            long lastTime;

            if (elem.getReadoutType() == OTHER) {
                type = "OTHER";
                srcId = -1;
                domId = -1;
                firstTime = 2;
                lastTime = 9;
            } else if (elem.getReadoutType() == II_GLOBAL) {
                type = "II_GLOBAL";
                srcId = 2;
                domId = 5;
                firstTime = 3;
                lastTime = 12;
            } else if (elem.getReadoutType() == IT_GLOBAL) {
                type = "IT_GLOBAL";
                srcId = 2;
                domId = 5;
                firstTime = 3;
                lastTime = 8;
            } else {
                throw new Error(permName + " bad type " +
                                elem.getReadoutType());
            }

            assertEquals(permName + " bad " + type + " source",
                         srcId, elem.getSourceID().getSourceID());
            assertEquals(permName + " bad " + type + " DOM",
                         domId, elem.getDomID().longValue());
            assertEquals(permName + " bad " + type + " first time",
                         firstTime, elem.getFirstTimeUTC().longValue());
            assertEquals(permName + " bad " + type + " last time",
                         lastTime, elem.getLastTimeUTC().longValue());
        }
    }

    @Test
    public void testMergePermutations()
    {
        final int NUM = 8;

        for (int i0 = 0; i0 < NUM; i0++) {
            for (int i1 = 0; i1 < NUM; i1++) {
                if (i1 == i0) continue;
                for (int i2 = 0; i2 < NUM; i2++) {
                    if (i2 == i0 || i2 == i1) continue;
                    for (int i3 = 0; i3 < NUM; i3++) {
                        if (i3 == i0 || i3 == i1 || i3 == i2) continue;
                        for (int i4 = 0; i4 < NUM; i4++) {
                            if (i4 == i0 || i4 == i1 || i4 == i2 || i4 == i3)
                                continue;
                            for (int i5 = 0; i5 < NUM; i5++) {
                                if (i5 == i0 || i5 == i1 || i5 == i2 ||
                                    i5 == i3 || i5 == i4)
                                    continue;
                                for (int i6 = 0; i6 < NUM; i6++) {
                                    if (i6 == i0 || i6 == i1 || i6 == i2 ||
                                        i6 == i3 || i6 == i4 || i6 == i5)
                                        continue;
                                    for (int i7 = 0; i7 < NUM; i7++) {
                                        if (i7 == i0 || i7 == i1 || i7 == i2 ||
                                            i7 == i3 || i7 == i4 || i7 == i5 ||
                                            i7 == i6)
                                            continue;

                                        permute(i0, i1, i2, i3, i4, i5, i6,
                                                i7);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testMergeII()
    {
        final int srcId = 2;
        final long domId = 5;
        final long firstTime = 100L;
        final long inc = 50L;
        final long lastTime = 200L;

        MockReadoutRequest srcReq = new MockReadoutRequest();
        srcReq.addElement(II_GLOBAL, srcId, firstTime, firstTime + inc, domId);
        srcReq.addElement(II_GLOBAL, srcId, lastTime - inc, lastTime, domId);

        MockTriggerRequest req = new MockTriggerRequest(1, 2, 3, 4, 5);
        req.setReadoutRequest(srcReq);

        MockReadoutRequest tgtReq = new MockReadoutRequest();

        ArrayList<ITriggerRequestPayload> list =
            new ArrayList<ITriggerRequestPayload>();
        list.add(req);

        ElementMerger.merge(tgtReq, list);

        List<IReadoutRequestElement> elems =
            tgtReq.getReadoutRequestElements();
        assertNotNull("Element list should not be null", elems);
        assertEquals("Bad number of elements", 1, elems.size());

        IReadoutRequestElement elem = elems.get(0);
        assertEquals("Bad type", II_GLOBAL, elem.getReadoutType());
        assertEquals("Bad source", srcId, elem.getSourceID().getSourceID());
        assertEquals("Bad DOM", domId, elem.getDomID().longValue());
        assertEquals("Bad first time",
                     firstTime, elem.getFirstTimeUTC().longValue());
        assertEquals("Bad last time",
                     lastTime, elem.getLastTimeUTC().longValue());
    }

    @Test
    public void testMergeIT()
    {
        final int srcId = 1234;
        final long domId = 54321;
        final long firstTime = 10000L;
        final long inc = 500L;
        final long lastTime = 20000L;

        MockReadoutRequest srcReq = new MockReadoutRequest();
        srcReq.addElement(IT_GLOBAL, srcId, firstTime, firstTime + inc, domId);
        srcReq.addElement(IT_GLOBAL, srcId, lastTime - inc, lastTime, domId);

        MockTriggerRequest req = new MockTriggerRequest(1, 2, 3, 4, 5);
        req.setReadoutRequest(srcReq);

        MockReadoutRequest tgtReq = new MockReadoutRequest();

        ArrayList<ITriggerRequestPayload> list =
            new ArrayList<ITriggerRequestPayload>();
        list.add(req);

        ElementMerger.merge(tgtReq, list);

        List<IReadoutRequestElement> elems =
            tgtReq.getReadoutRequestElements();
        assertNotNull("Element list should not be null", elems);
        assertEquals("Bad number of elements", 1, elems.size());

        IReadoutRequestElement elem = elems.get(0);
        assertEquals("Bad type", IT_GLOBAL, elem.getReadoutType());
        assertEquals("Bad source", srcId, elem.getSourceID().getSourceID());
        assertEquals("Bad DOM", domId, elem.getDomID().longValue());
        assertEquals("Bad first time",
                     firstTime, elem.getFirstTimeUTC().longValue());
        assertEquals("Bad last time",
                     lastTime, elem.getLastTimeUTC().longValue());
    }
}
