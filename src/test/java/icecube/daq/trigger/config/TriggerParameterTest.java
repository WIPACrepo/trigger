package icecube.daq.trigger.config;

import org.junit.*;
import static org.junit.Assert.*;

public class TriggerParameterTest
{
    @Test
    public void testAll()
    {
        TriggerParameter tp = new TriggerParameter();
        assertNull("Name is not null", tp.getName());
        assertNull("Value is not null", tp.getValue());

        final String name = "foo";
        tp.setName(name);

        assertEquals("Bad name", name, tp.getName());
        assertNull("Value is not null", tp.getValue());

        final String value = "foo";
        tp.setValue(value);

        assertEquals("Bad name", name, tp.getName());
        assertEquals("Bad value", value, tp.getValue());

        assertEquals("Bad string", name + " = " + value, tp.toString());
    }
}
