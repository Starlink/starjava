package uk.ac.starlink.ttools.convert;

import uk.ac.starlink.pal.AngleDR;
import uk.ac.starlink.pal.Galactic;
import uk.ac.starlink.pal.Pal;
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
        new FK5System( "fk5" ),
        new FK4System( "fk4" ),
        new GalSystem( "galactic" ),
        new SuperGalSystem( "supergalactic" ),
        new EclipticSystem( "ecliptic" ),
    };
    private static final Pal PAL = new Pal();

    /** Standard epoch for FK5 system. */
    private static final double FK5_EPOCH = 2000.0;

    private final String name_;
    private final String description_;
    private final String coord1_;
    private final String coord2_;

    /**
     * Constructor.
     *
     * @param   name  short system name
     * @param   description   a few words of description
     * @param   coord1  description of first coordinate
     * @param   coord2  description of second coordinate
     */
    protected SkySystem( String name, String description, 
                         String coord1, String coord2 ) {
        name_ = name;
        description_ = description;
        coord1_ = coord1;
        coord2_ = coord2;
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
            getDescription() + " " + coord1_,
            getDescription() + " " + coord2_,
        };
    }

    public String toString() {
        return getName();
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
                .append( sys.coord1_ )
                .append( ", " )
                .append( sys.coord2_ )
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
            super( name, "FK5 J2000.0", "Right Ascension", "Declination" );
        }
        public double[] fromFK5( double c1, double c2, double epoch ) {
            AngleDR in = new AngleDR( c1, c2 );
            AngleDR out = PAL.Preces( "FK5", epoch, FK5_EPOCH, in );
            return new double[] { out.getAlpha(), out.getDelta() };
        }
        public double[] toFK5( double c1, double c2, double epoch ) {
            AngleDR in = new AngleDR( c1, c2 );
            AngleDR out = PAL.Preces( "FK5", FK5_EPOCH, epoch, in );
            return new double[] { out.getAlpha(), out.getDelta() };
        }
    }

    /**
     * B1950.0 FK4 system.
     */
    private static class FK4System extends SkySystem {
        public FK4System( String name ) {
            super( name, "FK4 B1950.0", "Right Ascension", "Declination" );
        }
        public double[] fromFK5( double c1, double c2, double epoch ) {
            AngleDR in = new AngleDR( c1, c2 );
            AngleDR out = PAL.Fk54z( in, epoch ).getAngle();
            return new double[] { out.getAlpha(), out.getDelta() };
        }
        public double[] toFK5( double c1, double c2, double epoch ) {
            AngleDR in = new AngleDR( c1, c2 );
            AngleDR out = PAL.Fk45z( in, epoch );
            return new double[] { out.getAlpha(), out.getDelta() };
        }
    }

    /**
     * Galactic system.
     */
    private static class GalSystem extends SkySystem {
        public GalSystem( String name ) {
            super( name, "IAU 1958 Galactic", "Longitude", "Latitude" );
        }
        public double[] fromFK5( double c1, double c2, double epoch ) {
            AngleDR in = new AngleDR( c1, c2 );
            Galactic out = PAL.Eqgal( in );
            return new double[] { out.getLongitude(), out.getLatitude() };
        }
        public double[] toFK5( double c1, double c2, double epoch ) {
            Galactic in = new Galactic( c1, c2 );
            AngleDR out = PAL.Galeq( in );
            return new double[] { out.getAlpha(), out.getDelta() };
        }
    }

    /**
     * Supergalactic system.
     */
    private static class SuperGalSystem extends SkySystem {
        public SuperGalSystem( String name ) {
            super( name, "de Vaucouleurs Supergalactic", 
                   "Longitude", "Latitude" );
        }
        public double[] fromFK5( double c1, double c2, double epoch ) {
            AngleDR in = new AngleDR( c1, c2 );
            Galactic out = PAL.Galsup( PAL.Eqgal( in ) );
            return new double[] { out.getLongitude(), out.getLatitude() };
        }
        public double[] toFK5( double c1, double c2, double epoch ) {
            Galactic in = new Galactic( c1, c2 );
            AngleDR out = PAL.Galeq( PAL.Supgal( in ) );
            return new double[] { out.getAlpha(), out.getDelta() };
        }
    }

    /**
     * Ecliptic system.
     */
    private static class EclipticSystem extends SkySystem {
        public EclipticSystem( String name ) {
            super( name, "Ecliptic", "Longitude", "Latitude" );
        }
        public double[] fromFK5( double c1, double c2, double epoch ) {
            double mjd = Times.julianToMjd( epoch );
            AngleDR in = new AngleDR( c1, c2 );
            AngleDR out = PAL.Eqecl( in, mjd );
            return new double[] { out.getAlpha(), out.getDelta() };
        }
        public double[] toFK5( double c1, double c2, double epoch ) {
            double mjd = Times.julianToMjd( epoch );
            AngleDR in = new AngleDR( c1, c2 );
            AngleDR out = PAL.Ecleq( in, mjd );
            return new double[] { out.getAlpha(), out.getDelta() };
        }
    }
}
