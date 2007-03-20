package icecube.daq.trigger.monitor;

import icecube.icebucket.logging.LoggingConsumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.Socket;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.io.IOException;
import java.io.DataInputStream;
import java.nio.ByteBuffer;

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
    private static final int DEFAULT_NBUFF = 100;

    private InetAddress host = null;
    private final int port;
    private final int nBuff;

    private Socket socket;
    private DataInputStream input;
    private ByteBuffer buffer = ByteBuffer.allocate(72*128);

    public AmandaSocketReader() {
        this(DEFAULT_HOSTNAME, DEFAULT_PORT, DEFAULT_NBUFF);
    }

    public AmandaSocketReader(String hostname, int port, int nBuff) {
        this.port = port;
        this.nBuff = nBuff;
        try {
            host = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            log.fatal("Problem looking up ip address for host " + hostname + ": ", e);
        }
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        input = new DataInputStream(socket.getInputStream());
    }

    public void read() throws IOException {
        for (int i=0; i<nBuff; i++) {
            log.info("Reading buffer " + i);
            input.readFully(buffer.array());
        }
    }

    public void close() throws IOException {
        socket.close();
    }

    public static void main(String[] args) {

        LoggingConsumer.installDefault();

        String hostname = DEFAULT_HOSTNAME;
        int port = DEFAULT_PORT;
        int nBuff = DEFAULT_NBUFF;
        if (args.length < 3) {
            usage();
        } else {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
            nBuff = Integer.parseInt(args[2]);
        }

        AmandaSocketReader reader = new AmandaSocketReader(hostname, port, nBuff);
        try {
            reader.connect();
            reader.read();
            reader.close();
        } catch (IOException e) {
            log.fatal("Unable to connect: ", e);
        }

    }

    private static void usage() {
        log.info("Usage: AmandaSocketReader [hostname] [port] [nbytes]");
        System.exit(-1);
    }

}
