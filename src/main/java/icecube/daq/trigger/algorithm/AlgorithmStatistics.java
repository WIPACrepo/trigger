package icecube.daq.trigger.algorithm;

import icecube.daq.payload.IPayload;

/**
 * Produce a one-line summary of algorithm statistics
 */
public class AlgorithmStatistics
{
    private static final boolean ADD_EARLIEST = true;
    private static final boolean ADD_LATENCY = true;

    private ITriggerAlgorithm algorithm;

    public AlgorithmStatistics(ITriggerAlgorithm algorithm)
    {
        this.algorithm = algorithm;
    }

    /**
     * Add special debugging stuff here
     *
     * @return extra debugging string
     */
    public String getExtra()
    {
        /*
        if (algorithm instanceof SimpleMajorityTrigger) {
            return ((SimpleMajorityTrigger) algorithm).getTimeStats();
        }
        */

        return "";
    }

    public String getLatency()
    {
        final long latency = algorithm.getLatency();

        if (latency < 10000L) {
            return String.format("%.2fns", (float) latency / 10.0);
        } else if (latency < 10000000L) {
            return String.format("%.2fus", (float) latency / 10000.0);
        } else if (latency < 10000000000L) {
            return String.format("%.2fms", (float) latency / 10000000.0);
        }

        return String.format("%.2fs", (float) latency / 10000000000.0);
    }

    /**
     * Return trigger name (without excess verbiage)
     *
     * @return trigger name
     */
    public String getName()
    {
        if (algorithm instanceof SimpleMajorityTrigger) {
            SimpleMajorityTrigger smt = (SimpleMajorityTrigger) algorithm;
            return "SMT" + smt.getThreshold();
        }

        String fullname = algorithm.getTriggerName();
        int idx = fullname.lastIndexOf("Trigger");

        String name;
        if (idx < 0) {
            name = fullname;
        } else if (idx + 7 == fullname.length()) {
            name = fullname.substring(0, idx);
        } else {
            name = fullname.substring(0, idx) + fullname.substring(idx + 8);
        }

        if (name.endsWith("1")) {
            name = name.substring(0, name.length() - 1);
        }

        return name;
    }

    /**
     * Get number of requests waiting to be merged or sent.
     *
     * @return number of cached requests
     */
    public int getNumberOfCachedRequests()
    {
        return algorithm.getNumberOfCachedRequests();
    }

    /**
     * Return number of requests created by this algorithm.
     *
     * @return number of requests
     */
    public int getNumberOfCreatedRequests()
    {
        return algorithm.getTriggerCounter();
    }

    /**
     * Return number of hits/requests waiting to be processed.
     *
     * @return number of queued hits/requests
     */
    public int getNumberOfQueuedInputs()
    {
        return algorithm.getInputQueueSize();
    }

    @Override
    public String toString()
    {
        final int cached = getNumberOfCachedRequests();

        StringBuilder buf = new StringBuilder(getName());
        buf.append(':').append(getNumberOfQueuedInputs());
        buf.append("->").append(getNumberOfCreatedRequests());

        boolean addParen = false;
        if (cached > 0) {
            buf.append(" (").append(cached).append(" cached");
            if (ADD_LATENCY) {
                buf.append(", latency ").append(getLatency());
            }
            addParen = true;
        }

        if (ADD_EARLIEST) {
            if (cached > 0) {
                buf.append(", ");
            } else {
                buf.append(" (");
                addParen = true;
            }

            IPayload earliest = algorithm.getEarliestPayloadOfInterest();
            buf.append("earliest ");
            if (earliest == null) {
                buf.append("NULL");
            } else {
                buf.append(earliest.getUTCTime());
            }
        }

        if (addParen) {
            buf.append(")");
        }

        String extra = getExtra();
        if (extra != null && extra.length() > 0) {
            buf.append(" :: ").append(extra);
        }

        return buf.toString();
    }
}
