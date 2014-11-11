package icecube.daq.trigger.test;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.payload.IUTCTime;

import java.util.Calendar;
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
        throw new Error("Unimplemented");
    }

    public boolean isActive()
    {
        return !inactive;
    }

    public boolean isClosed()
    {
        return closed;
    }

    public void send(String varname, Priority prio, Calendar dateTime,
                     Map<String, Object> values)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void send(String varname, Priority prio, IUTCTime utcTime,
                     Map<String, Object> values)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void send(String varname, Priority prio, Map<String, Object> values)
        throws AlertException
    {
        if (closed) {
            throw new Error("Alerter has been closed");
        }

        assertEquals("Unexpected varname", varname, expVarName);
        assertEquals("Unexpected priority", prio, expPrio);

        numSent++;
    }

    public void sendAlert(Priority prio, String condition, Map x2)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void sendAlert(Priority prio, String condition, String notify,
                          Map<String, Object> vars)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void sendAlert(Calendar dateTime, Priority prio, String condition,
                          String notify, Map<String, Object> vars)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void sendAlert(IUTCTime utcTime, Priority prio, String condition,
                          String notify, Map<String, Object> vars)
        throws AlertException
    {
        throw new Error("Unimplemented");
    }

    public void sendObject(Object obj)
        throws AlertException
    {
        throw new Error("Unimplemented");
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
