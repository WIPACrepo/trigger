package icecube.daq.trigger.control;

import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.util.DOMRegistry;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class HitFilterTest
    extends TestCase
{
    private static final long SYNC_DOMID = 0x1e5b72775d19L;
    private static final long TRIG_DOMID = 0x1d165fc478caL;
    private static final long INICE_DOMID = 0xf23c1d938a5bL;
    private static final long ICETOP_DOMID = 0x45bb1c8696abL;
    private static final long DEEPCORE_DOMID = 0xd67fd910f139L;

    private MockHit syncHit = new MockHit(12345L, SYNC_DOMID);
    private MockHit trigHit = new MockHit(12345L, TRIG_DOMID);
    private MockHit iniceHit = new MockHit(12345L, INICE_DOMID);
    private MockHit icetopHit = new MockHit(12345L, ICETOP_DOMID);
    private MockHit deepcoreHit = new MockHit(12345L, DEEPCORE_DOMID);

    public HitFilterTest(String name)
    {
        super(name);
    }

    private void checkResponses(HitFilter filter, String name, boolean syncValid,
                                boolean trigValid, boolean iniceValid,
                                boolean icetopValid, boolean deepcoreValid)
    {
        assertEquals("Bad " + name + " filter response to sync hit",
                     syncValid, filter.useHit(syncHit));
        assertEquals("Bad " + name + " filter response to trig hit",
                     trigValid, filter.useHit(trigHit));
        assertEquals("Bad " + name + " filter response to inice hit",
                     iniceValid, filter.useHit(iniceHit));
        assertEquals("Bad " + name + " filter response to icetop hit",
                     icetopValid, filter.useHit(icetopHit));
        assertEquals("Bad " + name + " filter response to deepcore hit",
                     deepcoreValid, filter.useHit(deepcoreHit));
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        String configDir =
            getClass().getResource("/config/").getPath();

        String classCfgStr = "/classes/config/";
        if (configDir.endsWith(classCfgStr)) {
            int breakPt = configDir.length() - (classCfgStr.length() - 1);
            configDir = configDir.substring(0, breakPt) + "test-" +
                configDir.substring(breakPt);
        }

        DomSetFactory.setConfigurationDirectory(configDir);

        try {
            DomSetFactory.setDomRegistry(DOMRegistry.loadRegistry(configDir));
        } catch (Exception ex) {
            throw new Error("Cannot set DOM registry", ex);
        }
    }

    public static Test suite()
    {
        return new TestSuite(HitFilterTest.class);
    }

    public void testDefaultFilter()
    {
        checkResponses(new HitFilter(), "default", true, true, true, true, true);
    }

    public void testSyncFilter()
        throws ConfigException
    {
        checkResponses(new HitFilter(0), "sync",
                       true, false, false, false, false);
    }

    public void testTrigFilter()
        throws ConfigException
    {
        checkResponses(new HitFilter(1), "trig",
                       false, true, false, false, false);
    }

    public void testInIceFilter()
        throws ConfigException
    {
        checkResponses(new HitFilter(2), "in-ice",
                       false, false, true, false, false);
    }

    public void testIceTopFilter()
        throws ConfigException
    {
        checkResponses(new HitFilter(3), "icetop",
                       false, false, false, true, false);
    }

    public void testDeepCoreFilter()
        throws ConfigException
    {
        checkResponses(new HitFilter(4), "deep-core",
                       false, false, false, false, true);
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
