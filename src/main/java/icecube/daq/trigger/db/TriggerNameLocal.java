package icecube.daq.trigger.db;

/**
 * A simple representation of the TriggerName database table.
 */
public class TriggerNameLocal
{

    // Table columns
    public int triggerType;
    public String triggerName;

    // Default constructor
    public TriggerNameLocal() {
	this(0,"");
    }

    // Full constructor
    public TriggerNameLocal(int triggerType, String triggerName) {
	this.triggerType = triggerType;
	this.triggerName = triggerName;
    }

    // Override the toString() method to provide a useful dump
    public String toString() {
	return "TriggerName: TriggerType = " + triggerType +
	                   " TriggerName = " + triggerName;
    }

}
