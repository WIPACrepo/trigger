package icecube.daq.trigger.db;

/**
 * A simple representation of the TriggerConfiguration database table.
 */
public class TriggerConfigurationLocal
{

    // Table columns
    public int primaryKey;
    public int configurationId;
    public int triggerId;

    // Default constructor
    public TriggerConfigurationLocal() {
	this(0,0,0);
    }

    // Full constructor
    public TriggerConfigurationLocal(int primaryKey, int configurationId, int triggerId) {
	this.primaryKey      = primaryKey;
	this.configurationId = configurationId;
	this.triggerId       = triggerId;
    }

    // Override the toString() method to provide a useful dump
    public String toString() {
	return "TriggerConfiguration: PrimaryKey = "      + primaryKey +
    	                            " ConfigurationId = " + configurationId +
	                            " TriggerId = "       + triggerId;
    }

}
