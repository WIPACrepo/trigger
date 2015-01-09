package icecube.daq.trigger.test;

import com.google.gson.Gson;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.payload.IUTCTime;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MockAlerter
    implements Alerter
{
    private boolean inactive;
    private boolean closed;

    private int numSent;
    private String expVarName;
    private Priority expPrio;

    public MockAlerter()
    {
    }

    public void close()
    {
        closed = true;
    }

    public void deactivate()
    {
        inactive = true;
    }

    public int getNumSent()
    {
        return numSent;
    }

    public String getService()
    {
        return DEFAULT_SERVICE;
    }

    public boolean isActive()
    {
        return !inactive;
    }

    public boolean isClosed()
    {
        return closed;
    }

    public void sendObject(Object obj)
        throws AlertException
    {
        if (closed) {
            throw new Error("Alerter has been closed");
        }

        if (obj == null) {
            throw new Error("Cannot send null object");
        } else if (!(obj instanceof Map)) {
            throw new Error("Unexpected object type " +
                            obj.getClass().getName());
        }

        Map<String, Object> map = (Map<String, Object>) obj;

        String varname;
        if (!map.containsKey("varname")) {
            varname = null;
        } else {
            varname = (String) map.get("varname");
        }

        Alerter.Priority prio = Alerter.Priority.DEBUG;
        if (map.containsKey("prio")) {
            int tmpVal = (Integer) map.get("prio");
            for (Alerter.Priority p : Alerter.Priority.values()) {
                if (p.value() == tmpVal) {
                    prio = p;
                    break;
                }
            }
        }

        String dateStr;
        if (!map.containsKey("t")) {
            dateStr = null;
        } else {
            dateStr = (String) map.get("t");
        }

        Map<String, Object> values;
        if (!map.containsKey("value")) {
            values = null;
        } else {
            values = new HashMap<String, Object>();

            Map<String, Object> tmpVals =
                (Map<String, Object>) map.get("value");
            for (String key : tmpVals.keySet()) {
                values.put(key, tmpVals.get(key));
            }
        }

        if (expVarName == null || expPrio == null) {
            fail("Received unexpected " + varname + " alert, prio " + prio);
        }

        assertEquals("Unexpected varname", expVarName, varname);
        assertEquals("Unexpected priority", expPrio, prio);

        Gson gson = new Gson();
        gson.toJson(obj);

        numSent++;
    }

    public void setAddress(String host, int port)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void setExpectedPriority(Priority prio)
    {
        expPrio = prio;
    }

    public void setExpectedVarName(String name)
    {
        expVarName = name;
    }
}
