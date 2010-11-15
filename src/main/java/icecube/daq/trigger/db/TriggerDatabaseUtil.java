package icecube.daq.trigger.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Enumeration;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import icecube.daq.trigger.config.triggers.ObjectFactory;
import icecube.daq.trigger.config.triggers.ActiveTriggers;
import icecube.daq.trigger.config.triggers.TriggerConfigType;
import icecube.daq.trigger.config.triggers.ParameterConfigType;
import icecube.daq.trigger.config.triggers.ReadoutConfigType;

public class TriggerDatabaseUtil
{

    // Logging object
    private static final Log log = LogFactory.getLog(TriggerDatabaseUtil.class);

    // Singleton object
    private static TriggerDatabaseUtil instance = null;

    // Database configuration parameters
    private String driver = "";
    private String url    = "";
    private String user   = "";
    private String passwd = "";
    private boolean gotConfig = false;

    // Default values
    private String DEFAULT_DRIVER = "com.mysql.jdbc.Driver";
    private String DEFAULT_URL    = "jdbc:mysql://localhost:3306/triggers";
    private String DEFAULT_USER   = "trigger";
    private String DEFAULT_PASSWD = "triggerpassword";

    private int currentConfiguration = -1;
    private ActiveTriggers currentTriggers;

    // Private default constructor
    private TriggerDatabaseUtil() {}

    // Public access to singleton object
    public static TriggerDatabaseUtil getInstance() {
	if (instance == null) instance = new TriggerDatabaseUtil();
	return instance;
    }

    // Load the default values
    public boolean loadProperties() {

	log.info("Loading default DB config parameters");

	driver = DEFAULT_DRIVER;
	url    = DEFAULT_URL;
	user   = DEFAULT_USER;
	passwd = DEFAULT_PASSWD;
	gotConfig = true;

	return true;
    }

    // Fill DB config parameters from an xml Properties file
    public boolean loadProperties(String propFilename) {

	log.info("Loading DB config parameters from Properties file: " + propFilename);

	boolean loadOK = false;

	// Create a Properties object
	Properties prop = new Properties();

	// Try to load the xml file
	try {
	    FileInputStream file = new FileInputStream(propFilename);
	    prop.loadFromXML(file);
	    loadOK = true;
	} catch (FileNotFoundException fnfe) {
	    log.error("Properties file not found", fnfe);
	} catch (InvalidPropertiesFormatException ipfe) {
	    log.error("Error in properties file", ipfe);
	} catch (IOException ioe) {
	    log.error("Error loading xml file", ioe);
	}

	// Fill the global variables
	if (loadOK) {
	    driver = prop.getProperty("driver", DEFAULT_DRIVER);
	    url    = prop.getProperty("url", DEFAULT_URL);
	    user   = prop.getProperty("user", DEFAULT_USER);
	    passwd = prop.getProperty("passwd", DEFAULT_PASSWD);
	    gotConfig = true;
	}

	return loadOK;
    }

    // Open a connection to the DB (loadProperties must be called first)
    public Connection openDBConnection() {
 
	Connection connection = null;

	// Make sure that the db configuration was properly loaded
	if (!gotConfig) {
	    log.error("Database configuration was not loaded");
	    return connection;
	}

	// Open it up
	try {

	    // Install the driver
	    //log.debug("Installing driver: " + driver);
	    Class.forName(driver);

	    // Get the connection
	    //log.debug("Attmepting to make connection to " + url + " with " + user + " and " + passwd);
	    connection = DriverManager.getConnection(url, user, passwd);

	    // Set AutoCommit to false
	    connection.setAutoCommit(false);

	    //log.debug("Connection is open");

	} catch (SQLException sqle) {
	    log.error("SQL error establishing connection", sqle);
	} catch (ClassNotFoundException cnfe) {
	    log.error("Error establishing connection", cnfe);
	}

	return connection;
    }

    // Close a connection to the DB
    public boolean closeDBConnection(Connection connection) {

	boolean closed = false;

	try {

	    if (!connection.isClosed()) {
		
		// commit transactions
		connection.commit();

		// close the connection
		connection.close();
	    }

	    // the connection is closed
	    //log.debug("Connection is closed");
	    closed = true;

	} catch (SQLException sqle) {
	    log.error("SQL error closing connection", sqle);
	}

	return closed;
    }

    public boolean importXmlConfig(String fileName) {

	log.info("Parsing " + fileName);
	File file = new File(fileName);

	// setup the DocumentBuilder
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	factory.setNamespaceAware(true);
	DocumentBuilder builder = null;
	try {
	    builder = factory.newDocumentBuilder();
	} catch (ParserConfigurationException e) {
	    log.error("Error creating DocumentBuilder: ", e);
	    return false;
	}

	// parse the xml
	Element elem = null;
	try {
	    elem = builder.parse(file).getDocumentElement();
	} catch (SAXException se) {
	    log.error("Error parsing xml: ", se);
	    return false;
	} catch (IOException ioe) {
	    log.error("Error parsing xml: ", ioe);
	    return false;
	}
	
	// get trigger config objects
	ActiveTriggers activeTriggers = null;
	try {
	    JAXBContext context = JAXBContext.newInstance("icecube.daq.trigger.config.triggers");
	    Unmarshaller unmarshaller = context.createUnmarshaller();
	    activeTriggers = (ActiveTriggers) unmarshaller.unmarshal(elem);
	} catch (JAXBException e) {
	    log.error("Error unmarshalling xml element: ", e);
	    return false;
	}

	// insert into DB
	int configurationId = -1;

	List triggerList = activeTriggers.getTriggerConfig();
	int nTrigs = triggerList.size();
	Integer triggerIds[] = new Integer[nTrigs];

	log.info("There are " + nTrigs + " triggers to process:");

	// iterate over triggers
	for (int t=0; t<nTrigs; t++) {
	    TriggerConfigType trigger = (TriggerConfigType) triggerList.get(t);

	    // get trigger info
	    String triggerName = trigger.getTriggerName();
	    int sourceId = trigger.getSourceId();

	    log.info(" ");
	    log.info("Trigger " + t + ":");
	    log.info("   TriggerName = " + triggerName + " SourceId = " + sourceId);

	    // iterate over parameters
	    List paramList = trigger.getParameterConfig();
	    int nParams = paramList.size();
	    String paramNames[] = new String[nParams];
	    String paramValues[] = new String[nParams];
	    log.info("   Params (" + nParams + "):");
	    for (int i=0; i<nParams; i++) {
		ParameterConfigType param = (ParameterConfigType) paramList.get(i);
		paramNames[i] = param.getParameterName();
		paramValues[i] = param.getParameterValue();
		log.info("      " + paramNames[i] + " -> " + paramValues[i]);
	    }

	    // iterate over the readouts
	    List readoutList = trigger.getReadoutConfig();
	    int nReadouts = readoutList.size();
	    String readoutTypes[] = new String[nReadouts];
	    String readoutOffsetTimes[] = new String[nReadouts];
	    String readoutMinusTimes[] = new String[nReadouts];
	    String readoutPlusTimes[] = new String[nReadouts];
	    log.info("   Readouts (" + nReadouts + "):");
	    for (int i=0; i<nReadouts; i++) {
		ReadoutConfigType readout = (ReadoutConfigType) readoutList.get(i);
		readoutTypes[i] = String.valueOf(readout.getReadoutType());
		readoutOffsetTimes[i] = String.valueOf(readout.getTimeOffset());
		readoutMinusTimes[i] = String.valueOf(readout.getTimeMinus());
		readoutPlusTimes[i] = String.valueOf(readout.getTimePlus());
		log.info("      " + readoutTypes[i] + " from " + readoutOffsetTimes[i] + " - "
			 + readoutMinusTimes[i] + " + " + readoutPlusTimes[i]);
	    }

	    int triggerId = addTriggerId(triggerName, sourceId,
					 paramNames, paramValues,
					 readoutTypes, readoutOffsetTimes, readoutMinusTimes, readoutPlusTimes);
	    log.info("TriggerId for this trigger = " + triggerId);

	    triggerIds[t] = new Integer(triggerId);

	}

	// now see if there is a configurationId that matches this set of triggerIds
	boolean makeNew = false;
	List idList[] = new ArrayList[nTrigs];
	for (int i=0; i<nTrigs; i++) {
	    idList[i] = new ArrayList();
	    Integer triggerId = triggerIds[i];

	    List configurationLocals = TriggerConfigurationTable.findByTriggerId(triggerId);
	    if (configurationLocals.isEmpty()) {
		// this config does not exist, need to create the whole set
		makeNew = true;
		break;
	    } else {
		// this config does exist, get all the id's
		Iterator configurationLocalsIter = configurationLocals.iterator();
		while (configurationLocalsIter.hasNext()) {
		    TriggerConfigurationLocal configurationLocal = (TriggerConfigurationLocal) configurationLocalsIter.next();
		    idList[i].add(new Integer(configurationLocal.configurationId));
		}
	    }
	}

	// if all the triggers have a configuration id, need to see if there is one that contains all
	if (!makeNew) {
	    List commonList = new ArrayList();
	    // loop over all elements of the first list
	    Iterator list0Iter = idList[0].iterator();
	    while (list0Iter.hasNext()) {
		Integer element = (Integer) list0Iter.next();
		int count = 1;

		// now loop over all other lists
		for (int i=1; i<nTrigs; i++) {
		    List list = idList[i];

		    if (list.contains(element)) count++;
		}

		// check to see if count == nTrigs
		if (count == nTrigs) {
		    commonList.add(element);
		}
	    }

	    // check commonList
	    if (commonList.isEmpty()) {
		makeNew = true;
	    } else {
		// need to check if there is an id that references only these triggers
		List finalList = new ArrayList();
		Iterator commonListIter = commonList.iterator();
		while (commonListIter.hasNext()) {
		    Integer element = (Integer) commonListIter.next();

		    // get trigger for this configId
		    List configurationLocals = TriggerConfigurationTable.findByConfigurationId(element.intValue());
		    if (configurationLocals.size() == nTrigs) {
			// this one does not reference other triggers
			finalList.add(element);
		    }
		}

		if (finalList.isEmpty()) {
		    makeNew = true;
		} else {
		    if (finalList.size() > 1)
			log.warn("Multiple configurationId's with the same triggers!");
		    configurationId = ((Integer) finalList.get(0)).intValue();
		    log.info("Found an existing ConfigurationId for this set of triggers: " + configurationId);
		}

	    }
	}

	if (makeNew) {

	    log.info("Making a new ConfigurationId for this set of triggers...");

	    // first we need to know the largest configurationId in use
	    int maxId = Integer.MIN_VALUE;

	    List configurationLocals = TriggerConfigurationTable.findAll();
	    Iterator configurationLocalsIter = configurationLocals.iterator();
	    while (configurationLocalsIter.hasNext()) {
		TriggerConfigurationLocal local = (TriggerConfigurationLocal) configurationLocalsIter.next();
		int id = local.configurationId;
		if (id > maxId) maxId = id;
	    }
	    configurationId = maxId + 1;

	    // now create the local objects
	    for (int i=0; i<nTrigs; i++) {
		int triggerId = triggerIds[i].intValue();

		TriggerConfigurationLocal local = new TriggerConfigurationLocal(-1, configurationId, triggerId);
		int primaryKey = TriggerConfigurationTable.insert(local);
	    }

	    log.info("   new ConfigurationId = " + configurationId);

	}
	
	return selectConfiguration(configurationId);
    }

    public int addTriggerId(String triggerName, int sourceId,
			    String[] paramNames, String[] paramValues,
			    String[] readoutTypes, String[] offsetTimes,
			    String[] minusTimes, String[] plusTimes) {

	int triggerId = -1;

	int triggerType = addTriggerName(triggerName);
	int triggerConfigId = addTriggerConfig(paramNames, paramValues,
					       readoutTypes, offsetTimes, minusTimes, plusTimes);

	TriggerIdLocal local = new TriggerIdLocal(-1, triggerType, triggerConfigId, sourceId);
	triggerId = TriggerIdTable.insert(local);
	log.info("TriggerId " + triggerId + " has (TriggerType,TriggerConfigId,SourceId) = (" + triggerType + ","
		 + triggerConfigId + "," + sourceId + ")");

	return triggerId;
    }

    public int addTriggerName(String triggerName) {

	TriggerNameLocal local = new TriggerNameLocal(-1, triggerName);
	int triggerType = TriggerNameTable.insert(local);

	return triggerType;
    }

    public int addTriggerConfig(String[] paramNames, String[] paramValues,
				String[] readoutTypes, String[] offsetTimes,
				String[] minusTimes, String[] plusTimes) {

	int triggerConfigId = -1;

	int paramConfigId = addParameterConfig(paramNames, paramValues);	
	int readoutConfigId = addReadoutConfig(readoutTypes, offsetTimes, minusTimes, plusTimes);

	// if both paramConfigId and readoutConfigId are -1, so is triggerConfigId
	if ((paramConfigId < 0) && (readoutConfigId < 0)) {
	    log.info("TriggerConfigId -1 has (ParamConfigId,ReadoutConfigId) = (" + paramConfigId + "," + readoutConfigId + ")");
	    return -1;
	}

	// see if there is a triggerConfig for this pair of paramConfigId and readoutConfigId
	TriggerConfigLocal local = new TriggerConfigLocal(-1, paramConfigId, readoutConfigId);
	triggerConfigId = TriggerConfigTable.insert(local);

	log.info("TriggerConfigId " + triggerConfigId + " has (ParamConfigId,ReadoutConfigId) = (" + paramConfigId 
		 + "," + readoutConfigId + ")");
	return triggerConfigId;
    }

    public int addParameterConfig(String[] paramNames, String[] paramValues) {
	int paramConfigId = -1;

	int nParams = paramNames.length;
	Integer paramIds[] = new Integer[nParams];
	Integer paramValueIds[] = new Integer[nParams];
	for (int i=0; i<nParams; i++) {
	    paramIds[i] = new Integer(addParameter(paramNames[i]));
	    paramValueIds[i] = new Integer(addParameterValue(paramValues[i]));
	}

	// now check if there is a paramConfigId for this set of parameters
	boolean makeNew = false;
	List idList[] = new ArrayList[nParams];
	for (int i=0; i<nParams; i++) {
	    idList[i] = new ArrayList();
	    int paramId = paramIds[i].intValue();
	    int paramValueId = paramValueIds[i].intValue();

	    List paramConfigLocals = ParamConfigTable.findByParamIdParamValueId(paramId, paramValueId);
	    if (paramConfigLocals.isEmpty()) {
		// this config does not exist, need to create the whole set
		makeNew = true;
		break;
	    } else {
		// this config does exist, get all the id's
		Iterator paramConfigLocalsIter = paramConfigLocals.iterator();
		while (paramConfigLocalsIter.hasNext()) {
		    ParamConfigLocal paramConfigLocal = (ParamConfigLocal) paramConfigLocalsIter.next();
		    idList[i].add(new Integer(paramConfigLocal.paramConfigId));
		}
	    }
	}

	// if all the parameters have a paramConfigId, need to see if there is one that contains all
	if ((!makeNew) && (nParams>0)) {
	    List commonList = new ArrayList();
	    // loop over all elements of the first list
	    Iterator list0Iter = idList[0].iterator();
	    while (list0Iter.hasNext()) {
		Integer element = (Integer) list0Iter.next();
		int count = 1;

		// now loop over all other lists
		for (int i=1; i<nParams; i++) {
		    List list = idList[i];
		    if (list.contains(element)) count++;
		}

		// check to see if count == nParams
		if (count == nParams) {
		    // this element is common to all lists
		    commonList.add(element);
		}
	    }

	    // check common list
	    if (commonList.isEmpty()) {
		makeNew = true;
	    } else {
		// need to check if there is a configId that references only these parameters
		List finalList = new ArrayList();
		Iterator commonListIter = commonList.iterator();
		while (commonListIter.hasNext()) {
		    Integer element = (Integer) commonListIter.next();

		    // get parameters for this configId
		    List paramConfigLocals = ParamConfigTable.findByParamConfigId(element.intValue());
		    if (paramConfigLocals.size() == nParams) {
			// this one does not reference other parameters
			finalList.add(element);
		    }
		}

		if (finalList.isEmpty()) {
		    makeNew = true;
		} else {
		    if (finalList.size() > 1)
			log.warn("Multiple paramConfigId's with the same parameters!");
		    paramConfigId = ((Integer) finalList.get(0)).intValue();
		    log.info("Found an existing paramConfigId for this set of parameters: " + paramConfigId);
		}

	    }
	}

	if (makeNew) {

	    // first we need to know the largest paramConfigId in use
	    int maxId = Integer.MIN_VALUE;

	    List paramConfigLocals = ParamConfigTable.findAll();
	    Iterator paramConfigLocalsIter = paramConfigLocals.iterator();
	    while (paramConfigLocalsIter.hasNext()) {
		ParamConfigLocal local = (ParamConfigLocal) paramConfigLocalsIter.next();
		int id = local.paramConfigId;
		if (id > maxId) maxId = id;
	    }
	    paramConfigId = maxId + 1;

	    // now create the local objects
	    for (int i=0; i<nParams; i++) {
		int paramId = paramIds[i].intValue();
		int paramValueId = paramValueIds[i].intValue();

		ParamConfigLocal local = new ParamConfigLocal(-1, paramConfigId, paramId, paramValueId);
		int primaryKey = ParamConfigTable.insert(local);
	    }
	}

	return paramConfigId;
    }

    public int addParameter(String paramName) {

	ParamLocal local = new ParamLocal(-1, paramName);
	int paramId = ParamTable.insert(local);

	return paramId;
    }

    public int addParameterValue(String paramValue) {

	ParamValueLocal local = new ParamValueLocal(-1, paramValue);
	int paramValueId = ParamValueTable.insert(local);

	return paramValueId;
    }

    public int addReadoutConfig(String[] types, String[] offsetTimes, String[] minusTimes, String[] plusTimes) {
	int readoutConfigId = -1;

	int nReadouts = types.length;
	Integer readoutIds[] = new Integer[nReadouts];
	for (int i=0; i<nReadouts; i++) {
	    readoutIds[i] = new Integer(addReadout(Integer.parseInt(types[i]), Integer.parseInt(offsetTimes[i]),
						   Integer.parseInt(minusTimes[i]), Integer.parseInt(plusTimes[i])));
	}

	// now check if there is a readoutConfigId for this set of readouts
	boolean makeNew = false;
	List idList[] = new ArrayList[nReadouts];
	for (int i=0; i<nReadouts; i++) {
	    idList[i] = new ArrayList();
	    int readoutId = readoutIds[i].intValue();

	    List readoutConfigLocals = ReadoutConfigTable.findByReadoutId(readoutId);
	    if (readoutConfigLocals.isEmpty()) {
		// this config does not exist, need to create the whole set
		makeNew = true;
		break;
	    } else {
		// this config does exist, get all the id's
		Iterator readoutConfigLocalsIter = readoutConfigLocals.iterator();
		while (readoutConfigLocalsIter.hasNext()) {
		    ReadoutConfigLocal readoutConfigLocal = (ReadoutConfigLocal) readoutConfigLocalsIter.next();
		    idList[i].add(new Integer(readoutConfigLocal.readoutConfigId));
		}
	    }
	}

	// if all the readouts have a readoutConfigId, need to see if there is one that contains all
	if ((!makeNew) && (nReadouts>0)) {
	    List commonList = new ArrayList();
	    // loop over all elements of the first list
	    Iterator list0Iter = idList[0].iterator();
	    while (list0Iter.hasNext()) {
		Integer element = (Integer) list0Iter.next();
		int count = 1;

		// now loop over all other lists
		for (int i=1; i<nReadouts; i++) {
		    List list = idList[i];
		    if (list.contains(element)) count++;
		}

		// check to see if count == nReadouts
		if (count == nReadouts) {
		    // this element is common to all lists
		    commonList.add(element);
		}
	    }

	    // check common list
	    if (commonList.isEmpty()) {
		makeNew = true;
	    } else {
		// need to check if there is a configId that references only these readouts
		List finalList = new ArrayList();
		Iterator commonListIter = commonList.iterator();
		while (commonListIter.hasNext()) {
		    Integer element = (Integer) commonListIter.next();

		    // get parameters for this configId
		    List readoutConfigLocals = ReadoutConfigTable.findByReadoutConfigId(element.intValue());
		    if (readoutConfigLocals.size() == nReadouts) {
			// this one does not reference other readouts
			finalList.add(element);
		    }
		}

		if (finalList.isEmpty()) {
		    makeNew = true;
		} else {
		    if (finalList.size() > 1)
			log.warn("Multiple readoutConfigId's with the same readouts!");
		    readoutConfigId = ((Integer) finalList.get(0)).intValue();
		    log.info("Found an existing readoutConfigId for this set of readouts: " + readoutConfigId);
		}

	    }
	}

	if (makeNew) {

	    // first we need to know the largest readoutConfigId in use
	    int maxId = Integer.MIN_VALUE;

	    List readoutConfigLocals = ReadoutConfigTable.findAll();
	    Iterator readoutConfigLocalsIter = readoutConfigLocals.iterator();
	    while (readoutConfigLocalsIter.hasNext()) {
		ReadoutConfigLocal local = (ReadoutConfigLocal) readoutConfigLocalsIter.next();
		int id = local.readoutConfigId;
		if (id > maxId) maxId = id;
	    }
	    readoutConfigId = maxId + 1;

	    // now create the local objects
	    for (int i=0; i<nReadouts; i++) {
		int readoutId = readoutIds[i].intValue();

		ReadoutConfigLocal local = new ReadoutConfigLocal(-1, readoutConfigId, readoutId);
		int primaryKey = ReadoutConfigTable.insert(local);
	    }
	}

	return readoutConfigId;
    }

    public int addReadout(int readoutType, int timeOffset, int timeMinus, int timePlus) {

	ReadoutParametersLocal local = new ReadoutParametersLocal(-1, readoutType, timeOffset, timeMinus, timePlus);
	int readoutId = ReadoutParametersTable.insert(local);

	return readoutId;
    }

    public void dumpReadoutParameters() {
	ReadoutParametersTable.dump();
    }

    public void dumpReadoutConfig() {
	ReadoutConfigTable.dump();
    }

    public void dumpParam() {
	ParamTable.dump();
    }

    public void dumpParamValue() {
	ParamValueTable.dump();
    }

    public void dumpParamConfig() {
	ParamConfigTable.dump();
    }

    public void dumpTriggerConfig() {
	TriggerConfigTable.dump();
    }

    public void dumpTriggerName() {
	TriggerNameTable.dump();
    }

    public void dumpTriggerId() {
	TriggerIdTable.dump();
    }

    public void dumpTriggerConfiguration() {
	TriggerConfigurationTable.dump();
    }

    public void exportXmlConfig(String fileName) {

	FileWriter writer = null;
	try {
	    writer = new FileWriter(fileName);
	} catch (IOException e) {
	    log.fatal("Error opening output file for writing: ", e);
	}

	try {
	    ObjectFactory objectFactory = new ObjectFactory();
	    Marshaller marshaller = objectFactory.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    marshaller.marshal(currentTriggers, writer);
	} catch (JAXBException e) {
	    log.fatal("Error marshalling triggers to xml: ", e);
	}
    }
    
    public boolean selectConfiguration(int configurationId) {

	if (configurationId == currentConfiguration) return true;

	// Get list of triggerId's for this configuration
	List configurationLocals = TriggerConfigurationTable.findByConfigurationId(configurationId);
	if (configurationLocals.isEmpty()) {
	    log.error("No TriggerIds for ConfigurationId = " + configurationId);
	    return false;
	}

	// Create trigger objects
	ActiveTriggers triggers = null;
	ObjectFactory factory = new ObjectFactory();
	try {
	    triggers = factory.createActiveTriggers();
	} catch (JAXBException e) {
	    log.error("Error creating ActiveTriggers object: ", e);
	    return false;
	}
	
	// Set the configurationId
	triggers.setConfigurationId(configurationId);

	// Now iterate over the triggers in this configuration
	List triggerList = triggers.getTriggerConfig();
	Iterator configurationLocalsIter = configurationLocals.iterator();
	while (configurationLocalsIter.hasNext()) {
	    TriggerConfigurationLocal configurationLocal 
		= (TriggerConfigurationLocal) configurationLocalsIter.next();

	    // create the trigger object for this triggerId
	    TriggerConfigType trigger = null;
	    try {
		trigger = factory.createTriggerConfigType();
	    } catch (JAXBException e) {
		log.error("Error creating Trigger object: ", e);
		return false;
	    }

	    // get the triggerId and set it
	    int triggerId = configurationLocal.triggerId;
	    trigger.setTriggerId(triggerId);

	    // now get info from the TriggerId table
	    TriggerIdLocal triggerIdLocal = TriggerIdTable.findByTriggerId(triggerId);

	    // set the sourceId
	    trigger.setSourceId(triggerIdLocal.sourceId);

	    // get the triggerType and set it
	    int triggerType = triggerIdLocal.triggerType;
	    trigger.setTriggerType(triggerType);

	    // look up the triggerName
	    TriggerNameLocal triggerNameLocal = TriggerNameTable.findByTriggerType(triggerType);
	    trigger.setTriggerName(triggerNameLocal.triggerName);

	    // get the triggerConfigId and set it
	    int triggerConfigId = triggerIdLocal.triggerConfigId;
	    trigger.setTriggerConfigId(triggerConfigId);

	    // next look up the paramConfigId and readoutConfigId for this triggerConfigId
	    int paramConfigId = -1;  // initiaze them
	    int readoutConfigId = -1;
	    TriggerConfigLocal triggerConfigLocal = TriggerConfigTable.findByTriggerConfigId(triggerConfigId);
	    if (triggerConfigLocal != null) {
		paramConfigId = triggerConfigLocal.paramConfigId;
		readoutConfigId = triggerConfigLocal.readoutConfigId;
	    }

	    // Now deal with parameters
	    List paramConfigLocals = null;
	    if (paramConfigId >= 0) {
		paramConfigLocals = ParamConfigTable.findByParamConfigId(paramConfigId);
	    }
	    if (paramConfigLocals != null) {
		// iterate over the parameters for this trigger
		List parameterList = trigger.getParameterConfig();
		Iterator paramConfigLocalsIter = paramConfigLocals.iterator();
		while (paramConfigLocalsIter.hasNext()) {
		    ParamConfigLocal paramConfigLocal = (ParamConfigLocal) paramConfigLocalsIter.next();

		    // get Param
		    int paramId = paramConfigLocal.paramId;
		    ParamLocal paramLocal = ParamTable.findByParamId(paramId);

		    // get ParamValue
		    int paramValueId = paramConfigLocal.paramValueId;
		    ParamValueLocal paramValueLocal = ParamValueTable.findByParamValueId(paramValueId);

		    // create a Parameter object
		    ParameterConfigType parameter = null;
		    try {
			parameter = factory.createParameterConfigType();
		    } catch (JAXBException e) {
			log.error("Error creating a Parameter object: ", e);
			return false;
		    }

		    // now fill it
		    parameter.setParamId(paramId);
		    parameter.setParameterName(paramLocal.paramName);
		    parameter.setParameterValue(paramValueLocal.paramValue);

		    // add it to the list of parameters
		    parameterList.add(parameter);
		}
	    }

	    // Same thing, but with readouts
	    List readoutConfigLocals = null;
	    if (readoutConfigId >= 0) {
		readoutConfigLocals = ReadoutConfigTable.findByReadoutConfigId(readoutConfigId);
	    }
	    if (readoutConfigLocals != null) {
		// iterate over the readouts for this trigger
		List readoutList = trigger.getReadoutConfig();
		Iterator readoutConfigLocalsIter = readoutConfigLocals.iterator();
		while (readoutConfigLocalsIter.hasNext()) {
		    ReadoutConfigLocal readoutConfigLocal = (ReadoutConfigLocal) readoutConfigLocalsIter.next();

		    // get Readout
		    int readoutId = readoutConfigLocal.readoutId;
		    ReadoutParametersLocal readoutParametersLocal = ReadoutParametersTable.findByReadoutId(readoutId);

		    // create a Readout object
		    ReadoutConfigType readout = null;
		    try {
			readout = factory.createReadoutConfigType();
		    } catch (JAXBException e) {
			log.error("Error creating a Readout object: ", e);
			return false;
		    }

		    // now fill it
		    readout.setReadoutId(readoutId);
		    readout.setReadoutType(readoutParametersLocal.readoutType);
		    readout.setTimeOffset(readoutParametersLocal.timeOffset);
		    readout.setTimeMinus(readoutParametersLocal.timeMinus);
		    readout.setTimePlus(readoutParametersLocal.timePlus);

		    // add it to the list of readouts
		    readoutList.add(readout);
		}
	    }

	    // finally add the trigger to the list
	    triggerList.add(trigger);

	}

	// Update the current info
	currentConfiguration = configurationId;
	currentTriggers = triggers;

	return true;
    }

    public static void main(String[] args) {

	// Install some logging
	final Logger root = Logger.getRootLogger();
	final Enumeration appenders = root.getAllAppenders();
	if (!appenders.hasMoreElements()) {
	    root.addAppender(new ConsoleAppender(new PatternLayout("%m%n")));
	}

	// Load the DB properties
	TriggerDatabaseUtil dbUtil = TriggerDatabaseUtil.getInstance();
	boolean loaded = false;
	if (args.length > 0) {
	    // args[0] should be the name of a Properties file
	    loaded = dbUtil.loadProperties(args[0]);
	} else {
	    // use default values (localhost)
	    loaded = dbUtil.loadProperties();
	}

	// Test each table in turn
	if (loaded) {
	    /*
	     * For each:
	     *  1) Dump the table
	     *  2) Insert an existing row
	     *  3) Insert a new row
	     *  4) Dump the table
	     */

	    // ReadoutParameters
	    List rcList = ReadoutParametersTable.findAll();
	    Iterator rcIter = rcList.iterator();
	    while (rcIter.hasNext()) {
		ReadoutParametersLocal rcLocal = (ReadoutParametersLocal) rcIter.next();
		log.info("ReadoutParametersLocal BEFORE:\t" + rcLocal);
	    }

	    ReadoutParametersLocal test1 = new ReadoutParametersLocal(9,1,0,-8000,8000);
	    int pk1 = ReadoutParametersTable.insert(test1);
	    if (pk1 == 9) 
		log.info("This row exists " + pk1 + ": " + test1);
	    else
		log.info("ERROR: This row should exist " + pk1 + ": " + test1);

	    ReadoutParametersLocal test2 = new ReadoutParametersLocal(9,0,0,0,0);
	    int pk2 = ReadoutParametersTable.insert(test2);
	    if (pk2 == 9) 
		log.info("ERROR: This row should not exist " + pk2 + ": " + test2);
	    else
		log.info("This row does not exist " + pk2 + ": " + test2);

	    rcList = ReadoutParametersTable.findAll();
	    rcIter = rcList.iterator();
	    while (rcIter.hasNext()) {
		ReadoutParametersLocal rcLocal = (ReadoutParametersLocal) rcIter.next();
		log.info("ReadoutParametersLocal AFTER:\t" + rcLocal);
	    }



	    // ReadoutConfig
	    rcList = ReadoutConfigTable.findAll();
	    rcIter = rcList.iterator();
	    while (rcIter.hasNext()) {
		ReadoutConfigLocal rcLocal = (ReadoutConfigLocal) rcIter.next();
		log.info("ReadoutConfigLocal BEFORE:\t" + rcLocal);
	    }

	    ReadoutConfigLocal test3 = new ReadoutConfigLocal(9,4,2);
	    int pk3 = ReadoutConfigTable.insert(test3);
	    if (pk3 == 9) 
		log.info("This row exists " + pk3 + ": " + test3);
	    else
		log.info("ERROR: This row should exist " + pk3 + ": " + test3);

	    ReadoutConfigLocal test4 = new ReadoutConfigLocal(9,100,100);
	    int pk4 = ReadoutConfigTable.insert(test4);
	    if (pk4 == 9) 
		log.info("ERROR: This row should not exist " + pk4 + ": " + test4);
	    else
		log.info("This row does not exist " + pk4 + ": " + test4);

	    rcList = ReadoutConfigTable.findAll();
	    rcIter = rcList.iterator();
	    while (rcIter.hasNext()) {
		ReadoutConfigLocal rcLocal = (ReadoutConfigLocal) rcIter.next();
		log.info("ReadoutConfigLocal AFTER:\t" + rcLocal);
	    }


	    // Param
	    rcList = ParamTable.findAll();
	    rcIter = rcList.iterator();
	    while (rcIter.hasNext()) {
		ParamLocal rcLocal = (ParamLocal) rcIter.next();
		log.info("ParamLocal BEFORE:\t" + rcLocal);
	    }

	    ParamLocal test5 = new ParamLocal(9,"sourceId2");
	    int pk5 = ParamTable.insert(test5);
	    if (pk5 == 9) 
		log.info("This row exists " + pk5 + ": " + test5);
	    else
		log.info("ERROR: This row should exist " + pk5 + ": " + test5);

	    ParamLocal test6 = new ParamLocal(9,"crappiness");
	    int pk6 = ParamTable.insert(test6);
	    if (pk6 == 9) 
		log.info("ERROR: This row should not exist " + pk6 + ": " + test6);
	    else
		log.info("This row does not exist " + pk6 + ": " + test6);

	    rcList = ParamTable.findAll();
	    rcIter = rcList.iterator();
	    while (rcIter.hasNext()) {
		ParamLocal rcLocal = (ParamLocal) rcIter.next();
		log.info("ParamLocal AFTER:\t" + rcLocal);
	    }



	} else {
	    log.error("Error loading properties!");
	}

    }

}
