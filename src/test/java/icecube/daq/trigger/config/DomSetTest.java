package icecube.daq.trigger.config;

import icecube.daq.payload.IDOMID;
import icecube.daq.trigger.test.MockDOMID;
import icecube.daq.util.DOMInfo;

import java.util.ArrayList;

import org.junit.*;
import static org.junit.Assert.*;

public class DomSetTest
{
    @Test
    public void testEmpty()
    {
        final String name = "foo";

        DomSet ds = new DomSet(name);
        assertEquals("Bad name", name, ds.getName());
        assertEquals("Bad string", name + "*0", ds.toString());
        assertFalse("Null DOM should not be in empty set", ds.inSet(null));
        assertFalse("DOM should not be in empty set",
                    ds.inSet(new MockDOMID(1)));
    }

    @Test
    public void testSome()
    {
        final String name = "foo";

        DomSet ds = new DomSet(name);

        ArrayList<IDOMID> doms = new ArrayList<IDOMID>();
        ArrayList<Long> ids = new ArrayList<Long>();
        for (int i = 100; i < 200; i += 10) {
            IDOMID dom = new MockDOMID(i);
            doms.add(dom);
            ds.add(new DOMInfo(i, i / 50, (i % 50) + 1, i / 50));
        }

        assertEquals("Bad name", name, ds.getName());
        assertEquals("Bad string", name + "*" + doms.size(), ds.toString());
        assertFalse("Null DOM should not be in set", ds.inSet(null));

        for (IDOMID dom : doms) {
            assertTrue("DOM should be in set", ds.inSet(dom));
        }

        assertFalse("Compare with null should not succeed", ds.equals(null));
        assertFalse("Compare with string should not succeed",
                    ds.equals("foo"));
        assertFalse("Compare with empty DomSet should not succeed",
                    ds.equals(new DomSet("xxx")));
        assertTrue("Compare with self should succeed", ds.equals(ds));
    }
}
