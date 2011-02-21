package icecube.daq.trigger.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ReadoutParametersTable
{

    // Logging object
    private static final Log log = LogFactory.getLog(ReadoutParametersTable.class);

    // Table structure
    private static final String TABLE_NAME   = "ReadoutParameters";
    private static final String READOUT_ID   = "ReadoutId";
    private static final String READOUT_TYPE = "ReadoutType";
    private static final String TIME_OFFSET  = "TimeOffset";
    private static final String TIME_MINUS   = "TimeMinus";
    private static final String TIME_PLUS    = "TimePlus";

    private static final String FIND_BY_READOUT_ID =
	"SELECT * FROM " + TABLE_NAME +
	" WHERE " + READOUT_ID + " = ?";

    private static final String FIND_BY_READOUT_TYPE_TIME_OFFSET_TIME_MINUS_TIME_PLUS =
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE (" + READOUT_TYPE + " = ?) AND (" +
	             TIME_OFFSET  + " = ?) AND (" +
 	             TIME_MINUS   + " = ?) AND (" +
	             TIME_PLUS    + " = ?)";

    private static final String FIND_ALL =
	"SELECT * FROM " + TABLE_NAME;

    private static final String FIND_BY_READOUT_TYPE =
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + READOUT_TYPE + " = ?";

    private static final String FIND_BY_TIME_OFFSET =
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + TIME_OFFSET + " = ?";

    private static final String FIND_BY_TIME_MINUS =
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + TIME_MINUS + " = ?";

    private static final String FIND_BY_TIME_PLUS =
	"SELECT * FROM " + TABLE_NAME + 
	" WHERE " + TIME_PLUS + " = ?";

    private static final String INSERT =
	"INSERT INTO " + TABLE_NAME + "(" + 
	 READOUT_ID + "," +
	 READOUT_TYPE + "," +
	 TIME_OFFSET + "," +
	 TIME_MINUS + "," +
	 TIME_PLUS + ") VALUES (?,?,?,?,?)";

    private static final String MAX_PK =
	"SELECT MAX(" + READOUT_ID + ") FROM " + TABLE_NAME;

    private ReadoutParametersTable() {}

    public static ReadoutParametersLocal findByPrimaryKey(int key) {
	return findByReadoutId(key);
    }

    public static ReadoutParametersLocal findByReadoutId(int id) {

	ReadoutParametersLocal local = null;
	
	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_READOUT_ID);
	    statement.setInt(1, id);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next()) 
		log.error("More than 1 row found in " + TABLE_NAME + 
			  " with " + READOUT_ID + " = " + id);

	} catch (SQLException sqle) {
	    log.error("Error in findByReadoutId query", sqle);
	}
	closeConnection(conn);

	return local;
    }

    public static ReadoutParametersLocal findByReadoutTypeTimeOffsetTimeMinusTimePlus(int type,
										      int offset,
										      int minus,
										      int plus) {

	ReadoutParametersLocal local = null;

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_READOUT_TYPE_TIME_OFFSET_TIME_MINUS_TIME_PLUS);
	    statement.setInt(1, type);
	    statement.setInt(2, offset);
	    statement.setInt(3, minus);
	    statement.setInt(4, plus);

	    ResultSet results = statement.executeQuery();

	    if (results.first()) {
		int row = results.getRow();
		local = createLocal(results, row);
	    }

	    if (results.next()) 
		log.error("More than 1 row found in " + TABLE_NAME + 
			  " with " + READOUT_TYPE + " = " + type + " " +
			             TIME_OFFSET  + " = " + offset + " " +
			             TIME_MINUS   + " = " + minus + " " +
			             TIME_PLUS    + " = " + plus);

	} catch (SQLException sqle) {
	    log.error("Error in findByReadoutTypeTimeOffsetTimeMinusTimePlus query", sqle);
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
		ReadoutParametersLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findAll query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByReadoutType(int type) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_READOUT_TYPE);
	    statement.setInt(1, type);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		ReadoutParametersLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByReadoutType query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByTimeOffset(int offset) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_TIME_OFFSET);
	    statement.setInt(1, offset);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		ReadoutParametersLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByTimeOffset query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByTimeMinus(int minus) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_TIME_MINUS);
	    statement.setInt(1, minus);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		ReadoutParametersLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByTimeMinus query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static List findByTimePlus(int plus) {

	List localList = new ArrayList();

	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(FIND_BY_TIME_PLUS);
	    statement.setInt(1, plus);

	    ResultSet results = statement.executeQuery();

	    while (results.next()) {
		int row = results.getRow();
		ReadoutParametersLocal local = createLocal(results, row);
		localList.add(local);
	    }

	} catch (SQLException sqle) {
	    log.error("Error in findByTimePlus query", sqle);
	}
	closeConnection(conn);

	return localList;
    }

    public static int insert(ReadoutParametersLocal row) {

	// See if this row already exists
	ReadoutParametersLocal temp = findByReadoutTypeTimeOffsetTimeMinusTimePlus(row.readoutType,
										   row.timeOffset,
										   row.timeMinus,
										   row.timePlus);
	if (temp != null) {
	    // Row exists
	    log.info("ReadoutType, TimeOffset, TimeMinus, TimePlus quad (" + row.readoutType + ", " + row.timeOffset + ", "
		     + row.timeMinus + ", " + row.timePlus + ") exists with ReadoutId = " + temp.readoutId);
	    return temp.readoutId;
	} 
	log.info("ReadoutType, TimeOffset, TimeMinus, TimePlus quad (" + row.readoutType + ", " + row.timeOffset + ", "
		 + row.timeMinus + ", " + row.timePlus + ") does not exist...");

	// This is a new row, first get the next primary key
	int key = nextPrimaryKey();

	// Then do the insert
	Connection conn = null;
	try {
	    conn = openConnection();

	    PreparedStatement statement = conn.prepareStatement(INSERT);
	    statement.setInt(1, key);
	    statement.setInt(2, row.readoutType);
	    statement.setInt(3, row.timeOffset);
	    statement.setInt(4, row.timeMinus);
	    statement.setInt(5, row.timePlus);

	    statement.executeUpdate();

	} catch (SQLException sqle) {
	    log.error("Error in insert", sqle);
	}
	closeConnection(conn);
	
	log.info("   new ReadoutId = " + key);
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

    private static ReadoutParametersLocal createLocal(ResultSet results, int row) 
	throws SQLException {

	results.absolute(row);
	int id     = results.getInt(READOUT_ID);
	int type   = results.getInt(READOUT_TYPE);
	int offset = results.getInt(TIME_OFFSET);
	int minus  = results.getInt(TIME_MINUS);
	int plus   = results.getInt(TIME_PLUS);
	
	return new ReadoutParametersLocal(id, type, offset, minus, plus);
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
	log.info("---------------------");
	log.info("| ReadoutParameters |");
	log.info("---------------------------------------------------------------");
	log.info("| ReadoutId | ReadoutType | TimeOffset | TimeMinus | TimePlus |");
	log.info("---------------------------------------------------------------");
	while (localsIter.hasNext()) {
	    ReadoutParametersLocal local = (ReadoutParametersLocal) localsIter.next();
	    log.info("| " + local.readoutId + " | " + local.readoutType + " | " + local.timeOffset + " | " + local.timeMinus
		     + " | " + local.timePlus + " |");
	}
	log.info("---------------------------------------------------------------");
    }

}
