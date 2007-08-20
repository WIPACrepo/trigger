/*
 * class: AbstractGlobalTrigger
 *
 * Version $Id: AbstractGlobalTrigger.java,v 1.19 2006/03/16 19:08:25 shseo Exp $
 *
 * Date: August 30 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.control.GlobalTrigEventWrapper;
import icecube.daq.trigger.control.ConditionalTriggerBag;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.PayloadDestination;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is to provide a common method for all triggers in GT.
 *
 * @version $Id: AbstractGlobalTrigger.java,v 1.19 2006/03/16 19:08:25 shseo Exp $
 * @author shseo
 */
public abstract class AbstractGlobalTrigger extends AbstractTrigger
{
    /**
    * Log object for this class
    */
    private static final Log log = LogFactory.getLog(AbstractGlobalTrigger.class);

    public GlobalTrigEventWrapper mtGlobalTrigEventWrapper = new GlobalTrigEventWrapper();

    public ITriggerRequestPayload mtGlobalTrigEventPayload;

    public List mListSelectedTriggers = new ArrayList();
    public List mListOutputTriggers = new ArrayList();
    /**
     * output destination
     */
    public PayloadDestination payloadDestination;
    public int miMaxTimeGateWindowForCoincidenceTrigger;

    public ConditionalTriggerBag mtConditionalTriggerBag;
    /**
     *  Constructor
      */
    public AbstractGlobalTrigger()
    {
        super();
    }
 /*   public AbstractGlobalTrigger(TriggerConfiguration tTriggerConfiguration)
    {
         super(tTriggerConfiguration);
    }*/
    public void setTriggerFactory(TriggerRequestPayloadFactory triggerFactory)
    {
        this.triggerFactory = triggerFactory;
        mtGlobalTrigEventWrapper.setPayloadFactory(triggerFactory);
    }

    //todo: is this necessary only for ThroughputTrigger....?
    public void wrapTrigger(ITriggerRequestPayload tPayload) throws Exception
    {
        mtGlobalTrigEventWrapper.wrap(tPayload, getTriggerType(), getTriggerConfigId());

        mtGlobalTrigEventPayload = mtGlobalTrigEventWrapper.getGlobalTrigEventPayload_single();
        //mListAvailableTriggersToRelease.add(mtGlobalTrigEventPayload);
        //setAvailableTriggerToRelease();

        //This list is used for JUnitTest purpose only. So it needs periodic flush for a normal run.
        if(mtGlobalTrigEventPayload != null){
            if(mListOutputTriggers.size() <= 100){
                mListOutputTriggers.add(mtGlobalTrigEventPayload);
            }

            //--The firstTime here to set DummyPayload is the earliestReadoutTime.
            DummyPayload dummy = new DummyPayload(mtGlobalTrigEventPayload.getFirstTimeUTC());
            setEarliestPayloadOfInterest(dummy);
/*
            if(null != payloadDestination){
                try {
                    payloadDestination.writePayload(mtGlobalTrigEventPayload);
                } catch (IOException e) {
                    log.error("Couldn't write payload", e);
                }
            }
*/

            //--every wrapped trigger should be reported to GlobalTrigBag.
            reportTrigger((ILoadablePayload) mtGlobalTrigEventPayload);
            mtGlobalTrigEventPayload = null;
        }else{
            throw new NullPointerException("mtGlobalTrigEventPayload is NULL in wrapTrigger()");
        }

       // log.info("Number of GlobalTriggers to Release so far = " + getNumberAvailableTriggerToRelease());
    }
    /**
     * This returns a list of selected triggers by each global trigger algorithm.
     * This is necessary for JUnit test purpose.
     *
     * @return
     */
    public List getListOutputTriggers()
    {
        //--period of flush
        int iRegulator = 100;
        int iTotalOutputTriggers = mListOutputTriggers.size();
        int n = iTotalOutputTriggers/iRegulator;

        //--To flush.
        if(n > 0){
            mListOutputTriggers.removeAll(mListOutputTriggers.subList(0, n*iRegulator));
        }

        return mListOutputTriggers;
    }

    public void setPayloadDestination(PayloadDestination payloadDestination)
    {
        this.payloadDestination = payloadDestination;
    }

    public ConditionalTriggerBag getBag(){
        return mtConditionalTriggerBag;
    }
    public int getTriggerCounter()
    {
        return triggerCounter;
    }

}
