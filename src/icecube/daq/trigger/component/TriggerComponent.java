package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.io.PayloadDestinationOutputEngine;
import icecube.daq.io.SpliceablePayloadInputEngine;
import icecube.daq.juggler.component.DAQComponent;
import icecube.daq.juggler.component.DAQConnector;
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


public class TriggerComponent
    extends DAQComponent
{

    protected MasterPayloadFactory masterFactory;
    protected Splicer splicer;
    protected ITriggerManager triggerManager;
    protected ISourceID sourceId;
    protected SpliceablePayloadInputEngine inputEngine;
    protected PayloadDestinationOutputEngine outputEngine;

    public TriggerComponent(String name, int id) {
        super(name, id);

        // First do things common to all trigger components
        sourceId = SourceIdRegistry.getISourceIDFromNameAndId(name, id);
        IByteBufferCache bufferCache =
                new ByteBufferCache(256, 50000000L, 5000000L, name);
        addCache(bufferCache);
        masterFactory = new MasterPayloadFactory(bufferCache);

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

        splicer = new SplicerImpl(triggerManager);
        triggerManager.setSplicer(splicer);
        inputEngine = new SpliceablePayloadInputEngine(name, id,
                                                       name + "InputEngine",
                                                       splicer,
                                                       masterFactory);
        addEngine(inputType, inputEngine);
        outputEngine = new PayloadDestinationOutputEngine(name, id,
                                                          name + "OutputEngine");
        outputEngine.registerBufferManager(bufferCache);    
        triggerManager.setPayloadDestinationCollection(outputEngine.getPayloadDestinationCollection());
        addEngine(outputType, outputEngine);
    }

    public ITriggerManager getTriggerManager(){
        return triggerManager;
    }

    public ISourceID getSourceID(){
        return sourceId;
    }
}
