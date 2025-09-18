package uk.ac.starlink.ttools.plot;

import java.util.Arrays;

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
    private double loPos_ = Double.NaN;
    private double hiPos_ = Double.NaN;

    /* *************************** NOTE *********************************
     * NOTE: when reading and, especially, altering this code bear in 
     * mind that any comparison expression involving a NaN evaluates false,
     * so for instance ( ! ( a < b ) ) is NOT the same as ( a >= b ) for
     * floating point variables a, b.
     ********************************************************************/

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
    @SuppressWarnings("this-escape")
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
        loPos_ = range.loPos_;
        hiPos_ = range.hiPos_;
    }

    /**
     * Submits a value to this range.  The range will be expanded as required
     * to include <code>value</code> 
     *
     * @param  datum  value to accommodate in this range
     */
    public void submit( double datum ) {
        if ( isFinite( datum ) ) {
            if ( ! ( lo_ <= datum ) ) {
               lo_ = datum;
            }
            if ( ! ( hi_ >= datum ) ) {
                hi_ = datum;
            }
            if ( datum > 0.0 ) {
                if ( ! ( loPos_ <= datum ) ) {
                    loPos_ = datum;
                }
                if ( ! ( hiPos_ >= datum ) ) {
                    hiPos_ = datum;
                }
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
                if ( ! ( lo <= 0.0 ) ) {
                    loPos_ = lo;
                }
            }
            if ( ! Double.isInfinite( hi ) ) {
                hi_ = hi;
                if ( ! ( hi <= 0.0 ) ) {
                    hiPos_ = hi;
                }
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
            if ( loPos_ < hiPos_ ) {
                return new double[] { loPos_, hiPos_ };
            }
            else if ( loPos_ == hiPos_ ) {
                return new double[] { hiPos_ * 0.9, hiPos_ * 1.1 };
            }
            else if ( hiPos_ > 1.0 ) {
                return new double[] { 0.1, hiPos_ };
            }
            else if ( hiPos_ > 0.0 ) {
                return new double[] { hiPos_ * 0.001, hiPos_ };
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

                /* Defend against very large values. */
                double d0 = 1.0;
                double d1 = hi_ - d0 < hi_ + d0
                          ? d0
                          : 0.01 * Math.abs( hi_ );
                return new double[] { hi_ - d1, hi_ + d1 };
            }
            else {
                return new double[] { 0.0, 1.0 };
            }
        }
    }

    /**
     * Returns true if no data about this range has been set.
     *
     * @return   true for clear range
     */
    public boolean isClear() {
        return Double.isNaN( lo_ ) && Double.isNaN( hi_ );
    }

    /**
     * Returns true if both ends of the range have values which are not NaN.
     *
     * @return  true iff low and high are numbers
     */
    public boolean isFinite() {
        return ! Double.isNaN( lo_ ) && ! Double.isNaN( hi_ );
    }

    /**
     * Adds padding to either end of this range.
     *
     * @param  ratio  padding ratio (should normally be greater than 0)
     */
    public void pad( double ratio ) {
        if ( lo_ < hi_ ) {
            lo_ -= ( hi_ - lo_ ) * ratio;
            hi_ += ( hi_ - lo_ ) * ratio;
        }
        if ( loPos_ < hiPos_ ) {
            loPos_ /= Math.exp( Math.log( hiPos_ / loPos_ ) * ratio );
            hiPos_ *= Math.exp( Math.log( hiPos_ / loPos_ ) * ratio );
        }
    }

    /**
     * Unsets the lower and upper bounds for this range.
     */
    public void clear() {
        lo_ = Double.NaN;
        hi_ = Double.NaN;
        loPos_ = Double.NaN;
        hiPos_ = Double.NaN;
    }

    /**
     * Limits the bounds of this range.  If either of the submitted 
     * bounds is finite (not infinite and not NaN) then the corresponding
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
        if ( isFinite( lo ) ) {
            lo_ = lo;
            if ( ! ( lo <= 0.0 ) ) {
                loPos_ = lo;
            }
            if ( hi_ < lo_ ) {
                hi_ = lo_;
                hiPos_ = loPos_;
            }
        }
	if ( isFinite( hi ) && ! ( hi < lo_ ) ) {
            hi_ = hi;
            if ( ! ( hi <= 0.0 ) ) {
                hiPos_ = hi;
            }
            if ( lo_ > hi_ ) {
                lo_ = hi_;
                loPos_ = hiPos_;
            }
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

    /**
     * Extends this range by another one.
     * The effect is as if all the data that has been submitted to
     * the other range has been submitted to this one.
     *
     * @param  other  other range
     */
    public void extend( Range other ) {
        if ( isFinite( other.lo_ ) && ! ( lo_ < other.lo_ ) ) {
            lo_ = other.lo_;
        }
        if ( isFinite( other.hi_ ) && ! ( hi_ > other.hi_ ) ) {
            hi_ = other.hi_;
        }
        if ( isFinite( other.loPos_ ) && ! ( loPos_ < other.loPos_ ) ) {
            loPos_ = other.loPos_;
        }
        if ( isFinite( other.hiPos_ ) && ! ( hiPos_ > other.hiPos_ ) ) {
            hiPos_ = other.hiPos_;
        }
    }

    @Override
    public boolean equals( Object o ) {
        return o instanceof Range
            && Arrays.equals( this.stateArray(), ((Range) o).stateArray() );
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode( stateArray() );
    }

    @Override
    public String toString() {
        return "[" + lo_ + " .. " + hi_ + "]";
    }

    /**
     * Returns the complete state of this object as a double array.
     *
     * @return  state object
     */
    private double[] stateArray() {
        return new double[] { lo_, hi_, loPos_, hiPos_ };
    }

    /**
     * Indicates whether the given value is a finite real value.
     *
     * @param   value  value to test
     * @return  true iff value is non-infinite and non-NaN
     */
    private static boolean isFinite( double value ) {
        return ! Double.isNaN( value ) && ! Double.isInfinite( value );
    }
}
