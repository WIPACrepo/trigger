package icecube.daq.trigger.control;

import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockDOMID;
import icecube.daq.trigger.test.MockReadoutRequestElement;
import icecube.daq.trigger.test.MockSourceID;
import icecube.daq.trigger.test.MockUTCTime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class GlobalTrigEventReadoutElementsTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public GlobalTrigEventReadoutElementsTest(String name)
    {
        super(name);
    }

    private static Object newElem(int type, long firstTime, long lastTime,
                                  long domId, int srcId)
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
        return new TestSuite(GlobalTrigEventReadoutElementsTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testCreateAndFill()
    {
        GlobalTrigEventReadoutElements elems =
            new GlobalTrigEventReadoutElements();

        final int baseSrcId =
            SourceIdRegistry.STRING_HUB_SOURCE_ID;

        final long timeRange = 9999L;

        final long globalFirstTime = timeRange + 1;
        final long globalLastTime = globalFirstTime + timeRange;
        final long globalDomId = 123456789L;
        final int globalSrcId = baseSrcId + 1;

        final long iiGlobalFirstTime = globalLastTime + 1;
        final long iiGlobalLastTime = iiGlobalFirstTime + timeRange;
        final long iiGlobalDomId = 234567890L;
        final int iiGlobalSrcId = baseSrcId + 2;

        final long itGlobalFirstTime = iiGlobalLastTime + 1;
        final long itGlobalLastTime = itGlobalFirstTime + timeRange;
        final long itGlobalDomId = 345678901L;
        final int itGlobalSrcId = baseSrcId + 3;

        final long iiStringFirstTime = itGlobalLastTime + 1;
        final long iiStringLastTime = iiStringFirstTime + timeRange;
        final long iiStringDomId = 456789012L;
        final int iiStringSrcId = baseSrcId + 4;

        final long iiModuleFirstTime = iiStringLastTime + 1;
        final long iiModuleLastTime = iiModuleFirstTime + timeRange;
        final long iiModuleDomId = 567890123L;
        final int iiModuleSrcId = baseSrcId + 5;

        final long itModuleFirstTime = iiModuleLastTime + 1;
        final long itModuleLastTime = itModuleFirstTime + timeRange;
        final long itModuleDomId = 678901234L;
        final int itModuleSrcId = baseSrcId + 6;

        ArrayList list = new ArrayList();
        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_GLOBAL,
                         globalFirstTime, globalLastTime, globalDomId,
                         globalSrcId));
        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                         iiGlobalFirstTime, iiGlobalLastTime, iiGlobalDomId,
                         iiGlobalSrcId));
        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL,
                         itGlobalFirstTime, itGlobalLastTime, itGlobalDomId,
                         itGlobalSrcId));
        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_STRING,
                         iiStringFirstTime, iiStringLastTime, iiStringDomId,
                         iiStringSrcId));
        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_MODULE,
                         iiModuleFirstTime, iiModuleLastTime, iiModuleDomId,
                         iiModuleSrcId));
        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_IT_MODULE,
                         itModuleFirstTime, itModuleLastTime, itModuleDomId,
                         itModuleSrcId));

        List finalElems = elems.getManagedFinalReadoutRequestElements(list);

        assertEquals("Bad number of returned elements",
                     list.size() - 1, finalElems.size());

        for (Object obj : finalElems) {
            IReadoutRequestElement elem = (IReadoutRequestElement) obj;

            final String typeStr;
            final long firstTime;
            final long lastTime;
            final long domId;
            final long srcId;

            switch (elem.getReadoutType()) {
            case IReadoutRequestElement.READOUT_TYPE_GLOBAL:
                fail("Didn't expect to see a GLOBAL readout type");
                typeStr = null;
                firstTime = -1L;
                lastTime = -1L;
                domId = -1L;
                srcId = -1;
                break;
            case IReadoutRequestElement.READOUT_TYPE_II_GLOBAL:
                typeStr = "II_GLOBAL";
                firstTime = globalFirstTime;
                lastTime = iiGlobalLastTime;
                domId = globalDomId;
                srcId = globalSrcId;
                break;
            case IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL:
                typeStr = "IT_GLOBAL";
                firstTime = globalFirstTime;
                lastTime = itGlobalLastTime;
                domId = globalDomId;
                srcId = globalSrcId;
                break;
            case IReadoutRequestElement.READOUT_TYPE_II_STRING:
                typeStr = "II_STRING";
                firstTime = iiStringFirstTime;
                lastTime = iiStringLastTime;
                domId = iiStringDomId;
                srcId = iiStringSrcId;
                break;
            case IReadoutRequestElement.READOUT_TYPE_II_MODULE:
                typeStr = "II_MODULE";
                firstTime = iiModuleFirstTime;
                lastTime = iiModuleLastTime;
                domId = iiModuleDomId;
                srcId = iiModuleSrcId;
                break;
            case IReadoutRequestElement.READOUT_TYPE_IT_MODULE:
                typeStr = "IT_MODULE";
                firstTime = itModuleFirstTime;
                lastTime = itModuleLastTime;
                domId = itModuleDomId;
                srcId = itModuleSrcId;
                break;
            default:
                fail("Unknown source ID " + elem.getSourceID());
                typeStr = null;
                firstTime = -1L;
                lastTime = -1L;
                domId = -1L;
                srcId = -1;
                break;
            }

            if (typeStr != null) {
                assertEquals("Bad " + typeStr + " first time", firstTime,
                             elem.getFirstTimeUTC().longValue());
                assertEquals("Bad " + typeStr + " last time", lastTime,
                             elem.getLastTimeUTC().longValue());
                assertEquals("Bad " + typeStr + " DOM ID", domId,
                             elem.getDomID().longValue());
                assertEquals("Bad " + typeStr + " Source ID", srcId,
                             elem.getSourceID().getSourceID());
            }
        }
    }

    public void testMergeIntoOne()
    {
        GlobalTrigEventReadoutElements elems =
            new GlobalTrigEventReadoutElements();

        final int baseSrcId =
            SourceIdRegistry.STRING_HUB_SOURCE_ID;

        final long timeRange = 9999L;

        final long gFirstTime = timeRange + 1;
        final long gLastTime = gFirstTime + timeRange;
        final long gDomId = 123456789L;
        final int gSrcId = baseSrcId + 1;

        final long hFirstTime = gLastTime + 1;
        final long hLastTime = hFirstTime + timeRange;
        final long hDomId = 234567890L;
        final int hSrcId = baseSrcId + 2;

        ArrayList list = new ArrayList();
        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                         gFirstTime, gLastTime, gDomId, gSrcId));
        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                         hFirstTime, hLastTime, hDomId, hSrcId));

        List finalElems = elems.getManagedFinalReadoutRequestElements(list);

        assertEquals("Bad number of returned elements", 1, finalElems.size());

        IReadoutRequestElement elem =
            (IReadoutRequestElement) finalElems.get(0);

        assertEquals("Bad type",
                     IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                     elem.getReadoutType());
        assertEquals("Bad first time", gFirstTime,
                     elem.getFirstTimeUTC().longValue());
        assertEquals("Bad last time", hLastTime,
                     elem.getLastTimeUTC().longValue());
        assertEquals("Bad DOM ID", gDomId,
                     elem.getDomID().longValue());
        assertEquals("Bad Source ID", gSrcId,
                     elem.getSourceID().getSourceID());
    }

    public void testMergeTimeGap()
    {
        GlobalTrigEventReadoutElements elems =
            new GlobalTrigEventReadoutElements();
        elems.setAllowTimeGap(true);

        final int baseSrcId =
            SourceIdRegistry.STRING_HUB_SOURCE_ID;

        final long timeRange = 9999L;

        final long gFirstTime = timeRange + 1;
        final long gLastTime = gFirstTime + timeRange;
        final long gDomId = 123456789L;
        final int gSrcId = baseSrcId + 1;

        final long hFirstTime = gLastTime + 1;
        final long hLastTime = hFirstTime + timeRange;
        final long hDomId = 234567890L;
        final int hSrcId = baseSrcId + 2;

        ArrayList list = new ArrayList();
        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                         gFirstTime, gLastTime, gDomId, gSrcId));
        list.add(newElem(IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                         hFirstTime, hLastTime, hDomId, hSrcId));

        List finalElems = elems.getManagedFinalReadoutRequestElements(list);

        assertEquals("Bad number of returned elements", 2, finalElems.size());

        for (Object obj : finalElems) {
            IReadoutRequestElement elem = (IReadoutRequestElement) obj;

            final String typeStr;
            final long firstTime;
            final long lastTime;
            final long domId;

            assertEquals("Bad type",
                         IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                         elem.getReadoutType());

            switch (elem.getSourceID().getSourceID()) {
            case gSrcId:
                typeStr = "first";
                firstTime = gFirstTime;
                lastTime = gLastTime;
                domId = gDomId;
                break;
            case hSrcId:
                typeStr = "second";
                firstTime = hFirstTime;
                lastTime = hLastTime;
                domId = hDomId;
                break;
            default:
                fail("Unknown source ID " + elem.getSourceID());
                typeStr = null;
                firstTime = -1L;
                lastTime = -1L;
                domId = -1L;
                break;
            }

            if (typeStr != null) {
                assertEquals("Bad " + typeStr + " first time", firstTime,
                             elem.getFirstTimeUTC().longValue());
                assertEquals("Bad " + typeStr + " last time", lastTime,
                             elem.getLastTimeUTC().longValue());
                assertEquals("Bad " + typeStr + " DOM ID", domId,
                             elem.getDomID().longValue());
            }
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
