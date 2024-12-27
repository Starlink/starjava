package uk.ac.starlink.util;

import java.lang.reflect.Array;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.ShortBuffer;

/**
 * Convenience class which wraps one of the NIO &lt;Type&gt;Buffer
 * classes to provide generic functionality.  Using this class merely
 * allows one to invoke some of the methods which are defined on 
 * all the specific buffer types but not on the Buffer superclass itself
 * without a lot of pesky typecasting.
 *
 * @author   Mark Taylor (Starlink)
 */
public class GenericNioBuffer {

    private static abstract class Accessor {
        abstract void get( Object dst, int offset, int length );
        abstract void put( Object scr, int offset, int length );
        abstract Buffer duplicate();
        abstract boolean hasArray();
        abstract int arrayOffset();
        abstract Object array();
        abstract Class<?> getElementClass();
    }

    private final Accessor acc;
    private final Buffer buf;


    /**
     * Construct a GenericNioBuffer based on an existing 
     * {@link java.nio.Buffer}.
     *
     * @param   buf  the NIO buffer
     */
    public GenericNioBuffer( final Buffer buf ) {
        this.buf = buf;
        if ( buf instanceof ByteBuffer ) {
            acc = new Accessor() {
                ByteBuffer buffer = (ByteBuffer) buf;
                void get( Object dst, int offset, int length ) {
                    buffer.get( (byte[]) dst, offset, length );
                }
                void put( Object src, int offset, int length ) {
                    buffer.put( (byte[]) src, offset, length );
                }
                Buffer duplicate() {
                    return buffer.duplicate();
                }
                boolean hasArray() {
                    return buffer.hasArray();
                }
                int arrayOffset() {
                    return buffer.arrayOffset();
                }
                Object array() {
                    return buffer.array();
                }
                Class<?> getElementClass() {
                    return byte.class;
                }
            };
        }
        else if ( buf instanceof ShortBuffer ) {
            acc = new Accessor() {
                ShortBuffer buffer = (ShortBuffer) buf;
                void get( Object dst, int offset, int length ) {
                    buffer.get( (short[]) dst, offset, length );
                }
                void put( Object src, int offset, int length ) {
                    buffer.put( (short[]) src, offset, length );
                }
                Buffer duplicate() {
                    return buffer.duplicate();
                }
                boolean hasArray() {
                    return buffer.hasArray();
                }
                int arrayOffset() {
                    return buffer.arrayOffset();
                }
                Object array() {
                    return buffer.array();
                }
                Class<?> getElementClass() {
                    return short.class;
                }
            };
        }
        else if ( buf instanceof IntBuffer ) {
            acc = new Accessor() {
                IntBuffer buffer = (IntBuffer) buf;
                void get( Object dst, int offset, int length ) {
                    buffer.get( (int[]) dst, offset, length );
                }
                void put( Object src, int offset, int length ) {
                    buffer.put( (int[]) src, offset, length );
                }
                Buffer duplicate() {
                    return buffer.duplicate();
                }
                boolean hasArray() {
                    return buffer.hasArray();
                }
                int arrayOffset() {
                    return buffer.arrayOffset();
                }
                Object array() {
                    return buffer.array();
                }
                Class<?> getElementClass() {
                    return int.class;
                }
            };
        }
        else if ( buf instanceof FloatBuffer ) {
            acc = new Accessor() {
                FloatBuffer buffer = (FloatBuffer) buf;
                void get( Object dst, int offset, int length ) {
                    buffer.get( (float[]) dst, offset, length );
                }
                void put( Object src, int offset, int length ) {
                    buffer.put( (float[]) src, offset, length );
                }
                Buffer duplicate() {
                    return buffer.duplicate();
                }
                boolean hasArray() {
                    return buffer.hasArray();
                }
                int arrayOffset() {
                    return buffer.arrayOffset();
                }
                Object array() {
                    return buffer.array();
                }
                Class<?> getElementClass() {
                    return float.class;
                }
            };
        }
        else if ( buf instanceof DoubleBuffer ) {
            acc = new Accessor() {
                DoubleBuffer buffer = (DoubleBuffer) buf;
                void get( Object dst, int offset, int length ) {
                    buffer.get( (double[]) dst, offset, length );
                }
                void put( Object src, int offset, int length ) {
                    buffer.put( (double[]) src, offset, length );
                }
                Buffer duplicate() {
                    return buffer.duplicate();
                }
                boolean hasArray() {
                    return buffer.hasArray();
                }
                int arrayOffset() {
                    return buffer.arrayOffset();
                }
                Object array() {
                    return buffer.array();
                }
                Class<?> getElementClass() {
                    return double.class;
                }
            };
        }
        else { 
            throw new IllegalArgumentException(
                "Buffer " + buf + " is of unsupported type" ); 
        }
    }

    /**
     * Returns the buffer object on which this generic buffer is based.
     *
     * @return  the buffer set at construction
     */
    public Buffer getBuffer() {
        return buf;
    }

    /**
     * Generic relative bulk <i>get</i> method.
     * Fils a given destination array with primitives transferred 
     * from this buffer.
     *
     * @param   dst   an array of primitives matching the type of the 
     *                nio Buffer
     * @see java.nio.DoubleBuffer#get(double[])
     */
    public void get( Object dst ) {
        acc.get( dst, 0, Array.getLength( dst ) );
    }

    /**
     * Generic relative bulk <i>get</i> method.
     * Transfers a given number of primitives from this buffer into 
     * the given destination array starting at a given offset into the array.
     *
     * @param   dst   an array of primitives matching the type of the
     *                nio Buffer
     * @param   offset  the offset within the array of the first primitive
     *                  to be written
     * @param   length  the number of primitives to be transferred
     * @see java.nio.DoubleBuffer#get(double[],int,int)
     */
    public void get( Object dst, int offset, int length ) {
        acc.get( dst, offset, length );
    }

    /**
     * Generic relative bulk <i>put</i> method.
     * Transfers the entire content of the given source array into this buffer.
     *
     * @param   src   an array of primitives matching the type of the 
     *                nio Buffer
     * @see java.nio.DoubleBuffer#put(double[])
     */
    public void put( Object src ) {
        acc.put( src, 0, Array.getLength( src ) );
    }

    /**
     * Generic relative bulk <i>put</i> method.
     * Transfers a given number of primitives from the given source 
     * array starting at a given point into this buffer.
     *
     * @param   src   an array of primitives matching the type of the 
     *                nio Buffer
     * @param   offset  the offset within the array of the first primitive
     *                  to be read
     * @param   length  the number of primitives to tranfer
     * @see java.nio.DoubleBuffer#put(double[],int,int)
     */
    public void put( Object src, int offset, int length ) {
        acc.put( src, offset, length );
    }

    /**
     * Creates a new buffer that shares this buffer's content.
     * <p>
     * The content of the new buffer will be that of this buffer. 
     * Changes to this buffer's content will be visible in the new 
     * buffer, and vice versa; the two buffers' position, limit, and mark
     * values will be independent. 
     * <p>
     * The new buffer's capacity, limit, position, and mark values will 
     * be identical to those of this buffer. The new buffer will be direct 
     * if, and only if, this buffer is direct, and it will be
     * read-only if, and only if, this buffer is read-only. 
     *
     * @return  the new buffer.  Note it is a java.nio.Buffer and not 
     *          a copy of this GenericNioBuffer
     * @see java.nio.DoubleBuffer#duplicate
     */
    public Buffer duplicate() {
        return acc.duplicate();
    }

    /**
     * Tells whether or not this buffer is backed by an accessible 
     * primitive array.  If this method returns true then the
     * {@link #array} and {@link #arrayOffset} methods may safely be
     * invoked.
     *
     * @return   true if, and only if, this buffer is backed by an array
     *           and is not read-only
     * @see java.nio.DoubleBuffer#hasArray
     */
    public boolean hasArray() {
        return acc.hasArray();
    }

    /**
     * Returns the primitive array that backs this buffer 
     * <i>(optional operation)</i>.
     * Modifications to this buffer's content will cause the returned
     * array's content to be modified, and vice versa.
     * <p>
     * Invoke the {@link #hasArray} method before invoking this method in
     * order to ensure that this buffer has an accessible backing array.
     *
     * @return  the array that backs this buffer
     * @throws  ReadOnlyBufferException  if this buffer is backed by an
     *          array but is read-only
     * @throws  UnsupportedOperationException  if this buffer is not backed
     *          by an accessible array
     */
    public Object array() {
        return acc.array();
    }

    /**
     * Returns the offset within this buffer's backing array of the first
     * element of the buffer <i>(optional operation)</i>.
     * If this buffer is backed by an array then buffer position <i>p</i>
     * corresponds to array index <i>p</i> + arrayOffset().
     * <p>
     * Invoke the {@link #hasArray} method before invoking this method in
     * order to ensure that this buffer has an accessible backing array.
     *
     * @return   the offset within this buffer's array of the first 
     *           element of the buffer
     * @throws  ReadOnlyBufferException  if this buffer is backed by an
     *          array but is read-only
     * @throws  UnsupportedOperationException  if this buffer is not backed
     *          by an accessible array
     */
    public int arrayOffset() {
        return acc.arrayOffset();
    }

    /**
     * Returns the class object of the primitive type that the buffer 
     * holds.  Thus <code>double.class</code> is returned if the base buffer
     * is a <code>DoubleBuffer</code> etc.
     * 
     * @return  the class of the primitive elements that this buffer holds
     */
    public Class<?> getElementClass() {
        return acc.getElementClass();
    }

}
