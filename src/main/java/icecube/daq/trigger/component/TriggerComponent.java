package icecube.daq.trigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.SimpleOutputEngine;
import icecube.daq.io.SpliceablePayloadReader;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQComponent;
import icecube.daq.juggler.component.DAQConnector;
import icecube.daq.juggler.mbean.MemoryStatistics;
import icecube.daq.juggler.mbean.SystemStatistics;
import icecube.daq.oldpayload.impl.MasterPayloadFactory;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.oldpayload.impl.TriggerRequestRecord;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.VitreousBufferCache;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.SpliceableFactory;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.config.TriggerBuilder;
import icecube.daq.trigger.config.triggers.TriggerConfigType;
import icecube.daq.trigger.control.DummyTriggerManager;
import icecube.daq.trigger.control.GlobalTriggerManager;
import icecube.daq.trigger.control.ITriggerControl;
import icecube.daq.trigger.control.ITriggerManager;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.util.DOMRegistry;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

public class TriggerComponent
    extends DAQComponent
{
    private static final Log log = LogFactory.getLog(TriggerComponent.class);

    public static final String DEFAULT_AMANDA_HOST = "ic-twrdaq00";
    public static final int DEFAULT_AMANDA_PORT = 12014;

    public static final String DEFAULT_STRING_MAP_FILE_NAME = "hexagon_near.geo";

    private ISourceID sourceId;
    private IByteBufferCache inCache;
    private IByteBufferCache outCache;
    private ITriggerManager triggerManager;
    private Splicer splicer;
    private SpliceablePayloadReader inputEngine;
    private DAQComponentOutputProcess outputEngine;

    private String globalConfigurationDir;
    private String triggerConfigFileName;

    private boolean useDummy;

    public TriggerComponent(String name, int id) {
        this(name, id, DEFAULT_AMANDA_HOST, DEFAULT_AMANDA_PORT);
    }

    public TriggerComponent(String name, int id,
                            String amandaHost, int amandaPort) {
        super(name, id);

        // Create the source id of this component
        sourceId = SourceIdRegistry.getISourceIDFromNameAndId(name, id);

        boolean isGlobalTrigger = false;
        boolean isAmandaTrigger = false;

        // Get input and output types
        String inputType, outputType, shortName;
        if (name.equals(DAQCmdInterface.DAQ_GLOBAL_TRIGGER)) {
            isGlobalTrigger = true;
            inputType = DAQConnector.TYPE_TRIGGER;
            outputType = DAQConnector.TYPE_GLOBAL_TRIGGER;
            shortName = "GTrig";
        } else if (name.equals(DAQCmdInterface.DAQ_INICE_TRIGGER)) {
            inputType = DAQConnector.TYPE_STRING_HIT;
            outputType = DAQConnector.TYPE_TRIGGER;
            shortName = "IITrig";
        } else if (name.equals(DAQCmdInterface.DAQ_ICETOP_TRIGGER)) {
            inputType = DAQConnector.TYPE_ICETOP_HIT;
            outputType = DAQConnector.TYPE_TRIGGER;
            shortName = "ITTrig";
        } else if (name.equals(DAQCmdInterface.DAQ_AMANDA_TRIGGER)) {
            isAmandaTrigger = true;
            inputType = DAQConnector.TYPE_SELF_CONTAINED;
            outputType = DAQConnector.TYPE_TRIGGER;
            shortName = "ATrig";
        } else {
            throw new Error("Unknown trigger " + name);
        }

        //inCache = new VitreousBufferCache(shortName + "IN");
        inCache = new VitreousBufferCache(shortName + "IN", Long.MAX_VALUE);
        addCache(inCache);
        //addMBean("inCache", inCache);

        outCache = new VitreousBufferCache(shortName + "OUT");
        addCache(outputType, outCache);
        //addMBean("outCache", inCache);

        addMBean("jvm", new MemoryStatistics());
        addMBean("system", new SystemStatistics());

        SpliceableFactory factory = new MasterPayloadFactory(inCache);

        TriggerRequestPayloadFactory trFactory =
            new TriggerRequestPayloadFactory();
        trFactory.setByteBufferCache(outCache);

        // Now differentiate
        if (isGlobalTrigger) {
            // Global trigger
            triggerManager =
                new GlobalTriggerManager(factory, sourceId, trFactory);
        } else if (!useDummy) {
            triggerManager = new TriggerManager(factory, sourceId, trFactory);
        } else {
            triggerManager =
                new DummyTriggerManager(factory, sourceId, trFactory);
        }
        addMBean("manager", triggerManager);

        triggerManager.setOutgoingBufferCache(outCache);

        // Create splicer and introduce it to the trigger manager
        splicer = new HKN1Splicer(triggerManager);
        triggerManager.setSplicer(splicer);

        // Create and register input engine
        try {
            inputEngine = new SpliceablePayloadReader(name, 25000, splicer, factory);
        } catch (IOException ioe) {
            log.error("Couldn't create input reader");
            System.exit(1);
            inputEngine = null;
        }
        if (isAmandaTrigger) {
            try {
                inputEngine.addReverseConnection(amandaHost, amandaPort,
                                                 inCache);
            } catch (IOException ioe) {
                log.error("Couldn't connect to Amanda TWR", ioe);
                System.exit(1);
            }
        }
        addMonitoredEngine(inputType, inputEngine);

        // Create and register output engine
        outputEngine = new SimpleOutputEngine(name, id, name + "OutputEngine");
        triggerManager.setPayloadOutput(outputEngine);
        addMonitoredEngine(outputType, outputEngine);

    }

    public void closeAll()
    {
        inputEngine.destroyProcessor();
        outputEngine.destroyProcessor();
    }

    public void flush()
    {
        triggerManager.flush();
    }

    public IByteBufferCache getInputCache()
    {
        return inCache;
    }

    public IByteBufferCache getOutputCache()
    {
        return outCache;
    }

    public long getPayloadsSent()
    {
        return ((SimpleOutputEngine) outputEngine).getTotalRecordsSent();
    }

    public SpliceablePayloadReader getReader()
    {
        return inputEngine;
    }

    public Splicer getSplicer()
    {
        return splicer;
    }

    public DAQComponentOutputProcess getWriter()
    {
        return outputEngine;
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

    	// Setup DOMRegistry as 1st thing to do ...
    	try {
    		triggerManager.setDOMRegistry(DOMRegistry.loadRegistry(globalConfigurationDir));
    		log.info("loaded DOM registry");
    	}
    	catch (Exception ex) {
    		throw new DAQCompException("Error loading DOM registry", ex);
    	}

	// Also create the string map
	String stringMapFile = globalConfigurationDir + "/" + DEFAULT_STRING_MAP_FILE_NAME;
	triggerManager.createStringMap(stringMapFile);

        // Lookup the trigger configuration
        String globalConfigurationFileName = globalConfigurationDir + "/" +
            configName;;
        File cfgFile = new File(globalConfigurationFileName);
        if (!cfgFile.isFile()) {
            if (!globalConfigurationFileName.endsWith(".xml")) {
                globalConfigurationFileName += ".xml";
                cfgFile = new File(globalConfigurationFileName);
            }

            if (!cfgFile.isFile()) {
                throw new DAQCompException("Configuration file \"" + cfgFile +
                                           "\" does not exist");
            }
        }

        String triggerConfiguration;
        try {
            triggerConfiguration = GlobalConfiguration.getTriggerConfig(globalConfigurationFileName);
        } catch (Exception e) {
            log.error("Error extracting trigger configuration name from" +
                      " global configuraion file.", e);
            throw new DAQCompException("Cannot get trigger configuration name.",
                                       e);
        }
        triggerConfigFileName = globalConfigurationDir + "/trigger/" +
            triggerConfiguration;
        File f = new File(triggerConfigFileName);
        if (!f.isFile()) {
            if (!triggerConfigFileName.endsWith(".xml")) {
                triggerConfigFileName += ".xml";
                f = new File(triggerConfigFileName);
            }

            if (!f.isFile()) {
                throw new DAQCompException("Trigger configuration file \"" +
                                           triggerConfigFileName +
                                           "\" (from \"" + configName +
                                           "\") does not exist");
            }
        }

        List triggerConfigs =
            TriggerBuilder.getTriggerConfig(triggerConfigFileName);

        // Add triggers to the trigger manager
        List currentTriggers =
            TriggerBuilder.buildTriggers(triggerConfigs, sourceId);
        Iterator triggerIter = currentTriggers.iterator();
        while (triggerIter.hasNext()) {
            ITriggerControl trigger = (ITriggerControl) triggerIter.next();
            trigger.setTriggerHandler(triggerManager);
        }
        triggerManager.addTriggers(currentTriggers);

        addTriggerNames(triggerConfigs);
    }

    private static final void addTriggerNames(List triggerConfigs)
    {
        int max = 0;

        Iterator triggerIter = triggerConfigs.iterator();
        while (triggerIter.hasNext()) {
            TriggerConfigType cfgType = (TriggerConfigType) triggerIter.next();
            if (max < cfgType.getTriggerType()) {
                max = cfgType.getTriggerType();
            }
        }

        String[] typeNames = new String[max + 1];

        triggerIter = triggerConfigs.iterator();
        while (triggerIter.hasNext()) {
            TriggerConfigType cfgType = (TriggerConfigType) triggerIter.next();

            String trigName = cfgType.getTriggerName();

            int idx = trigName.indexOf("Trigger");
            if (idx > 0) {
                trigName = trigName.substring(0, idx);
            }

            typeNames[cfgType.getTriggerType()] = trigName;
        }

        TriggerRequestRecord.setTypeNames(typeNames);
    }

    public void resetting() throws DAQCompException {
        triggerManager.reset();
    }

    public ITriggerManager getTriggerManager(){
        return triggerManager;
    }

    public String getTriggerConfigFileName(){
        return triggerConfigFileName;
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
	return "$Id: TriggerComponent.java 12666 2011-02-15 21:02:34Z dglo $";
    }
}
