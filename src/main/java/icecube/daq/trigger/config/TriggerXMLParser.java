/*
 * class: TriggerXMLParser
 *
 * Version $Id: TriggerXMLParser.java 2629 2008-02-11 05:48:36Z dglo $
 *
 * Date: March 30 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.config;

import icecube.daq.trigger.config.triggers.ActiveTriggers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * This class creates jaxb trigger configuration objects from an xml file.
 *
 * @version $Id: TriggerXMLParser.java 2629 2008-02-11 05:48:36Z dglo $
 * @author pat
 */
public class TriggerXMLParser
{

    /**
     * Log object for this class.
     */
    private static final Log log = LogFactory.getLog(TriggerXMLParser.class);

    /**
     * Object for parsing xml into DOM.
     */
    private static DocumentBuilder documentBuilder = null;

    /**
     * Create a trigger configuration object from an xml DOM document.
     * @param document xml DOM document
     * @return jaxb trigger configuration object
     */
    public static ActiveTriggers parse(Document document) {
        try {
            return unmarshal(document.getDocumentElement());
        } catch (JAXBException e) {
            log.error("Error unmarshalling xml element", e);
            return null;
        }
    }

    /**
     * Create a trigger configuration object from an xml file name.
     * @param fileName name of xml file
     * @return jaxb trigger configuration object
     */
    public static ActiveTriggers parse(String fileName) {

        if (null == documentBuilder) {
            try {
                createDocumentBuilder();
            } catch (ParserConfigurationException pce) {
                log.error("Error creating DocumentBuilder", pce);
                return null;
            }
        }

        Element element = null;
        try {
            element = documentBuilder.parse(fileName).getDocumentElement();
        } catch (SAXException se) {
            log.error("Error parsing file " + fileName, se);
            return null;
        } catch (IOException ioe) {
            log.error("Error parsing file " + fileName, ioe);
            return null;
        }

        try {
            return unmarshal(element);
        } catch (JAXBException e) {
            log.error("Error unmarshalling xml element", e);
            return null;
        }

    }

    /**
     * Create a trigger configuration object from an xml file.
     * @param file xml file
     * @return jaxb trigger configuration object
     */
    public static ActiveTriggers parse(File file) {

        if (null == documentBuilder) {
            try {
                createDocumentBuilder();
            } catch (ParserConfigurationException pce) {
                log.error("Error creating DocumentBuilder", pce);
                return null;
            }
        }

        Element element = null;
        try {
            element = documentBuilder.parse(file).getDocumentElement();
        } catch (SAXException se) {
            log.error("Error parsing file " + file, se);
            return null;
        } catch (IOException ioe) {
            log.error("Error parsing file " + file, ioe);
            return null;
        }

        try {
            return unmarshal(element);
        } catch (JAXBException e) {
            log.error("Error unmarshalling xml element", e);
            return null;
        }

    }

    /**
     * Create a trigger configuration object from an input stream.
     * @param stream input stream tied to xml file
     * @return jaxb trigger configuration object
     */
    public static ActiveTriggers parse(InputStream stream) {

        if (null == documentBuilder) {
            try {
                createDocumentBuilder();
            } catch (ParserConfigurationException pce) {
                log.error("Error creating DocumentBuilder", pce);
                return null;
            }
        }

        Element element = null;
        try {
            element = documentBuilder.parse(stream).getDocumentElement();
        } catch (SAXException se) {
            log.error("Error parsing file " + stream, se);
            return null;
        } catch (IOException ioe) {
            log.error("Error parsing file " + stream, ioe);
            return null;
        }

        try {
            return unmarshal(element);
        } catch (JAXBException e) {
            log.error("Error unmarshalling xml element", e);
            return null;
        }

    }

    /**
     * Create the DOM document builder object.
     * @throws ParserConfigurationException
     */
    private static void createDocumentBuilder()
            throws ParserConfigurationException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        documentBuilder = factory.newDocumentBuilder();

    }

    /**
     * Turn an xml DOM element into a jaxb class.
     * @param element xml DOM element generated from parsing xml file
     * @return jaxb class ActiveTriggers
     * @throws JAXBException
     */
    private static ActiveTriggers unmarshal(Element element)
            throws JAXBException {

        final JAXBContext context = JAXBContext.newInstance("icecube.daq.trigger.config.triggers");
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        return (ActiveTriggers) unmarshaller.unmarshal(element);

    }

}
