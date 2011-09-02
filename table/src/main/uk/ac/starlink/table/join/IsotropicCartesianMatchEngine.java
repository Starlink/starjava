package uk.ac.starlink.table.join;

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

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        return matchScore( ndim_, toCoords( tuple1 ), toCoords( tuple2 ),
                           getError() );
    }

    public Object[] getBins( Object[] tuple ) {
        return getRadiusBins( toCoords( tuple ), getError() * 0.5 );
    }

    public boolean canBoundMatch() {
        return true;
    }

    public Comparable[][] getMatchBounds( Comparable[] minTuple,
                                          Comparable[] maxTuple ) {
        return createExtendedBounds( minTuple, maxTuple, getError(),
                                     indexRange( 0, ndim_ ) );
    }

    public String toString() {
        return ndim_ + "-d Cartesian";
    }

    /**
     * Returns the Cartesian coordinates for a given match tuple.
     *
     * @param  tuple  input tuple
     * @return  numeric ndim-element coordinate array
     */
    private double[] toCoords( Object[] tuple ) {
        double[] coords = new double[ ndim_ ];
        for ( int id = 0; id < ndim_; id++ ) {
            coords[ id ] = getNumberValue( tuple[ id ] );
        }
        return coords;
    }
}
