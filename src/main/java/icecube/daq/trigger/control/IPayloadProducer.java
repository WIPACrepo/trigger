/*
 * interface: IPayloadProducer
 *
 * Version $Id: IPayloadProducer.java 2125 2007-10-12 18:27:05Z ksb $
 *
 * Date: October 19 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.IPayloadDestinationCollection;

/**
 * This interface provides methods for setting payload destinations.
 *
 * @version $Id: IPayloadProducer.java 2125 2007-10-12 18:27:05Z ksb $
 * @author pat
 */
public interface IPayloadProducer
{

    void setPayloadDestinationCollection(IPayloadDestinationCollection payloadDestinationCollection);

}
