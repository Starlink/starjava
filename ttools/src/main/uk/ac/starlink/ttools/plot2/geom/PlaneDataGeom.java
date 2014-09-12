package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Defines positional data coordinates used by a 2-D Cartesian plot.
 * Singleton class.
 *
 * @author   Mark Taylor
 * @since    19 Feb 2013
 */
public class PlaneDataGeom implements DataGeom {

    /** Horizontal coordinate. */
    public static final FloatingCoord X_COORD = FloatingCoord.createCoord(
        new InputMeta( "x", "X" )
       .setShortDescription( "Horizontal coordinate" )
    , true );

    /** Vertical coordinate. */
    public static final FloatingCoord Y_COORD = FloatingCoord.createCoord(
         new InputMeta( "y", "Y" )
        .setShortDescription( "Vertical coordinate" )
    , true );
        

    /** Singleton instance. */
    public static PlaneDataGeom INSTANCE = new PlaneDataGeom();

    /**
     * Singleton constructor.
     */
    private PlaneDataGeom() {
    }

    /**
     * Returns 2.
     */
    public int getDataDimCount() {
        return 2;
    }

    public boolean hasPosition() {
        return true;
    }

    public String getVariantName() {
        return "Cartesian";
    }

    public Coord[] getPosCoords() {
        return new Coord[] { X_COORD, Y_COORD };
    }

    public boolean readDataPos( TupleSequence tseq, int ic, double[] dpos ) {
        double x = X_COORD.readDoubleCoord( tseq, ic++ );
        double y = Y_COORD.readDoubleCoord( tseq, ic++ );
        if ( Double.isNaN( x ) || Double.isNaN( y ) ) {
            return false;
        }
        else {
            dpos[ 0 ] = x;
            dpos[ 1 ] = y;
            return true;
        }
    }
}
