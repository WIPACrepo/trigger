package icecube.daq.trigger.test;

import com.google.gson.Gson;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.payload.IUTCTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

class ExpectedAlertCount
{
    private String varName;
    private Alerter.Priority prio;
    private int expected;
    private int count;

    ExpectedAlertCount(String varName, Alerter.Priority prio, int expected)
    {
        this.varName = varName;
        this.prio = prio;
        this.expected = expected;
    }

    void check()
    {
        if (expected != count) {
            fail(String.format("Expected %d %s %s alerts, got %d", expected,
                               varName, prio, count));
        }
    }

    void inc()
    {
        if (++count > expected) {
            fail(String.format("Saw extra alert for %s@%s (expected %d)",
                               varName, prio, expected));
        }
    }

    boolean isComplete()
    {
        return expected == count;
    }

    boolean matches(String varName, Alerter.Priority prio)
    {
        return this.varName.equals(varName) && this.prio.equals(prio);
    }
}

public class MockAlerter
    implements Alerter
{
    private boolean inactive;
    private boolean closed;

    List<ExpectedAlertCount> expected = new ArrayList<ExpectedAlertCount>();
    private int numSent;

    public MockAlerter()
    {
    }

    public void addExpected(String varName, Priority prio, int count)
    {
        expected.add(new ExpectedAlertCount(varName, prio, count));
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

        boolean found = false;
        for (ExpectedAlertCount e : expected) {
            if (!e.matches(varname, prio)) continue;

            e.inc();
            found = true;
        }
        if (!found) {
            fail("Received unexpected " + varname + " alert, prio " + prio);
        }

        // make sure we can turn alert into a JSON string
        Gson gson = new Gson();
        gson.toJson(obj);

        numSent++;
    }

    public void setAddress(String host, int port)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void waitForAlerts(int maxReps)
    {
        final int sleepTime = 100;

        for (int i = 0; i < maxReps; i++) {
            boolean complete = true;
            for (ExpectedAlertCount e : expected) {
                if (!e.isComplete()) {
                    complete = false;
                    break;
                }
            }
            if (complete) {
                // we've seen all expected alerts
                break;
            }

            // wait for more alerts
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
                // ignore interrupts
            }
        }

        // do final check, failing if there are missing alerts
        for (ExpectedAlertCount e : expected) {
            e.check();
        }
    }
}
