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

public class TriggerNameTable
{

    // Logging object
    private static final Log log = LogFactory.getLog(TriggerNameTable.class);

    // Table structure
    private static final String TABLE_NAME   = "TriggerName";
    private static final String TRIGGER_TYPE = "TriggerType";
    private static final String TRIGGER_NAME = "TriggerName";

    // SQL queries
    private static final String FIND_BY_TRIGGER_TYPE = 
	"SELECT * FROM " + TABLE_NAME +
	" WHERE " + TRIGGER_TYPE + " = ?";

    private static final String FIND_BY_TRIGGER_NAME =
	"SELECT * FROM " + TABLE_NAME +
	" WHERE " + TRIGGER_NAME + " = ?";

    private static final String FIND_ALL =
	"SELECT * FROM " + TABLE_NAME;

    private static final String INSERT =
	"INSERT INTO " + TABLE_NAME + "(" +
 	 TRIGGER_TYPE + "," +
	 TRIGGER_NAME + ") VALUES (?,?)";

    private static final String MAX_PK =
	"SELECT MAX(" + TRIGGER_TYPE + ") FROM " + TABLE_NAME;

    // private constructor
    private TriggerNameTable() {}

    public static TriggerNameLocal findByPrimaryKey(int key) {
	return findByTriggerType(key);
    }

    public static TriggerNameLocal findByTriggerType(int type) {

	TriggerNameLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_TRIGGER_TYPE);
	    statement.setInt(1, type);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + TRIGGER_TYPE + " = " + type);

	} catch (SQLException sqle) {
	    log.error("Error in findByTriggerType query", sqle);
	}
	closeConnection(conn);

	return local;
    }

    public static TriggerNameLocal findByTriggerName(String name) {

	TriggerNameLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_TRIGGER_NAME);
	    statement.setString(1, name);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + TRIGGER_NAME + " = " + name);

	} catch (SQLException sqle) {
	    log.error("Error in findByTriggerName query", sqle);
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
                TriggerNameLocal local = createLocal(results, row);
                localList.add(local);
            }

        } catch (SQLException sqle) {
            log.error("Error in findAll query", sqle);
        }
        closeConnection(conn);

        return localList;
    }

    public static int insert(TriggerNameLocal row) {

        // See if this row exists
        TriggerNameLocal temp = findByTriggerName(row.triggerName);

        if (temp != null) {
            // Row exists
	    log.info("TriggerName " + row.triggerName + " exists with TriggerType = " + temp.triggerType);
            return temp.triggerType;
        }
	log.info("TriggerName " + row.triggerName + " does not exist...");

        // This is a new row, first get the next primary key
        int key = nextPrimaryKey();

        // Then do the insert
        Connection conn = null;
        try {
            conn = openConnection();

            PreparedStatement statement = conn.prepareStatement(INSERT);
            statement.setInt(1, key);
            statement.setString(2, row.triggerName);

            statement.executeUpdate();

        } catch (SQLException sqle) {
            log.error("Error in insert", sqle);
        }
        closeConnection(conn);
        
	log.info("   new TriggerType = " + key);
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

    private static TriggerNameLocal createLocal(ResultSet results, int row) 
        throws SQLException {

        results.absolute(row);
        int type    = results.getInt(TRIGGER_TYPE);
        String name = results.getString(TRIGGER_NAME);
        
        return new TriggerNameLocal(type, name);
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
	log.info("| TriggerName |");
	log.info("-----------------------------");
	log.info("| TriggerType | TriggerName |");
	log.info("-----------------------------");
	while (localsIter.hasNext()) {
	    TriggerNameLocal local = (TriggerNameLocal) localsIter.next();
	    log.info("| " + local.triggerType + " | " + local.triggerName + " |");
	}
	log.info("-----------------------------");
    }

}
