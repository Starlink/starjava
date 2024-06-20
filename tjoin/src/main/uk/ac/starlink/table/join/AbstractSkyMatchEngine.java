package uk.ac.starlink.table.join;

import cds.healpix.common.math.FastMath;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Abstract superclass for MatchEngines which work on the celestial sphere.
 *
 * @author   Mark Taylor
 * @since    5 Sep 2011
 */
public abstract class AbstractSkyMatchEngine implements MatchEngine {

    private final SkyPixellator pixellator_;

    static final double INVERSE_ARC_SECOND = ( 180. * 60. * 60. ) / Math.PI;

    static final double FROM_DEG = Math.PI / 180.;
    static final double TO_DEG = 180. / Math.PI;
    static final double FROM_ARCSEC = Math.PI / ( 180. * 3600. );
    static final double TO_ARCSEC = ( 180. * 3600. ) / Math.PI;

    /**
     * Constructor.
     *
     * @param  pixellator   handles sky pixellisation
     * @param  scale   initial length scale for pixels, in radians
     */
    protected AbstractSkyMatchEngine( SkyPixellator pixellator, double scale ) {
        pixellator_ = pixellator;
        setScale( scale );
    }

    /**
     * Sets the length scale used for sky pixellisation.
     *
     * @param  scale  pixel length scale in radians
     */
    protected void setScale( double scale ) {
        pixellator_.setScale( scale );
    }

    /**
     * Returns the length scale used for sky pixellisation.
     *
     * @return  pixel length scale in radians
     */
    protected double getScale() {
        return pixellator_.getScale();
    }

    public DescribedValue[] getTuningParameters() {
        return new DescribedValue[] { pixellator_.getTuningParameter() };
    }

    /**
     * Returns this object's pixellator.
     *
     * @return   pixellator
     */
    public SkyPixellator getPixellator() {
        return pixellator_;
    }

    /**
     * Utility function to provide a match score between two points on the
     * sphere.
     *
     * @param   alpha1  right ascension of point 1 in radians
     * @param   delta1  declination of point 1 in radians
     * @param   alpha2  right ascension of point 2 in radians
     * @param   delta2  declination of point 2 in radians
     * @param   maxerr  maximum permitted separation of points 1 and 2
     *                  in radians
     * @return   distance along a great circle in arc seconds between
     *           the points if they are within <code>maxerr</code>
     *           of each other, otherwise -1
     */
    static double matchScore( double alpha1, double delta1,
                              double alpha2, double delta2, double maxerr ) {

        /* Cheap test which will throw out most comparisons straight away:
         * see if the separation in declination is greater than the maximum
         * acceptable separation. */
        if ( Math.abs( delta1 - delta2 ) > maxerr ) {
            return -1.0;
        }

        /* Declinations at least are close; do a proper test. */
        double sep = calculateSeparation( alpha1, delta1, alpha2, delta2 );
        return sep <= maxerr ? sep * INVERSE_ARC_SECOND : -1.0;
    }

    /**
     * Returns the maximum value that will be returned from the
     * static <code>matchScore</code> method for a given maximum error.
     *
     * @param   maxerr  maximum permitted separation of points 1 and 2
     *                  in radians
     * @return  maximum score value; currently that's <code>maxerr</code>
     *          converted to arcseconds
     */
    static double maxScore( double maxerr ) {
        return INVERSE_ARC_SECOND * maxerr;
    }

    public abstract String toString();

    /**
     * Returns the distance along a great circle between two points.
     *
     * @param   alpha1  right ascension of point 1 in radians
     * @param   delta1 declination of point 1 in radians
     * @param   alpha2  right ascension of point 2 in radians
     * @param   delta2 declination of point 2 in radians
     * @return  angular separation of point 1 and point 2 in radians
     */
    public static double calculateSeparation( double alpha1, double delta1,
                                              double alpha2, double delta2 ) {
        return haversineSeparationFormula( alpha1, delta1, alpha2, delta2 );
    }

    /**
     * Indicates whether a (longitude, latitude) pair can be interpreted
     * as a legal position on the sky.
     *
     * @param  alpha  longitude in radians
     * @param  delta  latitude in radians
     * @return  true iff alpha is a finite real number and
     *               delta is in the range -PI/2&lt;=delta&lt;=PI/2
     */
    public static boolean isSkyPosition( double alpha, double delta ) {
        return Double.isFinite( alpha )
            && delta >= -0.5 * Math.PI
            && delta <= +0.5 * Math.PI;
    }

    /**
     * Returns a ValueInfo like a supplied one but with units of "degrees".
     *
     * @param  info  input metadata
     * @return  output metadata
     */
    static ValueInfo inDegreeInfo( ValueInfo info ) {
        DefaultValueInfo dinfo1 = new DefaultValueInfo( info );
        dinfo1.setUnitString( "degrees" );
        return dinfo1;
    }

    /**
     * Returns a ValueInfo like a supplied one but with unts of "arcsec".
     *
     * @param  info  input metadata
     * @return  output metadata
     */
    static ValueInfo inArcsecInfo( ValueInfo info ) {
        DefaultValueInfo dinfo1 = new DefaultValueInfo( info );
        dinfo1.setUnitString( "arcsec" );
        return dinfo1;
    }

    /**
     * Returns a DescribedValue based on a supplied radians-based one
     * but with units of arcseconds.
     * The documentation is modified and getting or setting the value
     * in arcseconds will modify the radians-based value of the supplied
     * object appropriately.
     *
     * @param  param  input described value
     * @return  output described value
     */
    static DescribedValue radiansToArcsecParam( DescribedValue param ) {
        return new DescribedValue( inArcsecInfo( param.getInfo() ),
                                   multiply( param.getValue(), TO_ARCSEC ) ) {
            @Override
            public Object getValue() {
                return multiply( param.getValue(), TO_ARCSEC );
            }
            @Override
            public void setValue( Object value ) {
                param.setValue( multiply( value, FROM_ARCSEC ) );
            }
        };
    }

    /**
     * Multiplies a number supplied as a Number object by a given factor,
     * returning a Number object.
     *
     * @param  value  numeric value as wrapper object
     * @param  factor  multiplicand
     * @return  product as Double value, or null if trouble
     */
    static Double multiply( Object value, double factor ) {
        return value instanceof Number
             ? Double.valueOf( ((Number) value).doubleValue() * factor )
             : null;
    }

    /**
     * Law of cosines for spherical trigonometry.
     * This is ill-conditioned for small angles (the cases we are generally
     * interested in here).  So don't use it!
     *
     * @deprecated  Ill-conditioned for small angles
     * @param   alpha1  right ascension of point 1 in radians
     * @param   delta1  declination of point 1 in radians
     * @param   alpha2  right ascension of point 2 in radians
     * @param   delta2  declination of point 2 in radians
     * @return  angular separation of point 1 and point 2 in radians
     */
    @Deprecated
    private static double
            cosineSeparationFormula( double alpha1, double delta1,
                                     double alpha2, double delta2 ) {
        return Math.acos( Math.sin( delta1 ) * Math.sin( delta2 ) +
                          Math.cos( delta1 ) * Math.cos( delta2 )
                                             * Math.cos( alpha1 - alpha2 ) );
    }

    /**
     * Haversine formula for spherical trigonometry.
     * This does not exhibit the numerical instabilities of the cosine formula
     * at small angles.
     * <p>
     * This implementation derives from Bob Chamberlain's contribution
     * to the comp.infosystems.gis FAQ; he cites
     * R.W.Sinnott, "Virtues of the Haversine", Sky and Telescope vol.68,
     * no.2, 1984, p159.
     *
     * @param   alpha1  right ascension of point 1 in radians
     * @param   delta1  declination of point 1 in radians
     * @param   alpha2  right ascension of point 2 in radians
     * @param   delta2  declination of point 2 in radians
     * @return  angular separation of point 1 and point 2 in radians
     * @see  <http://www.census.gov/geo/www/gis-faq.txt>
     */
    private static double
            haversineSeparationFormula( double alpha1, double delta1,
                                        double alpha2, double delta2 ) {
        double sd2 = Math.sin( 0.5 * ( delta2 - delta1 ) );
        double sa2 = Math.sin( 0.5 * ( alpha2 - alpha1 ) );
        double a = sd2 * sd2 +
                   sa2 * sa2 * Math.cos( delta1 ) * Math.cos( delta2 );
        return a < 1.0 ? 2.0 * FastMath.asin( Math.sqrt( a ) )
                       : Math.PI;
    }

    /**
     * Returns the numeric value for an object if it is a Number,
     * and NaN otherwise.
     *
     * @param  numobj  object
     * @return  numeric value
     */
    static double getNumberValue( Object numobj ) {
        return numobj instanceof Number
             ? ((Number) numobj).doubleValue()
             : Double.NaN;
    }

    /**
     * Implements the parameter which controls the angular scale.
     */
    class SkyScaleParameter extends DescribedValue {

        /**
         * Constructor.
         *
         * @param  info  value metadata
         */
        SkyScaleParameter( ValueInfo info ) {
            super( info );
        }

        public Object getValue() {
            return Double.valueOf( getScale() );
        }

        public void setValue( Object value ) {
            setScale( getNumberValue( value ) );
        }
    }
}
