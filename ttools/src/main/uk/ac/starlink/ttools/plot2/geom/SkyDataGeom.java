package uk.ac.starlink.ttools.plot2.geom;

import java.util.Arrays;
import uk.ac.starlink.ttools.plot.Matrices;
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
            double[] rotmat =
                Matrices.mmMult( Matrices.invert( viewSys.toEquatorial() ),
                                 userSys.toEquatorial() );
            return new RotateSkyDataGeom( userSys + "-" + viewSys, rotmat );
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
        private final double[] rotmat_;
        private final double r0_;
        private final double r1_;
        private final double r2_;
        private final double r3_;
        private final double r4_;
        private final double r5_;
        private final double r6_;
        private final double r7_;
        private final double r8_;

        /**
         * Constructor.
         *
         * @param  variantName  name for this data geom
         * @param  rotmat  rotation matrix
         */
        RotateSkyDataGeom( String variantName, double[] rotmat ) {
            super( variantName );
            rotmat_ = rotmat;
            r0_ = rotmat[ 0 ];
            r1_ = rotmat[ 1 ];
            r2_ = rotmat[ 2 ];
            r3_ = rotmat[ 3 ];
            r4_ = rotmat[ 4 ];
            r5_ = rotmat[ 5 ];
            r6_ = rotmat[ 6 ];
            r7_ = rotmat[ 7 ];
            r8_ = rotmat[ 8 ];
        }

        public boolean readDataPos( TupleSequence tseq, int ic,
                                    double[] dpos ) {
            if ( super.readDataPos( tseq, ic, dpos ) ) {
                double dx = dpos[ 0 ];
                double dy = dpos[ 1 ];
                double dz = dpos[ 2 ];
                dpos[ 0 ] = r0_ * dx + r1_ * dy + r2_ * dz;
                dpos[ 1 ] = r3_ * dx + r4_ * dy + r5_ * dz;
                dpos[ 2 ] = r6_ * dx + r7_ * dy + r8_ * dz;
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 55442;
            code = 23 * code + Arrays.hashCode( rotmat_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof RotateSkyDataGeom ) {
                RotateSkyDataGeom other = (RotateSkyDataGeom) o;
                return Arrays.equals( this.rotmat_, other.rotmat_ );
            }
            else {
                return false;
            }
        }
    }
}
