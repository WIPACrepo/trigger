package icecube.daq.trigger.test;

import icecube.daq.payload.IWriteablePayload;

public interface PayloadValidator
{
    void validate(IWriteablePayload payload);
}
