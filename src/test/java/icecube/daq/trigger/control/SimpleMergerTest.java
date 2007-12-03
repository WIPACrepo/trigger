package icecube.daq.trigger.control;

import icecube.daq.trigger.IReadoutRequestElement;

import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockReadoutRequestElement;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class SimpleMergerTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public SimpleMergerTest(String name)
    {
        super(name);
    }

    private static IReadoutRequestElement newElem(int type, long firstTime,
                                                  long lastTime, long domId,
                                                  int srcId)
    {
        return new MockReadoutRequestElement(type, firstTime, lastTime,
                                             domId, srcId);
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        appender.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);
    }

    public static Test suite()
    {
        return new TestSuite(SimpleMergerTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testEmpty()
    {
        SimpleMerger merger = new SimpleMerger();

        List mergedList = merger.merge(new ArrayList());
        assertEquals("Bad merged list size", 0, mergedList.size());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Input list size should be greater than zero...!!!!",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testBadType()
    {
        SimpleMerger merger = new SimpleMerger();

        ArrayList list = new ArrayList();
        list.add(newElem(999, 10000L, 11999L, 123456789L, 123));
        list.add(newElem(999, 20000L, 21999L, 234567890L, 234));

        final int listSize = list.size();

        List mergedList = merger.merge(list);
        assertEquals("Bad merged list size", listSize, mergedList.size());

        if (appender.getNumberOfMessages() == 0) {
            fail("No errors were logged");
        } else {
            for (int i = 0; i < appender.getNumberOfMessages(); i++) {
                assertEquals("Bad log message #" + i,
                             "wrong Readout type",
                             appender.getMessage(i));
            }
            appender.clear();
        }
    }

    public void testOneElement()
    {
        SimpleMerger merger = new SimpleMerger();

        ArrayList list = new ArrayList();
        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_GLOBAL,
                         10000L, 19999L, 123456789L, 123));

        List mergedList = merger.merge(list);
        assertEquals("Bad merged list size", 1, mergedList.size());
    }

    public void testNoGapIIGlobal()
    {
        SimpleMerger merger = new SimpleMerger();

        ArrayList list = new ArrayList();

        final long aFirstTime = 10000L;
        final long aLastTime = 11999L;
        final long aDomId = 123456789L;
        final int aSrcId = 123;

        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                         aFirstTime, aLastTime, aDomId, aSrcId));

        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                         20000L, 21999L, 234567890L, 456));

        final long cFirstTime = 30000L;
        final long cLastTime = 31999L;
        final long cDomId = 345678901L;

        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                         cFirstTime, cLastTime, cDomId, 789));

        List mergedList = merger.merge(list);
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);
        assertEquals("Bad first time", aFirstTime,
                     elem.getFirstTimeUTC().getUTCTimeAsLong());
        assertEquals("Bad last time", cLastTime,
                     elem.getLastTimeUTC().getUTCTimeAsLong());
        assertEquals("Bad DOM ID", aDomId,
                     elem.getDomID().getDomIDAsLong());
        assertEquals("Bad Source ID", aSrcId,
                     elem.getSourceID().getSourceID());
    }

    public void testGapIIStringNoMerge()
    {
        SimpleMerger merger = new SimpleMerger();
        merger.setAllowTimeGap(true);

        final int srcId = 123;

        IReadoutRequestElement elemA =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                    10000L, 11999L, 123456789L, srcId);

        IReadoutRequestElement elemB =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                    20000L, 21999L, 234567890L, srcId);

        IReadoutRequestElement elemC =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                    30000L, 31999L, 345678901L, srcId);

        IReadoutRequestElement[] array =
            new IReadoutRequestElement[] { elemA, elemB, elemC };

        ArrayList list = new ArrayList();
        for (int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }

        List mergedList = merger.merge(list);
        assertEquals("Bad merged list size", array.length, mergedList.size());

        for (int i = 0; i < array.length; i++) {
            IReadoutRequestElement elem =
                (IReadoutRequestElement) mergedList.get(i);
            assertEquals("Bad first time",
                         array[i].getFirstTimeUTC(), elem.getFirstTimeUTC());
            assertEquals("Bad last time",
                         array[i].getLastTimeUTC(), elem.getLastTimeUTC());
            assertEquals("Bad DOM ID", array[i].getDomID(), elem.getDomID());
            assertEquals("Bad Source ID",
                         array[i].getSourceID(), elem.getSourceID());
        }
    }

    public void testGapIIStringMerge()
    {
        SimpleMerger merger = new SimpleMerger();
        merger.setAllowTimeGap(true);

        final int srcId = 123;

        IReadoutRequestElement elemA =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                    10000L, 14999L, 123456789L, srcId);

        IReadoutRequestElement elemB =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                    13000L, 16999L, 234567890L, srcId);

        IReadoutRequestElement elemC =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                    15000L, 19999L, 345678901L, srcId);

        IReadoutRequestElement[] array =
            new IReadoutRequestElement[] { elemA, elemB, elemC };

        ArrayList list = new ArrayList();
        for (int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }

        List mergedList = merger.merge(list);
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);
        assertEquals("Bad first time",
                     elemA.getFirstTimeUTC(), elem.getFirstTimeUTC());
        assertEquals("Bad last time",
                     elemC.getLastTimeUTC(), elem.getLastTimeUTC());
        assertEquals("Bad DOM ID", elemA.getDomID(), elem.getDomID());
        assertEquals("Bad Source ID",
                     elemA.getSourceID(), elem.getSourceID());
    }

    public void testNoGapIIString()
    {
        SimpleMerger merger = new SimpleMerger();

        ArrayList list = new ArrayList();

        final long aFirstTime = 10000L;
        final long aLastTime = 11999L;
        final long aDomId = 123456789L;
        final int aSrcId = 123;

        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                         aFirstTime, aLastTime, aDomId, aSrcId));

        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                         20000L, 21999L, 234567890L, aSrcId));

        final long cFirstTime = 30000L;
        final long cLastTime = 31999L;
        final long cDomId = 345678901L;

        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                         cFirstTime, cLastTime, cDomId, aSrcId));

        List mergedList = merger.merge(list);
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);
        assertEquals("Bad first time", aFirstTime,
                     elem.getFirstTimeUTC().getUTCTimeAsLong());
        assertEquals("Bad last time", cLastTime,
                     elem.getLastTimeUTC().getUTCTimeAsLong());
        assertEquals("Bad DOM ID", aDomId,
                     elem.getDomID().getDomIDAsLong());
        assertEquals("Bad Source ID", aSrcId,
                     elem.getSourceID().getSourceID());
    }

    public void testNoGapIIModule()
    {
        SimpleMerger merger = new SimpleMerger();

        ArrayList list = new ArrayList();

        final long aFirstTime = 10000L;
        final long aLastTime = 11999L;
        final long aDomId = 123456789L;
        final int aSrcId = 123;

        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                         aFirstTime, aLastTime, aDomId, aSrcId));

        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                         20000L, 21999L, aDomId, aSrcId));

        final long cFirstTime = 30000L;
        final long cLastTime = 31999L;

        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                         cFirstTime, cLastTime, aDomId, aSrcId));

        List mergedList = merger.merge(list);
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);
        assertEquals("Bad first time", aFirstTime,
                     elem.getFirstTimeUTC().getUTCTimeAsLong());
        assertEquals("Bad last time", cLastTime,
                     elem.getLastTimeUTC().getUTCTimeAsLong());
        assertEquals("Bad DOM ID", aDomId,
                     elem.getDomID().getDomIDAsLong());
        assertEquals("Bad Source ID", aSrcId,
                     elem.getSourceID().getSourceID());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
