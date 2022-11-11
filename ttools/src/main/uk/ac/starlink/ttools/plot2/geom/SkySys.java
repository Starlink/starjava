package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Sky coordinate system definition.
 * These are used to label the system in which sky coordinates are
 * supplied or required by SkyDataGeom so that a transformation can
 * be performed between them if required.
 * It is not an all-singing all-dancing toolkit for sky coordinate
 * system manipulation.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 * @see  SkyDataGeom
 */
public class SkySys {

    private final String name_;
    private final String description_;
    private final String lonName_;
    private final String latName_;
    private final String ucd1pBase_;
    private final String ucd1Base_;
    private final String lonLabel_;
    private final String latLabel_;
    private final String[][] namePairs_;
    private final double[] toEq_;

    /** J2000 Equatorial sky system. */
    public static final SkySys EQUATORIAL;

    /** IAU 1958 Galactic sky system. */
    public static final SkySys GALACTIC;

    /** De Vaucouleurs supergalactic system. */
    public static final SkySys SUPERGALACTIC;

    /** Ecliptic system, based on conversion at data 2000.0.
     *  Probably not respectable. */
    public static final SkySys ECLIPTIC2000;

    /* Coefficient values from Pal.Galeq (originally SLA_GALEQ). */
    private static double[] EQ2GAL = new double[] {
        -0.054875539726, -0.873437108010, -0.483834985808,
         0.494109453312, -0.444829589425,  0.746982251810,
        -0.867666135858, -0.198076386122,  0.455983795705
    };

    /* Coefficient values From Pal.Galsup (originally SLA_GALSUP). */
    private static double[] GAL2SUP = new double[] {
        -0.735742574804,  0.677261296414,  0.0,
        -0.074553778365, -0.080991471307,  0.993922590400,
         0.673145302109,  0.731271165817,  0.110081262225
    };

    /** Available sky coordinate systems. */
    private static final SkySys[] KNOWN_SYSTEMS = new SkySys[] {

        EQUATORIAL =
            new SkySys( "Equatorial", "J2000 equatorial system",
                        "Right Ascension", "Declination",
                        "eq", "EQ", "ra", "dec",
                        new String[][] { { "ra", "dec" },
                                         { "alpha", "delta" },
                                         { "ra2000", "dec2000" },
                                         { "ra2000", "de2000" },
                                         { "_RAJ2000", "_DEJ2000" } },
                        new double[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 } ),

        GALACTIC =
            new SkySys( "Galactic", "IAU 1958 galactic system",
                        "Galactic Longitude", "Galactic Latitude",
                        "galactic", "GAL", "lon", "lat",
                        new String[][] { { "gal_lon", "gal_lat" },
                                         { "gal_lon", "gal_lat" },
                                         { "glon", "glat" },
                                         { "glong", "glat" }, },
                        Matrices.invert( EQ2GAL ) ),

        SUPERGALACTIC =
            new SkySys( "SuperGalactic", "De Vaucouleurs supergalactic system",
                        "Super-Galactic Longitude", "Super-Galactic Latitude",
                        "supergalactic", "SG", "lon", "lat",
                        new String[ 0 ][],
                        Matrices.mmMult( Matrices.invert( EQ2GAL ),
                                         Matrices.invert( GAL2SUP ) ) ),

        ECLIPTIC2000 =
            new SkySys( "Ecliptic",
                        "ecliptic system based on conversion at 2000.0",
                        "Ecliptic Longitude", "Ecliptic Latitude",
                        "ecliptic2000", "EC", "lon", "lat",
                        new String[ 0 ][],
                        Matrices.invert( Matrices
                                        .fromPal( new Pal()
                                                 .Ecmat( 51544.0 ) ) ) ),
    };

    /**
     * Constructor.
     * A number of rules about how to guess columns that supply these values
     * are given.  All such strings are interpreted case-insensitively.
     *
     * @param  name  system name
     * @param  description  short description
     * @param  lonName  user-facing name for longitude coordinate
     * @param  latName  user-facing name for latitude coordinate
     * @param  ucd1pBase  root atom for coordinate UCDs in UCD1+
     * @param  ucd1Base   root atom for coordinate UCDs in UCD1
     * @param  lonLabel  name of longitude coordinate in UCDs
     * @param  latLabel  name of latitude coordinate in UCDs
     * @param  namePairs  array of likely-sounding lon,lat column name pairs
     * @param  toEquatorial  9-element rotation matrix to convert from this
     *                       system to J2000 Equatorial coordinates
     */
    private SkySys( String name, String description,
                    String lonName, String latName,
                    String ucd1pBase, String ucd1Base,
                    String lonLabel, String latLabel, String[][] namePairs,
                    double[] toEquatorial ) {
        name_ = name;
        description_ = description;
        lonName_ = lonName;
        latName_ = latName;
        ucd1pBase_ = ucd1pBase;
        ucd1Base_ = ucd1Base;
        lonLabel_ = lonLabel;
        latLabel_ = latLabel;
        namePairs_ = namePairs;
        toEq_ = toEquatorial;
        if ( toEq_.length != 9 ) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the name for this system.
     *
     * @return  name
     */
    public String getSysName() {
        return name_;
    }

    /**
     * Returns the description for this system.
     *
     * @return  description
     */
    public String getSysDescription() {
        return description_;
    }

    /**
     * Returns the human-readable name for longitude.
     *
     * @return  longitude coordinate name
     */
    public String getLongitudeName() {
        return lonName_;
    }

    /**
     * Returns the human-readable name for latitude.
     *
     * @return  latitude coordinate name
     */
    public String getLatitudeName() {
        return latName_;
    }

    /**
     * Returns a rotation matrix to convert coordinates in this system to
     * J2000 equatorial coordinates.
     *
     * @return  9-element rotation matrix
     */
    public double[] toEquatorial() {
        return toEq_.clone();
    }

    /**
     * Tries to guess at a pair of columns from a given selection that
     * represent the longitude, latitude coordinates in this sky system.
     *
     * @param   infos  array of metadata items for the available columns
     * @return  2-element array giving indexes into <code>infos</code> array
     *          of lon,lat items, or null if attempt fails
     */
    public int[] getCoordPair( ValueInfo[] infos ) {

        /* Assemble arrays of names and UCDs per info. */
        int ninfo = infos.length;
        String[] ucds = new String[ ninfo ];
        String[] names = new String[ ninfo ];
        for ( int i = 0; i < ninfo; i++ ) {
            ucds[ i ] = infos[ i ].getUCD();
            names[ i ] = infos[ i ].getName();
        }

        /* Make successively more desperate attempts to match possible
         * UCD values for lon/lat columns against them. */
        String lonUcd1p = "pos." + ucd1pBase_ + "." + lonLabel_;
        String latUcd1p = "pos." + ucd1pBase_ + "." + latLabel_;
        String lonUcd1 = "POS_" + ucd1Base_ + "_" + lonLabel_.toUpperCase();
        String latUcd1 = "POS_" + ucd1Base_ + "_" + latLabel_.toUpperCase();
        int[] pair = new int[] { -1, -1 };
        if ( getPair( pair, ucds, lonUcd1p + ";meta.main",
                                  latUcd1p + ";meta.main" )
          || getPair( pair, ucds, lonUcd1p, latUcd1p )
          || getPair( pair, ucds, lonUcd1 + "_MAIN", latUcd1 + "_MAIN" )
          || getPair( pair, ucds, lonUcd1, latUcd1 ) ) {
            return pair; 
        }

        /* If that fails try matching pairs of column names. */
        for ( int is = 0; is < namePairs_.length; is++ ) {
            String[] np = namePairs_[ is ];
            if ( getPair( pair, names, np[ 0 ], np[ 1 ] ) ) {
                return pair;
            }
        }

        /* If no luck return null to indicate failure. */
        return null;
    }

    /**
     * Does case-insensitive matching of a pair of a given pair of values
     * against a list of options.  If entries for both are found in the
     * options, the indices at which they were found in the list are
     * written into a supplied array.  Success is indicated by a boolean
     * return value.
     *
     * @param  pair  2-element integer array to receive result as pair of
     *               indices into <code>options</code> array on success
     * @param  options  list of strings against which matches are attempted
     * @param  lonValue  first item in pair
     * @param  latValue  second item in pair
     * @return   true on success (both items in pair found in options list)
     */
    private static boolean getPair( int[] pair, String[] options,
                                    String lonValue, String latValue ) {
        String lonval = lonValue.toLowerCase();
        String latval = latValue.toLowerCase();
        int ilon = -1;
        int ilat = -1;
        for ( int iopt = 0; iopt < options.length; iopt++ ) {
            String option = options[ iopt ];
            if ( option != null ) {
                option = option.toLowerCase();
                if ( lonval.equals( option ) ) {
                    ilon = iopt;
                }
                if ( latval.equals( option ) ) {
                    ilat = iopt;
                }
            }
        }
        if ( ilon >= 0 && ilat >= 0 ) {
            pair[ 0 ] = ilon;
            pair[ 1 ] = ilat;
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns the list of known sky system instances.
     *
     * @param  includeNull  true if null is to appear in the list
     * @return  list of instances
     */
    public static SkySys[] getKnownSystems( boolean includeNull ) {
        return PlotUtil.arrayConcat( includeNull ? new SkySys[] { null }
                                                 : new SkySys[ 0 ],
                                     KNOWN_SYSTEMS );
    }
}
