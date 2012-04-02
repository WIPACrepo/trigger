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
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.VitreousBufferCache;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.SpliceableFactory;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.algorithm.ITrigger;
import icecube.daq.trigger.config.TriggerBuilder;
import icecube.daq.trigger.control.DummyTriggerManager;
import icecube.daq.trigger.control.GlobalTriggerManager;
import icecube.daq.trigger.control.ITriggerManager;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.util.DOMRegistry;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private File triggerConfigFile;

    private boolean useDummy;
    private boolean isGlobalTrigger;

    public TriggerComponent(String name, int id) {
        this(name, id, DEFAULT_AMANDA_HOST, DEFAULT_AMANDA_PORT);
    }

    public TriggerComponent(String name, int id,
                            String amandaHost, int amandaPort) {
        super(name, id);

        // Create the source id of this component
        sourceId = SourceIdRegistry.getISourceIDFromNameAndId(name, id);

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
            triggerManager = new GlobalTriggerManager(sourceId, trFactory);
        } else if (!useDummy) {
            triggerManager = new TriggerManager(sourceId, trFactory);
        } else {
            triggerManager =
                new DummyTriggerManager(sourceId, trFactory);
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

    public long getPayloadsReceived()
    {
        return inputEngine.getTotalRecordsReceived();
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

        // Initialize DOMRegistry
        DOMRegistry registry;
        try {
            registry = DOMRegistry.loadRegistry(globalConfigurationDir);
            log.info("loaded DOM registry");
        } catch (Exception ex) {
            throw new DAQCompException("Error loading DOM registry", ex);
        }
        triggerManager.setDOMRegistry(registry);

        // Also create the string map
        File stringMapFile = new File(globalConfigurationDir,
                                      DEFAULT_STRING_MAP_FILE_NAME);
        triggerManager.createStringMap(stringMapFile);

        // Build the trigger configuration directory
        File cfgFile = new File(globalConfigurationDir, configName);
        if (!cfgFile.isFile()) {
            if (!configName.endsWith(".xml")) {
                cfgFile = new File(globalConfigurationDir,
                                   configName + ".xml");
            }

            if (!cfgFile.isFile()) {
                throw new DAQCompException("Configuration file \"" + cfgFile +
                                           "\" does not exist");
            }
        }

        // Lookup the trigger configuration
        String triggerConfiguration;
        try {
            triggerConfiguration = TriggerBuilder.getTriggerConfig(cfgFile);
        } catch (Exception e) {
            log.error("Error extracting trigger configuration name from" +
                      " global configuraion file.", e);
            throw new DAQCompException("Cannot get trigger configuration name.",
                                       e);
        }

        File triggerConfigDir = new File(globalConfigurationDir, "trigger");
        triggerConfigFile = new File(triggerConfigDir, triggerConfiguration);
        if (!triggerConfigFile.isFile()) {
            if (!triggerConfiguration.endsWith(".xml")) {
                triggerConfigFile =
                    new File(triggerConfigDir, triggerConfiguration + ".xml");
            }

            if (!triggerConfigFile.isFile()) {
                throw new DAQCompException("Trigger configuration file \"" +
                                           triggerConfigFile +
                                           "\" (from \"" + configName +
                                           "\") does not exist");
            }
        }

        // Add triggers to the trigger manager
        List<ITrigger> currentTriggers;
        try {
            currentTriggers =
                TriggerBuilder.buildTriggers(triggerConfigFile, sourceId);
        } catch (TriggerException te) {
            throw new DAQCompException("Cannot build triggers from \"" +
                                       triggerConfigFile + "\" for " +
                                       sourceId, te);
        }

        if (currentTriggers.size()  == 0) {
            throw new DAQCompException("No triggers specified in \"" +
                                       triggerConfigFile + "\" for " +
                                       sourceId);
        }

        for (ITrigger trigger : currentTriggers) {
            trigger.setTriggerHandler(triggerManager);
        }
        triggerManager.addTriggers(currentTriggers);

        addTriggerNames(currentTriggers);
    }

    private static final void addTriggerNames(List<ITrigger> triggers)
    {
        int max = 0;

        for (ITrigger trig : triggers) {
            if (max < trig.getTriggerType()) {
                max = trig.getTriggerType();
            }
        }

        String[] typeNames = new String[max + 1];

        for (ITrigger trig : triggers) {
            String trigName = trig.getTriggerName();

            if (trigName == null) {
                System.err.println("Class " + trig.getClass().getName() + " has no name");
                continue;
            }

            int idx = trigName.indexOf("Trigger");
            if (idx > 0) {
                trigName = trigName.substring(0, idx);
            }

            typeNames[trig.getTriggerType()] = trigName;
        }

        TriggerRequestRecord.setTypeNames(typeNames);
    }

    /**
     * Perform any actions related to switching to a new run.
     *
     * @param runNumber new run number
     *
     * @throws DAQCompException if there is a problem switching the component
     */
    public void switching(int runNumber) throws DAQCompException {
        if (isGlobalTrigger) {
            triggerManager.switchToNewRun();
        }
    }

    public void resetting() throws DAQCompException {
        triggerManager.reset();
    }

    public ITriggerManager getTriggerManager(){
        return triggerManager;
    }

    public File getTriggerConfigFile(){
        return triggerConfigFile;
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
	return "$Id: TriggerComponent.java 13612 2012-04-02 22:16:42Z dglo $";
    }
}
