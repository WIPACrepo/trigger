package icecube.daq.trigger.control;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.AlertQueue;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadFormatException;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.exceptions.MultiplicityDataException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Build a hash key from the trigger triplet (source, type, config ID)
 * XXX This code assumes that srcId is [0,31]*1000 and type is [0,255]
 */
class HashKey
    implements Comparable
{
    private int srcId;
    private int type;
    private int cfgId;
    private int key;

    HashKey(ITriggerRequestPayload req)
        throws MultiplicityDataException
    {
        this(req.getSourceID().getSourceID(), req.getTriggerType(),
             req.getTriggerConfigID());
    }

    HashKey(int srcId, int type, int cfgId)
        throws MultiplicityDataException
    {
        this.srcId = srcId;
        this.type = type;
        this.cfgId = cfgId;

        int tmpSrc = srcId / 1000;
        if (tmpSrc < 0 || tmpSrc > 31) {
            throw new MultiplicityDataException("Bad source ID " + srcId);
        }

        if (type < 0 || type > 255) {
            throw new MultiplicityDataException("Bad trigger type " + type);
        }

        if (cfgId < 0 || cfgId > Integer.MAX_VALUE >> 13) {
            throw new MultiplicityDataException("Bad trigger config ID " +
                                                cfgId);
        }

        key = tmpSrc + (type << 5) + (cfgId << 13);
    }

    public int compareTo(Object obj)
    {
        if (obj == null) {
            return 1;
        }

        HashKey other = (HashKey) obj;
        return key - other.key;
    }

    public boolean equals(Object obj)
    {
        return compareTo(obj) == 0;
    }

    public int getSourceID()
    {
        return srcId;
    }

    public int getType()
    {
        return type;
    }

    public int getConfigID()
    {
        return cfgId;
    }

    public int hashCode()
    {
        return key;
    }

    public String toString()
    {
        return String.format("s%d/t%d/c%d/k%d", srcId, type, cfgId, key);
    }
}

class CountData
{
    public static final long SECONDS_PER_BIN = 60;
    public static final long DAQ_SECOND = 10000000000L;
    public static final long DAQ_BIN_WIDTH = DAQ_SECOND * SECONDS_PER_BIN;

    public static final int RATE_VERSION = 0;

    private int runNumber;
    private long endTime;
    private int count;

    CountData(int runNumber, long endTime, int count)
    {
        this.runNumber = runNumber;
        this.endTime = endTime;
        this.count = count;
    }

    Map<String, Object> getValuesMap(int srcId, int type, int cfgId)
    {
        HashMap<String, Object> values = new HashMap<String, Object>();

        values.put("runNumber", Integer.valueOf(runNumber));

        values.put("sourceid", Integer.valueOf(srcId));
        values.put("trigid", Integer.valueOf(type));
        values.put("configid", Integer.valueOf(cfgId));

        values.put("value", Integer.valueOf(count));
        values.put("recordingStartTime",
                   UTCTime.toDateString(endTime - DAQ_BIN_WIDTH + 1));
        values.put("recordingStopTime", UTCTime.toDateString(endTime));
        values.put("version", RATE_VERSION);

        return values;
    }
}

class Bins
{
    private static final String XLABEL = "nchannels";
    private static final String YLABEL = "nentries";

    private int[] bins;
    private int overflow;
    private int maxLen;

    private long endTime = Long.MIN_VALUE;
    private int count;

    private ArrayList<CountData> counts = new ArrayList<CountData>();

    Bins(int maxBins)
    {
        bins = new int[maxBins];
    }

    synchronized Map<String, Object> getBinData()
    {
        int[] finalBins;
        if (maxLen >= bins.length) {
            finalBins = bins;
        } else {
            finalBins = Arrays.copyOfRange(bins, 0, maxLen);
        }

        int sum = 0;
        for (int i = 0; i < finalBins.length; i++) {
            sum += finalBins[i];
        }

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("binContents", finalBins);
        map.put("xmin", 0);
        map.put("xmax", finalBins.length);
        map.put("underflow", 0);
        map.put("overflow", overflow);
        map.put("xlabel", XLABEL);
        map.put("ylabel", YLABEL);
        map.put("nentries", sum);

        return map;
    }

    synchronized Map<String, Object> getCountData(int srcId, int type,
                                                  int cfgId)
    {
        if (counts.size() == 0) {
            return null;
        }

        return counts.remove(0).getValuesMap(srcId, type, cfgId);
    }

    synchronized void inc(IUTCTime firstTime, IUTCTime lastTime, int runNumber,
                          int bin)
    {
        if (bin >= maxLen) {
            maxLen = bin + 1;
        }
        if (bin >= bins.length) {
            overflow = 1;
        } else {
            bins[bin]++;
        }

        if (this.endTime == Long.MIN_VALUE) {
            this.endTime = firstTime.longValue() + CountData.DAQ_BIN_WIDTH;
        }

        while (lastTime.longValue() > endTime) {
            counts.add(new CountData(runNumber, endTime, count));
            endTime += CountData.DAQ_BIN_WIDTH;
            count = 0;
        }

        count++;
    }

    public String toString()
    {
        return "Bins[max=" + maxLen + ",ovflo=" + overflow +
            ",end=" + endTime + ",cnt=" + count + "]";
    }
}

public class MultiplicityDataManager
    implements IMonitoringDataManager
{
    public static final int MULTIPLICITY_VERSION = 0;

    /** Log object for this class */
    private static final Log LOG =
        LogFactory.getLog(MultiplicityDataManager.class);

    private static final int NUM_BINS = 200;

    private static final int NO_NUMBER = Integer.MIN_VALUE;

    /** Complete list of all configured algorithms for <b>all</b> handlers */
    private List<INewAlgorithm> algorithms;

    private AlertQueue alertQueue;
    private HashMap<HashKey, Bins> binmap;

    private Calendar startTime;
    private int runNumber = NO_NUMBER;
    private long firstGoodTime = Integer.MIN_VALUE;
    private long lastGoodTime = Integer.MIN_VALUE;

    private int nextRunNumber = NO_NUMBER;

    public MultiplicityDataManager()
    {
        algorithms = new ArrayList<INewAlgorithm>();
    }

    public void add(ITriggerRequestPayload req)
        throws MultiplicityDataException
    {
        if (binmap == null) {
            final String msg = "MultiplicityDataManager has not been started";
            throw new MultiplicityDataException(msg);
        }

        if (req.isMerged() || req.getTriggerConfigID() == -1) {
            // extract list of merged triggers
            List subList;
            try {
                subList = req.getPayloads();
            } catch (PayloadFormatException pfe) {
                LOG.error("Cannot fetch triggers from " + req, pfe);
                return;
            }

            if (subList == null) {
                LOG.error("No subtriggers found in " + req);
                return;
            }

            // count individual triggers
            for (Object obj : subList) {
                ITriggerRequestPayload sub = (ITriggerRequestPayload) obj;

                try {
                    sub.loadPayload();
                } catch (IOException ioe) {
                    LOG.error("Cannot load subtrigger " + sub, ioe);
                    continue;
                } catch (PayloadFormatException pfe) {
                    LOG.error("Cannot load subtrigger " + sub, pfe);
                    continue;
                }

                add(sub);
            }
            return;
        }

        // ignore algorithms with invalid multiplicities (SlowMP, FixedRate)
        if (!hasValidMultiplicity(req)) {
            return;
        }

        // ignore requests seen before the first good time
        if (firstGoodTime == Integer.MIN_VALUE) {
            final String msg =
                String.format("Not monitoring request #%d seen before" +
                              " first good time is set", req.getUID());
            LOG.error(msg);
            return;
        }

        final long reqFirst = req.getFirstTimeUTC().longValue();
        final long reqLast = req.getLastTimeUTC().longValue();

        // ignore non-good requests
        if (reqFirst < firstGoodTime) {
            // don't count requests starting before the first good time
            return;
        } else if (lastGoodTime > 0 && reqLast > lastGoodTime) {
            // don't count requests ending after the last good time
            return;
        }

        // and we've finally got something we can monitor!

        HashKey key;
        try {
            key = new HashKey(req);
        } catch (MultiplicityDataException mde) {
            throw new MultiplicityDataException("Cannot build key for " +
                                                req, mde);
        }

        List payloads;
        try {
            payloads = req.getPayloads();
        } catch (Exception ex) {
            final String msg = "Cannot get payloads for " + req;
            throw new MultiplicityDataException(msg);
        }

        int bin;
        if (payloads == null) {
            bin = 0;
        } else {
            bin = payloads.size();
        }

        synchronized (binmap) {
            if (!binmap.containsKey(key)) {
                // add new algorithm
                binmap.put(key, new Bins(NUM_BINS));
            }
            binmap.get(key).inc(req.getFirstTimeUTC(), req.getLastTimeUTC(),
                                runNumber, bin);
        }
    }

    public List<Map<String, Object>> getCounts()
        throws MultiplicityDataException
    {
        if (binmap == null) {
            final String msg =
                "MultiplicityDataManager has not been started";
            throw new MultiplicityDataException(msg);
        }

        List<Map<String, Object>> list = null;
        synchronized (binmap) {
            if (binmap.size() == 0) {
                // don't bother sending empty list
                list = null;
            } else {
                list = new ArrayList<Map<String, Object>>();
                for (HashKey key : binmap.keySet()) {
                    Bins bins = binmap.get(key);
                    while (true) {
                        Map<String, Object> values =
                            bins.getCountData(key.getSourceID(), key.getType(),
                                              key.getConfigID());
                        if (values == null) {
                            break;
                        }

                        list.add(values);
                    }
                }
            }
        }

        return list;
    }

    /**
     * Add an algorithm to the list.
     *
     * @param algorithm trigger algorithm
     */
    public void addAlgorithm(INewAlgorithm algorithm)
    {
        algorithms.add(algorithm);
    }

    /**
     * Is this request from an algorithm which includes all the relevant hits?
     *
     * SlowMPTrigger and FixedRateTrigger only save the first and last hits.
     *
     * @return <tt>true</tt> if this request should have a valid multiplicity
     */
    private boolean hasValidMultiplicity(ITriggerRequestPayload req)
    {
        for (INewAlgorithm a : algorithms) {
            if (req.getTriggerConfigID() == a.getTriggerConfigId() &&
                req.getTriggerType() == a.getTriggerType() &&
                req.getSourceID().getSourceID() == a.getSourceId())
            {
                return a.hasValidMultiplicity();
            }
        }

        LOG.error("Found request from unknown algorithm with source ID " +
                  req.getSourceID().getSourceID() + " configID " +
                  req.getTriggerConfigID() + " type " + req.getTriggerType());
        return false;
    }

    public void reset()
        throws MultiplicityDataException
    {
        if (nextRunNumber == NO_NUMBER) {
            final String msg = "Next run number has not been set";
            throw new MultiplicityDataException(msg);
        } else if (binmap == null) {
            final String msg =
                "MultiplicityDataManager has not been started";
            throw new MultiplicityDataException(msg);
        }

        synchronized (binmap) {
            start(nextRunNumber);
            binmap.clear();
        }

        nextRunNumber = NO_NUMBER;
    }

    public boolean send()
        throws MultiplicityDataException
    {
        if (binmap == null) {
            final String msg =
                "MultiplicityDataManager has not been started";
            throw new MultiplicityDataException(msg);
        }

        ArrayList<Map<String, Object>> valueList =
            new ArrayList<Map<String, Object>>();

        synchronized (binmap) {
            if (runNumber == NO_NUMBER) {
                final String msg = "Run number has not been set";
                throw new MultiplicityDataException(msg);
            } else if (binmap.size() == 0) {
                // don't bother sending empty list
                return false;
            } else if (alertQueue == null) {
                throw new MultiplicityDataException("AlertQueue has not" +
                                                    " been set");
            } else if (alertQueue.isStopped()) {
                final String msg = "AlertQueue " + alertQueue + " is stopped";
                throw new MultiplicityDataException(msg);
            }

            Calendar endTime = Calendar.getInstance();

            SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");

            for (HashKey key : binmap.keySet()) {
                HashMap<String, Object> values = new HashMap<String, Object>();

                values.put("sourceid", key.getSourceID());
                values.put("trigid", key.getType());
                values.put("configid", key.getConfigID());

                values.put("hist", binmap.get(key).getBinData());
                values.put("timeOfFirstEntry",
                           dateFormat.format(startTime.getTime()));
                values.put("timeOfLastEntry",
                           dateFormat.format(endTime.getTime()));
                values.put("runNumber", runNumber);
                values.put("version", MULTIPLICITY_VERSION);

                valueList.add(values);
            }

            startTime = Calendar.getInstance();
        }

        for (Map<String, Object> values : valueList) {
            try {
                alertQueue.push("trigger_multiplicity", Alerter.Priority.SCP,
                                values);
            } catch (AlertException ae) {
                throw new MultiplicityDataException("Cannot send alert", ae);
            }
        }

        return true;
    }

    public void setAlertQueue(AlertQueue alertQueue)
    {
        this.alertQueue = alertQueue;

        if (alertQueue != null && alertQueue.isStopped()) {
            alertQueue.start();
        }
    }

    /**
     * Set the first "good" time for the current run.
     *
     * @param firstTime first "good" time
     */
    public void setFirstGoodTime(long firstTime)
    {
        this.firstGoodTime = firstTime;
    }

    /**
     * Set the last "good" time for the current run.
     *
     * @param lastTime last "good" time
     */
    public void setLastGoodTime(long lastTime)
    {
        this.lastGoodTime = lastTime;
    }

    public void setNextRunNumber(int runNum)
        throws MultiplicityDataException
    {
        if (nextRunNumber != NO_NUMBER) {
            final String msg = "Cannot set next run number to " +
                runNum + "; already set to " + nextRunNumber;
            throw new MultiplicityDataException(msg);
        }

        nextRunNumber = runNum;
    }

    public void start(int runNum)
    {
        runNumber = runNum;

        if (startTime == null) {
            startTime = Calendar.getInstance();
        }

        if (binmap == null) {
            binmap = new HashMap<HashKey, Bins>();
        }
    }
}
