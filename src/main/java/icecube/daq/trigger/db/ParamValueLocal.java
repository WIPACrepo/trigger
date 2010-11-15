package icecube.daq.trigger.db;

/**
 * A simple representation of the ParamValue database table.
 */
public class ParamValueLocal
{

    // Table columns
    public int paramValueId;
    public String paramValue;

    // Default constructor
    public ParamValueLocal() {
	this(0,"");
    }

    // Full constructor
    public ParamValueLocal(int paramValueId, String paramValue) {
	this.paramValueId = paramValueId;
	this.paramValue   = paramValue;
    }

    // Override the toString() method to provide a useful dump
    public String toString() {
	return "ParamValue: ParamValueId = " + paramValueId +
          	          " ParamValue = "   + paramValue;
    }

}
