package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.io.PayloadDestinationOutputEngine;
import icecube.daq.io.SpliceablePayloadInputEngine;
import icecube.daq.juggler.component.DAQComponent;
import icecube.daq.juggler.component.DAQConnector;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.payload.MasterPayloadFactory;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ByteBufferCache;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerImpl;
import icecube.daq.trigger.control.ITriggerManager;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.trigger.control.GlobalTriggerManager;
import icecube.daq.trigger.control.ITriggerControl;
import icecube.daq.trigger.config.TriggerBuilder;

import java.util.List;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class TriggerComponent
    extends DAQComponent
{

    private static final Log log = LogFactory.getLog(TriggerComponent.class);

    protected ISourceID sourceId;
    protected IByteBufferCache bufferCache;
    protected ITriggerManager triggerManager;
    protected Splicer splicer;
    protected SpliceablePayloadInputEngine inputEngine;
    protected PayloadDestinationOutputEngine outputEngine;

    protected String globalConfigurationDir = null;
    protected String triggerConfigFileName = null;
    protected List currentTriggers = null;


    public TriggerComponent(String name, int id) {
        super(name, id);

        // Create the source id of this component
        sourceId = SourceIdRegistry.getISourceIDFromNameAndId(name, id);

        // Create the buffer cache and the payload factory
        bufferCache = new ByteBufferCache(256, 50000000L, 5000000L, name);
        addCache(bufferCache);
        MasterPayloadFactory masterFactory = new MasterPayloadFactory(bufferCache);

        // Now differentiate
        String inputType, outputType;
        if (name.equals(DAQCmdInterface.DAQ_GLOBAL_TRIGGER)) {

            // Global trigger
            triggerManager = new GlobalTriggerManager(masterFactory, sourceId);

            inputType = DAQConnector.TYPE_TRIGGER;
            outputType = DAQConnector.TYPE_GLOBAL_TRIGGER;
        } else {

            // Sub-detector triggers
            triggerManager = new TriggerManager(masterFactory, sourceId);

            if (name.equals(DAQCmdInterface.DAQ_INICE_TRIGGER)) {
                inputType = DAQConnector.TYPE_STRING_HIT;
                outputType = DAQConnector.TYPE_TRIGGER;
            } else if (name.equals(DAQCmdInterface.DAQ_ICETOP_TRIGGER)) {
                inputType = DAQConnector.TYPE_ICETOP_HIT;
                outputType = DAQConnector.TYPE_TRIGGER;
            } else if (name.equals(DAQCmdInterface.DAQ_AMANDA_TRIGGER)) {
                // todo: What should the inputType be for AMANDA???
                inputType = DAQConnector.TYPE_TEST_HIT;
                outputType = DAQConnector.TYPE_TRIGGER;
            } else {
                // Unknown name?
                inputType = "";
                outputType = "";
            }
        }

        // Create splicer and introduce it to the trigger manager
        splicer = new SplicerImpl(triggerManager);
        triggerManager.setSplicer(splicer);

        // Create and register io engines
        inputEngine = new SpliceablePayloadInputEngine(name, id,
                                                       name + "InputEngine",
                                                       splicer,
                                                       masterFactory);
        addMonitoredEngine(inputType, inputEngine);
        outputEngine = new PayloadDestinationOutputEngine(name, id,
                                                          name + "OutputEngine");
        outputEngine.registerBufferManager(bufferCache);
        triggerManager.setPayloadDestinationCollection(outputEngine.getPayloadDestinationCollection());
        addMonitoredEngine(outputType, outputEngine);

    }

    /**
     * Tell trigger or other component where top level XML configuration tree lives
     */
    public void setGlobalConfigurationDir(String dirName) {
        globalConfigurationDir = dirName;
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

        // Lookup the trigger configuration
        String triggerConfiguration = null;
        String globalConfigurationFileName = globalConfigurationDir + "/" + configName + ".xml";
        try {
            triggerConfiguration = GlobalConfiguration.getTriggerConfig(globalConfigurationFileName);
        } catch (Exception e) {
            log.error("Error extracting trigger configuration name from global configuraion file.", e);
            throw new DAQCompException("Cannot get trigger configuration name.", e);
        }
        triggerConfigFileName = globalConfigurationDir + "/trigger/" + triggerConfiguration + ".xml";

        // Add triggers to the trigger manager
        currentTriggers = TriggerBuilder.buildTriggers(triggerConfigFileName, sourceId);
        Iterator triggerIter = currentTriggers.iterator();
        while (triggerIter.hasNext()) {
            ITriggerControl trigger = (ITriggerControl) triggerIter.next();
            trigger.setTriggerHandler(triggerManager);
        }
        triggerManager.addTriggers(currentTriggers);

    }

    public ITriggerManager getTriggerManager(){
        return triggerManager;
    }

    public ISourceID getSourceID(){
        return sourceId;
    }
}
