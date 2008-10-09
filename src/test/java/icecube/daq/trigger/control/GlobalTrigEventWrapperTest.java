package icecube.daq.trigger.control;

import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.test.MockAppender;
import icecube.daq.trigger.test.MockReadoutRequest;
import icecube.daq.trigger.test.MockReadoutRequestElement;
import icecube.daq.trigger.test.MockTriggerRequest;

import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class GlobalTrigEventWrapperTest
    extends TestCase
{
    private static final MockAppender appender =
        new MockAppender(/*org.apache.log4j.Level.ALL*/)/*.setVerbose(true)*/;

    public GlobalTrigEventWrapperTest(String name)
    {
        super(name);
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        appender.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);
    }

    public static Test suite()
    {
        return new TestSuite(GlobalTrigEventWrapperTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testCreate()
    {
        GlobalTrigEventWrapper evtWrap = new GlobalTrigEventWrapper();
        assertNull("Unexpected single event",
                   evtWrap.getGlobalTrigEventPayload_single());
        assertNull("Unexpected merged event",
                   evtWrap.getGlobalTrigEventPayload_merged());
        assertNull("Unexpected final event",
                   evtWrap.getGlobalTrigEventPayload_final());
    }

    public void testWrapSingle()
    {
        GlobalTrigEventWrapper evtWrap = new GlobalTrigEventWrapper();

        MockTriggerRequest tr = new MockTriggerRequest(10000L, 20000L);
        tr.setSourceID(123);
        tr.setReadoutRequest(new MockReadoutRequest());

        evtWrap.wrap(tr, 456, 789);
        assertNotNull("Expected single event",
                   evtWrap.getGlobalTrigEventPayload_single());
        assertNull("Unexpected merged event",
                   evtWrap.getGlobalTrigEventPayload_merged());
        assertNull("Unexpected final event",
                   evtWrap.getGlobalTrigEventPayload_final());
    }

    public void testWrapSingleNullReadout()
    {
        GlobalTrigEventWrapper evtWrap = new GlobalTrigEventWrapper();

        MockTriggerRequest tr = new MockTriggerRequest(10000L, 20000L);
        tr.setSourceID(123);
        //tr.setReadoutRequest(new MockReadoutRequest());

        evtWrap.wrap(tr, 456, 789);
        assertNotNull("Expected single event",
                      evtWrap.getGlobalTrigEventPayload_single());
        assertNull("Unexpected merged event",
                   evtWrap.getGlobalTrigEventPayload_merged());
        assertNull("Unexpected final event",
                   evtWrap.getGlobalTrigEventPayload_final());
    }

    public void testWrapMerged()
    {
        GlobalTrigEventWrapper evtWrap = new GlobalTrigEventWrapper();

        MockReadoutRequest rr = new MockReadoutRequest();
        rr.addElement(new MockReadoutRequestElement(1, 11000L, 12000L, 123L,
                                                    123));

        MockTriggerRequest tr = new MockTriggerRequest(10000L, 20000L);
        tr.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        tr.setReadoutRequest(rr);

        MockTriggerRequest subTR = new MockTriggerRequest(13000L, 14000L);
        subTR.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        tr.addPayload(subTR);

        ArrayList list = new ArrayList();
        list.add(tr);

        evtWrap.wrapMergingEvent(list);
        assertNull("Unexpected single event",
                   evtWrap.getGlobalTrigEventPayload_single());
        assertNotNull("Expected merged event",
                      evtWrap.getGlobalTrigEventPayload_merged());
        assertNull("Unexpected final event",
                   evtWrap.getGlobalTrigEventPayload_final());
    }

    public void testWrapMergedMulti()
    {
        GlobalTrigEventWrapper evtWrap = new GlobalTrigEventWrapper();

        ArrayList list = new ArrayList();

        MockReadoutRequest rr;
        MockTriggerRequest tr;

        rr = new MockReadoutRequest();
        rr.addElement(new MockReadoutRequestElement(1, 10000L, 20000L, 123L,
                                                    123));

        tr = new MockTriggerRequest(10000L, 20000L, 3, 4);
        tr.setSourceID(123);
        tr.setReadoutRequest(rr);
        list.add(tr);

        rr = new MockReadoutRequest();
        rr.addElement(new MockReadoutRequestElement(2, 30000L, 40000L, 456L,
                                                    456));

        tr = new MockTriggerRequest(30000L, 40000L, 5, 6);
        tr.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        tr.setReadoutRequest(rr);
        list.add(tr);

        evtWrap.wrapMergingEvent(list);
        assertNull("Unexpected single event",
                   evtWrap.getGlobalTrigEventPayload_single());
        assertNotNull("Expected merged event",
                      evtWrap.getGlobalTrigEventPayload_merged());
        assertNull("Unexpected final event",
                   evtWrap.getGlobalTrigEventPayload_final());
    }

    public void testWrapMergedAfterSingle()
    {
        GlobalTrigEventWrapper evtWrap = new GlobalTrigEventWrapper();

        MockTriggerRequest tr = new MockTriggerRequest(11111L, 22222L);
        tr.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        //tr.setReadoutRequest(new MockReadoutRequest());

        evtWrap.wrap(tr, 456, 789);
        assertNotNull("Expected single event",
                      evtWrap.getGlobalTrigEventPayload_single());
        assertNull("Unexpected merged event",
                   evtWrap.getGlobalTrigEventPayload_merged());
        assertNull("Unexpected final event",
                   evtWrap.getGlobalTrigEventPayload_final());

        MockTriggerRequest tr2 = new MockTriggerRequest(30000L, 40000L);
        tr2.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        tr2.setReadoutRequest(new MockReadoutRequest());

        ArrayList list = new ArrayList();
        list.add(tr2);

        evtWrap.wrapMergingEvent(list);
        assertNotNull("Expected single event",
                      evtWrap.getGlobalTrigEventPayload_single());
        assertNotNull("Expected merged event",
                      evtWrap.getGlobalTrigEventPayload_merged());
        assertNull("Unexpected final event",
                   evtWrap.getGlobalTrigEventPayload_final());
    }

    public void testWrapMergedWithParams()
    {
        GlobalTrigEventWrapper evtWrap = new GlobalTrigEventWrapper();

        MockReadoutRequest rr = new MockReadoutRequest();
        rr.addElement(new MockReadoutRequestElement(1, 10000L, 20000L, 123L, 123));

        MockTriggerRequest tr = new MockTriggerRequest(11000L, 12000L);
        tr.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        tr.setReadoutRequest(rr);

        MockTriggerRequest subTR = new MockTriggerRequest(13000L, 14000L);
        subTR.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        tr.addPayload(subTR);

        ArrayList list = new ArrayList();
        list.add(tr);

        evtWrap.wrapMergingEvent(list, 456, 789);
        assertNull("Unexpected single event",
                   evtWrap.getGlobalTrigEventPayload_single());
        assertNotNull("Expected merged event",
                      evtWrap.getGlobalTrigEventPayload_merged());
        assertNull("Unexpected final event",
                   evtWrap.getGlobalTrigEventPayload_final());
    }

    public void testWrapMergedWithParamsAfterSingle()
    {
        GlobalTrigEventWrapper evtWrap = new GlobalTrigEventWrapper();

        MockTriggerRequest tr = new MockTriggerRequest(11111L, 22222L);
        tr.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        //tr.setReadoutRequest(new MockReadoutRequest());

        evtWrap.wrap(tr, 456, 789);
        assertNotNull("Expected single event",
                      evtWrap.getGlobalTrigEventPayload_single());
        assertNull("Unexpected merged event",
                   evtWrap.getGlobalTrigEventPayload_merged());
        assertNull("Unexpected final event",
                   evtWrap.getGlobalTrigEventPayload_final());

        MockTriggerRequest tr2 = new MockTriggerRequest(30000L, 40000L);
        tr2.setSourceID(SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID);
        tr2.setReadoutRequest(new MockReadoutRequest());

        ArrayList list = new ArrayList();
        list.add(tr2);

        evtWrap.wrapMergingEvent(list, 456, 789);
        assertNotNull("Expected single event",
                      evtWrap.getGlobalTrigEventPayload_single());
        assertNotNull("Expected merged event",
                      evtWrap.getGlobalTrigEventPayload_merged());
        assertNull("Unexpected final event",
                   evtWrap.getGlobalTrigEventPayload_final());
    }

    public void testWrapFinal()
    {
        GlobalTrigEventWrapper evtWrap = new GlobalTrigEventWrapper();

        MockTriggerRequest tr = new MockTriggerRequest(10000L, 20000L);
        tr.setSourceID(123);
        tr.setReadoutRequest(new MockReadoutRequest());

        evtWrap.wrapFinalEvent(tr, 456);
        assertNull("Unexpected single event",
                   evtWrap.getGlobalTrigEventPayload_single());
        assertNull("Unexpected merged event",
                   evtWrap.getGlobalTrigEventPayload_merged());
        assertNotNull("Expected final event",
                      evtWrap.getGlobalTrigEventPayload_final());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
