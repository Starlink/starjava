package uk.ac.starlink.array;

import java.lang.reflect.Array;
import java.io.IOException;
import java.nio.Buffer;
import uk.ac.starlink.util.GenericNioBuffer;

/**
 * Implementation of ArrayImpl which uses a {@link java.nio.Buffer} for
 * the data storage.  Can be used in such a way that the buffer itself
 * is not actually created until it is required (and hence is never created
 * if it is not required).
 *
 * @author   Mark Taylor (Starlink)
 * @see java.nio.Buffer
 */
public class NioArrayImpl implements ArrayImpl {

    /**
     * Defines an object which can provide deferred access to a
     * {@link java.nio.Buffer}.  Can be used to provide access to a buffer
     * which will only be created on demand; if it is never required it
     * will never be created.
     */
    public interface BufferGetter {

        /**
         * Indicates whether the buffer, when created, will be read-only
         * or not.
         * 
         * @return  true if the buffer created by getBuffer will be 
         *          read-only
         */
        boolean isReadOnly();

        /**
         * Returns an NIO buffer.  Will be called a maximum of once.
         *
         * @return  the deferred-access NIO buffer.
         * @throws  IOException  if something goes wrong
         */
        Buffer getBuffer() throws IOException;

        /**
         * Releases the NIO buffer.  Ought to be called when this object,
         * and the buffer it provides, are no longer required.
         * Will only be called if getBuffer has been called.
         * It cannot be guaranteed however that this method will be called.
         * 
         * @throws   IOException  if something goes wrong
         */
        void releaseBuffer() throws IOException;
    }

    private final BufferGetter bufget;
    private final OrderedNDShape oshape;
    private final Type type;
    private final Number badValue;
    private final boolean isWritable;
    private Object mappedArray;
    private Buffer nioBuf;
    private GenericNioBuffer genBuf;

    /**
     * Constructs an ArrayImpl using a deferred-access buffer object.
     * This constructor will only create the NIO buffer when it is 
     * required - if pixel access is never performed the buffer is never
     * created.
     *
     * @param  bufget   an object which can do deferred creation of a
     *                  {@link java.nio.Buffer}
     * @param   oshape  the shape of the array
     * @param   type   the data type of the array
     * @param   badval the magic bad value for the array (may be null)
     */
    public NioArrayImpl( BufferGetter bufget, OrderedNDShape oshape, Type type,
                         Number badval ) {
        this.bufget = bufget;
        this.oshape = oshape;
        this.type = type;
        this.badValue = badval;
        isWritable = ! bufget.isReadOnly();
    }

    /**
     * Constructs an ArrayImpl from a {@link java.nio.Buffer}.
     *
     * @param   buf  an NIO buffer
     * @param   oshape  the shape of the array
     * @param   type   the data type of the array
     * @param   badval the magic bad value for the array (may be null)
     */
    public NioArrayImpl( final Buffer buf, OrderedNDShape oshape, Type type,
                         Number badval ) {
        this( new BufferGetter() {
                  public boolean isReadOnly() { return buf.isReadOnly(); }
                  public Buffer getBuffer() { return buf; }
                  public void releaseBuffer() {}
              }, oshape, type, badval );
    }

    public void open() throws IOException {

        /* Get the buffer; use a duplicate of the supplied one and rewind
         * it to ensure that it is not being simultaneously modified 
         * by the supplier. */
        nioBuf = new GenericNioBuffer( bufget.getBuffer() )
                .duplicate()
                .rewind();

        /* Create a convenience object capable of treating NIO buffers
         * in a generic fashion. */
        genBuf = new GenericNioBuffer( nioBuf );

        /* Check that that buffer is of the correct type. */
        if ( genBuf.getElementClass() != type.javaClass() ) {
            throw new IOException(
                "Supplied buffer is of type " + genBuf.getElementClass() +
                " not declared type " + type.javaClass() );
        }

        /* See if the buffer has a backing array; if it does, and it is
         * exactly the same size as this NDArray then we can map it. */
        if ( genBuf.hasArray() &&
             genBuf.arrayOffset() == 0 &&
             nioBuf.capacity() == Array.getLength( genBuf.array() ) ) {
            mappedArray = genBuf.array();
        }
        else {
            mappedArray = null;
        }
    }

    public boolean canMap() { 
        return mappedArray != null;
    }

    public Object getMapped() {
        return mappedArray;
    }
  
    public void close() throws IOException {
        if ( nioBuf != null ) {
            bufget.releaseBuffer();
        }
        mappedArray = null;
        nioBuf = null;
        genBuf = null;
    }

    public OrderedNDShape getShape() {
        return oshape;
    }
    public Type getType() {
        return type;
    }
    public Number getBadValue() {
        return badValue;
    }
    public boolean isRandom() {
        return true;
    }
    public boolean isReadable() {
        return true;
    }
    public boolean isWritable() {
        return isWritable;
    }

    public boolean multipleAccess() {
        return true;
    }

    public AccessImpl getAccess() {
        final Buffer acNioBuf = genBuf.duplicate();
        final GenericNioBuffer acGenBuf = new GenericNioBuffer( acNioBuf );
        return new AccessImpl() {
            public void setOffset( long off ) {
                acNioBuf.position( (int) off );
            }
            public void read( Object buffer, int start, int size ) {
                acGenBuf.get( buffer, start, size );
            }
            public void write( Object buffer, int start, int size ) {
                acGenBuf.put( buffer, start, size );
            }
            public void close() {
            }
        };
    }

}
