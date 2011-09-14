package icecube.daq.trigger.algorithm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: pat
 * Date: Dec 11, 2006
 * Time: 1:29:53 PM
 */
public class AmandaSpaseTrigger
    extends AmandaTrigger
{

    private static final Log log = 
        LogFactory.getLog(AmandaSpaseTrigger.class);

    private static int triggerNumber = 0;

    public AmandaSpaseTrigger()
    {
        triggerNumber++;
        triggerBit = SPASE;
    }

    public void setTriggerName(String triggerName) 
    {
        super.triggerName = triggerName + triggerNumber;
        if (log.isInfoEnabled()) {
            log.info("TriggerName set to " + super.triggerName);
        }
    }


}
