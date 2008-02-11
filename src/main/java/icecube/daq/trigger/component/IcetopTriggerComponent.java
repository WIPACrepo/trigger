package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;

public class IcetopTriggerComponent
    extends TriggerComponent
{

    private static final String COMPONENT_NAME = DAQCmdInterface.DAQ_ICETOP_TRIGGER;
    private static final int COMPONENT_ID = 0;

    public IcetopTriggerComponent() {
        super(COMPONENT_NAME, COMPONENT_ID);
    }

    public static void main(String[] args) throws DAQCompException {
        new DAQCompServer(new IcetopTriggerComponent(), args);
    }

}
