/*
 * class: GlobalReadoutElements
 *
 * Version $Id: GlobalReadoutElements.java, shseo
 *
 * Date: August 5 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.trigger.IReadoutRequestElement;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.splicer.PayloadFactory;

import java.util.*;

/**
 * This class is to produce final ReadoutRequestElement for one GlobalTrigEvent.
 * Required is Input of an unorganized ReadoutRequestelements (Vector).
 * Both timeOverlap and spaceOverlap will be handled here via SimpleMerger.java and SmartMerger.java.
 *
 * @version $Id: GlobalTrigEventReadoutElements.java,v 1.4 2005/09/16 18:13:11 shseo Exp $
 * @author shseo
 */
public class GlobalTrigEventReadoutElements
{
    private TriggerRequestPayloadFactory DEFAULT_TRIGGER_FACTORY = new TriggerRequestPayloadFactory();
    private TriggerRequestPayloadFactory triggerFactory;

    private SimpleMerger mtSimpleMerger;
    private SmartMerger mtSmartMerger;

    private Vector mVecFinalReadoutElements = new Vector();

    private List mListGlobal_II = new ArrayList();
    private List mListString_II = new ArrayList();
    private List mListModule_II = new ArrayList();

    private List mListGlobal_IT = new ArrayList();
    private List mListModule_IT = new ArrayList();

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public GlobalTrigEventReadoutElements()
    {
        mtSimpleMerger = new SimpleMerger();
        mtSmartMerger = new SmartMerger();

        setPayloadFactory(DEFAULT_TRIGGER_FACTORY);
    }

    /**
     * This method is to classify each ReadoutElement according to its ReadoutTypes.
     * This will produce GlobalList, StringList and  ModuleList.
     *
     * @param inputElements
     */
    private void classifyReadoutTypes(List inputElements)
    {
        Iterator iterElements = inputElements.iterator();
        while(iterElements.hasNext())
        {
            IReadoutRequestElement element = (IReadoutRequestElement) iterElements.next();

            int readoutType = element.getReadoutType();

            switch(readoutType){
                //IIIT-GLOBAL needs to be separated out as II-GLOBAL and IT-GLOBAL.
                case IReadoutRequestElement.READOUT_TYPE_GLOBAL:

                    IReadoutRequestElement element_II = triggerFactory.createReadoutRequestElement(
                                                IReadoutRequestElement.READOUT_TYPE_II_GLOBAL,
                                                element.getFirstTimeUTC(),
                                                element.getLastTimeUTC(),
                                                element.getDomID(),
                                                element.getSourceID());

                    IReadoutRequestElement element_IT = triggerFactory.createReadoutRequestElement(
                                                IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL,
                                                element.getFirstTimeUTC(),
                                                element.getLastTimeUTC(),
                                                element.getDomID(),
                                                element.getSourceID());

                    mListGlobal_II.add(element_II);
                    mListGlobal_IT.add(element_IT);
                    break;

                case IReadoutRequestElement.READOUT_TYPE_II_GLOBAL:
                    mListGlobal_II.add(element);
                    break;

                case IReadoutRequestElement.READOUT_TYPE_II_STRING:
                    mListString_II.add(element);
                    break;

                case IReadoutRequestElement.READOUT_TYPE_II_MODULE:
                    mListModule_II.add(element);
                    break;

                case IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL:
                    mListGlobal_IT.add(element);
                    break;

                case IReadoutRequestElement.READOUT_TYPE_IT_MODULE:
                    mListModule_IT.add(element);
                    break;

                default:
                    //todo: error message.....
                    break;
            }

        }

    }
    /**
     * This method is to get all ReadoutTypes in current GlobalTrigEvent.
     *
     * @return
     */
    private List getCurrentEventReadoutTypeLists()
    {
        List listCurrentReadoutTypeLists = new ArrayList();

        if(0 != mListGlobal_II.size()) listCurrentReadoutTypeLists.add(mListGlobal_II);
        if(0 != mListString_II.size()) listCurrentReadoutTypeLists.add(mListString_II);
        if(0 != mListModule_II.size()) listCurrentReadoutTypeLists.add(mListModule_II);

        if(0 != mListGlobal_IT.size()) listCurrentReadoutTypeLists.add(mListGlobal_IT);
        if(0 != mListModule_IT.size()) listCurrentReadoutTypeLists.add(mListModule_IT);

        return listCurrentReadoutTypeLists;
    }
    /**
     * This method is the main method and called by GlobalTrigEventWrapper.java.
     * This returns final vector of ReadoutRequestElements for a GTEvent.
     *
     * @param inputElements
     * @return
     */
    public Vector getManagedFinalReadoutRequestElements(List inputElements)
    {
        initialize();

        classifyReadoutTypes(inputElements);

        //Each element of this list is another list (of ReadoutType) containing ReadoutElements.
        List listCurrentEventReadoutTypeLists = getCurrentEventReadoutTypeLists();
        List listSimpleMergedSameReadoutLists = new ArrayList();
        for(int i=0; i<listCurrentEventReadoutTypeLists.size(); i++)
        {
            List listSameReadoutTypeElements =
                (List) listCurrentEventReadoutTypeLists.get(i);

            if(listSameReadoutTypeElements.size() > 1)
            {
                mtSimpleMerger.merge(listSameReadoutTypeElements);
                listSimpleMergedSameReadoutLists.add(mtSimpleMerger.getListSimpleMergedSameReadoutElements());

            } else
            {
                listSimpleMergedSameReadoutLists.add(listSameReadoutTypeElements);
            }
        }

        if(listSimpleMergedSameReadoutLists.size() > 1)
        {
            mtSmartMerger.merge(listSimpleMergedSameReadoutLists);
            mVecFinalReadoutElements.addAll(mtSmartMerger.getFinalReadoutElementsTimeOrdered_All());

        }else if(1 == listSimpleMergedSameReadoutLists.size()) //--> no need for smartMerge.
        {
            mVecFinalReadoutElements.addAll((List) listSimpleMergedSameReadoutLists.get(0));
        }

        return mVecFinalReadoutElements;
    }
    /**
     * After making each ReadoutRequestElement, do initialize.: Vector, List, etc....
     */
    public void initialize()
    {
        mListGlobal_II.clear();
        mListString_II.clear();
        mListModule_II.clear();
        mListGlobal_IT.clear();
        mListModule_IT.clear();

        mVecFinalReadoutElements.clear();

    }

    public void setPayloadFactory(PayloadFactory triggerFactory) {
       this.triggerFactory = (TriggerRequestPayloadFactory) triggerFactory;
       mtSimpleMerger.setPayloadFactory(triggerFactory);
       mtSmartMerger.setPayloadFactory(triggerFactory);
    }

    public void setTimeGap_option(int iTimeGap_option)
    {
        mtSimpleMerger.setTimeGap_option(iTimeGap_option);
    }

}
