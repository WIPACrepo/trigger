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

public class ParamTable
{

    // Logging object
    private static final Log log = LogFactory.getLog(ParamTable.class);

    // Table structure
    private static final String TABLE_NAME = "Param";
    private static final String PARAM_ID   = "ParamId";
    private static final String PARAM_NAME = "ParamName";

    // SQL queries
    private static final String FIND_BY_PARAM_ID = 
	"SELECT * FROM " + TABLE_NAME +
	" WHERE " + PARAM_ID + " = ?";

    private static final String FIND_BY_PARAM_NAME =
	"SELECT * FROM " + TABLE_NAME +
	" WHERE " + PARAM_NAME + " = ?";

    private static final String FIND_ALL =
	"SELECT * FROM " + TABLE_NAME;

    private static final String INSERT =
	"INSERT INTO " + TABLE_NAME + "(" +
 	 PARAM_ID + "," +
	 PARAM_NAME + ") VALUES (?,?)";

    private static final String MAX_PK =
	"SELECT MAX(" + PARAM_ID + ") FROM " + TABLE_NAME;

    // private constructor
    private ParamTable() {}

    public static ParamLocal findByPrimaryKey(int key) {
	return findByParamId(key);
    }

    public static ParamLocal findByParamId(int id) {

	ParamLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PARAM_ID);
	    statement.setInt(1, id);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + PARAM_ID + " = " + id);

	} catch (SQLException sqle) {
	    log.error("Error in findByParamId query", sqle);
	}
	closeConnection(conn);

	return local;
    }

    public static ParamLocal findByParamName(String name) {

	ParamLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_PARAM_NAME);
	    statement.setString(1, name);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next())
		log.error("More than 1 row found in " + TABLE_NAME +
			  " with " + PARAM_NAME + " = " + name);

	} catch (SQLException sqle) {
	    log.error("Error in findByParamName query", sqle);
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
                ParamLocal local = createLocal(results, row);
                localList.add(local);
            }

        } catch (SQLException sqle) {
            log.error("Error in findAll query", sqle);
        }
        closeConnection(conn);

        return localList;
    }

    public static int insert(ParamLocal row) {

        // See if this row exists
        ParamLocal temp = findByParamName(row.paramName);

        if (temp != null) {
            // Row exists
	    log.info("ParamName " + row.paramName + " exists with ParamId = " + temp.paramId);
            return temp.paramId;
        }
	log.info("ParamName " + row.paramName + " does not exist...");

        // This is a new row, first get the next primary key
        int key = nextPrimaryKey();

        // Then do the insert
        Connection conn = null;
        try {
            conn = openConnection();

            PreparedStatement statement = conn.prepareStatement(INSERT);
            statement.setInt(1, key);
            statement.setString(2, row.paramName);

            statement.executeUpdate();

        } catch (SQLException sqle) {
            log.error("Error in insert", sqle);
        }
        closeConnection(conn);
        
	log.info("   new ParamId = " + key);
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

    private static ParamLocal createLocal(ResultSet results, int row) 
        throws SQLException {

        results.absolute(row);
        int id      = results.getInt(PARAM_ID);
        String name = results.getString(PARAM_NAME);
        
        return new ParamLocal(id, name);
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
	log.info("---------");
	log.info("| Param |");
	log.info("-----------------------");
	log.info("| ParamId | ParamName |");
	log.info("-----------------------");
	while (localsIter.hasNext()) {
	    ParamLocal local = (ParamLocal) localsIter.next();
	    log.info("| " + local.paramId + " | " + local.paramName + " |");
	}
	log.info("-----------------------");
    }

}
