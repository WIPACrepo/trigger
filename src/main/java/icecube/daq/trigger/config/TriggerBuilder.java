package icecube.daq.trigger.config;

import icecube.daq.payload.ISourceID;
import icecube.daq.payload.impl.SourceID;
import icecube.daq.trigger.algorithm.ITrigger;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.TriggerException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

/**
 * Configuration file utility
 */
public class TriggerBuilder
{
    /**
     * Build and configure all triggers from a configuration file
     *
     * @param triggerConfig location of trigger configuration file
     * @param srcId source ID of trigger component
     *
     * @throws TriggerException if there is a problem
     */
    public static List<ITrigger> buildTriggers(File triggerConfig,
                                               ISourceID srcId)
        throws TriggerException
    {
        // open trigger config file
        FileInputStream in;
        try {
            in = new FileInputStream(triggerConfig);
        } catch (IOException ioe) {
            throw new ConfigException("Cannot open trigger configuration" +
                                       " file \"" + triggerConfig + "\"", ioe);
        }

        // load and configure all triggers
        try {
            SAXReader rdr = new SAXReader();
            Document doc;
            try {
                doc = rdr.read(in);
            } catch (DocumentException de) {
                throw new ConfigException("Cannot read run configuration" +
                                           " file \"" + triggerConfig + "\"",
                                           de);
            }

            return TriggerBuilder.buildTriggers(doc, srcId);
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                // ignore errors on close
            }
        }
    }

    /**
     * Build and configure all triggers specified in the XML document
     *
     * @param doc XML document
     * @param componentId source ID of trigger component
     *
     * @return list of configured triggers
     *
     * @throws TriggerException if there is a problem
     */
    public static List<ITrigger> buildTriggers(Document doc,
                                               ISourceID componentId)
        throws TriggerException
    {
        ArrayList<ITrigger> trigList = new ArrayList<ITrigger>();

        List<Node> nodeList = doc.selectNodes("activeTriggers/triggerConfig");
        for (Node n : nodeList) {

            int srcId = Integer.parseInt(n.valueOf("sourceId"));
            if (srcId != componentId.getSourceID()) {
                continue;
            }

            String name = n.valueOf("triggerName");

            ITrigger trig;
            try {
                Class trigClass =
                    Class.forName("icecube.daq.trigger.algorithm." + name);
                trig = (ITrigger) trigClass.newInstance();
            } catch (Exception ex) {
                throw new ConfigException("Cannot load trigger \"" + name +
                                          "\"", ex);
            }

            trig.setTriggerName(name);
            trig.setSourceId(new SourceID(srcId));

            int cfgId = Integer.parseInt(n.valueOf("triggerConfigId"));
            trig.setTriggerConfigId(cfgId);

            int trigType = Integer.parseInt(n.valueOf("triggerType"));
            trig.setTriggerType(trigType);

            List<Node> paramList = n.selectNodes("parameterConfig");
            for (Node pnode : paramList) {
                String pname = pnode.valueOf("parameterName");
                String pval = pnode.valueOf("parameterValue");
                trig.addParameter(new TriggerParameter(pname, pval));
            }

            List<Node> rdoutList = n.selectNodes("readoutConfig");
            for (Node rnode : rdoutList) {
                int rtype = Integer.parseInt(rnode.valueOf("readoutType"));
                int roff = Integer.parseInt(rnode.valueOf("timeOffset"));
                int rminus = Integer.parseInt(rnode.valueOf("timeMinus"));
                int rplus = Integer.parseInt(rnode.valueOf("timePlus"));
                trig.addReadout(new TriggerReadout(rtype, roff, rminus,
                                                   rplus));
            }

            if (!trig.isConfigured()) {
                throw new ConfigException("Trigger " + name +
                                          " is not fully configured");
            }

            trigList.add(trig);
        }

        return trigList;
    }

    /**
     * Get the text associated with an XML node.
     *
     * @param branch node containing text
     */
    public static String getNodeText(Branch branch)
    {
        StringBuilder str = new StringBuilder();

        for (Iterator iter = branch.nodeIterator(); iter.hasNext(); ) {
            Node node = (Node) iter.next();

            if (node.getNodeType() != Node.TEXT_NODE) {
                continue;
            }

            str.append(node.getText());
        }

        return str.toString().trim();
    }

    /**
     * Extract the name of the trigger configuration file from a run
     * configuration file.
     *
     * @param runConfig location of run configuration file
     *
     * @throws ConfigException if there is a problem
     */
    public static String getTriggerConfig(File runConfig)
        throws ConfigException
    {
        if (!runConfig.exists()) {
            throw new ConfigException("Cannot find run configuration file \"" +
                                      runConfig + "\"");
        }

        FileInputStream in;
        try {
            in = new FileInputStream(runConfig);
        } catch (IOException ioe) {
            throw new ConfigException("Cannot open run configuration file \"" +
                                      runConfig + "\"", ioe);
        }

        try {
            SAXReader rdr = new SAXReader();
            Document doc;
            try {
                doc = rdr.read(in);
            } catch (DocumentException de) {
                throw new ConfigException("Cannot read run configuration" +
                                          " file \"" + runConfig + "\"", de);
            }

            Node tcNode = doc.selectSingleNode("runConfig/triggerConfig");
            if (tcNode == null) {
                throw new ConfigException("Run configuration file \"" +
                                          runConfig + " does not contain" +
                                          " <triggerConfig>");
            }

            return getNodeText((Branch) tcNode);
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                // ignore errors on close
            }
        }
    }
}
