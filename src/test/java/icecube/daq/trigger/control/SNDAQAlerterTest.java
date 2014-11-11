package icecube.daq.trigger.control;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.trigger.algorithm.INewAlgorithm;
import icecube.daq.trigger.algorithm.SimpleMajorityTrigger;
import icecube.daq.trigger.test.MockAlerter;
import icecube.daq.trigger.test.MockAlgorithm;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockHit;
import icecube.daq.trigger.test.MockTriggerRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;

import org.junit.*;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

class MyMockAlerter
    extends MockAlerter
{
    private static final boolean DEBUG = false;

    private int msgNum;
    private boolean sawError;

    private ArrayDeque<Map<String, Object>> expected =
        new ArrayDeque<Map<String, Object>>();

    void addExpectedEntry(int numHits, long time)
    {
        if (DEBUG) {
            System.err.println("PUSH entry@" + time + "#" + numHits);
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("trigger", "SMT8");
        map.put("num", numHits);
        map.put("t", UTCTime.toDateString(time));
        synchronized (expected) {
            expected.addLast(map);
        }
    }

    void addExpectedStart(int runNum, long time)
    {
        if (DEBUG) {
            System.err.println("PUSH start@" + time + "#" + runNum);
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("start", runNum);
        map.put("t", UTCTime.toDateString(time));
        synchronized (expected) {
            expected.addLast(map);
        }
    }

    void addExpectedStop(int runNum, long time)
    {
        if (DEBUG) {
            System.err.println("PUSH stop@" + time + "#" + runNum);
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("stop", runNum);
        map.put("t", UTCTime.toDateString(time));
        synchronized (expected) {
            expected.addLast(map);
        }
    }

    boolean isEmpty()
    {
        return expected.size() == 0;
    }

    public void sendObject(Object obj)
        throws AlertException
    {
        msgNum++;

        if (isClosed()) {
            sawError = true;
            throw new Error("Alerter has been closed for message #" + msgNum);
        }

        if (!(obj instanceof Map)) {
            sawError = true;
            throw new AlertException("Message #" + msgNum + " contains " +
                                     obj.getClass().getName());
        }

        Map<String, Object> map = (Map<String, Object>) obj;
        if (DEBUG) {
            System.err.println("=== Msg #" + msgNum);
            for (String key : map.keySet()) {
                System.err.println(key + ": " + map.get(key));
            }
        }

        Map<String, Object> expMap;
        synchronized (expected) {
            if (expected.isEmpty()) {
                sawError = true;
                throw new Error("Got unexpected message #" + msgNum);
            }

            expMap = expected.pop();
        }
        if (DEBUG) {
            System.err.println("=== Exp #" + msgNum);
            for (String key : expMap.keySet()) {
                System.err.println(key + ": " + expMap.get(key));
            }
        }

        for (String key : expMap.keySet()) {
            if (!map.containsKey(key)) {
                sawError = true;
                throw new Error("Didn't expect message #" + msgNum +
                                " containing '" + key + "'");
            }

            Object expObj = expMap.get(key);
            if (!expObj.equals(map.get(key))) {
                sawError = true;
                throw new Error("Message #:" + msgNum + " field '" + key +
                                "' should be " + expObj + ", not " +
                                map.get(key));
            }
        }
    }

    boolean sawError()
    {
        return sawError;
    }
}

class MySNDAQAlerter
    extends SNDAQAlerter
{
    private MyMockAlerter mockAlerter;

    public MySNDAQAlerter(List<INewAlgorithm> algorithms)
        throws AlertException
    {
        super(algorithms);
    }

    public Alerter createZMQAlerter(String host, int port)
        throws AlertException
    {
        mockAlerter = new MyMockAlerter();
        return mockAlerter;
    }

    MyMockAlerter getMockAlerter()
    {
        return mockAlerter;
    }
}

public class SNDAQAlerterTest
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    @Before
    public void setUp()
        throws Exception
    {
        appender.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);

        // initialize SNDAQ ZMQ address to nonsense
        System.getProperties().setProperty(SNDAQAlerter.PROPERTY, ":12345");
    }

    @After
    public void tearDown()
        throws Exception
    {
        // remove SNDAQ ZMQ address
        System.clearProperty(SNDAQAlerter.PROPERTY);

        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());
    }

    @Test
    public void testOpenClose()
        throws AlertException
    {
        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();

        //algorithms.add(new MockAlgorithm("foo"));

        SNDAQAlerter alerter = new MySNDAQAlerter(algorithms);
        alerter.close();
    }

    @Test
    public void testSMT8()
        throws AlertException
    {
        ArrayList<INewAlgorithm> algorithms =
            new ArrayList<INewAlgorithm>();

        final int cfgId = 123;
        final int trigType = 456;

        SimpleMajorityTrigger smt8 = new SimpleMajorityTrigger();
        smt8.setTriggerName("SimpleMajorityTrigger-Test8");
        smt8.setTriggerConfigId(cfgId);
        smt8.setTriggerType(trigType);
        smt8.setThreshold(8);

        algorithms.add(smt8);

        final int runNum = 1234;

        MySNDAQAlerter alerter = new MySNDAQAlerter(algorithms);
        alerter.setRunNumber(runNum);

        MyMockAlerter mockZMQ = alerter.getMockAlerter();

        final long oneSecond = 10000000000L;

        long startTime = 1234567890L;

        long endTime = startTime;
        for (int i = 0; i < 10; i++) {
            endTime = startTime + oneSecond;

            MockTriggerRequest req =
                new MockTriggerRequest(i + 10, trigType, cfgId,
                                       startTime, endTime);
            for (int j = 0; j <= i; j++) {
                req.addPayload(new MockHit(startTime + ((long) j) * 10000L));
            }

            if (i == 0) {
                mockZMQ.addExpectedStart(runNum, endTime);
            }

            mockZMQ.addExpectedEntry(i + 1, endTime);

            alerter.process(req);

            startTime += oneSecond * 5;
        }

        mockZMQ.addExpectedStop(runNum, endTime);

        alerter.close();

        assertFalse("ZMQ alerter expects more messages", mockZMQ.isEmpty());
        assertFalse("ZMQ alerter saw one or more errors", mockZMQ.sawError());
    }
}
