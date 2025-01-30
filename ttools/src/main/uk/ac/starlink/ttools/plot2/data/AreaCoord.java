package uk.ac.starlink.ttools.plot2.data;

import java.util.function.Function;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.Area;
import uk.ac.starlink.ttools.AreaDomain;
import uk.ac.starlink.ttools.AreaMapper;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SphereDataGeom;

/**
 * Coord implementation for Area (shape) values.
 * It can currently work with shapes specified as strings using
 * (a subset of) the STC-S syntax referenced by TAP 1.0,
 * and as floating point arrays using the CIRCLE, POLYGON and POINT xtypes
 * defined in DALI 1.1.
 *
 * <p>The serialisation to floating point array is in three parts:
 * <ol>
 * <li>characteristic (typically central) position in data space
 *     (2 elements for plane coords, 3 elements for sky coords)</li>
 * <li>type code (1 element, equal to an integer)</li>
 * <li>data array giving area details
 *     (variable length, interpretation dependent on type code)</li>
 * </ol>
 * The point of storing the characteristic position is so that that
 * position can be extracted for each row, for use in a point cloud
 * to benefit from the standard machinery for auto-ranging, selection
 * by region etc.  Note that to decode this position information,
 * a DataGeom supplied from the {@link #getAreaDataGeom} method must be used.
 *
 * @author   Mark Taylor
 * @since    27 Mar 2020
 * @see  <a href="http://www.ivoa.net/documents/DALI/20170517/"
 *          >DALI 1.1 section 3.3</a>
 * @see  <a href="http://www.ivoa.net/documents/TAP/20100327/"
 *          >TAP 1.0 section 6</a>
 */
public abstract class AreaCoord<DG extends DataGeom> implements Coord {

    private final Input input_;
    private final boolean isRequired_;
    private final int nDataDim_;
    private final boolean allowPoint_;

    private static final InputMeta META;
    static {
        META = new InputMeta( "area", "Area" );
        META.setShortDescription( "Specifies a 2D plot region" );
        META.setXmlDescription( new String[] {
            "<p>Expression giving the geometry of a 2D region on the plot.",
            "It may be a string- or array-valued expression,",
            "and its interpretation depends on the value of the corresponding",
            "<code>" + META.getShortName() + "type</code> parameter.",
            "</p>",
        } );
    }

    /** Instance for use with Plane plot type. */
    public static final AreaCoord<PlaneDataGeom> PLANE_COORD =
        createPlaneCoord( META, true );

    /** Instance for use with Sky plot type. */
    public static final AreaCoord<SkyDataGeom> SKY_COORD =
        createSkyCoord( META, true );

    /** Instance for use with Sphere plot type. */
    public static final AreaCoord<SphereDataGeom> SPHERE_COORD =
        createSphereCoord( META, true );

    private static final double[] NO_AREA = new double[ 0 ];

    /**
     * Constructor.
     *
     * @param   meta   coordinate user metadata
     * @param   isRequired   true if coordinate must be present
     * @param   nDataDim   dimensionality of data coordinate space
     */
    private AreaCoord( InputMeta meta, boolean isRequired, int nDataDim ) {
        input_ = new Input( meta, AreaDomain.INSTANCE );
        isRequired_ = isRequired;
        nDataDim_ = nDataDim;
        allowPoint_ = true;
    }

    /**
     * Writes the position in data coordinates of the characteristic
     * (typically central) point of a given area object into the start
     * of a supplied array.
     *
     * @param  area  area object
     * @param dpos  coordinate array into which characteristic position
     *              data coords are written
     */
    protected abstract void writeDataPos( Area area, double[] dpos );

    /**
     * Returns a DataGeom that can be used to read position data from 
     * objects serialized by this coordinate.
     * The returned DataGeom instance is based on a given instance;
     * it may inherit some behaviour (for instance coordinate rotation
     * in case of a SkyDataGeom).
     *
     * @param  baseGeom   DataGeom instance providing context behaviour
     * @return  data geom
     */
    public abstract DG getAreaDataGeom( DG baseGeom );

    public Input[] getInputs() {
        return new Input[] { input_ };
    }

    public StorageType getStorageType() {
        return StorageType.DOUBLE_ARRAY;
    }

    public boolean isRequired() {
        return isRequired_;
    }

    public Function<Object[],double[]> inputStorage( ValueInfo[] infos,
                                                     DomainMapper[] dms ) {
        if ( dms[ 0 ] instanceof AreaMapper ) {
            AreaMapper mapper = (AreaMapper) dms[ 0 ];
            Class<?> clazz = infos[ 0 ].getContentClass();
            Function<Object,Area> areaFunc = mapper.areaFunction( clazz );
            return areaFunc == null
                 ? values -> NO_AREA
                 : values -> areaStorage( areaFunc.apply( values[ 0 ] ) );
        }
        else {
            assert false : dms[ 0 ];
            return null;
        }
    }

    /**
     * Reads an Area value from an appropriate field in a given Tuple.
     *
     * @param  tuple  tuple
     * @return  icol   index of column in tuple corresponding to this coord
     */
    public Area readAreaCoord( Tuple tuple, int icol ) {
        double[] array = (double[]) tuple.getObjectValue( icol );
        int na = array.length;
        if ( na == 0 ) {
            return null;
        }
        else {
            assert array.length > nDataDim_
                && array[ nDataDim_ ] == (int) array[ nDataDim_ ];
            Area.Type type = Area.Type.fromInt( (int) array[ nDataDim_ ] );
            assert type != null;
            if ( type != null ) {
                int nd = na - nDataDim_ - 1;
                double[] areaData = new double[ nd ];
                System.arraycopy( array, nDataDim_ + 1, areaData, 0, nd );
                return new Area( type, areaData );
            }
            else {
                return null;
            }
        }
    }

    /**
     * Serializes a given shape to a numeric array containing all the
     * information about it, ready for writing to storage.
     *
     * @param   area   shape object
     * @return   serialization of area as a numeric array (not null)
     */
    private double[] areaStorage( Area area ) {
        if ( area == null ) {
            return NO_AREA;
        }
        else {
            double[] areaData = area.getDataArray();
            int nd = areaData.length;
            double[] storage = new double[ nDataDim_ + 1 + nd ];
            writeDataPos( area, storage );
            storage[ nDataDim_ ] = area.getType().ordinal();
            System.arraycopy( areaData, 0, storage, nDataDim_ + 1, nd );
            return storage;
        }
    }

    /**
     * Constructs a custom AreaCoord instance for use with Plane plot type.
     *
     * @param  meta  user coordinate metadata
     * @param  isRequired   true iff this coordinate is required for plot
     * @return  new instance
     */
    public static AreaCoord<PlaneDataGeom>
            createPlaneCoord( InputMeta meta, boolean isRequired ) {
        return new AreaCoord<PlaneDataGeom>( meta, isRequired, 2 ) {
            private final PlaneDataGeom geom_ = new AreaPlaneDataGeom( this );
            protected void writeDataPos( Area area, double[] dpos ) {
                area.writePlaneCoords2( dpos );
            }
            public PlaneDataGeom getAreaDataGeom( PlaneDataGeom baseGeom ) {
                return geom_;
            }
        };
    }

    /**
     * Constructs a custom AreaCoord instance for use with Sky plot type.
     *
     * @param  meta  user coordinate metadata
     * @param  isRequired   true iff this coordinate is required for plot
     * @return  new instance
     */
    public static AreaCoord<SkyDataGeom>
            createSkyCoord( InputMeta meta, boolean isRequired ) {
        return new AreaCoord<SkyDataGeom>( meta, isRequired, 3 ) {
            protected void writeDataPos( Area area, double[] dpos ) {
                area.writeSkyCoords3( dpos );
            }
            public SkyDataGeom getAreaDataGeom( SkyDataGeom baseGeom ) {
                return new AreaSkyDataGeom( this, baseGeom );
            }
        };
    }

    /**
     * Constructs a custom AreaCoord instance for use with Sphere plot type.
     * Note this assumes that the supplied Tuples have the radial coordinate
     * directly after the area coordinate.
     *
     * @param  meta  user coordinate metadata
     * @param  isRequired   true iff this coordinate is required for plot
     * @return  new instance
     */
    public static AreaCoord<SphereDataGeom>
            createSphereCoord( InputMeta meta, boolean isRequired ) {
        return new AreaCoord<SphereDataGeom>( meta, isRequired, 3 ) {
            private final SphereDataGeom geom_ = new AreaSphereDataGeom( this );
            protected void writeDataPos( Area area, double[] dpos ) {
                area.writeSkyCoords3( dpos );
            }
            public SphereDataGeom getAreaDataGeom( SphereDataGeom baseGeom ) {
                return geom_;
            }
        };
    }

    /**
     * DataGeom for use with a planar AreaCoord.
     */
    private static class AreaPlaneDataGeom extends PlaneDataGeom {
        private final AreaCoord<PlaneDataGeom> areaCoord_;

        /**
         * Constructor.
         *
         * @param  areaCoord  coord
         */
        AreaPlaneDataGeom( AreaCoord<PlaneDataGeom> areaCoord ) {
            areaCoord_ = areaCoord;
        }
        @Override
        public Coord[] getPosCoords() {
            return new Coord[] { areaCoord_ };
        }
        @Override
        public boolean readDataPos( Tuple tuple, int ic, double[] dpos ) {
            double[] array = (double[]) tuple.getObjectValue( ic );
            if ( array.length == 0 ) {
                return false;
            }
            else {
                dpos[ 0 ] = array[ 0 ];
                dpos[ 1 ] = array[ 1 ];
                return true;
            }
        }
        @Override
        public int hashCode() {
            return areaCoord_.hashCode();
        }
        @Override
        public boolean equals( Object o ) {
            if ( o instanceof AreaPlaneDataGeom ) {
                AreaPlaneDataGeom other = (AreaPlaneDataGeom) o;
                return this.areaCoord_.equals( other.areaCoord_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * DataGeom for use with a sky AreaCoord.
     */
    private static class AreaSkyDataGeom extends SkyDataGeom {
        final AreaCoord<SkyDataGeom> areaCoord_;
        final SkyDataGeom baseGeom_;

        /**
         * Constructor.
         *
         * @param  areaCoord  coord
         * @param  baseGeom   DataGeom instance supplying rotation
         */
        AreaSkyDataGeom( AreaCoord<SkyDataGeom> areaCoord,
                         SkyDataGeom baseGeom ) {
            super( baseGeom.getVariantName(), baseGeom.getViewSystem() );
            areaCoord_ = areaCoord;
            baseGeom_ = baseGeom;
        }
        @Override
        public Coord[] getPosCoords() {
            return new Coord[] { areaCoord_ };
        }
        @Override
        public boolean readDataPos( Tuple tuple, int ic, double[] dpos ) {
            double[] array = (double[]) tuple.getObjectValue( ic );
            if ( array.length == 0 ) {
                return false;
            }
            else {
                dpos[ 0 ] = array[ 0 ];
                dpos[ 1 ] = array[ 1 ];
                dpos[ 2 ] = array[ 2 ];
                baseGeom_.rotate( dpos );
                return true;
            }
        }
        public void rotate( double[] dpos ) {
            baseGeom_.rotate( dpos );
        }
        public void unrotate( double[] dpos ) {
            baseGeom_.unrotate( dpos );
        }
        @Override
        public int hashCode() {
            int code = 442023;
            code = 23 * code + areaCoord_.hashCode();
            code = 23 * code + baseGeom_.hashCode();
            return code;
        }
        @Override
        public boolean equals( Object o ) {
            if ( o instanceof AreaSkyDataGeom ) {
                AreaSkyDataGeom other = (AreaSkyDataGeom) o;
                return this.areaCoord_.equals( other.areaCoord_ )
                    && this.baseGeom_.equals( other.baseGeom_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * DataGeom for use with a sphere AreaCoord.
     * Note this assumes that a radial coordinate comes directly after
     * the area coordinate in supplied tuples.
     */
    private static class AreaSphereDataGeom extends SphereDataGeom {
        private final AreaCoord<SphereDataGeom> areaCoord_;

        /**
         * Constructor.
         *
         * @param  areaCoord  coord
         */
        AreaSphereDataGeom( AreaCoord<SphereDataGeom> areaCoord ) {
            areaCoord_ = areaCoord;
        }
        @Override
        public Coord[] getPosCoords() {
            return new Coord[] { areaCoord_ };
        }
        @Override
        public boolean readDataPos( Tuple tuple, int ic, double[] dpos ) {
            double[] array = (double[]) tuple.getObjectValue( ic );
            if ( array.length == 0 ) {
                return false;
            }
            else {
                double r = tuple.getDoubleValue( ic + 1 );
                if ( Double.isNaN( r ) ) {
                    dpos[ 0 ] = array[ 0 ];
                    dpos[ 1 ] = array[ 1 ];
                    dpos[ 2 ] = array[ 2 ];
                }
                else {
                    dpos[ 0 ] = r * array[ 0 ];
                    dpos[ 1 ] = r * array[ 1 ];
                    dpos[ 2 ] = r * array[ 2 ];
                }
                return true;
            }
        }
        @Override
        public int hashCode() {
            return areaCoord_.hashCode();
        }
        @Override
        public boolean equals( Object o ) {
            if ( o instanceof AreaSphereDataGeom ) {
                AreaSphereDataGeom other = (AreaSphereDataGeom) o;
                return this.areaCoord_.equals( other.areaCoord_ );
            }
            else {
                return false;
            }
        }
    }
}
