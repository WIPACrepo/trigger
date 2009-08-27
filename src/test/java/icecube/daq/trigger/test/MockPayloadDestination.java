package icecube.daq.trigger.test;

import icecube.daq.payload.IPayloadDestination;
import icecube.daq.payload.IPayloadDestinationCollection;
import icecube.daq.payload.IPayloadDestinationCollectionController;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.IWriteablePayload;

import java.io.IOException;
import java.nio.ByteBuffer;
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

    /**
     * Returns boolean to tell if label operation should be performed.
     * this saves objects work if they are using the label feature for
     * PayloadDestinations which do not do labeling.
     * @return boolean true if labeling is on, false if off.
     */
    public boolean doLabel()
    {
        throw new Error("Unimplemented");
    }

    /**
     * simple labeling routine which is a stub but is useful for debugging.
     * this is NOT INTENDED to contribute to the output stream.
     * @param sLabel String which indicates some aspect of how the destination is being used
     *                   at a point in the writing of payloads.
     */
    public IPayloadDestination label(String sLabel)
    {
        throw new Error("Unimplemented");
    }

    public IPayloadDestination indent()
    {
        throw new Error("Unimplemented");
    }

    /**
     * removes indentation level for labeling
     */
    public IPayloadDestination undent()
    {
        throw new Error("Unimplemented");
    }

    /**
     * This method writes bytes from the given offset in the ByteBuffer for a length of iBytes
     * to the destination.
     * @param iOffset the offset in the ByteBuffer to start
     * @param tBuffer ByteBuffer from which to write to destination.
     * @param iBytes  the number of bytes to write to the destination.
     *
     * @throws IOException....if an error occurs either reading the ByteBuffer or writing
     *                        to the destination.
     */
    public void write(int iOffset, ByteBuffer tBuffer, int iBytes)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Writes to the output stream the eight
     * low-order bits of the argument <code>b</code>.
     * The 24 high-order  bits of <code>b</code>
     * are ignored.
     *
     * @param      b   the byte to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(String sName, int b)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Writes to the output stream all the bytes in array <code>b</code>.
     * If <code>b</code> is <code>null</code>,
     * a <code>NullPointerException</code> is thrown.
     * If <code>b.length</code> is zero, then
     * no bytes are written. Otherwise, the byte
     * <code>b[0]</code> is written first, then
     * <code>b[1]</code>, and so on the last byte
     * written is <code>b[b.length-1]</code>.
     *
     * @param      sName String the label
     * @param      b   the data.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(String sName, byte[] b)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Writes to the output stream all the bytes in array <code>b</code>.
     * If <code>b</code> is <code>null</code>,
     * a <code>NullPointerException</code> is thrown.
     * If <code>b.length</code> is zero, then
     * no bytes are written. Otherwise, the byte
     * <code>b[0]</code> is written first, then
     * <code>b[1]</code>, and so on; the last byte
     * written is <code>b[b.length-1]</code>.
     *
     * @param      sName the label
     * @param      sSpecial any special interpretation of this data
     * @param      b   the data.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(String sName, String sSpecial, byte[] b)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * This method writes bytes from the given offset in the ByteBuffer for a length of iBytes
     * to the destination.
     *
     * @param sFieldName name of the field.
     * @param iOffset the offset in the ByteBuffer to start
     * @param tBuffer ByteBuffer from which to write to destination.
     * @param iBytes the number of bytes to write to the destination.
     *
     * @throws IOException....if an error occurs either reading the ByteBuffer or writing
     *                        to the destination.
     */
    public void write(String sFieldName, int iOffset, ByteBuffer tBuffer, int iBytes)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Writes to the output stream the eight low-
     * order bits of the argument <code>v</code>.
     * The 24 high-order bits of <code>v</code>
     * are ignored. (This means  that <code>writeByte</code>
     * does exactly the same thing as <code>write</code>
     * for an integer argument.) The byte written
     * by this method may be read by the <code>readByte</code>
     * method of interface <code>DataInput</code>,
     * which will then return a <code>byte</code>
     * equal to <code>(byte)v</code>.
     *
     * @param      v   the byte value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeByte(String sName, int v)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Writes two bytes to the output
     * stream to represent the value of the argument.
     * The byte values to be written, in the  order
     * shown, are: <p>
     * <pre><code>
     * (byte)(0xff &amp; (v &gt;&gt; 8))
     * (byte)(0xff &amp; v)
     * </code> </pre> <p>
     * The bytes written by this method may be
     * read by the <code>readShort</code> method
     * of interface <code>DataInput</code> , which
     * will then return a <code>short</code> equal
     * to <code>(short)v</code>.
     *
     * @param      v   the <code>short</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeShort(String sName, int v)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Writes an <code>int</code> value, which is
     * comprised of four bytes, to the output stream.
     * The byte values to be written, in the  order
     * shown, are:
     * <p><pre><code>
     * (byte)(0xff &amp; (v &gt;&gt; 24))
     * (byte)(0xff &amp; (v &gt;&gt; 16))
     * (byte)(0xff &amp; (v &gt;&gt; &#32; &#32;8))
     * (byte)(0xff &amp; v)
     * </code></pre><p>
     * The bytes written by this method may be read
     * by the <code>readInt</code> method of interface
     * <code>DataInput</code> , which will then
     * return an <code>int</code> equal to <code>v</code>.
     *
     * @param      v   the <code>int</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeInt(String sName, int v)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Writes a <code>long</code> value, which is
     * comprised of eight bytes, to the output stream.
     * The byte values to be written, in the  order
     * shown, are:
     * <p><pre><code>
     * (byte)(0xff &amp; (v &gt;&gt; 56))
     * (byte)(0xff &amp; (v &gt;&gt; 48))
     * (byte)(0xff &amp; (v &gt;&gt; 40))
     * (byte)(0xff &amp; (v &gt;&gt; 32))
     * (byte)(0xff &amp; (v &gt;&gt; 24))
     * (byte)(0xff &amp; (v &gt;&gt; 16))
     * (byte)(0xff &amp; (v &gt;&gt;  8))
     * (byte)(0xff &amp; v)
     * </code></pre><p>
     * The bytes written by this method may be
     * read by the <code>readLong</code> method
     * of interface <code>DataInput</code> , which
     * will then return a <code>long</code> equal
     * to <code>v</code>.
     *
     * @param      v   the <code>long</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeLong(String sName, long v)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Writes every character in the string <code>s</code>,
     * to the output stream, in order,
     * two bytes per character. If <code>s</code>
     * is <code>null</code>, a <code>NullPointerException</code>
     * is thrown.  If <code>s.length</code>
     * is zero, then no characters are written.
     * Otherwise, the character <code>s[0]</code>
     * is written first, then <code>s[1]</code>,
     * and so on; the last character written is
     * <code>s[s.length-1]</code>. For each character,
     * two bytes are actually written, high-order
     * byte first, in exactly the manner of the
     * <code>writeChar</code> method.
     *
     * @param      s   the string value to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeChars(String sName, String s)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Writes out elements of an short array from a source element
     * to a final element.
     *
     * @param sArrayName name of the int array
     * @param iFirst the first element number to write
     * @param iLast the last element number to write
     * @param iaArray array containing the elements to write
     *
     */
    public void writeShortArrayRange(String sArrayName, int iFirst, int iLast,
                                     short[] iaArray)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Writes out elements of an short array from a source element
     * to a final element as bytes.
     *
     * @param sArrayName name of the int array
     * @param iFirst the first element number to write
     * @param iLast the last element number to write
     * @param iaArray array containing the elements to write
     *
     */
    public void writeShortArrayRangeAsBytes(String sArrayName, int iFirst, int iLast,
                                            short[] iaArray)
        throws IOException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Writes out elements of an int array from a source element
     * to a final element.
     *
     * @param sArrayName name of the int array
     * @param iFirst the first element number to write
     * @param iLast the last element number to write
     * @param iaArray array containing the elements to write
     *
     */
    public void writeIntArrayRange(String sArrayName, int iFirst, int iLast,
                                   int[] iaArray)
        throws IOException
    {
        throw new Error("Unimplemented");
    }
}
