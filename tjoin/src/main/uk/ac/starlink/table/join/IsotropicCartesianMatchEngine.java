package uk.ac.starlink.table.join;

import java.util.function.Supplier;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Matcher which matches in an isotropic N-dimensional Cartesian space.
 * Two points are considered matching if they fall within a given error
 * distance of each other.
 * The isotropic scale is used as the error.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Aug 2004
 */
public class IsotropicCartesianMatchEngine 
        extends AbstractCartesianMatchEngine {

    private final int ndim_;
    private final DescribedValue[] matchParams_;

    private static final ValueInfo ERR_INFO =
        new DefaultValueInfo( "Error", Number.class,
                              "Maximum Cartesian separation for match" );
    private static final ValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Spatial distance between matched points" );

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
        super( ndim );
        ndim_ = ndim;
        matchParams_ = new DescribedValue[] {
                           new IsotropicScaleParameter( ERR_INFO ) };
        setIsotropicScale( err );
    }

    /**
     * Sets the matching error.
     *
     * @param   err  maximum match error
     */
    public void setError( double err ) {
        setIsotropicScale( err );
    }

    /**
     * Returns the matching error.
     *
     * @return  maximum match error
     */
    public double getError() {
        return getIsotropicScale();
    }

    public ValueInfo[] getTupleInfos() {
        ValueInfo[] infos = new ValueInfo[ ndim_ ];
        for ( int id = 0; id < ndim_; id++ ) {
            infos[ id ] = createCoordinateInfo( id );
        }
        return infos;
    }

    public DescribedValue[] getMatchParameters() {
        return matchParams_;
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public double getScoreScale() {
        return getError();
    }

    public Supplier<MatchKit> createMatchKitFactory() {
        final Supplier<CartesianBinner> binnerFact = createBinnerFactory();
        final double error = getError();
        return () -> new IsotropicMatchKit( error, binnerFact.get() );
    }

    public boolean canBoundMatch() {
        return true;
    }

    public NdRange getMatchBounds( NdRange[] inRanges, int index ) {
        return createExtendedBounds( inRanges[ index ], getError(),
                                     indexRange( 0, ndim_ ) );
    }

    public String toString() {
        return ndim_ + "-d Cartesian";
    }

    /**
     * MatchKit implementation for use with this class.
     */
    private static class IsotropicMatchKit implements MatchKit {

        final double error_;
        final CartesianBinner binner_;
        final int ndim_;
        final double[] work0_;
        final double[] work1_;
        final double[] work2_;

        /**
         * Constructor.
         *
         * @param  error  maximum permissible separation
         * @param  binner  binner
         */
        IsotropicMatchKit( double error, CartesianBinner binner ) {
            error_ = error;
            binner_ = binner;
            ndim_ = binner.getNdim();
            work0_ = new double[ ndim_ ];
            work1_ = new double[ ndim_ ];
            work2_ = new double[ ndim_ ];
        }

        public double matchScore( Object[] tuple1, Object[] tuple2 ) {
            binner_.toCoords( tuple1, work1_ );
            binner_.toCoords( tuple2, work2_ );
            return AbstractCartesianMatchEngine
                  .matchScore( ndim_, work1_, work2_, error_ );
        }

        public Object[] getBins( Object[] tuple ) {
            binner_.toCoords( tuple, work0_ );
            return binner_.getRadiusBins( work0_, error_ * 0.5 );
        }
    }
}
