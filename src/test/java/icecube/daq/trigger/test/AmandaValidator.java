package icecube.daq.trigger.test;

import icecube.daq.payload.IWriteablePayload;

import icecube.daq.trigger.ITriggerRequestPayload;

public class AmandaValidator
    extends BaseValidator
{
    private long timeInc;
    private long nextStart;

    /**
     * Validate Amanda triggers.
     *
     * @param timeInc amount by which trigger times are incremented
     */
    AmandaValidator(long timeInc)
    {
        this.timeInc = timeInc;
        nextStart = timeInc * 2L;
    }

    public void validate(IWriteablePayload payload)
    {
        if (!(payload instanceof ITriggerRequestPayload)) {
            throw new Error("Unexpected payload " +
                            payload.getClass().getName());
        }

        //dumpPayloadBytes(payload);

        ITriggerRequestPayload tr = (ITriggerRequestPayload) payload;

        long firstTime = getUTC(tr.getFirstTimeUTC());
        long lastTime = getUTC(tr.getLastTimeUTC());

        if (firstTime != nextStart) {
            throw new Error("Expected first trigger time " + nextStart +
                            ", not " + firstTime);
        } else if (lastTime != nextStart) {
            throw new Error("Expected last trigger time " + nextStart +
                            ", not " + lastTime);
        }

        nextStart = firstTime + timeInc;
    }
}
