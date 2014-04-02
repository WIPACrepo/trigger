package icecube.daq.trigger.control;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.ZMQAlerter;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.algorithm.SimpleMajorityTrigger;

import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SNDAQAlerter
{
    private static final Log LOG = LogFactory.getLog(SNDAQAlerter.class);

    private ZMQAlerter zmq;

    private int smt8cfgId;
    private int smt8type;
    private boolean smt8init;

    private AlertThread thread;

    public SNDAQAlerter(String host, int port)
        throws AlertException
    {
        zmq = new ZMQAlerter();
        zmq.setAddress(host, port);
    }

    /**
     * Close any open files/sockets.
     */
    public void close()
    {
        thread.stop();
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

        thread = new AlertThread();
    }

    public void process(ITriggerRequestPayload req)
        throws AlertException
    {
        if (req.getTriggerConfigID() != smt8cfgId ||
            req.getTriggerType() != smt8type)
        {
            return;
        }

        int numHits;
        try {
            numHits = req.getPayloads().size();
        } catch (DataFormatException dfe) {
            LOG.error("Cannot get number of SMT8 hits", dfe);
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

    class AlertThread
        implements Runnable
    {
        private Deque queue = new ArrayDeque();
        private boolean stopping;
        private boolean stopped;

        public void queue(Object obj)
        {
            synchronized (queue) {
                queue.addLast(obj);
                queue.notify();
            }
        }

        public void run()
        {
            while (!stopping || queue.size() > 0) {
                Object obj;
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
                        obj = null;
                    } else {
                        obj = queue.removeFirst();
                    }
                }

                if (obj == null) {
                    continue;
                }

                try {
                    zmq.sendObject(obj);
                } catch (AlertException ae) {
                    LOG.error("Cannot send " + obj, ae);
                }
            }

            if (zmq.isActive()) {
                zmq.close();
            }

            stopped = true;
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
