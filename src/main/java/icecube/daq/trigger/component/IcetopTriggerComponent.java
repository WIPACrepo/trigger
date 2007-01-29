package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IcetopTriggerComponent
    extends TriggerComponent
{

    private static final Log log = LogFactory.getLog(IcetopTriggerComponent.class);

    private static final String COMPONENT_NAME = DAQCmdInterface.DAQ_ICETOP_TRIGGER;
    private static final int COMPONENT_ID = 0;

    public IcetopTriggerComponent() {
        super(COMPONENT_NAME, COMPONENT_ID);
    }

    public static void main(String[] args) throws DAQCompException {
        new DAQCompServer(new IcetopTriggerComponent(), args);
    }

}
