package icecube.daq.trigger.algorithm;

public class AlgorithmStatistics
{
    private ITriggerAlgorithm algorithm;

    public AlgorithmStatistics(ITriggerAlgorithm algorithm)
    {
        this.algorithm = algorithm;
    }

    public String getExtra()
    {
        /*
        if (algorithm instanceof SimpleMajorityTrigger) {
            return ((SimpleMajorityTrigger) algorithm).getTimeStats();
        }
        */

        return "";
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
        StringBuilder buf = new StringBuilder(getName());
        buf.append(':').append(getNumberOfQueuedInputs());
        buf.append("->").append(getNumberOfCreatedRequests());
        buf.append(" (").append(getNumberOfCachedRequests());
        buf.append(" cached)");

        String extra = getExtra();
        if (extra.length() > 0) {
            buf.append(" :: ").append(extra);
        }

        return buf.toString();
    }
}
