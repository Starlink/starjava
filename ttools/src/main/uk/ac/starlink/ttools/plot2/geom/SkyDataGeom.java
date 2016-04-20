package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.SkyCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Defines positional data coordinates used by a sky plot.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public abstract class SkyDataGeom implements DataGeom {

    private final String variantName_;

    private static final SkyCoord SKY_COORD =
        SkyCoord.createCoord( SkyCoord.SkyVariant.SURFACE, true );

    /** Instance which converts between unspecified, but identical, systems. */
    public static final SkyDataGeom GENERIC = createGeom( null, null );

    /**
     * Constructor.
     *
     * @param  variantName  name for this data geom
     */
    protected SkyDataGeom( String variantName ) {
        variantName_ = variantName;
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
        return variantName_;
    }

    public Coord[] getPosCoords() {
        return new Coord[] { SKY_COORD };
    }

    public boolean readDataPos( TupleSequence tseq, int ic, double[] dpos ) {
        return SKY_COORD.readSkyCoord( tseq, ic, dpos );
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
            return new UnitSkyDataGeom( "generic" );
        }
        else if ( userSys == viewSys ) {
            return new UnitSkyDataGeom( userSys.toString() );
        }
        else {
            return new RotateSkyDataGeom( userSys + "-" + viewSys,
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
         */
        UnitSkyDataGeom( String variantName ) {
            super( variantName );
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

        /**
         * Constructor.
         *
         * @param  variantName  name for this data geom
         * @param  rotation  sky rotation
         */
        RotateSkyDataGeom( String variantName, Rotation rotation ) {
            super( variantName );
            rotation_ = rotation;
        }

        public boolean readDataPos( TupleSequence tseq, int ic,
                                    double[] dpos ) {
            if ( super.readDataPos( tseq, ic, dpos ) ) {
                rotation_.rotate( dpos );
                return true;
            }
            else {
                return false;
            }
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
