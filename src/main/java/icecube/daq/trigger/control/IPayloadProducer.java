/*
 * interface: IPayloadProducer
 *
 * Version $Id: IPayloadProducer.java 13874 2012-08-28 19:14:11Z dglo $
 *
 * Date: October 19 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.io.DAQComponentOutputProcess;

/**
 * This interface provides methods for setting payload destinations.
 *
 * @version $Id: IPayloadProducer.java 13874 2012-08-28 19:14:11Z dglo $
 * @author pat
 */
public interface IPayloadProducer
{

    void setPayloadOutput(DAQComponentOutputProcess outProc);

}
