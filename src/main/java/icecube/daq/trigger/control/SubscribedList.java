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
    private static final int CHUNK_SIZE = 1000;

    /**
     * List of subscribers
     */
    private List<PayloadSubscriber> subs = new ArrayList<PayloadSubscriber>();

    private ArrayList<IPayload> staged = new ArrayList<IPayload>();

    /**
     * Push any cached payloads out to the algorithm threads
     */
    public void flush()
    {
        synchronized (subs) {
            synchronized (staged) {
                for (PayloadSubscriber sub : subs) {
                    sub.pushAll(staged);
                }

                staged.clear();
            }
        }
    }

    /**
     * Get the lengths of all subscriber lists
     *
     * @return map of subscriber names and list lengths
     */
    public Map<String, Integer> getLengths()
    {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        synchronized (subs) {
            for (PayloadSubscriber sub : subs) {
                map.put(sub.getName(), sub.size());
            }
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
     * Are there any subscribers to this list?
     *
     * @return <tt>false</tt> if the list has one or more subscribers
     */
    public boolean isEmpty()
    {
        return subs.isEmpty();
    }

    /**
     * Push a new payload onto the list
     *
     * @param pay new payload
     */
    public void push(IPayload pay)
    {
        if (!subs.isEmpty()) {
            if (CHUNK_SIZE == 1) {
                synchronized (subs) {
                    for (PayloadSubscriber sub : subs) {
                        sub.push(pay);
                    }
                }
            } else {
                synchronized (staged) {
                    staged.add(pay);

                    if (staged.size() > CHUNK_SIZE) {
                        flush();
                    }
                }
            }
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
        synchronized (subs) {
            for (PayloadSubscriber sub : subs) {
                final int len = sub.size();
                if (len > longest) {
                    longest = len;
                }
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
        @Override
        public String getName()
        {
            return name;
        }

        /**
         * Is there data available?
         *
         * @return <tt>true</tt> if there are more payloads available
         */
        @Override
        public boolean hasData()
        {
            return !list.isEmpty();
        }

        /**
         * Has this list been stopped?
         *
         * @return <tt>true</tt> if the list has been stopped
         */
        @Override
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
        @Override
        public IPayload pop()
        {
            synchronized (list) {
                while (!stopping && list.isEmpty()) {
                    try {
                        list.wait();
                    } catch (InterruptedException ie) {
                        return null;
                    }
                }

                if (stopping && list.isEmpty()) {
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
        @Override
        public void push(IPayload pay)
        {
            synchronized (list) {
                list.addLast(pay);

                // let subscribers know that there's data available
                list.notify();
            }
        }

        /**
         * Add a list of payloads to the queue.
         *
         * @param payloads list of payload
         */
        @Override
        public void pushAll(List<IPayload> payloads)
        {
            synchronized (list) {
                list.addAll(payloads);

                // let subscribers know that there's data available
                list.notify();
            }
        }

        /**
         * Get the number of queued payloads
         *
         * @return size of internal queue
         */
        @Override
        public int size()
        {
            return list.size();
        }

        /**
         * No more payloads will be collected
         */
        @Override
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
        @Override
        public String toString()
        {
            return name + "@" + list.size() +
                (stopping ? ":stopping" : "") +
                (stopped ? ":stopped" : "");
        }
    }
}
