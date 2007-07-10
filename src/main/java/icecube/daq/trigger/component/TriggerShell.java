package icecube.daq.trigger.component;

import icecube.daq.io.SpliceablePayloadReader;

import icecube.daq.juggler.component.DAQCompServer;
import icecube.daq.juggler.component.DAQComponent;
import icecube.daq.juggler.component.DAQConnector;
import icecube.daq.juggler.component.DAQCompException;

import icecube.daq.juggler.mbean.MemoryStatistics;
import icecube.daq.juggler.mbean.SystemStatistics;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.MasterPayloadFactory;
import icecube.daq.payload.VitreousBufferCache;

import icecube.daq.payload.splicer.Payload;

import icecube.daq.splicer.Spliceable;
import icecube.daq.splicer.SpliceableFactory;
import icecube.daq.splicer.SplicedAnalysis;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerChangedEvent;
import icecube.daq.splicer.SplicerImpl;
import icecube.daq.splicer.SplicerListener;

import java.io.IOException;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * SplicedAnalysis which immediately recycled all payloads.
 */
class DevNullAnalysis
    implements SplicedAnalysis, SplicerListener
{
    private static final Log LOG = LogFactory.getLog(DevNullAnalysis.class);

    private Splicer splicer;

    /**
     * Called when the {@link Splicer Splicer} enters the disposed state.
     *
     * @param event the event encapsulating this state change.
     */
    public void disposed(SplicerChangedEvent event)
    {
        // ignored
    }

    /**
     * Called when the {@link Splicer Splicer} enters the failed state.
     *
     * @param event the event encapsulating this state change.
     */
    public void failed(SplicerChangedEvent event)
    {
        // ignored
    }

    /**
     * Called by the {@link Splicer Splicer} to analyze the
     * List of Spliceable objects provided.
     *
     * @param splicedObjects a List of Spliceable objects.
     * @param decrement the decrement of the indices in the List since the last
     * invocation.
     */
    public void execute(List splicedObjects, int decrement)
    {
        synchronized (splicedObjects) {
            if (splicedObjects.size() > 0) {
                Object lastObj =
                    splicedObjects.get(splicedObjects.size() - 1);

                splicer.truncate((Spliceable) lastObj);
            }
        }
    }

    /**
     * Returns the {@link SpliceableFactory} that should be used to create the
     * {@link Spliceable Spliceable} objects used by this
     * object.
     *
     * @return the SpliceableFactory that creates Spliceable objects.
     */
    public SpliceableFactory getFactory()
    {
        try {
            throw new Error("Unimplemented");
        } catch (Error err) {
            LOG.error("Unimplemented", err);
            throw err;
        }
    }

    /**
     * Set Splicer associated with this analysis.
     * @param splicer parent Splicer
     */
    public void setSplicer(Splicer splicer)
    {
        this.splicer = splicer;

        splicer.addSplicerListener(this);
    }

    /**
     * Called when the {@link Splicer Splicer} enters the started state.
     *
     * @param event the event encapsulating this state change.
     */
    public void started(SplicerChangedEvent event)
    {
        // ignored
    }

    /**
     * Called when the {@link Splicer Splicer} enters the starting state.
     *
     * @param event the event encapsulating this state change.
     */
    public void starting(SplicerChangedEvent event)
    {
        // ignored
    }

    /**
     * Called when the {@link Splicer Splicer} enters the stopped state.
     *
     * @param event the event encapsulating this state change.
     */
    public void stopped(SplicerChangedEvent event)
    {
        // ignored
    }

    /**
     * Called when the {@link Splicer Splicer} enters the stopping state.
     *
     * @param event the event encapsulating this state change.
     */
    public void stopping(SplicerChangedEvent event)
    {
        // ignored
    }

    /**
     * Called when the {@link Splicer Splicer} has truncated its "rope". This
     * method is called whenever the "rope" is cut, for example to make a clean
     * start from the frayed beginning of a "rope" or cutting the rope when
     * reaching the Stopped state. This is not only invoked as the result of
     * the {@link Splicer#truncate(Spliceable)} method being invoked.
     * <p/>
     * This enables the client to be notified as to which Spliceable are never
     * going to be accessed again by the Splicer.
     * <p/>
     * When entering the Stopped state the {@link SplicerChangedEvent#getSpliceable()}
     * method will return the {@link Splicer#LAST_POSSIBLE_SPLICEABLE} object.
     *
     * @param event the event encapsulating this truncation.
     */
    public void truncated(SplicerChangedEvent event)
    {
        Iterator iter = event.getAllSpliceables().iterator();
        while (iter.hasNext()) {
            Payload pay = (Payload) iter.next();
            try {
                pay.recycle();
            } catch (Exception ex) {
                LOG.error("Couldn't recycle payload " +
                          icecube.daq.payload.DebugDumper.toString(pay),
                          ex);
            }
        }
    }
}

/**
 * Debugging shell which reads from stringHubs and does nothing else.
 */
public class TriggerShell
    extends DAQComponent
{
    private static final Log log = LogFactory.getLog(TriggerShell.class);

    private static final String COMPONENT_NAME = "triggerShell";
    private static final int COMPONENT_ID = 0;

    public TriggerShell(String name, int id)
    {
        super(name, id);

        // Create the buffer cache and the payload factory
        IByteBufferCache bufferCache = new VitreousBufferCache();
        addCache(bufferCache);

        MasterPayloadFactory masterFactory =
            new MasterPayloadFactory(bufferCache);

        addMBean("jvm", new MemoryStatistics());
        addMBean("system", new SystemStatistics());

        // Now differentiate
        String inputType, outputType;

        DevNullAnalysis analysis = new DevNullAnalysis();

        inputType = DAQConnector.TYPE_STRING_HIT;

        // Create splicer and introduce it to the trigger manager
        Splicer splicer = new SplicerImpl(analysis);
        analysis.setSplicer(splicer);

        // Create and register io engines
        SpliceablePayloadReader inputEngine;
        try {
            inputEngine =
                new SpliceablePayloadReader(name, splicer, masterFactory);
        } catch (IOException ioe) {
            log.error("Couldn't create input reader");
            System.exit(1);
            inputEngine = null;
        }
        addMonitoredEngine(inputType, inputEngine);
    }

    public static void main(String[] args)
        throws DAQCompException
    {
        new DAQCompServer(new TriggerShell(COMPONENT_NAME, COMPONENT_ID), args);
    }
}
