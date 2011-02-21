package icecube.daq.trigger.db;

/**
 * A simple representation of the TriggerConfig database table.
 */
public class TriggerConfigLocal
{

    // Table columns
    public int triggerConfigId;
    public int paramConfigId;
    public int readoutConfigId;

    // Default constructor
    public TriggerConfigLocal() {
	this(0,0,0);
    }

    // Full constructor
    public TriggerConfigLocal(int triggerConfigId, int paramConfigId, int readoutConfigId) {
	this.triggerConfigId = triggerConfigId;
	this.paramConfigId   = paramConfigId;
	this.readoutConfigId = readoutConfigId;
    }

    // Override the toString() method to provide a useful dump
    public String toString() {
	return "TriggerConfig: TriggerConfigId = " + triggerConfigId +
    	                     " ParamConfigId = "   + paramConfigId +
	                     " ReadoutConfigId = " + readoutConfigId;
    }

}
