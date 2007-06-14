/*
 * interface: IPayloadProducer
 *
 * Version $Id: IPayloadProducer.java,v 1.2 2006/05/08 02:44:44 toale Exp $
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
 * @version $Id: IPayloadProducer.java,v 1.2 2006/05/08 02:44:44 toale Exp $
 * @author pat
 */
public interface IPayloadProducer
{

    void setPayloadDestinationCollection(IPayloadDestinationCollection payloadDestinationCollection);

}
