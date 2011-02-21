package icecube.daq.trigger.db;

/**
 * A simple representation of the ParamConfig database table.
 */
public class ParamConfigLocal
{

    // Table columns
    public int primaryKey;
    public int paramConfigId;
    public int paramId;
    public int paramValueId;

    // Default constructor
    public ParamConfigLocal() {
	this(0,0,0,0);
    }

    // Full constructor
    public ParamConfigLocal(int primaryKey, int paramConfigId, int paramId, int paramValueId) {
	this.primaryKey    = primaryKey;
	this.paramConfigId = paramConfigId;
	this.paramId       = paramId;
	this.paramValueId  = paramValueId;
    }

    // Override the toString() method to provide a useful dump
    public String toString() {
	return "ParamConfig: PrimaryKey = "    + primaryKey +
    	                   " ParamConfigId = " + paramConfigId +
	                   " ParamId = "       + paramId +
	                   " ParamValueId = "  + paramValueId;
    }

}
