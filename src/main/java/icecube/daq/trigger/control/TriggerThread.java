package icecube.daq.trigger.control;

import icecube.daq.payload.IPayload;
import icecube.daq.trigger.algorithm.INewAlgorithm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Trigger algorithm thread.
 */
class TriggerThread
    implements Runnable
{
    /** Message logger. */
    private static final Log LOG = LogFactory.getLog(TriggerThread.class);

    private int id;
    private INewAlgorithm algorithm;
    private Thread thread;
    private boolean stopped;

    TriggerThread(int id, INewAlgorithm algorithm)
    {
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
            throw new Error("Algorithm is not subscribed to any lists");
        }

        thread = new Thread(this);
        thread.setName(algorithm.getTriggerName() + "-Thread");
        thread.start();
    }

    public void stop()
    {
        stopped = true;

        algorithm.getSubscriber().stop();
    }

    public void run()
    {
        while (!stopped || algorithm.getSubscriber().hasData()) {
            IPayload pay = algorithm.getSubscriber().pop();
            if (pay == null) {
                if (stopped && !algorithm.getSubscriber().hasData()) {
                    break;
                }

                LOG.error("Ignoring null payload for " +
                          algorithm.getTriggerName());
            } else if (pay == TriggerManager.FLUSH_PAYLOAD) {
                algorithm.sendLast();
            } else {
                try {
                    algorithm.runTrigger(pay);
                } catch (Exception ex) {
                    LOG.error("Trigger " + algorithm + " failed for " + pay,
                              ex);
                }
            }

            Thread.yield();
        }
    }

    public String toString()
    {
        return "TriggerThread#" + id + ":" + algorithm.getTriggerName() + ":" +
            algorithm.getSubscriber() + (stopped ? ":stopped" : "");
    }
}
