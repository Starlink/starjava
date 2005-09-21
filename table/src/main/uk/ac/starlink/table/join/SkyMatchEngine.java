package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Abstract superclass for match engines which match positions on the sky.
 * The tuples it uses are two-element arrays of {@link java.lang.Number}
 * objects, the first giving Right Ascension in radians, and the second
 * giving Declination in radians.
 * The <tt>separation</tt> attribute indicates how many radians may
 * separate two points on the celestial sphere for them to be 
 * considered matching.
 *
 * <p>Concrete superclasses will need to implement at least the
 * {@link MatchEngine#getBins} method.
 *
 * @author   Mark Taylor (Starlink)
 * @since    15 Mar 2005
 */
public abstract class SkyMatchEngine implements MatchEngine {

    private double separation_;
    private DescribedValue sepValue_ = new SkySeparationValue();

    private static final double ARC_SECOND = Math.PI / 180 / 60 / 60;
    private static final DefaultValueInfo SEP_INFO =
        new DefaultValueInfo( "Error", Double.class,
                              "Maximum separation along a great circle" );
    private static final DefaultValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Distance between matched objects " +
                              "along a great circle" );
    static {
        SEP_INFO.setUnitString( "radians" );
        SCORE_INFO.setUnitString( "arcsec" );
    }

    /**
     * Constructs a new match engine which considers two points
     * (RA,Dec tuples) to match if they are within a given angular
     * distance on the celestial sphere.
     *
     * @param   separation   match radius in radians
     */
    public SkyMatchEngine( double separation ) {
        setSeparation( separation );
    }

    /**
     * Configures this match engine to consider two points
     * (RA,Dec tuples) to match if they are within a given angular
     * distance on the celestial sphere.
     *
     * @param   separation   match radius in radians
     */
    public void setSeparation( double separation ) {
        separation_ = separation;
    }

    /**
     * Returns the separation between points within which they will be
     * considered to match.
     *
     * @return   match radius in radians
     */
    public double getSeparation() {
        return separation_;
    }

    /**
     * Matches two tuples representing RA,Dec coordinates if they are
     * within <tt>separation</tt> radians of each other on the sky.
     *
     * @param   radec1  2-element array of Number objects giving RA &amp; dec
     *                  of first point
     * @param   radec2  2-element array of Number objects giving RA &amp; dec
     *                  of second point
     * @return  distance in radians between <tt>radec1</tt> and <tt>radec2</tt>
     *          if they are close enough to match, otherwise -1
     */
    public double matchScore( Object[] radec1, Object[] radec2 ) {

        /* Cheap test which will throw out most comparisons straight away:
         * see if the separation in declination is greater than the maximum
         * acceptable separation. */
        double dec1 = ((Number) radec1[ 1 ]).doubleValue();
        double dec2 = ((Number) radec2[ 1 ]).doubleValue();
        if ( Math.abs( dec1 - dec2 ) > separation_ ) {
            return -1.0;
        }

        /* Declinations at least are close; do a proper test. */
        double ra1 = ((Number) radec1[ 0 ]).doubleValue();
        double ra2 = ((Number) radec2[ 0 ]).doubleValue();
        double sep = calculateSeparation( ra1, dec1, ra2, dec2 );
        return sep <= separation_ ? sep / ARC_SECOND : -1.0;
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public ValueInfo[] getTupleInfos() {
        return new ValueInfo[] { Tables.RA_INFO, Tables.DEC_INFO };
    }

    public DescribedValue[] getMatchParameters() {
        return new DescribedValue[] { sepValue_ };
    }

    public String toString() {
        return "Sky";
    }

    public boolean canBoundMatch() {
        return true;
    }

    public Comparable[][] getMatchBounds( Comparable[] radecMinIn,
                                          Comparable[] radecMaxIn ) {

        /* Get numeric values of RA and Dec input limits. */
        double rMinIn = radecMinIn[ 0 ] == null
                      ? Double.NaN
                      : ((Number) radecMinIn[ 0 ]).doubleValue();
        double dMinIn = radecMinIn[ 1 ] == null
                      ? Double.NaN
                      : ((Number) radecMinIn[ 1 ]).doubleValue();
        double rMaxIn = radecMaxIn[ 0 ] == null
                      ? Double.NaN
                      : ((Number) radecMaxIn[ 0 ]).doubleValue();
        double dMaxIn = radecMaxIn[ 1 ] == null
                      ? Double.NaN
                      : ((Number) radecMaxIn[ 1 ]).doubleValue();

        /* Calculate the corresponding output limits - these are similar,
         * but including an extra error of separation in any direction.
         * Any that we can't work out for one reason or another is stored
         * as NaN. */
        double rMinOut;
        double rMaxOut;
        double dMinOut = dMinIn - separation_;
        double dMaxOut = dMaxIn + separation_;
        if ( ! Double.isNaN( dMinOut ) && ! Double.isNaN( dMaxOut ) ) {
            double rDiffMax =
                Math.max( Math.abs( separation_ / Math.cos( dMinOut ) ),
                          Math.abs( separation_ / Math.cos( dMaxOut ) ) );
            rMinOut = rMinIn - rDiffMax;
            rMaxOut = rMaxIn + rDiffMax;
        }
        else {
            rMinOut = Double.NaN;
            rMaxOut = Double.NaN;
        }

        /* Finally prepare the results as Number objects.  It's a bit
         * fiddly since we have to make sure that the returned objects
         * are of the same type as the input ones (since otherwise
         * comparisons on them will probably fail with a ClassCastException).
         * We only consider the possibility here that they are of type
         * Float or Double - in the weird case in which they are not,
         * null values are returned, which is quite legal and will not
         * lead to incorrect results (only perhaps less efficient). */
        Comparable[] radecMinOut = new Comparable[ 2 ];
        Comparable[] radecMaxOut = new Comparable[ 2 ];
        if ( ! Double.isNaN( rMinOut ) ) {
            if ( radecMinIn[ 0 ] instanceof Float ) {
                radecMinOut[ 0 ] = new Float( (float) rMinOut );
            }
            else if ( radecMinIn[ 0 ] instanceof Double ) {
                radecMinOut[ 0 ] = new Double( rMinOut );
            }
        }
        if ( ! Double.isNaN( dMinOut ) ) {
            if ( radecMinIn[ 1 ] instanceof Float ) {
                radecMinOut[ 1 ] = new Float( (float) dMinOut );
            }
            else if ( radecMinIn[ 1 ] instanceof Double ) {
                radecMinOut[ 1 ] = new Double( dMinOut );
            }
        }
        if ( ! Double.isNaN( rMaxOut ) ) {
            if ( radecMaxIn[ 0 ] instanceof Float ) {
                radecMaxOut[ 0 ] = new Float( (float) rMaxOut );
            }
            else if ( radecMaxIn[ 0 ] instanceof Double ) {
                radecMaxOut[ 0 ] = new Double( rMaxOut );
            }
        }
        if ( ! Double.isNaN( dMaxOut ) ) {
            if ( radecMaxIn[ 1 ] instanceof Float ) {
                radecMaxOut[ 1 ] = new Float( (float) dMaxOut );
            }
            else if ( radecMaxIn[ 1 ] instanceof Double ) {
                radecMaxOut[ 1 ] = new Double( dMaxOut );
            }
        }
        return new Comparable[][] { radecMinOut, radecMaxOut };
    }

    /**
     * Returns the distance along a great circle between two points.
     *
     * @param   ra1  right ascension of point 1 in radians
     * @param   dec1 declination of point 1 in radians
     * @param   ra2  right ascension of point 2 in radians
     * @param   dec2 declination of point 2 in radians
     * @return  angular separation of point 1 and point 2 in radians
     */
    public static double calculateSeparation( double ra1, double dec1,
                                              double ra2, double dec2 ) {
        return haversineSeparationFormula( ra1, dec1, ra2, dec2 );
    }

    /**
     * Law of cosines for spherical trigonometry.
     * This is ill-conditioned for small angles (the cases we are generally
     * interested in here).  So don't use it!
     *
     * @deprecated  Ill-conditioned for small angles
     * @param   ra1  right ascension of point 1 in radians
     * @param   dec1 declination of point 1 in radians
     * @param   ra2  right ascension of point 2 in radians
     * @param   dec2 declination of point 2 in radians
     * @return  angular separation of point 1 and point 2 in radians
     */
    private static double cosineSeparationFormula( double ra1, double dec1,
                                                   double ra2, double dec2 ) {
        return Math.acos( Math.sin( dec1 ) * Math.sin( dec2 ) +
                          Math.cos( dec1 ) * Math.cos( dec2 )
                                           * Math.cos( ra1 - ra2 ) );
    }

    /**
     * Haversine formula for spherical trigonometry.
     * This does not have the numerical instabilities of the cosine formula
     * at small angles.
     * <p>
     * This implementation derives from Bob Chamberlain's contribution
     * to the comp.infosystems.gis FAQ; he cites
     * R.W.Sinnott, "Virtues of the Haversine", Sky and Telescope vol.68,
     * no.2, 1984, p159.
     *
     * @param   ra1  right ascension of point 1 in radians
     * @param   dec1 declination of point 1 in radians
     * @param   ra2  right ascension of point 2 in radians
     * @param   dec2 declination of point 2 in radians
     * @return  angular separation of point 1 and point 2 in radians
     * @see  <http://www.census.gov/geo/www/gis-faq.txt>
     */
    private static double haversineSeparationFormula( double ra1, double dec1,
                                                     double ra2, double dec2 ) {
        double sd2 = Math.sin( 0.5 * ( dec2 - dec1 ) );
        double sr2 = Math.sin( 0.5 * ( ra2 - ra1 ) );
        double a = sd2 * sd2 +
                   sr2 * sr2 * Math.cos( dec1 ) * Math.cos( dec2 );
        return a < 1.0 ? 2.0 * Math.asin( Math.sqrt( a ) )
                       : Math.PI;
    }

    /**
     * Implements the parameter which controls the matching error.
     */
    private class SkySeparationValue extends DescribedValue {
        SkySeparationValue() {
            super( SEP_INFO );
        }
        public Object getValue() {
            return new Double( getSeparation() );
        }
        public void setValue( Object value ) {
            setSeparation( ((Double) value).doubleValue() );
        }
    }
}
