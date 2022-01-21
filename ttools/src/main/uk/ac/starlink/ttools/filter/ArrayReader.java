package uk.ac.starlink.ttools.filter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads values from a typed numeric array.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2022
 */
public class ArrayReader {

    private final LengthReader lrdr_;
    private final ValueReader vrdr_;

    /** Instance for double[] arrays. */
    private static final ArrayReader DOUBLE =
        new ArrayReader( a -> a instanceof double[] ? ((double[]) a).length :-1,
                         (a,i) -> a instanceof double[] ? ((double[]) a)[ i ]
                                                        : Double.NaN );

    /** Instance for float[] arrays. */
    private static final ArrayReader FLOAT =
        new ArrayReader( a -> a instanceof float[] ? ((float[]) a).length : -1,
                         (a,i) -> a instanceof float[] ? ((float[]) a)[ i ]
                                                       : Double.NaN );

    /** Instance for long[] arrays. */
    private static final ArrayReader LONG =
        new ArrayReader( a -> a instanceof long[] ? ((long[]) a).length : -1,
                         (a,i) -> a instanceof long[] ? ((long[]) a)[ i ]
                                                      : Double.NaN );
                                                 

    /** Instance for int[] arrays. */
    private static final ArrayReader INT =
        new ArrayReader( a -> a instanceof int[] ? ((int[]) a).length : -1,
                         (a,i) -> a instanceof int[] ? ((int[]) a)[ i ]
                                                     : Double.NaN );

    /** Instance for short[] arrays. */
    private static final ArrayReader SHORT =
        new ArrayReader( a -> a instanceof short[] ? ((short[]) a).length : -1,
                         (a,i) -> a instanceof short[] ? ((short[]) a)[ i ]
                                                       : Double.NaN );

    /** Instance for byte[] arrays. */
    private static final ArrayReader BYTE =
        new ArrayReader( a -> a instanceof byte[] ? ((byte[]) a).length : -1,
                         (a,i) -> a instanceof byte[] ? ((byte[]) a)[ i ]
                                                      : Double.NaN );

    private static final Map<Class<?>,ArrayReader> READERS = createReaderMap();

    /**
     * Constructor.
     *
     * @param  lrdr  reads array length
     * @param  vrdr  reads array elements
     */
    private ArrayReader( LengthReader lrdr, ValueReader vrdr ) {
        lrdr_ = lrdr;
        vrdr_ = vrdr;
    }

    /**
     * Returns array length for a suitable array object.
     *
     * @param  array   candidate array object
     * @return  array length if array is of type expected by this reader,
     *          otherwise -1
     */
    public int getLength( Object array ) {
        return lrdr_.getLength( array );
    }

    /**
     * Returns element numeric value for a suitable array object.
     *
     * @param  array   candidate array object
     * @return  numeric value of element <code>index</code> if array is of
     *          tye expected by this reader, otherwise Double.NaN
     */
    public double getValue( Object array, int index ) {
        return vrdr_.getValue( array, index );
    }

    /**
     * Returns an instance of this class suitable for a given array class.
     *
     * @param   arrayClazz  class of arrays to read
     * @return   array reader instance for array objects of the submitted type,
     *           or null if nothing suitable is available
     */
    public static ArrayReader forClass( Class<?> arrayClazz ) {
        return READERS.get( arrayClazz );
    }

    /**
     * Constructs a map of all known ArrayReader instances.
     *
     * @return  unmodifiable map of arrayClass-&gt;ArrayReader
     */
    private static Map<Class<?>,ArrayReader> createReaderMap() {
        Map<Class<?>,ArrayReader> map = new HashMap<>();
        map.put( double[].class, DOUBLE );
        map.put( float[].class, FLOAT );
        map.put( int[].class, INT );
        map.put( short[].class, SHORT );
        map.put( byte[].class, BYTE );
        return Collections.unmodifiableMap( map );
    }

    /**
     * Knows how to acquire array length.
     */
    @FunctionalInterface
    private interface LengthReader {

        /**
         * Returns array length for a suitable array object.
         *
         * @param  array   candidate array object
         * @return  array length if array is of type expected by this reader,
         *          otherwise -1
         */
        int getLength( Object array );
    }

    /**
     * Knows how to read array elements.
     */
    @FunctionalInterface
    private interface ValueReader {

        /**
         * Returns element numeric value for a suitable array object.
         *
         * @param  array   candidate array object
         * @return  numeric value of element <code>index</code> if array is of
         *          tye expected by this reader, otherwise Double.NaN
         */
        double getValue( Object array, int index );
    }
}
