package icecube.daq.trigger.test;

import icecube.daq.payload.IPayloadDestination;
import icecube.daq.payload.IPayloadDestinationCollection;
import icecube.daq.payload.IPayloadDestinationCollectionController;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IWriteablePayload;

import java.io.IOException;
import java.util.Collection;

public class MockPayloadDestination
    implements IPayloadDestinationCollection
{
    private boolean verbose;
    private int numWritten;
    private IOException writeException;
    private PayloadValidator validator;

    public MockPayloadDestination()
    {
    }

    public void addPayloadDestination(ISourceID srcId, IPayloadDestination dest)
    {
        throw new Error("Unimplemented");
    }

    public void close() throws IOException {
        closeAllPayloadDestinations();
    }

    public void closeAllPayloadDestinations()
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    public void closePayloadDestination(ISourceID srcId)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    public Collection getAllPayloadDestinations()
    {
        throw new Error("Unimplemented");
    }

    public Collection getAllSourceIDs()
    {
        throw new Error("Unimplemented");
    }

    public int getNumberWritten()
    {
        return numWritten;
    }

    public IPayloadDestination getPayloadDestination(ISourceID srcId)
    {
        throw new Error("Unimplemented");
    }

    public void registerController(IPayloadDestinationCollectionController x0)
    {
        throw new Error("Unimplemented");
    }

    public void setValidator(PayloadValidator validator)
    {
        this.validator = validator;
    }

    public void setVerbose(boolean val)
    {
        verbose = val;
    }

    public void setWritePayloadException(IOException ioe)
    {
        writeException = ioe;
    }

    public void stopAllPayloadDestinations()
        throws IOException
    {
        // do nothing
    }

    public void stop() throws IOException {
        stopAllPayloadDestinations();
    }

    public void stopPayloadDestination(ISourceID srcId)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    public int writePayload(ISourceID srcId, IWriteablePayload pay)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    public int writePayload(IWriteablePayload pay)
        throws IOException
    {
        if (writeException != null) {
            throw writeException;
        }

        numWritten++;

        if (validator != null) {
            try {
                validator.validate(pay);
            } catch (Error err) {
                throw new Error("Payload #" + numWritten + ": " +
                                err.getMessage() + "\n" + pay, err);
            }
        }

        if (verbose) {
            System.err.println("Wrote #" + numWritten + ": " + pay);
        }

        return -1;
    }
}
