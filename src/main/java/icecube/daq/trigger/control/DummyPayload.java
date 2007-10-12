/*
 * class: DummyPayload
 *
 * Version $Id: DummyPayload.java 2125 2007-10-12 18:27:05Z ksb $
 *
 * Date: October 7 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.splicer.Spliceable;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;

/**
 * This class is a dummy payload that only has a UTC time associated with it.
 * Its main purpose is for truncating the Splicer.
 *
 * @version $Id: DummyPayload.java 2125 2007-10-12 18:27:05Z ksb $
 * @author pat
 */
public class DummyPayload implements Spliceable, IPayload
{

    private IUTCTime payloadTimeUTC;

    public DummyPayload(IUTCTime payloadTimeUTC) {
        this.payloadTimeUTC = payloadTimeUTC;
    }

    /**
     * returns the length in bytes of this payload
     */
    public int getPayloadLength() {
        return 0;
    }

    /**
     * returns the Payload type
     */
    public int getPayloadType() {
        return -1;
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
     * gets the UTC time tag of a payload
     */
    public IUTCTime getPayloadTimeUTC() {
        return payloadTimeUTC;
    }

    public int compareTo(Object object) {
        return payloadTimeUTC.compareTo(((IPayload) object).getPayloadTimeUTC());
    }

    /**
     * This method allows a deepCopy of itself.
     *
     * @return Object which is a copy of the object which implements this interface.
     */
    public Object deepCopy() {
        return new DummyPayload(this.payloadTimeUTC);
    }

}
