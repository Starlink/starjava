package uk.ac.starlink.topcat;

/**
 * Utility class for handling positive integers.
 * Useful if you want to reference many instances of small values,
 * and not so many of large ones; this implementation
 * caches the small ones, and creates the large ones on demand.
 *
 * @author   Mark Taylor
 * @since    3 Feb 2015
 */
public abstract class Count {

    private long value_;

    private static final int N_SMALL = 200;
    private static final Count[] SMALL_COUNTS = createCounts( N_SMALL );

    /**
     * Private constructor forces use of factory method.
     */
    private Count() {
    }

    /**
     * Returns the numeric value of this count.
     *
     * @return  count value
     */
    public abstract long getValue();

    /**
     * Obtains an instance of this class for a given value.
     *
     * @param  lval  value
     * @return   instance
     */
    public static Count getCount( long lval ) {
        if ( lval < N_SMALL ) {
            return SMALL_COUNTS[ (int) lval ];
        }
        else if ( lval < Integer.MAX_VALUE ) {
            return createIntCount( (int) lval );
        }
        else {
            return createLongCount( lval );
        }
    }

    /**
     * Returns the next count instance in sequence.
     * As a special case, an input null value is treated as a count with
     * value zero.
     *
     * @param   in  input value, or null
     * @return   count for in+1
     */
    public static Count increment( Count in ) {
        return getCount( ( in == null ? 0 : in.getValue() ) + 1 );
    }

    /**
     * Returns a sequence of the first N counts.
     *
     * @param   n   number required
     * @return  array of counts representing 0, 1, 2, ...
     */
    private static Count[] createCounts( int n ) {
        Count[] counts = new Count[ n ];
        for ( int i = 0; i < n; i++ ) {
            counts[ i ] = createIntCount( i );
        }
        return counts;
    }

    /**
     * Returns a new instance based on an int.
     *
     * @param  ival  value
     * @return  instance
     */
    private static Count createIntCount( final int ival ) {
        return new Count() {
            public long getValue() {
                return ival;
            }
        };
    }

    /**
     * Returns a new instance based on a long.
     *
     * @param  lval  value
     * @return   instance
     */
    private static Count createLongCount( final long lval ) {
        return new Count() {
            public long getValue() {
                return lval;
            }
        };
    }
}
