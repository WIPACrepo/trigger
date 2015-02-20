package icecube.daq.trigger.test;

import icecube.daq.trigger.component.TriggerComponent;
import icecube.daq.trigger.exceptions.UnimplementedError;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Splicer;

class TriggerMonitor
{
    private TriggerComponent comp;
    private String prefix;

    private long received;
    private long queuedIn;
    private long processed;
    private long queuedReq;
    private long queuedOut;
    private long sent;
    private boolean stopped;
    private boolean forcedStop;
    private boolean summarized;

    TriggerMonitor(TriggerComponent comp, String prefix)
    {
        this.comp = comp;
        this.prefix = prefix;
    }

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
            if (queuedIn != comp.getTriggerManager().getNumInputsQueued()) {
                queuedIn = comp.getTriggerManager().getNumInputsQueued();
                changed = true;
            }
            if (processed != comp.getTriggerManager().getTotalProcessed()) {
                processed = comp.getTriggerManager().getTotalProcessed();
                changed = true;
            }
            if (queuedReq != comp.getTriggerManager().getNumRequestsQueued()) {
                queuedReq = comp.getTriggerManager().getNumRequestsQueued();
                changed = true;
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

    public String getPrefix()
    {
        return prefix;
    }

    public long getSent()
    {
        return sent;
    }

    public Splicer getSplicer()
    {
        return comp.getSplicer();
    }

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
        return String.format(" %s %d->%d->%d->%d->%d->%d", prefix, received,
                             queuedIn, processed, queuedReq, queuedOut, sent);
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
            dumpSplicer(trigMon.getPrefix(), trigMon.getSplicer());
        }

    }

    private void dumpSplicer(String title, Splicer splicer)
    {
        System.err.println("*********************");
        System.err.println("*** " + title + " Splicer");
        System.err.println("*********************");
        String[] desc = ((HKN1Splicer) splicer).dumpDescription();
        for (int d = 0; d < desc.length; d++) {
            System.err.println("  " + desc[d]);
        }
    }

    public boolean waitForStasis(int staticReps, int maxReps, int expEvents,
                                 boolean verbose, boolean dumpSplicers)
    {
        final int SLEEP_MSEC = 100;

        int numStatic = 0;
        for (int i = 0; i < maxReps; i++) {
            boolean changed = false;

            changed |= trigMon.check();

            if (changed) {
                numStatic = 0;
            } else if (trigMon.isStopped())
            {
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
