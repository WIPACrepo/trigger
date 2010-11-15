package icecube.daq.trigger.db;

/**
 * A simple representation of the Param database table.
 */
public class ParamLocal
{

    // Table columns
    public int paramId;
    public String paramName;

    // Default constructor
    public ParamLocal() {
	this(0,"");
    }

    // Full constructor
    public ParamLocal(int paramId, String paramName) {
	this.paramId   = paramId;
	this.paramName = paramName;
    }

    // Override the toString() method to provide a useful dump
    public String toString() {
	return "Param: ParamId = "   + paramId +
               	     "\tParamName = " + paramName;
    }

}
