package uk.ac.starlink.table.join;

import java.util.function.Supplier;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Matcher which matches in an anisotropic N-dimensional Cartesian space.
 * Two points are considered matching if they fall within an error
 * ellipsoid whose principal axis lengths are specified.
 * Tuples are just N-element position coordinate vectors.
 *
 * @author   Mark Taylor (Starlink)
 * @since    15 Aug 2004
 */
public class AnisotropicCartesianMatchEngine
         extends AbstractCartesianMatchEngine {

    private final int ndim_;
    private final double[] err2rs_;
    private final DescribedValue[] matchParams_;

    private static final ValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Normalised distance between matched points"
                            + " (0 is identical, 1 is worst possible match)" );

    /**
     * Constructor.
     * The initial values of the ellipse principal axis lengths
     * are specified here;
     * the dimensionality of the space is defined by the length of this array.
     *
     * @param   errs  initial axis lengths of the error ellipse
     */
    public AnisotropicCartesianMatchEngine( double[] errs ) {
        super( errs.length );
        ndim_ = errs.length;
        err2rs_ = new double[ ndim_ ];
        matchParams_ = new DescribedValue[ ndim_ ];
        for ( int id = 0; id < ndim_; id++ ) {
            matchParams_[ id ] = new ErrorParameter( id );
            setScale( id, errs[ id ] );
        }
    }

    /**
     * Sets the matching error in a given dimension.
     *
     * @param  idim  dimension index
     * @param  err   axis length of error ellipse in dimension <code>idim</code>
     */
    public void setError( int idim, double err ) {
        setScale( idim, err );
    }

    /**
     * Returns the matching error in a given dimension.
     *
     * @param  idim  dimension index
     * @return  axis length of error ellipse in dimension <code>idim</code>
     */
    public double getError( int idim ) {
        return getScale( idim );
    }

    protected void setScale( int idim, double err ) {
        super.setScale( idim, err );
        err2rs_[ idim ] = 1.0 / ( err * err );
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

    public Supplier<MatchKit> createMatchKitFactory() {
        final Supplier<CartesianBinner> binnerFact = createBinnerFactory();
        final double[] err2rs = err2rs_.clone();
        return () -> new AnisotropicMatchKit( err2rs, binnerFact.get() );
    }

    public double getScoreScale() {
        return 1.0;
    }

    public Supplier<Coverage> createCoverageFactory() {
        final double[] errs = new double[ ndim_ ];
        for ( int idim = 0; idim < ndim_; idim++ ) {
            errs[ idim ] = getError( idim );
        }
        return () -> CuboidCoverage.createFixedCartesianCoverage( ndim_, errs );
    }

    public String toString() {
        return ndim_ + "-d Cartesian Anisotropic";
    }

    /**
     * MatchKit implementation for use with this class.
     */
    private static class AnisotropicMatchKit implements MatchKit {

        final double[] err2rs_;
        final CartesianBinner binner_;
        final int ndim_;
        final double[] work0_;
        final double[] work1_;
        final double[] work2_;

        /**
         * Constructor.
         *
         * @param   ndim-element array of per-dimension err**-2 values
         * @param   binner  binner object
         */
        AnisotropicMatchKit( double[] err2rs, CartesianBinner binner ) {
            err2rs_ = err2rs;
            binner_ = binner;
            ndim_ = binner_.getNdim();
            work0_ = new double[ ndim_ ];
            work1_ = new double[ ndim_ ];
            work2_ = new double[ ndim_ ];
        }

        public double matchScore( Object[] tuple1, Object[] tuple2 ) {
            binner_.toCoords( tuple1, work1_ );
            binner_.toCoords( tuple2, work2_ );
            double normDist2 = 0;
            for ( int id = 0; id < ndim_; id++ ) {
                double d = work2_[ id ] - work1_[ id ];
                normDist2 += d * d * err2rs_[ id ];
                if ( ! ( normDist2 <= 1.0 ) ) {
                    return -1.0;
                }
            }
            assert normDist2 >= 0 && normDist2 <= 1.0;
            return Math.sqrt( normDist2 );
        }

        public Object[] getBins( Object[] tuple ) {
            binner_.toCoords( tuple, work0_ );
            return binner_.getScaleBins( work0_ );
        }
    }

    /**
     * Implements a parameter controlling the matching error in a given
     * dimension.
     */
    private class ErrorParameter extends DescribedValue {
        private final int idim_;

        /**
         * Constructor.
         *
         * @param  idim  dimension index
         */
        ErrorParameter( int idim ) {
            super( new DefaultValueInfo( "Error in "
                                       + getCoordinateName( idim ),
                                         Number.class,
                                         "Axis length of error ellipse in "
                                       + getCoordinateDescription( idim )
                                       + " direction" ) );
            idim_ = idim;
        }

        public Object getValue() {
            return Double.valueOf( getError( idim_ ) );
        }

        public void setValue( Object value ) {
            setError( idim_, getNumberValue( value ) );
        }
    }
}
