package icecube.daq.trigger.control;

import icecube.daq.payload.IPayload;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A list which can feed its contents to multiple subscribers
 */
public class SubscribedList
{
    /**
     * List of subscribers
     */
    private ArrayList<ListSubscriber> subs = new ArrayList<ListSubscriber>();

    /**
     * Create subscribed list managment object.
     */
    public SubscribedList()
    {
    }

    /**
     * Get the lengths of all subscriber lists
     *
     * @return map of subscriber names and list lengths
     */
    public Map<String, Integer> getLengths()
    {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (ListSubscriber sub : subs) {
            synchronized (sub.list) {
                map.put(sub.name, sub.list.size());
            }
        }
        return map;
    }

    /**
     * Push a new payload onto the list
     *
     * @param pay new payload
     */
    public void push(IPayload pay)
    {
        for (ListSubscriber sub : subs) {
            synchronized (sub.list) {
                sub.list.addLast(pay);

                // let subscribers know that there's data available
                sub.list.notify();
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
        for (ListSubscriber sub : subs) {
            synchronized (sub.list) {
                if (sub.list.size() > longest) {
                    longest = sub.list.size();
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
        ListSubscriber newSub = new ListSubscriber(name);
        synchronized (subs) {
            subs.add(newSub);
        }
        return newSub;
    }

    /**
     * Internal subscriber class which has access to the list
     */
    class ListSubscriber
        implements PayloadSubscriber
    {
        /**
         * List of payloads
         */
        ArrayDeque<IPayload> list = new ArrayDeque<IPayload>();

        /** Subscriber name */
        private String name;
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
         * Is there data available?
         *
         * @return <tt>true</tt> if there are more payloads available
         */
        public boolean hasData()
        {
            return list.size() > 0;
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
                while (!stopped && list.size() == 0) {
                    try {
                        list.wait();
                    } catch (InterruptedException ie) {
                        return null;
                    }
                }

                if (stopped && list.size() == 0) {
                    return null;
                }

                return list.removeFirst();
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
                stopped = true;
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
                (stopped ? ":stopped" : "");
        }
    }
}
