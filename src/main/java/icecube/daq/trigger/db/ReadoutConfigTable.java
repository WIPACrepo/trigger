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

public class ReadoutConfigTable
{

    // Logging object
    private static final Log log = LogFactory.getLog(ReadoutConfigTable.class);

    // Table structure
    private static final String TABLE_NAME        = "ReadoutConfig";
    private static final String PRIMARY_KEY       = "PrimaryKey";
    private static final String READOUT_CONFIG_ID = "ReadoutConfigId";
    private static final String READOUT_ID        = "ReadoutId";

    // SQL queries
    private static final String FIND_BY_PRIMARY_KEY = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + PRIMARY_KEY + " = ?";

    private static final String FIND_BY_READOUT_CONFIG_ID_READOUT_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE (" + READOUT_CONFIG_ID + " = ?) AND (" +
	             READOUT_ID        + " = ?)";

    private static final String FIND_ALL =
	"SELECT * FROM " + TABLE_NAME;

    private static final String FIND_BY_READOUT_CONFIG_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + READOUT_CONFIG_ID + " = ?";

    private static final String FIND_BY_READOUT_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + READOUT_ID + " = ?";

    private static final String INSERT =
	"INSERT INTO " + TABLE_NAME + "(" +
 	 PRIMARY_KEY + "," +
	 READOUT_CONFIG_ID + "," +
	 READOUT_ID + ") VALUES (?,?,?)";

    private static final String MAX_PK =
	"SELECT MAX(" + PRIMARY_KEY + ") FROM " + TABLE_NAME;

    // private constructor
    private ReadoutConfigTable() {}

    public static ReadoutConfigLocal findByPrimaryKey(int key) {

	ReadoutConfigLocal local = null;

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

    public static ReadoutConfigLocal findByReadoutConfigIdReadoutId(int configId, int id) {

	ReadoutConfigLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_READOUT_CONFIG_ID_READOUT_ID);
	    statement.setInt(1, configId);
	    statement.setInt(2, id);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + READOUT_CONFIG_ID + " = " + configId + " " +
			             READOUT_ID        + " = " + id);

	} catch (SQLException sqle) {
	    log.error("Error in findByReadoutConfigIdReadoutId query", sqle);
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
		ReadoutConfigLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findAll query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByReadoutConfigId(int configId) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_READOUT_CONFIG_ID);
	    statement.setInt(1, configId);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		ReadoutConfigLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByReadoutConfigId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByReadoutId(int id) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_READOUT_ID);
	    statement.setInt(1, id);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		ReadoutConfigLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByReadoutId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static int insert(ReadoutConfigLocal row) {

	// See if this row exists
	ReadoutConfigLocal temp = findByReadoutConfigIdReadoutId(row.readoutConfigId, row.readoutId);

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
	    statement.setInt(2, row.readoutConfigId);
	    statement.setInt(3, row.readoutId);

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

    private static ReadoutConfigLocal createLocal(ResultSet results, int row)
	throws SQLException {

	results.absolute(row);
	int key      = results.getInt(PRIMARY_KEY);
	int configId = results.getInt(READOUT_CONFIG_ID);
	int id       = results.getInt(READOUT_ID);

	return new ReadoutConfigLocal(key, configId, id);
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
	log.info("| ReadoutConfig |");
	log.info("--------------------------------------------");
	log.info("| PrimaryKey | ReadoutConfigId | ReadoutId |");
	log.info("--------------------------------------------");
	while (localsIter.hasNext()) {
	    ReadoutConfigLocal local = (ReadoutConfigLocal) localsIter.next();
	    log.info("| " + local.primaryKey + " | " + local.readoutConfigId + " | " + local.readoutId + " |");
	}
	log.info("--------------------------------------------");
    }

}
