package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FixedLengthVectorCoord;
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
    public static final FixedLengthVectorCoord XYZ_COORD =
        FixedLengthVectorCoord.XYZ_COORD;

    /** Singleton instance. */
    public static CubeVectorDataGeom INSTANCE = new CubeVectorDataGeom();

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
        XYZ_COORD.readElements( tuple, ic, dpos );
        return ! ( Double.isNaN( dpos[ 0 ] ) ||
                   Double.isNaN( dpos[ 1 ] ) ||
                   Double.isNaN( dpos[ 2 ] ) );
    }
}
