package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * DataGeom for working with 3-d space that uses a 3-vector as its
 * input.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2024
 */
public class CubeVectorDataGeom implements DataGeom {

    /** 3-vector coordinate. */
    public static final FloatingArrayCoord XYZ_COORD = createXyzCoord();

    /** Singleton instance. */
    public static CubeVectorDataGeom INSTANCE = new CubeVectorDataGeom();

    /** Vector coordinate. */
    private static FloatingArrayCoord createXyzCoord() {
        InputMeta meta = new InputMeta( "xyz", "XYZ Vector" );
        meta.setShortDescription( "3-element Cartesian component array" );
        meta.setValueUsage( "array" );
        meta.setXmlDescription( new String[] {
            "<p>3-element array giving the X, Y and Z components",
            "of a point in 3-d space.",
            "If an array longer than 3 elements is supplied,",
            "the extra elements are ignored.",
            "</p>",
        } );
        return FloatingArrayCoord.createCoord( meta, true );
    }

    /**
     * Private singleton constructor.
     */
    private CubeVectorDataGeom() {
    }

    /**
     * Returns 3.
     */
    public int getDataDimCount() {
        return 3;
    }

    public String getVariantName() {
        return "Vector";
    }

    public Coord[] getPosCoords() {
        return new Coord[] { XYZ_COORD };
    }

    public boolean readDataPos( Tuple tuple, int ic, double[] dpos ) {
        double[] array = XYZ_COORD.readArrayCoord( tuple, ic ); 
        if ( array != null && array.length >= 3 ) {
            double x = array[ 0 ];
            double y = array[ 1 ];
            double z = array[ 2 ];
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
        else {
            return false;
        }
    }
}
