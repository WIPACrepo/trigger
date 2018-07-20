package icecube.daq.trigger.config;

import icecube.daq.common.MockAppender;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.test.MockDOMID;
import icecube.daq.trigger.test.MockDOMRegistry;
import icecube.daq.util.DOMInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

class MyRegistry
    extends MockDOMRegistry
{
    final DOMInfo addDom(int hub, int pos)
    {
        return addDom(hub, pos, hub);
    }

    final DOMInfo addDom(int hubId, int pos, int originalString)
    {
        final long mbid = (long) (hubId + 1) * 0x1000L + (long) pos;
        DOMInfo dom = new DOMInfo(mbid, originalString, pos, hubId);
        addDom(mbid, originalString, pos, hubId);
        return dom;
    }

    final void addIceTop(int hub, int originalString)
    {
        for (int pos = 61; pos <= 64; pos++) {
            addDom(originalString, pos, hub);
        }
    }

    final void addInIce(int hub)
    {
        for (int pos = 1; pos <= 60; pos++) {
            addDom(hub, pos, hub);
        }
    }

    final void addScintillators(int hub, int originalString)
    {
        for (int pos = 65; pos <= 66; pos++) {
            addDom(originalString, pos, hub);
        }
    }
}

public class DomSetFactoryTest
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    private static final MockDOMRegistry registry = buildRegistry();

    private static final MockDOMRegistry buildRegistry()
    {
        MyRegistry reg = new MyRegistry();
        reg.addDom(0, 65);
        reg.addDom(0, 66);

        // add string 1 with icetop DOMs on hub 203 and scintillors on hub 211
        reg.addInIce(1);
        reg.addIceTop(1, 203);
        reg.addScintillators(1, 211);

        // add deep core string 35 with icetop DOMs on hub 204
        reg.addInIce(35);
        reg.addIceTop(35, 204);

        // add deep core string 54 with icetop DOMs on hub 205
        reg.addInIce(54);
        reg.addIceTop(54, 205);

        // add deep core string 82 with icetop DOMs on hub 206 and
        // scintillors on hub 211
        reg.addInIce(82);
        reg.addIceTop(82, 206);
        reg.addScintillators(82, 211);

        return reg;
    }

    /**
     * Check that a string of integers contains the values in the expected list
     * @param listStr a string of comma separated values and/or ranges
     * @param expected list of expected integer values
     * @throws ConfigException if the string cannot be parsed
     */
    private void checkList(String listStr, int[] expected)
        throws ConfigException
    {
        List<Integer> list = DomSetFactory.parseList(listStr, listStr, null);
        for (int i : expected) {
            if (!list.contains(Integer.valueOf(i))) {
                fail("List \"" + listStr + "\" does not contain " + i);
            }
        }
    }

    /**
     * This is a bit ugly; it's the hard-coded rules from the standard
     * domset-definitions.xml file
     */
    private Set<DOMInfo> extractDomSet(int setId)
    {
        HashSet<DOMInfo> domset = new HashSet<DOMInfo>();

        for (DOMInfo dom : registry.allDOMs()) {
            switch (setId) {
            case 0: // AMANDA_SYNC
                if (dom.getStringMajor() == 0 && dom.getStringMinor() == 91) {
                    domset.add(dom);
                }
                break;
            case 1: // AMANDA_TRIG
                if (dom.getStringMajor() == 0 && dom.getStringMinor() == 92) {
                    domset.add(dom);
                }
                break;
            case 2: // INICE
                if (dom.getHubId() <= 78 && dom.getStringMinor() <= 60) {
                    domset.add(dom);
                }
                break;
            case 3: // ICETOP
                if (dom.getHubId() >= 201 && dom.getStringMinor() >= 61 &&
                    dom.getStringMinor() <= 64)
                {
                    domset.add(dom);
                }
                break;
            case 4: // DEEPCORE1
                if (isOldDeepCore(dom) &&
                    dom.getStringMinor() >= 41 && dom.getStringMinor() <= 60)
                {
                    domset.add(dom);
                } else if (isInnerDeepCore(dom)) {
                    domset.add(dom);
                }
                break;
            case 5: // DEEPCORE2
                if (isOldDeepCore(dom) &&
                    dom.getStringMinor() >= 39 && dom.getStringMinor() <= 60)
                {
                    domset.add(dom);
                } else if (isInnerDeepCore(dom)) {
                    domset.add(dom);
                }
                break;
            case 6: // DEEPCORE3
                if (isNewDeepCore(dom) &&
                    dom.getStringMinor() >= 39 && dom.getStringMinor() <= 60)
                {
                    domset.add(dom);
                } else if (isInnerDeepCore(dom)) {
                    domset.add(dom);
                }
                break;
            }
        }

        return domset;
    }

    private boolean isInnerDeepCore(DOMInfo dom)
    {
        return dom.getHubId() >= 79 && dom.getHubId() <= 86 &&
            dom.getStringMinor() >= 11 && dom.getStringMinor() <= 60;
    }

    private boolean isNewDeepCore(DOMInfo dom)
    {
        switch (dom.getHubId()) {
        case 25: return true;
        case 26: return true;
        case 27: return true;
        case 34: return true;
        case 35: return true;
        case 36: return true;
        case 37: return true;
        case 44: return true;
        case 45: return true;
        case 46: return true;
        case 47: return true;
        case 54: return true;
        }

        return false;
    }

    private boolean isOldDeepCore(DOMInfo dom)
    {
        switch (dom.getHubId()) {
        case 26: return true;
        case 27: return true;
        case 35: return true;
        case 36: return true;
        case 37: return true;
        case 45: return true;
        case 46: return true;
        }

        return false;
    }

    @BeforeClass
    public static final void setUpClass()
        throws Exception
    {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);

        DomSetFactory.setDomRegistry(registry);
    }

    @Before
    public void setUp()
        throws Exception
    {
        DomSetFactory.setConfigurationDirectory(findTestConfig());
    }

    @After
    public void tearDown()
        throws Exception
    {
        appender.assertNoLogMessages();
    }

    private String findTestConfig()
    {
        final String configDir =
            getClass().getResource("/config/").getPath();

        final String classCfgStr = "/classes/config/";
        if (!configDir.endsWith(classCfgStr)) {
            return configDir;
        }

        final int breakPt = configDir.length() - (classCfgStr.length() - 1);
        return configDir.substring(0, breakPt) + "test-" +
                configDir.substring(breakPt);
    }

    @Test
    public void testBadSet()
    {
        try {
            DomSetFactory.getDomSet(9999);
            fail("Should not find unknown DomSet");
        } catch (ConfigException cex) {
            assertTrue("Unexpected exception " + cex,
                       cex.getMessage().startsWith("Cannot find DomSet"));
        }
    }

    @Test
    public void testNullName()
    {
        try {
            DomSetFactory.getDomSet(null);
            fail("Should not find null DomSet");
        } catch (ConfigException cex) {
            final String errmsg = "DomSet name cannot be null";
            assertTrue("Unexpected exception " + cex,
                       cex.getMessage().startsWith(errmsg));
        }
    }

    @Test
    public void testNoConfigDir()
    {
        try {
            DomSetFactory.setConfigurationDirectory(null);
        } catch (ConfigException cex) {
            fail("Cannot set configuration directory to null: " + cex);
        }

        try {
            DomSetFactory.getDomSet(1);
            fail("Should not find DomSet without config directory");
        } catch (ConfigException cex) {
            assertTrue("Unexpected exception " + cex,
                       cex.getMessage().startsWith("Trigger configuration"));
        }

        try {
            DomSetFactory.getDomSet("ABC");
            fail("Should not find DomSet without config directory");
        } catch (ConfigException cex) {
            assertTrue("Unexpected exception " + cex,
                       cex.getMessage().startsWith("Trigger configuration"));
        }
    }

    @Test
    public void testOldIds()
        throws ConfigException
    {
        final String oldname = "old-" + DomSetFactory.DOMSET_DEFS_FILE;
        final int oldDefs = 7;

        for (int i = 0; i < oldDefs; i++) {
            DomSet ds = DomSetFactory.getDomSet(i, oldname);

            Set<DOMInfo> doms = extractDomSet(i);
            assertEquals("Bad number of DOMs in DomSet " + i, doms.size(),
                         ds.size());
            for (DOMInfo dom : doms) {
                MockDOMID domId = new MockDOMID(dom.getNumericMainboardId());
                assertTrue("DomSet " + i + " is missing " + dom,
                           ds.inSet(domId));
            }
        }
    }

    @Test
    public void testAllIds()
        throws ConfigException
    {
        for (int i = 0; i < 7; i++) {
            DomSet ds = DomSetFactory.getDomSet(i);

            Set<DOMInfo> doms = extractDomSet(i);
            assertEquals("Bad number of DOMs in DomSet " + i, doms.size(),
                         ds.size());
            for (DOMInfo dom : doms) {
                MockDOMID domId = new MockDOMID(dom.getNumericMainboardId());
                assertTrue("DomSet " + i + " is missing " + dom,
                           ds.inSet(domId));
            }
        }
    }

    @Test
    public void testListParse()
        throws ConfigException
    {
        int[] one = new int[1];
        for (int i = 0; i < 10; i++) {
            one[0] = i;
            checkList(Integer.toString(i), one);
        }

        int[] four = new int[] { 1, 2, 3, 4 };

        checkList("1-4", four);
        checkList("1,2,3,4", four);
        checkList("2-3,1,4", four);
        checkList("4,1,2-3", four);
    }
}
