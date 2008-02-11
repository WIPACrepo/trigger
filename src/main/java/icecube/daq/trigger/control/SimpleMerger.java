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

import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.splicer.PayloadFactory;
import icecube.daq.trigger.IReadoutRequestElement;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is to take care of timeOverlap and spaceOverlap of ReadoutElements
 * in the same ReadoutType in a given GlobalTrigEvent.
 *
 * TODO: Massively clean up this code!!!
 *
 * @version $Id: SimpleMerger.java 2629 2008-02-11 05:48:36Z dglo $
 * @author shseo
 */
public class SimpleMerger
{
    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(SimpleMerger.class);

    private TriggerRequestPayloadFactory DEFAULT_TRIGGER_FACTORY = new TriggerRequestPayloadFactory();
    private TriggerRequestPayloadFactory triggerFactory;

    private boolean allowTimeGap;

    private Sorter tSorter = new Sorter();

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
    }
    /**
     * This is the main method.
     *
     * @param listSameReadoutElements : the same ReadoutType is required as input parameter.
     */
    public List merge(List listSameReadoutElements)
    {
        List mListSimpleMergedSameReadoutElements = new ArrayList();

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
                                listTimeManagedSameIDElements = manageTimeOverlap(listSameIDElements, allowTimeGap);
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
                    listTimeManagedSameIDElements = manageTimeOverlap(listSameReadoutElements, allowTimeGap);
                } catch (Exception e) {
                    log.error("Couldn't manage time overlap", e);
                }
                mListSimpleMergedSameReadoutElements.addAll(listTimeManagedSameIDElements);
            }

        }

        return mListSimpleMergedSameReadoutElements;
    }
    private void classifySameIDElements(List listSameReadoutElements)
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
        if(listSameReadoutElements.size() <= 1 ||
           iReadoutType == IReadoutRequestElement.READOUT_TYPE_II_GLOBAL ||
           iReadoutType == IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL)
        {
            // XXX I don't think it's possible for this code to be run
            if(listSameReadoutElements.size() == 1)
            {
                mlistDiffIDElements.addAll(listSameReadoutElements);

            }else
            {
                mlistSameIDElementLists.add(listSameReadoutElements);

            }
        }else
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
        }

    }
    /**
     * @param listSameReadoutElementsSameID
     * @return
     */
    private List manageTimeOverlap_NoGap(List listSameReadoutElementsSameID) throws Exception {
        List listTimeManagedElementsSameID = new ArrayList();

        listTimeManagedElementsSameID.add(makeNewReadoutElement(listSameReadoutElementsSameID));

        return listTimeManagedElementsSameID;
    }
    private List manageTimeOverlap_Gap(List listSameReadoutElementsSameID) throws Exception {
        List listTimeManagedElementsSameID = new ArrayList();
        List listTempMergedElements = new ArrayList();
        List listUnmergedElements = new ArrayList();

        //if time-overlap then make new IReadoutRequestElement. --> put in listTimeManagedElementsSameID.
        IReadoutRequestElement lastElement = null;
        IReadoutRequestElement currentElement = null;

        IUTCTime lastUTCTime_end = null;
        IUTCTime currentUTCTime_start = null;

        //Before checking timeOverlap, sort input list.
        if(listSameReadoutElementsSameID.size() == 1)
        {
            // XXX I don't think it's possible for this code to be run
            listTimeManagedElementsSameID.addAll(listSameReadoutElementsSameID);

        }else if(listSameReadoutElementsSameID.size() > 1)
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

        }

        return listTimeManagedElementsSameID;

    }
    /**
     * This method will merge time-overlapping elements within the same elementID.
     *
     * @param listSameReadoutElementsSameID: list of ReadoutElements w/ the same ISourceID or IDOMID.
     */
    private List manageTimeOverlap(List listSameReadoutElementsSameID, boolean allowTimeGap) throws Exception {
        List listTimeManagedElementsSameID = new ArrayList();

        if(!allowTimeGap)
        {
            listTimeManagedElementsSameID = manageTimeOverlap_NoGap(listSameReadoutElementsSameID);

        }else{

            listTimeManagedElementsSameID = manageTimeOverlap_Gap(listSameReadoutElementsSameID);

        }

        return listTimeManagedElementsSameID;
    }
    /**
     * This method will create a new ReadoutElement based on the list of
     * elements to be merged.
     *
     * @param list list of merged elements
     * @return
     */
    private IReadoutRequestElement makeNewReadoutElement(List list) throws Exception {

        if(list.size() == 0)
        {
            throw new Exception("List should contain at least one element!");
        }

        IReadoutRequestElement elem =
            (IReadoutRequestElement) list.get(0);

        IUTCTime earliestUTCTime =
            tSorter.getUTCTimeEarliest(list);
        IUTCTime latestUTCTime = tSorter.getUTCTimeLatest(list);

        return triggerFactory.createReadoutRequestElement(elem.getReadoutType(),
                                                          earliestUTCTime,
                                                          latestUTCTime,
                                                          elem.getDomID(),
                                                          elem.getSourceID());

    }
    public void setPayloadFactory(PayloadFactory triggerFactory)
    {
        this.triggerFactory = (TriggerRequestPayloadFactory) triggerFactory;
    }
    public void setAllowTimeGap(boolean val)
    {
        allowTimeGap = val;
    }
}
