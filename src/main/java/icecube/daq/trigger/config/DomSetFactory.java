/**
 * Created by IntelliJ IDEA.
 * User: pat
 * Date: Sep 13, 2006
 * Time: 11:42:00 AM
 *
 *
 * Factory object with static method for constructing a DomSet based on
 * an id. Uses the DOMRegistry to get list of domIds and contructs
 * a DomSet.
 *
 */

package icecube.daq.trigger.config;

import icecube.daq.util.DOMRegistry;
import icecube.daq.util.DeployedDOM;

import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class DomSetFactory
{

    /**
     * logging object
     */
    private static final Log log = LogFactory.getLog(DomSetFactory.class);

    /**
     * DOMRegistry
     */
    private static DOMRegistry domRegistry = null;

    public static void setDomRegistry(DOMRegistry dr) {
	domRegistry = dr;
    }


    /**
     * Get a DomSet based on this domSetId
     * @param domSetId id of DomSet stored in db
     * @return returns a DomSet, or null if domSetId does not exist
     */
    public static DomSet getDomSet(int domSetId) {

        // check the DOMRegistry
        if (domRegistry == null) {
            log.warn("Must set the DOMRegistry first");
            return null;
        }

        if (domSetId == 0) {
            // AMANDA SYNC (0,91)
            boolean good = false;
            ArrayList<String> domIds = new ArrayList<String>();
            
            Set<DeployedDOM> doms = domRegistry.getDomsOnString(0);
            for (DeployedDOM dom: doms) {
                int pos = dom.getStringMinor();
                if (pos == 91) {
                    // this is our baby
                    String domId = dom.getMainboardId();
                    domIds.add(domId);
                    good = true;
                }
            }

            if (good) {
                return new DomSet("AMANDA_SYNC", domIds);
            } else {
                log.error("Failed to create DomSet for domSetId=0 (AMANDA_SYNC)");
                return null;
            }

        } else if (domSetId == 1) {
            // AMANDA TRIG (0,92)
            boolean good = false;
            ArrayList<String> domIds = new ArrayList<String>();

            Set<DeployedDOM> doms = domRegistry.getDomsOnString(0);
            for (DeployedDOM dom: doms) {
                int pos = dom.getStringMinor();
                if (pos == 92) {
                    // this is our baby
                    String domId = dom.getMainboardId();
                    domIds.add(domId);
                    good = true;
                }
            }

            if (good) {
                return new DomSet("AMANDA_TRIG", domIds);
            } else {
                log.error("Failed to create DomSet for domSetId=1 (AMANDA_TRIG)");
                return null;
            }


        } else if (domSetId == 2) {
            // InIce (1,1)-(1,60):(78,1)-(78,60)
            ArrayList<String> domIds = new ArrayList<String>();

            for (int str=1; str<=78; str++) {
                Set<DeployedDOM> doms = domRegistry.getDomsOnString(str);
                for (DeployedDOM dom: doms) {
                    int pos = dom.getStringMinor();
                    if ( (pos >= 1) && (pos <= 60) ) {
                        String domId = dom.getMainboardId();
                        domIds.add(domId);
                    }
                }
            }

            if (!domIds.isEmpty()) {
                return new DomSet("INICE", domIds);
            } else {
                log.error("Failed to create DomSet for domSetId=2 (INICE)");
                return null;
            }

        } else if (domSetId == 3) {
            // IceTop (1,61)-(1,64):(80,61)-(80,64)
            ArrayList<String> domIds = new ArrayList<String>();

            for (int str=201; str<=220; str++) {
                Set<DeployedDOM> doms = domRegistry.getDomsOnString(str);
                for (DeployedDOM dom: doms) {
                    int pos = dom.getStringMinor();
                    if ( (pos >= 61) && (pos <= 64) ) {
                        String domId = dom.getMainboardId();
                        domIds.add(domId);
                    }
                }
            }

            if (!domIds.isEmpty()) {
                return new DomSet("ICETOP", domIds);
            } else {
                log.error("Failed to create DomSet for domSetId=3 (ICETOP)");
                return null;
            }

        } else if ( (domSetId == 4) || (domSetId == 5) ) {
            // DeepCore
            ArrayList<String> domIds = new ArrayList<String>();

            // InIce strings are 26,27,35,36,37,45,46
	    // DomSet 4 used 41-60
	    // DomSet 5 will use 39-60
            int minPos = 41;
	    if (domSetId == 5) minPos = 39;
            int maxPos = 60;

            // 26
            Set<DeployedDOM> doms = domRegistry.getDomsOnString(26);
            for (DeployedDOM dom: doms) {
                int pos = dom.getStringMinor();
                if ( (pos >= minPos) && (pos <= maxPos) ) {
                    String domId = dom.getMainboardId();
                    domIds.add(domId);
                }
            }

            // 27
            doms = domRegistry.getDomsOnString(27);
            for (DeployedDOM dom: doms) {
                int pos = dom.getStringMinor();
                if ( (pos >= minPos) && (pos <= maxPos) ) {
                    String domId = dom.getMainboardId();
                    domIds.add(domId);
                }
            }

            // 35
            doms = domRegistry.getDomsOnString(35);
            for (DeployedDOM dom: doms) {
                int pos = dom.getStringMinor();
                if ( (pos >= minPos) && (pos <= maxPos) ) {
                    String domId = dom.getMainboardId();
                    domIds.add(domId);
                }
            }

            // 36
            doms = domRegistry.getDomsOnString(36);
            for (DeployedDOM dom: doms) {
                int pos = dom.getStringMinor();
                if ( (pos >= minPos) && (pos <= maxPos) ) {
                    String domId = dom.getMainboardId();
                    domIds.add(domId);
                }
            }

            // 37
            doms = domRegistry.getDomsOnString(37);
            for (DeployedDOM dom: doms) {
                int pos = dom.getStringMinor();
                if ( (pos >= minPos) && (pos <= maxPos) ) {
                    String domId = dom.getMainboardId();
                    domIds.add(domId);
                }
            }

            // 45
            doms = domRegistry.getDomsOnString(45);
            for (DeployedDOM dom: doms) {
                int pos = dom.getStringMinor();
                if ( (pos >= minPos) && (pos <= maxPos) ) {
                    String domId = dom.getMainboardId();
                    domIds.add(domId);
                }
            }

            // 46
            doms = domRegistry.getDomsOnString(46);
            for (DeployedDOM dom: doms) {
                int pos = dom.getStringMinor();
                if ( (pos >= minPos) && (pos <= maxPos) ) {
                    String domId = dom.getMainboardId();
                    domIds.add(domId);
                }
            }

            // DeepCore strings are 79-86
            minPos = 11;
            maxPos = 60;

            for (int str=79; str<=86; str++) {
                doms = domRegistry.getDomsOnString(str);
                for (DeployedDOM dom: doms) {
                    int pos = dom.getStringMinor();
                    if ( (pos >= minPos) && (pos <= maxPos) ) {
                        String domId = dom.getMainboardId();
                        domIds.add(domId);
                    }
                }
            }

            if (!domIds.isEmpty()) {
                return new DomSet("DEEPCORE", domIds);
            } else {
		if (domSetId == 4)
		    log.error("Failed to create DomSet for domSetId=4 (DEEPCORE)");
		else if (domSetId == 5)
		    log.error("Failed to create DomSet for domSetId=5 (DEEPCORE)");
                return null;
            }

        } else {
            if (domSetId != -1) {
                log.error("Invalid DomSetId! must be 0,1,2,3,or 4, not " +
                        domSetId);
            }

            return null;

        }

    }

}
