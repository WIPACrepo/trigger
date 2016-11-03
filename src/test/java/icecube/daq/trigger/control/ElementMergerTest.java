package icecube.daq.trigger.control;

import icecube.daq.common.MockAppender;
import icecube.daq.payload.IDOMID;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadException;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.test.MockDOMID;
import icecube.daq.trigger.test.MockReadoutRequest;
import icecube.daq.trigger.test.MockReadoutRequestElement;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockTriggerRequest;
import icecube.daq.trigger.test.MockUTCTime;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

import org.junit.*;
import static org.junit.Assert.*;

class RdoutReqElemComparator
    implements Comparator<IReadoutRequestElement>
{
    public int compare(IReadoutRequestElement e1, IReadoutRequestElement e2)
    {
        int val = e2.getReadoutType() - e1.getReadoutType();
        if (val == 0) {
            long tmp = e1.getFirstTime() - e2.getFirstTime();
            if (tmp < 0) {
                val = -1;
            } else if (tmp > 0) {
                val = 1;
            } else {
                tmp = e1.getLastTime() - e2.getLastTime();
                if (tmp < 0) {
                    val = -1;
                } else if (tmp > 0) {
                    val = 1;
                } else {
                    val = 0;
                }
            }
        }

        return val;
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

    private static final int NO_STRING = IReadoutRequestElement.NO_STRING;
    private static final long NO_DOM = IReadoutRequestElement.NO_DOM;

    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    @Before
    public void setUp()
        throws Exception
    {
        appender.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);
    }

    @After
    public void tearDown()
        throws Exception
    {
        appender.assertNoLogMessages();
    }

    private static final void assertDOMId(long domId, IDOMID domObj)
    {
        if (domObj == null) {
            if (domId != NO_DOM) {
                fail(String.format("Got null DOM ID, expected #%012x", domId));
            }
        } else {
            assertEquals("Bad DOM", domId, domObj.longValue());
        }
    }

    private static final void assertSourceId(int srcId, ISourceID srcObj)
    {
        if (srcObj == null) {
            if (srcId != NO_STRING) {
                fail("Got null source ID, expected #" + srcId);
            }
        } else {
            assertEquals("Bad source", srcId, srcObj.getSourceID());
        }
    }

    @Test
    public void testMergeMany()
    {
        MockReadoutRequest srcReq = new MockReadoutRequest();
        srcReq.addElement(GLOBAL, NO_STRING, 3, 4, NO_DOM);
        srcReq.addElement(OTHER, NO_STRING, 3, 4, NO_DOM);
        srcReq.addElement(GLOBAL, NO_STRING, 7, 8, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, 7, 8, NO_DOM);
        srcReq.addElement(GLOBAL, NO_STRING, 5, 6, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, 5, 6, NO_DOM);

        final List<IReadoutRequestElement> expElems =
            collapseAndMerge(srcReq.getReadoutRequestElements());

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
        assertEquals("Bad number of elements", expElems.size(), elems.size());

        compareElements("MergeMany", expElems, elems);

        final String msg = "Not merging ReadoutRequestElement type#" + OTHER +
            " (range [3-4])";
        appender.assertLogMessage(msg);
        appender.assertNoLogMessages();
    }

    private List<IReadoutRequestElement>
        collapseAndMerge(List<IReadoutRequestElement> origList)
    {
        final int typeGlobal =
            IReadoutRequestElement.READOUT_TYPE_GLOBAL;
        final int typeInice =
            IReadoutRequestElement.READOUT_TYPE_II_GLOBAL;
        final int typeIcetop =
            IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL;
        boolean foundGlobal = false;
        boolean foundLocal = false;
        for (IReadoutRequestElement elem : origList) {
            if (elem.getReadoutType() == typeGlobal) {
                foundGlobal = true;
                if (foundLocal) {
                    break;
                }
            } else if (elem.getReadoutType() == typeInice ||
                       elem.getReadoutType() == typeIcetop)
            {
                foundLocal = true;
                if (foundGlobal) {
                    break;
                }
            }
        }

        List<IReadoutRequestElement> sorted;
        if (!foundGlobal || !foundLocal) {
            sorted = new ArrayList<IReadoutRequestElement>(origList);
        } else {
            sorted = collapseGlobal(origList);
        }
        Collections.sort(sorted, new RdoutReqElemComparator());

        List<IReadoutRequestElement> elems =
            new ArrayList<IReadoutRequestElement>();

        IReadoutRequestElement prevElem = null;
        for (IReadoutRequestElement elem : sorted) {
            if (prevElem != null) {
                if (prevElem.getReadoutType() != elem.getReadoutType()) {
                    // cannot merge new element with different type
                    elems.add(prevElem);
                } else if (!isSameSource(prevElem.getSourceID(),
                                        elem.getSourceID()))
                {
                    // cannot merge new element with different source
                    elems.add(prevElem);
                } else if (!isSameDOM(prevElem.getDomID(), elem.getDomID())) {
                    // cannot merge new element with different DOM
                    elems.add(prevElem);
                } else if (prevElem.getLastTime() < elem.getFirstTime()) {
                    // cannot merge new element with non-overlapping time
                    elems.add(prevElem);
                } else {
                    long lastTime;
                    if (prevElem.getLastTime() < elem.getLastTime()) {
                        lastTime = elem.getLastTime();
                    } else {
                        lastTime = prevElem.getLastTime();
                    }

                    // merge overlapping elements
                    elem =
                        new MockReadoutRequestElement(elem.getReadoutType(),
                                                      prevElem.getFirstTime(),
                                                      lastTime, NO_DOM,
                                                      NO_STRING);
                }
            }

            prevElem = elem;
        }

        if (prevElem != null) {
            // add final element
            elems.add(prevElem);
        }

        return elems;
    }

    private List<IReadoutRequestElement>
        collapseGlobal(List<IReadoutRequestElement> origList)
    {
        final int typeGlobal =
            IReadoutRequestElement.READOUT_TYPE_GLOBAL;

        List<IReadoutRequestElement> newList =
            new ArrayList<IReadoutRequestElement>();
        for (IReadoutRequestElement elem : origList) {
            if (elem.getReadoutType() != typeGlobal) {
                newList.add(elem);
                continue;
            }

            for (int i = 0; i < 2; i++) {
                MockReadoutRequestElement newElem =
                    new MockReadoutRequestElement(elem);

                int newType;
                if (i == 0) {
                    newType = IReadoutRequestElement.READOUT_TYPE_II_GLOBAL;
                } else {
                    newType = IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL;
                }
                newElem.setReadoutType(newType);

                newList.add(newElem);
            }
        }

        return newList;
    }

    private void compareElements(String name,
                                 List<IReadoutRequestElement> expList,
                                 List<IReadoutRequestElement> gotList)
    {
        List<IReadoutRequestElement> tmpList =
            new ArrayList<IReadoutRequestElement>(expList);
        for (IReadoutRequestElement elem : gotList) {
//System.out.println("CMP " + elem);
            IReadoutRequestElement foundElem = null;
            for (IReadoutRequestElement chkElem : tmpList) {
//System.out.println("    CHK " + chkElem);
                if (elem.getReadoutType() == chkElem.getReadoutType() &&
                    elem.getFirstTime() == chkElem.getFirstTime() &&
                    elem.getLastTime() == chkElem.getLastTime() &&
                    isSameDOM(elem.getDomID(), chkElem.getDomID()) &&
                    isSameSource(elem.getSourceID(), chkElem.getSourceID()))
                {
                    foundElem = chkElem;
                    break;
                }
            }

            if (foundElem == null) {
                fail(String.format("Found unknown %s element %s", name, elem));
            }

            tmpList.remove(foundElem);
        }

        assertEquals("Expected extra elements", 0, tmpList.size());
    }

    private boolean isSameDOM(IDOMID dom1, IDOMID dom2)
    {
        if (dom1 == null) {
            return dom2 == null;
        } else if (dom2 == null) {
            return false;
        }

        return dom1.equals(dom2);
    }

    private boolean isSameSource(ISourceID src1, ISourceID src2)
    {
        if (src1 == null) {
            return src2 == null;
        } else if (src2 == null) {
            return false;
        }

        return src1.equals(src2);
    }

    private void permute(int i0, int i1, int i2, int i3, int i4, int i5,
                         int i6, int i7)
    {
        MockReadoutRequest srcReq = new MockReadoutRequest();

        for (int j = 0; j < 8; j++) {
            if (j == i0) {
                srcReq.addElement(GLOBAL, NO_STRING, 5, 6, NO_DOM);
            } else if (j == i1) {
                srcReq.addElement(GLOBAL, NO_STRING, 7, 8, NO_DOM);
            } else if (j == i2) {
                srcReq.addElement(II_GLOBAL, NO_STRING, 3, 7, NO_DOM);
            } else if (j == i3) {
                srcReq.addElement(II_GLOBAL, NO_STRING, 6, 12, NO_DOM);
            } else if (j == i4) {
                srcReq.addElement(IT_GLOBAL, NO_STRING, 3, 4, NO_DOM);
            } else if (j == i5) {
                srcReq.addElement(IT_GLOBAL, NO_STRING, 5, 6, NO_DOM);
            } else if (j == i6) {
                srcReq.addElement(OTHER, 0, 2, 6, 0);
            } else if (j == i7) {
                srcReq.addElement(OTHER, 0, 8, 9, 0);
            }
        }

        final String permName = String.format("Perm[%d/%d/%d/%d/%d/%d/%d/%d]",
                                              i0, i1, i2, i3, i4, i5, i6, i7);

        final List<IReadoutRequestElement> expElems =
            collapseAndMerge(srcReq.getReadoutRequestElements());

        MockTriggerRequest req = new MockTriggerRequest(1, 2, 3, 4, 5);
        req.setReadoutRequest(srcReq);

        MockReadoutRequest tgtReq = new MockReadoutRequest();

        ArrayList<ITriggerRequestPayload> list =
            new ArrayList<ITriggerRequestPayload>();
        list.add(req);

        ElementMerger.merge(tgtReq, list);

        List<IReadoutRequestElement> elems =
            tgtReq.getReadoutRequestElements();
        assertNotNull(permName + " element list should not be null", elems);
        assertEquals(permName + " bad number of elements",
                     expElems.size(), elems.size());

        compareElements(permName, expElems, elems);

        try {
            assertEquals("Bad number of log messages",
                         2, appender.getNumberOfMessages());
            for (int i = 0; i < 2; i++) {
                final String msg = (String) appender.getMessage(i);

                final String front = "Not merging ReadoutRequestElement" +
                    " type#" + OTHER + " (range ";
                assertTrue("Bad log message " + msg, msg.startsWith(front));

                boolean found = false;

                String[] back = new String[] { "[2-6])", "[8-9])" };
                for (int r = 0; r < back.length; r++) {
                    if (msg.endsWith(back[r])) {
                        found = true;
                        break;
                    }
                }

                assertTrue("Bad log message " + msg, found);
            }
        } finally {
            appender.clear();
        }
    }

    @Test
    public void testCompare()
    {
        ElementData ed1 = new ElementData(GLOBAL, 5L, 6L, NO_STRING, NO_DOM);
        ElementData ed2 =
            new ElementData(IT_GLOBAL, 3L, 4L, NO_STRING, NO_DOM);
        ElementData ed3 =
            new ElementData(GLOBAL, 7L, 8L, NO_STRING, NO_DOM);
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
        final long firstTime = 100L;
        final long inc = 50L;
        final long lastTime = 200L;

        MockReadoutRequest srcReq = new MockReadoutRequest();
        srcReq.addElement(II_GLOBAL, NO_STRING, firstTime, firstTime + inc,
                          NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, lastTime - inc, lastTime,
                          NO_DOM);

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
        assertSourceId(NO_STRING, elem.getSourceID());
        assertDOMId(NO_DOM, elem.getDomID());
        assertEquals("Bad first time",
                     firstTime, elem.getFirstTime());
        assertEquals("Bad last time",
                     lastTime, elem.getLastTime());
    }

    @Test
    public void testMergeGI()
    {
        final long firstTime = 100L;
        final long inc = 50L;
        final long lastTime = 200L;

        MockReadoutRequest srcReq = new MockReadoutRequest();
        srcReq.addElement(GLOBAL, NO_STRING, firstTime, firstTime + inc,
                          NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, lastTime - inc, lastTime,
                          NO_DOM);

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
        assertEquals("Bad number of elements", 2, elems.size());

        boolean sawOne = false;
        boolean sawTwo = false;

        for (IReadoutRequestElement elem : elems) {
            long expFirst;
            long expLast;
            if (elem.getReadoutType() == II_GLOBAL) {
                expFirst = firstTime;
                expLast = lastTime;
                sawOne = true;
            } else if (elem.getReadoutType() == IT_GLOBAL) {
                expFirst = firstTime;
                expLast = lastTime - inc;
                sawTwo = true;
            } else {
                fail("Unknown readout type #" + elem.getReadoutType() +
                     "in " + elem);
                break;
            }

            assertSourceId(NO_STRING, elem.getSourceID());
            assertDOMId(NO_DOM, elem.getDomID());

            assertEquals("Bad first time for " + elem,
                         expFirst, elem.getFirstTime());
            assertEquals("Bad last time for " + elem,
                         expLast, elem.getLastTime());
        }

        assertTrue("Didn't see first request", sawOne);
        assertTrue("Didn't see second request", sawTwo);
    }

    @Test
    public void testMergeIT()
    {
        final long inc = 500L;
        final long firstOne = 10000L;
        final long lastOne = firstOne + inc;
        final long firstTwo = 20000L;
        final long lastTwo = firstTwo + inc;

        final int uid = 1;
        final int type = 2;
        final int cfgId = 3;

        MockTriggerRequest req = new MockTriggerRequest(uid, type, cfgId,
                                                        firstOne, lastTwo);

        MockReadoutRequest srcReq = new MockReadoutRequest();
        srcReq.addElement(IT_GLOBAL, NO_STRING, firstOne, lastOne, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, firstTwo, lastTwo, NO_DOM);

        req.setReadoutRequest(srcReq);

        MockReadoutRequest tgtReq = new MockReadoutRequest();

        ArrayList<ITriggerRequestPayload> list =
            new ArrayList<ITriggerRequestPayload>();
        list.add(req);

        ElementMerger.merge(tgtReq, list);

        List<IReadoutRequestElement> elems =
            tgtReq.getReadoutRequestElements();
        assertNotNull("Element list should not be null", elems);
        assertEquals("Bad number of elements", 2, elems.size());

        boolean sawOne = false;
        boolean sawTwo = false;

        for (IReadoutRequestElement elem : elems) {
            assertEquals("Bad type", IT_GLOBAL, elem.getReadoutType());
            assertSourceId(NO_STRING, elem.getSourceID());
            assertDOMId(NO_DOM, elem.getDomID());

            if (!sawOne && elem.getFirstTime() == firstOne) {
                sawOne = true;
                assertEquals("Bad last time for time 1 " + firstOne,
                             lastOne, elem.getLastTime());
            } else if (!sawTwo &&
                       elem.getFirstTime() == firstTwo)
            {
                sawTwo = true;
                assertEquals("Bad last time for time 2 " + firstTwo,
                             lastTwo, elem.getLastTime());
            } else {
                fail("Bad first time " + elem.getFirstTime());
            }
        }

        assertTrue("Didn't see first request", sawOne);
        assertTrue("Didn't see second request", sawTwo);
    }

    @Test
    public void testMergeGT()
    {
        final long inc = 500L;
        final long firstOne = 10000L;
        final long lastOne = firstOne + inc;
        final long firstTwo = 20000L;
        final long lastTwo = firstTwo + inc;

        final int uid = 1;
        final int type = 2;
        final int cfgId = 3;

        MockTriggerRequest req = new MockTriggerRequest(uid, type, cfgId,
                                                        firstOne, lastTwo);

        MockReadoutRequest srcReq = new MockReadoutRequest();
        srcReq.addElement(GLOBAL, NO_STRING, firstOne, firstTwo, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, firstTwo, lastTwo, NO_DOM);

        req.setReadoutRequest(srcReq);

        MockReadoutRequest tgtReq = new MockReadoutRequest();

        ArrayList<ITriggerRequestPayload> list =
            new ArrayList<ITriggerRequestPayload>();
        list.add(req);

        ElementMerger.merge(tgtReq, list);

        List<IReadoutRequestElement> elems =
            tgtReq.getReadoutRequestElements();
        assertNotNull("Element list should not be null", elems);
        assertEquals("Bad number of elements", 2, elems.size());

        boolean sawOne = false;
        boolean sawTwo = false;

        for (IReadoutRequestElement elem : elems) {
            long expFirst;
            long expLast;
            if (elem.getReadoutType() == II_GLOBAL) {
                expFirst = firstOne;
                expLast = firstTwo;
                sawOne = true;
            } else if (elem.getReadoutType() == IT_GLOBAL) {
                expFirst = firstOne;
                expLast = lastTwo;
                sawTwo = true;
            } else {
                fail("Unknown readout type #" + elem.getReadoutType() +
                     "in " + elem);
                break;
            }

            assertSourceId(NO_STRING, elem.getSourceID());
            assertDOMId(NO_DOM, elem.getDomID());

            assertEquals("Bad first time for " + elem,
                         expFirst, elem.getFirstTime());
            assertEquals("Bad last time for " + elem,
                         expLast, elem.getLastTime());
        }

        assertTrue("Didn't see first request", sawOne);
        assertTrue("Didn't see second request", sawTwo);
    }

    @Test
    public void testMergeWithNonoverlap()
    {
        final long firstOne = 250000;
        final long lastOne = firstOne + 100000;
        final long firstTwo = lastOne + 15000;
        final long lastTwo = firstTwo + 100000;

        final int uid = 1;
        final int type = 2;
        final int cfgId = 3;

        MockTriggerRequest req = new MockTriggerRequest(uid, type, cfgId,
                                                        firstOne, lastTwo);

        MockReadoutRequest srcReq = new MockReadoutRequest();
        srcReq.addElement(II_GLOBAL, NO_STRING, firstOne + 1000, lastOne,
                          NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, firstOne, lastOne - 5555,
                          NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, firstOne, lastOne - 8765,
                          NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, firstTwo, lastTwo, NO_DOM);

        req.setReadoutRequest(srcReq);

        MockReadoutRequest tgtReq = new MockReadoutRequest();

        ArrayList<ITriggerRequestPayload> list =
            new ArrayList<ITriggerRequestPayload>();
        list.add(req);

        ElementMerger.merge(tgtReq, list);

        List<IReadoutRequestElement> elems =
            tgtReq.getReadoutRequestElements();
        assertNotNull("Element list should not be null", elems);
        assertEquals("Bad number of elements", 2, elems.size());

        boolean sawOne = false;
        boolean sawTwo = false;

        for (IReadoutRequestElement elem : elems) {
            assertEquals("Bad type", II_GLOBAL, elem.getReadoutType());
            assertSourceId(NO_STRING, elem.getSourceID());
            assertDOMId(NO_DOM, elem.getDomID());

            if (!sawOne && elem.getFirstTime() == firstOne) {
                sawOne = true;
                assertEquals("Bad last time for time 1 " + firstOne,
                             lastOne, elem.getLastTime());
            } else if (!sawTwo &&
                       elem.getFirstTime() == firstTwo)
            {
                sawTwo = true;
                assertEquals("Bad last time for time 2 " + firstTwo,
                             lastTwo, elem.getLastTime());
            } else {
                fail("Bad first time " + elem.getFirstTime());
            }
        }

        assertTrue("Didn't see first request", sawOne);
        assertTrue("Didn't see second request", sawTwo);
    }

    @Test
    public void testMergeMultiSource()
    {
        final long firstZero = 200000;
        final long lastZero = firstZero + 15000;
        final long firstOne = lastZero + 15000;
        final long lastOne = firstOne + 100000;
        final long firstTwo = lastOne + 15000;
        final long lastTwo = firstTwo + 100000;
        final long firstThree = lastTwo + 15000;
        final long lastThree = lastTwo + 100000;

        final int uid = 1;
        final int type = 2;
        final int cfgId = 3;

        MockTriggerRequest req = new MockTriggerRequest(uid, type, cfgId,
                                                        firstOne, lastTwo);

        MockReadoutRequest srcReq = new MockReadoutRequest();
        srcReq.addElement(GLOBAL, NO_STRING, firstZero, lastZero, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, firstOne, lastOne, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, firstOne, lastOne - 5555,
                          NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, firstOne, firstTwo + 1234,
                          NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, firstTwo, lastTwo, NO_DOM);
        srcReq.addElement(GLOBAL, NO_STRING, firstThree, lastThree, NO_DOM);

        req.setReadoutRequest(srcReq);

        MockReadoutRequest tgtReq = new MockReadoutRequest();

        ArrayList<ITriggerRequestPayload> list =
            new ArrayList<ITriggerRequestPayload>();
        list.add(req);

        ElementMerger.merge(tgtReq, list);

        List<IReadoutRequestElement> elems =
            tgtReq.getReadoutRequestElements();
        assertNotNull("Element list should not be null", elems);
        assertEquals("Bad number of elements", 6, elems.size());

        int numII = 0;
        int numIT = 0;

        for (IReadoutRequestElement elem : elems) {
            assertSourceId(NO_STRING, elem.getSourceID());
            assertDOMId(NO_DOM, elem.getDomID());

            if (elem.getReadoutType() == II_GLOBAL) {
                numII++;

                if (elem.getFirstTime() == firstZero) {
                    assertEquals("Bad last II time",
                                 lastZero, elem.getLastTime());
                } else if (elem.getFirstTime() == firstOne) {
                    assertEquals("Bad last II time",
                                 lastOne, elem.getLastTime());
                } else if (elem.getFirstTime() == firstThree) {
                    assertEquals("Bad last II time",
                                 lastThree, elem.getLastTime());
                } else {
                    assertEquals("Bad first II time",
                                 firstOne, elem.getFirstTime());
                }
            } else if (elem.getReadoutType() == IT_GLOBAL) {
                numIT++;

                if (elem.getFirstTime() == firstZero) {
                    assertEquals("Bad last IT time",
                                 lastZero, elem.getLastTime());
                } else if (elem.getFirstTime() == firstOne) {
                    assertEquals("Bad last IT time",
                                 lastTwo, elem.getLastTime());
                } else if (elem.getFirstTime() == firstThree) {
                    assertEquals("Bad last IT time",
                                 lastThree, elem.getLastTime());
                } else {
                    assertEquals("Bad first IT time",
                                 firstOne, elem.getFirstTime());
                }
            } else {
                fail("Unknown type for " + elem);
            }
        }

        assertEquals("Bad number of II_GLOBAL elements", 3, numII);
        assertEquals("Bad number of IT_GLOBAL elements", 3, numIT);
    }

    @Test
    public void testMergeSpecific1()
    {
        final int uid = 1;
        final int type = 2;
        final int cfgId = 3;

        final long firstIIOne = 116198449402970174L;
        final long lastIIOne = 116198449403103228L;

        final long firstIITwo = 116198449403109929L;
        final long lastIITwo = 116198449403365004L;

        final long firstITOne = 116198449402910174L;
        final long lastITOne = 116198449403391861L;

        MockTriggerRequest req =
            new MockTriggerRequest(uid, type, cfgId, firstIIOne, lastITOne);

        MockReadoutRequest srcReq = new MockReadoutRequest();

        srcReq.addElement(GLOBAL, NO_STRING, firstIITwo,
                          116198449403310410L, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, firstIIOne,
                          116198449403076695L, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, firstIIOne,
                          116198449403078654L, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, firstIIOne,
                          lastIIOne, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, 116198449402985271L,
                          116198449403094811L, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, 116198449402985271L,
                          116198449403098003L, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, 116198449403221057L,
                          116198449403330649L, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, 116198449403221057L,
                          116198449403335005L, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, 116198449403221057L,
                          lastIITwo, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, 116198449403231119L,
                          116198449403340203L, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, 116198449403236945L,
                          116198449403342492L, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, 116198449403241580L,
                          116198449403351333L, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, 116198449403251861L,
                          116198449403360749L, NO_DOM);

        srcReq.addElement(IT_GLOBAL, NO_STRING, firstITOne,
                          116198449403110174L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, firstITOne,
                          116198449403110174L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, firstITOne,
                          116198449403110174L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, 116198449402925271L,
                          116198449403125271L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, 116198449402925271L,
                          116198449403125271L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, 116198449403161057L,
                          116198449403361057L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, 116198449403161057L,
                          116198449403361057L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, 116198449403161057L,
                          116198449403361057L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, 116198449403171119L,
                          116198449403371119L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, 116198449403176945L,
                          116198449403376945L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, 116198449403181580L,
                          116198449403381580L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, 116198449403191861L,
                          lastITOne, NO_DOM);
        srcReq.addElement(II_GLOBAL, NO_STRING, 116198449403228099L,
                          116198449403362691L, NO_DOM);
        srcReq.addElement(IT_GLOBAL, NO_STRING, 116198449403188099L,
                          116198449403388099L, NO_DOM);

        req.setReadoutRequest(srcReq);

        MockReadoutRequest tgtReq = new MockReadoutRequest();

        ArrayList<ITriggerRequestPayload> list =
            new ArrayList<ITriggerRequestPayload>();
        list.add(req);

        ElementMerger.merge(tgtReq, list);

        List<IReadoutRequestElement> elems =
            tgtReq.getReadoutRequestElements();
        assertNotNull("Element list should not be null", elems);
        assertEquals("Bad number of elements", 3, elems.size());

        int numII = 0;
        int numIT = 0;

        for (IReadoutRequestElement elem : elems) {
            assertSourceId(NO_STRING, elem.getSourceID());
            assertDOMId(NO_DOM, elem.getDomID());

            if (elem.getReadoutType() == II_GLOBAL) {
                numII++;

                if (elem.getFirstTime() == firstIIOne) {
                    assertEquals("Bad last II time",
                                 lastIIOne, elem.getLastTime());
                } else if (elem.getFirstTime() == firstIITwo) {
                    assertEquals("Bad last II time",
                                 lastIITwo, elem.getLastTime());
                } else {
                    fail("Bad II first time " + elem.getFirstTime() +
                         " (last time " + elem.getLastTime() + ")");
                }
            } else if (elem.getReadoutType() == IT_GLOBAL) {
                numIT++;

                if (elem.getFirstTime() == firstITOne) {
                    assertEquals("Bad last IT time",
                                 lastITOne, elem.getLastTime());
                } else {
                    fail("Bad IT first time " + elem.getFirstTime() +
                         " (last time " + elem.getLastTime() + ")");
                }
            } else {
                fail("Unknown type for " + elem);
            }
        }

        assertEquals("Bad number of II_GLOBAL elements", 2, numII);
        assertEquals("Bad number of IT_GLOBAL elements", 1, numIT);
    }
}
