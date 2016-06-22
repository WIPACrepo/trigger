package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IHitPayload;

import java.util.Comparator;

class HitComparator
    implements Comparator<IHitPayload>
{
    public int compare(IHitPayload h1, IHitPayload h2)
    {
        if (h1.getUTCTime() < h2.getUTCTime()) {
            return -1;
        } else if (h1.getUTCTime() > h2.getUTCTime()) {
            return 1;
        }

        return 0;
    }
}
