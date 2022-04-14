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

    /**
     * Uses the pixellator to get a list of bin objects for a given
     * small circle.
     *
     * @param   alpha  right ascension of circle centre in radians
     * @param   delta  declination of circle centre in radians
     * @param   radius  radius of circle centre in radians
     * @return  list of opaque pixel objects, comparable for equality,
     *          representing all pixels which are at least partially
     *          overlapped by the given circle
     */
    Object[] getBins( double alpha, double delta, double radius ) {
        return ( ! Double.isNaN( alpha ) &&
                 ! Double.isNaN( delta ) &&
                 radius >= 0 )
             ? pixellator_.getPixels( alpha, delta, radius )
             : NO_BINS;
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
             ? new Double( ((Number) value).doubleValue() * factor )
             : null;
    }

    /**
     * Multiplies all the min and max values in a given NdRange by a
     * supplied factor.
     *
     * @param   range  input range
     * @param   factor  multiplicand
     * @return   rescaled NdRange
     */
    static NdRange multiplyNdRange( NdRange range, double factor ) {
        Comparable<?>[] mins = range.getMins().clone();
        Comparable<?>[] maxs = range.getMaxs().clone();
        int ndim = mins.length;
        for ( int i = 0; i < ndim; i++ ) {
            mins[ i ] = multiply( mins[ i ], factor );
            maxs[ i ] = multiply( maxs[ i ], factor );
        }
        return new NdRange( mins, maxs );
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
     * Utility method to return a pair of min/max comparable arrays
     * based on an input pair, but with RA and Dec coordinates extended
     * according to a known error radius.
     * For the indicated RA and Dec elements the bounds will be widened
     * appropriately.  Other elements will be null.
     * If the RA/Dec bounds cannot be extended appropriately for some reason,
     * null will be used.
     *
     * @param  inRange   input bounds
     * @param  ialpha    index in tuples of the right ascension coordinate
     * @param  idelta    index in tuples of the declination coordinate
     * @param  err       amount in radians to extend bounds by
     * @return output bounds - effectively input bounds broadened by errors
     */
    static NdRange createExtendedSkyBounds( NdRange inRange, int ialpha,
                                            int idelta, double err ) {
        Comparable<?>[] minTuple = inRange.getMins();
        Comparable<?>[] maxTuple = inRange.getMaxs();

        /* Get numeric values of sky coordinate input limits. */
        double alphaMinIn = getNumberValue( minTuple[ ialpha ] );
        double deltaMinIn = getNumberValue( minTuple[ idelta ] );
        double alphaMaxIn = getNumberValue( maxTuple[ ialpha ] );
        double deltaMaxIn = getNumberValue( maxTuple[ idelta ] );

        /* Calculate the corresponding output limits - these are similar,
         * but including an extra error of separation in any direction.
         * Any that we can't work out for one reason or another is stored
         * as NaN. */
        double alphaMinOut;
        double alphaMaxOut;
        double deltaMinOut = deltaMinIn - err;
        double deltaMaxOut = deltaMaxIn + err;
        if ( ! Double.isNaN( deltaMinOut ) && ! Double.isNaN( deltaMaxOut ) ) {

            /* Use trig to adjust right ascension limits accordingly. */
            double alphaDiffMax =
                Math.max( Math.abs( err / Math.cos( deltaMinOut ) ),
                          Math.abs( err / Math.cos( deltaMaxOut ) ) );
            alphaMinOut = alphaMinIn - alphaDiffMax;
            alphaMaxOut = alphaMaxIn + alphaDiffMax;

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
            if ( ! ( alphaMinOut >= 0 && alphaMinOut <= 2 * Math.PI &&
                     alphaMaxOut >= 0 && alphaMaxOut <= 2 * Math.PI ) ) {
                alphaMinOut = Double.NaN;
                alphaMaxOut = Double.NaN;
            }
        }
        else {
            alphaMinOut = Double.NaN;
            alphaMaxOut = Double.NaN;
        }

        /* Finally insert the values as objects into appropriate output
         * Comparable arrays and return the result. */
        Comparable<?>[] minOuts = new Comparable<?>[ minTuple.length ];
        Comparable<?>[] maxOuts = new Comparable<?>[ maxTuple.length ];
        minOuts[ ialpha ] = toFloatingNumber( alphaMinOut, minTuple[ ialpha ] );
        minOuts[ idelta ] = toFloatingNumber( deltaMinOut, minTuple[ idelta ] );
        maxOuts[ ialpha ] = toFloatingNumber( alphaMaxOut, maxTuple[ ialpha ] );
        maxOuts[ idelta ] = toFloatingNumber( deltaMaxOut, maxTuple[ idelta ] );
        return new NdRange( minOuts, maxOuts );
    }

    /**
     * Invokes {@link #createExtendedSkyBounds} with input and output
     * NdRanges in degrees rather than radians.
     *
     * @param  inRange   input bounds, RA and Dec limits in degrees
     * @param  ialphaDeg index in tuples of the right ascension coordinate
     *                   measured in degrees
     * @param  ideltaDeg index in tuples of the declination coordinate
     *                   measured in degrees
     * @param  errRadians    amount in radians to extend bounds by
     * @return output bounds - effectively input bounds broadened by errors
     */
    static NdRange createExtendedSkyBoundsDegrees( NdRange inRange,
                                                   int ialphaDeg, int ideltaDeg,
                                                   double errRadians ) {
        return multiplyNdRange(
               createExtendedSkyBounds( multiplyNdRange( inRange, FROM_DEG ),
                                        ialphaDeg, ideltaDeg, errRadians ),
               TO_DEG );
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
     * Turn a numeric value into a floating point number object
     * of the same type as a template object.  If the template is not
     * Float or Double, or if the value is NaN, null is returned.
     *
     * @param   value  numeric value
     * @param   template  object with template type
     * @return  Float or Double object of same type as template and value
     *          as value, or null
     */
    private static Comparable<?> toFloatingNumber( double value,
                                                   Comparable<?> template ) {
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
            return new Double( getScale() );
        }

        public void setValue( Object value ) {
            setScale( getNumberValue( value ) );
        }
    }
}
