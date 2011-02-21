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

public class TriggerConfigTable
{

    // Logging object
    private static final Log log = LogFactory.getLog(TriggerConfigTable.class);

    // Table structure
    private static final String TABLE_NAME        = "TriggerConfig";
    private static final String TRIGGER_CONFIG_ID = "TriggerConfigId";
    private static final String PARAM_CONFIG_ID   = "ParamConfigId";
    private static final String READOUT_CONFIG_ID = "ReadoutConfigId";

    // SQL queries
    private static final String FIND_BY_TRIGGER_CONFIG_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + TRIGGER_CONFIG_ID + " = ?";

    private static final String FIND_BY_PARAM_CONFIG_ID_READOUT_CONFIG_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE (" + PARAM_CONFIG_ID   + " = ?) AND (" +
	             READOUT_CONFIG_ID + " = ?)";

    private static final String FIND_ALL =
	"SELECT * FROM " + TABLE_NAME;

    private static final String FIND_BY_PARAM_CONFIG_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + PARAM_CONFIG_ID + " = ?";

    private static final String FIND_BY_READOUT_CONFIG_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + READOUT_CONFIG_ID + " = ?";

    private static final String INSERT =
	"INSERT INTO " + TABLE_NAME + "(" +
 	 TRIGGER_CONFIG_ID + "," +
	 PARAM_CONFIG_ID + "," +
	 READOUT_CONFIG_ID + ") VALUES (?,?,?)";

    private static final String MAX_PK =
	"SELECT MAX(" + TRIGGER_CONFIG_ID + ") FROM " + TABLE_NAME;

    // private constructor
    private TriggerConfigTable() {}

    public static TriggerConfigLocal findByPrimaryKey(int key) {
	return findByTriggerConfigId(key);
    }

    public static TriggerConfigLocal findByTriggerConfigId(int id) {
	TriggerConfigLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_TRIGGER_CONFIG_ID);
	    statement.setInt(1, id);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + TRIGGER_CONFIG_ID + " = " + id);

	} catch (SQLException sqle) {
	    log.error("Error in findByTriggerConfigId query", sqle);
	}
	closeConnection(conn);

	return local;
    }

    public static TriggerConfigLocal findByParamConfigIdReadoutConfigId(int paramId, int readoutId) {

	TriggerConfigLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PARAM_CONFIG_ID_READOUT_CONFIG_ID);
	    statement.setInt(1, paramId);
	    statement.setInt(2, readoutId);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + PARAM_CONFIG_ID   + " = " + paramId + " " +
			             READOUT_CONFIG_ID + " = " + readoutId);

	} catch (SQLException sqle) {
	    log.error("Error in findByParamConfigIdReadoutConfigId query", sqle);
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
		TriggerConfigLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findAll query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByParamConfigId(int paramId) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PARAM_CONFIG_ID);
	    statement.setInt(1, paramId);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		TriggerConfigLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByParamConfigId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByReadoutConfigId(int readoutId) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_READOUT_CONFIG_ID);
	    statement.setInt(1, readoutId);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		TriggerConfigLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByReadoutConfigId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static int insert(TriggerConfigLocal row) {

	// See if this row exists
	TriggerConfigLocal temp = findByParamConfigIdReadoutConfigId(row.paramConfigId, row.readoutConfigId);

	if (temp != null) {
	    // row exists
	    return temp.triggerConfigId;
	}

	// this is a new row
	int key = nextPrimaryKey();

	// do insert
	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(INSERT);
	    statement.setInt(1, key);
	    statement.setInt(2, row.paramConfigId);
	    statement.setInt(3, row.readoutConfigId);

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

    private static TriggerConfigLocal createLocal(ResultSet results, int row)
	throws SQLException {

	results.absolute(row);
	int configId  = results.getInt(TRIGGER_CONFIG_ID);
	int paramId   = results.getInt(PARAM_CONFIG_ID);
	int readoutId = results.getInt(READOUT_CONFIG_ID);

	return new TriggerConfigLocal(configId, paramId, readoutId);
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
	log.info("-----------------");
	log.info("| TriggerConfig |");
	log.info("-----------------------------------------------------");
	log.info("| TriggerConfigId | ParamConfigId | ReadoutConfigId |");
	log.info("-----------------------------------------------------");
	while (localsIter.hasNext()) {
	    TriggerConfigLocal local = (TriggerConfigLocal) localsIter.next();
	    log.info("| " + local.triggerConfigId + " | " + local.paramConfigId + " | " + local.readoutConfigId + " |");
	}
	log.info("-----------------------------------------------------");
    }

}
