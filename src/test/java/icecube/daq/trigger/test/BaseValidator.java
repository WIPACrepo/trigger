package icecube.daq.trigger.test;

import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class BaseValidator
    implements PayloadValidator
{
    static long getUTC(IUTCTime time)
    {
        if (time == null) {
            return -1L;
        }

        return time.getUTCTimeAsLong();
    }

    static void dumpPayloadBytes(IWriteablePayload payload)
    {
        ByteBuffer buf = ByteBuffer.allocate(payload.getPayloadLength());

        int len;
        try {
            len = payload.writePayload(false, 0, buf);
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
            len = -1;
        }

        StringBuffer strbuf = new StringBuffer();
        for (int i = 0; i < buf.limit(); i++) {
            String str = Integer.toHexString(buf.get(i));
            if (str.length() < 2) {
                strbuf.append('0').append(str);
            } else if (str.length() > 2) {
                strbuf.append(str.substring(str.length() - 2));
            } else {
                strbuf.append(str);
            }
            strbuf.append(' ');
        }

        System.err.println("LEN "+len+" HEX "+strbuf.toString());
    }
}
