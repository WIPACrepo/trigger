/*
 * class: ThreeCoincidenceTrigger
 *
 * Version $Id: ThreeCoincidenceTrigger.java 2125 2007-10-12 18:27:05Z ksb $
 *
 * Date: September 6 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.control.ConditionalTriggerBag;
import icecube.daq.trigger.config.TriggerParameter;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Three coincidence trigger algorithm which uses timeGate (using CoincidenceTriggerBag)
 * for safe release of a selected trigger.
 *
 * @version $Id: ThreeCoincidenceTrigger.java 2125 2007-10-12 18:27:05Z ksb $
 * @author shseo
 */
public class ThreeCoincidenceTrigger
        extends CoincidenceTrigger
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(ThreeCoincidenceTrigger.class);

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

    private boolean mbConfiguredTriggerType_1 = false;
    private boolean mbConfiguredTriggerConfigId_1 = false;
    private boolean mbConfiguredSourceId_1 = false;

    private boolean mbConfiguredTriggerType_2 = false;
    private boolean mbConfiguredTriggerConfigId_2 = false;
    private boolean mbConfiguredSourceId_2 = false;

    private boolean mbConfiguredTriggerType_3 = false;
    private boolean mbConfiguredTriggerConfigId_3 = false;
    private boolean mbConfiguredSourceId_3 = false;

    private final int NUMBER_OF_REQUIRED_CONFIG_PARAMETERS = 9;
    int miCountConfigurationParameters = 0;

    private boolean flushing = false;
    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public ThreeCoincidenceTrigger()
    {
        super();

        //--make CoincidenceTriggerBag object w/ this specific trigger type, configID, sourceId.
        mtConditionalTriggerBag = new ConditionalTriggerBag(getTriggerType(), getTriggerConfigId(), getSourceId());

        //--set this specific trigger algorithm in the CoincidenceTriggerBag.
        mtConditionalTriggerBag.setConditionalTriggerAlgorithm(this);

    }
    /**
     * This method returns a list of configuredTriggerIDs required by this triggerAlgorithm.
     * @return
     */
    public List getConfiguredTriggerIDs()
    {
        List listConfiguredTriggerIDs = new ArrayList();
        listConfiguredTriggerIDs.add(new Integer(miConfiguredTrigId_1));
        listConfiguredTriggerIDs.add(new Integer(miConfiguredTrigId_2));
        listConfiguredTriggerIDs.add(new Integer(miConfiguredTrigId_3));

        return listConfiguredTriggerIDs;
    }
    /**
     * Check whether a given trigger is configured or not.:
     * If so, it will be used in ThreeCoincidenceTrigger algorithm.
     *
     * @param tPayload
     * @return
     */
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

    /**
     * This method is to check whether ThreeCoincidenceTrigger was configured or not.
     *
     * @return
     */
    public boolean isConfigured()
    {
        return (mbConfiguredTriggerType_1 && mbConfiguredTriggerType_2 && mbConfiguredTriggerType_3
            && mbConfiguredTriggerConfigId_1 && mbConfiguredTriggerConfigId_2 && mbConfiguredTriggerConfigId_3
            && mbConfiguredSourceId_1 && mbConfiguredSourceId_2 && mbConfiguredSourceId_3);
    }

    /**
     * This method is to obtain trigger configuration information.
     *
     * @param parameter TriggerParameter object
     * @throws UnknownParameterException
     */
    public void addParameter(TriggerParameter parameter) throws UnknownParameterException, IllegalParameterValueException
    {
        miCountConfigurationParameters++;

        String paramName = parameter.getName();
        String paramValue = parameter.getValue();

        if (paramName.compareTo("triggerType1") == 0) {
            miConfiguredTriggerType_1 = Integer.parseInt(paramValue);
            mbConfiguredTriggerType_1 = true;
        } else if (paramName.compareTo("triggerConfigId1") == 0) {
            miConfiguredTriggerConfigId_1 = Integer.parseInt(paramValue);
            mbConfiguredTriggerConfigId_1 = true;
        } else if (paramName.compareTo("sourceId1") == 0) {
            miConfiguredSourceId_1 = Integer.parseInt(paramValue);
            mbConfiguredSourceId_1 = true;
        } else if (paramName.compareTo("triggerType2") == 0) {
            miConfiguredTriggerType_2 = Integer.parseInt(paramValue);
            mbConfiguredTriggerType_2 = true;
        } else if (paramName.compareTo("triggerConfigId2") == 0) {
            miConfiguredTriggerConfigId_2 = Integer.parseInt(paramValue);
            mbConfiguredTriggerConfigId_2 = true;
        } else if (paramName.compareTo("sourceId2") == 0) {
            miConfiguredSourceId_2 = Integer.parseInt(paramValue);
            mbConfiguredSourceId_2 = true;
        } else if (paramName.compareTo("triggerType3") == 0) {
            miConfiguredTriggerType_3 = Integer.parseInt(paramValue);
            mbConfiguredTriggerType_3 = true;
        } else if (paramName.compareTo("triggerConfigId3") == 0) {
            miConfiguredTriggerConfigId_3 = Integer.parseInt(paramValue);
            mbConfiguredTriggerConfigId_3 = true;
        } else if (paramName.compareTo("sourceId3") == 0) {
            miConfiguredSourceId_3 = Integer.parseInt(paramValue);
            mbConfiguredSourceId_3 = true;
        }

        super.addParameter(parameter);

        if(miCountConfigurationParameters == NUMBER_OF_REQUIRED_CONFIG_PARAMETERS){
            if(!isConfigured())
            {
                log.error("ThreecoincidenceTrigger was NOT properly configured!");
            }else{
                log.info("ThreecoincidenceTrigger was properly configured!");
            }
        }

    }
}
