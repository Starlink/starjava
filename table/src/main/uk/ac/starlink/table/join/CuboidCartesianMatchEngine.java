package uk.ac.starlink.table.join;

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

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        double[] coords1 = toCoords( tuple1 );
        double[] coords2 = toCoords( tuple2 );
        double dist2 = 0;
        for ( int id = 0; id < ndim_; id++ ) {
            double d = coords2[ id ] - coords1[ id ];
            double d2 = d * d;
            if ( d2 > err2s_[ id ] ) {
                return -1;
            }
            dist2 += d2;
        }
        return Math.sqrt( dist2 );
    }

    public Object[] getBins( Object[] tuple ) {
        return getScaleBins( toCoords( tuple ) );
    }

    public boolean canBoundMatch() {
        return true;
    }

    public NdRange getMatchBounds( NdRange[] inRanges, int index ) {
        Comparable[] inMins = inRanges[ index ].getMins();
        Comparable[] inMaxs = inRanges[ index ].getMaxs();
        Comparable[] outMins = new Comparable[ ndim_ ];
        Comparable[] outMaxs = new Comparable[ ndim_ ];
        for ( int id = 0; id < ndim_; id++ ) {
            double err = getError( id );
            outMins[ id ] = add( inMins[ id ], -err );
            outMaxs[ id ] = add( inMaxs[ id ], +err );
        }
        return new NdRange( outMins, outMaxs );
    }

    public String toString() {
        return ndim_ + "-d Cuboid";
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
            return new Double( getError( idim_ ) );
        }

        public void setValue( Object value ) {
            setError( idim_, getNumberValue( value ) );
        }
    }
}
