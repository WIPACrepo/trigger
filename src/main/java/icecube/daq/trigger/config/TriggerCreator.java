package icecube.daq.trigger.config;

import icecube.daq.payload.ISourceID;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.common.ITriggerAlgorithm;
import icecube.daq.trigger.exceptions.ConfigException;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.util.JAXPUtil;
import icecube.daq.util.JAXPUtilException;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
    public static void buildTriggers(Document doc, int compId,
                                     List<ITriggerAlgorithm> trigList,
                                     List<INewAlgorithm> extraList)
        throws TriggerException
    {
        NodeList nodeList;
        try {
            nodeList =
                JAXPUtil.extractNodeList(doc, "activeTriggers/triggerConfig");
        } catch (JAXPUtilException jux) {
            throw new TriggerException(jux);
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node n = nodeList.item(i);

            int srcId = Integer.parseInt(getElementText(n, "sourceId"));
            if (compId != srcId && extraList == null) {
                // this algorithm is for a different trigger handler;
                //  if we don't want to save all configured algorithms, skip it
                continue;
            }

            String name = getElementText(n, "triggerName");

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

            int cfgId = Integer.parseInt(getElementText(n, "triggerConfigId"));
            trig.setTriggerConfigId(cfgId);

            int trigType = Integer.parseInt(getElementText(n, "triggerType"));
            trig.setTriggerType(trigType);

            trig.setSourceId(srcId);

            NodeList paramList;
            try {
                paramList = JAXPUtil.extractNodeList(n, "parameterConfig");
            } catch (JAXPUtilException jux) {
                throw new TriggerException(jux);
            }

            for (int j = 0; j < paramList.getLength(); j++) {
                Node pnode = paramList.item(j);

                String pname = getElementText(pnode, "parameterName");
                String pval = getElementText(pnode, "parameterValue");
                trig.addParameter(pname, pval);
            }

            NodeList rdoutList;
            try {
                rdoutList = JAXPUtil.extractNodeList(n, "readoutConfig");
            } catch (JAXPUtilException jux) {
                throw new TriggerException(jux);
            }

            for (int j = 0; j < rdoutList.getLength(); j++) {
                Node rnode = rdoutList.item(j);

                int rtype = Integer.parseInt(getElementText(rnode,
                                                            "readoutType"));
                int roff = Integer.parseInt(getElementText(rnode,
                                                           "timeOffset"));
                int rminus = Integer.parseInt(getElementText(rnode,
                                                             "timeMinus"));
                int rplus = Integer.parseInt(getElementText(rnode,
                                                            "timePlus"));
                trig.addReadout(rtype, roff, rminus, rplus);
            }

            if (!trig.isConfigured()) {
                throw new ConfigException("Trigger " + name +
                                          " is not fully configured");
            }

            if (compId == srcId) {
                trigList.add(trig);
            } else if (extraList != null) {
                extraList.add(trig);
            }
        }
    }

    private static String getElementText(Node n, String tag)
    {
        if (n.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }

        NodeList list = ((Element) n).getElementsByTagName(tag);
        if (list.getLength() == 0) {
            return null;
        }

        String val = "";
        for (int i = 0; i < list.getLength(); i++) {
            val += list.item(i).getTextContent();
        }

        return val;
    }
}
