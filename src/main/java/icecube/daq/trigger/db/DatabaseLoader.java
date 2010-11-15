package icecube.daq.trigger.db;

import java.util.Enumeration;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

import icecube.daq.trigger.db.TriggerDatabaseUtil;

public class DatabaseLoader
{

    // Logging object
    private static final Log log = LogFactory.getLog(DatabaseLoader.class);

    public static void main(String[] args) {

	// Install some logging
	final Logger root = Logger.getRootLogger();
	final Enumeration appenders = root.getAllAppenders();
	if (!appenders.hasMoreElements()) {
	    root.addAppender(new ConsoleAppender(new PatternLayout("%m%n")));
	}

	// Reguire 3 command line arguments
	if (args.length != 3) {
	    log.fatal("Must supply three things: DB config (use DEFAULT), input xml file, output xml file");
	}
	String dbConfig = args[0];
	String xmlFile = args[1];
	String outFile = args[2];

	log.info("DatabaseLoader...");
	log.info("  Database properties: " + dbConfig);
	log.info("  Trigger configuration input xml file: " + xmlFile);
	log.info("  Trigger configuration output xml file: " + outFile);
	log.info(" ");

	// Setup the DB
	TriggerDatabaseUtil dbUtil = TriggerDatabaseUtil.getInstance();
	boolean loaded = false;
	if (dbConfig.equalsIgnoreCase("DEFAULT")) {
	    loaded = dbUtil.loadProperties();
	} else {
	    loaded = dbUtil.loadProperties(dbConfig);
	}
	if (!loaded) {
	    log.fatal("Failed to get DB configuration");
	}

	// Try to insert into DB
	boolean good = dbUtil.importXmlConfig(xmlFile);

	// now export the formatted xml
	dbUtil.exportXmlConfig(outFile);
    }


}
