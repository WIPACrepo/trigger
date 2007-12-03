/*
 * interface: IPayloadProducer
 *
 * Version $Id: IPayloadProducer.java 2351 2007-12-03 17:19:40Z dglo $
 *
 * Date: October 19 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.payload.IPayloadOutput;

/**
 * This interface provides methods for setting payload destinations.
 *
 * @version $Id: IPayloadProducer.java 2351 2007-12-03 17:19:40Z dglo $
 * @author pat
 */
public interface IPayloadProducer
{

    void setPayloadOutput(IPayloadOutput payloadOutput);

}
