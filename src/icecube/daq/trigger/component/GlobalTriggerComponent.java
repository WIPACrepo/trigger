package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;
import icecube.daq.juggler.mock.MockAppender;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GlobalTriggerComponent
    extends TriggerComponent
{

    private static final Log log = LogFactory.getLog(GlobalTriggerComponent.class);    

    private static final String COMPONENT_NAME = DAQCmdInterface.DAQ_GLOBAL_TRIGGER;
    private static final int COMPONENT_ID = 0;

    public GlobalTriggerComponent() {
        super(COMPONENT_NAME, COMPONENT_ID);
    }

    public static void main(String[] args) throws DAQCompException {
        new DAQCompServer(new TriggerComponent(COMPONENT_NAME, COMPONENT_ID), args);
    }

}
