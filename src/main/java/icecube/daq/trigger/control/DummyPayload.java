/*
 * class: DummyPayload
 *
 * Version $Id: DummyPayload.java 17485 2019-07-30 15:10:03Z dglo $
 *
 * Date: October 7 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.splicer.Spliceable;
import icecube.daq.trigger.exceptions.UnimplementedError;

import java.nio.ByteBuffer;

/**
 * This class is a dummy payload that only has a UTC time associated with it.
 *
 * @version $Id: DummyPayload.java 17485 2019-07-30 15:10:03Z dglo $
 * @author pat
 */
public class DummyPayload
    implements Spliceable, ILoadablePayload
{
    private long time;
    private IUTCTime utcTime;

    /**
     * Create a dummy payload.
     *
     * @param payloadTimeUTC payload time
     */
    public DummyPayload(IUTCTime payloadTimeUTC)
    {
        if (payloadTimeUTC == null) {
            throw new Error("Payload time cannot be null");
        }

        this.time = payloadTimeUTC.longValue();
    }

    /**
     * Create a dummy payload.
     *
     * @param time payload time
     */
    public DummyPayload(long time)
    {
        this.time = time;
    }

    /**
     * Compare this payload against others in the splicer.
     *
     * @param spl spliced object
     *
     * @return the usual comparison values
     */
    @Override
    public int compareSpliceable(Spliceable spl)
    {
        if (spl == null) {
            return -1;
        }

        IUTCTime payTime = ((IPayload) spl).getPayloadTimeUTC();
        if (payTime == null) {
            return -1;
        }

        long val = time - payTime.longValue();
        if (val < 0) {
            return -1;
        } else if (val > 0) {
            return 1;
        }

        return 0;
    }

    /**
     * This method allows a deepCopy of itself.
     *
     * @return Object which is a copy of the object which implements this
     *         interface.
     */
    @Override
    public Object deepCopy()
    {
        return new DummyPayload(time);
    }

    /**
     * XXX unimplemented
     *
     * @return UnimplementedError
     */
    @Override
    public ByteBuffer getPayloadBacking()
    {
        throw new UnimplementedError();
    }

    /**
     * returns the Payload interface type as defined in the
     * PayloadInterfaceRegistry.
     *
     * @return one of the defined types in
     *         icecube.daq.payload.PayloadInterfaceRegistry
     */
    @Override
    public int getPayloadInterfaceType()
    {
        return -1;
    }

    /**
     * gets the UTC time tag of a payload
     *
     * @return UTC time object
     */
    @Override
    public IUTCTime getPayloadTimeUTC()
    {
        if (utcTime == null) {
            utcTime = new UTCTime(time);
        }

        return utcTime;
    }

    /**
     * returns the Payload type
     *
     * @return -1
     */
    @Override
    public int getPayloadType()
    {
        return -1;
    }

    /**
     * gets the UTC time tag of a payload as a long value
     *
     * @return UTC time
     */
    @Override
    public long getUTCTime()
    {
        return time;
    }

    @Override
    public int length()
    {
        return 0;
    }

    /**
     * Do nothing
     */
    @Override
    public void loadPayload()
    {
        // do nothing
    }

    /**
     * Do nothing
     */
    @Override
    public void recycle()
    {
        // do nothing
    }

    /**
     * Do nothing
     *
     * @param cache ignored
     */
    @Override
    public void setCache(IByteBufferCache cache)
    {
        // do nothing
    }

    /**
     * Return a debugging string.
     *
     * @return debugging string
     */
    @Override
    public String toString()
    {
        return "Dummy@" + time;
    }
}
