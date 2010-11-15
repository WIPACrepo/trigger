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

public class ParamConfigTable
{

    // Logging object
    private static final Log log = LogFactory.getLog(ParamConfigTable.class);

    // Table structure
    private static final String TABLE_NAME      = "ParamConfig";
    private static final String PRIMARY_KEY     = "PrimaryKey";
    private static final String PARAM_CONFIG_ID = "ParamConfigId";
    private static final String PARAM_ID        = "ParamId";
    private static final String PARAM_VALUE_ID  = "ParamValueId";

    // SQL queries
    private static final String FIND_BY_PRIMARY_KEY = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + PRIMARY_KEY + " = ?";

    private static final String FIND_BY_PARAM_CONFIG_ID_PARAM_ID_PARAM_VALUE_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE (" + PARAM_CONFIG_ID + " = ?) AND (" +
	             PARAM_ID        + " = ?) AND (" +
	             PARAM_VALUE_ID  + " = ?)";

    private static final String FIND_ALL =
	"SELECT * FROM " + TABLE_NAME;

    private static final String FIND_BY_PARAM_ID_PARAM_VALUE_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE (" + PARAM_ID       + " = ?) AND (" +
	             PARAM_VALUE_ID + " = ?)";

    private static final String FIND_BY_PARAM_CONFIG_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + PARAM_CONFIG_ID + " = ?";

    private static final String FIND_BY_PARAM_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + PARAM_ID + " = ?";

    private static final String FIND_BY_PARAM_VALUE_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + PARAM_VALUE_ID + " = ?";

    private static final String INSERT =
	"INSERT INTO " + TABLE_NAME + "(" +
 	 PRIMARY_KEY + "," +
	 PARAM_CONFIG_ID + "," +
	 PARAM_ID + "," +
	 PARAM_VALUE_ID + ") VALUES (?,?,?,?)";

    private static final String MAX_PK =
	"SELECT MAX(" + PRIMARY_KEY + ") FROM " + TABLE_NAME;

    // private constructor
    private ParamConfigTable() {}

    public static ParamConfigLocal findByPrimaryKey(int key) {

	ParamConfigLocal local = null;

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

    public static ParamConfigLocal findByParamConfigIdParamIdParamValueId(int configId, int id, int valueId) {

	ParamConfigLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PARAM_CONFIG_ID_PARAM_ID_PARAM_VALUE_ID);
	    statement.setInt(1, configId);
	    statement.setInt(2, id);
	    statement.setInt(3, valueId);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + PARAM_CONFIG_ID + " = " + configId + " " +
			             PARAM_ID        + " = " + id + " " +
			             PARAM_VALUE_ID  + " = " + valueId);

	} catch (SQLException sqle) {
	    log.error("Error in findByParamConfigIdParamIdParamValueId query", sqle);
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
		ParamConfigLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findAll query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByParamIdParamValueId(int id, int valueId) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PARAM_ID_PARAM_VALUE_ID);
	    statement.setInt(1, id);
	    statement.setInt(2, valueId);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		ParamConfigLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByParamIdParamValueId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByParamConfigId(int configId) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PARAM_CONFIG_ID);
	    statement.setInt(1,configId);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		ParamConfigLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByParamConfigId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByParamId(int id) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PARAM_ID);
	    statement.setInt(1, id);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		ParamConfigLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByParamId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByParamValueId(int valueId) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PARAM_VALUE_ID);
	    statement.setInt(1, valueId);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		ParamConfigLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByParamValueId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static int insert(ParamConfigLocal row) {

	// See if this row exists
	ParamConfigLocal temp = findByParamConfigIdParamIdParamValueId(row.paramConfigId, row.paramId, row.paramValueId);

	if (temp != null) {
	    // row exists
	    log.info("ParamConfigId, ParamId, ParamValueId triplet (" + row.paramConfigId + ", " + row.paramId + ", " + row.paramValueId
		     + ") exists with PrimaryKey = " + temp.primaryKey);
	    return temp.primaryKey;
	}
	log.info("ParamConfigId, ParamId, ParamValueId triplet (" + row.paramConfigId + ", " + row.paramId + ", " + row.paramValueId
		     + ") does not exist...");

	// this is a new row
	int key = nextPrimaryKey();

	// do insert
	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(INSERT);
	    statement.setInt(1, key);
	    statement.setInt(2, row.paramConfigId);
	    statement.setInt(3, row.paramId);
	    statement.setInt(4, row.paramValueId);

	    statement.executeUpdate();

	} catch (SQLException sqle) {
	    log.error("Error in insert", sqle);
	}
	closeConnection(conn);

	log.info("   new PrimaryKey = " + key);
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

    private static ParamConfigLocal createLocal(ResultSet results, int row)
	throws SQLException {

	results.absolute(row);
	int key      = results.getInt(PRIMARY_KEY);
	int configId = results.getInt(PARAM_CONFIG_ID);
	int id       = results.getInt(PARAM_ID);
	int valueId  = results.getInt(PARAM_VALUE_ID);

	return new ParamConfigLocal(key, configId, id, valueId);
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
	log.info("---------------");
	log.info("| ParamConfig |");
	log.info("-------------------------------------------------------");
	log.info("| PrimaryKey | ParamConfigId | ParamId | ParamValueId |");
	log.info("-------------------------------------------------------");
	while (localsIter.hasNext()) {
	    ParamConfigLocal local = (ParamConfigLocal) localsIter.next();
	    log.info("| " + local.primaryKey + " | " + local.paramConfigId + " | " + local.paramId + " | " + local.paramValueId + " |");
	}
	log.info("-------------------------------------------------------");
    }

}
