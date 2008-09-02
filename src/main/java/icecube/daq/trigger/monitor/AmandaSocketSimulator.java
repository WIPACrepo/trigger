package icecube.daq.trigger.monitor;

import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID4B;
import icecube.daq.payload.impl.UTCTime8B;
import icecube.daq.payload.splicer.Payload;
import icecube.daq.trigger.IReadoutRequest;
import icecube.daq.trigger.ITriggerRequestPayload;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.icebucket.logging.LoggingConsumer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: toale
 * Date: Mar 19, 2007
 * Time: 2:18:53 PM
 */
public class AmandaSocketSimulator
        implements Runnable {

    private static final Log log = LogFactory.getLog(AmandaSocketSimulator.class);

    private static final int DEFAULT_PORT = 12345;
    private static final double DEFAULT_RATE = 200;

    private final double rate;
    private final ServerSocket server;

    private boolean running = false;

    private int count = 0;
    private long lastTime = 0;
    private Random random;
    private TriggerRequestPayloadFactory triggerFactory = new TriggerRequestPayloadFactory();
    private long waitTime;
    private ByteBuffer buffer = ByteBuffer.allocate(128*72);

    public AmandaSocketSimulator() throws IOException {
        this(DEFAULT_PORT);
    }

    public AmandaSocketSimulator(int port) throws IOException {
        rate = DEFAULT_RATE;
        waitTime = (long) (1000*128/rate);
        random = new Random(System.currentTimeMillis());
        server = new ServerSocket(port);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (running) {
            stop();
        }
        server.close();
    }

    public void start() {
        if (!running) {
            log.info("Starting socket simulation...");
            running = true;
            Thread simulator = new Thread(this);
            simulator.start();
        }
    }

    public void stop() {
        log.info("Stopping socket simulation...");
        running = false;
    }

    public void run() {

        while (running) {
            // Wait for connection
            Socket socket = null;
            DataOutputStream output = null;
            try {
                log.info("Wait for connection...");
                socket = server.accept();
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                log.error("Error accepting connection: ", e);
            }

            if (socket != null) {

                // Send triggers
                int nSent = 0;
                while (running) {
                    fillBuffer();
                    try {
                        output.write(buffer.array());
                    } catch (IOException e) {
                        log.error("Error writing to output stream: ", e);
                        break;
                    }
                    if (log.isInfoEnabled()) {
                        log.info("Sent " + nSent);
                    }
                    nSent++;
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        // interrupted
                    }
                }

                // Close connection
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("Error closing connection: ", e);
                }
            }
        }

    }

    /**
     * Generate a random 9 bit integer (not including 0)
     * @return random mask
     */
    private int generateTriggerMask() {
        return (1 + random.nextInt(512));
    }

    /**
     * Generate a delta t (in 1/10 nanoseconds) based on the rate
     * @return waiting time in DAQ units
     */
    private long generateDelta() {
        double toss;
        do {
            toss = random.nextDouble();
        } while (toss <= 0.0);
        return (long) (-1e10*Math.log(toss)/rate);
    }

    /**
     * Generate the next TriggerRequestPayload
     * @return a trigger payload
     */
    private ITriggerRequestPayload generateTrigger() {

        int triggerType = 0;
        int configId = generateTriggerMask();
        ISourceID sourceId =
            new SourceID4B(SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID);
        long nextTime = lastTime + generateDelta();
        IUTCTime time = new UTCTime8B(nextTime);
        Vector payloads = new Vector();
        Vector readouts = new Vector();
        IReadoutRequest readout = TriggerRequestPayloadFactory.createReadoutRequest(sourceId, count, readouts);
        Payload trigger = triggerFactory.createPayload(count, triggerType, configId, sourceId, time, time, payloads,readout);
        count++;
        lastTime = nextTime;

        return (ITriggerRequestPayload) trigger;
    }

    private void fillBuffer() {
        buffer.clear();
        for (int i=0; i<128; i++) {
            int offset = i*72;
            ITriggerRequestPayload trigger = generateTrigger();
            if (log.isInfoEnabled()) {
                log.info("Trigger size = " + trigger.getPayloadLength());
            }
            try {
                trigger.writePayload(false, offset, buffer);
            } catch (IOException e) {
                log.error("Error writing to buffer: ", e);
            }
        }
        buffer.flip();
    }

    public static void main(String[] args) {

        LoggingConsumer.installDefault();

        int port = DEFAULT_PORT;
        if (args.length < 1) {
            usage();
        } else {
            port = Integer.parseInt(args[0]);
        }

        AmandaSocketSimulator amanda = null;
        try {
            amanda = new AmandaSocketSimulator(port);
        } catch (IOException e) {
            log.fatal("IOException creating server side socket on port " + port + ": ", e);
        }

        if (amanda != null) {
            amanda.start();
        }

    }

    private static void usage() {
        log.info("Usage: AmandaSocketSimulator [port]");
        System.exit(-1);
    }


}
