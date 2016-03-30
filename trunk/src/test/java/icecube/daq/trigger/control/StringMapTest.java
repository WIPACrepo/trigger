package icecube.daq.trigger.control;

import icecube.daq.trigger.test.MockAppender;

import java.util.List;

import org.junit.*;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

public class StringMapTest
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
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());
    }

    @Test
    public void testSimple()
    {
        StringMap map = StringMap.getInstance();
        for (int i = 0; i < 86; i++) {
            List<Integer> list = map.getNeighbors(Integer.valueOf(i));
            Integer voff = map.getVerticalOffset(Integer.valueOf(i));

            if (i > 0 && i <= 80) {
                assertEquals("Bad number of log messages",
                             0, appender.getNumberOfMessages());

                int expVOff;
                if (i == 21) {
                    expVOff = 1;
                } else if (i == 38) {
                    expVOff = -1;
                } else {
                    expVOff = 0;
                }

                assertEquals("Bad vertical offset for " + i,
                             voff, Integer.valueOf(expVOff));
            } else {
                assertEquals("Bad number of log messages for " + i,
                             1, appender.getNumberOfMessages());

                final String msg = "Coordinate of string " + i + " is null";
                assertEquals("Bad log message for " + i,
                             msg, appender.getMessage(0));

                appender.clear();

                assertNull("Vertical offset for " + i + "  is not null", voff);
            }

            assertNotNull("Returned list for " + i + " is null", list);
            assertEquals("Returned list for " + i + " is not empty",
                         0, list.size());
        }
    }
}
