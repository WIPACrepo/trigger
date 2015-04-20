package icecube.daq.trigger.control;

import icecube.daq.payload.IPayload;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A list which can feed its contents to multiple subscribers
 */
public class SubscribedList
{
    /**
     * List of subscribers
     */
    private List<PayloadSubscriber> subs = new ArrayList<PayloadSubscriber>();

    /**
     * Get the lengths of all subscriber lists
     *
     * @return map of subscriber names and list lengths
     */
    public Map<String, Integer> getLengths()
    {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (PayloadSubscriber sub : subs) {
            map.put(sub.getName(), sub.size());
        }
        return map;
    }

    /**
     * Get the number of subscribers to this list
     *
     * @return number of subscribers
     */
    public int getNumSubscribers()
    {
        return subs.size();
    }

    /**
     * Push a new payload onto the list
     *
     * @param pay new payload
     */
    public void push(IPayload pay)
    {
        if (subs.size() == 0) {
            throw new Error("No subscribers have been added");
        }

        for (PayloadSubscriber sub : subs) {
            sub.push(pay);
        }
    }

    /**
     * Get the size of the largest subscriber list.
     *
     * @return list size
     */
    public int size()
    {
        int longest = 0;
        for (PayloadSubscriber sub : subs) {
            final int len = sub.size();
            if (len > longest) {
                longest = len;
            }
        }
        return longest;
    }

    /**
     * Add a subscriber.
     *
     * @param name subscriber name
     *
     * @return subscriber object
     */
    public PayloadSubscriber subscribe(String name)
    {
        PayloadSubscriber newSub = new ListSubscriber(name);
        synchronized (subs) {
            subs.add(newSub);
        }
        return newSub;
    }

    /**
     * Remove a subscriber.
     *
     * @param sub subscriber object
     *
     * @return <tt>true</tt> if the subscriber was removed
     */
    public boolean unsubscribe(PayloadSubscriber sub)
    {
        synchronized (subs) {
            return subs.remove(sub);
        }
    }

    /**
     * Internal subscriber class which has access to the list
     */
    private static class ListSubscriber
        implements PayloadSubscriber
    {
        /**
         * List of payloads
         */
        ArrayDeque<IPayload> list = new ArrayDeque<IPayload>();

        /** Subscriber name */
        private String name;
        /** Is the list stopping? */
        private boolean stopping;
        /** Has the list been stopped? */
        private boolean stopped;

        /**
         * Create a list subscriber
         *
         * @param name subscriber name
         */
        ListSubscriber(String name)
        {
            this.name = name;
        }

        /**
         * Get subscriber name
         *
         * @return name
         */
        public String getName()
        {
            return name;
        }

        /**
         * Is there data available?
         *
         * @return <tt>true</tt> if there are more payloads available
         */
        public boolean hasData()
        {
            return list.size() > 0;
        }

        /**
         * Has this list been stopped?
         *
         * @return <tt>true</tt> if the list has been stopped
         */
        public boolean isStopped()
        {
            return stopped;
        }

        /**
         * Return the next available payload.  Note that this may block if
         * there are no payloads queued.
         *
         * @return next available payload.
         */
        public IPayload pop()
        {
            synchronized (list) {
                while (!stopping && list.size() == 0) {
                    try {
                        list.wait();
                    } catch (InterruptedException ie) {
                        return null;
                    }
                }

                if (stopping && list.size() == 0) {
                    stopped = true;
                    return null;
                }

                return list.removeFirst();
            }
        }

        /**
         * Add a payload to the queue.
         *
         * @param pay payload
         */
        public void push(IPayload pay)
        {
            synchronized (list) {
                list.addLast(pay);

                // let subscribers know that there's data available
                list.notify();
            }
        }

        /**
         * Get the number of queued payloads
         *
         * @return size of internal queue
         */
        public int size()
        {
            return list.size();
        }

        /**
         * No more payloads will be collected
         */
        public void stop()
        {
            synchronized (list) {
                stopping = true;
                list.notify();
            }
        }

        /**
         * Return a description of this subscriber
         *
         * @return string
         */
        public String toString()
        {
            return name + "@" + list.size() +
                (stopping ? ":stopping" : "") +
                (stopped ? ":stopped" : "");
        }
    }
}
