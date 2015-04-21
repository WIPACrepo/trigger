package icecube.daq.trigger.control;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.AlertQueue;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.juggler.alert.ZMQAlerter;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.PayloadFormatException;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.algorithm.SimpleMajorityTrigger;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SNDAQAlerter
{
    public static final String PROPERTY = "icecube.sndaq.zmq.address";

    private static final Log LOG = LogFactory.getLog(SNDAQAlerter.class);

    private Alerter alerter;

    private int smt8cfgId;
    private int smt8type;
    private boolean smt8init;

    private String lastTime;
    private boolean sentStart;

    private AlertQueue alertQueue;

    private int runNumber = Integer.MIN_VALUE;

    /**
     * Create a supernova DAQ alerter.
     *
     * @param algorithms trigger algorithms
     *
     * @throws AlertException if there is a problem
     */
    public SNDAQAlerter(List<INewAlgorithm> algorithms)
        throws AlertException
    {
        String address = System.getProperty(PROPERTY, null);
        if (address == null) {
            throw new AlertException("No " + PROPERTY + " property");
        }

        final int ic = address.indexOf(':');
        if (ic < 0) {
            throw new AlertException("Bad SNDAQ address \"" + address + "\"");
        }

        String host;
        if (ic == 0) {
            host = "localhost";
        } else {
            host = address.substring(0, ic);
        }

        int port;
        String pstr = address.substring(ic + 1);
        try {
            port = Integer.parseInt(pstr);
        } catch (NumberFormatException nfe) {
            throw new AlertException("Bad port in SNDAQ address \"" + address +
                                     "\"");
        }

        alerter = createZMQAlerter(host, port);

        loadAlgorithms(algorithms);
    }

    /**
     * Close any open files/sockets.
     */
    public void close()
    {
        if (alertQueue != null) {
            sendStop();

            alertQueue.stop();
        }
    }

    /**
     * Create the ZMQ alerter.  This method exists mainly to allow unit tests
     * to inject a mock alerter.
     *
     * @param host host name
     * @param port port number
     *
     * @return alerter
     *
     * @throws AlertException if the ZMQ connection cannot be made
     */
    public Alerter createZMQAlerter(String host, int port)
        throws AlertException
    {
        ZMQAlerter z = new ZMQAlerter();
        z.setAddress(host, port);
        return z;
    }

    /**
     * Get number of alerts dropped
     *
     * @return number of alerts dropped
     */
    public long getNumDropped()
    {
        return alertQueue.getNumDropped();
    }

    /**
     * Get number of alerts queued
     *
     * @return number of alerts queued
     */
    public int getNumQueued()
    {
        return alertQueue.getNumQueued();
    }

    /**
     * Get number of alerts sent
     *
     * @return number of alerts sent
     */
    public long getNumSent()
    {
        return alertQueue.getNumSent();
    }

    /**
     * Get the current run number
     *
     * @return run number
     */
    public int getRunNumber()
    {
        return runNumber;
    }

    /**
     * Get the service name
     *
     * @return service name
     */
    public String getService()
    {
        throw new Error("SNDAQAlerter does not use the service name");
    }

    /**
     * If <tt>true</tt>, alerts will be sent to one or more recipients.
     *
     * @return <tt>true</tt> if this alerter will send messages
     */
    public boolean isActive()
    {
        return alerter.isActive();
    }

    /**
     * Grab algorithm information, used for identifying requests.
     *
     * @param algorithms trigger algorithms
     */
    private void loadAlgorithms(List<INewAlgorithm> algorithms)
    {
        for (INewAlgorithm a : algorithms) {
            if (!a.getTriggerName().startsWith("SimpleMajorityTrigger")) {
                continue;
            }

            SimpleMajorityTrigger smt = (SimpleMajorityTrigger) a;
            if (smt.getThreshold() != 8) {
                continue;
            }

            smt8cfgId = smt.getTriggerConfigId();
            smt8type = smt.getTriggerType();
            smt8init = true;
        }

        if (smt8init) {
            alertQueue = new AlertQueue("SNDAQQueue", alerter);
            alertQueue.start();
        }
    }

    /**
     * Process a single trigger request
     *
     * @param req trigger request
     */
    public void process(ITriggerRequestPayload req)
    {
        if (!smt8init || req.getTriggerConfigID() != smt8cfgId ||
            req.getTriggerType() != smt8type)
        {
            return;
        }

        int numHits;
        try {
            numHits = req.getPayloads().size();
        } catch (PayloadFormatException pfe) {
            LOG.error("Cannot get number of SMT8 hits", pfe);
            return;
        }

        final String timeStr = req.getLastTimeUTC().toDateString();

        if (!sentStart) {
            // send 'start' message
            HashMap<String, Object> startMap = new HashMap<String, Object>();
            startMap.put("start", runNumber);
            startMap.put("t", timeStr);

            try {
                alertQueue.push(startMap);
            } catch (AlertException ae) {
                LOG.error("Cannot send initial SNDAQ message", ae);
            }

            sentStart = true;
        }

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("trigger", "SMT8");
        map.put("t", timeStr);
        map.put("num", numHits);

        try {
            alertQueue.push(map);
        } catch (AlertException ae) {
            LOG.error("Cannot send SNDAQ message", ae);
        }

        lastTime = timeStr;
    }

    private void sendStop()
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("stop", runNumber);
        map.put("t", lastTime);

        try {
            alertQueue.push(map);
        } catch (AlertException ae) {
            LOG.error("Cannot send final SNDAQ message", ae);
        }
    }

    /**
     * Set monitoring server host and port
     *
     * @param host - server host name
     * @param port - server port number
     *
     * @throws AlertException if there is a problem with one of the parameters
     */
    public void setAddress(String host, int port)
        throws AlertException
    {
        alerter.setAddress(host, port);
    }

    /**
     * Set the current run number
     *
     * @param num run number
     */
    public void setRunNumber(int num)
    {
        if (runNumber != Integer.MIN_VALUE && runNumber != num &&
            alertQueue != null)
        {
            // if a run is in progress, we must be switching to a new run
            sendStop();
            sentStart = false;
        }

        // set the run number
        runNumber = num;
    }
}
