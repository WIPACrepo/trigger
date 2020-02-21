package icecube.daq.trigger.control;

import icecube.daq.payload.IPayload;
import icecube.daq.performance.queue.QueueStrategy;
import org.jctools.queues.SpscUnboundedArrayQueue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A list which can feed its contents to multiple subscribers
 */
public class SubscribedList
{

    /** Support for runtime selection of PayloadSubscriber impl */
    private static final String SubscriberImplCfg =
            System.getProperty("icecube.daq.trigger.control.subscriber-impl",
                    SubscriberImpl.LOCK_FREE_LIST_SUBSCRIBER_BEST_PERF.name());

    private static final SubscriberImpl SubscriberFactory =
            SubscriberImpl.valueOf(SubscriberImplCfg.toUpperCase());


    /**
     * Factory for selecting the PayloadSubscriber implementation.
     *
     * Queue selection is critical to overall trigger performance.  Changes should be validated
     * for performance under a representative production load.
     */
    enum SubscriberImpl
    {
        // used c 2013-2020
        // high performance cost
        LIST_SUBSCRIBER()
                {
                    @Override
                    public PayloadSubscriber createSubscriber(final String name)
                    {
                        return new ListSubscriber(name);
                    }
                },


        // introduced 2020 - great performance, standard JDK lock-free Queue<T> implementation
        LOCK_FREE_LIST_SUBSCRIBER()
                {
                    @Override
                    public PayloadSubscriber createSubscriber(final String name)
                    {
                        ConcurrentLinkedQueue<IPayload> base = new ConcurrentLinkedQueue<>();
                        QueueStrategy<IPayload> q =
                                new QueueStrategy.NonBlockingPollBackoff<>(base, 100, 1000);


                        return new LockFreeListSubscriber(q, name);
                    }
                },
        // introduced 2020 - great performance, 3rd party Queue<T> implementation
        // NOTE: Thread safe only with a single thread producer and a single thread consumer
        LOCK_FREE_LIST_SUBSCRIBER_BEST_PERF()
                {
                    @Override
                    public PayloadSubscriber createSubscriber(final String name)
                    {
                        //chunk size chosen to hold about 1 sec worth of hits
                        SpscUnboundedArrayQueue<IPayload> base = new SpscUnboundedArrayQueue<IPayload>(1024 * 128);
                        QueueStrategy<IPayload> q =
                                new QueueStrategy.NonBlockingPollBackoff<>(base, 100, 1000);


                        return new LockFreeListSubscriber(q, name);
                    }
                };


        public abstract PayloadSubscriber createSubscriber(String name);

    }


    /**
     * List of subscribers
     */
    private List<PayloadSubscriber> subs = new ArrayList<PayloadSubscriber>();

    private ArrayList<IPayload> staged = new ArrayList<IPayload>();


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
     * Stop all subscribers
     */
    public void stop()
    {
        synchronized (subs) {
            synchronized (staged) {
                for (PayloadSubscriber sub : subs) {
                    sub.stop();
                }

                staged.clear();
            }
        }
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
        PayloadSubscriber newSub = SubscriberFactory.createSubscriber(name);
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
            synchronized (list)
            {
                return !list.isEmpty();
            }
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

            //Note: Support TriggerThread looping post-stop
            if(stopped)
            {
                return PayloadSubscriber.STOPPED_PAYLOAD;
            }

            synchronized (list)
            {
                while(list.isEmpty())
                {
                    try {
                        list.wait();
                    } catch (InterruptedException ie) {
                        throw new Error("Unexpected interruption", ie);
                    }
                }

                IPayload payload = list.removeFirst();
                if(payload == PayloadSubscriber.STOPPED_PAYLOAD)
                {
                    stopped = true;
                }
                return payload;

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
         * Get the number of queued payloads
         *
         * @return size of internal queue
         */
        @Override
        public int size()
        {
            synchronized (list)
            {
                return list.size();
            }
        }

        /**
         * No more payloads will be collected
         */
        @Override
        public void stop()
        {
            stopping = true;
            push(PayloadSubscriber.STOPPED_PAYLOAD);
        }

        /**
         * Return a description of this subscriber
         *
         * @return string
         */
        @Override
        public String toString()
        {
            return name + "@" + size() +
                    (stopping ? ":stopping" : "") +
                    (stopped ? ":stopped" : "");
        }
    }


    /**
     * PayloadSubscribe implemented with a lock-free queue
     */
    public static class LockFreeListSubscriber implements PayloadSubscriber
    {
        /**
         * List of payloads
         */
       final  QueueStrategy<IPayload> q;

        /** Subscriber name */
        private String name;
        /** Is the list stopping? */
        private volatile boolean stopping;
        /** Has the list been stopped? */
        private volatile boolean stopped;

        /**
         * Create a list subscriber
         *
         * @param q
         * @param name subscriber name
         */
        LockFreeListSubscriber(QueueStrategy<IPayload> q, String name)
        {
            this.q = q;
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
            //return list.size() > 0;

            return q.size() > 0;
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

            //Note: Support TriggerThread looping post-stop
            if(stopped)
            {
                return PayloadSubscriber.STOPPED_PAYLOAD;
            }

            try
            {
                IPayload payload = q.dequeue();
                if(payload == PayloadSubscriber.STOPPED_PAYLOAD)
                {
                    stopped = true;
                }
                return payload;
            }
            catch (InterruptedException ie)
            {
                throw new Error("Unexpected interruption", ie);
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
            try
            {
                q.enqueue(pay);
            }
            catch (InterruptedException ie)
            {
                throw new Error("Unexpected interruption", ie);
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
            return q.size();
        }

        /**
         * No more payloads will be collected
         */
        @Override
        public void stop()
        {
            stopping = true;
            push(PayloadSubscriber.STOPPED_PAYLOAD);
        }

        /**
         * Return a description of this subscriber
         *
         * @return string
         */
        @Override
        public String toString()
        {
            return name + "@" + size() +
                    (stopping ? ":stopping" : "") +
                    (stopped ? ":stopped" : "");
        }
    }

}
