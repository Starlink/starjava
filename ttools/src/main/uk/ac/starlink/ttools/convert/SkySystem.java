package uk.ac.starlink.ttools.convert;

import uk.ac.starlink.pal.AngleDR;
import uk.ac.starlink.pal.Cartesian;
import uk.ac.starlink.pal.Galactic;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.pal.Spherical;
import uk.ac.starlink.ttools.func.Times;

/**
 * Represents a sky coordinate system.
 *
 * <p>Conversions all go via FK5 J2000.0, I think.  SLALIB (Pal) is
 * used to do the work.
 * In some cases an epoch is considered.  No opportunity is given for
 * using an equinox value.  I am not an expert on sky coordinate systems,
 * and I think there may be subtleties which I'm not addressing
 * accurately here.
 *
 * @author   Mark Taylor
 * @since    30 Aug 2005
 */
public abstract class SkySystem {

    /** All known SkySystem instances. */
    private static final SkySystem[] KNOWN_SYSTEMS = new SkySystem[] {
        new ICRSSystem( "icrs" ),
        new FK5System( "fk5" ),
        new FK4System( "fk4" ),
        new GalSystem( "galactic" ),
        new SuperGalSystem( "supergalactic" ),
        new EclipticSystem( "ecliptic" ),
    };
    private static final Pal PAL = new Pal();

    /** Standard epoch for FK5 system. */
    private static final double FK5_EPOCH = 2000.0;

    /** PI / 2. */
    private static final double PI2 = Math.PI / 2;

    private final String name_;
    private final String description_;
    private final String descrip1_;
    private final String descrip2_;
    private final String colname1_;
    private final String colname2_;

    /**
     * Constructor.
     *
     * @param   name  short system name
     * @param   description   a few words of description
     * @param   descrip1  short description of first coordinate
     * @param   descrip2  short description of second coordinate
     * @param   colname1   label for first coordinate suitable for 
     *                     use as column name
     * @param   colname2   label for second coordinate suitable for
     *                     use as column name
     */
    protected SkySystem( String name, String description, 
                         String descrip1, String descrip2,
                         String colname1, String colname2 ) {
        name_ = name;
        description_ = description;
        descrip1_ = descrip1;
        descrip2_ = descrip2;
        colname1_ = colname1;
        colname2_ = colname2;
    }

    /**
     * Converts from FK5 J2000.0 into this system.
     *
     * @param  c1  right ascension in FK5 (radians)
     * @param  c2  declination in FK5 (radians)
     * @return  2-element array of coordinates in this system (radians)
     */
    public abstract double[] fromFK5( double c1, double c2, double epoch );

    /**
     * Converts to FK5 J2000.0 from this system.
     *
     * @param  c1  first coordinate in this system (radians)
     * @param  c2  second coordinate in this system (radians)
     * @return  2-element array containing (RA, Dec) in FK5 (radians)
     */
    public abstract double[] toFK5( double c1, double c2, double epoch );

    /**
     * Returns the name of this system.
     *
     * @return   system name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short description of this system.
     *
     * @return   system description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns a full description of each coordinate in this system.
     *
     * @return  array of coordinate descriptions
     */
    public String[] getCoordinateDescriptions() {
        return new String[] {
            getDescription() + " " + descrip1_,
            getDescription() + " " + descrip2_,
        };
    }

    /**
     * Returns names of the coordinates in this system.
     *
     * @return  array of coordinate names
     */
    public String[] getCoordinateNames() {
        return new String[] {
            descrip1_,
            descrip2_,
        };
    }

    /**
     * Returns labels suitable for use as column names in this system.
     *
     * @return  array of column names
     */
    public String[] getCoordinateColumnNames() {
        return new String[] {
            colname1_,
            colname2_,
        };
    }

    public String toString() {
        return getDescription();
    }

    /**
     * Returns an array of all the systems known.
     *
     * @return   system array
     */
    public static SkySystem[] getKnownSystems() {
        return (SkySystem[]) KNOWN_SYSTEMS.clone();
    }

    /**
     * Returns a snippet of XML which describes the systems and their
     * meanings.
     *
     * @return   XML description of this class and its instances
     */
    public static String getSystemUsage() {
        StringBuffer sbuf = new StringBuffer( "<ul>" );
        for ( int i = 0; i < KNOWN_SYSTEMS.length; i++ ) {
            SkySystem sys = KNOWN_SYSTEMS[ i ];
            sbuf.append( "<li><code>" )
                .append( sys.getName().toLowerCase() )
                .append( "</code>: " )
                .append( sys.getDescription() )
                .append( " (" )
                .append( sys.descrip1_ )
                .append( ", " )
                .append( sys.descrip2_ )
                .append( ")</li>" )
                .append( '\n' );
        }
        sbuf.append( "</ul>\n" );
        return sbuf.toString();
    }

    /**
     * Returns a system which matches a given string.  Abbreviations may
     * be used.
     * 
     * @param  sysName   name to match
     * @return   a SkySystem, not null
     * @throws   IllegalArgumentException  if sysName isn't a system name
     */
    public static SkySystem getSystemFor( String sysName ) {
        String lname = sysName.toLowerCase();
        SkySystem gotSys = null;
        for ( int i = 0; i < KNOWN_SYSTEMS.length; i++ ) {
            SkySystem iSys = KNOWN_SYSTEMS[ i ];
            if ( iSys.getName().toLowerCase().startsWith( lname ) ) {
                if ( gotSys == null ) {
                    gotSys = iSys;
                }
                else {
                    throw new IllegalArgumentException(
                        "Ambiguous system name: " + sysName );
                }
            }
        }
        if ( gotSys == null ) {
            StringBuffer sbuf = new StringBuffer()
                .append( "Unknown system name: " )
                .append( sysName )
                .append( "\nKnown Systems: " );
            for ( int i = 0; i < KNOWN_SYSTEMS.length; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( KNOWN_SYSTEMS[ i ].getName().toLowerCase() );
            }
            sbuf.append( '.' );
            throw new IllegalArgumentException( sbuf.toString() );
        }
        return gotSys;
    }

    /**
     * J2000.0 FK5 system.  This is the one that all others are converted
     * to/from to perform any-to-any conversion in this class.
     */
    private static class FK5System extends SkySystem {
        public FK5System( String name ) {
            super( name, "FK5 J2000.0", "Right Ascension", "Declination",
                   "RA2000", "DEC2000" );
        }
        public double[] fromFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                AngleDR in = new AngleDR( c1, c2 );
                AngleDR out = PAL.Preces( "FK5", epoch, FK5_EPOCH, in );
                return new double[] { out.getAlpha(), out.getDelta() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
        public double[] toFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                AngleDR in = new AngleDR( c1, c2 );
                AngleDR out = PAL.Preces( "FK5", FK5_EPOCH, epoch, in );
                return new double[] { out.getAlpha(), out.getDelta() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
    }

    /**
     * International Celestial Reference System (as used for Hipparcos).
     */
    private static class ICRSSystem extends SkySystem {
        public ICRSSystem( String name ) {
            super( name, "ICRS (Hipparcos)",
                   "Right Ascension", "Declination", "RA", "DEC" );
        }
        public double[] fromFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                AngleDR in = new AngleDR( c1, c2 );
                AngleDR out = fk5hz( in, epoch );
                return new double[] { out.getAlpha(), out.getDelta() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
        public double[] toFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                AngleDR in = new AngleDR( c1, c2 );
                AngleDR out = hfk5z( in, epoch );
                return new double[] { out.getAlpha(), out.getDelta() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
    }

    /**
     * B1950.0 FK4 system.
     */
    private static class FK4System extends SkySystem {
        public FK4System( String name ) {
            super( name, "FK4 B1950.0", "Right Ascension", "Declination",
                   "RA1950", "DEC1950" );
        }
        public double[] fromFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                AngleDR in = new AngleDR( c1, c2 );
                AngleDR out = PAL.Fk54z( in, epoch ).getAngle();
                return new double[] { out.getAlpha(), out.getDelta() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
        public double[] toFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                AngleDR in = new AngleDR( c1, c2 );
                AngleDR out = PAL.Fk45z( in, epoch );
                return new double[] { out.getAlpha(), out.getDelta() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
    }

    /**
     * Galactic system.
     */
    private static class GalSystem extends SkySystem {
        public GalSystem( String name ) {
            super( name, "IAU 1958 Galactic", "Longitude", "Latitude",
                   "GAL_LONG", "GAL_LAT" );
        }
        public double[] fromFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                AngleDR in = new AngleDR( c1, c2 );
                Galactic out = PAL.Eqgal( in );
                return new double[] { out.getLongitude(), out.getLatitude() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
        public double[] toFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                Galactic in = new Galactic( c1, c2 );
                AngleDR out = PAL.Galeq( in );
                return new double[] { out.getAlpha(), out.getDelta() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
    }

    /**
     * Supergalactic system.
     */
    private static class SuperGalSystem extends SkySystem {
        public SuperGalSystem( String name ) {
            super( name, "de Vaucouleurs Supergalactic", 
                   "Longitude", "Latitude", "SUPERGAL_LONG", "SUPERGAL_LAT" );
        }
        public double[] fromFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                AngleDR in = new AngleDR( c1, c2 );
                Galactic out = PAL.Galsup( PAL.Eqgal( in ) );
                return new double[] { out.getLongitude(), out.getLatitude() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
        public double[] toFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                Galactic in = new Galactic( c1, c2 );
                AngleDR out = PAL.Galeq( PAL.Supgal( in ) );
                return new double[] { out.getAlpha(), out.getDelta() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
    }

    /**
     * Ecliptic system.
     */
    private static class EclipticSystem extends SkySystem {
        public EclipticSystem( String name ) {
            super( name, "Ecliptic", "Longitude", "Latitude",
                   "LONG", "LAT" );
        }
        public double[] fromFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                double mjd = Times.julianToMjd( epoch );
                AngleDR in = new AngleDR( c1, c2 );
                AngleDR out = PAL.Eqecl( in, mjd );
                return new double[] { out.getAlpha(), out.getDelta() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
        public double[] toFK5( double c1, double c2, double epoch ) {
            if ( isLatitude( c2 ) ) {
                double mjd = Times.julianToMjd( epoch );
                AngleDR in = new AngleDR( c1, c2 );
                AngleDR out = PAL.Ecleq( in, mjd );
                return new double[] { out.getAlpha(), out.getDelta() };
            }
            else {
                return new double[] { Double.NaN, Double.NaN };
            }
        }
    }

    /**
     * Returns true only if the given angle is a valid latitude
     * (in range -PI/2..+PI/2).
     *
     * @param   theta  angle in radians
     * @return  true iff theta is in range for a latitude
     */
    private static boolean isLatitude( double theta ) {
        return theta >= - PI2 && theta <= + PI2;
    }

    /**
     * Convert from FK5 to ICRS coordinates.
     * This routine is missing from PAL.
     *
     * @param  r2000  FK5 coordinates
     * @param  bepoch   epoch 
     * @return  ICRS  coordinates
     */
    private static AngleDR fk5hz( AngleDR r2000, double bepoch ) {

        /* This implementation is a java-isation of the source code from
         * the FORTRAN SLALIB routine FK5HZ. */
        final double AS2R = 0.484813681109535994e-5;
        final double EPX = -19.9e-3 * AS2R;
        final double EPY =  -9.1e-3 * AS2R;
        final double EPZ = +22.9e-3 * AS2R;
        final double OMX = -0.30e-3 * AS2R;
        final double OMY = +0.60e-3 * AS2R;
        final double OMZ = +0.70e-3 * AS2R;
        final double[] ORTN = new double[] { EPX, EPY, EPZ };
        double[] p5e = PAL.Dcs2c( r2000 );
        double[][] r5h = PAL.Dav2m( ORTN );
        double t = 2000.0 - bepoch;
        double[] vst = new double[] { OMX * t, OMY * t, OMZ * t };
        double[][] rst = PAL.Dav2m( vst );
        double[] p5 = PAL.Dimxv( rst, p5e );
        double[] ph = PAL.Dmxv( r5h, p5 );
        AngleDR hipp = PAL.Dcc2s( ph );
        hipp.setAlpha( PAL.Dranrm( hipp.getAlpha() ) );
        return hipp;
    }

    /**
     * Convert from ICRS to FK5 coordinates.
     * This routine is missing from PAL.
     *
     * @param  rHipp  ICRS angle
     * @param  bepoch  epoch
     * @return  FK5 coordinates
     */
    private static AngleDR hfk5z( AngleDR rHipp, double bepoch ) {

        /* This implementation is a java-isation of the source code from
         * the FORTRAN SLALIB routine HFK5Z. */
        final double AS2R = 0.484813681109535994e-5;
        final double EPX = -19.9e-3 * AS2R;
        final double EPY =  -9.1e-3 * AS2R;
        final double EPZ = +22.9e-3 * AS2R;
        final double OMX = -0.30e-3 * AS2R;
        final double OMY = +0.60e-3 * AS2R;
        final double OMZ = +0.70e-3 * AS2R;
        final double[] ORTN = new double[] { EPX, EPY, EPZ };
        double[] ph = PAL.Dcs2c( rHipp );
        double[][] r5h = PAL.Dav2m( ORTN );
        double[] s5 = new double[] { OMX, OMY, OMZ };
        double[] sh = PAL.Dmxv( r5h, s5 );
        double t = bepoch - 2000.0;
        double[] vst = new double[] { OMX * t, OMY * t, OMZ * t };
        double[][] rst = PAL.Dav2m( vst );
        double[][] r5ht = PAL.Dmxm( r5h, rst );
        double[] pv5e1 = PAL.Dimxv( r5ht, ph );
        double[] vv = PAL.Dvxv( sh, ph );
        double[] pv5e2 = PAL.Dimxv( r5ht, vv );
        Cartesian pv5e = new Cartesian( pv5e1[ 0 ], pv5e1[ 1 ], pv5e1[ 2 ],
                                        pv5e2[ 0 ], pv5e2[ 1 ], pv5e2[ 2 ] );
        Spherical sph5 = PAL.Dc62s( pv5e );
        double r5 = PAL.Dranrm( sph5.getLong() );
        double d5 = sph5.getLat();
        return new AngleDR( r5, d5 );
    }
}
