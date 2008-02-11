package icecube.daq.trigger.component;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Created by IntelliJ IDEA.
 * User: pat
 * Date: Dec 18, 2006
 * Time: 6:24:31 PM
 */
public class GlobalConfiguration
{

    /**
     * Logging object
     */
    private static final Log log = LogFactory.getLog(GlobalConfiguration.class);

    /**
     * Name of dom config xml element
     */
    private static final String DOM_CONFIG_TAG = "domConfigList";

    /**
     * Name of trigger config xml element
     */
    private static final String TRIGGER_CONFIG_TAG = "triggerConfig";

    /**
     * Get the DOM configuration name.
     * @param configFileName fully qualified global configuration file name
     * @return name of DOM configuration
     * @throws ParserConfigurationException if the xml parser cannot be created
     * @throws SAXException if the xml file cannot be parsed
     * @throws IOException if the xml file cannot be parsed
     */
    public static String getDomConfigList(String configFileName)
            throws ParserConfigurationException, SAXException, IOException {
        return parseDomConfig(configFileName);
    }

    /**
     * Get the trigger configuration name.
     * @param configFileName fully qualified global configuration file name
     * @return name of trigger configuration
     * @throws ParserConfigurationException if the xml parser cannot be created
     * @throws SAXException if the xml file cannot be parsed
     * @throws IOException if the xml file cannot be parsed
     */
    public static String getTriggerConfig(String configFileName)
            throws ParserConfigurationException, SAXException, IOException {
        return parseTriggerConfig(configFileName);
    }

    /**
     * Parse the global configuration file for the DOM config.
     * @param configFileName fully qualified global configuration file name
     * @return dom configuration tag
     * @throws ParserConfigurationException if the xml parser cannot be created
     * @throws SAXException if the xml file cannot be parsed
     * @throws IOException if the xml file cannot be parsed
     */
    private static String parseDomConfig(String configFileName)
            throws ParserConfigurationException, SAXException, IOException {

        Element rootElement = getRootElement(configFileName);

        NodeList elements = rootElement.getElementsByTagName(DOM_CONFIG_TAG);
        if (1 != elements.getLength()) {
            log.error("More than one " + DOM_CONFIG_TAG + " elements found in " + configFileName);
            return null;
        }
        return elements.item(0).getNodeValue();

    }

    /**
     * Parse the global configuration file for the trigger config.
     * @param configFileName fully qualified global configuration file name
     * @return trigger configuration tag
     * @throws ParserConfigurationException if the xml parser cannot be created
     * @throws SAXException if the xml file cannot be parsed
     * @throws IOException if the xml file cannot be parsed
     */
    private static String parseTriggerConfig(String configFileName)
            throws ParserConfigurationException, SAXException, IOException {

        Element rootElement = getRootElement(configFileName);

        NodeList elements = rootElement.getElementsByTagName(TRIGGER_CONFIG_TAG);
        if (1 != elements.getLength()) {
            log.error("More than one " + TRIGGER_CONFIG_TAG + " elements found in " + configFileName);
            return null;
        }
	String nodeValue = elements.item(0).getFirstChild().getNodeValue();
	log.info("triggerConfig element has: " + nodeValue);
        return elements.item(0).getFirstChild().getNodeValue();

    }

    /**
     * Extract the root element from the xml file.
     * @param configFileName Name of xml file
     * @return root xml element
     * @throws ParserConfigurationException if the xml parser cannot be created
     * @throws SAXException if the xml file cannot be parsed
     * @throws IOException if the xml file cannot be parsed
     */
    private static Element getRootElement(String configFileName)
            throws ParserConfigurationException, SAXException, IOException {

	log.info("Getting root element of xml file: " + configFileName);
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return documentBuilder.parse(configFileName).getDocumentElement();

    }
}
