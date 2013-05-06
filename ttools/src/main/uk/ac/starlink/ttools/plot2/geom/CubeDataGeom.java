package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Defines positional data coordinates used by a 3-D Cartesian plot.
 * Singleton class.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public class CubeDataGeom implements DataGeom {

    private static final FloatingCoord X_COORD = posCoord( "X" );
    private static final FloatingCoord Y_COORD = posCoord( "Y" );
    private static final FloatingCoord Z_COORD = posCoord( "Z" );

    /** Singleton instance. */
    public static CubeDataGeom INSTANCE = new CubeDataGeom();

    /**
     * Private singleton constructor.
     */
    private CubeDataGeom() {
    }

    /**
     * Returns 3.
     */
    public int getDataDimCount() {
        return 3;
    }

    public String getVariantName() {
        return "Cartesian";
    }

    public Coord[] getPosCoords() {
        return new Coord[] { X_COORD, Y_COORD, Z_COORD };
    }

    public boolean readDataPos( TupleSequence tseq, int ic, double[] dpos ) {
        double x = X_COORD.readDoubleCoord( tseq, ic++ );
        double y = Y_COORD.readDoubleCoord( tseq, ic++ );
        double z = Z_COORD.readDoubleCoord( tseq, ic++ );
        if ( Double.isNaN( x ) || Double.isNaN( y ) || Double.isNaN( z ) ) {
            return false;
        }
        else {
            dpos[ 0 ] = x;
            dpos[ 1 ] = y;
            dpos[ 2 ] = z;
            return true;
        }
    }

    /**
     * Utility method to create a coordinate for one of the Cartesian axes.
     *
     * @param  axis name
     * @return  coordinate
     */
    private static FloatingCoord posCoord( String axName ) {
        return FloatingCoord
              .createCoord( axName, axName + " coordinate", true );
    }
}
