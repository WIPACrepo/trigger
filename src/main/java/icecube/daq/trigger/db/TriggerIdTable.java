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

public class TriggerIdTable
{

    // Logging object
    private static final Log log = LogFactory.getLog(TriggerIdTable.class);

    // Table structure
    private static final String TABLE_NAME        = "TriggerId";
    private static final String TRIGGER_ID        = "TriggerId";
    private static final String TRIGGER_TYPE      = "TriggerType";
    private static final String TRIGGER_CONFIG_ID = "TriggerConfigId";
    private static final String SOURCE_ID         = "SourceId";

    // SQL queries
    private static final String FIND_BY_TRIGGER_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + TRIGGER_ID + " = ?";

    private static final String FIND_BY_TRIGGER_TYPE_TRIGGER_CONFIG_ID_SOURCE_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE (" + TRIGGER_TYPE      + " = ?) AND (" +
	             TRIGGER_CONFIG_ID + " = ?) AND (" +
	             SOURCE_ID         + " = ?)";

    private static final String FIND_ALL =
	"SELECT * FROM " + TABLE_NAME;

    private static final String FIND_BY_TRIGGER_TYPE = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + TRIGGER_TYPE + " = ?";

    private static final String FIND_BY_TRIGGER_CONFIG_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + TRIGGER_CONFIG_ID + " = ?";

    private static final String FIND_BY_SOURCE_ID = 
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + SOURCE_ID + " = ?";

    private static final String INSERT =
	"INSERT INTO " + TABLE_NAME + "(" +
 	 TRIGGER_ID + "," +
	 TRIGGER_TYPE + "," +
	 TRIGGER_CONFIG_ID + "," +
	 SOURCE_ID + ") VALUES (?,?,?,?)";

    private static final String MAX_PK =
	"SELECT MAX(" + TRIGGER_ID + ") FROM " + TABLE_NAME;

    // private constructor
    private TriggerIdTable() {}

    public static TriggerIdLocal findByPrimaryKey(int key) {
	return findByTriggerId(key);
    }

    public static TriggerIdLocal findByTriggerId(int id) {

	TriggerIdLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_TRIGGER_ID);
	    statement.setInt(1, id);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + TRIGGER_ID + " = " + id);

	} catch (SQLException sqle) {
	    log.error("Error in findByTriggerId query", sqle);
	}
	closeConnection(conn);

	return local;
    }

    public static TriggerIdLocal findByTriggerTypeTriggerConfigIdSourceId(int type, int config, int source) {

	TriggerIdLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_TRIGGER_TYPE_TRIGGER_CONFIG_ID_SOURCE_ID);
	    statement.setInt(1, type);
	    statement.setInt(2, config);
	    statement.setInt(3, source);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + TRIGGER_TYPE      + " = " + type + " " +
			             TRIGGER_CONFIG_ID + " = " + config + " " +
			             SOURCE_ID         + " = " + source);

	} catch (SQLException sqle) {
	    log.error("Error in findByTriggerTypeTriggerConfigIdSourceId query", sqle);
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
		TriggerIdLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findAll query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByTriggerType(int type) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_TRIGGER_TYPE);
	    statement.setInt(1,type);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		TriggerIdLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByTriggerType query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByTriggerConfigId(int config) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_TRIGGER_CONFIG_ID);
	    statement.setInt(1, config);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		TriggerIdLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByTriggerConfigId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findBySourceId(int source) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_SOURCE_ID);
	    statement.setInt(1, source);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		TriggerIdLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findBySourceId query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static int insert(TriggerIdLocal row) {

	// See if this row exists
	TriggerIdLocal temp = findByTriggerTypeTriggerConfigIdSourceId(row.triggerType, row.triggerConfigId, row.sourceId);

	if (temp != null) {
	    // row exists
	    return temp.triggerId;
	}

	// this is a new row
	int key = nextPrimaryKey();

	// do insert
	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(INSERT);
	    statement.setInt(1, key);
	    statement.setInt(2, row.triggerType);
	    statement.setInt(3, row.triggerConfigId);
	    statement.setInt(4, row.sourceId);

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

    private static TriggerIdLocal createLocal(ResultSet results, int row)
	throws SQLException {

	results.absolute(row);
	int id     = results.getInt(TRIGGER_ID);
	int type   = results.getInt(TRIGGER_TYPE);
	int config = results.getInt(TRIGGER_CONFIG_ID);
	int source = results.getInt(SOURCE_ID);

	return new TriggerIdLocal(id, type, config, source);
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
	log.info("-------------");
	log.info("| TriggerId |");
	log.info("--------------------------------------------------------");
	log.info("| TriggerId | TriggerType | TriggerConfigId | SourceId |");
	log.info("--------------------------------------------------------");
	while (localsIter.hasNext()) {
	    TriggerIdLocal local = (TriggerIdLocal) localsIter.next();
	    log.info("| " + local.triggerId + " | " + local.triggerType + " | " + local.triggerConfigId + " | " + local.sourceId + " |");
	}
	log.info("--------------------------------------------------------");
    }

}
