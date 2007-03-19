package icecube.daq.trigger.monitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import icecube.icebucket.logging.LoggingConsumer;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: toale
 * Date: Mar 19, 2007
 * Time: 2:18:53 PM
 */
public class AmandaSocketSimulator implements Runnable {


    private static final Log log = LogFactory.getLog(AmandaSocketSimulator.class);

    private static final int DEFAULT_PORT = 12345;
    private static final int DEFAULT_NTRIGS = 128*100;

    private final int nTrigs;
    private final ServerSocket server;

    private boolean running = false;

    public AmandaSocketSimulator() throws IOException {
        this(DEFAULT_PORT);
    }

    public AmandaSocketSimulator(int port) throws IOException {
        nTrigs = DEFAULT_NTRIGS;

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
            try {
                log.info("Wait for connection...");
                socket = server.accept();
            } catch (IOException e) {
                log.error("Error accepting connection: ", e);
            }

            if (socket != null) {

                // Send triggers
                int nSent = 0;
                while (running && (nSent < nTrigs)) {
                    log.info("Sent " + nSent);
                    nSent++;
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
