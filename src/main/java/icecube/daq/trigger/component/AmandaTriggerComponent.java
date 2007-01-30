package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AmandaTriggerComponent
    extends TriggerComponent
{

    private static final Log log = LogFactory.getLog(AmandaTriggerComponent.class);

    private static final String COMPONENT_NAME = DAQCmdInterface.DAQ_AMANDA_TRIGGER;
    private static final int COMPONENT_ID = 0;

    public AmandaTriggerComponent() {
        super(COMPONENT_NAME, COMPONENT_ID);
    }

    public static void main(String[] args) throws DAQCompException {
        new DAQCompServer(new AmandaTriggerComponent(), args);
    }

}
