package uk.ac.starlink.ttools.plot2;

/**
 * Designates a sub-range.
 * A subrange is a pair of values (lo,hi) for which lo&lt;=hi,
 * which modifies an external range.
 * If (lo,hi) is (0,1), the external range is unmodified.
 * The natural span of a subrange is therefore in the range 0-1,
 * but there is nothing to stop its values going lower than zero or
 * greater than 1.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
@Equality
public class Subrange {

    private final double lo_;
    private final double hi_;

    /**
     * Constructor.
     *
     * @param   lo  lower limit
     * @param   hi  upper limit
     * @throws  IllegalArgumentException unless lo&lt;=hi
     */
    public Subrange( double lo, double hi ) {
        if ( ! ( lo <= hi ) ) {
            throw new IllegalArgumentException( "Bad range: "
                                              + lo + ", " + hi );
        }
        lo_ = lo;
        hi_ = hi;
    }

    /**
     * Constructs a subrange covering the whole range 0-1.
     */
    public Subrange() {
        this( 0, 1 );
    }

    /**
     * Returns lower limit.
     *
     * @return  low bound
     */
    public double getLow() {
        return lo_;
    }

    /**
     * Returns upper limit.
     *
     * @return  high bound
     */
    public double getHigh() {
        return hi_;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Subrange ) {
            Subrange other = (Subrange) o;
            return this.lo_ == other.lo_
                && this.hi_ == other.hi_;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits( (float) lo_ )
             + Float.floatToIntBits( (float) hi_ );
    }

    @Override
    public String toString() {
        return "(" + lo_ + "," + hi_ + ")";
    }
}
