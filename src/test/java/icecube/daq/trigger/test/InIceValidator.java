package icecube.daq.trigger.test;

import icecube.daq.payload.IWriteablePayload;

import icecube.daq.trigger.ITriggerRequestPayload;

public class InIceValidator
    extends BaseValidator
{
    private long timeStep;
    private long timeSpan;

    private long nextStart;
    private long nextEnd;

    /**
     * Validate in-ice triggers.
     *
     * @param timeStep amount by which trigger times are incremented
     */
    InIceValidator(long timeStep, int reps)
    {
        this.timeStep = timeStep;
        this.timeSpan = timeStep * (long) (reps - 1);

        nextStart = timeStep;
        nextEnd = nextStart + timeSpan;
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
        } else if (lastTime != nextEnd) {
            throw new Error("Expected last trigger time " + nextEnd +
                            ", not " + lastTime);
        }

        nextStart = lastTime + timeStep;
        nextEnd = nextStart + timeSpan;
    }
}
