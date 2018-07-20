package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;

/**
 * Trigger handler for evaluating in-ice hits.
 */
public class IniceTriggerComponent
    extends TriggerComponent
{
    /**
     * Create an in-ice hit trigger handler.
     *
     * @throws DAQCompException if component cannot be created
     */
    public IniceTriggerComponent()
        throws DAQCompException
    {
        super(DAQCmdInterface.DAQ_INICE_TRIGGER, 0);
    }

    public static void main(String[] args)
        throws DAQCompException
    {
        DAQCompServer srvr;
        try {
            srvr = new DAQCompServer(new IniceTriggerComponent(), args);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            System.exit(1);
            return; // without this, compiler whines about uninitialized 'srvr'
        }
        srvr.startServing();
    }
}
