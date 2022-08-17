package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * Defines positional data coordinates used by a 2-D time plot.
 *
 * <p>Note the first coordinate is time, whose values are in seconds since
 * the Unix epoch, as defined by {@link uk.ac.starlink.table.TimeMapper}.
 *
 * <p>This is a singleton class.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2013
 */
public class TimeDataGeom implements DataGeom {

    public static final FloatingCoord T_COORD =
        FloatingCoord.createTimeCoord(
            new InputMeta( "t", "Time" )
           .setShortDescription( "Time coordinate" )
           .setValueUsage( "time" )
        , true );

    public static final FloatingCoord Y_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "y", "Y" )
           .setShortDescription( "Vertical coordinate" )
       , true );

    /** Singleton instance. */
    public static TimeDataGeom INSTANCE = new TimeDataGeom();

    /**
     * Singleton constructor.
     */
    private TimeDataGeom() {
    }

    /**
     * Returns 2.
     */
    public int getDataDimCount() {
        return 2;
    }

    public String getVariantName() {
        return "Time";
    }

    public Coord[] getPosCoords() {
        return new Coord[] { T_COORD, Y_COORD };
    }

    public boolean readDataPos( Tuple tuple, int ic, double[] dpos ) {
        double t = T_COORD.readDoubleCoord( tuple, ic++ );
        double y = Y_COORD.readDoubleCoord( tuple, ic++ );
        if ( Double.isNaN( t ) || Double.isNaN( y ) ) {
            return false;
        }
        else {
            dpos[ 0 ] = t;
            dpos[ 1 ] = y;
            return true;
        }
    }
}
