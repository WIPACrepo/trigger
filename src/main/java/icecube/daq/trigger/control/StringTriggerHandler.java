package icecube.daq.trigger.control;

import icecube.daq.payload.*;
import icecube.daq.trigger.impl.TriggerRequestPayloadFactory;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.trigger.algorithm.DefaultStringTrigger;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: toale
 * Date: Mar 30, 2007
 * Time: 1:36:18 PM
 */
public class StringTriggerHandler
        extends TriggerHandler
        implements IStringTriggerHandler {

    private static final Log log = LogFactory.getLog(StringTriggerHandler.class);

    private MasterPayloadFactory masterFactory;

    public StringTriggerHandler(ISourceID sourceId) {
        super(sourceId);
    }

    public StringTriggerHandler(ISourceID sourceId, TriggerRequestPayloadFactory outputFactory) {
        super(sourceId, outputFactory);
    }

    /**
     * Method to process payloads, assumes that they are time ordered.
     * @param payload payload to process
     */
    public void process(ILoadablePayload payload) {

        int interfaceType = payload.getPayloadInterfaceType();

        // make sure we have hit payloads (or hit data payloads)
        if ( (interfaceType == PayloadInterfaceRegistry.I_HIT_PAYLOAD) ||
             (interfaceType == PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD)) {

            // loop over triggers
            for (Object aTriggerList : triggerList) {
                ITriggerControl trigger = (ITriggerControl) aTriggerList;
                try {
                    trigger.runTrigger(payload);
                } catch (TriggerException e) {
                    log.error("Exception while running trigger: " + e);
                }
            }

        } else {
            log.warn("TriggerHandler only knows about either hitPayloads or TriggerRequestPayloads!");
        }

        // Check triggerBag and issue triggers
        issueTriggers();

    }


    protected void init() {
        super.init();
        addTrigger(createDefaultTrigger());
    }

    public void setMasterPayloadFactory(MasterPayloadFactory masterFactory) {
        this.masterFactory = masterFactory;
        outputFactory = (TriggerRequestPayloadFactory)
                masterFactory.getPayloadFactory(PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST);
    }

    /**
     * Receives a ByteBuffer from a source.
     *
     * @param tBuffer ByteBuffer the new buffer to be processed.
     */
    public void receiveByteBuffer(ByteBuffer tBuffer) {
        ILoadablePayload payload;
        try {
            payload = masterFactory.createPayload(0, tBuffer);
            process(payload);
        } catch (IOException ioe) {
            log.error("Error creating hit payload", ioe);
        } catch (DataFormatException dfe) {
            log.error("Error creating hit payload", dfe);
        }
    }

    /**
     * This method is called when the destination is closed.
     */
    public void destinationClosed() {
        flush();
    }

    private ITriggerControl createDefaultTrigger() {
        return new DefaultStringTrigger();
    }

}
