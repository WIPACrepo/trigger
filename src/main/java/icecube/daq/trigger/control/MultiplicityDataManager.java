package icecube.daq.trigger.control;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.AlertQueue;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadFormatException;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.trigger.algorithm.ITriggerAlgorithm;
import icecube.daq.trigger.exceptions.MultiplicityDataException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

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
    private boolean validMultiplicity;

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
        this.validMultiplicity = validMultiplicity;

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

    @Override
    public int compareTo(Object obj)
    {
        if (obj == null) {
            return 1;
        }

        HashKey other = (HashKey) obj;
        return key - other.key;
    }

    @Override
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

    public boolean hasValidMultiplicity()
    {
        return validMultiplicity;
    }

    @Override
    public int hashCode()
    {
        return key;
    }

    public void setValidMultiplicity(boolean val)
    {
        validMultiplicity = val;
    }

    @Override
    public String toString()
    {
        return String.format("s%d/t%d/c%d/k%d", srcId, type, cfgId, key);
    }
}

/**
 * This is essentially a 'struct' to hold three values
 */
class CountData
{
    private int runNumber;
    private long endTime;
    private int count;

    CountData(int runNumber, long endTime, int count)
    {
        this.runNumber = runNumber;
        this.endTime = endTime;
        this.count = count;
    }

    int getCount()
    {
        return count;
    }

    long getEndTime()
    {
        return endTime;
    }

    int getRunNumber()
    {
        return runNumber;
    }
}

class Bins
{
    public static final int RATE_VERSION = 0;

    private static final String XLABEL = "nchannels";
    private static final String YLABEL = "nentries";

    public static final long SECONDS_PER_BIN = 60;
    public static final long DAQ_TICKS_PER_SECOND = 10000000000L;
    public static final long WIDTH =
        DAQ_TICKS_PER_SECOND * SECONDS_PER_BIN;

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

    private void addBin(int runNumber)
    {
        counts.add(new CountData(runNumber, endTime, count));
        count = 0;
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

    synchronized Map<String, Object> getSummary(int srcId, int type,
                                                int cfgId, int numBins,
                                                boolean allowPartial)
    {
        if (counts.size() == 0 || (!allowPartial && counts.size() < numBins)) {
            return null;
        }

        // if we're returning a partial summary, get the correct number of bins
        if (counts.size() < numBins) {
            numBins = counts.size();
        }

        int runNumber = Integer.MAX_VALUE;
        long startTime = Long.MIN_VALUE;
        long endTime = Long.MAX_VALUE;
        int total = 0;

        for (int i = 0; i < numBins; i++) {
            CountData cd = counts.remove(0);
            if (i == 0) {
                runNumber = cd.getRunNumber();
                startTime = cd.getEndTime() - WIDTH + 1;
            } else if (cd.getRunNumber() != runNumber) {
                // next bin is for a different run
                counts.add(0, cd);
                break;
            }

            endTime = cd.getEndTime();
            total += cd.getCount();
        }

        HashMap<String, Object> values = new HashMap<String, Object>();

        values.put("runNumber", Integer.valueOf(runNumber));

        values.put("sourceid", Integer.valueOf(srcId));
        values.put("trigid", Integer.valueOf(type));
        values.put("configid", Integer.valueOf(cfgId));

        values.put("value", Integer.valueOf(total));
        values.put("recordingStartTime", UTCTime.toDateString(startTime));
        values.put("recordingStopTime", UTCTime.toDateString(endTime));
        values.put("version", RATE_VERSION);

        return values;
    }

    /**
     * Increment the specified bin and total count
     *
     * @param firstTime start time of trigger
     * @param lastTime end time of trigger
     * @param runNumber current run number
     * @param bin bin number
     */
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

        if (endTime == Long.MIN_VALUE) {
            endTime = firstTime.longValue() + WIDTH;
        }

        // (lastTime - firstTime) may span multiple bins;
        // first bin gets full count, remaining bins set to 0
        while (lastTime.longValue() > endTime) {
            addBin(runNumber);
            endTime += WIDTH;
        }

        count++;
    }

    @Override
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
    private static final Logger LOG =
        Logger.getLogger(MultiplicityDataManager.class);

    private static final int MAX_BINS = 200;

    private static final int NO_NUMBER = Integer.MIN_VALUE;

    /** Complete list of all configured algorithms for <b>all</b> handlers */
    private List<ITriggerAlgorithm> algorithms;

    private AlertQueue alertQueue;
    private HashMap<HashKey, Bins> binmap;

    private Calendar startTime;
    private int runNumber = NO_NUMBER;
    private long firstGoodTime = Integer.MIN_VALUE;
    private long lastGoodTime = Integer.MIN_VALUE;

    private int nextRunNumber = NO_NUMBER;

    public MultiplicityDataManager()
    {
        algorithms = new ArrayList<ITriggerAlgorithm>();
    }

    @Override
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
                // will this provide a valid multiplicity value?
                key.setValidMultiplicity(hasValidMultiplicity(req));

                // add new algorithm
                binmap.put(key, new Bins(MAX_BINS));
            }
            binmap.get(key).inc(req.getFirstTimeUTC(), req.getLastTimeUTC(),
                                runNumber, bin);
        }
    }

    /**
     * Add an algorithm to the list.
     *
     * @param algorithm trigger algorithm
     */
    public void addAlgorithm(ITriggerAlgorithm algorithm)
    {
        algorithms.add(algorithm);
    }

    /**
     * Add together up to <tt>numBins</tt> bins and return totals
     *
     * @param numBins number of bins to summarize
     * @param allowPartial if <tt>true</tt>, return a summary of less than
     *                     <tt>numBins</tt> bins
     *
     * @return <tt>null</tt> if there are not enough bins to summarize
     */
    public Iterable<Map<String, Object>> getSummary(int numBins,
                                                    boolean allowPartial,
                                                    boolean allowEmptyBins)
        throws MultiplicityDataException
    {
        if (binmap == null) {
            final String msg =
                "MultiplicityDataManager has not been started";
            throw new MultiplicityDataException(msg);
        }

        List<Map<String, Object>> list = null;
        synchronized (binmap) {
            if (binmap.size() == 0 && !allowEmptyBins) {
                // don't bother sending empty list
                list = null;
            } else {
                list = new ArrayList<Map<String, Object>>();
                for (HashKey key : binmap.keySet()) {
                    Bins bins = binmap.get(key);
                    while (true) {
                        Map<String, Object> values =
                            bins.getSummary(key.getSourceID(), key.getType(),
                                            key.getConfigID(), numBins,
                                            allowPartial);
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
     * Is this request from an algorithm which includes all the relevant hits?
     *
     * SlowMPTrigger and FixedRateTrigger only save the first and last hits.
     *
     * @return <tt>true</tt> if this request should have a valid multiplicity
     */
    private boolean hasValidMultiplicity(ITriggerRequestPayload req)
    {
        for (ITriggerAlgorithm a : algorithms) {
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

    @Override
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

    @Override
    public boolean sendFinal()
        throws MultiplicityDataException
    {
        MultiplicityDataException delayed = null;
        try {
            // send final bin(s) of data
            sendSingleBin(true);
        } catch (MultiplicityDataException mde) {
            if (delayed == null) {
                // cache first exception and throw it when we're done
                delayed = mde;
            }
            delayed = mde;
        }

        boolean rtnval;
        try {
            // send multiplicity data for entire run
            rtnval = sendMultiplicity();
        } catch (MultiplicityDataException mde) {
            if (delayed == null) {
                // cache first exception and throw it when we're done
                delayed = mde;
            }
            rtnval = false;
        }

        if (delayed != null) {
            // throw cached exception
            throw delayed;
        }

        return rtnval;
    }

    private boolean sendMultiplicity()
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
                if (!key.hasValidMultiplicity()) {
                    continue;
                }

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

    /**
     * Send the current bin of data to I3Live
     *
     * @return list of trigger count data.
     */
    @Override
    public boolean sendSingleBin(boolean isFinal)
        throws MultiplicityDataException
    {
        return sendTriggerCounts(isFinal);
    }

    private boolean sendTriggerCounts(boolean isFinal)
        throws MultiplicityDataException
    {
        if (alertQueue == null) {
            throw new MultiplicityDataException("AlertQueue has not" +
                                                " been set");
        }

        Iterable<Map<String, Object>> mapper = getSummary(10, isFinal, true);
        if (mapper == null) {
            // if there's no data, we're done
            return false;
        }

        for (Map<String, Object> values : mapper) {
            try {
                alertQueue.push("trigger_rate", Alerter.Priority.EMAIL,
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
