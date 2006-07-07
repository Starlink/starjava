package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;

/**
 * Matcher which matches in an isotropic N-dimensional Cartesian space.
 * Two points are considered matching if they fall within an error
 * sphere of a given size.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Aug 2004
 */
public class IsotropicCartesianMatchEngine 
        extends AbstractCartesianMatchEngine {

    private double error_;
    private final int ndim_;
    final DescribedValue errorParam_;

    /**
     * Constructs a matcher which matches points in an
     * <tt>ndim</tt>-dimensional Cartesian space.
     * An initial isotropic error margin is specified.
     *
     * @param   ndim  dimensionality of the space
     * @param   err  initial maximum distance between two matching points
     * @param   normaliseScores  <tt>true</tt> iff you want match scores
     *                           to be normalised
     */
    public IsotropicCartesianMatchEngine( int ndim, double err, 
                                          boolean normaliseScores ) {
        super( ndim, normaliseScores );
        ndim_ = ndim;
        setError( err );
        error_ = err;
        errorParam_ = new ErrorParam();
    }

    /**
     * Sets the isotropic matching error.
     *
     * @param   err  radius of error sphere
     */
    public void setError( double err ) {
        for ( int i = 0; i < ndim_; i++ ) {
            setError( i, err );
        }
        error_ = err;
    }

    /**
     * Returns the isotropic matching error.
     *
     * @return  radius of error sphere
     */
    public double getError() {
        for ( int i = 0; i < ndim_; i++ ) {
            assert getError( i ) == error_;
        }
        return error_;
    }

    /**
     * Returns a single parameter controlling the isotropic error 
     * (radius of a match sphere).
     */
    public DescribedValue[] getMatchParameters() {
        return new DescribedValue[] { errorParam_ };
    }

    public String toString() {
        return ndim_ + "-d Cartesian";
    }

    /**
     * Implements the parameter which controls the matching error.
     */
    private class ErrorParam extends DescribedValue {
        ErrorParam() {
            super( new DefaultValueInfo( "Error", Number.class,
                                 "Maximum Cartesian separation for match" ) );
        }
        public Object getValue() {
            return new Double( getError() );
        }
        public void setValue( Object value ) {
            setError( ((Number) value).doubleValue() );
        }
    }

}
