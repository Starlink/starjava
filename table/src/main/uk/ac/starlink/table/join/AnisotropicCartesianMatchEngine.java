package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;

/**
 * Matcher which matches in an anisotropic N-dimensional Cartesian space.
 * Two points are considered matching if they fall within an error 
 * ellipse whose principle radii can be specified.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Aug 2004
 */
public class AnisotropicCartesianMatchEngine
        extends AbstractCartesianMatchEngine {

    private final int ndim_;
    private final DescribedValue[] errorParams_;

    /**
     * Constructs an anisotropic Cartesian matcher.
     * Matching scores are normalised by default, since it won't in general
     * make sense to use non-normalised ones (there's no obvious metric
     * in anisotropic space).
     * The initial values of the error ellipse are specified here; 
     * the dimensionality of the space is defined by the length of
     * this array.
     *
     * @param   errs  initial axis lengths of error ellipse
     */
    public AnisotropicCartesianMatchEngine( double[] errs ) {
        super( errs.length, true );
        ndim_ = errs.length;
        errorParams_ = new ErrorParam[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            setError( i, errs[ i ] );
            errorParams_[ i ] = new ErrorParam( i );
        }
    }

    /**
     * Returns a set of matching parameters, one representing the 
     * radius of each axis of the error ellipse for each dimension.
     */
    public DescribedValue[] getMatchParameters() {
        return (DescribedValue[]) errorParams_.clone();
    }

    public String toString() {
        return ndim_ + "-d Cartesian (anisotropic)";
    }

    /**
     * Implements the parameters controlling the matching errors.
     */
    private class ErrorParam extends DescribedValue {
        final int idim_;
        ErrorParam( int idim ) {
            super( new DefaultValueInfo( "Error in " + 
                                         getCoordinateName( idim ),
                                         Double.class,
                                         "Radius of error ellipse in " +
                                         getCoordinateDescription( idim ) + 
                                         " direction" ) );
            idim_ = idim;
        }
        public Object getValue() {
            return new Double( getError( idim_ ) );
        }
        public void setValue( Object value ) {
            setError( idim_, ((Number) value).doubleValue() );
        }
    }

}
