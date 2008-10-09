package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;

public class AmandaTriggerComponent
    extends TriggerComponent
{

    private static final String COMPONENT_NAME = DAQCmdInterface.DAQ_AMANDA_TRIGGER;
    private static final int COMPONENT_ID = 0;

    public AmandaTriggerComponent() {
        super(COMPONENT_NAME, COMPONENT_ID);
    }

    public AmandaTriggerComponent(String amandaHost, int amandaPort) {
        super(COMPONENT_NAME, COMPONENT_ID, amandaHost, amandaPort);
    }

    public static void main(String[] args) throws DAQCompException {
        DAQCompServer srvr;
        try {
            srvr = new DAQCompServer(new AmandaTriggerComponent(), args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
            return; // without this, compiler whines about uninitialized 'srvr'
        }
        srvr.startServing();
    }

}
