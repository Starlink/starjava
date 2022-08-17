package uk.ac.starlink.ttools.plot2.geom;

import cds.healpix.Healpix;
import cds.healpix.HealpixNested;
import cds.healpix.VerticesAndPathComputer;
import uk.ac.starlink.ttools.plot2.CdsHealpixUtil;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.LongCoord;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * DataGeom implementation for HEALPix indices.
 * This can convert from a single integer to a sky position,
 * the center of the corresponding HEALPix cell.
 * It has to be parameterised by the HEALPix level, and optionally
 * data/view sky system information.
 *
 * <p>In general you should use the provided factory methods to
 * instantiate this abstract class.  If you want to implement a
 * concrete subclass, make sure you implement the methods requred
 * {@link uk.ac.starlink.ttools.plot2.Equality}.
 * 
 * @author   Mark Taylor
 * @since    21 Dec 2018
 */
public abstract class HealpixDataGeom implements DataGeom {

    /** Coordinate for HEALPix pixel index. */
    public static final LongCoord HEALPIX_COORD =
       new LongCoord(
            new InputMeta( "healpix", "HEALPix index" )
           .setShortDescription( "HEALPix index" )
           .setXmlDescription( new String[] {
                "<p>HEALPix index indicating the sky position of the tile",
                "whose value is plotted.",
                "If not supplied, the assumption is that the supplied table",
                "contains one row for each HEALPix tile at a given level,",
                "in ascending order.",
                "</p>",
            } )
        , false );

    /**
     * Placeholder instance.
     * This can be used where an instance of this class is required
     * for documentation purposes, but it is not
     * capable of actual transformations.
     */
    public static final HealpixDataGeom DUMMY_INSTANCE =
        new UnitHealpixDataGeom( "dummy", -1, true );

    private final String variantName_;
    private final boolean isNest_;
    private final int level_;
    private final VerticesAndPathComputer vpc_;
    private final HealpixNested ringer_;
    private final long nsky_;

    /**
     * Constructor.
     *
     * @param  variantName  datageom name
     * @param  level   healpix level (log2(nside))
     * @param  isNest  true for nested, false for ring
     */
    private HealpixDataGeom( String variantName, int level, boolean isNest ) {
        variantName_ = variantName;
        level_ = level;
        isNest_ = isNest;
        vpc_ = level >= 0 ? Healpix.getNestedFast( level ) : null;
        ringer_ = level >= 0 ? Healpix.getNested( level ) : null;
        nsky_ = 12L << 2 * level;
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
        return new Coord[] { HEALPIX_COORD };
    }

    public boolean readDataPos( Tuple tuple, int icol, double[] dpos ) {

        /* Missing values are returned from tuple.getLongValue as
         * Long.MIN_VALUE.  That's good, since negative values cannot be
         * HEALPix indices, so we can ignore them without ambiguity. */
        long ipix = tuple.getLongValue( icol );
        if ( ipix >= 0 && ipix < nsky_ ) {
            if ( ! isNest_ ) {
                ipix = ringer_.toNested( ipix );
            }

            /* Use the supplied dpos array as workspace to avoid
             * allocating a new one. */
            vpc_.center( ipix, dpos );
            double lonRad = dpos[ 0 ];
            double latRad = dpos[ 1 ];
            CdsHealpixUtil.lonlatToVector( lonRad, latRad, dpos );
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof HealpixDataGeom ) {
            HealpixDataGeom other = (HealpixDataGeom) o;
            return this.level_ == other.level_
                && this.isNest_ == other.isNest_;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 9932;
        code = 23 * code + level_;
        code = 23 * code + ( isNest_ ? 11 : 29 );
        return code;
    }

    /**
     * Creates a data geom with input user data in one sky system and
     * output data coordinates in another sky system.
     * If both of the supplied sky systems are supplied, the corresponding
     * rotation will be applied; if one or zero are supplied, these parameters
     * are ignored.
     *
     * @param  level   healpix level (log2(nside))
     * @param   userSys  sky system in which user data is supplied, may be null
     * @param   viewSys  sky system in which the plot is viewed, may be null
     * @return   new data geom
     */
    public static HealpixDataGeom createGeom( int level, boolean isNest,
                                              SkySys userSys, SkySys viewSys ) {
        if ( userSys == null || viewSys == null ) {
            return new UnitHealpixDataGeom( "hpx:generic", level, isNest );
        }
        else if ( userSys == viewSys ) {
            return new UnitHealpixDataGeom( "hpx:" + userSys, level, isNest );
        }
        else {
            return new RotateHealpixDataGeom( "hpx:" + userSys + "-" + viewSys,
                                              level, isNest,
                                              Rotation
                                             .createRotation( userSys,
                                                              viewSys ) );
        }
    }

    /**
     * HealpixDataGeom concrete subclass with no rotation.
     */
    private static final class UnitHealpixDataGeom extends HealpixDataGeom {

        /**
         * Constructor.
         *
         * @param  name  datageom name
         * @param  level   healpix level (log2(nside))
         * @param  isNest  true for nested, false for ring
         */
        UnitHealpixDataGeom( String name, int level, boolean isNest ) {
            super( name, level, isNest );
        }

        @Override
        public boolean equals( Object o ) {
            return o instanceof UnitHealpixDataGeom
                && super.equals( o );
        }
    }

    /**
     * HealpixDataGeom concrete subclass with rotation.
     */
    private static class RotateHealpixDataGeom extends HealpixDataGeom {

        private final Rotation rotation_;

        /**
         * Constructor.
         *
         * @param  name  datageom name
         * @param  level   healpix level (log2(nside))
         * @param  isNest  true for nested, false for ring
         * @param  rotation  rotation matrix
         */
        RotateHealpixDataGeom( String name, int level, boolean isNest,
                               Rotation rotation ) {
            super( name, level, isNest );
            rotation_ = rotation;
        }

        @Override
        public boolean readDataPos( Tuple tuple, int icol, double[] dpos ) {
            if ( super.readDataPos( tuple, icol, dpos ) ) {
                rotation_.rotate( dpos );
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof RotateHealpixDataGeom ) {
                RotateHealpixDataGeom other = (RotateHealpixDataGeom) o;
                return super.equals( other )
                    && this.rotation_.equals( other.rotation_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = super.hashCode();
            code = 23 * code + rotation_.hashCode();
            return code;
        }
    }
}
