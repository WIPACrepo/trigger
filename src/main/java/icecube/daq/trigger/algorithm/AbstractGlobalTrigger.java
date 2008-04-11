/*
 * class: AbstractGlobalTrigger
 *
 * Version $Id: AbstractGlobalTrigger.java 2904 2008-04-11 17:38:14Z dglo $
 *
 * Date: August 30 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.algorithm;

import icecube.daq.payload.ILoadablePayload;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.control.ConditionalTriggerBag;
import icecube.daq.trigger.control.DummyPayload;
import icecube.daq.trigger.control.GlobalTrigEventWrapper;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is to provide a common method for all triggers in GT.
 *
 * @version $Id: AbstractGlobalTrigger.java 2904 2008-04-11 17:38:14Z dglo $
 * @author shseo
 */
public abstract class AbstractGlobalTrigger extends AbstractTrigger
{
    private GlobalTrigEventWrapper mtGlobalTrigEventWrapper = new GlobalTrigEventWrapper();

    private ITriggerRequestPayload mtGlobalTrigEventPayload;

    private List mListSelectedTriggers = new ArrayList();
    protected List mListOutputTriggers = new ArrayList();
    private int miMaxTimeGateWindowForCoincidenceTrigger;

    protected ConditionalTriggerBag mtConditionalTriggerBag;
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

    public ConditionalTriggerBag getBag(){
        return mtConditionalTriggerBag;
    }
    public int getTriggerCounter()
    {
        return triggerCounter;
    }

}
