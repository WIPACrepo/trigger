/*
 * interface: IPayloadProducer
 *
 * Version $Id: IPayloadProducer.java 2904 2008-04-11 17:38:14Z dglo $
 *
 * Date: October 19 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.payload.IPayloadOutput;

/**
 * This interface provides methods for setting payload destinations.
 *
 * @version $Id: IPayloadProducer.java 2904 2008-04-11 17:38:14Z dglo $
 * @author pat
 */
public interface IPayloadProducer
{

    void setPayloadOutput(DAQComponentOutputProcess outProc);

}
