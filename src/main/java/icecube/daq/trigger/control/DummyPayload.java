/*
 * class: DummyPayload
 *
 * Version $Id: DummyPayload.java 12680 2011-02-18 21:51:17Z dglo $
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
import icecube.daq.splicer.Spliceable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

/**
 * This class is a dummy payload that only has a UTC time associated with it.
 * Its main purpose is for truncating the Splicer.
 *
 * @version $Id: DummyPayload.java 12680 2011-02-18 21:51:17Z dglo $
 * @author pat
 */
public class DummyPayload
    implements Spliceable, ILoadablePayload
{

    private IUTCTime payloadTimeUTC;

    public DummyPayload(IUTCTime payloadTimeUTC) {
        this.payloadTimeUTC = payloadTimeUTC;
    }

    public int compareSpliceable(Spliceable spl) {
        return payloadTimeUTC.compareTo(((IPayload) spl).getPayloadTimeUTC());
    }

    /**
     * This method allows a deepCopy of itself.
     *
     * @return Object which is a copy of the object which implements this interface.
     */
    public Object deepCopy() {
        return new DummyPayload(this.payloadTimeUTC);
    }

    public ByteBuffer getPayloadBacking()
    {
        throw new Error("Unimplemented");
    }

    /**
     * returns the Payload interface type as defined in the PayloadInterfaceRegistry.
     *
     * @return int ... one of the defined types in icecube.daq.payload.PayloadInterfaceRegistry
     */
    public int getPayloadInterfaceType() {
        return -1;
    }

    /**
     * returns the length in bytes of this payload
     */
    public int getPayloadLength() {
        return 0;
    }

    /**
     * gets the UTC time tag of a payload
     */
    public IUTCTime getPayloadTimeUTC() {
        return payloadTimeUTC;
    }

    /**
     * returns the Payload type
     */
    public int getPayloadType() {
        return -1;
    }

    public void loadPayload()
        throws IOException, DataFormatException
    {
        // do nothing
    }

    public void recycle()
    {
        // do nothing
    }

    public void setCache(IByteBufferCache cache)
    {
        // do nothing
    }

    public String toString()
    {
        return "Dummy@" + payloadTimeUTC;
    }
}
