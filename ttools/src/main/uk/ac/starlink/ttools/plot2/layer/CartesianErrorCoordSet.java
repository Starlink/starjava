package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * MultiPointCoordSet for errors in Cartesian data coordinates.
 *
 * @author   Mark Taylor
 * @since    19 Mar 2013
 */
public class CartesianErrorCoordSet implements MultiPointCoordSet {

    private final int ndim_;
    private final FloatingCoord[] pCoords_;
    private final FloatingCoord[] mCoords_;

    /**
     * Constructor.
     *
     * @param   axisNames   names of Cartesian axes; the length of this array
     *                      defines the dimensionality of the space
     */
    public CartesianErrorCoordSet( String[] axisNames ) {
        ndim_ = axisNames.length;
        pCoords_ = new FloatingCoord[ ndim_ ];
        mCoords_ = new FloatingCoord[ ndim_ ];
        for ( int idim = 0; idim < ndim_; idim++ ) {
            String axName = axisNames[ idim ];
            pCoords_[ idim ] =
                FloatingCoord
               .createCoord( axName + " Positive Error",
                             "Error in " + axName + " positive direction; "
                           + "used in negative direction too if no negative "
                           + "error is supplied", false );
            mCoords_[ idim ] =
                FloatingCoord
               .createCoord( axName + " Negative Error",
                             "Error in " + axName + " negative direction; "
                           + "defaults to same as positive error "
                           + "if left blank", false );
        }
    }

    public Coord[] getCoords() {
        Coord[] coords = new Coord[ ndim_ * 2 ];
        for ( int idim = 0; idim < ndim_; idim++ ) {
            coords[ idim * 2 + 0 ] = pCoords_[ idim ];
            coords[ idim * 2 + 1 ] = mCoords_[ idim ];
        }
        return coords;
    }

    public int getPointCount() {
        return ndim_ * 2;
    }

    public boolean readPoints( TupleSequence tseq, int icol, double[] dpos0,
                               double[][] dposExtras ) {
        boolean hasErrors = false;
        for ( int idim = 0; idim < ndim_; idim++ ) {
            int pIndex = idim * 2 + 0;
            int mIndex = idim * 2 + 1;
            double pErr = pCoords_[ idim ]
                         .readDoubleCoord( tseq, icol + pIndex );
            double mErr = mCoords_[ idim ]
                         .readDoubleCoord( tseq, icol + mIndex );
            if ( Double.isNaN( mErr ) ) {
                mErr = pErr;
            }
            boolean pOk = pErr > 0;
            boolean mOk = mErr > 0;
            double[] pDpos = dposExtras[ pIndex ];
            double[] mDpos = dposExtras[ mIndex ];
            for ( int j = 0; j < ndim_; j++ ) {
                double dp = dpos0[ j ];
                pDpos[ j ] = dp;
                mDpos[ j ] = dp;
            }
            if ( pOk ) {
                pDpos[ idim ] += pErr;
            }
            if ( mOk ) {
                mDpos[ idim ] -= mErr;
            }
            hasErrors = hasErrors || pOk || mOk;
        }
        return hasErrors;
    }
}
