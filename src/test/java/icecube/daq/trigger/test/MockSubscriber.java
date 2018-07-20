package icecube.daq.trigger.test;

import icecube.daq.payload.IPayload;
import icecube.daq.trigger.control.PayloadSubscriber;
import icecube.daq.trigger.control.TriggerThread;

import java.util.ArrayList;

public class MockSubscriber
    implements PayloadSubscriber
{
    private ArrayList<IPayload> payloads = new ArrayList<IPayload>();
    private boolean stopping;

    private TriggerThread thrd;

    public void add(IPayload pay)
    {
        payloads.add(pay);
    }

    @Override
    public String getName()
    {
        return "MockSubscriber";
    }

    @Override
    public boolean hasData()
    {
        return payloads.size() > 0;
    }

    @Override
    public boolean isStopped()
    {
        return stopping && payloads.size() == 0;
    }

    @Override
    public IPayload pop()
    {
        if (payloads.size() <= 1 && thrd != null) {
            thrd.stop();
        }

        if (payloads.size() < 1) {
            return null;
        }

        return payloads.remove(0);
    }

    @Override
    public void push(IPayload pay)
    {
        throw new Error("Unimplemented");
    }

    @Override
    public int size()
    {
        return payloads.size();
    }

    public void setThread(TriggerThread thrd)
    {
        this.thrd = thrd;
    }

    @Override
    public void stop()
    {
        stopping = true;
        // do nothing
    }
}
