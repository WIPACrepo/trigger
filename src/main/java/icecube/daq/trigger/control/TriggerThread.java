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
    private PayloadSubscriber sub;
    private INewAlgorithm algorithm;
    private Thread thread;
    private boolean stopped;

    TriggerThread(int id, PayloadSubscriber sub, INewAlgorithm algorithm)
    {
        this.id = id;
        this.sub = sub;
        this.algorithm = algorithm;

        thread = new Thread(this);
        thread.setName(algorithm.getTriggerName() + "-Thread");
        thread.start();
    }

    public boolean isStopped()
    {
        return stopped;
    }

    public void join()
    {
        try {
            thread.join();
        } catch (InterruptedException ie) {
            // ignore interrupts
        }
    }

    public void stop()
    {
        stopped = true;

        sub.stop();
    }

    public void run()
    {
        while (!stopped || sub.hasData()) {
            IPayload pay = sub.pop();
            if (pay == null) {
                LOG.error("Ignoring null payload for " + algorithm);
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
            sub + (stopped ? ":stopped" : "");
    }
}
