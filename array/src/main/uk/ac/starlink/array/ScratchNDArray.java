package uk.ac.starlink.array;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.nio.ByteOrder;

/**
 * An NDArray with data held in a fast random-access scratch array in 
 * memory or on local disk.
 *
 * @author   Mark Taylor (Starlink)
 * @see   CopyNDArray
 */
public class ScratchNDArray extends BridgeNDArray {

    /**
     * Constructs a scratch array with shape, type and bad value handling
     * characteristics copied from a template ArrayDescription.
     * The nature of the backing store is chosen automatically based 
     * on how large the requested array will be.
     *
     * @param  template NDArray
     */
    public ScratchNDArray( ArrayDescription template ) {
        this( template.getShape(), template.getType(), 
              template.getBadHandler() );
    }


    /**
     * Constructs a scratch array with shape, type and bad value handling
     * supplied explicitly.
     * The nature of the backing store is chosen automatically based 
     * on how large the requested array will be.
     *
     * @param   shape       shape of the new array
     * @param   type        primitive data type of the new array
     * @param   badHandler  bad value handler to use for the new array
     */
    public ScratchNDArray( OrderedNDShape shape, Type type,
                           BadHandler badHandler ) {
        this( shape, type, badHandler, 
              getDefaultBackingStore( shape.getNumPixels() * 
                                        type.getNumBytes() ) );
    }

    /**
     * Constructs a scratch array with shape, type, bad value handling
     * and backing store type supplied explicitly.
     *
     * @param   shape       shape of the new array
     * @param   type        primitive data type of the new array
     * @param   badHandler  bad value handler to use for the new array
     * @param   bstore      indicates what method should be used to 
     *                      implement the backing store of the array
     */
    public ScratchNDArray( OrderedNDShape shape, Type type, 
                           BadHandler badHandler, BackingStore bstore ) {
        super( new NioArrayImpl( getNioBuffer( type, shape.getNumPixels(), 
                                               bstore ),
                                 shape, type, badHandler.getBadValue() ) );
    }

    /**
     * Gets the default backing store type for an array of a given size 
     * in bytes.
     *
     * @param  nbytes  number of bytes needed to allocate
     * @return  the backing store type used by default for an array of
     *          nbytes bytes long
     */
    private static BackingStore getDefaultBackingStore( long nbytes ) {
        if ( nbytes < directAllocationThreshold() ) {
            return BackingStore.MEMORY;
        }
        else if ( nbytes < Integer.MAX_VALUE ) {
            return BackingStore.DIRECT;
        }
        else {
            throw new UnsupportedOperationException(
                "Buffers longer than " + Integer.MAX_VALUE + 
                " not yet supported" );
        }
    }

    /**
     * Returns an NIO buffer of a given type and size, using a given
     * backing store type.
     *
     * @param  type     primitive type of the buffer
     * @param  npix     number of pixels
     * @param  bstore   the backing store implementation type
     * @return   a new NIO Buffer of the requested type and size
     */
    private static Buffer getNioBuffer( Type type, long npix, 
                                        BackingStore bstore ) {

        /* Allocate a buffer backed by a normal array in memory. */
        if ( bstore == BackingStore.MEMORY ) {
            Object array = type.newArray( (int) npix );
            if ( type == Type.BYTE ) {
                return ByteBuffer.wrap( (byte[]) array );
            }
            else if ( type == Type.SHORT ) {
                return ShortBuffer.wrap( (short[]) array );
            }
            else if ( type == Type.INT ) {
                return IntBuffer.wrap( (int[]) array );
            }   
            else if ( type == Type.FLOAT ) {
                return FloatBuffer.wrap( (float[]) array );
            }
            else if ( type == Type.DOUBLE ) {
                return DoubleBuffer.wrap( (double[]) array );
            }
            else {
                throw new AssertionError();
            }
        }

        /* Get a buffer backed by a directly allocated array. */
        else if ( bstore == BackingStore.DIRECT ) {
            long nbyte = npix * type.getNumBytes();
            if ( nbyte > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException( 
                    "Too many bytes " + nbyte + " for direct allocation" );
            }

            /* Directly allocate a byte buffer to contain the data. */
            ByteBuffer byteBuf = ByteBuffer.allocateDirect( (int) nbyte );

            /* Set its byte order to that appropriate for the native platform
             * for efficiency (do I want to do this, or does it prevent the
             * thing from having a backing array?? */
            byteBuf.order( ByteOrder.nativeOrder() );

            /* Get an appropriately typed buffer. */
            if ( type == Type.BYTE ) {
                return byteBuf;
            }
            else if ( type == Type.SHORT ) {
                return byteBuf.asShortBuffer();
            }
            else if ( type == Type.INT ) {
                return byteBuf.asIntBuffer();
            }
            else if ( type == Type.FLOAT ) {
                return byteBuf.asFloatBuffer();
            }
            else if ( type == Type.DOUBLE ) {
                return byteBuf.asDoubleBuffer();
            }
            else {
                throw new AssertionError();
            }
        }
        else {
            throw new AssertionError();
        }
    }

    /**
     * Returns the number of bytes above which a directly-allocated rather
     * than a java primitive array should be used.
     */
    private static int directAllocationThreshold() {
        return (int)
               Math.min( Math.max( Runtime.getRuntime().totalMemory() / 64L,
                                   1048576L ),
                         (long) Integer.MAX_VALUE );
    }

    /**
     * Typesafe enum class enumerating the types of backing store
     * implementation available.
     */
    public static class BackingStore {
        private String name;
        private BackingStore( String name ) {
            this.name = name;
        }
        public static final BackingStore MEMORY = new BackingStore( "MEMORY" );
        public static final BackingStore DIRECT = new BackingStore( "DIRECT" );
    }
}
