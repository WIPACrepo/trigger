package icecube.daq.trigger.config;

import icecube.daq.payload.ISourceID;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.common.ITriggerAlgorithm;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.TriggerException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.Node;

/**
 * Create and configure all trigger algorithm objects.
 */
public abstract class TriggerCreator
{
    /**
     * Build and configure all the specified trigger algorithms.
     *
     * @param doc XML document
     * @param compId component source ID
     *
     * @return list of configured trigger algorithms
     *
     * @throws TriggerException if there is a problem
     */
    public static List<ITriggerAlgorithm> buildTriggers(Document doc,
                                                        ISourceID compId)
        throws TriggerException
    {
        ArrayList<ITriggerAlgorithm> trigList =
            new ArrayList<ITriggerAlgorithm>();

        List<Node> nodeList = doc.selectNodes("activeTriggers/triggerConfig");
        for (Node n : nodeList) {
            int srcId = Integer.parseInt(n.valueOf("sourceId"));
            if (compId != null && compId.getSourceID() != srcId) {
                continue;
            }

            String name = n.valueOf("triggerName");

            INewAlgorithm trig;
            try {
                Class trigClass =
                    Class.forName("icecube.daq.trigger.algorithm." + name);
                trig = (INewAlgorithm) trigClass.newInstance();
            } catch (Exception ex) {
                throw new ConfigException("Cannot load trigger \"" + name +
                                          "\"", ex);
            }

            trig.setTriggerName(name);

            int cfgId = Integer.parseInt(n.valueOf("triggerConfigId"));
            trig.setTriggerConfigId(cfgId);

            int trigType = Integer.parseInt(n.valueOf("triggerType"));
            trig.setTriggerType(trigType);

            trig.setSourceId(srcId);

            List<Node> paramList = n.selectNodes("parameterConfig");
            for (Node pnode : paramList) {
                String pname = pnode.valueOf("parameterName");
                String pval = pnode.valueOf("parameterValue");
                trig.addParameter(pname, pval);
            }

            List<Node> rdoutList = n.selectNodes("readoutConfig");
            for (Node rnode : rdoutList) {
                int rtype = Integer.parseInt(rnode.valueOf("readoutType"));
                int roff = Integer.parseInt(rnode.valueOf("timeOffset"));
                int rminus = Integer.parseInt(rnode.valueOf("timeMinus"));
                int rplus = Integer.parseInt(rnode.valueOf("timePlus"));
                trig.addReadout(rtype, roff, rminus, rplus);
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
     * Build a text string from all the text nodes in <tt>branch</tt>.
     *
     * @param branch XML document branch containing a text string
     *
     * @return trimmed text string
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
}
