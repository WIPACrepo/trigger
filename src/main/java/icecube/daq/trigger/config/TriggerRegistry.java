package icecube.daq.trigger.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: pat
 * Date: Feb 6, 2007
 * Time: 12:32:39 PM
 */
public final class TriggerRegistry
{

    private static final Integer SMT_TYPE   = new Integer(0);
    private static final Integer  CT_TYPE   = new Integer(1);
    private static final Integer MBT_TYPE   = new Integer(2);
    private static final Integer  TT_TYPE   = new Integer(3);
    private static final Integer FRT_TYPE   = new Integer(4);
    private static final Integer SBT_TYPE   = new Integer(5);
    private static final Integer TBT_TYPE   = new Integer(6);
    private static final Integer AFT_TYPE   = new Integer(7);
    private static final Integer AVT_TYPE   = new Integer(8);
    private static final Integer AM18T_TYPE = new Integer(9);
    private static final Integer AM24T_TYPE = new Integer(10);
    private static final Integer AST_TYPE   = new Integer(11);
    private static final Integer  ST_TYPE   = new Integer(12);
    private static final Integer ART_TYPE   = new Integer(13);
    private static final Integer AT0T_TYPE  = new Integer(14);
    private static final Integer ALT_TYPE   = new Integer(15);

    private static final String SMT_NAME   = "SimpleMajorityTrigger";
    private static final String  CT_NAME   = "CalibrationTrigger";
    private static final String MBT_NAME   = "MinBiasTrigger";
    private static final String  TT_NAME   = "ThroughputTrigger";
    private static final String FRT_NAME   = "FixedRateTrigger";
    private static final String SBT_NAME   = "SyncBoardTrigger";
    private static final String TBT_NAME   = "TrigBoardTrigger";
    private static final String AFT_NAME   = "AmandaMFrag20Trigger";
    private static final String AVT_NAME   = "AmandaVolumeTrigger";
    private static final String AM18T_NAME = "AmandaM18Trigger";
    private static final String AM24T_NAME = "AmandaM24Trigger";
    private static final String AST_NAME   = "AmandaStringTrigger";
    private static final String  ST_NAME   = "AmandaSpaseTrigger";
    private static final String ART_NAME   = "AmandaRandomTrigger";
    private static final String AT0T_NAME  = "AmandaCalibT0Trigger";
    private static final String ALT_NAME   = "AmandaCalibLaserTrigger";

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
        nameByType.put(  SMT_TYPE,   SMT_NAME);
        nameByType.put(   CT_TYPE,    CT_NAME);
        nameByType.put(  MBT_TYPE,   MBT_NAME);
        nameByType.put(   TT_TYPE,    TT_NAME);
        nameByType.put(  FRT_TYPE,   FRT_NAME);
        nameByType.put(  SBT_TYPE,   SBT_NAME);
        nameByType.put(  TBT_TYPE,   TBT_NAME);
        nameByType.put(  AFT_TYPE,   AFT_NAME);
        nameByType.put(  AVT_TYPE,   AVT_NAME);
        nameByType.put(AM18T_TYPE, AM18T_NAME);
        nameByType.put(AM24T_TYPE, AM24T_NAME);
        nameByType.put(  AST_TYPE,   AST_NAME);
        nameByType.put(   ST_TYPE,    ST_NAME);
        nameByType.put(  ART_TYPE,   ART_NAME);
        nameByType.put( AT0T_TYPE,  AT0T_NAME);
        nameByType.put(  ALT_TYPE,   ALT_NAME);

        typeByName = new HashMap();
        typeByName.put(  SMT_NAME,   SMT_TYPE);
        typeByName.put(   CT_NAME,    CT_TYPE);
        typeByName.put(  MBT_NAME,   MBT_TYPE);
        typeByName.put(   TT_NAME,    TT_TYPE);
        typeByName.put(  FRT_NAME,   FRT_TYPE);
        typeByName.put(  SBT_NAME,   SBT_TYPE);
        typeByName.put(  TBT_NAME,   TBT_TYPE);
        typeByName.put(  AFT_NAME,   AFT_TYPE);
        typeByName.put(  AVT_NAME,   AVT_TYPE);
        typeByName.put(AM18T_NAME, AM18T_TYPE);
        typeByName.put(AM24T_NAME, AM24T_TYPE);
        typeByName.put(  AST_NAME,   AST_TYPE);
        typeByName.put(   ST_NAME,    ST_TYPE);
        typeByName.put(  ART_NAME,   ART_TYPE);
        typeByName.put( AT0T_NAME,  AT0T_TYPE);
        typeByName.put(  ALT_NAME,   ALT_TYPE);
    }

    public static int getTriggerConfigId(List parameterList, List readoutList) {
        String configString = "";

        Iterator parameterIter = parameterList.iterator();
        while (parameterIter.hasNext()) {
            TriggerParameter parameter = (TriggerParameter) parameterIter.next();
            configString += (parameter.toString() + "\n");
        }

        Iterator readoutIter = readoutList.iterator();
        while (readoutIter.hasNext()) {
            TriggerReadout readout = (TriggerReadout) readoutIter.next();
            configString += (readout.toString() + "\n");
        }

        return configString.hashCode();
    }

}
