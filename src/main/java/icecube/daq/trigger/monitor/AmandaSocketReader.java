package icecube.daq.trigger.monitor;

import icecube.icebucket.logging.LoggingConsumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.Socket;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: toale
 * Date: Mar 19, 2007
 * Time: 2:01:29 PM
 */
public class AmandaSocketReader {

    private static final Log log = LogFactory.getLog(AmandaSocketReader.class);

    private static final String DEFAULT_HOSTNAME = "localhost";
    private static final int DEFAULT_PORT = 12345;
    private static final int DEFAULT_NBYTES = 72*128;

    private InetAddress host = null;
    private final int port;
    private final int nBytes;

    public AmandaSocketReader() {
        this(DEFAULT_HOSTNAME, DEFAULT_PORT, DEFAULT_NBYTES);
    }

    public AmandaSocketReader(String hostname, int port, int nBytes) {
        this.port = port;
        this.nBytes = nBytes;
        try {
            host = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            log.fatal("Problem looking up ip address for host " + hostname + ": ", e);
        }
    }

    public void connect() throws IOException {
        Socket socket = new Socket(host, port);
    }

    public void read() {

    }

    public static void main(String[] args) {

        LoggingConsumer.installDefault();

        String hostname = DEFAULT_HOSTNAME;
        int port = DEFAULT_PORT;
        int nBytes = DEFAULT_NBYTES;
        if (args.length < 3) {
            usage();
        } else {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
            nBytes = Integer.parseInt(args[2]);
        }

        AmandaSocketReader reader = new AmandaSocketReader(hostname, port, nBytes);
        try {
            reader.connect();
            reader.read();
        } catch (IOException e) {
            log.fatal("Unable to connect: ", e);
        }

    }

    private static void usage() {
        log.info("Usage: AmandaSocketReader [hostname] [port] [nbytes]");
        System.exit(-1);
    }

}
