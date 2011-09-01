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
 * This can operate in two modes, with or without per-object errors.
 * In the latter, the <tt>separation</tt> attribute indicates how many
 * radians may separate two points on the celestial sphere for them to be 
 * considered matching.  In the former, that threshold is determined by
 * the sum of the errors supplied by each point, and the separation is
 * just used as a guide value for tuning purposes.
 *
 * @author   Mark Taylor (Starlink)
 * @since    15 Mar 2005
 */
public abstract class SkyMatchEngine implements MatchEngine {

    private double separation_;
    private boolean useErrors_;
    private DescribedValue sepValue_ = new SkySeparationValue();

    private static final double ARC_SECOND = Math.PI / 180 / 60 / 60;
    private static final DefaultValueInfo SEP_INFO =
        new DefaultValueInfo( "Max Error", Number.class,
                              "Maximum separation along a great circle" );
    private static final DefaultValueInfo SCALE_INFO =
        new DefaultValueInfo( "Scale", Number.class,
                              "Rough average of per-object error distance; "
                            + "just used for tuning to set "
                            + "default pixel size" );
    private static final DefaultValueInfo ERR_INFO =
        new DefaultValueInfo( "Error", Number.class,
                              "Per-object error radius along a great circle" );
    private static final DefaultValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Distance between matched objects " +
                              "along a great circle" );
    static {
        SEP_INFO.setUnitString( "radians" );
        SEP_INFO.setNullable( false );

        SCALE_INFO.setUnitString( "radians" );
        SCALE_INFO.setNullable( false );

        ERR_INFO.setUnitString( "radians" );
        ERR_INFO.setNullable( true );

        SCORE_INFO.setUnitString( "arcsec" );
        SCORE_INFO.setUCD( "pos.angDistance" );
    }

    /**
     * Constructs a new match engine which considers two points
     * (RA,Dec tuples) to match if they are within a given angular
     * distance on the celestial sphere.
     *
     * @param   useErrors   if true, per-row errors can be specified as
     *          a third element of the tuples; otherwise only the fixed
     *          separation value counts
     */
    public SkyMatchEngine( boolean useErrors ) {
        setUseErrors( useErrors );
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
     * Indicates whether this engine is using per-row errors for matching.
     *
     * @return  true  iff per-row error radii are in use
     */
    public boolean getUseErrors() {
        return useErrors_;
    }

    /**
     * Sets whether this engine will use per-row errors for matching.
     *
     * @param  use  true to use per-row errors
     */
    public void setUseErrors( boolean use ) {
        useErrors_ = use;
        sepValue_ = new SkySeparationValue();
    }

    /**
     * Determines the match score of two tuples.
     * If <code>useErrors</code> is true the tuples are three-element,
     * giving RA, Dec and error radius, all as Number objects in radians.
     * If it is false, they are two element, giving just RA and Dec.
     *
     * @param   tuple1  values representing first point
     * @param   tuple2  values representing second point
     * @return  distance in arcseconds between first and second points
     *          if they are close enough to match, otherwise -1
     */
    public double matchScore( Object[] tuple1, Object[] tuple2 ) {

        /* Work out maximum permissible distance between points. */
        double err = getUseErrors() ? getNumberValue( tuple1[ 2 ] ) +
                                      getNumberValue( tuple2[ 2 ] )
                                    : getSeparation();
        if ( ! ( err >= 0 ) ) {
            return -1.0;
        }

        /* Cheap test which will throw out most comparisons straight away:
         * see if the separation in declination is greater than the maximum
         * acceptable separation. */
        double dec1 = getNumberValue( tuple1[ 1 ] );
        double dec2 = getNumberValue( tuple2[ 1 ] );
        if ( Math.abs( dec1 - dec2 ) > err ) {
            return -1.0;
        }

        /* Declinations at least are close; do a proper test. */
        double ra1 = getNumberValue( tuple1[ 0 ] );
        double ra2 = getNumberValue( tuple2[ 0 ] );
        double sep = calculateSeparation( ra1, dec1, ra2, dec2 );
        return sep <= err ? sep / ARC_SECOND : -1.0;
    }

    public Object[] getBins( Object[] tuple ) {
        double ra = getNumberValue( tuple[ 0 ] );
        double dec = getNumberValue( tuple[ 1 ] );
        double err = useErrors_ ? getNumberValue( tuple[ 2 ] )
                                : getSeparation();
        return ( ! Double.isNaN( ra ) && ! Double.isNaN( dec ) && err >= 0 )
             ? getBins( ra, dec, err )
             : NO_BINS;
    }

    /**
     * Returns a set of keys for bins into which possible matches 
     * for a given sky position, with a given error, might fall.
     * The returned objects can be anything, but should have their
     * <code>equals</code> and <code>hashCode</code> methods 
     * implemented properly for comparison.
     *
     * @param   ra   right ascension of point to test (radians)
     * @param   dec  declination of point to test (radians)
     * @param   err  possible distance away from given location of match
     * @see    #getBins(java.lang.Object[])
     */
    protected abstract Object[] getBins( double ra, double dec, double err );

    public ValueInfo getMatchScoreInfo() {
        return new DefaultValueInfo( SCORE_INFO );
    }

    public ValueInfo[] getTupleInfos() {
        return getUseErrors()
             ? new ValueInfo[] { new DefaultValueInfo( Tables.RA_INFO ),
                                 new DefaultValueInfo( Tables.DEC_INFO ),
                                 new DefaultValueInfo( ERR_INFO ), }
             : new ValueInfo[] { new DefaultValueInfo( Tables.RA_INFO ),
                                 new DefaultValueInfo( Tables.DEC_INFO ), };
    }

    public DescribedValue[] getMatchParameters() {
        return new DescribedValue[] { sepValue_ };
    }

    public String toString() {
        return getUseErrors() ? "Sky with Errors"
                              : "Sky";
    }

    public boolean canBoundMatch() {
        return true;
    }

    public Comparable[][] getMatchBounds( Comparable[] minIns,
                                          Comparable[] maxIns ) {

        /* Get numeric values of RA and Dec input limits. */
        double rMinIn = getNumberValue( minIns[ 0 ] );
        double dMinIn = getNumberValue( minIns[ 1 ] );
        double rMaxIn = getNumberValue( maxIns[ 0 ] );
        double dMaxIn = getNumberValue( maxIns[ 1 ] );

        /* Get the maximum error which may surround any of the presented
         * points. */
        boolean useerr = getUseErrors();
        double err = useerr ? 2 * getNumberValue( maxIns[ 2 ] )
                            : getSeparation();

        /* Calculate the corresponding output limits - these are similar,
         * but including an extra error of separation in any direction.
         * Any that we can't work out for one reason or another is stored
         * as NaN. */
        double rMinOut;
        double rMaxOut;
        double dMinOut = dMinIn - err;
        double dMaxOut = dMaxIn + err;
        if ( ! Double.isNaN( dMinOut ) && ! Double.isNaN( dMaxOut ) ) {

            /* Use trig to adjust right ascension limits correctly. */
            double rDiffMax =
                Math.max( Math.abs( err / Math.cos( dMinOut ) ),
                          Math.abs( err / Math.cos( dMaxOut ) ) );
            rMinOut = rMinIn - rDiffMax;
            rMaxOut = rMaxIn + rDiffMax;

            /* Check that the RA limits are in the range 0-360 degrees.
             * If not, the range may be straddling RA=0, or may be using
             * an unconventional range for RA.  In either case, attempting
             * to use box-like bounds to confine the possible match range
             * may do the wrong thing.  There's nothing magic about the
             * range 0..360 (as opposed to, e.g., -180..180), but it is
             * necessary that all the datasets for a given match use the
             * same range convention.  If any of the limits are out of
             * range in this way, give up on attempting to provide
             * bounding values for RA.  Note this test will catch values
             * which are infinite or NaN as well. */
            if ( ! ( rMinOut >= 0 && rMinOut <= 2 * Math.PI &&
                     rMaxOut >= 0 && rMaxOut <= 2 * Math.PI ) ) {
                rMinOut = Double.NaN;
                rMaxOut = Double.NaN;
            }
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
        Comparable[] minOuts = new Comparable[ useerr ? 3 : 2 ];
        Comparable[] maxOuts = new Comparable[ useerr ? 3 : 2 ];
        minOuts[ 0 ] = toFloatingNumber( rMinOut, minIns[ 0 ] );
        minOuts[ 1 ] = toFloatingNumber( dMinOut, minIns[ 1 ] );
        maxOuts[ 0 ] = toFloatingNumber( rMaxOut, maxIns[ 0 ] );
        maxOuts[ 1 ] = toFloatingNumber( dMaxOut, maxIns[ 1 ] );
        return new Comparable[][] { minOuts, maxOuts };
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
     * Returns the numeric value for an object if it is a Number,
     * and NaN otherwise.
     *
     * @param  numobj  object
     * @return  numeric value
     */
    private static double getNumberValue( Object numobj ) {
        return numobj instanceof Number
             ? ((Number) numobj).doubleValue()
             : Double.NaN;
    }

    /**
     * Turn a numeric value into a floating point number object
     * of the same type as a template object.  If the template is not
     * Float or Double, or if the value is NaN, null is returned.
     *
     * @param   value  numeric value
     * @param   template  object with template type
     * @return  Float or Double object of same type as template and value
     *          as value, or null
     */
    private static Comparable toFloatingNumber( double value,
                                                Comparable template ) {
        if ( Double.isNaN( value ) ) {
            return null;
        }
        else if ( template instanceof Double ) {
            return new Double( value );
        }
        else if ( template instanceof Float ) {
            return new Float( (float) value );
        }
        else {
            return null;
        }
    }

    /**
     * Implements the parameter which controls the matching error.
     */
    private class SkySeparationValue extends DescribedValue {
        SkySeparationValue() {
            super( useErrors_ ? SCALE_INFO : SEP_INFO );
        }
        public Object getValue() {
            return new Double( getSeparation() );
        }
        public void setValue( Object value ) {
            setSeparation( ((Double) value).doubleValue() );
        }
    }
}
