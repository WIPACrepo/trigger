package icecube.daq.trigger.control;

import icecube.daq.payload.IByteBufferReceiver;
import icecube.daq.payload.MasterPayloadFactory;

/**
 * Created by IntelliJ IDEA.
 * User: toale
 * Date: Mar 30, 2007
 * Time: 1:29:30 PM
 */
public interface IStringTriggerHandler
        extends ITriggerHandler, IByteBufferReceiver
{

    void setMasterPayloadFactory(MasterPayloadFactory factory);


}
