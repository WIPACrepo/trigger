package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;

public class IniceTriggerComponent
    extends TriggerComponent
{

    private static final String COMPONENT_NAME = DAQCmdInterface.DAQ_INICE_TRIGGER;
    private static final int COMPONENT_ID = 0;

    public IniceTriggerComponent() {
        super(COMPONENT_NAME, COMPONENT_ID);
    }

    public static void main(String[] args) throws DAQCompException {
        new DAQCompServer(new IniceTriggerComponent(), args);
    }

}
