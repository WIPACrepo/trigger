package icecube.daq.trigger.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: pat
 * Date: Feb 6, 2007
 * Time: 12:32:39 PM
 */
public final class TriggerRegistry
{

    private static final Log log = LogFactory.getLog(TriggerRegistry.class);

    private static final Integer SMT_TYPE = new Integer(0);
    private static final Integer  CT_TYPE = new Integer(1);
    private static final Integer MBT_TYPE = new Integer(2);
    private static final Integer  TT_TYPE = new Integer(3);
    private static final Integer FRT_TYPE = new Integer(4);
    private static final Integer SBT_TYPE = new Integer(5);
    private static final Integer TBT_TYPE = new Integer(6);

    private static final String SMT_NAME = "SimpleMajorityTrigger";
    private static final String  CT_NAME = "CalibrationTrigger";
    private static final String MBT_NAME = "MinBiasTrigger";
    private static final String  TT_NAME = "ThroughputTrigger";
    private static final String FRT_NAME = "FixedRateTrigger";
    private static final String SBT_NAME = "SyncBoardTrigger";
    private static final String TBT_NAME = "TrigBoardTrigger";

    private static Map nameByType = null;
    private static Map typeByName = null;

    public static int getTriggerType(String triggerName) {
        if (null == typeByName) createMaps();
        if (typeByName.containsKey(triggerName)) {
           return ((Integer) typeByName.get(triggerName)).intValue();
        } else {
           return -1;
        }
    }

    public static String getTriggerName(int triggerType) {
        if (null == nameByType) createMaps();
        Integer type = new Integer(triggerType);
        if (nameByType.containsKey(type)) {
           return ((String) nameByType.get(type));
        } else {
           return null;
        }
    }

    private static void createMaps() {
        nameByType = new HashMap();
        nameByType.put(SMT_TYPE, SMT_NAME);
        nameByType.put( CT_TYPE,  CT_NAME);
        nameByType.put(MBT_TYPE, MBT_NAME);
        nameByType.put( TT_TYPE,  TT_NAME);
        nameByType.put(FRT_TYPE, FRT_NAME);
        nameByType.put(SBT_TYPE, SBT_NAME);
        nameByType.put(TBT_TYPE, TBT_NAME);

        typeByName = new HashMap();
        typeByName.put(SMT_NAME, SMT_TYPE);
        typeByName.put( CT_NAME,  CT_TYPE);
        typeByName.put(MBT_NAME, MBT_TYPE);
        typeByName.put( TT_NAME,  TT_TYPE);
        typeByName.put(FRT_NAME, FRT_TYPE);
        typeByName.put(SBT_NAME, SBT_TYPE);
        typeByName.put(TBT_NAME, TBT_TYPE);
    }

}
