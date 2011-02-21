package icecube.daq.trigger.db;

/**
 * A simple representation of the ReadoutParameters database table.
 */
public class ReadoutParametersLocal
{

    // Table columns
    public int readoutId;
    public int readoutType;
    public int timeOffset;
    public int timeMinus;
    public int timePlus;

    // Default constructor
    public ReadoutParametersLocal() {
	this(0,0,0,0,0);
    }

    // Full constructor
    public ReadoutParametersLocal(int readoutId, int readoutType, int timeOffset, int timeMinus, int timePlus) {
	this.readoutId   = readoutId;
	this.readoutType = readoutType;
	this.timeOffset  = timeOffset;
	this.timeMinus   = timeMinus;
	this.timePlus    = timePlus;
    }

    // Override the toString() method to provide a useful dump
    public String toString() {
	return "ReadoutParameters: ReadoutId = "   + readoutId +
    	                         "\tReadoutType = " + readoutType +
	                         "\tTimeOffset = "  + timeOffset +
                                 "\tTimeMinus = "   + timeMinus +
                         	 "\tTimePlus = "    + timePlus;
    }

}
