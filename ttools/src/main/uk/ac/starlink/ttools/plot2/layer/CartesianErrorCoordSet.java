package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * MultiPointCoordSet for bidirectional errors in Cartesian data coordinates.
 *
 * @author   Mark Taylor
 * @since    19 Mar 2013
 */
public class CartesianErrorCoordSet implements CartesianMultiPointCoordSet {

    private final int nSpaceDim_;
    private final int nErrDim_;
    private final int[] iErrDims_;
    private final FloatingCoord[] pCoords_;
    private final FloatingCoord[] mCoords_;

    /**
     * Constructor.
     * It constructs a coord set for error coordinates in
     * one or more dimensions within in a Cartesian space
     * of <code>nSpaceDim</code> dimensions.
     *
     * @param   axisNames   <code>nSpaceDim</code>-element array of names
     *                      of Cartesian axes (only those represented in
     *                      <code>iErrDims</code> are actually used)
     * @param   iErrDims    array of axis indices for which errors are to
     *                      be acquired; each element must be an index in the
     *                      range 0 &lt;= x &lt; <code>nSpaceDim</code>
     */
    public CartesianErrorCoordSet( String[] axisNames, int[] iErrDims ) {
        nSpaceDim_ = axisNames.length;
        iErrDims_ = iErrDims;
        nErrDim_ = iErrDims.length;
        pCoords_ = new FloatingCoord[ nErrDim_ ];
        mCoords_ = new FloatingCoord[ nErrDim_ ];
        for ( int jdim = 0; jdim < nErrDim_; jdim++ ) {
            int iErrDim = iErrDims[ jdim ];
            String axName = axisNames[ iErrDim ];
            String axname = axName.toLowerCase();
            pCoords_[ jdim ] =
                FloatingCoord.createCoord(
                    new InputMeta( axname + "errhi",
                                   axName + " Positive Error" )
                   .setShortDescription( "Error in " + axName
                                       + " positive direction" )
                   .setXmlDescription( new String[] {
                        "<p>Error in the " + axName + " coordinate",
                        "in the positive direction.",
                        "If no corresponding negative error value is supplied,",
                        "then this value is also used in the negative",
                        "direction, i.e. in that case errors are assumed",
                        "to be symmetric.",
                        "</p>",
                    } )
                , false );
            mCoords_[ jdim ] =
                FloatingCoord.createCoord(
                    new InputMeta( axname + "errlo",
                                   axName + " Negative Error" )
                   .setShortDescription( "Error in " + axName
                                       + " negative direction" )
                   .setXmlDescription( new String[] {
                        "<p>Error in the " + axName + " coordinate",
                        "in the negative direction.",
                        "If left blank, it is assumed to take the same value",
                        "as the positive error.",
                        "</p>",
                    } )
                , false );
        }
    }

    public Coord[] getCoords() {
        Coord[] coords = new Coord[ nErrDim_ * 2 ];
        for ( int jdim = 0; jdim < nErrDim_; jdim++ ) {
            coords[ jdim * 2 + 0 ] = pCoords_[ jdim ];
            coords[ jdim * 2 + 1 ] = mCoords_[ jdim ];
        }
        return coords;
    }

    public int getPointCount() {
        return nErrDim_ * 2;
    }

    public boolean readPoints( Tuple tuple, int icol,
                               double[] dpos0, double[][] dposExtras ) {
        boolean hasErrors = false;
        for ( int jdim = 0; jdim < nErrDim_; jdim++ ) {
            int iErrDim = iErrDims_[ jdim ];
            int pIndex = jdim * 2 + 0;
            int mIndex = jdim * 2 + 1;
            double pErr = pCoords_[ jdim ]
                         .readDoubleCoord( tuple, icol + pIndex );
            double mErr = mCoords_[ jdim ]
                         .readDoubleCoord( tuple, icol + mIndex );
            if ( Double.isNaN( mErr ) ) {
                mErr = pErr;
            }
            boolean pOk = pErr > 0;
            boolean mOk = mErr > 0;
            double[] pDpos = dposExtras[ pIndex ];
            double[] mDpos = dposExtras[ mIndex ];
            for ( int i = 0; i < nSpaceDim_; i++ ) {
                double dp = dpos0[ i ];
                pDpos[ i ] = dp;
                mDpos[ i ] = dp;
            }
            if ( pOk ) {
                pDpos[ iErrDim ] += pErr;
            }
            if ( mOk ) {
                mDpos[ iErrDim ] -= mErr;
            }
            hasErrors = hasErrors || pOk || mOk;
        }
        return hasErrors;
    }

    /**
     * Returns a coord set with errors in all of the dimensions of a
     * Cartesian space.
     *
     * @param  axisNames  names of the dimensions; the length of this array
     *                    defines the dimensionality of the space
     * @return   new coord set
     */
    public static CartesianErrorCoordSet
            createAllAxesErrorCoordSet( String[] axisNames ) {
        int ndim = axisNames.length;
        int[] idims = new int[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            idims[ i ] = i;
        }
        return new CartesianErrorCoordSet( axisNames, idims );
    }

    /**
     * Returns a coord set with errors in a single indicated dimension of a
     * Cartesian space.
     *
     * @param  ndim   dimensionality of the Cartesian space
     * @param  iErrDim   index of the dimension for which error coordinates
     *                   are to be obtained
     * @param  errAxisName  label of the axis indicated by <code>iErrDim</code>
     * @return   new coord set
     */
    public static CartesianErrorCoordSet
            createSingleAxisErrorCoordSet( int ndim, int iErrDim,
                                           String errAxisName ) {
        String[] axisNames = new String[ ndim ];
        axisNames[ iErrDim ] = errAxisName;
        int[] iErrDims = new int[] { iErrDim };
        return new CartesianErrorCoordSet( axisNames, iErrDims );
    }
}
