package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.SkyCoord;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * Defines positional data coordinates used by an isotropic spherical
 * polar 3-D plot.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public abstract class SphereDataGeom implements DataGeom {

    private static final SkyCoord SPHERE_COORD =
        SkyCoord.createCoord( SkyCoord.SkyVariant.VOLUME_OR_NULL, true );

    /** Standard instance. */
    public static final SphereDataGeom INSTANCE = new SphereDataGeom() {};

    /**
     * Constructor.
     */
    protected SphereDataGeom() {
    }

    /**
     * Returns 3.
     */
    public int getDataDimCount() {
        return 3;
    }

    public String getVariantName() {
        return "Polar";
    }

    public Coord[] getPosCoords() {
        return new Coord[] { SPHERE_COORD };
    }

    public boolean readDataPos( Tuple tuple, int ic, double[] dpos ) {
        return SPHERE_COORD.readSkyCoord( tuple, ic, dpos );
    }
}
