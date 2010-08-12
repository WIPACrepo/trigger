package icecube.daq.trigger.monitor;

import icecube.daq.oldpayload.impl.TriggerRequestPayload;
import icecube.daq.payload.IUTCTime;

import java.io.IOException;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: toale
 * Date: Mar 20, 2007
 * Time: 11:11:03 AM
 */
public class AmandaTriggerAnalyzer {

    private static final Log log = LogFactory.getLog(AmandaTriggerAnalyzer.class);

    private int veryFirstUID = -1;
    private int lastUID = -1;
    private IUTCTime veryFirstTime = null;
    private IUTCTime lastTime = null;

    private long[] bitCount = new long[512];
    private long[] deltaHist = new long[101];
    private double deltaSum = 0;
    private double deltaSqSum = 0;
    private int deltaCount = 0;

    public AmandaTriggerAnalyzer() {
        for (int i=0; i<512; i++) {
            bitCount[i] = 0;
        }
        for (int i=0; i<101; i++) {
            deltaHist[i] = 0;
        }
    }

    public void analyze(TriggerRequestPayload trigger) {

        try {
            trigger.loadPayload();
        } catch (DataFormatException dfe) {
            log.error("Error loading payload: ", dfe);
        }

        int uid = trigger.getUID();
        int config = trigger.getTriggerConfigID();
        IUTCTime time = trigger.getFirstTimeUTC();

        processUID(uid);
        processMask(config);
        processTime(time);
    }

    private void processMask(int mask) {
        bitCount[mask-1]++;
    }

    private void processUID(int uid) {
        if (veryFirstUID == -1) veryFirstUID = uid;

        if (lastUID != -1) {
            int delta = uid - lastUID;
            if (delta != 1) {
                log.warn("MISSING UID: last uid = " + lastUID + "  current uid = " + uid);
            }
        }

        lastUID = uid;
    }

    private void processTime(IUTCTime time) {
        if (veryFirstTime == null) veryFirstTime = time;

        if (lastTime != null) {
            double delta = (time.timeDiff_ns(lastTime)/1000000.0);
            int bin = (int) delta;
            if (bin > 100) bin = 100;
            deltaHist[bin]++;

            deltaSum += delta;
            deltaSqSum += (delta*delta);
            deltaCount++;
        }


        lastTime = time;
    }

    public void dump() {

        if (!log.isInfoEnabled()) {
            return;
        }

        log.info("BitMask count:");
        for (int i=0; i<512; i++) {
            int mask = i+1;
            if (mask < 10) {
                log.info("   " + mask + " : " + bitCount[i]);
            } else if (mask < 100) {
                log.info("  " + mask + " : " + bitCount[i]);
            } else {
                log.info(" " + mask + " : " + bitCount[i]);
            }
        }

        log.info("Delta T:");
        for (int i=0; i<101; i++) {
            if (i < 10) {
                log.info("   " + i + " : " + deltaHist[i]);
            } else if (i < 100) {
                log.info("  " + i + " : " + deltaHist[i]);
            } else {
                log.info(" " + i + " : " + deltaHist[i]);
            }
        }
        if (deltaCount > 0) {
            double avg = deltaSum/deltaCount;
            double sig = 0;
            if (deltaCount > 1) {
                double sigSq = (deltaSqSum - deltaSum*deltaSum/deltaCount)/(deltaCount - 1);
                if (sigSq > 0) sig = Math.sqrt(sigSq);
            }

            log.info("Mean = " + avg + "    Sigma = " + sig);
        }



    }

}
