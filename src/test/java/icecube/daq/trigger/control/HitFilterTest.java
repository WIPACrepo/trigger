package icecube.daq.trigger.control;

import icecube.daq.trigger.test.MockHit;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class HitFilterTest
    extends TestCase
{
    private static final long SYNC_DOMID = 0x1e5b72775d19L;
    private static final long TRIG_DOMID = 0x1d165fc478caL;

    private MockHit syncHit = new MockHit(12345L, SYNC_DOMID);
    private MockHit trigHit = new MockHit(12345L, TRIG_DOMID);
    private MockHit otherHit = new MockHit(12345L, 0x123456789abcL);

    public HitFilterTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(HitFilterTest.class);
    }

    public void testDefaultFilter()
    {
        HitFilter filter = new HitFilter();

        assertFalse("Bad default filter response to sync hit",
                   filter.useHit(syncHit));
        assertFalse("Bad default filter response to trig hit",
                    filter.useHit(trigHit));
        assertTrue("Bad default filter response to other hit",
                   filter.useHit(otherHit));
    }

    public void testSyncFilter()
    {
        HitFilter filter = new HitFilter(0);

        assertTrue("Bad sync filter response to sync hit",
                   filter.useHit(syncHit));
        assertFalse("Bad sync filter response to trig hit",
                    filter.useHit(trigHit));
        assertFalse("Bad sync filter response to other hit",
                    filter.useHit(otherHit));
    }

    public void testTrigFilter()
    {
        HitFilter filter = new HitFilter(1);

        assertFalse("Bad trig filter response to sync hit",
                   filter.useHit(syncHit));
        assertTrue("Bad trig filter response to trig hit",
                    filter.useHit(trigHit));
        assertFalse("Bad trig filter response to other hit",
                    filter.useHit(otherHit));
    }

    public void testOtherFilter()
    {
        HitFilter filter = new HitFilter(2);

        assertFalse("Bad other filter response to sync hit",
                   filter.useHit(syncHit));
        assertFalse("Bad other filter response to trig hit",
                    filter.useHit(trigHit));
        assertTrue("Bad other filter response to other hit",
                    filter.useHit(otherHit));
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
