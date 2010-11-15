package icecube.daq.trigger.db;

/**
 * A simple representation of the ReadoutConfig database table.
 */
public class ReadoutConfigLocal
{

    // Table columns
    public int primaryKey;
    public int readoutConfigId;
    public int readoutId;

    // Default constructor
    public ReadoutConfigLocal() {
	this(0,0,0);
    }

    // Full constructor
    public ReadoutConfigLocal(int primaryKey, int readoutConfigId, int readoutId) {
	this.primaryKey      = primaryKey;
	this.readoutConfigId = readoutConfigId;
	this.readoutId       = readoutId;
    }

    // Override the toString() method to provide a useful dump
    public String toString() {
	return "ReadoutConfig: PrimaryKey = "      + primaryKey +
    	                     "\tReadoutConfigId = " + readoutConfigId +
	                     "\tReadoutId = "       + readoutId;
    }

}
