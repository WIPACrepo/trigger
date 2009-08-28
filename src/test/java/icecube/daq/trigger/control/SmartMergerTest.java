package icecube.daq.trigger.control;

import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockReadoutRequestElement;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class SmartMergerTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public SmartMergerTest(String name)
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
        return new TestSuite(SmartMergerTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testSimpleGlobal()
    {
        SmartMerger merger = new SmartMerger();

        final int gIISrcId = 123;

        IReadoutRequestElement elemA =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                    10000L, 11999L, 123456789L, gIISrcId);

        IReadoutRequestElement elemB =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                    20000L, 21999L, 234567890L, gIISrcId);

        IReadoutRequestElement elemC =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                    30000L, 31999L, 345678901L, gIISrcId);

        IReadoutRequestElement[] array =
            new IReadoutRequestElement[] { elemA, elemB, elemC };

        ArrayList list = new ArrayList();
        for (int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }

        ArrayList listList = new ArrayList();
        listList.add(list);

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
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

    public void testBadType()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList list = new ArrayList();
        list.add(newElem(999, 10000L, 11999L, 123456789L, 123));

        ArrayList listList = new ArrayList();
        listList.add(list);

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", 0, mergedList.size());

        assertEquals("Bad number of messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Unknown ReadoutType!!!", appender.getMessage(0));
        appender.clear();
    }

    public void testAllTypesDiscrete()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement[] array = new IReadoutRequestElement[5];
        for (int i = 0; i < array.length; i++) {

            IReadoutRequestElement elem;
            switch (i) {
            case 0:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 1:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                               20000L, 21999L, 234567890L, 234);
                break;
            case 2:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                               30000L, 31999L, 345678901L, 345);
                break;
            case 3:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL,
                               40000L, 41999L, 456789012L, 456);
                break;
            case 4:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_IT_MODULE,
                               50000L, 51999L, 567890123L, 567);
                break;
            default:
                throw new Error("Unknown index " + i);
            }

            array[i] = elem;

            ArrayList list = new ArrayList();
            list.add(elem);

            listList.add(list);
        }

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
//System.err.println("Got "+mergedList.size()+" elems, "+appender.getNumberOfMessages()+" log msgs, "+mergedList);
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

    public void testAllTypesMergeable()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement[] array = new IReadoutRequestElement[5];
        for (int i = 0; i < array.length; i++) {

            IReadoutRequestElement elem;
            switch (i) {
            case 0:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 1:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 2:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 3:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 4:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_IT_MODULE,
                               10000L, 11999L, 123456789L, 123);
                break;
            default:
                throw new Error("Unknown index " + i);
            }

            array[i] = elem;

            ArrayList list = new ArrayList();
            list.add(elem);

            listList.add(list);
        }

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", 2, mergedList.size());

        for (Object obj : mergedList) {
            IReadoutRequestElement elem = (IReadoutRequestElement) obj;

            for (int i = 0; i < array.length; i++) {
                if (elem.getReadoutType() == array[i].getReadoutType()) {
                    assertEquals("Bad first time", array[i].getFirstTimeUTC(),
                                 elem.getFirstTimeUTC());
                    assertEquals("Bad last time", array[i].getLastTimeUTC(),
                                 elem.getLastTimeUTC());
                    assertEquals("Bad DOM ID",
                                 array[i].getDomID(), elem.getDomID());
                    assertEquals("Bad Source ID",
                                 array[i].getSourceID(), elem.getSourceID());
                }
            }
        }
    }

    public void testInIcePartialMerge()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement[] array = new IReadoutRequestElement[2];
        for (int i = 0; i < 3; i++) {

            IReadoutRequestElement elem;
            switch (i) {
            case 0:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                               10000L, 11999L, 123456789L, 123);
                array[0] = elem;
                break;
            case 1:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 2:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                               20000L, 21999L, 234567890L, 456);
                array[1] = elem;
                break;
            default:
                throw new Error("Unknown index " + i);
            }

            ArrayList list = new ArrayList();
            list.add(elem);

            listList.add(list);
        }

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", array.length, mergedList.size());

        for (Object obj : mergedList) {
            IReadoutRequestElement elem = (IReadoutRequestElement) obj;

            for (int i = 0; i < array.length; i++) {
                if (elem.getReadoutType() == array[i].getReadoutType()) {
                    assertEquals("Bad first time", array[i].getFirstTimeUTC(),
                                 elem.getFirstTimeUTC());
                    assertEquals("Bad last time", array[i].getLastTimeUTC(),
                                 elem.getLastTimeUTC());
                    assertEquals("Bad DOM ID",
                                 array[i].getDomID(), elem.getDomID());
                    assertEquals("Bad Source ID",
                                 array[i].getSourceID(), elem.getSourceID());
                }
            }
        }
    }

    public void testIceTopGlobalOnly()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement expElem =
            newElem(IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL,
                    10000L, 11999L, 123456789L, 123);

        ArrayList list = new ArrayList();
        list.add(expElem);

        listList.add(list);

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);

        assertEquals("Bad first time", expElem.getFirstTimeUTC(),
                     elem.getFirstTimeUTC());
        assertEquals("Bad last time", expElem.getLastTimeUTC(),
                     elem.getLastTimeUTC());
        assertEquals("Bad DOM ID",
                     expElem.getDomID(), elem.getDomID());
        assertEquals("Bad Source ID",
                     expElem.getSourceID(), elem.getSourceID());
    }

    public void testIceTopModuleOnly()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement expElem =
            newElem(IReadoutRequestElement.READOUT_TYPE_IT_MODULE,
                    10000L, 11999L, 123456789L, 123);

        ArrayList list = new ArrayList();
        list.add(expElem);

        listList.add(list);

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);

        assertEquals("Bad first time", expElem.getFirstTimeUTC(),
                     elem.getFirstTimeUTC());
        assertEquals("Bad last time", expElem.getLastTimeUTC(),
                     elem.getLastTimeUTC());
        assertEquals("Bad DOM ID",
                     expElem.getDomID(), elem.getDomID());
        assertEquals("Bad Source ID",
                     expElem.getSourceID(), elem.getSourceID());
    }

    public void testInIceNoModuleMerge()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement[] array = new IReadoutRequestElement[2];
        for (int i = 0; i < array.length; i++) {

            IReadoutRequestElement elem;
            switch (i) {
            case 0:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 1:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                               10000L, 11999L, 123456789L, 123);
                break;
            default:
                throw new Error("Bad index " + i);
            }

            array[i] = elem;

            ArrayList list = new ArrayList();
            list.add(elem);

            listList.add(list);
        }

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);

        assertEquals("Bad first time", array[0].getFirstTimeUTC(),
                     elem.getFirstTimeUTC());
        assertEquals("Bad last time", array[0].getLastTimeUTC(),
                     elem.getLastTimeUTC());
        assertEquals("Bad DOM ID",
                     array[0].getDomID(), elem.getDomID());
        assertEquals("Bad Source ID",
                     array[0].getSourceID(), elem.getSourceID());
    }

    public void testInIceNoModuleNoMerge()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement[] array = new IReadoutRequestElement[2];
        for (int i = 0; i < array.length; i++) {

            IReadoutRequestElement elem;
            switch (i) {
            case 0:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 1:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                               20000L, 21999L, 234567890L, 456);
                break;
            default:
                throw new Error("Bad index " + i);
            }

            array[i] = elem;

            ArrayList list = new ArrayList();
            list.add(elem);

            listList.add(list);
        }


        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", array.length, mergedList.size());

        for (Object obj : mergedList) {
            IReadoutRequestElement elem = (IReadoutRequestElement) obj;

            for (int i = 0; i < array.length; i++) {
                if (elem.getReadoutType() == array[i].getReadoutType()) {
                    assertEquals("Bad first time", array[i].getFirstTimeUTC(),
                                 elem.getFirstTimeUTC());
                    assertEquals("Bad last time", array[i].getLastTimeUTC(),
                                 elem.getLastTimeUTC());
                    assertEquals("Bad DOM ID",
                                 array[i].getDomID(), elem.getDomID());
                    assertEquals("Bad Source ID",
                                 array[i].getSourceID(), elem.getSourceID());
                }
            }
        }
    }

    public void testInIceNoStringMerge()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement[] array = new IReadoutRequestElement[2];
        for (int i = 0; i < array.length; i++) {

            IReadoutRequestElement elem;
            switch (i) {
            case 0:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 1:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                               10000L, 11999L, 123456789L, 123);
                break;
            default:
                throw new Error("Bad index " + i);
            }

            array[i] = elem;

            ArrayList list = new ArrayList();
            list.add(elem);

            listList.add(list);
        }

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);

        assertEquals("Bad first time", array[0].getFirstTimeUTC(),
                     elem.getFirstTimeUTC());
        assertEquals("Bad last time", array[0].getLastTimeUTC(),
                     elem.getLastTimeUTC());
        assertEquals("Bad DOM ID",
                     array[0].getDomID(), elem.getDomID());
        assertEquals("Bad Source ID",
                     array[0].getSourceID(), elem.getSourceID());
    }

    public void testInIceNoStringNoMerge()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement[] array = new IReadoutRequestElement[2];
        for (int i = 0; i < array.length; i++) {

            IReadoutRequestElement elem;
            switch (i) {
            case 0:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 1:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                               20000L, 21999L, 234567890L, 456);
                break;
            default:
                throw new Error("Bad index " + i);
            }

            array[i] = elem;

            ArrayList list = new ArrayList();
            list.add(elem);

            listList.add(list);
        }

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", array.length, mergedList.size());

        for (Object obj : mergedList) {
            IReadoutRequestElement elem = (IReadoutRequestElement) obj;

            for (int i = 0; i < array.length; i++) {
                if (elem.getReadoutType() == array[i].getReadoutType()) {
                    assertEquals("Bad first time", array[i].getFirstTimeUTC(),
                                 elem.getFirstTimeUTC());
                    assertEquals("Bad last time", array[i].getLastTimeUTC(),
                                 elem.getLastTimeUTC());
                    assertEquals("Bad DOM ID",
                                 array[i].getDomID(), elem.getDomID());
                    assertEquals("Bad Source ID",
                                 array[i].getSourceID(), elem.getSourceID());
                }
            }
        }
    }

    public void testInIceNoGlobalMerge()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement[] array = new IReadoutRequestElement[2];
        for (int i = 0; i < array.length; i++) {

            IReadoutRequestElement elem;
            switch (i) {
            case 0:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 1:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                               10000L, 11999L, 123456789L, 123);
                break;
            default:
                throw new Error("Bad index " + i);
            }

            array[i] = elem;

            ArrayList list = new ArrayList();
            list.add(elem);

            listList.add(list);
        }

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);

        assertEquals("Bad first time", array[0].getFirstTimeUTC(),
                     elem.getFirstTimeUTC());
        assertEquals("Bad last time", array[0].getLastTimeUTC(),
                     elem.getLastTimeUTC());
        assertEquals("Bad DOM ID",
                     array[0].getDomID(), elem.getDomID());
        assertEquals("Bad Source ID",
                     array[0].getSourceID(), elem.getSourceID());
    }

    public void testInIceNoGlobalNoMerge()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement[] array = new IReadoutRequestElement[2];
        for (int i = 0; i < array.length; i++) {

            IReadoutRequestElement elem;
            switch (i) {
            case 0:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                               10000L, 11999L, 123456789L, 123);
                break;
            case 1:
                elem = newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                               20000L, 21999L, 234567890L, 456);
                break;
            default:
                throw new Error("Bad index " + i);
            }

            array[i] = elem;

            ArrayList list = new ArrayList();
            list.add(elem);

            listList.add(list);
        }

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", array.length, mergedList.size());

        for (Object obj : mergedList) {
            IReadoutRequestElement elem = (IReadoutRequestElement) obj;

            for (int i = 0; i < array.length; i++) {
                if (elem.getReadoutType() == array[i].getReadoutType()) {
                    assertEquals("Bad first time", array[i].getFirstTimeUTC(),
                                 elem.getFirstTimeUTC());
                    assertEquals("Bad last time", array[i].getLastTimeUTC(),
                                 elem.getLastTimeUTC());
                    assertEquals("Bad DOM ID",
                                 array[i].getDomID(), elem.getDomID());
                    assertEquals("Bad Source ID",
                                 array[i].getSourceID(), elem.getSourceID());
                }
            }
        }
    }

    public void testInIceGlobalOnly()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement expElem =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                    10000L, 11999L, 123456789L, 123);

        ArrayList list = new ArrayList();
        list.add(expElem);

        listList.add(list);

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);

        assertEquals("Bad first time", expElem.getFirstTimeUTC(),
                     elem.getFirstTimeUTC());
        assertEquals("Bad last time", expElem.getLastTimeUTC(),
                     elem.getLastTimeUTC());
        assertEquals("Bad DOM ID",
                     expElem.getDomID(), elem.getDomID());
        assertEquals("Bad Source ID",
                     expElem.getSourceID(), elem.getSourceID());
    }

    public void testInIceStringOnly()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement expElem =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                    10000L, 11999L, 123456789L, 123);

        ArrayList list = new ArrayList();
        list.add(expElem);

        listList.add(list);

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);

        assertEquals("Bad first time", expElem.getFirstTimeUTC(),
                     elem.getFirstTimeUTC());
        assertEquals("Bad last time", expElem.getLastTimeUTC(),
                     elem.getLastTimeUTC());
        assertEquals("Bad DOM ID",
                     expElem.getDomID(), elem.getDomID());
        assertEquals("Bad Source ID",
                     expElem.getSourceID(), elem.getSourceID());
    }

    public void testInIceModuleOnly()
    {
        SmartMerger merger = new SmartMerger();

        ArrayList listList = new ArrayList();

        IReadoutRequestElement expElem =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                    10000L, 11999L, 123456789L, 123);

        ArrayList list = new ArrayList();
        list.add(expElem);

        listList.add(list);

        merger.merge(listList);
        List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
        assertEquals("Bad merged list size", 1, mergedList.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) mergedList.get(0);

        assertEquals("Bad first time", expElem.getFirstTimeUTC(),
                     elem.getFirstTimeUTC());
        assertEquals("Bad last time", expElem.getLastTimeUTC(),
                     elem.getLastTimeUTC());
        assertEquals("Bad DOM ID",
                     expElem.getDomID(), elem.getDomID());
        assertEquals("Bad Source ID",
                     expElem.getSourceID(), elem.getSourceID());
    }

    public void testMergeSmaller()
    {
        SmartMerger merger = new SmartMerger();

        int srcId = SourceIdRegistry.INICE_TRIGGER_SOURCE_ID;

        final long lgFirstTime = 10000L;
        final long lgLastTime = 19999L;
        final long lgDomId = 123456789L;

        final long smDomId = 987654321L;

        IReadoutRequestElement large =
            newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                    lgFirstTime, lgLastTime, lgDomId, srcId);

        for (int i = 0; i < 10; i++) {
            long firstTime;
            long lastTime;

            switch (i) {
            case 0:
                firstTime = lgFirstTime - 100L;
                lastTime = lgLastTime;
                break;
            case 1:
                firstTime = lgFirstTime + 100L;
                lastTime = lgLastTime;
                break;
            case 2:
                firstTime = lgFirstTime;
                lastTime = lgLastTime - 100L;
                break;
            case 3:
                firstTime = lgFirstTime;
                lastTime = lgLastTime + 100L;
                break;
            case 4:
                firstTime = lgFirstTime - 100L;
                lastTime = lgLastTime - 100L;
                break;
            case 5:
                firstTime = lgFirstTime + 100L;
                lastTime = lgLastTime - 100L;
                break;
            case 6:
                firstTime = lgFirstTime + 100L;
                lastTime = lgLastTime - 100L;
                break;
            case 7:
                firstTime = lgFirstTime - 100L;
                lastTime = lgLastTime + 100L;
                break;
            case 8:
                firstTime = lgFirstTime - 1000L;
                lastTime = lgFirstTime - 100L;
                break;
            case 9:
                firstTime = lgLastTime + 100L;
                lastTime = lgLastTime + 1000L;
                break;
            default:
                throw new Error("Bad index " + i);
            }

            IReadoutRequestElement small =
                newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                        firstTime, lastTime, smDomId, srcId);

            ArrayList listList = new ArrayList();

            ArrayList list;

            list = new ArrayList();
            list.add(large);
            listList.add(list);

            list = new ArrayList();
            list.add(small);
            listList.add(list);

            final int stringType =
                IReadoutRequestElement.READOUT_TYPE_II_STRING;

            ArrayList expList = new ArrayList();
            if (firstTime < large.getFirstTimeUTC().longValue()) {
                long lt;
                if (lgFirstTime - 1L < lastTime) {
                    lt = lgFirstTime - 1L;
                } else {
                    lt = lastTime;
                }

                expList.add(newElem(stringType, firstTime, lt,
                                    smDomId, srcId));

            }
            expList.add(large);
            if (lastTime > large.getLastTimeUTC().longValue()) {
                long ft;
                if (lgLastTime + 1L > firstTime) {
                    ft = lgLastTime + 1L;
                } else {
                    ft = firstTime;
                }

                expList.add(newElem(stringType, ft, lastTime,
                                    smDomId, srcId));
            }

            merger.merge(listList);
            List mergedList = merger.getFinalReadoutElementsTimeOrdered_All();
            //System.err.println("Got "+mergedList.size()+" elems, "+appender.getNumberOfMessages()+" log msgs");
            assertEquals("Bad merged list#" + i + " size",
                         expList.size(), mergedList.size());

            for (int j = 0; j < expList.size(); j++) {
                IReadoutRequestElement expElem =
                    (IReadoutRequestElement) expList.get(j);
                IReadoutRequestElement actElem =
                    (IReadoutRequestElement) mergedList.get(j);

                assertEquals("Bad readout type #" + i, expElem.getReadoutType(),
                             actElem.getReadoutType());
                assertEquals("Bad first time #" + i, expElem.getFirstTimeUTC(),
                             actElem.getFirstTimeUTC());
                assertEquals("Bad last time #" + i, expElem.getLastTimeUTC(),
                             actElem.getLastTimeUTC());
                assertEquals("Bad DOM ID #" + i,
                             expElem.getDomID(), actElem.getDomID());
                assertEquals("Bad Source ID #" + i,
                             expElem.getSourceID(), actElem.getSourceID());
            }
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
