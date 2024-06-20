package uk.ac.starlink.table.join;

import java.util.function.Supplier;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Matchers which matches within a cuboidal shape in a Cartesian space.
 * This differs from most of the other N-dimensional match engines
 * which use an ellipsoid of some sort.  It may be useful for identifying
 * associations with pixels etc.
 * Tuples are just N-element position coordinate vectors.
 *
 * @author    Mark Taylor
 * @since     10 Feb 2014
 */
public class CuboidCartesianMatchEngine extends AbstractCartesianMatchEngine {

    private final int ndim_;
    private final double[] err2s_;
    private final DescribedValue[] matchParams_;
    private double scoreScale_;

    private static final ValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Unscaled distance between matched points" );

    /**
     * Constructor.
     * The initial values of the ellipse principal axis lengths
     * are specified here;
     * the dimensionality of the space is defined by the length of this array.
     *
     * @param   errs  initial axis lengths of the error ellipse
     */
    public CuboidCartesianMatchEngine( double[] errs ) {
        super( errs.length );
        ndim_ = errs.length;
        err2s_ = new double[ ndim_ ];
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
        err2s_[ idim ] = err * err;
        double sscale2 = 0;
        for ( int i = 0; i < ndim_; i++ ) {
            sscale2 += err2s_[ i ];
        }
        scoreScale_ = Math.sqrt( sscale2 );
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

    public ValueInfo getMatchScoreInfo(){ 
        return SCORE_INFO;
    }

    public Supplier<MatchKit> createMatchKitFactory() {
        final Supplier<CartesianBinner> binnerFact = createBinnerFactory();
        final double[] err2s = err2s_.clone();
        return () -> new CuboidCartesianMatchKit( err2s, binnerFact.get() );
    }

    public Supplier<Coverage> createCoverageFactory() {
        final double[] errs = new double[ ndim_ ];
        for ( int idim = 0; idim < ndim_; idim++ ) {
            errs[ idim ] = getError( idim );
        }
        return () -> CuboidCoverage.createFixedCartesianCoverage( ndim_, errs );
    }

    public double getScoreScale() {
        return scoreScale_;
    }

    public String toString() {
        return ndim_ + "-d Cuboid";
    }

    /**
     * MatchKit implementation for use with this class.
     */
    private static class CuboidCartesianMatchKit implements MatchKit {

        final double[] err2s_;
        final CartesianBinner binner_;
        final int ndim_;
        final double[] work0_;
        final double[] work1_;
        final double[] work2_;

        /**
         * Constructor.
         *
         * @param  err2s  per-dimension array of squared error values
         * @param  binner  binner
         */
        CuboidCartesianMatchKit( double[] err2s, CartesianBinner binner ) {
            err2s_ = err2s;
            binner_ = binner;
            ndim_ = binner.getNdim();
            work0_ = new double[ ndim_ ];
            work1_ = new double[ ndim_ ];
            work2_ = new double[ ndim_ ];
        }

        public double matchScore( Object[] tuple1, Object[] tuple2 ) {
            binner_.toCoords( tuple1, work1_ );
            binner_.toCoords( tuple2, work2_ );
            double dist2 = 0;
            for ( int id = 0; id < ndim_; id++ ) {
                double d = work2_[ id ] - work1_[ id ];
                double d2 = d * d;
                if ( d2 > err2s_[ id ] ) {
                    return -1;
                }
                dist2 += d2;
            }
            return Math.sqrt( dist2 );
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
                                         "Half length of cuboid in "
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
