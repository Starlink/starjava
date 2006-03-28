package uk.ac.starlink.topcat.plot;

/**
 * Describes a one-dimensional range.
 * This is effectively a lower and upper bound, but either of these
 * may be absent.
 *
 * @author   Mark Taylor
 * @since    28 Mar 2006
 */
public class Range {

    private double lo_ = Double.NaN;
    private double hi_ = Double.NaN;
    private double loPositive_ = Double.NaN;

    /**
     * Constructs an unbounded range.
     */
    public Range() {
    }

    /**
     * Constructs a range with given lower and upper bounds.
     * Either or both may be NaN.
     *
     * @param  lo  lower bound
     * @param  hi  upper bound
     */
    public Range( double lo, double hi ) {
        this();
        setBounds( lo, hi );
    }

    /**
     * Constructs a range with given lower and upper bounds.
     * The first two elements of the <code>bounds</code> array are taken
     * as the initial lower and upper bounds.  Either may be NaN.
     *
     * @param  bounds  2-element array giving lower, upper bounds
     */
    public Range( double[] bounds ) {
        this( bounds[ 0 ], bounds[ 1 ] );
    }

    /**
     * Constructs a new range which is a copy of an existing one.
     *
     * @param   range  range to copy
     */
    public Range( Range range ) {
        lo_ = range.lo_;
        hi_ = range.hi_;
        loPositive_ = range.loPositive_;
    }

    /**
     * Submits a value to this range.  The range will be expanded as required
     * to include <code>value</code> 
     *
     * @param  datum  value to accommodate in this range
     */
    public void submit( double datum ) {
        if ( ! Double.isNaN( datum ) && ! Double.isInfinite( datum ) ) {
            if ( ! ( lo_ < datum ) ) {
               lo_ = datum;
            }
            if ( ! ( hi_ > datum ) ) {
                hi_ = datum;
            }
            if ( datum > 0.0 && ! ( loPositive_ < datum ) ) {
                loPositive_ = datum;
            }
        }
    }

    /**
     * Resets the bounds of this range.
     * The first two elements of the <code>bounds</code> array are taken
     * as the initial lower and upper bounds.  Either may be NaN.
     *
     * @param  bounds  2-element array giving lower, upper bounds
     */
    public void setBounds( double[] bounds ) {
        setBounds( bounds[ 0 ], bounds[ 1 ] );
    }

    /**
     * Resets the bounds of this range.
     * Either or both may be NaN.
     *
     * @param  lo  lower bound
     * @param  hi  upper bound
     */
    public void setBounds( double lo, double hi ) {
        if ( lo > hi ) {
            throw new IllegalArgumentException( "Bad range: " + lo + " .. " + 
                                                hi );
        }
        else {
            clear();
            if ( ! Double.isInfinite( lo ) ) {
                lo_ = lo;
                if ( lo > 0.0 ) {
                    loPositive_ = lo;
                }
            }
            if ( ! Double.isInfinite( hi ) ) {
                hi_ = hi;
            }
        }
    }

    /**
     * Returns the current bounds of this range.  Either or both may be null.
     *
     * @return   2-element array giving lower, upper bound values
     */
    public double[] getBounds() {
        return new double[] { lo_, hi_ };
    }

    /**
     * Returns finite upper and lower bounds for this range.  Both are
     * guaranteed to be non-infinite and non-NaN.  If no finite lower
     * and upper bounds have ever been set for this range, they will 
     * have to be made up to some extent.  If the <code>positive</code>
     * parameter is set true, then both returned bounds are guaranteed
     * to be greater than zero.
     *
     * @param  positive  true iff strictly positive bounds are required
     * @return  2-element array giving finite lower, upper bounds
     */
    public double[] getFiniteBounds( boolean positive ) {
        if ( positive ) {
            if ( loPositive_ < hi_ ) {
                return new double[] { loPositive_, hi_ };
            }
            else if ( loPositive_ == hi_ ) {
                return new double[] { hi_ * 0.9, hi_ * 1.1 };
            }
            else if ( hi_ > 1.0 ) {
                return new double[] { 0.1, hi_ };
            }
            else if ( hi_ > 0.0 ) {
                return new double[] { hi_ * 0.001, hi_ };
            }
            else {
                return new double[] { 1.0, 10.0 };
            }
        }
        else {
            if ( lo_ < hi_ ) {
                return new double[] { lo_, hi_ };
            }
            else if ( lo_ == hi_ ) {
                return new double[] { hi_ * 0.9, hi_ * 1.1 };
            }
            else {
                return new double[] { 0.0, 1.0 };
            }
        }
    }

    /**
     * Unsets the lower and upper bounds for this range.
     */
    public void clear() {
        lo_ = Double.NaN;
        hi_ = Double.NaN;
        loPositive_ = Double.NaN;
    }

    /**
     * Limits the bounds of this range.  If either of the submitted 
     * bounds is finite (not inifinite and not NaN) then the corresponding
     * bound of this range will be replaced by it.
     *
     * @param  lo  new lower bound, or NaN
     * @param  hi  new upper bound, or NaN
     */
    public void limit( double lo, double hi ) {
        if ( lo > hi ) {
            throw new IllegalArgumentException( "Bad range: "
                                              + lo + " .. " + hi );
        }
        if ( ! Double.isNaN( lo ) && ! Double.isInfinite( lo ) ) {
            lo_ = lo;
            if ( lo > 0.0 ) {
                loPositive_ = lo;
            }
        }
	if ( ! Double.isNaN( hi ) && ! Double.isInfinite( hi ) && hi >= lo_ ) {
            hi_ = hi;
        }
    }

    /**
     * Limits the bounds of this range.  If either of the submitted 
     * bounds is finite (not inifinite and not NaN) then the corresponding
     * bound of this range will be replaced by it.
     *
     * @param  bounds  2-element array giving new lower, upper bounds;
     *         either may be NaN
     */
    public void limit( double[] bounds) {
        if ( bounds != null ) {
            limit( bounds[ 0 ], bounds[ 1 ] );
        }
    }

    /**
     * Limits this range by another one.
     * If either of the bounds of <code>boundRange</code> is finite, it
     * will replace the corresponding bound of this one.
     *
     * @param  boundRange  range giving new bounds
     */
    public void limit( Range boundRange ) {
        limit( boundRange.getBounds() );
    }

    public String toString() {
        return "[" + lo_ + " .. " + hi_ + "]";
    }
}
