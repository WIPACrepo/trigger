package icecube.daq.trigger.control;

import icecube.daq.payload.IPayload;
import icecube.daq.trigger.algorithm.INewAlgorithm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Trigger algorithm thread.
 */
public class TriggerThread
    implements Runnable
{
    /** Message logger. */
    private static final Log LOG = LogFactory.getLog(TriggerThread.class);

    private int id;
    private INewAlgorithm algorithm;
    private Thread thread;
    private boolean stopping;
    private boolean stopped;
    private long numSent;

    TriggerThread(int id, INewAlgorithm algorithm)
    {
        if (algorithm == null) {
            throw new Error("Algorithm cannot be null");
        }

        this.id = id;
        this.algorithm = algorithm;
    }

    public boolean isStopped()
    {
        return stopped;
    }

    public void join()
    {
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException ie) {
                // ignore interrupts
            }
        }
    }


    public void start()
    {
        if (algorithm.getSubscriber() == null) {
            throw new Error(algorithm.toString() +
                            " is not subscribed to any lists");
        }

        thread = new Thread(this);
        thread.setName(algorithm.getTriggerName() + "-Thread");
        thread.start();
    }

    public void stop()
    {
        stopping = true;

        PayloadSubscriber sub = algorithm.getSubscriber();
        if (sub == null) {
            LOG.error(algorithm.toString() +
                      " is not subscribed to any lists");
        } else {
            sub.stop();
        }
    }

    public void run()
    {
        while (true) {
            IPayload pay = algorithm.getSubscriber().pop();
            if (pay == null) {
                if (stopping && algorithm.getSubscriber().isStopped()) {
                    break;
                }

                LOG.error("Ignoring null payload for " +
                          algorithm.getTriggerName());
            } else if (pay == TriggerManager.FLUSH_PAYLOAD) {
                algorithm.sendLast();
            } else {
                numSent++;
                try {
                    algorithm.runTrigger(pay);
                } catch (Throwable thr) {
                    LOG.error("Trigger " + algorithm + " failed for " + pay,
                              thr);
                }
            }
        }

        stopped = true;
    }

    public String toString()
    {
        PayloadSubscriber sub = algorithm.getSubscriber();

        int qlen;
        if (sub == null) {
            qlen = -1;
        } else {
            qlen = sub.size();
        }

        return "TriggerThread#" + id +
            ":" + algorithm.getTriggerName() +
            "[" + sub + "]" +
            ",sent#" + numSent +
            ",q#" + qlen +
            (stopping ? ":stopping" : "") +
            (stopped ? ":stopped" : "");
    }
}
