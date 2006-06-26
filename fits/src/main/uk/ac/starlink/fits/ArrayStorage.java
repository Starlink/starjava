package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Abstract superclass for objects which do the nuts and bolts of
 * writing array data for column-oriented storage.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 * @see      ColumnStore
 */
abstract class ArrayStorage {

    private final Class componentClass_;
    private final char formatChar_;
    private final int typeBytes_;

    /**
     * Constructor.
     *
     * @param   componentClass  element type of the arrays this will store
     * @param   formatChar      FITS format identification character
     * @param   typeBytes       number of bytes required for a single element
     */
    protected ArrayStorage( Class componentClass, char formatChar,
                            int typeBytes ) {
        componentClass_ = componentClass;
        formatChar_ = formatChar;
        typeBytes_ = typeBytes;
    }

    /**
     * Returns the element type for arrays stored by this object.
     *
     * @return  array element class
     */
    public Class getComponentClass() {
        return componentClass_;
    }

    /**
     * Returns the FITS format identification character.
     *
     * @return   FITS type char
     */
    public char getFormatChar() {
        return formatChar_;
    }

    /**
     * Returns the number of bytes per element stored.
     *
     * @return  number of bytes per element
     */
    public int getTypeBytes() {
        return typeBytes_;
    }

    /**
     * Writes a given array to an output stream.
     *
     * @param  array  array of type compatible with this storage object
     * @param  out    destination stream
     */
    public abstract void writeArray( Object array, DataOutput out )
            throws IOException;

    /** Instance for storing boolean arrays. */
    public static ArrayStorage BOOLEAN = new ArrayStorage( boolean.class, 'L',
                                                           1 ) {
        public void writeArray( Object array, DataOutput out )
                throws IOException {
            boolean[] values = (boolean[]) array;
            for ( int i = 0; i < values.length; i++ ) {
                out.writeByte( values[ i ] ? 'T' : 'F' );
            }
        }
    };

    /** Instance for storing <code>byte</code> arrays. */
    public static ArrayStorage BYTE = new ArrayStorage( byte.class, 'B', 1 ) {
        public void writeArray( Object array, DataOutput out )
                throws IOException {
            out.write( (byte[]) array );
        }
    };

    /** Instance for storing <code>short</code> arrays. */
    public static ArrayStorage SHORT = new ArrayStorage( short.class, 'I', 2 ) {
        public void writeArray( Object array, DataOutput out )
                throws IOException {
            short[] values = (short[]) array;
            for ( int i = 0; i < values.length; i++ ) {
                out.writeShort( values[ i ] );
            }
        }
    };

    /** Instance for storing <code>int</code> arrays. */
    public static ArrayStorage INT = new ArrayStorage( int.class, 'J', 4 ) {
        public void writeArray( Object array, DataOutput out )
                throws IOException {
            int[] values = (int[]) array;
            for ( int i = 0; i < values.length; i++ ) {
                out.writeInt( values[ i ] );
            }
        }
    };

    /** Instance for storing <code>long</code> arrays. */
    public static ArrayStorage LONG = new ArrayStorage( long.class, 'K', 8 ) {
        public void writeArray( Object array, DataOutput out )
                throws IOException {
            long[] values = (long[]) array;
            for ( int i = 0; i < values.length; i++ ) {
                out.writeLong( values[ i ] );
            }
        }
    };

    /** Instance for storing <code>float</code> arrays. */
    public static ArrayStorage FLOAT = new ArrayStorage( float.class, 'E', 4 ) {
        public void writeArray( Object array, DataOutput out )
                throws IOException {
            float[] values = (float[]) array;
            for ( int i = 0; i < values.length; i++ ) {
                out.writeFloat( values[ i ] );
            }
        }
    };

    /** Instance for storing <code>double</code> arrays. */
    public static ArrayStorage DOUBLE = new ArrayStorage( double.class,
                                                          'D', 8 ) {
        public void writeArray( Object array, DataOutput out )
                throws IOException {
            double[] values = (double[]) array;
            for ( int i = 0; i < values.length; i++ ) {
                out.writeDouble( values[ i ] );
            }
        }
    };
}
