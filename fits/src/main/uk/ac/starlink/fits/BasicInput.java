package uk.ac.starlink.fits;

import java.io.IOException;

/**
 * Interface defining the basic data input operations required for
 * the FITS reading classes.  All the read operations operate at the
 * current position of the assumed stream and advance the current
 * position past the item they just read.  Storage is FITS-like,
 * which, happily, matches ByteBuffer conventions.  Random access
 * may or may not be supported.
 *
 * <p>This interface has some similarities to {@link java.io.DataInput},
 * and that interface could have been used instead, but this one is
 * explicitly used for the hand-coded FITS reader implementation to
 * make clear which operations need to be efficient.  At present
 * no multi-byte (or multi-other-primitive-type) read operations are
 * included, since it's not clear that these are required in practice
 * for efficient table input, though for (uncommon?) tables that have
 * columns with large array values that might not be true.
 * If that turns out to be an important use case, such methods can
 * be added to this interface, implemented in its implementations,
 * and used in the clients of this interface.
 *
 * <p>Instances of this are <strong>not</strong> expected to be safe for
 * use from multiple threads.  Depending on the implementation,
 * ignoring that fact may be a <em>very bad idea indeed</em>.
 *
 * @author   Mark Taylor
 * @since    1 Dec 2014
 */
public interface BasicInput {

    /**
     * Reads a byte from the stream.
     * The current position is advanced.
     *
     * @return  byte value
     */
    byte readByte() throws IOException;

    /**
     * Reads a 2-byte integer from the stream.
     * The current position is advanced.
     *
     * @return  short value
     */
    short readShort() throws IOException;

    /**
     * Reads a 4-byte integer from the stream.
     * The current position is advanced.
     *
     * @return  int value
     */
    int readInt() throws IOException;

    /**
     * Reads an 8-byte integer from the stream.
     * The current position is advanced.
     *
     * @return  long value
     */
    long readLong() throws IOException;

    /**
     * Reads a 4-byte floating point value from the stream.
     * The current position is advanced.
     *
     * @return  float value
     */
    float readFloat() throws IOException;

    /**
     * Reads an 8-byte floating point value from the stream.
     * The current position is advanced.
     *
     * @return  double value
     */
    double readDouble() throws IOException;

    /**
     * Skips a given number of bytes forwards through the stream.
     * An exception is thrown if there are not enough bytes left.
     * 
     * @param  nbyte  number of bytes to skip
     */
    void skip( long nbyte ) throws IOException;

    /**
     * Releases resources belonging to this object.
     * Attempts to use it after a call to this method result in
     * undefined behaviour.
     */
    void close() throws IOException;

    /**
     * Indicates whether this object supports random access.
     * The seek and getOffset methods may only be called if this method
     * returns true.
     *
     * @return   true  iff random access is supported
     */
    boolean isRandom();

    /**
     * Moves the current position of this stream to a given byte offset
     * (optional operation).
     *
     * @throws  UnsupportedOperationException  if not random-access
     */
    void seek( long offset ) throws IOException;  // optional

    /**
     * Returns the curent position in this stream
     * (optional operation).
     *
     * @throws  UnsupportedOperationException  if not random-access
     */
    long getOffset(); // optional
}
