package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.SkyCoord;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * Defines positional data coordinates used by a sky plot.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public abstract class SkyDataGeom implements DataGeom {

    private final String variantName_;
    private final SkySys viewSys_;

    private static final SkyCoord SKY_COORD =
        SkyCoord.createCoord( SkyCoord.SkyVariant.SURFACE, true );

    /** Instance which converts between unspecified, but identical, systems. */
    public static final SkyDataGeom GENERIC = createGeom( null, null );

    /**
     * Constructor.
     *
     * @param  variantName  name for this data geom
     * @param  viewSys   nominal sky coordinate system of view;
     *                   if unknown or not applicable, may be null
     */
    protected SkyDataGeom( String variantName, SkySys viewSys ) {
        variantName_ = variantName;
        viewSys_ = viewSys;
    }

    /**
     * Returns 3.
     */
    public int getDataDimCount() {
        return 3;
    }

    public String getVariantName() {
        return variantName_;
    }

    public Coord[] getPosCoords() {
        return new Coord[] { SKY_COORD };
    }

    public boolean readDataPos( Tuple tuple, int ic, double[] dpos ) {
        return SKY_COORD.readSkyCoord( tuple, ic, dpos );
    }

    /**
     * Rotates a 3-vector in place from this geom's data coordinate system
     * to its view coordinate system.
     *
     * @param  dpos  (x,y,z) vector to be rotated in place
     */
    public abstract void rotate( double[] dpos );

    /**
     * Rotates a 3-vector in place from this geom's view coordinate system
     * to its data coordinate system.
     *
     * @param  dpos  (x,y,z) vector to be rotated in place
     */
    public abstract void unrotate( double[] dpos );

    /**
     * Returns the nominal sky coordinate system of this geom's view,
     * if available.
     *
     * @return   nominal sky view system, or null if not known or applicable
     */
    public SkySys getViewSystem() {
        return viewSys_;
    }

    public abstract int hashCode();

    public abstract boolean equals( Object other );

    /**
     * Creates a data geom with input user data in one sky system and
     * output data coordinates in another sky system.
     *
     * @param   userSys  sky system in which user data is supplied
     * @param   viewSys  sky system in which the plot is viewed
     * @return   new data geom
     */
    public static SkyDataGeom createGeom( SkySys userSys, SkySys viewSys ) {
        if ( userSys == null || viewSys == null ) {
            return new UnitSkyDataGeom( "generic", null );
        }
        else if ( userSys == viewSys ) {
            return new UnitSkyDataGeom( userSys.toString(), viewSys );
        }
        else {
            return new RotateSkyDataGeom( userSys + "-" + viewSys, viewSys,
                                          Rotation
                                         .createRotation( userSys, viewSys ) );
        }
    }

    /**
     * SkyDataGeom implementation with no transformation of coordinates.
     */
    private static class UnitSkyDataGeom extends SkyDataGeom {

        /**
         * Constructor.
         *
         * @param  variantName  name for this data geom
         * @param  viewSys  nominal view coordinate system, or null
         */
        UnitSkyDataGeom( String variantName, SkySys viewSys ) {
            super( variantName, viewSys );
        }

        public void rotate( double[] dpos ) {
        }

        public void unrotate( double[] dpos ) {
        }

        @Override
        public int hashCode() {
            int code = UnitSkyDataGeom.class.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof UnitSkyDataGeom ) {
                UnitSkyDataGeom other = (UnitSkyDataGeom) o;
                return true;
            }
            else {
                return false;
            }
        }
    }

    /**
     * SkyDataGeom implementation that performs a rotation
     * from user to data coords.
     */
    private static class RotateSkyDataGeom extends SkyDataGeom {
        private final Rotation rotation_;
        private final Rotation inverseRotation_;

        /**
         * Constructor.
         *
         * @param  variantName  name for this data geom
         * @param  viewSys  nominal view coordinate system, or null
         * @param  rotation  sky rotation
         */
        RotateSkyDataGeom( String variantName, SkySys viewSys,
                           Rotation rotation ) {
            super( variantName, viewSys );
            rotation_ = rotation;
            inverseRotation_ = rotation.invert();
        }

        public boolean readDataPos( Tuple tuple, int ic, double[] dpos ) {
            if ( super.readDataPos( tuple, ic, dpos ) ) {
                rotation_.rotate( dpos );
                return true;
            }
            else {
                return false;
            }
        }

        public void rotate( double[] dpos ) {
            rotation_.rotate( dpos );
        }

        public void unrotate( double[] dpos ) {
            inverseRotation_.rotate( dpos );
        }

        @Override
        public int hashCode() {
            int code = 55442;
            code = 23 * code + rotation_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof RotateSkyDataGeom ) {
                RotateSkyDataGeom other = (RotateSkyDataGeom) o;
                return this.rotation_.equals( other.rotation_ );
            }
            else {
                return false;
            }
        }
    }
}
