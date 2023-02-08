package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * MultiPointCoordSet for vectors in Cartesian data coordinates.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public class CartesianVectorCoordSet implements CartesianMultiPointCoordSet {

    private final int ndim_;
    private final FloatingCoord[] componentCoords_;

    /**
     * Constructor.
     *
     * @param  axisNames  names of cartesian axes
     */
    public CartesianVectorCoordSet( String[] axisNames ) {
        ndim_ = axisNames.length;
        componentCoords_ = new FloatingCoord[ ndim_ ];
        for ( int id = 0; id < ndim_; id++ ) {
            String axName = axisNames[ id ];
            String axname = axName.toLowerCase();
            componentCoords_[ id ] =
                FloatingCoord.createCoord(
                    new InputMeta( axname + "delta", axName + " Delta" )
                   .setShortDescription( axName + " component of vector" )
                   .setXmlDescription( new String[] {
                        "<p>Vector component in the " + axName + " direction.",
                        "</p>",
                    } )
                , true );
        }
    }

    public Coord[] getCoords() {
        int nc = componentCoords_.length;
        Coord[] coords = new Coord[ nc ];
        for ( int ic = 0; ic < nc; ic++ ) {
            coords[ ic ] = componentCoords_[ ic ];
        }
        return coords;
    }

    public int getPointCount() {
        return 1;
    }

    public boolean readPoints( Tuple tuple, int icol, double[] xy0,
                               double[][] xyExtras ) {
        double[] xy1 = xyExtras[ 0 ];
        for ( int idim = 0; idim < ndim_; idim++ ) {
            double delta = componentCoords_[ idim ]
                          .readDoubleCoord( tuple, icol + idim );
            if ( Double.isNaN( delta ) ) {
                return false;
            }
            xy1[ idim ] = xy0[ idim ] + delta;
        }
        return true;
    }
}
