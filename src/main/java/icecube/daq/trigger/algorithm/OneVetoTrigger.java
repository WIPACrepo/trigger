/*
 * class: OneVetoTrigger
 *
 * Version $Id: OneVetoTrigger.java,v 1.2 2006/06/30 18:27:53 dwharton Exp $
 *
 * Date: January 25 2006
 *
 * (c) 2006 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.trigger.config.TriggerParameter;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is accept all incoming triggers but to veto one configured triggers.
 *
 * @version $Id: OneVetoTrigger.java,v 1.2 2006/06/30 18:27:53 dwharton Exp $
 * @author shseo
 */
public class OneVetoTrigger
        extends VetoTrigger
{
    /**
    * Log object for this class
    */
    private static final Log log = LogFactory.getLog(OneVetoTrigger.class);

    private int miConfiguredTriggerType_1;
    private int miConfiguredTriggerConfigId_1;
    private int miConfiguredSourceId_1;

    private static final int miUnconfiguredTrigId = -1;
    private static final int miConfiguredTrigId_1 = 1;

    boolean mbConfigTriggerType_1 = false;
    boolean mbConfigTriggerConfigId_1 = false;
    boolean mbConfigSourceId_1 = false;

    private final int NUMBER_OF_REQUIRED_CONFIG_PARAMETERS = 3;
    int miCurrentNumberOfConfigurationParameters = 0;

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public OneVetoTrigger()
    {
        super();
    }
    /**
     * This method is to provide triggerId which will be used for coincidence triggering.
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

        }else
        {
            iTriggerId = miUnconfiguredTrigId;
        }

        return iTriggerId;
    }

    public List getConfiguredTriggerIDs()
    {
        List listConfiguredTriggerIDs = new ArrayList();
        listConfiguredTriggerIDs.add(new Integer(miConfiguredTrigId_1));

        return listConfiguredTriggerIDs;
    }

    public boolean isConfiguredTrigger(ITriggerRequestPayload tPayload) {
        int iTrigId = getTriggerId(tPayload);
        if(iTrigId != miUnconfiguredTrigId)
        {
            return true;
        }
         return false;
    }

    public boolean isConfigured() {
        boolean bIsConfigured = mbConfigTriggerType_1 &&
                                mbConfigTriggerConfigId_1 &&
                                mbConfigSourceId_1;

        return bIsConfigured;

    }
    /**
     * This method performs configuration.
     *
     * @param parameter TriggerParameter object
     * @throws icecube.daq.trigger.exceptions.UnknownParameterException
     */
    public void addParameter(TriggerParameter parameter) throws UnknownParameterException, IllegalParameterValueException
    {
        miCurrentNumberOfConfigurationParameters++;

        String paramName = parameter.getName();
        String paramValue = parameter.getValue();

        if (paramName.compareTo("triggerType1") == 0) {
            miConfiguredTriggerType_1 = Integer.parseInt(paramValue);
            //mlistConfiguredTriggerType.add(new Integer(Integer.parseInt(paramValue)));
            mbConfigTriggerType_1 = true;
        } else if (paramName.compareTo("triggerConfigId1") == 0) {
            miConfiguredTriggerConfigId_1 = Integer.parseInt(paramValue);
            //mlistConfiguredTriggerConfigId.add(new Integer(Integer.parseInt(paramValue)));
            mbConfigTriggerConfigId_1 = true;
        } else if (paramName.compareTo("sourceId1") == 0) {
            miConfiguredSourceId_1 = Integer.parseInt(paramValue);
            //mlistConfiguredSourceId.add(new Integer(Integer.parseInt(paramValue)));
            mbConfigSourceId_1 = true;
        } else {
            throw new UnknownParameterException("Unknown parameter: " + paramName);
        }

        super.addParameter(parameter);

        if(miCurrentNumberOfConfigurationParameters == NUMBER_OF_REQUIRED_CONFIG_PARAMETERS){
            if(!isConfigured())
            {
                log.error("OneVetoTrigger was NOT properly configured!");
            }else{
                log.info("OneVetoTrigger was properly configured!");
            }
        }

    }
}
