package icecube.daq.trigger.db;

/**
 * A simple representation of the TriggerId database table.
 */
public class TriggerIdLocal
{

    // Table columns
    public int triggerId;
    public int triggerType;
    public int triggerConfigId;
    public int sourceId;

    // Default constructor
    public TriggerIdLocal() {
	this(0,0,0,0);
    }

    // Full constructor
    public TriggerIdLocal(int triggerId, int triggerType, int triggerConfigId, int sourceId) {
	this.triggerId       = triggerId;
	this.triggerType     = triggerType;
	this.triggerConfigId = triggerConfigId;
	this.sourceId        = sourceId;
    }

    // Override the toString() method to provide a useful dump
    public String toString() {
	return "TriggerId: TriggerId = "       + triggerId +
    	                 " TriggerType = "     + triggerType +
	                 " TriggerConfigId = " + triggerConfigId +
 	                 " SourceId = "        + sourceId;
    }

}
