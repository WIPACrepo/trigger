/*
 * class: ThreeVetoTrigger
 *
 * Version $Id: ThreeVetoTrigger.java 15155 2014-09-22 15:41:57Z dglo $
 *
 * Date: January 25 2006
 *
 * (c) 2006 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is accept all incoming triggers but to veto three configured triggers.
 *
 * @version $Id: ThreeVetoTrigger.java 15155 2014-09-22 15:41:57Z dglo $
 * @author shseo
 */
public class ThreeVetoTrigger
        extends VetoTrigger
{
    /**
    * Log object for this class
    */
    private static final Log LOG =
        LogFactory.getLog(ThreeVetoTrigger.class);
    private int miConfiguredTriggerType_1;
    private int miConfiguredTriggerConfigId_1;
    private int miConfiguredSourceId_1;

    private int miConfiguredTriggerType_2;
    private int miConfiguredTriggerConfigId_2;
    private int miConfiguredSourceId_2;

    private int miConfiguredTriggerType_3;
    private int miConfiguredTriggerConfigId_3;
    private int miConfiguredSourceId_3;


    private static final int miUnconfiguredTrigId = -1;
    private static final int miConfiguredTrigId_1 = 1;
    private static final int miConfiguredTrigId_2 = 2;
    private static final int miConfiguredTrigId_3 = 3;

    private boolean mbConfiguredTriggerType_1;
    private boolean mbConfiguredTriggerConfigId_1;
    private boolean mbConfiguredSourceId_1;

    private boolean mbConfiguredTriggerType_2;
    private boolean mbConfiguredTriggerConfigId_2;
    private boolean mbConfiguredSourceId_2;

    private boolean mbConfiguredTriggerType_3;
    private boolean mbConfiguredTriggerConfigId_3;
    private boolean mbConfiguredSourceId_3;

    private final int NUMBER_OF_REQUIRED_CONFIG_PARAMETERS = 9;
    private int miCountConfigurationParameters;

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public ThreeVetoTrigger()
    {
        super();
    }

    public List getConfiguredTriggerIDs()
    {
        List listConfiguredTriggerIDs = new ArrayList();
        listConfiguredTriggerIDs.add(Integer.valueOf(miConfiguredTrigId_1));
        listConfiguredTriggerIDs.add(Integer.valueOf(miConfiguredTrigId_2));
        listConfiguredTriggerIDs.add(Integer.valueOf(miConfiguredTrigId_3));

        return listConfiguredTriggerIDs;
    }
    /**
     * Return the corresponding triggerID for a given trigger.
     *
     * @param tPayload
     * @return
     */
    public int getTriggerId(ITriggerRequestPayload tPayload)
    {
        int iTriggerId = miUnconfiguredTrigId; //Unconfigured triggerId

        int iTrigType = ((ITriggerRequestPayload) tPayload).getTriggerType();
        int iTrigConfigId = ((ITriggerRequestPayload) tPayload).getTriggerConfigID();
        int iSourceId = ((ITriggerRequestPayload) tPayload).getSourceID().getSourceID();

        if(iTrigType == miConfiguredTriggerType_1
             && iTrigConfigId == miConfiguredTriggerConfigId_1
                && iSourceId == miConfiguredSourceId_1)
        {
            iTriggerId = miConfiguredTrigId_1;

        }else if(iTrigType == miConfiguredTriggerType_2
                 && iTrigConfigId == miConfiguredTriggerConfigId_2
                     && iSourceId == miConfiguredSourceId_2)
        {
            iTriggerId = miConfiguredTrigId_2;

        }else if(iTrigType == miConfiguredTriggerType_3
                 && iTrigConfigId == miConfiguredTriggerConfigId_3
                     && iSourceId == miConfiguredSourceId_3)
        {
            iTriggerId = miConfiguredTrigId_3;
        }

        return iTriggerId;
    }

    public boolean isConfiguredTrigger(ITriggerRequestPayload tPayload)
    {
        boolean bIsConfiguredPayload = false;

        int iTrigId = getTriggerId(tPayload);

        if(iTrigId != miUnconfiguredTrigId)
        {
            bIsConfiguredPayload = true;
        }

         return bIsConfiguredPayload;

    }

    public boolean isConfigured()
    {
        return (mbConfiguredTriggerType_1 && mbConfiguredTriggerType_2 && mbConfiguredTriggerType_3
            && mbConfiguredTriggerConfigId_1 && mbConfiguredTriggerConfigId_2 && mbConfiguredTriggerConfigId_3
            && mbConfiguredSourceId_1 && mbConfiguredSourceId_2 && mbConfiguredSourceId_3);

    }
    /**
     * Add a trigger parameter.
     *
     * @param name parameter name
     * @param value parameter value
     *
     * @throws UnknownParameterException if the parameter is unknown
     * @throws IllegalParameterValueException if the parameter value is bad
     */
    public void addParameter(String name, String value)
        throws UnknownParameterException, IllegalParameterValueException
    {
        miCountConfigurationParameters++;

        String paramName = name;
        String paramValue = value;

        if (paramName.compareTo("getTriggerType()1") == 0) {
            miConfiguredTriggerType_1 = Integer.parseInt(paramValue);
            mbConfiguredTriggerType_1 = true;
        } else if (paramName.compareTo("getTriggerConfigId()1") == 0) {
            miConfiguredTriggerConfigId_1 = Integer.parseInt(paramValue);
            mbConfiguredTriggerConfigId_1 = true;
        } else if (paramName.compareTo("getSourceId()1") == 0) {
            miConfiguredSourceId_1 = Integer.parseInt(paramValue);
            mbConfiguredSourceId_1 = true;
        } else if (paramName.compareTo("getTriggerType()2") == 0) {
            miConfiguredTriggerType_2 = Integer.parseInt(paramValue);
            mbConfiguredTriggerType_2 = true;
        } else if (paramName.compareTo("getTriggerConfigId()2") == 0) {
            miConfiguredTriggerConfigId_2 = Integer.parseInt(paramValue);
            mbConfiguredTriggerConfigId_2 = true;
        } else if (paramName.compareTo("getSourceId()2") == 0) {
            miConfiguredSourceId_2 = Integer.parseInt(paramValue);
            mbConfiguredSourceId_2 = true;
        } else if (paramName.compareTo("getTriggerType()3") == 0) {
            miConfiguredTriggerType_3 = Integer.parseInt(paramValue);
            mbConfiguredTriggerType_3 = true;
        } else if (paramName.compareTo("getTriggerConfigId()3") == 0) {
            miConfiguredTriggerConfigId_3 = Integer.parseInt(paramValue);
            mbConfiguredTriggerConfigId_3 = true;
        } else if (paramName.compareTo("getSourceId()3") == 0) {
            miConfiguredSourceId_3 = Integer.parseInt(paramValue);
            mbConfiguredSourceId_3 = true;
        }

        super.addParameter(name, value);

        if(miCountConfigurationParameters == NUMBER_OF_REQUIRED_CONFIG_PARAMETERS){
            if(!isConfigured())
            {
                LOG.error("ThreecoincidenceTrigger was NOT properly configured!");
            }else{
                LOG.info("ThreecoincidenceTrigger was properly configured!");
            }
        }

    }
}
