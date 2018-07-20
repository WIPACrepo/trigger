package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;

/**
 * Trigger handler for merging together all local triggers
 */
public class GlobalTriggerComponent
    extends TriggerComponent
{
    /**
     * Create a global trigger handler.
     *
     * @throws DAQCompException if component cannot be created
     */
    public GlobalTriggerComponent()
        throws DAQCompException
    {
        super(DAQCmdInterface.DAQ_GLOBAL_TRIGGER, 0);
    }

    public static void main(String[] args)
        throws DAQCompException
    {
        DAQCompServer srvr;
        try {
            srvr = new DAQCompServer(new GlobalTriggerComponent(), args);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            System.exit(1);
            return; // without this, compiler whines about uninitialized 'srvr'
        }
        srvr.startServing();
    }
}
