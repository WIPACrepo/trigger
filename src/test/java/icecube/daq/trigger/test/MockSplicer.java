package icecube.daq.trigger.test;

import icecube.daq.splicer.ClosedStrandException;
import icecube.daq.splicer.MonitorPoints;
import icecube.daq.splicer.OrderingException;
import icecube.daq.splicer.Spliceable;
import icecube.daq.splicer.SplicedAnalysis;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerChangedEvent;
import icecube.daq.splicer.SplicerException;
import icecube.daq.splicer.SplicerListener;
import icecube.daq.splicer.StrandTail;

import java.io.IOException;

import java.nio.channels.SelectableChannel;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MockSplicer
    implements Splicer
{
    private static final Log LOG = LogFactory.getLog(MockSplicer.class);

    private int state = STOPPED;

    private SplicedAnalysis analysis;
    private ArrayList<SplicerListener> listeners =
        new ArrayList<SplicerListener>();
    private ArrayList<MockStrandTail> strands = new ArrayList<MockStrandTail>();

    private ArrayList<Spliceable> rope = new ArrayList<Spliceable>();
    private int ropeDec = 0;

    private int nextTailNum = 0;

    class MockStrandTail
        implements StrandTail
    {
        private MockSplicer splicer;
        private int tailState = STARTED;
        private int tailNum = nextTailNum++;

        public MockStrandTail(MockSplicer splicer)
        {
            this.splicer = splicer;
        }

        public void close()
        {
            throw new Error("Unimplemented");
        }

        public Spliceable head()
        {
            throw new Error("Unimplemented");
        }

        public boolean isClosed()
        {
            throw new Error("Unimplemented");
        }

        public StrandTail push(List spliceables)
            throws OrderingException, ClosedStrandException
        {
            throw new OrderingException("Unimplemented");
        }

        public synchronized StrandTail push(Spliceable spliceable)
            throws OrderingException, ClosedStrandException
        {
            if (state == Splicer.STOPPED) {
                start(spliceable);
            }

            if (tailState == Splicer.STOPPING) {
                LOG.error("Attempt to push on top of LAST_POSSIBLE_SPLICEABLE");
            } else if (spliceable == Splicer.LAST_POSSIBLE_SPLICEABLE) {
                tailState = STOPPING;
                tailStateChanged();
            } else {
                addSpliceable(spliceable);
            }

            return this;
        }

        public int size()
        {
            return 0;
        }

        public String toString()
        {
            return "MockStrand#" + tailNum;
        }
    }

    public MockSplicer(SplicedAnalysis analysis)
    {
        this.analysis = analysis;
    }

    public void addSpliceableChannel(SelectableChannel selChan)
        throws IOException
    {
        throw new IOException("Unimplemented");
    }

    private void addSpliceable(Spliceable spl)
    {
        synchronized (rope) {
            rope.add(spl);
            if (rope.size() > 5 &&
                (state == Splicer.STARTED || state == Splicer.STOPPING))
            {
                int oldDec = ropeDec;
                ropeDec = 0;
                analysis.execute(rope, oldDec);
            }
        }
    }

    public void addSplicerListener(SplicerListener listener)
    {
        listeners.add(listener);
    }

    public void analyze()
    {
        throw new Error("Unimplemented");
    }

    public StrandTail beginStrand()
    {
        MockStrandTail strand = new MockStrandTail(this);
        strands.add(strand);
        return strand;
    }

    public void dispose()
    {
        throw new Error("Unimplemented");
    }

    public void forceStop()
    {
        throw new Error("Unimplemented");
    }

    public SplicedAnalysis getAnalysis()
    {
        return analysis;
    }

    public MonitorPoints getMonitorPoints()
    {
        throw new Error("Unimplemented");
    }

    public int getState()
    {
        return state;
    }

    public String getStateString()
    {
        return getStateString(state);
    }

    public String getStateString(int i0)
    {
        String str;
        switch (state) {
        case FAILED:
            str = "FAILED";
            break;
        case STOPPED:
            str = "STOPPED";
            break;
        case STARTING:
            str = "STARTING";
            break;
        case STARTED:
            str = "STARTED";
            break;
        case DISPOSED:
            str = "DISPOSED";
            break;
        default:
            str = "UNKNOWN";
            break;
        }
        return str;
    }

    public int getStrandCount()
    {
        throw new Error("Unimplemented");
    }

    public List pendingChannels()
    {
        throw new Error("Unimplemented");
    }

    public List pendingStrands()
    {
        throw new Error("Unimplemented");
    }

    public void removeSpliceableChannel(SelectableChannel selChan)
    {
        throw new Error("Unimplemented");
    }

    public void removeSplicerListener(SplicerListener listener)
    {
        throw new Error("Unimplemented");
    }

    public void start()
    {
    }

    public synchronized void start(Spliceable spliceable)
    {
        if (state == STARTED) {
            return;
        }

        ArrayList empty = new ArrayList();

        if (listeners.size() > 0) {
            SplicerChangedEvent evt =
                new SplicerChangedEvent(this, STARTING, spliceable, empty);
            for (SplicerListener l : listeners) {
                l.starting(evt);
            }
        }

        state = STARTED;

        if (listeners.size() > 0) {
            SplicerChangedEvent evt =
                new SplicerChangedEvent(this, STARTED, spliceable, empty);
            for (SplicerListener l : listeners) {
                l.started(evt);
            }
        }
    }

    public void stop()
    {
        for (MockStrandTail strand : strands) {
            if (strand.tailState != STOPPING) {
                try {
                    strand.push(LAST_POSSIBLE_SPLICEABLE);
                } catch (SplicerException sex) {
                    LOG.error("Couldn't push last spliceable", sex);
                }
            }
        }
    }

    public void stop(Spliceable spliceable)
        throws OrderingException
    {
        throw new OrderingException("Unimplemented");
    }

    private void tailStateChanged()
    {
        boolean allStopped = true;
        for (MockStrandTail strand : strands) {
            if (strand.tailState != STOPPING) {
                allStopped = false;
                break;
            }
        }

        if (allStopped) {
            ArrayList empty = new ArrayList();

            if (listeners.size() > 0) {
                SplicerChangedEvent evt =
                    new SplicerChangedEvent(this, STOPPING,
                                            LAST_POSSIBLE_SPLICEABLE, empty);
                for (SplicerListener l : listeners) {
                    l.stopping(evt);
                }
            }

            analysis.execute(rope, ropeDec);
            truncate(LAST_POSSIBLE_SPLICEABLE);
            state = STOPPED;

            if (listeners.size() > 0) {
                SplicerChangedEvent evt =
                    new SplicerChangedEvent(this, STOPPED,
                                            LAST_POSSIBLE_SPLICEABLE, empty);
                for (SplicerListener l : listeners) {
                    l.stopped(evt);
                }
            }
        }
    }

    public void truncate(Spliceable spliceable)
    {
        ArrayList<Spliceable> cutRope = new ArrayList<Spliceable>();

        synchronized (rope) {
            int truncIdx = -1;

            while (rope.size() > 0 &&
                   rope.get(0).compareTo(spliceable) <= 0)
            {
                Spliceable cutSpl = rope.remove(0);
                cutRope.add(cutSpl);
            }
            ropeDec += cutRope.size();
        }

        for (SplicerListener listener : listeners) {
            listener.truncated(new SplicerChangedEvent(this, state, spliceable,
                                                       cutRope));
        }
    }
}
