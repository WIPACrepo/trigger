/*
 * class: smartMerger
 *
 * Version $Id: smartMerger.java, shseo
 *
 * Date: August 6 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.control;

import icecube.daq.trigger.IReadoutRequestElement;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.splicer.PayloadFactory;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is to take care of timeOverlap and spaceOverlap between different ReadoutTypes
 * in a single GlobalTrigEvent after SimpleMerger.
 *
 * @version $Id: SmartMerger.java,v 1.4 2005/09/16 18:13:11 shseo Exp $
 * @author shseo
 */
public class SmartMerger
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(SmartMerger.class);

    private TriggerRequestPayloadFactory DEFAULT_TRIGGER_FACTORY = new TriggerRequestPayloadFactory();
    private TriggerRequestPayloadFactory triggerFactory;

    private Sorter mtSorter = new Sorter();

    private List mListFinalReadoutElements_All = new ArrayList();

    private List mListReadoutElements_II_Global = new ArrayList();
    private List mListReadoutElements_II_String = new ArrayList();
    private List mListReadoutElements_II_Module = new ArrayList();
    private List mListReadoutElements_IT_Global = new ArrayList();
    private List mListReadoutElements_IT_String = new ArrayList();
    private List mListReadoutElements_IT_Module = new ArrayList();

    // list containg new ReadoutElements of all (Global, Strings, Modules) in InIce
    private List mListFinalReadoutElements_InIce = new ArrayList();

    // list containg new ReadoutElements of all (Global, Strings, Modules) in IceTop
    private List mListFinalReadoutElements_IceTop = new ArrayList();

    private final double mdNanoSec_negative = -0.1;
    private final double mdNanoSec_positive = 0.1;

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public SmartMerger()
    {
        setPayloadFactory(DEFAULT_TRIGGER_FACTORY);
    }
    /**
     * This method assigns lists according to ReadoutType.
     *
     * @param listSimpleMergedSameReadoutTypeLists
     */
    private void assignListReadoutType(List listSimpleMergedSameReadoutTypeLists)
    {
        initialize();

        for(int i=0; i<listSimpleMergedSameReadoutTypeLists.size(); i++)
        {
            List listSameReadoutElements = (List) listSimpleMergedSameReadoutTypeLists.get(i);

            IReadoutRequestElement element = (IReadoutRequestElement) listSameReadoutElements.get(0);

            switch(element.getReadoutType())
            {
                case IReadoutRequestElement.READOUT_TYPE_II_GLOBAL:
                    mListReadoutElements_II_Global = listSameReadoutElements;
                    break;

                case IReadoutRequestElement.READOUT_TYPE_II_STRING:
                    mListReadoutElements_II_String = listSameReadoutElements;
                    break;

                case IReadoutRequestElement.READOUT_TYPE_II_MODULE:
                    mListReadoutElements_II_Module = listSameReadoutElements;
                    break;

                case IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL:
                    mListReadoutElements_IT_Global = listSameReadoutElements;
                    break;

                case IReadoutRequestElement.READOUT_TYPE_IT_MODULE:
                    mListReadoutElements_IT_Module = listSameReadoutElements;
                    break;

                default:
                    log.error("Unknown ReadoutType!!!");
                    break;

            }

        }

    }
    /**
     * This method initializes all lists before merge().
     */
    private void initialize()
    {
        mListReadoutElements_II_Global = new ArrayList();
        mListReadoutElements_II_String = new ArrayList();
        mListReadoutElements_II_Module = new ArrayList();
        mListReadoutElements_IT_Global = new ArrayList();
        mListReadoutElements_IT_String = new ArrayList();
        mListReadoutElements_IT_Module = new ArrayList();

        mListFinalReadoutElements_InIce = new ArrayList();
        mListFinalReadoutElements_IceTop = new ArrayList();
        mListFinalReadoutElements_All = new ArrayList();
    }
    /**
     * This method manages timeOverlap among differnt ReadoutTypes in InIce.
     */
    private void manageReadout_InIce()
    {
        //Only one ReadoutType has already been taken care in GlobalTrigEventReadoutElements.

        if(0 != mListReadoutElements_II_Global.size() &&
           0 != mListReadoutElements_II_String.size() &&
           0 == mListReadoutElements_II_Module.size())
        {
            //Larger ReadoutElements are not affected.
            //Only Smaller ReadoutElements are adjusted according to LargerReadoutElements.
            mListFinalReadoutElements_InIce.addAll(mListReadoutElements_II_Global);

            //check timeOverlap between Strings and Global. --> new ReadoutElement
            List listNewReadoutElements_II_String = new ArrayList();
            listNewReadoutElements_II_String.addAll(
                    (List) getListNewAdjustedSmallerReadoutElements(
                        mListReadoutElements_II_Global, mListReadoutElements_II_String));

            if(0 != listNewReadoutElements_II_String.size())
            {
                mListFinalReadoutElements_InIce.addAll(listNewReadoutElements_II_String);
            }

        }else if(0 != mListReadoutElements_II_Global.size() &&
                 0 != mListReadoutElements_II_String.size() &&
                 0 != mListReadoutElements_II_Module.size())
        {
            //Larger ReadoutElements are not affected.
            //Only Smaller ReadoutElements are adjusted according to LargerReadoutElements.
            mListFinalReadoutElements_InIce.addAll(mListReadoutElements_II_Global);

            List listNewReadoutElements_II_String = new ArrayList();
            listNewReadoutElements_II_String.addAll(
                    (List) getListNewAdjustedSmallerReadoutElements(
                            mListReadoutElements_II_Global, mListReadoutElements_II_String));

            List listNewReadoutElements_II_Module = new ArrayList();

            if(0 != listNewReadoutElements_II_String.size())
            {
                mListFinalReadoutElements_InIce.addAll(listNewReadoutElements_II_String);

                listNewReadoutElements_II_Module.addAll(
                    (List) getListNewAdjustedSmallerReadoutElements(
                            listNewReadoutElements_II_String,  mListReadoutElements_II_Module));

                if(0 != listNewReadoutElements_II_Module.size())
                {
                    List listNewReadoutElements_II_Module2 = new ArrayList();
                    listNewReadoutElements_II_Module2.addAll(
                            (List) getListNewAdjustedSmallerReadoutElements(
                                    mListReadoutElements_II_Global, listNewReadoutElements_II_Module));

                    if(0 != listNewReadoutElements_II_Module2.size())
                    {
                        mListFinalReadoutElements_InIce.addAll(listNewReadoutElements_II_Module2);
                    }
                }

            }else
            {
                listNewReadoutElements_II_Module.addAll(
                        (List) getListNewAdjustedSmallerReadoutElements(
                                mListReadoutElements_II_Global, mListReadoutElements_II_Module));

                if(0 != listNewReadoutElements_II_Module.size())
                {
                    mListFinalReadoutElements_InIce.addAll(listNewReadoutElements_II_Module);
                }
            }

        }else if(0 == mListReadoutElements_II_Global.size() &&
                 0 != mListReadoutElements_II_String.size() &&
                 0 != mListReadoutElements_II_Module.size())
        {
            mListFinalReadoutElements_InIce.addAll(mListReadoutElements_II_String);

            List listNewReadoutElements_II_Module = new ArrayList();

            listNewReadoutElements_II_Module.addAll(
                    (List) getListNewAdjustedSmallerReadoutElements(
                            mListReadoutElements_II_String,  mListReadoutElements_II_Module));

            if(0 != listNewReadoutElements_II_Module.size())
            {
                mListFinalReadoutElements_InIce.addAll(listNewReadoutElements_II_Module);
            }

        }else if(0 != mListReadoutElements_II_Global.size() &&
                 0 == mListReadoutElements_II_String.size() &&
                 0 != mListReadoutElements_II_Module.size())
        {
            mListFinalReadoutElements_InIce.addAll(mListReadoutElements_II_Global);

            List listNewReadoutElements_II_Module = new ArrayList();
            listNewReadoutElements_II_Module.addAll(
                    (List) getListNewAdjustedSmallerReadoutElements(
                            mListReadoutElements_II_Global,  mListReadoutElements_II_Module));

            if(0 != listNewReadoutElements_II_Module.size())
            {
                mListFinalReadoutElements_InIce.addAll(listNewReadoutElements_II_Module);
            }
        }else if(0 != mListReadoutElements_II_Global.size() &&
                 0 == mListReadoutElements_II_String.size() &&
                 0 == mListReadoutElements_II_Module.size())
        {
            mListFinalReadoutElements_InIce.addAll(mListReadoutElements_II_Global);

        }else if(0 == mListReadoutElements_II_Global.size() &&
                 0 != mListReadoutElements_II_String.size() &&
                 0 == mListReadoutElements_II_Module.size())
        {
            mListFinalReadoutElements_InIce.addAll(mListReadoutElements_II_String);

        }else if(0 == mListReadoutElements_II_Global.size() &&
                 0 == mListReadoutElements_II_String.size() &&
                 0 != mListReadoutElements_II_Module.size())
        {
            mListFinalReadoutElements_InIce.addAll(mListReadoutElements_II_Module);

        }

    }
    /**
     * This method manages timeOverlap among differnt ReadoutTypes in IceTop.
     * Note: There will be no READOUT_IT_String. But I implement it for generality.
     */
    private void manageReadout_IceTop()
    {
        // same as manageReadout_InIce() but substitute with IT
       if(0 != mListReadoutElements_IT_Global.size() &&
                 0 != mListReadoutElements_IT_String.size() &&
                 0 == mListReadoutElements_IT_Module.size())
        {
           mListFinalReadoutElements_IceTop.addAll(mListReadoutElements_IT_Global);

            //check timeOverlap between Strings and Global. --> new ReadoutElement
            List listNewReadoutElements_IT_String = new ArrayList();
            listNewReadoutElements_IT_String.addAll(
                    (List) getListNewAdjustedSmallerReadoutElements(
                            mListReadoutElements_IT_Global, mListReadoutElements_IT_String));

            if(0 != listNewReadoutElements_IT_String.size())
            {
                mListFinalReadoutElements_IceTop.addAll(listNewReadoutElements_IT_String);
            }

        }else if(0 != mListReadoutElements_IT_Global.size() &&
                 0 != mListReadoutElements_IT_String.size() &&
                 0 != mListReadoutElements_IT_Module.size())
        {
            mListFinalReadoutElements_IceTop.addAll(mListReadoutElements_IT_Global);

            List listNewReadoutElements_IT_String = new ArrayList();
            listNewReadoutElements_IT_String.addAll(
                    (List) getListNewAdjustedSmallerReadoutElements(
                            mListReadoutElements_IT_Global, mListReadoutElements_IT_String));

            List listNewReadoutElements_IT_Module = new ArrayList();

            if(0 != listNewReadoutElements_IT_String.size())
            {
                mListFinalReadoutElements_IceTop.addAll(listNewReadoutElements_IT_String);

                listNewReadoutElements_IT_Module.addAll(
                        (List) getListNewAdjustedSmallerReadoutElements(
                                listNewReadoutElements_IT_String,  mListReadoutElements_IT_Module));

                if(0 != listNewReadoutElements_IT_Module.size())
                {
                    List listNewReadoutElements_IT_Module2 = new ArrayList();
                    listNewReadoutElements_IT_Module2.addAll(
                            (List) getListNewAdjustedSmallerReadoutElements(
                                    mListReadoutElements_IT_Global, listNewReadoutElements_IT_Module));

                    if(0 != listNewReadoutElements_IT_Module2.size())
                    {
                        mListFinalReadoutElements_IceTop.addAll(listNewReadoutElements_IT_Module2);
                    }
                }

            }else{

                listNewReadoutElements_IT_Module.addAll(
                        (List) getListNewAdjustedSmallerReadoutElements(
                                mListReadoutElements_IT_Global, mListReadoutElements_IT_Module));

                if(0 != listNewReadoutElements_IT_Module.size())
                {
                    mListFinalReadoutElements_IceTop.addAll(listNewReadoutElements_IT_Module);
                }

            }

        }else if(0 == mListReadoutElements_IT_Global.size() &&
                 0!= mListReadoutElements_IT_String.size() &&
                 0 != mListReadoutElements_IT_Module.size())
        {
            mListFinalReadoutElements_IceTop.addAll(mListReadoutElements_IT_String);

            List listNewReadoutElements_IT_Module = new ArrayList();

            listNewReadoutElements_IT_Module.addAll(
                    (List) getListNewAdjustedSmallerReadoutElements(
                            mListReadoutElements_IT_String,  mListReadoutElements_IT_Module));

            if(0 != listNewReadoutElements_IT_Module.size())
            {
                mListFinalReadoutElements_IceTop.addAll(listNewReadoutElements_IT_Module);
            }

        }else if(0 != mListReadoutElements_IT_Global.size() &&
                 0 == mListReadoutElements_IT_String.size() &&
                 0 != mListReadoutElements_IT_Module.size())
        {
            mListFinalReadoutElements_IceTop.addAll(mListReadoutElements_IT_Global);

            List listNewReadoutElements_IT_Module = new ArrayList();
            listNewReadoutElements_IT_Module.addAll(
                    (List) getListNewAdjustedSmallerReadoutElements(
                            mListReadoutElements_IT_Global, mListReadoutElements_IT_Module));

            if(0 != listNewReadoutElements_IT_Module.size())
            {
                mListFinalReadoutElements_IceTop.addAll(listNewReadoutElements_IT_Module);
            }
        }else if(0 != mListReadoutElements_IT_Global.size() &&
                 0 == mListReadoutElements_IT_String.size() &&
                 0 == mListReadoutElements_IT_Module.size())
        {
            mListFinalReadoutElements_IceTop.addAll(mListReadoutElements_IT_Global);

        }else if(0 == mListReadoutElements_IT_Global.size() &&
                 0 != mListReadoutElements_IT_String.size() &&
                 0 == mListReadoutElements_IT_Module.size())
        {
            mListFinalReadoutElements_IceTop.addAll(mListReadoutElements_IT_String);

        }else if(0 == mListReadoutElements_IT_Global.size() &&
                 0 == mListReadoutElements_IT_String.size() &&
                 0 != mListReadoutElements_IT_Module.size())
        {
            mListFinalReadoutElements_IceTop.addAll(mListReadoutElements_IT_Module);
        }

    }
    /**
     * This method will make a new ReadoutElement.
     * All other information will be the same as the oldElement but startTime and/or endTime.
     *
     * @param tOldElement
     * @param tTime_start
     * @param tTime_end
     * @return
     */
    private IReadoutRequestElement makeNewReadoutElement(IReadoutRequestElement tOldElement,
                                                         IUTCTime tTime_start, IUTCTime tTime_end)
    {
        IReadoutRequestElement tNewReadoutElement =
                triggerFactory.createReadoutRequestElement(
                                                tOldElement.getReadoutType(),
                                                tTime_start,
                                                tTime_end,
                                                tOldElement.getDomID(),
                                                tOldElement.getSourceID());

        return tNewReadoutElement;
    }
    /**
     * This method is the heart of this class.
     * In this method timeOverlap between different ReadoutTypes will be managed.
     *
     * @param listLargerSameReadoutElements: II_GLOBAL, II_STRING, or IT_GLOBAL
     * @param listSmallerSameReadoutElements: II_String, II_DOM, IT_DOM
     */
    private List getListNewAdjustedSmallerReadoutElements(List listLargerSameReadoutElements,
                                                          List listSmallerSameReadoutElements)
    {
        List listNewElements = new ArrayList();
        
        for(int i=0; i < listSmallerSameReadoutElements.size(); i++)
        {
            IReadoutRequestElement tSmElement = (IReadoutRequestElement) listSmallerSameReadoutElements.get(i);

            boolean bDiffSourceID = false;

            int iSmSourceId = -1;
            if(null != tSmElement.getSourceID())
            {
                iSmSourceId = tSmElement.getSourceID().getSourceID();
            }

            IUTCTime tSmTime_start = tSmElement.getFirstTimeUTC();
            IUTCTime tSmTime_end = tSmElement.getLastTimeUTC();

            for(int k=0; k < listLargerSameReadoutElements.size(); k++)
            {
                int iLastIndexOfListLargerSameReadoutElements = listLargerSameReadoutElements.size() - 1;
                IReadoutRequestElement tLgElement = (IReadoutRequestElement) listLargerSameReadoutElements.get(k);
                int iLgReadoutType = tLgElement.getReadoutType();
                int iLgSourceID = -1;

                if(null != tLgElement.getSourceID())
                {
                    iLgSourceID = tLgElement.getSourceID().getSourceID();
                }

                IUTCTime tLgTime_start = tLgElement.getFirstTimeUTC();
                IUTCTime tLgTime_end = tLgElement.getLastTimeUTC();
                IUTCTime tTime_boundary_start;
                IUTCTime tTime_boundary_end;

                tLgTime_end.timeDiff_ns(tLgTime_start);

                //check timeOverlap if larger=Global or
                // larger.SourceID == smaller.SourceID (larger = String, smaller = DOM case)
                if(iLgReadoutType == IReadoutRequestElement.READOUT_TYPE_II_GLOBAL
                    || iLgReadoutType == IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL
                    || (iSmSourceId == iLgSourceID && iSmSourceId != -1))
                {
                    if(tSmTime_start.compareTo(tLgTime_start) < 0)
                    {
                        if(tSmTime_end.compareTo(tLgTime_start) < 0) // case A: complete isolation
                        {
                            //No modification is necessary, just put in a new list.
                            listNewElements.add(tSmElement);
                            break;//check next element in the listSmallerSameReadoutElements

                        }else if(tSmTime_end.compareTo(tLgTime_start) >= 0
                                && tSmTime_end.compareTo(tLgTime_end) <= 0) // case B:
                        {
                            //take care of Boundary condition
                            tTime_boundary_end = tLgTime_start.getOffsetUTCTime(mdNanoSec_negative);

                            //make a new ReadoutElement and put in a new list.
                            listNewElements.add(makeNewReadoutElement(tSmElement, tSmTime_start, tTime_boundary_end));
                            break;//check next element in the listSmallerSameReadoutElements

                        }else if(tSmTime_end.compareTo(tLgTime_start) > 0
                                && tSmTime_end.compareTo(tLgTime_end) > 0) // case C:
                        {
                            //take care of Boundary condition
                            tTime_boundary_end = tLgTime_start.getOffsetUTCTime(mdNanoSec_negative);

                            //make a new ReadoutElement and put in a new list.
                            listNewElements.add(makeNewReadoutElement(tSmElement, tSmTime_start, tTime_boundary_end));

                            //take care of boundary condition
                            tSmTime_start = tLgTime_end.getOffsetUTCTime(mdNanoSec_positive);
                            if(k == iLastIndexOfListLargerSameReadoutElements)
                            {
                                listNewElements.add(makeNewReadoutElement(tSmElement, tSmTime_start, tSmTime_end));
                            }

                        }

                    }else if(tSmTime_start.compareTo(tLgTime_start) >= 0
                        && tSmTime_start.compareTo(tLgTime_end) <= 0)
                    {
                        if(tSmTime_end.compareTo(tLgTime_end) <= 0) // case D: complete absorption
                        {
                            //absorbed, don't add to the listNewReadoutElement.
                            break;

                        }else if(tSmTime_end.compareTo(tLgTime_end) > 0) // case E:
                        {
                            tTime_boundary_start = tLgTime_end.getOffsetUTCTime(mdNanoSec_positive);
                            tSmTime_start = tTime_boundary_start;

                            //tSmTime_start = tLgTime_end;
                            if(k == iLastIndexOfListLargerSameReadoutElements)
                            {
                                listNewElements.add(makeNewReadoutElement(tSmElement, tSmTime_start, tSmTime_end));
                            }
                            //proceed to the next element in the larger list.
                        }

                    }else if(tSmTime_start.compareTo(tLgTime_end) > 0) // case F: complete isolation
                    {
                        if(k == iLastIndexOfListLargerSameReadoutElements)
                        {
                            listNewElements.add(tSmElement);
                        }
                        //do nothing proceed to the next element in the larger list.

                    }

                    }else if(iSmSourceId != iLgSourceID)
                    {
                        bDiffSourceID = true;
                    }

                }

            if(bDiffSourceID)
            {
                listNewElements.add(tSmElement);
            }

        }

        return listNewElements;
    }
    /**
     * This is the main method called in GlobalTrigReadoutElements.java after SimpleMerger.
     *
     * @param listSimplyMergedReadoutTypeLists: require SimpleMerge for each list of ReadoutType.
     */
    public void merge(List listSimplyMergedReadoutTypeLists)
    {
        initialize();

        //Assign lists according to ReadoutType.
        assignListReadoutType(listSimplyMergedReadoutTypeLists);

        //InIce
        manageReadout_InIce();
        //IceTop
        manageReadout_IceTop();

        if(0 != mListFinalReadoutElements_InIce.size())
        {
            mListFinalReadoutElements_All.addAll(mListFinalReadoutElements_InIce);
        }
        if(0 != mListFinalReadoutElements_IceTop.size())
        {
            mListFinalReadoutElements_All.addAll(mListFinalReadoutElements_IceTop);
        }

        //time-order the final ReadoutList
        if(mListFinalReadoutElements_All.size() > 0)
        {
            List listTimeordered = new ArrayList();
            listTimeordered =mtSorter.getReadoutElementsUTCTimeSorted(mListFinalReadoutElements_All);
            mListFinalReadoutElements_All = new ArrayList();
            mListFinalReadoutElements_All = listTimeordered;
        }

    }
    public List getFinalReadoutElementsTimeOrdered_All()
    {
        return mListFinalReadoutElements_All;
    }
    public void setPayloadFactory(PayloadFactory triggerFactory) {
        this.triggerFactory = (TriggerRequestPayloadFactory) triggerFactory;
    }

}
