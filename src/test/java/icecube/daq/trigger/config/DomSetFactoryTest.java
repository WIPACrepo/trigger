package icecube.daq.trigger.config;

import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.util.DOMRegistry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class DomSetFactoryTest
    extends TestCase
{
    private static final int MIN_ID = 0;
    private static final int MAX_ID = 6;

    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    private String configDir;
    private DOMRegistry domRegistry;

    private File testDir;

    public DomSetFactoryTest(String name)
    {
        super(name);
    }

    private File createDefFile(String text)
        throws ConfigException, IOException
    {
        if (testDir == null || !testDir.exists()) {
            testDir = File.createTempFile("tmpconfig", ".test");
            testDir.deleteOnExit();

            if (!testDir.delete()) {
                throw new IOException("Cannot delete temp file " + testDir);
            }

            if (!testDir.mkdir()) {
                throw new IOException("Cannot create test directory " +
                                      testDir);
            }
        }

        File trigDir = new File(testDir, "trigger");
        if (!trigDir.exists()) {
            if (!trigDir.mkdir()) {
                throw new IOException("Cannot create trigger config" +
                                      " directory " + trigDir);
            }
        }

        File tmpFile = File.createTempFile("bad-domset", ".xml", trigDir);

        BufferedWriter out = new BufferedWriter(new FileWriter(tmpFile));
        out.write(text);
        out.close();

        DomSetFactory.setConfigurationDirectory(trigDir.getParent());

        return tmpFile;
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        appender.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);

        if (configDir == null) {
            configDir = getClass().getResource("/config/").getPath();
        }
        DomSetFactory.setConfigurationDirectory(configDir);

        if (domRegistry == null) {
            domRegistry = DOMRegistry.loadRegistry(configDir);
        }
        DomSetFactory.setDomRegistry(domRegistry);
    }

    public static Test suite()
    {
        return new TestSuite(DomSetFactoryTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testNoRegistry()
        throws Exception
    {
        DomSetFactory.setDomRegistry(null);
        DomSet ds = DomSetFactory.getDomSet(1);
        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Must set the DOMRegistry first", appender.getMessage(0));
        appender.clear();

        assertNull("Unexpected DomSet", ds);
    }

    public void testBadConfigDir()
    {
        final String badPath = "/bad/path";

        try {
            DomSetFactory.setConfigurationDirectory(badPath);
        } catch (ConfigException ex) {
            final String badMsg = "Trigger configuration directory \"" +
                badPath + "/trigger\" does not exist";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testUnsetConfigDir()
        throws Exception
    {
        DomSetFactory.setConfigurationDirectory(null);
        try {
            DomSetFactory.getDomSet(1, "foo.xml");
            fail("Should not get DomSet 1");
        } catch (ConfigException ex) {
            final String badMsg = "Trigger configuration directory" +
                " has not been set";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testNoConfigFile()
        throws Exception
    {
        String cfgName = "foo.xml";
        try {
            DomSetFactory.getDomSet(1, cfgName);
            fail("Should not get DomSet 1");
        } catch (ConfigException ex) {
            File cfgFile = new File(configDir, "trigger/" + cfgName);
            final String badMsg = "DomSet definitions file \"" +
                cfgFile + "\" does not exist";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testEmpty()
        throws Exception
    {
        File defFile = createDefFile("");
        try {
            DomSetFactory.getDomSet(1, defFile.getName());
            fail("Should not get DomSet 1");
        } catch (ConfigException ex) {
            final String badMsg = "Cannot read run configuration file";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testNoSets()
        throws Exception
    {
        File defFile = createDefFile("<domsets/>");
        try {
            DomSetFactory.getDomSet(1, defFile.getName());
            fail("Should not get DomSet 1");
        } catch (ConfigException ex) {
            final String badMsg = "Cannot find DomSet #1";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testSetEmpty()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset/></domsets>");
        try {
            DomSetFactory.getDomSet(1, defFile.getName());
            fail("Should not get DomSet 1");
        } catch (ConfigException ex) {
            final String badMsg = "Cannot find DomSet #1";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Ignoring bad DomSet ID \"\"",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testSetLessEmpty()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset>" +
                                     "<string hub=\"1\" position=\"1\"/>" +
                                     "</domset></domsets>");
        try {
            DomSetFactory.getDomSet(1, defFile.getName());
            fail("Should not get DomSet 1");
        } catch (ConfigException ex) {
            final String badMsg = "Cannot find DomSet #1";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Ignoring bad DomSet ID \"\"",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testSetNoID()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset name=\"foo\">" +
                                     "<string hub=\"1\" position=\"1\"/>" +
                                     "</domset></domsets>");
        try {
            DomSetFactory.getDomSet(1, defFile.getName());
            fail("Should not get DomSet 1");
        } catch (ConfigException ex) {
            final String badMsg = "Cannot find DomSet #1";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Ignoring bad DomSet ID \"\"",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testSetNoName()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\">" +
                                     "<string hub=\"1\" position=\"1\"/>" +
                                     "</domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet \"x\"");
        } catch (ConfigException ex) {
            final String badMsg = "Cannot find DomSet \"x\"";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testSetEmptyID()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"\" name=\"x\">" +
                                     "<string hub=\"1\" position=\"1\"/>" +
                                     "</domset></domsets>");
        try {
            DomSetFactory.getDomSet(1, defFile.getName());
            fail("Should not get DomSet \"x\"");
        } catch (ConfigException ex) {
            final String badMsg = "Cannot find DomSet #1";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message",
                     "Ignoring bad DomSet ID \"\"",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testSetEmptyName()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"\">" +
                                     "<string hub=\"1\" position=\"1\"/>" +
                                     "</domset></domsets>");
        DomSet ds = DomSetFactory.getDomSet("", defFile.getName());
        assertEquals("Bad DomSet name", "", ds.getName());
    }


    public void testOuterStringNoHub()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<string position=\"1\"/>" +
                                     "</domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad string hub \"\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testOuterStringNoPosition()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<string hub=\"1\"/>" +
                                     "</domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad string position \"\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testOuterStringBadHub()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<string hub=\"A\" position=\"1\"/>" +
                                     "</domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad string hub \"A\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testOuterStringBadPosition()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<string hub=\"1\" position=\"B\"/>" +
                                     "</domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad string position \"B\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testPositionsNoLow()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions high=\"2\">" +
                                     "<string hub=\"1\"/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad low position \"\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testPositionsNoHigh()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"1\">" +
                                     "<string hub=\"1\"/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad high position \"\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testPositionsBadLow()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"A\" high=\"2\">" +
                                     "<string hub=\"1\"/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad low position \"A\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testPositionsBadHigh()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"1\" high=\"B\">" +
                                     "<string hub=\"1\"/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad high position \"B\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testPositionsReversed()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"2\" high=\"1\">" +
                                     "<string hub=\"1\"/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad <positions> for DomSet x:" +
                " Low value 2 is greater than high value 1";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testStringNoHub()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"1\" high=\"2\">" +
                                     "<string/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad string hub \"\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testStringBadHub()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"1\" high=\"2\">" +
                                     "<string hub=\"A\"/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad string hub \"A\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testStringUnneededPosition()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"1\" high=\"2\">" +
                                     "<string hub=\"1\" position=\"2\"/>" +
                                     "</positions></domset></domsets>");
        DomSet ds = DomSetFactory.getDomSet("x", defFile.getName());

        assertEquals("Bad number of log messages",
                     1, appender.getNumberOfMessages());
        assertEquals("Bad log message", "DomSet x positions 1-2, hub 1" +
                     " should not include position 2",
                     appender.getMessage(0));
        appender.clear();
    }

    public void testStringsNoLow()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"1\" high=\"2\">" +
                                     "<strings highhub=\"2\"/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad strings lowhub \"\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testStringsNoHigh()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"1\" high=\"2\">" +
                                     "<strings lowhub=\"1\"/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad strings highhub \"\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testStringsBadLow()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"1\" high=\"2\">" +
                                     "<strings lowhub=\"A\" highhub=\"2\"/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad strings lowhub \"A\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testStringsBadHigh()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"1\" high=\"2\">" +
                                     "<strings lowhub=\"1\" highhub=\"B\"/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad strings highhub \"B\" for DomSet x";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testStringsReversed()
        throws Exception
    {
        File defFile = createDefFile("<domsets><domset id=\"1\" name=\"x\">" +
                                     "<positions low=\"1\" high=\"2\">" +
                                     "<strings lowhub=\"2\" highhub=\"1\"/>" +
                                     "</positions></domset></domsets>");
        try {
            DomSetFactory.getDomSet("x", defFile.getName());
            fail("Should not get DomSet x");
        } catch (ConfigException ex) {
            final String badMsg = "Bad <strings> for DomSet x:" +
                " Low hub 2 is greater than high hub 1";
            assertTrue("Unexpected exception: " + ex.getMessage(),
                       ex.getMessage().startsWith(badMsg));
        }
    }

    public void testAllID()
        throws Exception
    {
        final int MAX_ID = 6;

        for (int i = -1; i <= MAX_ID + 1; i++) {
            boolean expectDomSet = (i >= 0 && i <= MAX_ID);

            DomSet ds;
            try {
                ds = DomSetFactory.getDomSet(i);
                if (!expectDomSet) {
                    fail("Should not have gotten DomSet #" + i);
                }
            } catch (ConfigException ce) {
                if (expectDomSet) {
                    fail("Didn't expect DomSet #" + i + " to fail: " + ce);
                }
                continue;
            }

            if (i != -1) {
                assertEquals("Bad number of log messages for id " + i,
                             0, appender.getNumberOfMessages());
            } else {
                assertEquals("Bad number of log messages for id " + i,
                             1, appender.getNumberOfMessages());
                assertEquals("Bad log message",
                             "Returning null for DomSet -1 (not sure why," +
                             " though)", appender.getMessage(0));
                appender.clear();
            }

            if (i < 0 || i > MAX_ID) {
                assertNull("Unexpected DomSet returned for id " + i, ds);
            }
        }
    }

    public void testAllName()
        throws Exception
    {
        String[] names = new String[] {
            null, "BOGUS", "AMANDA_SYNC", "AMANDA_TRIG", "INICE",
            "ICETOP", "DEEPCORE1", "DEEPCORE2", "DEEPCORE3",
        };

        for (String name: names) {
            boolean expectDomSet = (name != null && !name.equals("BOGUS"));
            DomSet ds;
            try {
                ds = DomSetFactory.getDomSet(name);
                if (!expectDomSet) {
                    fail("Should not have gotten DomSet " + name);
                }
            } catch (ConfigException ce) {
                if (expectDomSet) {
                    fail("Didn't expect DomSet " + name + " to fail: " + ce);
                }
                continue;
            }

            if (expectDomSet) {
                assertEquals("Bad number of log messages for " + name,
                             0, appender.getNumberOfMessages());
            } else {
                assertEquals("Bad number of log messages for " + name,
                             1, appender.getNumberOfMessages());
                assertEquals("Bad log message",
                             "Returning null for DomSet -1 (not sure why," +
                             " though)", appender.getMessage(0));
                appender.clear();
            }

            if (!expectDomSet) {
                assertNull("Unexpected DomSet returned for " + name, ds);
            }
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
