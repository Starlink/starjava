package uk.ac.starlink.ttools.plot2;

/**
 * Designates a sub-range; 0 &lt;= lo &lt;= hi &lt;= 1.
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
     * @throws  IllegalArgumentException unless 0&lt;=lo&lt;=hi&lt;=1
     */
    public Subrange( double lo, double hi ) {
        if ( ! ( lo >= 0 && lo <= hi && hi <= 1 ) ) {
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
