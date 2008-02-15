package icecube.daq.trigger.control;

import icecube.daq.eventbuilder.IEventPayload;
import icecube.daq.eventbuilder.IReadoutDataPayload;
import icecube.daq.payload.IDOMID;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.IHitDataPayload;
import icecube.daq.trigger.IHitPayload;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.IReadoutRequestElement;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.config.TriggerRegistry;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Check that payload contents are valid.
 */
public abstract class PayloadChecker
{
    /** Log object. */
    private static final Log LOG =
        LogFactory.getLog(PayloadChecker.class);

    private static final int SMT_TYPE =
        TriggerRegistry.getTriggerType("SimpleMajorityTrigger");

    /**
     * Should we ignore readout request elements which fall outside the
     * request bounds in non-global trigger request?
     */
    private static final boolean IGNORE_NONGLOBAL_RREQS = true;

    /**
     * Get string representation of a DOM ID.
     *
     * @param dom DOM ID
     *
     * @return DOM ID string
     */
    private static String getDOMString(IDOMID dom)
    {
        if (dom == null || dom.longValue() == 0xffffffffffffffffL) {
            return "";
        }

        return dom.toString();
    }

    /**
     * Get string representation of an event.
     *
     * @param evt event
     *
     * @return event string
     */
    private static String getEventString(IEventPayload evt)
    {
        return "event #" + evt.getEventUID();
    }

    /**
     * Get string representation of a hit.
     *
     * @param hit hit
     *
     * @return hit string
     */
    private static String getHitString(IHitPayload hit)
    {
        return "hit@" + hit.getHitTimeUTC();
    }

    /**
     * Get string representation of a readout data payload.
     *
     * @param rdp readout data payload
     *
     * @return readout data payload string
     */
    private static String getReadoutDataString(IReadoutDataPayload rdp)
    {
        return "RDP #" + rdp.getRequestUID();
    }

    /**
     * Get string representation of a readout request element.
     *
     * @param elem readout request element
     *
     * @return readout request element string
     */
    private static String getRRElementString(IReadoutRequestElement elem)
    {
        String domStr = getDOMString(elem.getDomID());
        String srcStr = getSourceString(elem.getSourceID());

        return "rrElem[" + getReadoutType(elem.getReadoutType()) +
            (domStr == "" ? "" : " dom " + domStr) +
            (srcStr == "" ? "" : " src " + srcStr) +
            "]";
    }

    /**
     * Get string representation of a readout request element.
     *
     * @param elem readout request element
     *
     * @return readout request element string
     */
    private static String getReadoutType(int rdoutType)
    {
        switch (rdoutType) {
        case IReadoutRequestElement.READOUT_TYPE_GLOBAL:
            return "global";
        case IReadoutRequestElement.READOUT_TYPE_II_GLOBAL:
            return "inIceGlobal";
        case IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL:
            return "icetopGlobal";
        case IReadoutRequestElement.READOUT_TYPE_II_STRING:
            return "string";
        case IReadoutRequestElement.READOUT_TYPE_II_MODULE:
            return "dom";
        case IReadoutRequestElement.READOUT_TYPE_IT_MODULE:
            return "module";
        default:
            break;
        }

        return "unknownRdoutType#" + rdoutType;
    }

    /**
     * Get string representation of a source ID.
     *
     * @param src source ID
     *
     * @return source ID string
     */
    private static String getSourceString(ISourceID src)
    {
        if (src == null || src.getSourceID() < 0) {
            return "";
        }

        return src.toString();
    }

    /**
     * Get string representation of a trigger request.
     *
     * @param tr trigger request
     *
     * @return trigger request string
     */
    private static String getTriggerRequestString(ITriggerRequestPayload tr)
    {
        return "trigReq #" + tr.getUID() + "[" +
            getTriggerTypeString(tr.getTriggerType()) + "/" +
            getSourceString(tr.getSourceID()) +
            "]";
    }

    /** List of trigger types */
    private static String[] trigTypes = new String[] {
        "SimpMaj", "Calib", "MinBias", "Thruput", "FixedRt", "SyncBrd",
        "TrigBrd", "AmMFrag20", "AmVol", "AmM18", "AmM24", "AmStr",
        "AmSpase", "AmRand", "AmCalT0", "AmCalLaser",
    };

    /**
     * Get string representation of trigger type.
     *
     * @param trigType trigger type
     *
     * @return trigger type string
     */
    private static String getTriggerTypeString(int trigType)
    {
        if (trigType >= 0 && trigType < trigTypes.length) {
            return trigTypes[trigType];
        }

        return "unknownTrigType#" + trigType;
    }

    /**
     * Is there a SimpleMajorityTrigger inside the trigger request?
     *
     * @param tr trigger request
     *
     * @return <tt>true</tt> if the trigger request contains a
     *         SimpleMajorityTrigger
     */
    private static boolean hasSMTTrigger(ITriggerRequestPayload tr)
    {
        loadPayload(tr);

        if (tr.getSourceID().getSourceID() ==
            SourceIdRegistry.INICE_TRIGGER_SOURCE_ID &&
            tr.getTriggerType() == SMT_TYPE)
        {
            return true;
        }

        List payList;
        try {
            payList = tr.getPayloads();
        } catch (Exception ex) {
            LOG.error("Couldn't fetch payloads for " + tr, ex);
            return false;
        }

        for (Object obj : payList) {
            if (obj instanceof ITriggerRequestPayload) {
                if (hasSMTTrigger((ITriggerRequestPayload) obj)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Is the second pair of times contained within the first set of
     * times?
     *
     * @param descr0 description of first pair of times
     * @param first0 first time
     * @param last0 last time
     * @param descr1 description of second pair of times
     * @param first1 first time to be checked
     * @param last1 last time to be checked
     * @param verbose <tt>true</tt> if log message should be written when
     *                second pair of times is not within the first pair
     *
     * @return <tt>true</tt> if second pair of times are within the first pair
     */
    private static boolean isIntervalContained(String descr0, IUTCTime first0,
                                               IUTCTime last0, String descr1,
                                               IUTCTime first1, IUTCTime last1,
                                               boolean verbose)
    {
        boolean isContained =
            first0.getUTCTimeAsLong() <= first1.getUTCTimeAsLong() &&
            last0.getUTCTimeAsLong() >= last1.getUTCTimeAsLong();

        if (!isContained && verbose) {
            long firstDiff =
                first0.getUTCTimeAsLong() - first1.getUTCTimeAsLong();
            long lastDiff = last0.getUTCTimeAsLong() - last1.getUTCTimeAsLong();

            LOG.error(descr0 + " interval [" + first0 + "-" + last0 +
                      "] does not contain " + descr1 + " interval [" + first1 +
                      "-" + last1 + "] diff [" + firstDiff + "-" + lastDiff +
                      "]");
        }

        return isContained;
    }

    /**
     * Load the payload data.
     *
     * @param pay payload
     *
     * @return <tt>false</tt> if payload could not be loaded
     */
    private static boolean loadPayload(IPayload pay)
    {
        ILoadablePayload loadable = (ILoadablePayload) pay;

        try {
            loadable.loadPayload();
        } catch (Exception ex) {
            LOG.error("Couldn't load payload", ex);
            return false;
        }

        return true;
    }

    /**
     * Return a short description of the payload.
     *
     * @param pay payload
     *
     * @return short description string
     */
    public static String toString(IPayload pay)
    {
        if (pay instanceof IEventPayload) {
            return getEventString((IEventPayload) pay);
        } else if (pay instanceof IHitPayload) {
            return getHitString((IHitPayload) pay);
        } else if (pay instanceof IReadoutDataPayload) {
            return getReadoutDataString((IReadoutDataPayload) pay);
        } else if (pay instanceof ITriggerRequestPayload) {
            return getTriggerRequestString((ITriggerRequestPayload) pay);
        } else {
            return pay.toString();
        }
    }

    /**
     * Validate all subcomponents of an event payload.
     *
     * @param evt event
     * @param verbose <tt>true</tt> if errors should be logged
     *
     * @return <tt>true</tt> if event is valid
     */
    public static boolean validateEvent(IEventPayload evt, boolean verbose)
    {
        loadPayload(evt);

        String evtDesc = getEventString(evt);
        IUTCTime evtFirst = evt.getFirstTimeUTC();
        IUTCTime evtLast = evt.getLastTimeUTC();
        if (!validateInterval(evtDesc, evtFirst, evtLast, verbose)) {
            return false;
        }

        ITriggerRequestPayload trigReq = evt.getTriggerRequestPayload();
        loadPayload(trigReq);

        String trDesc = getTriggerRequestString(trigReq);
        IUTCTime trFirst = trigReq.getFirstTimeUTC();
        IUTCTime trLast = trigReq.getLastTimeUTC();
        if (!validateInterval(trDesc, trFirst, trLast, verbose)) {
            return false;
        }

        if (!isIntervalContained(evtDesc, evtFirst, evtLast,
                                 trDesc, trFirst, trLast, verbose))
        {
            return false;
        }

        if (!validateTriggerRequest(trigReq, verbose)) {
            return false;
        }

        ArrayList evtHits = new ArrayList();
        for (Object obj : evt.getReadoutDataPayloads()) {
            IReadoutDataPayload rdp = (IReadoutDataPayload) obj;
            loadPayload(rdp);

            String rdpDesc = getReadoutDataString(rdp);
            IUTCTime rdpFirst = rdp.getFirstTimeUTC();
            IUTCTime rdpLast = rdp.getLastTimeUTC();

            if (!validateInterval(rdpDesc, rdpFirst, rdpLast, verbose)) {
                return false;
            }

            if (!isIntervalContained(trDesc, trFirst, trLast,
                                     rdpDesc, rdpFirst, rdpLast, verbose))
            {
                return false;
            }

            validateReadoutDataPayload(rdp, verbose);

            List rdpHits = rdp.getHitList();
            if (rdpHits != null) {
                evtHits.addAll(rdpHits);
            }
        }

        if (hasSMTTrigger(trigReq) && evtHits.size() < 8) {
            LOG.error(getEventString(evt) + " contains " + evtHits.size() +
                      " hits, but should have at least 8");
            return false;
        }

        return true;
    }

    /**
     * Ensure that interval times are non-null and that first time is not
     * greater than second time.
     *
     * @param descr description of payload which contains the interval
     * @param first first time
     * @param last last time
     * @param verbose <tt>true</tt> if errors should be logged
     *
     * @return <tt>true</tt> if interval is valid
     */
    private static boolean validateInterval(String descr, IUTCTime first,
                                            IUTCTime last, boolean verbose)
    {
        if (first == null || last == null) {
            if (verbose) {
                LOG.error("Cannot get interval for " + descr);
            }

            return false;
        }

        if (first.getUTCTimeAsLong() > last.getUTCTimeAsLong()) {
            if (verbose) {
                LOG.error("Bad " + descr + " interval [" + first + "-" + last +
                          "]");
            }

            return false;
        }

        return true;
    }

    /**
     * Validate all subcomponents of a payload.
     *
     * @param pay payload
     * @param verbose <tt>true</tt> if errors should be logged
     *
     * @return <tt>true</tt> if payload is valid
     */
    public static boolean validatePayload(IPayload pay, boolean verbose)
    {
        boolean rtnVal;

        if (pay == null) {
            rtnVal = false;
        } else if (pay instanceof IEventPayload) {
            rtnVal = validateEvent((IEventPayload) pay, verbose);
        } else if (pay instanceof ITriggerRequestPayload) {
            rtnVal = validateTriggerRequest((ITriggerRequestPayload) pay,
                                            verbose);
        } else if (pay instanceof IHitPayload) {
            // hits have nothing to validate
            rtnVal = true;
        } else {
            LOG.error("Unknown payload type " + pay.getClass().getName());
            rtnVal = false;
        }

        return rtnVal;
    }

    /**
     * Validate all subcomponents of a readout data payload.
     *
     * @param rdp readout data payload
     * @param verbose <tt>true</tt> if errors should be logged
     *
     * @return <tt>true</tt> if readout data payload is valid
     */
    public static boolean validateReadoutDataPayload(IReadoutDataPayload rdp,
                                                     boolean verbose)
    {
        String rdpDesc = getReadoutDataString(rdp);
        IUTCTime rdpFirst = rdp.getFirstTimeUTC();
        IUTCTime rdpLast = rdp.getLastTimeUTC();

        for (Object obj : rdp.getDataPayloads()) {
            IHitDataPayload hit = (IHitDataPayload) obj;
            loadPayload(hit);

            String hitDesc = getHitString(hit);
            IUTCTime time = hit.getHitTimeUTC();

            if (!isIntervalContained(rdpDesc, rdpFirst, rdpLast,
                                     hitDesc, time, time, verbose))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate all subcomponents of a trigger request.
     *
     * @param tr trigger request
     * @param verbose <tt>true</tt> if errors should be logged
     *
     * @return <tt>true</tt> if trigger request is valid
     */
    public static boolean validateTriggerRequest(ITriggerRequestPayload tr,
                                                 boolean verbose)
    {
        loadPayload(tr);

        String trDesc = getTriggerRequestString(tr);
        IUTCTime trFirst = tr.getFirstTimeUTC();
        IUTCTime trLast = tr.getLastTimeUTC();
        if (!validateInterval(trDesc, trFirst, trLast, verbose)) {
            return false;
        }

        IReadoutRequest rReq = tr.getReadoutRequest();

        if (rReq != null) {
            List elemList = rReq.getReadoutRequestElements();

            IReadoutRequestElement[] elems =
                new IReadoutRequestElement[elemList.size()];

            int nextElem = 0;
            for (Object obj : elemList) {
                IReadoutRequestElement elem = (IReadoutRequestElement) obj;

                String elemDesc = getRRElementString(elem);

                IUTCTime elemFirst = elem.getFirstTimeUTC();
                IUTCTime elemLast = elem.getLastTimeUTC();
                if (!validateInterval(elemDesc, elemFirst, elemLast, verbose)) {
                    return false;
                }

                /* XXX - only validate readout requests for global triggers */
                if ((!IGNORE_NONGLOBAL_RREQS ||
                     tr.getSourceID().getSourceID() ==
                     SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID) &&
                    !isIntervalContained(trDesc, trFirst, trLast, elemDesc,
                                         elemFirst, elemLast, verbose))
                {
                    return false;
                }

                elems[nextElem++] = elem;
            }
        }

        List payList;
        try {
            payList = tr.getPayloads();
        } catch (Exception ex) {
            LOG.error("Couldn't fetch payloads for " + trDesc, ex);
            return false;
        }

        for (Object obj : payList) {
            if (obj instanceof ITriggerRequestPayload) {
                ITriggerRequestPayload subTR = (ITriggerRequestPayload) obj;
                loadPayload(subTR);

                String subDesc = getTriggerRequestString(subTR);
                IUTCTime subFirst = subTR.getFirstTimeUTC();
                IUTCTime subLast = subTR.getLastTimeUTC();
                if (!validateInterval(subDesc, subFirst, subLast, verbose)) {
                    return false;
                }

                if (!isIntervalContained(trDesc, trFirst, trLast,
                                         subDesc, subFirst, subLast, verbose))
                {
                    return false;
                }

                if (!validateTriggerRequest(subTR, verbose)) {
                    return false;
                }
            } else if (obj instanceof IHitPayload) {
                IHitPayload hit = (IHitPayload) obj;
                loadPayload(hit);

                IUTCTime time = hit.getHitTimeUTC();
                String hitDesc = "hit@" + time;

                if (!isIntervalContained(trDesc, trFirst, trLast,
                                         hitDesc, time, time, verbose))
                {
                    return false;
                }
            } else {
                LOG.error("Unknown payload type " + obj.getClass().getName() +
                          " in " + trDesc);
                return false;
            }
        }

        return true;
    }
}
