package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.io.PayloadDestinationOutputEngine;
import icecube.daq.io.SpliceablePayloadReader;
import icecube.daq.juggler.component.DAQComponent;
import icecube.daq.juggler.component.DAQConnector;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.mbean.MemoryStatistics;
import icecube.daq.juggler.mbean.SystemStatistics;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.MasterPayloadFactory;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.VitreousBufferCache;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerImpl;
import icecube.daq.splicer.SpliceableFactory;
import icecube.daq.trigger.config.TriggerBuilder;
import icecube.daq.trigger.control.ITriggerManager;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.trigger.control.GlobalTriggerManager;
import icecube.daq.trigger.control.ITriggerControl;
import icecube.daq.trigger.control.DummyTriggerManager;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.util.DOMRegistry;

import java.nio.ByteBuffer;
import java.io.IOException;

import java.util.List;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class TriggerComponent
    extends DAQComponent
{
    private static final Log log = LogFactory.getLog(TriggerComponent.class);

    public static final String DEFAULT_AMANDA_HOST = "ic-twrdaq00";
    public static final int DEFAULT_AMANDA_PORT = 12014;

    protected ISourceID sourceId;
    protected IByteBufferCache bufferCache;
    protected ITriggerManager triggerManager;
    protected Splicer splicer;
    protected SpliceablePayloadReader inputEngine;
    protected PayloadDestinationOutputEngine outputEngine;

    protected String globalConfigurationDir;
    protected String triggerConfigFileName;
    protected List currentTriggers;

    private String amandaHost = DEFAULT_AMANDA_HOST;
    private int amandaPort = DEFAULT_AMANDA_PORT;

    private boolean useCacheMaxVal;
    private boolean useDummy;

    public TriggerComponent(String name, int id) {
        this(name, id, DEFAULT_AMANDA_HOST, DEFAULT_AMANDA_PORT);
    }

    public TriggerComponent(String name, int id,
                            String amandaHost, int amandaPort) {
        super(name, id);
        
        this.amandaHost = amandaHost;
        this.amandaPort = amandaPort;

        // Create the source id of this component
        sourceId = SourceIdRegistry.getISourceIDFromNameAndId(name, id);

        if (!useCacheMaxVal) {
            bufferCache = new VitreousBufferCache();
        } else {
            bufferCache = new VitreousBufferCache(Long.MAX_VALUE);
        }
        
        addCache(bufferCache);
        //addMBean("bufferCache", bufferCache);

        addMBean("jvm", new MemoryStatistics());
        addMBean("system", new SystemStatistics());

        SpliceableFactory factory;

        // Now differentiate
        String inputType, outputType;
        if (name.equals(DAQCmdInterface.DAQ_GLOBAL_TRIGGER)) {
            factory = new MasterPayloadFactory(bufferCache);

            // Global trigger
            triggerManager = new GlobalTriggerManager(factory, sourceId);

            inputType = DAQConnector.TYPE_TRIGGER;
            outputType = DAQConnector.TYPE_GLOBAL_TRIGGER;
        } else {
            factory = new MasterPayloadFactory(bufferCache);

            // Sub-detector triggers
            if (!useDummy) {
                triggerManager = new TriggerManager(factory, sourceId);
            } else {
                triggerManager = new DummyTriggerManager(factory, sourceId);
            }

            IByteBufferCache trigCache = new VitreousBufferCache();
            addCache(DAQConnector.TYPE_TRIGGER, trigCache);

            MasterPayloadFactory mpf =
                new MasterPayloadFactory(trigCache);
            final int trId = PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST;
            TriggerRequestPayloadFactory trFactory =
                (TriggerRequestPayloadFactory) mpf.getPayloadFactory(trId);
            triggerManager.setOutputFactory(trFactory);

            if (name.equals(DAQCmdInterface.DAQ_INICE_TRIGGER)) {
                inputType = DAQConnector.TYPE_STRING_HIT;
                outputType = DAQConnector.TYPE_TRIGGER;
            } else if (name.equals(DAQCmdInterface.DAQ_ICETOP_TRIGGER)) {
                inputType = DAQConnector.TYPE_ICETOP_HIT;
                outputType = DAQConnector.TYPE_TRIGGER;
            } else if (name.equals(DAQCmdInterface.DAQ_AMANDA_TRIGGER)) {
                inputType = DAQConnector.TYPE_SELF_CONTAINED;
                outputType = DAQConnector.TYPE_TRIGGER;
            } else {
                // Unknown name?
                inputType = "";
                outputType = "";
            }
        }

        // Create splicer and introduce it to the trigger manager
        splicer = new HKN1Splicer(triggerManager);
        triggerManager.setSplicer(splicer);

        // Create and register io engines
        try {
            inputEngine = new SpliceablePayloadReader(name, splicer, factory);
        } catch (IOException ioe) {
            log.error("Couldn't create input reader");
            System.exit(1);
            inputEngine = null;
        }
        if (name.equals(DAQCmdInterface.DAQ_AMANDA_TRIGGER)) {
            try {
                inputEngine.addReverseConnection(amandaHost, amandaPort,
                                                 bufferCache);
            } catch (IOException ioe) {
                log.error("Couldn't connect to Amanda TWR", ioe);
                System.exit(1);
            }
        }
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

	// Setup DOMRegistry
	try {
	    triggerManager.setDOMRegistry(DOMRegistry.loadRegistry(globalConfigurationDir));
	} catch (ParserConfigurationException pce) {
	    log.error("Error loading the DOMRegistry", pce);
	} catch (SAXException se) {
	    log.error("Error loading the DOMRegistry", se);
	} catch (IOException ioe) {
	    log.error("Error loading the DOMRegistry", ioe);
	}

    }

    public ITriggerManager getTriggerManager(){
        return triggerManager;
    }

    public ISourceID getSourceID(){
        return sourceId;
    }

    /**
     * Return this component's svn version info as a String.
     *
     * @return svn version id as a String
     */
    public String getVersionInfo()
    {
	return "$Id: TriggerComponent.java 2319 2007-11-27 18:35:03Z jacobsen $";
    }
}
