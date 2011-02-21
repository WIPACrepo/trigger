package icecube.daq.trigger.db;

import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

import icecube.daq.trigger.db.TriggerDatabaseUtil;

public class DatabaseDumper
{

    // Logging object
    private static final Log log = LogFactory.getLog(DatabaseDumper.class);

    public static void main(String[] args) {

	// Install some logging
	final Logger root = Logger.getRootLogger();
	final Enumeration appenders = root.getAllAppenders();
	if (!appenders.hasMoreElements()) {
	    root.addAppender(new ConsoleAppender(new PatternLayout("%m%n")));
	}

	// Reguire 1 command line arguments
	if (args.length != 1) {
	    log.fatal("Must supply DB config (use DEFAULT)");
	}
	String dbConfig = args[0];

	log.info("DatabaseDumper...");
	log.info("  Database properties: " + dbConfig);
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


	log.info(" ");
	dbUtil.dumpReadoutParameters();
	log.info(" ");
	dbUtil.dumpReadoutConfig();
	log.info(" ");
	dbUtil.dumpParam();
	log.info(" ");
	dbUtil.dumpParamValue();
	log.info(" ");
	dbUtil.dumpParamConfig();
	log.info(" ");
	dbUtil.dumpTriggerConfig();
	log.info(" ");
	dbUtil.dumpTriggerName();
	log.info(" ");
	dbUtil.dumpTriggerId();
	log.info(" ");
	dbUtil.dumpTriggerConfiguration();

    }


}
