package icecube.daq.trigger.test;

import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.component.TriggerComponent;

import java.util.Arrays;
import java.util.HashMap;

class AlgorithmData
{
    private int queuedIn;
    private int queuedOut;
    private long sent;

    int getQueuedIn()
    {
        return queuedIn;
    }

    int getQueuedOut()
    {
        return queuedOut;
    }

    long getSent()
    {
        return sent;
    }

    void setQueuedIn(int val)
    {
        queuedIn = val;
    }

    void setQueuedOut(int val)
    {
        queuedOut = val;
    }

    void setSent(long val)
    {
        sent = val;
    }

    public String toString()
    {
        return String.format("%d->%d->%d", queuedIn, queuedOut, sent);
    }
}

interface ComponentMonitor
{
    boolean check();
    String getPrefix();
    Splicer getSplicer();
    boolean isStopped();
}

class TriggerMonitor
    implements ComponentMonitor
{
    private TriggerComponent comp;
    private String prefix;

    private long received;
    private long processed;
    private HashMap<ITriggerAlgorithm, AlgorithmData> algoData =
        new HashMap<ITriggerAlgorithm, AlgorithmData>();
    private ITriggerAlgorithm[] algoKeys;
    private long queuedOut;
    private long sent;
    private boolean stopped;
    private boolean summarized;

    TriggerMonitor(TriggerComponent comp, String prefix)
    {
        this.comp = comp;
        this.prefix = prefix;
    }

    @Override
    public boolean check()
    {
        if (stopped != summarized) {
            summarized = stopped;
        }

        boolean newStopped = (comp == null ||
                              (!comp.getReader().isRunning() &&
                               comp.getWriter().isStopped()));

        boolean changed = false;
        if (comp != null && !summarized) {
            if (received != comp.getPayloadsReceived()) {
                received = comp.getPayloadsReceived();
                changed = true;
            }

            if (processed != comp.getTriggerManager().getTotalProcessed()) {
                processed = comp.getTriggerManager().getTotalProcessed();
                changed = true;
            }

            Iterable<ITriggerAlgorithm> iter = comp.getAlgorithms();
            if (iter == null) {
                throw new Error("No algorithms available from " +
                                comp.getClass().getName());
            }

            boolean added = false;
            for (ITriggerAlgorithm algo : iter) {
                if (!algoData.containsKey(algo)) {
                    algoData.put(algo, new AlgorithmData());
                    added = true;
                }
                AlgorithmData data = algoData.get(algo);
                if (data.getQueuedIn() != algo.getInputQueueSize()) {
                    data.setQueuedIn(algo.getInputQueueSize());
                    changed = true;
                }
                if (data.getQueuedOut() != algo.getNumberOfCachedRequests()) {
                    data.setQueuedOut(algo.getNumberOfCachedRequests());
                    changed = true;
                }
                if (data.getSent() != algo.getSentTriggerCount()) {
                    data.setSent(algo.getSentTriggerCount());
                    changed = true;
                }
            }
            if (added) {
                Object[] tmpKeys = algoData.keySet().toArray();
                Arrays.sort(tmpKeys);
                algoKeys = new ITriggerAlgorithm[tmpKeys.length];
                for (int i = 0; i < tmpKeys.length; i++) {
                    algoKeys[i] = (ITriggerAlgorithm) tmpKeys[i];
                }
            }

            if (queuedOut != comp.getTriggerManager().getNumOutputsQueued()) {
                queuedOut = comp.getTriggerManager().getNumOutputsQueued();
                changed = true;
            }

            if (sent != comp.getPayloadsSent()) {
                sent = comp.getPayloadsSent();
                changed = true;
            }
        }

        if (stopped != newStopped) {
            stopped = newStopped;
        }

        return changed;
    }

    @Override
    public String getPrefix()
    {
        return prefix;
    }

    public long getSent()
    {
        return sent;
    }

    @Override
    public Splicer getSplicer()
    {
        return comp.getSplicer();
    }

    @Override
    public boolean isStopped()
    {
        return stopped;
    }

    public String toString()
    {
        if (comp == null) {
            return "";
        }

        if (summarized) {
            return " " + prefix + " stopped";
        }

        summarized = stopped;

        StringBuilder buf = new StringBuilder();

        if (algoKeys != null) {
            for (ITriggerAlgorithm algo : algoKeys) {
                if (buf.length() > 0) buf.append(' ');
                buf.append(algo.getTriggerName()).append(' ');
                buf.append(algoData.get(algo));
            }
        }

        return String.format(" %s %d->%d->[%s]->%d->%d", prefix, received,
                             processed, buf.toString(), queuedOut, sent);
    }
}

public class ActivityMonitor
{
    private TriggerMonitor trigMon;

    public ActivityMonitor(TriggerComponent comp, String prefix)
    {
        trigMon = new TriggerMonitor(comp, prefix);
    }

    private void dumpProgress(int rep, int expEvents, boolean dumpSplicers)
    {
        System.err.println("#" + rep + ":" + trigMon);

        if (dumpSplicers && trigMon.getSent() < expEvents + 1) {
            dumpSplicer(trigMon);
        }

    }

    private void dumpSplicer(ComponentMonitor mon)
    {
        final String title = mon.getPrefix();
        final Splicer splicer = mon.getSplicer();

        final String splats = "*********************";
        if (!(splicer instanceof HKN1Splicer)) {
            System.err.println(splats);
            System.err.println("*** Unknown " + title + " Splicer: " +
                               splicer.getClass().getName());
            System.err.println(splats);
        } else {
            System.err.println(splats);
            System.err.println("*** " + title + " Splicer");
            System.err.println(splats);
            String[] desc = ((HKN1Splicer) splicer).dumpDescription();
            for (int d = 0; d < desc.length; d++) {
                System.err.println("  " + desc[d]);
            }
        }
    }

    private boolean isChanged()
    {
        return trigMon.check();
    }

    private boolean isStopped()
    {
        return trigMon.isStopped();
    }

    public boolean waitForStasis(int staticReps, int maxReps, int expEvents,
                                 boolean verbose, boolean dumpSplicers)
    {
        final int SLEEP_MSEC = 100;

        int numStatic = 0;
        for (int i = 0; i < maxReps; i++) {

            if (isChanged()) {
                numStatic = 0;
            } else if (isStopped()) {
                numStatic += staticReps / 2;
            } else {
                numStatic++;
            }

            if (verbose) {
                dumpProgress(i, expEvents, dumpSplicers);
            }

            if (numStatic >= staticReps) {
                break;
            }

            try {
                Thread.sleep(SLEEP_MSEC);
            } catch (Throwable thr) {
                // ignore errors
            }
        }

        return numStatic >= staticReps;
    }
}
