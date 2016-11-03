package icecube.daq.trigger.config;

import icecube.daq.common.MockAppender;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

import org.junit.*;
import static org.junit.Assert.*;

public class TriggerReadoutTest
{
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

    @Test
    public void testAll()
    {
        TriggerReadout tr = new TriggerReadout();
        assertEquals("Bad default type",
                     TriggerReadout.DEFAULT_READOUT_TYPE, tr.getType());
        assertEquals("Bad default offset", 0, tr.getOffset());
        assertEquals("Bad default minus", 0, tr.getMinus());
        assertEquals("Bad default plus", 0, tr.getPlus());

        final int offset = 10;
        tr.setOffset(offset);

        assertEquals("Bad default type",
                     TriggerReadout.DEFAULT_READOUT_TYPE, tr.getType());
        assertEquals("Bad offset", offset, tr.getOffset());
        assertEquals("Bad default minus", 0, tr.getMinus());
        assertEquals("Bad default plus", 0, tr.getPlus());

        final int minus = 15;
        tr.setMinus(minus);

        assertEquals("Bad default type",
                     TriggerReadout.DEFAULT_READOUT_TYPE, tr.getType());
        assertEquals("Bad offset", offset, tr.getOffset());
        assertEquals("Bad minus", minus, tr.getMinus());
        assertEquals("Bad default plus", 0, tr.getPlus());

        final int plus = 20;
        tr.setPlus(plus);

        assertEquals("Bad default type",
                     TriggerReadout.DEFAULT_READOUT_TYPE, tr.getType());
        assertEquals("Bad offset", offset, tr.getOffset());
        assertEquals("Bad minus", minus, tr.getMinus());
        assertEquals("Bad plus", plus, tr.getPlus());
    }

    @Test
    public void testNegCTOR()
    {
        final int offset = 100;
        final int minus = 200;
        final int plus = 500;

        TriggerReadout tr =
            new TriggerReadout(TriggerReadout.DEFAULT_READOUT_TYPE,
                               offset, -minus, -plus);

        assertEquals("Bad default type",
                     TriggerReadout.DEFAULT_READOUT_TYPE, tr.getType());
        assertEquals("Bad offset", offset, tr.getOffset());
        assertEquals("Bad minus", minus, tr.getMinus());
        assertEquals("Bad plus", plus, tr.getPlus());

        final String msg1 = "Readout time minus should be non-negative";
        appender.assertLogMessage(msg1);

        final String msg2 = "Readout time plus should be non-negative";
        appender.assertLogMessage(msg2);

        appender.assertNoLogMessages();
    }

    @Test
    public void testNegMinus()
    {
        TriggerReadout tr = new TriggerReadout();
        tr.setMinus(-5);
        assertEquals("Bad minus", 5, tr.getMinus());

        final String msg = "Readout time minus should be non-negative";
        appender.assertLogMessage(msg);

        appender.assertNoLogMessages();
    }

    @Test
    public void testNegPlus()
    {
        TriggerReadout tr = new TriggerReadout();
        tr.setPlus(-5);
        assertEquals("Bad plus", 5, tr.getPlus());

        final String msg = "Readout time plus should be non-negative";
        appender.assertLogMessage(msg);

        appender.assertNoLogMessages();
    }
}
