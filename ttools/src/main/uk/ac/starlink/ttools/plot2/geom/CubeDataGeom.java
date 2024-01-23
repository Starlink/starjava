package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * Defines positional data coordinates used by a 3-D Cartesian plot.
 * Singleton class.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public class CubeDataGeom implements DataGeom {

    /** X coordinate. */
    public static final FloatingCoord X_COORD = posCoord( "X" );

    /** Y coordinate. */
    public static final FloatingCoord Y_COORD = posCoord( "Y" );

    /** Z coordinate. */
    public static final FloatingCoord Z_COORD = posCoord( "Z" );

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
        return "Components";
    }

    public Coord[] getPosCoords() {
        return new Coord[] { X_COORD, Y_COORD, Z_COORD };
    }

    public boolean readDataPos( Tuple tuple, int ic, double[] dpos ) {
        double x = X_COORD.readDoubleCoord( tuple, ic++ );
        double y = Y_COORD.readDoubleCoord( tuple, ic++ );
        double z = Z_COORD.readDoubleCoord( tuple, ic++ );
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
        InputMeta meta =
            new InputMeta( axName.toLowerCase(), axName.toUpperCase() );
        meta.setShortDescription( axName + " coordinate" );
        return FloatingCoord.createCoord( meta, true );
    }
}
