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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
//import org.xml.sax.SAXException;

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

    private String configDir;

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
     * Close all files.
     */
    public void closeAll()
    {
        inputEngine.destroyProcessor();
        outputEngine.destroyProcessor();
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
            DomSetFactory.setConfigurationDirectory(configDir);
        } catch (TriggerException ex) {
            throw new DAQCompException("Bad trigger configuration directory",
                                       ex);
        }

        File runConfig;
        if (configName.endsWith(".xml")) {
            runConfig = new File(configDir, configName);
        } else {
            runConfig = new File(configDir, configName + ".xml");
        }

        if (!runConfig.exists()) {
            throw new DAQCompException("Cannot find run configuration file \"" +
                                       runConfig + "\"");
        }

        String tcName = readTriggerConfigName(runConfig);
        System.err.println("Reading trigger config from " + tcName);


        File trigCfgDir = new File(configDir, "trigger");
        if (!trigCfgDir.exists()) {
            throw new DAQCompException("Cannot find trigger configuration" +
                                       " directory \"" +
                                       trigCfgDir + "\"");
        }

        File trigConfig;
        if (tcName.endsWith(".xml")) {
            trigConfig = new File(trigCfgDir, tcName);
        } else {
            trigConfig = new File(trigCfgDir, tcName + ".xml");
        }

        if (!trigConfig.exists()) {
            throw new DAQCompException("Cannot find trigger configuration" +
                                       " file \"" + trigConfig + "\"");
        }

        algorithms = readTriggers(trigConfig, sourceId);

        if (algorithms.size()  == 0) {
            throw new DAQCompException("No triggers specified in \"" +
                                       trigConfig + "\" for " +
                                       sourceId);
        }

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
        return "$Id: TriggerComponent.java 14207 2013-02-11 22:18:48Z dglo $";
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
     * Extract the name of the trigger configuration file from the
     * run configuration file.
     *
     * @param runConfig run configuration file
     *
     * @return name of trigger configuration file
     *
     * @throws DAQCompException if there was a problem
     */
    private static String readTriggerConfigName(File runConfig)
        throws DAQCompException
    {
        FileInputStream in;
        try {
            in = new FileInputStream(runConfig);
        } catch (IOException ioe) {
            throw new DAQCompException("Cannot open run configuration" +
                                       " file \"" + runConfig + "\"", ioe);
        }

        try {
            SAXReader rdr = new SAXReader();
            Document doc;
            try {
                doc = rdr.read(in);
            } catch (DocumentException de) {
                throw new DAQCompException("Cannot read run configuration" +
                                           " file \"" + runConfig + "\"", de);
            }

            Node tcNode = doc.selectSingleNode("runConfig/triggerConfig");
            if (tcNode == null) {
                throw new DAQCompException("Run configuration file \"" +
                                           runConfig + " does not contain" +
                                           " <triggerConfig>");
            }

            return TriggerCreator.getNodeText((Branch) tcNode);
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                // ignore errors on close
            }
        }
    }

    /**
     * Read all the trigger algorithm specifications.
     *
     * @param triggerConfig trigger configuration XML file
     * @param srcId source ID of trigger handler being configured
     *
     * @return list of trigger algorithms
     */
    private static List<ITriggerAlgorithm> readTriggers(File triggerConfig,
                                                        ISourceID srcId)
        throws DAQCompException
    {
        if (srcId == null) {
            throw new DAQCompException("Source ID has not been set");
        }

        FileInputStream in;
        try {
            in = new FileInputStream(triggerConfig);
        } catch (IOException ioe) {
            throw new DAQCompException("Cannot open trigger configuration" +
                                       " file \"" + triggerConfig + "\"", ioe);
        }

        try {
            SAXReader rdr = new SAXReader();
            Document doc;
            try {
                doc = rdr.read(in);
            } catch (DocumentException de) {
                throw new DAQCompException("Cannot read run configuration" +
                                           " file \"" + triggerConfig + "\"",
                                           de);
            }

            try {
                return TriggerCreator.buildTriggers(doc, srcId);
            } catch (TriggerException te) {
                throw new DAQCompException("Cannot build triggers", te);
            }
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                // ignore errors on close
            }
        }
    }

    /**
     * Set the location of the global configuration directory.
     *
     * @param dirName absolute path of configuration directory
     */
    public void setGlobalConfigurationDir(String dirName)
    {
        configDir = dirName;
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
            triggerManager.switchToNewRun();
        }
    }
}
