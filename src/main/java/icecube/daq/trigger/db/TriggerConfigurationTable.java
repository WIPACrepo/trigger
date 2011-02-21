package icecube.daq.trigger.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TriggerConfigurationTable
{

    // Logging object
    private static final Log log = LogFactory.getLog(TriggerConfigurationTable.class);

    // Table structure
    private static final String TABLE_NAME       = "TriggerConfiguration";
    private static final String PRIMARY_KEY      = "PrimaryKey";
    private static final String CONFIGURATION_ID = "ConfigurationId";
    private static final String TRIGGER_ID       = "TriggerId";

    // SQL queries
    private static final String FIND_BY_PRIMARY_KEY = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + PRIMARY_KEY + " = ?";

    private static final String FIND_BY_CONFIGURATION_ID_TRIGGER_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE (" + CONFIGURATION_ID + " = ?) AND (" +
	             TRIGGER_ID       + " = ?)";

    private static final String FIND_ALL =
	"SELECT * FROM " + TABLE_NAME;

    private static final String FIND_BY_CONFIGURATION_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + CONFIGURATION_ID + " = ?";

    private static final String FIND_BY_TRIGGER_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + TRIGGER_ID + " = ?";

    private static final String INSERT =
	"INSERT INTO " + TABLE_NAME + "(" +
 	 PRIMARY_KEY + "," +
	 CONFIGURATION_ID + "," +
	 TRIGGER_ID + ") VALUES (?,?,?)";

    private static final String MAX_PK =
	"SELECT MAX(" + PRIMARY_KEY + ") FROM " + TABLE_NAME;

    // private constructor
    private TriggerConfigurationTable() {}

    public static TriggerConfigurationLocal findByPrimaryKey(int key) {

	TriggerConfigurationLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PRIMARY_KEY);
	    statement.setInt(1, key);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + PRIMARY_KEY + " = " + key);

	} catch (SQLException sqle) {
	    log.error("Error in findByPrimaryKey query", sqle);
	}
	closeConnection(conn);

	return local;
    }

    public static TriggerConfigurationLocal findByConfigurationIdTriggerId(int configId, int id) {

	TriggerConfigurationLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_CONFIGURATION_ID_TRIGGER_ID);
	    statement.setInt(1, configId);
	    statement.setInt(2, id);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + CONFIGURATION_ID + " = " + configId + " " +
			             TRIGGER_ID       + " = " + id);

	} catch (SQLException sqle) {
	    log.error("Error in findByConfigurationIdTriggerId query", sqle);
	}
	closeConnection(conn);

	return local;
    }

    public static List findAll() {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_ALL);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		TriggerConfigurationLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findAll query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByConfigurationId(int configId) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_CONFIGURATION_ID);
	    statement.setInt(1, configId);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		TriggerConfigurationLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByConfigurationId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByTriggerId(int id) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_TRIGGER_ID);
	    statement.setInt(1, id);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		TriggerConfigurationLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByTriggerId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static int insert(TriggerConfigurationLocal row) {

	// See if this row exists
	TriggerConfigurationLocal temp = findByConfigurationIdTriggerId(row.configurationId, row.triggerId);

	if (temp != null) {
	    // row exists
	    return temp.primaryKey;
	}

	// this is a new row
	int key = nextPrimaryKey();

	// do insert
	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(INSERT);
	    statement.setInt(1, key);
	    statement.setInt(2, row.configurationId);
	    statement.setInt(3, row.triggerId);

	    statement.executeUpdate();

	} catch (SQLException sqle) {
	    log.error("Error in insert", sqle);
	}
	closeConnection(conn);

	return key;
    }

    private static Connection openConnection() {
	TriggerDatabaseUtil dbUtil = TriggerDatabaseUtil.getInstance();
	return dbUtil.openDBConnection();
    }

    private static void closeConnection(Connection conn) {
	TriggerDatabaseUtil dbUtil = TriggerDatabaseUtil.getInstance();
	dbUtil.closeDBConnection(conn);
    }

    private static TriggerConfigurationLocal createLocal(ResultSet results, int row)
	throws SQLException {

	results.absolute(row);
	int key      = results.getInt(PRIMARY_KEY);
	int configId = results.getInt(CONFIGURATION_ID);
	int id       = results.getInt(TRIGGER_ID);

	return new TriggerConfigurationLocal(key, configId, id);
    }

    private static int nextPrimaryKey() {

	int next = -1;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(MAX_PK);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int max = results.getInt(1);
		next = max + 1;
	    } else {
		log.error("Error getting max primary key");
		return -1;
	    }

	} catch (SQLException sqle) {
	    log.error("Error in nextPrimaryKey", sqle);
	}
	closeConnection(conn);

	return next;
    }

    public static void dump() {
	List locals = findAll();
	Iterator localsIter = locals.iterator();
	log.info("------------------------");
	log.info("| TriggerConfiguration |");
	log.info("--------------------------------------------");
	log.info("| PrimaryKey | ConfigurationId | TriggerId |");
	log.info("--------------------------------------------");
	while (localsIter.hasNext()) {
	    TriggerConfigurationLocal local = (TriggerConfigurationLocal) localsIter.next();
	    log.info("| " + local.primaryKey + " | " + local.configurationId + " | " + local.triggerId + " |");
	}
	log.info("--------------------------------------------");
    }

}
