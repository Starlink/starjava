package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Match Engine which works in an N-dimensional Cartesian space with
 * isotropic per-row errors.
 *
 * @author   Mark Taylor
 * @since    1 Sep 2011
 */
public class CircularErrorCartesianMatchEngine
        extends AbstractErrorCartesianMatchEngine {

    private final int ndim_;

    private static final DefaultValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Spatial distance between matched points" );

    /**
     * Constructor.
     *
     * @param  ndim  dimensionality
     * @param   scale   rough scale of errors
     */
    public CircularErrorCartesianMatchEngine( int ndim, double scale ) {
        super( ndim, scale );
        ndim_ = ndim;
    }

    public ValueInfo[] getTupleInfos() {
        List<ValueInfo> infoList = new ArrayList<ValueInfo>();
        for ( int id = 0; id < ndim_; id++ ) {
            infoList.add( AbstractCartesianMatchEngine
                         .createCoordinateInfo( ndim_, id ) );
        }
        DefaultValueInfo errInfo =
            new DefaultValueInfo( "Error", Number.class,
                                  "Per-object error radius" );
        errInfo.setNullable( false );
        infoList.add( errInfo );
        return infoList.toArray( new ValueInfo[ 0 ] );
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public String toString() {
        return ndim_ + "-d Cartesian with Errors";
    }

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {

        /* Determine the distance threshold for matching of this pair. */
        double errSum = getNumberValue( tuple1[ ndim_ ] )
                      + getNumberValue( tuple2[ ndim_ ] );

        /* If the distance in any dimension is greater than the maximum,
         * reject it straight away.  This is a cheap test which will normally
         * reject most pairs. */
        for ( int id = 0; id < ndim_; id++ ) {
            if ( Math.abs( getNumberValue( tuple1[ id ] ) -
                           getNumberValue( tuple2[ id ] ) ) > errSum ) {
                return -1;
            }
        }

        /* Otherwise, calculate the distance using Pythagoras. */
        double dist2 = 0;
        for ( int id = 0; id < ndim_; id++ ) {
            double dist = getNumberValue( tuple1[ id ] ) -
                          getNumberValue( tuple2[ id ] );
            dist2 += dist * dist;
        }
        return dist2 <= errSum * errSum ? Math.sqrt( dist2 )
                                        : -1;
    }

    public Object[] getBins( Object[] tuple ) {
        double[] coords = new double[ ndim_ ];
        for ( int id = 0; id < ndim_; id++ ) {
            coords[ id ] = getNumberValue( tuple[ id ] );
        }
        return getBins( coords, getNumberValue( tuple[ ndim_ ] ) );
    }

    public boolean canBoundMatch() {
        return true;
    }

    public Comparable[][] getMatchBounds( Comparable[] inMins,
                                          Comparable[] inMaxs ) {

        /* Extend the bounds region both ways in each space dimension by
         * twice the maximum error value. */
        Comparable[] outMins = new Comparable[ ndim_ + 1 ];
        Comparable[] outMaxs = new Comparable[ ndim_ + 1 ];
        double err = 2 * getNumberValue( inMaxs[ ndim_ ] );
        if ( err >= 0 ) {
            for ( int id = 0; id < ndim_; id++ ) {
                outMins[ id ] = AbstractCartesianMatchEngine
                               .add( inMins[ id ], -err );
                outMaxs[ id ] = AbstractCartesianMatchEngine
                               .add( inMaxs[ id ], +err );
            }
        }
        return new Comparable[][] { outMins, outMaxs };
    }
}
