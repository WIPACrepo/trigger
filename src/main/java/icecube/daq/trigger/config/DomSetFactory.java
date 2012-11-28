package icecube.daq.trigger.config;

import icecube.daq.util.DOMRegistry;
import icecube.daq.util.DeployedDOM;
import icecube.daq.trigger.exceptions.ConfigException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;


/**
 * Configuration file utility
 */
public class DomSetFactory
{
    /**
     * logging object
     */
    private static final Log LOG = LogFactory.getLog(DomSetFactory.class);

    /**
     * Name of the DomSet definitions file
     */
    private static final String DOMSET_DEFS_FILE = "domset-definitions.xml";

    /**
     * Trigger configuration directory
     * (usually ~pdaq/pDAQ__current/config/trigger)
     */
    private static File TRIGGER_CONFIG_DIR;

    /**
     * DOMRegistry
     */
    private static DOMRegistry domRegistry = null;

    private static void addAllDoms(List<String>domIds, int hub, int low,
                                   int high)
    {
        for (DeployedDOM dom : domRegistry.getDomsOnHub(hub)) {
            int pos = dom.getStringMinor();
            if (pos >= low && pos <= high) {
                domIds.add(dom.getMainboardId());
            }
        }
    }

    /**
     * Load the specified DomSet.
     *
     * @param id DomSet ID
     *
     * @throws ConfigException if there is a problem
     */
    public static DomSet getDomSet(int id)
        throws ConfigException
    {
        return getDomSet(id, null);
    }

    /**
     * Load the specified DomSet.
     *
     * @param id DomSet ID
     * @param filename name of XML file containing DomSet definitions
     *
     * @throws ConfigException if there is a problem
     */
    public static DomSet getDomSet(int id, String filename)
        throws ConfigException
    {
        if (filename == null) {
            filename = DOMSET_DEFS_FILE;
        }

        Document doc = loadXMLDocument(filename);
        List<Node> nodeList = doc.selectNodes("//domsets/domset");
        for (Node n : nodeList) {
            int dsid;
            try {
                dsid = Integer.parseInt(n.valueOf("@id"));
            } catch (NumberFormatException nfe) {
                LOG.error("Ignoring bad DomSet ID \"" + n.valueOf("@id") +
                          "\"");
                continue;
            }

            if (dsid == id) {
                return loadDomSet(n.valueOf("@name"), n);
            }
        }

        throw new ConfigException("Cannot find DomSet #" + id);
    }

    /**
     * Load the specified DomSet.
     *
     * @param name DomSet name
     *
     * @throws ConfigException if there is a problem
     */
    public static DomSet getDomSet(String name)
        throws ConfigException
    {
        return getDomSet(name, null);
    }

    /**
     * Load the specified DomSet.
     *
     * @param name DomSet name
     * @param filename name of XML file containing DomSet definitions
     *
     * @throws ConfigException if there is a problem
     */
    public static DomSet getDomSet(String name, String filename)
        throws ConfigException
    {
        if (name == null) {
            throw new ConfigException("DomSet name cannot be null");
        }
        if (filename == null) {
            filename = DOMSET_DEFS_FILE;
        }

        Document doc = loadXMLDocument(filename);
        List<Node> nodeList = doc.selectNodes("//domsets/domset");
        for (Node n : nodeList) {
            if (name.equals(n.valueOf("@name"))) {
                return loadDomSet(name, n);
            }
        }

        throw new ConfigException("Cannot find DomSet \"" + name + "\"");
    }

    private static DomSet loadDomSet(String name, Node topNode)
        throws ConfigException
    {
        // check the DOMRegistry
        if (domRegistry == null) {
            LOG.warn("Must set the DOMRegistry first");
            return null;
        }

        ArrayList<String> domIds = new ArrayList<String>();

        loadOuterStrings(name, domIds, topNode);

        List<Node> posList = topNode.selectNodes("positions");
        for (Node p : posList) {
            int low;
            try {
                low = Integer.parseInt(p.valueOf("@low"));
            } catch (NumberFormatException nfe) {
                throw new ConfigException("Bad low position \"" +
                                          p.valueOf("@low") +
                                          "\" for DomSet " + name);
            }

            int high;
            try {
                high = Integer.parseInt(p.valueOf("@high"));
            } catch (NumberFormatException nfe) {
                throw new ConfigException("Bad high position \"" +
                                          p.valueOf("@high") +
                                          "\" for DomSet " + name);
            }

            if (low > high) {
                throw new ConfigException("Bad <positions> for DomSet " +
                                          name + ": Low value " + low +
                                          " is greater than high value " +
                                          high);
            }

            List<Node> strList = p.selectNodes("string");
            for (Node s : strList) {
                int hub;
                try {
                    hub = Integer.parseInt(s.valueOf("@hub"));
                } catch (NumberFormatException nfe) {
                    throw new ConfigException("Bad string hub \"" +
                                              s.valueOf("@hub") +
                                              "\" for DomSet " + name);
                }

                String pos = s.valueOf("@position");
                if (pos.length() != 0) {
                    LOG.error("DomSet " + name + " positions " + low + "-" +
                              high + ", hub " + hub + " should not include" +
                              " position " + pos);
                }

                addAllDoms(domIds, hub, low, high);
            }

            List<Node> strsList = p.selectNodes("strings");
            for (Node s : strsList) {
                int lowhub;
                try {
                    lowhub = Integer.parseInt(s.valueOf("@lowhub"));
                } catch (NumberFormatException nfe) {
                    throw new ConfigException("Bad strings lowhub \"" +
                                              s.valueOf("@lowhub") +
                                              "\" for DomSet " + name);
                }

                int highhub;
                try {
                    highhub = Integer.parseInt(s.valueOf("@highhub"));
                } catch (NumberFormatException nfe) {
                    throw new ConfigException("Bad strings highhub \"" +
                                              s.valueOf("@highhub") +
                                              "\" for DomSet " + name);
                }

                if (lowhub > highhub) {
                    throw new ConfigException("Bad <strings> for DomSet " +
                                              name + ": Low hub " + lowhub +
                                              " is greater than high hub " +
                                              highhub);
                }

                for (int hub = lowhub; hub <= highhub; hub++) {
                    addAllDoms(domIds, hub, low, high);
                }
            }
        }

        return new DomSet(name, domIds);
    }

    private static void loadOuterStrings(String name, List<String> domIds,
                                         Node topNode)
        throws ConfigException
    {
        List<Node> list = topNode.selectNodes("string");
        for (Node s : list) {
            int hub;
            try {
                hub = Integer.parseInt(s.valueOf("@hub"));
            } catch (NumberFormatException nfe) {
                throw new ConfigException("Bad string hub \"" +
                                          s.valueOf("@hub") +
                                          "\" for DomSet " + name);
            }

            int pos;
            try {
                pos = Integer.parseInt(s.valueOf("@position"));
            } catch (NumberFormatException nfe) {
                throw new ConfigException("Bad string position \"" +
                                          s.valueOf("@position") +
                                          "\" for DomSet " + name);
            }

            addAllDoms(domIds, hub, pos, pos);
        }
    }

    private static Document loadXMLDocument(String filename)
        throws ConfigException
    {
        if (TRIGGER_CONFIG_DIR == null) {
            throw new ConfigException("Trigger configuration directory" +
                                      " has not been set");
        }

        File defnFile = new File(TRIGGER_CONFIG_DIR, filename);
        if (!defnFile.isFile()) {
            throw new ConfigException("DomSet definitions file \"" +
                                      defnFile + "\" does not exist");
        }

        // open definitions file
        FileInputStream in;
        try {
            in = new FileInputStream(defnFile);
        } catch (IOException ioe) {
            throw new ConfigException("Cannot open DomSet definitions" +
                                      " file \"" + defnFile + "\"", ioe);
        }

        // load definitions
        try {
            SAXReader rdr = new SAXReader();
            Document doc;
            try {
                doc = rdr.read(in);
            } catch (DocumentException de) {
                throw new ConfigException("Cannot read run configuration" +
                                          " file \"" + defnFile + "\"", de);
            }

            return doc;
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                // ignore errors on close
            }
        }
    }

    public static void setConfigurationDirectory(String configDir)
        throws ConfigException
    {
        if (configDir == null) {
            TRIGGER_CONFIG_DIR = null;
        } else {
            File tmpDir = new File(configDir, "trigger");
            if (!tmpDir.isDirectory()) {
                throw new ConfigException("Trigger configuration directory \"" +
                                          tmpDir + "\" does not exist");
            }

            TRIGGER_CONFIG_DIR = tmpDir;
        }
    }

    public static void setDomRegistry(DOMRegistry dr) {
        domRegistry = dr;
    }
}
