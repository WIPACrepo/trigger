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

public class ParamValueTable
{

    // Logging object
    private static final Log log = LogFactory.getLog(ParamValueTable.class);

    // Table structure
    private static final String TABLE_NAME     = "ParamValue";
    private static final String PARAM_VALUE_ID = "ParamValueId";
    private static final String PARAM_VALUE    = "ParamValue";

    // SQL queries
    private static final String FIND_BY_PARAM_VALUE_ID = 
	"SELECT * FROM " + TABLE_NAME +
	" WHERE " + PARAM_VALUE_ID + " = ?";

    private static final String FIND_BY_PARAM_VALUE =
	"SELECT * FROM " + TABLE_NAME +
	" WHERE " + PARAM_VALUE + " = ?";

    private static final String FIND_ALL =
	"SELECT * FROM " + TABLE_NAME;

    private static final String INSERT =
	"INSERT INTO " + TABLE_NAME + "(" +
 	 PARAM_VALUE_ID + "," +
	 PARAM_VALUE + ") VALUES (?,?)";

    private static final String MAX_PK =
	"SELECT MAX(" + PARAM_VALUE_ID + ") FROM " + TABLE_NAME;

    // private constructor
    private ParamValueTable() {}

    public static ParamValueLocal findByPrimaryKey(int key) {
	return findByParamValueId(key);
    }

    public static ParamValueLocal findByParamValueId(int id) {

	ParamValueLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PARAM_VALUE_ID);
	    statement.setInt(1, id);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + PARAM_VALUE_ID + " = " + id);

	} catch (SQLException sqle) {
	    log.error("Error in findByParamValueId query", sqle);
	}
	closeConnection(conn);

	return local;
    }

    public static ParamValueLocal findByParamValue(String value) {

	ParamValueLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PARAM_VALUE);
	    statement.setString(1, value);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + PARAM_VALUE + " = " + value);

	} catch (SQLException sqle) {
	    log.error("Error in findByParamValue query", sqle);
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
                ParamValueLocal local = createLocal(results, row);
                localList.add(local);
            }

        } catch (SQLException sqle) {
            log.error("Error in findAll query", sqle);
        }
        closeConnection(conn);

        return localList;
    }

    public static int insert(ParamValueLocal row) {

        // See if this row exists
        ParamValueLocal temp = findByParamValue(row.paramValue);

        if (temp != null) {
            // Row exists
	    log.info("ParamValue " + row.paramValue + " exists with ParamValueId = " + temp.paramValueId);
            return temp.paramValueId;
        } 
	log.info("ParamValue " + row.paramValue + " does not exist...");

        // This is a new row, first get the next primary key
        int key = nextPrimaryKey();

        // Then do the insert
        Connection conn = null;
        try {
            conn = openConnection();

            PreparedStatement statement = conn.prepareStatement(INSERT);
            statement.setInt(1, key);
            statement.setString(2, row.paramValue);

            statement.executeUpdate();

        } catch (SQLException sqle) {
            log.error("Error in insert", sqle);
        }
        closeConnection(conn);
        
	log.info("   new ParamValueId = " + key);
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

    private static ParamValueLocal createLocal(ResultSet results, int row) 
        throws SQLException {

        results.absolute(row);
        int id       = results.getInt(PARAM_VALUE_ID);
        String value = results.getString(PARAM_VALUE);
        
        return new ParamValueLocal(id, value);
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
	log.info("--------------");
	log.info("| ParamValue |");
	log.info("-----------------------------");
	log.info("| ParamValueId | ParamValue |");
	log.info("-----------------------------");
	while (localsIter.hasNext()) {
	    ParamValueLocal local = (ParamValueLocal) localsIter.next();
	    log.info("| " + local.paramValueId + " | " + local.paramValue + " |");
	}
	log.info("-----------------------------");
    }

}
