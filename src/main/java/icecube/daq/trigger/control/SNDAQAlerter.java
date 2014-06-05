package icecube.daq.trigger.control;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.ZMQAlerter;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.PayloadFormatException;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.algorithm.SimpleMajorityTrigger;

import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SNDAQAlerter
{
    public static final String PROPERTY = "icecube.sndaq.zmq.address";

    private static final Log LOG = LogFactory.getLog(SNDAQAlerter.class);

    private ZMQAlerter zmq;

    private int smt8cfgId;
    private int smt8type;
    private boolean smt8init;

    private AlertThread thread;

    private int runNumber = Integer.MIN_VALUE;

    public SNDAQAlerter()
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

        zmq = new ZMQAlerter();
        zmq.setAddress(host, port);
    }

    /**
     * Close any open files/sockets.
     */
    public void close()
    {
        if (thread != null) {
            thread.stop();
        }
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
        return zmq.isActive();
    }

    /**
     * Grab algorithm information, used for identifying requests.
     *
     * @param algorithms trigger algorithms
     */
    public void loadAlgorithms(List<INewAlgorithm> algorithms)
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
            thread = new AlertThread();
            thread.start();
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

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("trigger", "SMT8");
        map.put("t", req.getLastTimeUTC().toDateString());
        map.put("num", numHits);

        thread.queue(map);
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
        zmq.setAddress(host, port);
    }

    /**
     * Set the current run number
     *
     * @param runNumber run number
     */
    public void setRunNumber(int num)
    {
        runNumber = num;
    }

    /**
     * Thread which sends alerts to SNDAQ
     */
    class AlertThread
        implements Runnable
    {
        private Thread thread;
        private Deque<HashMap<String, Object>> queue =
            new ArrayDeque<HashMap<String, Object>>();

        private Object lastTime;

        private boolean sentStart;

        private boolean stopping;
        private boolean stopped;

        AlertThread()
        {
            thread = new Thread(this);
            thread.setName("AlertThread");
        }

        public void queue(HashMap<String, Object> map)
        {
            synchronized (queue) {
                queue.addLast(map);
                queue.notify();
            }
        }

        public void run()
        {
            while (!stopping || queue.size() > 0) {
                HashMap<String, Object> map;
                synchronized (queue) {
                    if (queue.size() == 0) {
                        try {
                            queue.wait();
                        } catch (InterruptedException ie) {
                            LOG.error("Interrupt while waiting for alert queue",
                                      ie);
                        }
                    }

                    if (queue.size() == 0) {
                        map = null;
                    } else {
                        map = queue.removeFirst();
                    }
                }

                if (map == null) {
                    continue;
                }

                if (map.containsKey("t")) {
                    lastTime = map.get("t");
                } else {
                    LOG.error("SNDAQ message does not contain 't' field");
                }

                if (!sentStart) {
                    sendAction("start", lastTime, runNumber);
                    sentStart = true;
                }

                try {
                    zmq.sendObject(map);
                } catch (AlertException ae) {
                    LOG.error("Cannot send " + map, ae);
                }
            }

            sendAction("stop", lastTime, runNumber);

            if (zmq.isActive()) {
                zmq.close();
            }

            stopped = true;
        }

        public void sendAction(String action, Object time, int runNumber)
        {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put(action, runNumber);
            map.put("t", time);

            try {
                zmq.sendObject(map);
            } catch (AlertException ae) {
                LOG.error("Cannot send " + map, ae);
            }
        }

        public void start()
        {
            thread.start();
        }

        public void stop()
        {
            synchronized (queue) {
                stopping = true;
                queue.notify();
            }
        }
    }
}
