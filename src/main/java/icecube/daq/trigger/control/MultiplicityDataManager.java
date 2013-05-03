package icecube.daq.trigger.control;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.trigger.exceptions.MultiplicityDataException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

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

    private long endTime;
    private int count;

    CountData(long endTime, int count)
    {
        this.endTime = endTime;
        this.count = count;
    }

    Map<String, Object> getValuesMap(int srcId, int type, int cfgId)
    {
        HashMap<String, Object> values = new HashMap<String, Object>();

        values.put("sourceid", Integer.valueOf(srcId));
        values.put("trigid", Integer.valueOf(type));
        values.put("configid", Integer.valueOf(cfgId));

        values.put("count", Integer.valueOf(count));
        values.put("recordingStartTime",
                   UTCTime.toDateString(endTime - DAQ_BIN_WIDTH + 1));
        values.put("recordingEndTime", UTCTime.toDateString(endTime));

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

    synchronized void inc(IUTCTime firstTime, IUTCTime lastTime, int bin)
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
            counts.add(new CountData(endTime, count));
            endTime += CountData.DAQ_BIN_WIDTH;
            count = 0;
        }

        count++;
    }

    public String toString()
    {
        return "Bins[max=" + maxLen + ",ovflo=" + overflow + "]";
    }
}

public class MultiplicityDataManager
    implements IMonitoringDataManager
{
    /** Log object for this class */
    private static final Log LOG =
        LogFactory.getLog(MultiplicityDataManager.class);

    private static final int NUM_BINS = 200;

    private static final int NO_NUMBER = Integer.MIN_VALUE;

    private Alerter alerter;
    private HashMap<HashKey, Bins> map;

    private Calendar startTime;
    private int runNumber;

    private int nextRunNumber = NO_NUMBER;

    public MultiplicityDataManager(Alerter alerter)
    {
        this.alerter = alerter;
    }

    public void add(ITriggerRequestPayload req)
        throws MultiplicityDataException
    {
        if (map == null) {
            final String msg = "MultiplicityDataManager has not been started";
            throw new MultiplicityDataException(msg);
        }

        if (!req.isMerged() && req.getTriggerConfigID() != -1) {
            // this is a real request

            HashKey key;
            try {
                key = new HashKey(req);
            } catch (MultiplicityDataException mde) {
                throw new MultiplicityDataException("Cannot build key for " +
                                                    req, mde);
            }

            if (!map.containsKey(key)) {
                // add new algorithm
                map.put(key, new Bins(NUM_BINS));
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
            map.get(key).inc(req.getFirstTimeUTC(), req.getLastTimeUTC(), bin);
        } else {
            // extract list of merged triggers
            List subList;
            try {
                subList = req.getPayloads();
            } catch (DataFormatException dfe) {
                LOG.error("Cannot fetch triggers from " + req, dfe);
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
                } catch (DataFormatException dfe) {
                    LOG.error("Cannot load subtrigger " + sub, dfe);
                    continue;
                }

                add(sub);
            }
        }
    }

    public List<Map> getCounts()
        throws MultiplicityDataException
    {
        if (map == null) {
            final String msg = "MultiplicityDataManager has not been started";
            throw new MultiplicityDataException(msg);
        } else if (map.size() == 0) {
            // don't bother sending empty list
            return null;
        } else if (!alerter.isActive()) {
            final String msg = "Alerter " + alerter + " is not active";
            throw new MultiplicityDataException(msg);
        }

        List<Map> list = new ArrayList<Map>();
        for (HashKey key : map.keySet()) {
            Bins bins = map.get(key);
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

        return list;
    }

    public void reset()
        throws MultiplicityDataException
    {
        if (nextRunNumber == NO_NUMBER) {
            final String msg = "Next run number has not been set";
            throw new MultiplicityDataException(msg);
        }

        start(nextRunNumber);
        nextRunNumber = NO_NUMBER;
    }

    public boolean send()
        throws MultiplicityDataException
    {
        if (map == null) {
            final String msg = "MultiplicityDataManager has not been started";
            throw new MultiplicityDataException(msg);
        } else if (map.size() == 0) {
            // don't bother sending empty list
            return false;
        } else if (!alerter.isActive()) {
            final String msg = "Alerter " + alerter + " is not active";
            throw new MultiplicityDataException(msg);
        }

        Calendar endTime = Calendar.getInstance();

        SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");

        for (HashKey key : map.keySet()) {
            HashMap<String, Object> values = new HashMap<String, Object>();

            values.put("sourceid", key.getSourceID());
            values.put("trigid", key.getType());
            values.put("configid", key.getConfigID());

            values.put("hist", map.get(key).getBinData());
            values.put("timeOfFirstEntry",
                       dateFormat.format(startTime.getTime()));
            values.put("timeOfLastEntry",
                       dateFormat.format(endTime.getTime()));
            values.put("runNumber", runNumber);

            try {
                alerter.send("trigger_multiplicity", Alerter.Priority.SCP,
                             values);
            } catch (AlertException ae) {
                throw new MultiplicityDataException("Cannot send alert", ae);
            }
        }

        return true;
    }

    public void setNextRunNumber(int runNum)
        throws MultiplicityDataException
    {
        if (nextRunNumber != NO_NUMBER) {
            final String msg = "Next run number has already been set";
            throw new MultiplicityDataException(msg);
        }

        nextRunNumber = runNum;
    }

    public void start(int runNum)
    {
        startTime = Calendar.getInstance();
        runNumber = runNum;

        map = new HashMap<HashKey, Bins>();
    }
}
