package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.config.ITriggerConfig;
import icecube.daq.trigger.config.TriggerBuilder;
import icecube.daq.trigger.config.TriggerReadout;
import icecube.daq.trigger.control.GlobalTriggerManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GlobalTriggerComponent
    extends TriggerComponent
{

    private static final String COMPONENT_NAME = DAQCmdInterface.DAQ_GLOBAL_TRIGGER;
    private static final int COMPONENT_ID = 0;

    public GlobalTriggerComponent() {
        super(COMPONENT_NAME, COMPONENT_ID);
    }

    /**
     * Configure a component using the specified configuration name.
     *
     * @param configName configuration name
     *
     * @throws icecube.daq.juggler.component.DAQCompException
     *          if there is a problem configuring
     */
    public void configuring(String configName) throws DAQCompException {
        super.configuring(configName);

        // Now get the maximum readout length
        List readouts = new ArrayList();

        ISourceID iniceSourceId = SourceIdRegistry.getISourceIDFromNameAndId(DAQCmdInterface.DAQ_INICE_TRIGGER, 0);
        readouts.addAll(getReadouts(iniceSourceId));

        ISourceID icetopSourceId = SourceIdRegistry.getISourceIDFromNameAndId(DAQCmdInterface.DAQ_ICETOP_TRIGGER, 0);
        readouts.addAll(getReadouts(icetopSourceId));

        ISourceID amandaSourceId = SourceIdRegistry.getISourceIDFromNameAndId(DAQCmdInterface.DAQ_AMANDA_TRIGGER, 0);
        readouts.addAll(getReadouts(amandaSourceId));

        ((GlobalTriggerManager) getTriggerManager()).setMaxTimeGateWindow(getMaxReadoutTimeEarliest(readouts));

    }

    private List getReadouts(ISourceID sourceId) {
        List readouts = new ArrayList();
        Iterator iter = TriggerBuilder.buildTriggers(getTriggerConfigFileName(), sourceId).iterator();
        while (iter.hasNext()) {
            ITriggerConfig trigger = (ITriggerConfig) iter.next();
            readouts.addAll(trigger.getReadoutList());
        }
        return readouts;
    }

    /**
     * Find maximumReadoutTimeEarliest among configured readoutTimeWindows.
     *
     * @param readoutList list of active subdetector trigger readouts
     * @return maximum readout extent into past
     */
    private int getMaxReadoutTimeEarliest(List readoutList) {

        int maxPastOverall = Integer.MAX_VALUE;

        // loop over triggers
        Iterator readoutIter = readoutList.iterator();
        while (readoutIter.hasNext()) {
            TriggerReadout readout = (TriggerReadout) readoutIter.next();

            // check this readout against the overall earliest
            int maxPast = TriggerReadout.getMaxReadoutPast(readout);
            if (maxPast < maxPastOverall) {
                maxPastOverall = maxPast;
            }
        }

        // todo: Agree on the sign of the returned int.
        return maxPastOverall;
    }

    public static void main(String[] args) throws DAQCompException {
        DAQCompServer srvr;
        try {
            srvr = new DAQCompServer(new GlobalTriggerComponent(), args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
            return; // without this, compiler whines about uninitialized 'srvr'
        }
        srvr.startServing();
    }

}
