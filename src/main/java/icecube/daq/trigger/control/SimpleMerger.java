/*
 * class: simpleMerger
 *
 * Version $Id: simpleMerger.java, shseo
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

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is to take care of timeOverlap and spaceOverlap of ReadoutElements
 * in the same ReadoutType in a given GlobalTrigEvent.
 *
 * @version $Id: SimpleMerger.java,v 1.3 2005/09/16 18:13:11 shseo Exp $
 * @author shseo
 */
public class SimpleMerger
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(SimpleMerger.class);

    private TriggerRequestPayloadFactory DEFAULT_TRIGGER_FACTORY = new TriggerRequestPayloadFactory();
    private TriggerRequestPayloadFactory triggerFactory = null;

    private final int mi_TIMEGAP_NO = 1;
    private final int mi_TIMEGAP_YES = 2;
    private int DEFAULT_TIMEGAP_OPTION = mi_TIMEGAP_NO;
    private int mi_TimeGap_option;

    Sorter tSorter = new Sorter();

    private List mListSimpleMergedSameReadoutElements = new ArrayList();

    private List mlistSameIDElementLists = new ArrayList();
    private List mlistDiffIDElements = new ArrayList();

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public SimpleMerger()
    {
        setPayloadFactory(DEFAULT_TRIGGER_FACTORY);
        setTimeGap_option(DEFAULT_TIMEGAP_OPTION);
    }
    /**
     * This is the main mehtod.
     *
     * @param listSameReadoutElements : the same ReadoutType is required as input parameter.
     */
    public void merge(List listSameReadoutElements)
    {
        mListSimpleMergedSameReadoutElements = new ArrayList();

        //There is no need to merge if the sizeList == 1.
        if(listSameReadoutElements.size() == 1) //listSize == 1.
        {
            mListSimpleMergedSameReadoutElements.add(listSameReadoutElements.get(0));

        }else if (listSameReadoutElements.size() == 0)
        {
            log.error("Input list size should be greater than zero...!!!!");

        }else if(listSameReadoutElements.size() > 1)
        {
            //First of all, time-order the inputList.
            List tempList = tSorter.getReadoutElementsUTCTimeSorted(listSameReadoutElements);
            listSameReadoutElements = new ArrayList();
            listSameReadoutElements = tempList;

            List listSameIDElementLists = new ArrayList();
            List listTimeManagedSameIDElements = new ArrayList();

            int iReadoutType = ((IReadoutRequestElement) listSameReadoutElements.get(0)).getReadoutType();

            if(iReadoutType != IReadoutRequestElement.READOUT_TYPE_II_GLOBAL
                && iReadoutType != IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL)
            {
                classifySameIDElements(listSameReadoutElements);

                if(mlistSameIDElementLists.size() != 0)
                {

                    for(int i=0; i < mlistSameIDElementLists.size(); i++)
                    {
                        listTimeManagedSameIDElements = new ArrayList();
                        List listSameIDElements = (List) mlistSameIDElementLists.get(i);

                        if(listSameIDElements.size() > 1)
                        {
                            try {
                                listTimeManagedSameIDElements = manageTimeOverlap(listSameIDElements, mi_TimeGap_option);
                            } catch (Exception e) {
                                log.error("Couldn't manage time overlap", e);
                            }
                            mListSimpleMergedSameReadoutElements.addAll(listTimeManagedSameIDElements);

                        }else if(listSameIDElements.size() == 1)
                        {
                            mListSimpleMergedSameReadoutElements.addAll(listSameIDElements);
                        }

                    }
                }
                if(mlistDiffIDElements.size() != 0)
                {
                    mListSimpleMergedSameReadoutElements.addAll(mlistDiffIDElements);
                }
            }else{ // all elements have the same ISourceID (i.e., InIce or IceTop)
                // thus, only Check timeOverlap, merge, and produce IReadoutRequestElement.

                try {
                    listTimeManagedSameIDElements = manageTimeOverlap(listSameReadoutElements, mi_TimeGap_option);
                } catch (Exception e) {
                    log.error("Couldn't manage time overlap", e);
                }
                mListSimpleMergedSameReadoutElements.addAll(listTimeManagedSameIDElements);
            }

        }

    }
    public void classifySameIDElements(List listSameReadoutElements)
    {
        mlistSameIDElementLists = new ArrayList();
        mlistDiffIDElements = new ArrayList();
        List listSameIDElementIndex = new ArrayList();

        IReadoutRequestElement element_last = null;
        IReadoutRequestElement element_current = null;

        int iSourceID_last = -1;
        int iSourceID_current = -1;

        long iDOMID_last = -1;
        long iDOMID_current = -1;

        IReadoutRequestElement tElement = (IReadoutRequestElement) listSameReadoutElements.get(0);
        int iReadoutType = tElement.getReadoutType();

      // select ReadoutElements whose SourceID is the same and put in lists. (i.e., same String# or DOM#)
        if(listSameReadoutElements.size() > 1
            && iReadoutType != IReadoutRequestElement.READOUT_TYPE_II_GLOBAL
            && iReadoutType != IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL)
        {
            for(int i=0; i < listSameReadoutElements.size(); i++)
            {
                //listSameIDElementIndex = new ArrayList();
                i=0;
                element_last = (IReadoutRequestElement) listSameReadoutElements.get(i);

                if(iReadoutType == IReadoutRequestElement.READOUT_TYPE_II_STRING)
                {
                    iSourceID_last = element_last.getSourceID().getSourceID();

                }else if(iReadoutType == IReadoutRequestElement.READOUT_TYPE_II_MODULE
                            || iReadoutType == IReadoutRequestElement.READOUT_TYPE_IT_MODULE)
                {
                    iDOMID_last = element_last.getDomID().getDomIDAsLong();
                }else{
                    log.error("wrong Readout type");
                }

                if(listSameReadoutElements.size() > 1)
                {
                    for(int j=i+1; j < listSameReadoutElements.size(); j++)
                    {
                        element_current = (IReadoutRequestElement) listSameReadoutElements.get(j);

                        if(iReadoutType == IReadoutRequestElement.READOUT_TYPE_II_STRING)
                        {
                            iSourceID_current = element_current.getSourceID().getSourceID();

                        }else if(iReadoutType == IReadoutRequestElement.READOUT_TYPE_II_MODULE
                            || iReadoutType == IReadoutRequestElement.READOUT_TYPE_IT_MODULE)
                        {
                            iDOMID_current = element_current.getDomID().getDomIDAsLong();
                        }else{
                            log.error("wrong Readout type");
                        }

                        if((iReadoutType == IReadoutRequestElement.READOUT_TYPE_II_STRING
                                                && iSourceID_last == iSourceID_current)
                            || (iReadoutType == IReadoutRequestElement.READOUT_TYPE_II_MODULE
                                                && iDOMID_last == iDOMID_current)
                            || (iReadoutType == IReadoutRequestElement.READOUT_TYPE_IT_MODULE
                                                && iDOMID_last == iDOMID_current))
                        {
                            if(listSameIDElementIndex.size() == 0)
                            {
                                listSameIDElementIndex.add(new Integer(i));
                            }
                            listSameIDElementIndex.add(new Integer(j));

                        }

                    }
                    //after collecting the sameIDElementIndex, put them in a new listSameIDElement
                    //  and remove them from the sameReadoutElements.
                    if(listSameIDElementIndex.size() != 0)
                    {
                        List listTemp = new ArrayList();
                        for(int k=0; k <listSameIDElementIndex.size(); k++)
                        {
                            int index = ((Integer) listSameIDElementIndex.get(k)).intValue();
                            IReadoutRequestElement element = (IReadoutRequestElement) listSameReadoutElements.get(index);
                            listTemp.add(element);
                        }
                        for(int k=0; k <listSameIDElementIndex.size(); k++)
                        {
                            int index = ((Integer) listSameIDElementIndex.get(k)).intValue();

                            listSameReadoutElements.remove(index-k);
                        }
                        //i = 0;
                        listSameIDElementIndex = new ArrayList();

                        mlistSameIDElementLists.add(listTemp);
                    }else
                    {
                        mlistDiffIDElements.add(element_last);
                        listSameReadoutElements.remove(i);
                        i--;
                        if(i == listSameReadoutElements.size() - 2)
                        {
                            mlistDiffIDElements.add(element_current);
                            listSameReadoutElements.remove(i+1);
                        }

                    }

                    if(listSameReadoutElements.size() == 1)
                    {
                        mlistDiffIDElements.add(listSameReadoutElements.get(0));
                    }

                }

            }//for loop

        }else
        {
            if(listSameReadoutElements.size() == 1)
            {
                mlistDiffIDElements.addAll(listSameReadoutElements);

            }else
            {
                mlistSameIDElementLists.add(listSameReadoutElements);

            }
        }

    }
    /**
     * @param listSameReadoutElementsSameID
     * @return
     */
    public List manageTimeOverlap_NoGap(List listSameReadoutElementsSameID) throws Exception {
        List listTimeManagedElementsSameID = new ArrayList();

        listTimeManagedElementsSameID.add(makeNewReadoutElement(listSameReadoutElementsSameID));

        return listTimeManagedElementsSameID;
    }
    public List manageTimeOverlap_Gap(List listSameReadoutElementsSameID) throws Exception {
        List listTimeManagedElementsSameID = new ArrayList();
        List listTempMergedElements = new ArrayList();
        List listUnmergedElements = new ArrayList();

        //if time-overlap then make new IReaoutRequestElement. --> put in listTimeManagedElementsSameID.
        IReadoutRequestElement lastElement = null;
        IReadoutRequestElement currentElement = null;

        IUTCTime lastUTCTime_end = null;
        IUTCTime currentUTCTime_start = null;

        //Before checking timeOverlap, sort input list.
        if(listSameReadoutElementsSameID.size() > 1)
        {
            List listTemp = tSorter.getReadoutElementsUTCTimeSorted(listSameReadoutElementsSameID);
            listSameReadoutElementsSameID = new ArrayList();
            listSameReadoutElementsSameID = listTemp;

            for(int i=0; i < listSameReadoutElementsSameID.size() - 1; i++)
            {
                lastElement = (IReadoutRequestElement) listSameReadoutElementsSameID.get(i);
                currentElement = (IReadoutRequestElement) listSameReadoutElementsSameID.get(i+1);

                lastUTCTime_end = lastElement.getLastTimeUTC();
                currentUTCTime_start = currentElement.getFirstTimeUTC();

                double diff = lastUTCTime_end.compareTo(currentUTCTime_start);

                if(diff >= 0){ //overlap

                    listTempMergedElements.add(lastElement);
                        //handle last element.
                    if(i == listSameReadoutElementsSameID.size() - 2)
                    {
                        listTempMergedElements.add(currentElement);
                        listTimeManagedElementsSameID.add(makeNewReadoutElement(listTempMergedElements));
                        listTempMergedElements = new ArrayList();
                    }

                } else{

                    if(listTempMergedElements.size() != 0)
                    {
                        listTempMergedElements.add(lastElement);
                        listTimeManagedElementsSameID.add(makeNewReadoutElement(listTempMergedElements));
                        listTempMergedElements = new ArrayList();

                    }else {
                        listUnmergedElements.add(lastElement);
                    }
                    //take care of the last elment.
                    if(i == listSameReadoutElementsSameID.size() - 2)
                    {
                        listUnmergedElements.add(currentElement);
                    }

                }

            }
            //put unmergedElements to listTimeManagedElementsSameID.
            if(0 != listUnmergedElements.size()){

                listTimeManagedElementsSameID.addAll(listUnmergedElements);
            }

        }else if(listSameReadoutElementsSameID.size() == 1)
        {
            listTimeManagedElementsSameID.addAll(listSameReadoutElementsSameID);
        }

        return listTimeManagedElementsSameID;

    }
    /**
     * This method will merge time-overlapping elements within the same elementID.
     *
     * @param listSameReadoutElementsSameID: list of ReadoutElements w/ the same ISourceID or IDOMID.
     */
    public List manageTimeOverlap(List listSameReadoutElementsSameID, int iTimeGap_option) throws Exception {
        List listTimeManagedElementsSameID = new ArrayList();

        if(iTimeGap_option == mi_TIMEGAP_NO)
        {
            listTimeManagedElementsSameID = manageTimeOverlap_NoGap(listSameReadoutElementsSameID);

        }else if(iTimeGap_option == mi_TIMEGAP_YES){

            listTimeManagedElementsSameID = manageTimeOverlap_Gap(listSameReadoutElementsSameID);

        }else{
            throw new NullPointerException("Unknown configuration detected in TimeGap_option");
        }

        return listTimeManagedElementsSameID;
    }
    /**
     * This method will create a new ReadoutElement based on the list of Elements to be merged.
     *
     * @param listMergedElements
     * @return
     */
    public IReadoutRequestElement makeNewReadoutElement(List listMergedElements) throws Exception {
        IReadoutRequestElement element = null;
        //need to manage time only
        //find the earliest/latest Time
        //todo:use the mehtod in mtSorter....?

/*
        IUTCTime earliestUTCTime = new UTCTime8B(Long.MAX_VALUE);
        IUTCTime latestUTCTime = new UTCTime8B(Long.MIN_VALUE);

        IUTCTime startUTCTime = null;
        IUTCTime endUTCTime = null;

        for(int i=0; i<listMergedElements.size(); i++)
        {
            element = (IReadoutRequestElement) listMergedElements.get(i);
            startUTCTime = element.getFirstTimeUTC();
            endUTCTime = element.getLastTimeUTC();

            if(earliestUTCTime.compareTo(startUTCTime) > 0)
            {
                earliestUTCTime = startUTCTime;
            }
            if(latestUTCTime.compareTo(endUTCTime) < 0)
            {
                latestUTCTime = endUTCTime;
            }

        }
*/

        if(listMergedElements.size() > 0)
        {
            element = (IReadoutRequestElement) listMergedElements.get(0);

        }else{
            throw new Exception("listMergedElements should contain at least one element!");
        }

        IUTCTime earliestUTCTime = tSorter.getUTCTimeEarliest(listMergedElements);
        IUTCTime latestUTCTime = tSorter.getUTCTimeLatest(listMergedElements);

        IReadoutRequestElement newElement = triggerFactory.createReadoutRequestElement(
                                                element.getReadoutType(),
                                                earliestUTCTime,
                                                latestUTCTime,
                                                element.getDomID(),
                                                element.getSourceID());


        return newElement;

    }
    public List getListSameIDElementLists()
    {
        return mlistSameIDElementLists;
    }

    public List getListDiffIDElements()
    {
        return mlistDiffIDElements;
    }
    public List getListSimpleMergedSameReadoutElements()
    {
        return mListSimpleMergedSameReadoutElements;
    }
    public void setPayloadFactory(PayloadFactory triggerFactory)
    {
        this.triggerFactory = (TriggerRequestPayloadFactory) triggerFactory;
    }
    public void setTimeGap_option(int iTimeGap_option)
    {
        mi_TimeGap_option = iTimeGap_option;
    }
    public int getTimeGap_option()
    {
        return mi_TimeGap_option;
    }
}
