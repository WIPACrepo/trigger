package icecube.daq.trigger.monitor;

import icecube.daq.trigger.impl.TriggerRequestPayload;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.icebucket.logging.LoggingConsumer;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private static final int DEFAULT_NTRIG = 128;

    private InetAddress host = null;
    private final int port;

    private Socket socket;
    private DataInputStream input;
    private ByteBuffer buffer = ByteBuffer.allocate(72);
    private TriggerRequestPayloadFactory triggerFactory = new TriggerRequestPayloadFactory();

    private AmandaTriggerAnalyzer analyzer = new AmandaTriggerAnalyzer();

    public AmandaSocketReader() {
        this(DEFAULT_HOSTNAME, DEFAULT_PORT);
    }

    public AmandaSocketReader(String hostname, int port) {
        this.port = port;
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

    public void read(int nTrig) throws IOException {
        for (int i=0; i<nTrig; i++) {
            log.info("Reading trigger " + i);
            input.readFully(buffer.array());
            TriggerRequestPayload trigger;
            try {
                trigger = (TriggerRequestPayload) triggerFactory.createPayload(0, buffer);
                analyzer.analyze(trigger);
            } catch (DataFormatException e) {
                log.error("Error creating trigger payload: ", e);
            }
        }
        analyzer.dump();
    }

    public void close() throws IOException {
        socket.close();
    }

    public static void main(String[] args) {

        LoggingConsumer.installDefault();

        String hostname = DEFAULT_HOSTNAME;
        int port = DEFAULT_PORT;
        int nTrig = DEFAULT_NTRIG;
        if (args.length < 3) {
            usage();
        } else {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
            nTrig = Integer.parseInt(args[2]);
        }

        AmandaSocketReader reader = new AmandaSocketReader(hostname, port);
        try {
            reader.connect();
            reader.read(nTrig);
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
