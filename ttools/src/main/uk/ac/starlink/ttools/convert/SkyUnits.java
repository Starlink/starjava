package uk.ac.starlink.ttools.convert;

import uk.ac.starlink.ttools.func.CoordsRadians;

/**
 * Class defining units used for a two-coordinate sky coordinate system.
 * Each coordinate represents an angle.
 */
public abstract class SkyUnits {

    /** Degrees. */
    public static final SkyUnits DEGREES = new DegreesUnits( "degrees" );

    /** Radians. */
    public static final SkyUnits RADIANS = new RadiansUnits( "radians" );

    /** Sexagesimal (hh:mm:ss.s, dd:mm:ss.s). */
    public static final SkyUnits SEXAGESIMAL = 
        new SexagesimalUnits( "sexagesimal", 1, 2 );

    /** List of all the available instances of this class. */
    private static final SkyUnits[] KNOWN_UNITS = {
        DEGREES, RADIANS, SEXAGESIMAL,
    };

    /** Default unit (DEGREES). */
    public static final SkyUnits DEFAULT_UNIT = KNOWN_UNITS[ 0 ];

    private final String name_;

    /**
     * Constructor.
     */
    protected SkyUnits( String name ) {
        name_ = name;
    }

    /**
     * Takes a pair of objects representing coordinates in these units,
     * and converts them to radians.
     *
     * @param   c1  first input coordinate, in these units
     * @param   c2  second input coordinate, in these units
     * @return  array of two output coordinates, in radians
     */
    public abstract double[] decode( Object c1, Object c2 );

    /**
     * Takes a pair of coordinates in radians and converts them to
     * these units.
     *
     * @param   c1  first input coordinate, in radians
     * @param   c2  second input coordinate, in radians
     * @return  array of two output coordinates, in these units
     */
    public abstract Object[] encode( double c1, double c2 );

    /**
     * Returns a pair of descriptions for the units of the first and
     * second coordinates represented by these units.
     * Tries to stick to the VOUnits standard.
     *
     * @return   array of two unit names
     */
    public abstract String[] getUnitStrings();

    /**
     * Returns a pair of classes for the first and second coordinates
     * represented by these units.
     *
     * @return   array of two classes, the types which should be presented
     *           to the <code>decode</code> method and will be returned
     *           from the <code>encode</code> method
     */
    public abstract Class<?>[] getUnitTypes();

    /**
     * Returns the name of this unit system.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    public String toString() {
        return getName();
    }

    /**
     * Returns a list of all the available instances of this class.
     *
     * @return   array of known unit systems
     */
    public static SkyUnits[] getKnownUnits() {
        return KNOWN_UNITS.clone();
    }

    /**
     * Returns a unit as specified by a name.  Name may be abbreviated.
     * Returns a suitable unit or throws an IllegalArgumentException.
     */
    public static SkyUnits getUnitsFor( String unitName ) {
        if ( unitName == null || unitName.length() == 0 ) {
            return DEFAULT_UNIT;
        }
        if ( unitName.matches( "[Ss][Ee][Xx][a-zA-Z]*[0-9]+" ) ) {
            String digits = unitName.replaceAll( "[^0-9]*", "" );
            int ndig = Integer.parseInt( digits );
            int hdp = Math.max( ndig - 1, 0 );
            int ddp = Math.max( ndig, 0 );
            return new SexagesimalUnits( unitName, hdp, ddp );
        }
        String lname = unitName.toLowerCase();
        for ( int i = 0; i < KNOWN_UNITS.length; i++ ) {
            SkyUnits unit = KNOWN_UNITS[ i ];
            if ( unit.getName().toLowerCase().startsWith( lname ) ) {
                return unit;
            }
        }
        StringBuffer sbuf = new StringBuffer( "Unknown unit: " )
            .append( unitName )
            .append( "\nKnown units are: " );
        int nunit = KNOWN_UNITS.length;
        for ( int i = 0; i < nunit; i++ ) {
            sbuf.append( KNOWN_UNITS[ i ] );
            sbuf.append( i < nunit - 1 ? ", " : "." );
        }
        throw new IllegalArgumentException( sbuf.toString() );
    }

    /**
     * Radians.
     */
    private static class RadiansUnits extends SkyUnits {
        RadiansUnits( String name ) {
            super( name );
        }
        public String[] getUnitStrings() {
            return new String[] { "rad", "rad" };
        }
        public Class<?>[] getUnitTypes() {
            return new Class<?>[] { Double.class, Double.class };
        }
        public double[] decode( Object c1, Object c2 ) {
            return new double[] {
                c1 instanceof Number ? ((Number) c1).doubleValue()
                                     : Double.NaN,
                c2 instanceof Number ? ((Number) c2).doubleValue()
                                     : Double.NaN,
            };
        }
        public Object[] encode( double c1, double c2 ) {
            return new Object[] {
                Double.valueOf( c1 ),
                Double.valueOf( c2 ),
            };
        }
    }

    /**
     * Degrees.
     */
    private static class DegreesUnits extends SkyUnits {
        public DegreesUnits( String name ) {
            super( name );
        }
        public String[] getUnitStrings() {
            return new String[] { "deg", "deg" };
        }
        public Class<?>[] getUnitTypes() {
            return new Class<?>[] { Double.class, Double.class };
        }
        public double[] decode( Object c1, Object c2 ) {
            return new double[] {
                c1 instanceof Number 
                    ? Math.toRadians( ((Number) c1).doubleValue() )
                    : Double.NaN,
                c2 instanceof Number
                    ? Math.toRadians( ((Number) c2).doubleValue() )
                    : Double.NaN,
            };
        }
        public Object[] encode( double c1, double c2 ) {
            return new Object[] {
                Double.valueOf( Math.toDegrees( c1 ) ),
                Double.valueOf( Math.toDegrees( c2 ) ),
            };
        }
    }

    /**
     * Sexagesimal.
     */
    private static class SexagesimalUnits extends SkyUnits {
        private final int dPlaces_;
        private final int hPlaces_;
        public SexagesimalUnits( String name, int dPlaces, int hPlaces ) {
            super( name );
            dPlaces_ = dPlaces;
            hPlaces_ = hPlaces;
        }
        public String[] getUnitStrings() {
            return new String[] { "'hms'", "'dms'" };
        }
        public Class<?>[] getUnitTypes() {
            return new Class<?>[] { String.class, String.class };
        }
        public double[] decode( Object c1, Object c2 ) {
            return new double[] {
                c1 instanceof String ? CoordsRadians.hmsToRadians( (String) c1 )
                                     : Double.NaN,
                c2 instanceof String ? CoordsRadians.dmsToRadians( (String) c2 )
                                     : Double.NaN,
            };
        }
        public Object[] encode( double c1, double c2 ) {
            return new Object[] {
                CoordsRadians.radiansToHms( c1, hPlaces_ ),
                CoordsRadians.radiansToDms( c2, dPlaces_ ),
            };
        }
    }
}
