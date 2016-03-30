package icecube.daq.trigger.config;

import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.util.DOMRegistry;
import icecube.daq.util.DeployedDOM;
import icecube.daq.util.JAXPUtil;
import icecube.daq.util.JAXPUtilException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Configuration file utility
 */
public abstract class DomSetFactory
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
    private static File triggerConfigDir;

    /** DOMRegistry */
    private static DOMRegistry domRegistry;

    /**
     * Add all DOMs from <tt>hub</tt> within the range
     * [<tt>low</tt>-<tt>high</tt>] to the <tt>domIds</tt> list.
     */
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
     * @return DOMs in the named set
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
     * @return DOMs in the named set
     *
     * @throws ConfigException if there is a problem
     */
    public static DomSet getDomSet(int id, String filename)
        throws ConfigException
    {
        if (triggerConfigDir == null) {
            throw new ConfigException("Trigger configuration directory" +
                                      " has not been set");
        }

        String realname;
        if (filename == null) {
            realname = DOMSET_DEFS_FILE;
        } else {
            realname = filename;
        }

        Document doc;
        try {
            doc = JAXPUtil.loadXMLDocument(triggerConfigDir, realname);
        } catch (JAXPUtilException jux) {
            throw new ConfigException(jux);
        }

        NodeList nodeList;
        try {
            nodeList = JAXPUtil.extractNodeList(doc, "//domsets/domset");
        } catch (JAXPUtilException jux) {
            throw new ConfigException(jux);
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node n = nodeList.item(i);

            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element elem = (Element) n;

            int dsid;
            try {
                dsid = Integer.parseInt(elem.getAttribute("id"));
            } catch (NumberFormatException nfe) {
                LOG.error("Ignoring bad DomSet ID \"" +
                          elem.getAttribute("id") + "\"");
                continue;
            }

            if (dsid == id) {
                return loadDomSet(elem.getAttribute("name"), n);
            }
        }

        throw new ConfigException("Cannot find DomSet #" + id);
    }

    /**
     * Load the specified DomSet from the default DomSet file.
     *
     * @param name DomSet name
     *
     * @return DOMs in the named set
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
     * @return DOMs in the named set
     *
     * @throws ConfigException if there is a problem
     */
    public static DomSet getDomSet(String name, String filename)
        throws ConfigException
    {
        if (name == null) {
            throw new ConfigException("DomSet name cannot be null");
        }

        if (triggerConfigDir == null) {
            throw new ConfigException("Trigger configuration directory" +
                                      " has not been set");
        }

        String realname;
        if (filename == null) {
            realname = DOMSET_DEFS_FILE;
        } else {
            realname = filename;
        }

        Document doc;
        try {
            doc = JAXPUtil.loadXMLDocument(triggerConfigDir, realname);
        } catch (JAXPUtilException jux) {
            throw new ConfigException(jux);
        }

        NodeList nodeList;
        try {
            nodeList = JAXPUtil.extractNodeList(doc, "//domsets/domset");
        } catch (JAXPUtilException jux) {
            throw new ConfigException(jux);
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node n = nodeList.item(i);

            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element elem = (Element) n;

            if (name.equals(elem.getAttribute("name"))) {
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

        XPath xpath = XPathFactory.newInstance().newXPath();

        NodeList posList;
        try {
            posList = JAXPUtil.extractNodeList(xpath, topNode, "positions");
        } catch (JAXPUtilException jux) {
            throw new ConfigException(jux);
        }

        for (int i = 0; i < posList.getLength(); i++) {
            Node np = posList.item(i);

            if (np.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element p = (Element) np;

            int low;
            try {
                low = Integer.parseInt(p.getAttribute("low"));
            } catch (NumberFormatException nfe) {
                throw new ConfigException("Bad low position \"" +
                                          p.getAttribute("low") +
                                          "\" for DomSet " + name);
            }

            int high;
            try {
                high = Integer.parseInt(p.getAttribute("high"));
            } catch (NumberFormatException nfe) {
                throw new ConfigException("Bad high position \"" +
                                          p.getAttribute("high") +
                                          "\" for DomSet " + name);
            }

            if (low > high) {
                throw new ConfigException("Bad <positions> for DomSet " +
                                          name + ": Low value " + low +
                                          " is greater than high value " +
                                          high);
            }

            NodeList strList;
            try {
                strList = JAXPUtil.extractNodeList(xpath, p, "string");
            } catch (JAXPUtilException jux) {
                throw new ConfigException(jux);
            }

            for (int j = 0; j < strList.getLength(); j++) {
                Node n = strList.item(j);

                if (n.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element s = (Element) n;

                int hub;
                try {
                    hub = Integer.parseInt(s.getAttribute("hub"));
                } catch (NumberFormatException nfe) {
                    throw new ConfigException("Bad string hub \"" +
                                              s.getAttribute("hub") +
                                              "\" for DomSet " + name);
                }

                String pos = s.getAttribute("position");
                if (pos.length() != 0) {
                    LOG.error("DomSet " + name + " positions " + low + "-" +
                              high + ", hub " + hub + " should not include" +
                              " position " + pos);
                }

                addAllDoms(domIds, hub, low, high);
            }

            NodeList strsList;
            try {
                strsList = JAXPUtil.extractNodeList(xpath, p, "strings");
            } catch (JAXPUtilException jux) {
                throw new ConfigException(jux);
            }

            for (int j = 0; j < strsList.getLength(); j++) {
                Node n = strsList.item(j);

                if (n.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element s = (Element) n;

                int lowhub;
                try {
                    lowhub = Integer.parseInt(s.getAttribute("lowhub"));
                } catch (NumberFormatException nfe) {
                    throw new ConfigException("Bad strings lowhub \"" +
                                              s.getAttribute("lowhub") +
                                              "\" for DomSet " + name);
                }

                int highhub;
                try {
                    highhub = Integer.parseInt(s.getAttribute("highhub"));
                } catch (NumberFormatException nfe) {
                    throw new ConfigException("Bad strings highhub \"" +
                                              s.getAttribute("highhub") +
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
        NodeList list;
        try {
            list = JAXPUtil.extractNodeList(topNode, "string");
        } catch (JAXPUtilException jux) {
            throw new ConfigException(jux);
        }

        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);

            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element s = (Element) n;

            int hub;
            try {
                hub = Integer.parseInt(s.getAttribute("hub"));
            } catch (NumberFormatException nfe) {
                throw new ConfigException("Bad string hub \"" +
                                          s.getAttribute("hub") +
                                          "\" for DomSet " + name);
            }

            int pos;
            try {
                pos = Integer.parseInt(s.getAttribute("position"));
            } catch (NumberFormatException nfe) {
                throw new ConfigException("Bad string position \"" +
                                          s.getAttribute("position") +
                                          "\" for DomSet " + name);
            }

            addAllDoms(domIds, hub, pos, pos);
        }
    }

    /**
     * Set the configuration directory.
     *
     * @param configDir absolute path of DAQ configuration directory
     *
     * @throws ConfigException if there is a problem
     */
    public static void setConfigurationDirectory(String configDir)
        throws ConfigException
    {
        if (configDir == null) {
            triggerConfigDir = null;
        } else {
            File tmpDir = new File(configDir, "trigger");
            if (!tmpDir.isDirectory()) {
                throw new ConfigException("Trigger configuration" +
                                          " directory \"" + tmpDir +
                                          "\" does not exist");
            }

            triggerConfigDir = tmpDir;
        }
    }

    /**
     * Set the DOM registry used to find all DOMs associated with a hub.
     *
     * @param dr DOM registry
     */
    public static void setDomRegistry(DOMRegistry dr)
    {
        domRegistry = dr;
    }
}
