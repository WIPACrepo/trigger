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
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.PayloadFactory;
import icecube.daq.payload.impl.VitreousBufferCache;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.SpliceableFactory;
import icecube.daq.splicer.Splicer;
import icecube.daq.trigger.common.DAQTriggerComponent;
import icecube.daq.trigger.common.ITriggerAlgorithm;
import icecube.daq.trigger.common.ITriggerManager;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.trigger.config.TriggerCreator;
import icecube.daq.trigger.control.TriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.util.DOMRegistry;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.Node;

/**
 * Base class for trigger handlers.
 */
public class TriggerComponent
    extends DAQComponent
    implements DAQTriggerComponent
{
    /** Message logger. */
    private static final Log LOG =
        LogFactory.getLog(TriggerComponent.class);

    private ISourceID sourceId;

    private TriggerManager triggerManager;

    private IByteBufferCache inCache;
    private IByteBufferCache outCache;
    private Splicer splicer;
    private SpliceablePayloadReader inputEngine;
    private SimpleOutputEngine outputEngine;

    private boolean isGlobalTrigger;

    private File configDir;

    private List<ITriggerAlgorithm> algorithms;

    /**
     * Create a trigger hander component
     *
     * @param name component name
     * @param id component ID (usually <tt>0</tt> for trigger handlers)
     */
    public TriggerComponent(String name, int id)
    {
        super(name, id);

        // Create the source id of this component
        sourceId = SourceIdRegistry.getISourceIDFromNameAndId(name, id);

        // Get input and output types
        String inputType;
        String outputType;
        String shortName;
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
        } else {
            throw new Error("Unknown trigger " + name);
        }

        inCache = new VitreousBufferCache(shortName + "IN", Long.MAX_VALUE);
        addCache(inCache);

        outCache = new VitreousBufferCache(shortName + "OUT");
        addCache(outputType, outCache);

        addMBean("jvm", new MemoryStatistics());
        addMBean("system", new SystemStatistics());

        SpliceableFactory factory = new PayloadFactory(inCache);

        triggerManager = new TriggerManager(sourceId, outCache);
        addMBean("manager", triggerManager);

        // Create splicer and introduce it to the trigger manager
        splicer = new HKN1Splicer(triggerManager);
        triggerManager.setSplicer(splicer);

        // Create and register input engine
        try {
            inputEngine = new SpliceablePayloadReader(name, 25000, splicer,
                                                      factory);
        } catch (IOException ioe) {
            LOG.error("Couldn't create input reader");
            System.exit(1);
            inputEngine = null;
        }
        addMonitoredEngine(inputType, inputEngine);

        // Create and register output engine
        outputEngine = new SimpleOutputEngine(name, id, name + "OutputEngine");
        triggerManager.setOutputEngine(outputEngine);
        addMonitoredEngine(outputType, outputEngine);
    }

    /**
     * Close all open files, sockets, etc.
     *
     * @throws IOException if there is a problem
     */
    public void closeAll()
        throws IOException
    {
        inputEngine.destroyProcessor();
        outputEngine.destroyProcessor();

        super.closeAll();
    }

    /**
     * Configure this component using the specified run configuration file.
     *
     * @param configName base run configuration file name
     *                   (with or without trailing ".xml")
     *
     * @throws DAQCompException if there is a problem
     */
    public void configuring(String configName)
        throws DAQCompException
    {
        if (configDir == null) {
            throw new DAQCompException("Global configuration directory has" +
                                       " not been set");
        }

        // Initialize DOMRegistry
        DOMRegistry registry;
        try {
            registry = DOMRegistry.loadRegistry(configDir);
            LOG.info("loaded DOM registry");
        } catch (Exception ex) {
            throw new DAQCompException("Error loading DOM registry", ex);
        }
        triggerManager.setDOMRegistry(registry);

        // Inform DomSetFactory of the configuration directory location
        try {
            DomSetFactory.setConfigurationDirectory(configDir.getPath());
        } catch (TriggerException ex) {
            throw new DAQCompException("Bad trigger configuration directory",
                                       ex);
        }

        Document doc = loadXMLDocument(configDir, configName);

        Node tcNode = doc.selectSingleNode("runConfig/triggerConfig");
        if (tcNode == null) {
            throw new DAQCompException("Run configuration file \"" +
                                       configName + "\" does not contain" +
                                       " <triggerConfig>");
        }

        String tcName = TriggerCreator.getNodeText((Branch) tcNode);

        File trigCfgDir = new File(configDir, "trigger");
        if (!trigCfgDir.exists()) {
            throw new DAQCompException("Cannot find trigger configuration" +
                                       " directory \"" +
                                       trigCfgDir + "\"");
        }

        if (sourceId == null) {
            throw new DAQCompException("Source ID has not been set");
        }

        Document tcDoc = loadXMLDocument(trigCfgDir, tcName);
        try {
            algorithms = TriggerCreator.buildTriggers(tcDoc, sourceId);
        } catch (TriggerException te) {
            throw new DAQCompException("Cannot build triggers", te);
        }

        if (algorithms.size()  == 0) {
            throw new DAQCompException("No triggers specified in \"" +
                                       tcName + "\" for " + sourceId);
        }

        for (ITriggerAlgorithm a : algorithms) {
            addMBean(a.getTriggerName(), a);
        }

        triggerManager.setAlerter(getAlerter());
        triggerManager.addTriggers(algorithms);
    }

    /**
     * Attempt to send any cached trigger requests.
     */
    public void flushTriggers()
    {
        triggerManager.flush();
    }

    /**
     * Get the list of configured algorithms.
     *
     * @return list of algorithms
     */
    public List<ITriggerAlgorithm> getAlgorithms()
    {
        return algorithms;
    }

    /**
     * Get the ByteBufferCache used to track the incoming hit payloads
     *
     * @return input cache
     */
    public IByteBufferCache getInputCache()
    {
        return inCache;
    }

    /**
     * Get the trigger counts for detector monitoring.
     *
     *
     * @return list of trigger count data.
     */
    public List<Map> getMoniCounts()
    {
        return triggerManager.getMoniCounts();
    }

    /**
     * Get the ByteBufferCache used to track the outgoing request payloads
     *
     * @return output cache
     */
    public IByteBufferCache getOutputCache()
    {
        return outCache;
    }

    /**
     * Get the total number of hits which have been queued for processing
     *
     * @return total number of hits
     */
    public long getPayloadsProcessed()
    {
        return triggerManager.getTotalProcessed();
    }

    /**
     * Get the current number of hits/requests received
     *
     * @return hits/requests received
     */
    public long getPayloadsReceived()
    {
        return inputEngine.getTotalRecordsReceived();
    }

    /**
     * Get the current number of trigger requests written out
     *
     * @return requests sent
     */
    public long getPayloadsSent()
    {
        return outputEngine.getTotalRecordsSent();
    }

    /**
     * Get the input reader for this component.
     *
     * @return input reader
     */
    public SpliceablePayloadReader getReader()
    {
        return inputEngine;
    }

    /**
     * Get the input splicer for this component.
     *
     * @return splicer
     */
    public Splicer getSplicer()
    {
        return splicer;
    }

    /**
     * Get the trigger manager for this component.
     *
     * @return trigger manager
     */
    public ITriggerManager getTriggerManager()
    {
        return triggerManager;
    }

    /**
     * Return this component's svn version info as a String.
     *
     * @return svn version id as a String
     */
    public String getVersionInfo()
    {
        return "$Id: TriggerComponent.java 14864 2014-02-26 22:07:00Z dglo $";
    }

    /**
     * Get the output process for this component.
     *
     * @return output process
     */
    public DAQComponentOutputProcess getWriter()
    {
        return outputEngine;
    }

    /**
     * Set the first "good" time for the current run.
     *
     * @param firstTime first "good" time
     */
    public void setFirstGoodTime(long firstTime)
    {
        triggerManager.setFirstGoodTime(firstTime);
    }

    /**
     * Set the location of the global configuration directory.
     *
     * @param dirName absolute path of configuration directory
     */
    public void setGlobalConfigurationDir(String dirName)
    {
        configDir = new File(dirName);

        if (!configDir.exists()) {
            throw new Error("Configuration directory \"" + configDir +
                            "\" does not exist");
        }
    }

    /**
     * Set the last "good" time for the current run.
     *
     * @param lastTime last "good" time
     */
    public void setLastGoodTime(long lastTime)
    {
        triggerManager.setLastGoodTime(lastTime);
    }

    /**
     * Set the initial run number for a "conventional" run.
     *
     * @param runNumber run number
     */
    public void setRunNumber(int runNumber)
    {
        triggerManager.setRunNumber(runNumber);
    }

    /**
     * Send histograms after run has stopped.
     *
     * @throws DAQCompException if there is a problem sending histograms
     */
    public void stopped()
        throws DAQCompException
    {
        if (isGlobalTrigger) {
            triggerManager.sendHistograms();
        }
    }

    /**
     * Perform any actions related to switching to a new run.
     *
     * @param runNumber new run number
     *
     * @throws DAQCompException if there is a problem switching the component
     */
    public void switching(int runNumber)
        throws DAQCompException
    {
        if (isGlobalTrigger) {
            // histograms are sent inside switchToNewRun()
            triggerManager.switchToNewRun(null, runNumber);
        }
    }
}
