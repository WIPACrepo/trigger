package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;

/**
 * Trigger handler for evaluating icetop hits.
 */
public class IcetopTriggerComponent
    extends TriggerComponent
{
    /**
     * Create an icetop hit trigger handler.
     */
    public IcetopTriggerComponent()
    {
        super(DAQCmdInterface.DAQ_ICETOP_TRIGGER, 0);
    }

    public static void main(String[] args)
        throws DAQCompException
    {
        DAQCompServer srvr;
        try {
            srvr = new DAQCompServer(new IcetopTriggerComponent(), args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
            return; // without this, compiler whines about uninitialized 'srvr'
        }
        srvr.startServing();
    }
}
