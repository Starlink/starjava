package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.SkyCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Defines positional data coordinates used by an isotropic spherical
 * polar 3-D plot.
 * Singleton class.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public class SphereDataGeom implements DataGeom {

    private static final SkyCoord SPHERE_COORD =
        SkyCoord.createCoord( SkyCoord.SkyVariant.VOLUME_OR_NULL, true );

    /** Singleton instance. */
    public static final SphereDataGeom INSTANCE = new SphereDataGeom();

    /**
     * Private singleton constructor.
     */
    private SphereDataGeom() {
    }

    /**
     * Returns 3.
     */
    public int getDataDimCount() {
        return 3;
    }

    public boolean hasPosition() {
        return true;
    }

    public String getVariantName() {
        return "Polar";
    }

    public Coord[] getPosCoords() {
        return new Coord[] { SPHERE_COORD };
    }

    public boolean readDataPos( TupleSequence tseq, int ic, double[] dpos ) {
        return SPHERE_COORD.readSkyCoord( tseq, ic, dpos );
    }
}
