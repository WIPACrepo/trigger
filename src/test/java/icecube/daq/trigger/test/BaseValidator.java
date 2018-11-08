package icecube.daq.trigger.test;

import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.impl.TriggerRequestFactory;
import icecube.daq.trigger.exceptions.UnimplementedError;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

public abstract class BaseValidator
    implements PayloadValidator
{
    private static final Logger LOG = Logger.getLogger(BaseValidator.class);

    private TriggerRequestFactory factory;

    private int invalidCount;

    @Override
    public boolean foundInvalid()
    {
        return invalidCount > 0;
    }

    public long getUTC(IUTCTime utc)
    {
        if (utc == null) {
            return -1L;
        }

        return utc.longValue();
    }

    @Override
    public boolean validate(ByteBuffer payBuf)
    {
        if (factory == null) {
            factory = new TriggerRequestFactory(null);
        }

        if (payBuf.limit() == 4 && payBuf.getInt(0) == 4) {
            // ignore stop message
            return true;
        }

        IWriteablePayload payload;
        try {
            payload = factory.createPayload(payBuf, 0);
        } catch (Exception ex) {
            LOG.error("Couldn't validate byte buffer", ex);
            invalidCount++;
            return false;
        }

        if (!validate(payload)) {
            invalidCount++;
            return false;
        }

        return true;
    }

    @Override
    public boolean validate(IWriteablePayload payload)
    {
        throw new UnimplementedError();
    }

}
