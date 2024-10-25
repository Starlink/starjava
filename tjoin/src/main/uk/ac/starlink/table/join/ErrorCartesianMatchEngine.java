package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Match Engine which works in an N-dimensional Cartesian space with
 * isotropic per-row errors.
 * Tuples are N+1 element, with the last element being the error radius,
 * so that a match results when the distance between two objects is
 * no greater than the combination of their error radii.
 * This combination may be either a simple sum or the sum in quadrature,
 * according to configuration.
 *
 * @author   Mark Taylor
 * @since    1 Sep 2011
 */
public class ErrorCartesianMatchEngine extends AbstractCartesianMatchEngine {

    private final int ndim_;
    private final ErrorSummation errorSummation_;
    private final DescribedValue[] matchParams_;

    private static final DefaultValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Scaled distance between matched points, 0..1" );
    private static final DefaultValueInfo ERROR_INFO =
        new DefaultValueInfo( "Error", Number.class,
                              "Per-object error radius" );
    private static final DefaultValueInfo ERRSCALE_INFO =
        new DefaultValueInfo( "Scale", Number.class,
                              "Rough average of per-object error distance; "
                            + "just used for tuning in conjunction with "
                            + "bin factor" );

    /**
     * Constructor.
     *
     * <p>The <code>errorSummation</code> parameter configures how the
     * match score is assessed from the error values of two tuples.
     * The match threshold is determined by summing the error values,
     * either by simple addition or by addition in quadrature.
     *
     * @param  ndim  dimensionality
     * @param  errorSummation  how to combine errors; if null, simple is used
     * @param   scale   rough scale of errors
     */
    @SuppressWarnings("this-escape")
    public ErrorCartesianMatchEngine( int ndim, ErrorSummation errorSummation,
                                      double scale ) {
        super( ndim );
        ndim_ = ndim;
        errorSummation_ = errorSummation == null ? ErrorSummation.SIMPLE
                                                 : errorSummation;
        matchParams_ = new DescribedValue[] {
                           new IsotropicScaleParameter( ERRSCALE_INFO ) };
        setIsotropicScale( scale );
    }

    /**
     * Sets the distance scale, which should be roughly the average
     * of per-object error distance
     * This is just used in conjunction with the bin factor for tuning.
     *
     * @param   scale  characteristic scale of errors
     */
    public void setScale( double scale ) {
        super.setIsotropicScale( scale );
    }

    /**
     * Returns the distance scale.
     *
     * @return  characteristic scale of errors
     */
    public double getScale() {
        return super.getIsotropicScale();
    }

    public ValueInfo[] getTupleInfos() {
        List<ValueInfo> infoList = new ArrayList<ValueInfo>();
        for ( int id = 0; id < ndim_; id++ ) {
            infoList.add( createCoordinateInfo( id ) );
        }
        infoList.add( ERROR_INFO );
        return infoList.toArray( new ValueInfo[ 0 ] );
    }

    public DescribedValue[] getMatchParameters() {
        return matchParams_;
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    @Override
    public String toString() {
        return new StringBuffer()
              .append( ndim_ )
              .append( "-d Cartesian with Errors" )
              .append( errorSummation_.getTail() )
              .toString();
    }

    public Supplier<MatchKit> createMatchKitFactory() {
        final Supplier<CartesianBinner> binnerFact = createBinnerFactory();
        return () -> new ErrorCartesianMatchKit( errorSummation_,
                                                 binnerFact.get() );
    }

    public Supplier<Coverage> createCoverageFactory() {
        final CuboidCoverage.PointDecoder pointDecoder =
            CuboidCoverage.createCartesianPointDecoder( ndim_ );
        final CuboidCoverage.ErrorDecoder errDecoder = this::getTupleError;
        return () ->
            CuboidCoverage
           .createVariableErrorCoverage( ndim_, pointDecoder, errDecoder );
    }

    /**
     * Returns unity.
     */
    public double getScoreScale() {
        return 1.0;
    }

    /**
     * Returns the Cartesian position coordinates associated with an
     * input tuple.
     *
     * @param  tuple  (ndim+1)-element coords,error array
     * @return  ndim-element coordinate array
     */
    private double[] getTupleCoords( Object[] tuple ) {
        double[] coords = new double[ ndim_ ];
        for ( int id = 0; id < ndim_; id++ ) {
            coords[ id ] = getNumberValue( tuple[ id ] );
        }
        return coords;
    }

    /**
     * Returns the error value associated with an input tuple.
     *
     * @param  tuple  (ndim+1)-element coords,error array
     * @return   error value
     */
    private double getTupleError( Object[] tuple ) {
        return getNumberValue( tuple[ ndim_ ] );
    }

    /**
     * MatchKit implementation for use with this class.
     */
    private static class ErrorCartesianMatchKit implements MatchKit {
        final ErrorSummation errorSummation_;
        final CartesianBinner binner_;
        final int ndim_;
        final double[] work0_;
        final double[] work1_;
        final double[] work2_;

        /**
         * Constructor.
         *
         * @param  errorSummation  error combination method
         * @param  binner  binner
         */
        ErrorCartesianMatchKit( ErrorSummation errorSummation,
                                CartesianBinner binner ) {
            errorSummation_ = errorSummation;
            binner_ = binner;
            ndim_ = binner_.getNdim();
            work0_ = new double[ ndim_ ];
            work1_ = new double[ ndim_ ];
            work2_ = new double[ ndim_ ];
        }

        public double matchScore( Object[] tuple1, Object[] tuple2 ) {
            binner_.toCoords( tuple1, work1_ );
            binner_.toCoords( tuple2, work2_ );
            double e1 = getTupleError( tuple1 );
            double e2 = getTupleError( tuple2 );
            double err2 = errorSummation_.combineSquared( e1, e2 );
            double dist2 = 0;
            for ( int id = 0; id < ndim_; id++ ) {
                double d = work2_[ id ] - work1_[ id ];
                dist2 += d * d;
                if ( ! ( dist2 <= err2 ) ) {
                    return -1;
                }
            }
            double score = err2 > 0 ? Math.sqrt( dist2 / err2 ) : 0.0;
            assert score >= 0 && score <= 1;
            return score;
        }

        public Object[] getBins( Object[] tuple ) {
            binner_.toCoords( tuple, work0_ );
            return binner_.getRadiusBins( work0_, getTupleError( tuple ) );
        }

        /**
         * Returns the error value associated with an input tuple.
         *
         * @param  tuple  (ndim+1)-element coords,error array
         * @return   error value
         */
        private double getTupleError( Object[] tuple ) {
            return getNumberValue( tuple[ ndim_ ] );
        }
    }
}
