package uk.ac.starlink.util;

import java.util.BitSet;

/**
 * Looks out for an unused value in a sequence of submitted numbers.
 * This can be used to identify a suitable 'magic' representation
 * for a bad value.
 *
 * @author   Mark Taylor
 * @since    22 Jun 2006
 */
public class ValueWatcher {

    private final long loBound_;
    private final long hiBound_;
    private final BitSet used_;

    /**
     * Constructs a ValueWatcher which will look out for values in the 
     * range covered by <code>loBound</code> and <code>hiBound</code> 
     * inclusive.  These shouldn't be arbitrarily far apart - 
     * storage of up to about one bit per value in the range will be
     * required.
     *
     * @param  loBound  lower bound to watch for (inclusive)
     * @param  hiBound  upper bound to watch for (inclusive)
     */
    public ValueWatcher( long loBound, long hiBound ) {
        if ( hiBound - loBound >= Integer.MAX_VALUE ) {
            throw new IllegalArgumentException( "Range too wide" );
        }
        loBound_ = loBound;
        hiBound_ = hiBound;
        used_ = new BitSet();
    }

    /**
     * Takes note of a value.  <code>val</code> will never subsequently
     * be returned from {@link #getUnused}.
     *
     * @param  val  value to note
     */
    public void useValue( long val ) {
        if ( val <= hiBound_ && val >= loBound_ ) {
            used_.set( (int) ( val - loBound_ ) );
        }
    }

    /**
     * Returns a value in the range <code>loBound..hiBound</code> which 
     * has never been sumitted to {@link #useValue}.
     * If there is no such value, returns null.
     *
     * @return   unused value in range
     */
    public Long getUnused() {
        long value = loBound_ + used_.nextClearBit( 0 );
        return value <= hiBound_ ? Long.valueOf( value ) : null;
    }
}
