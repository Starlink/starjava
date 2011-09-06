package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * MatchEngine which matches objects on the celestial sphere according to
 * per-object error radii.
 * The tuples it uses are three-element arrays of {@link java.lang.Number}
 * objects, representing Right Ascension, Declination, and error radius
 * respectively, all in radians.  Other similar longitude/latitude-like
 * coordinate system may alternatively be used.
 * Two tuples are considered to match if the distance along a great circle
 * of their central positions is no greater than the sum of their per-object
 * radii.
 *
 * <p>A length scale must be supplied, which should be of comparable size
 * to the average per-object error, and which affects performance but not
 * the result.  The effect of this is to provide a default for the 
 * pixellisation tuning parameter.  If the tuning parameter is set directly,
 * the length scale is ignored.
 *
 * @author   Mark Taylor
 * @since    6 Sep 2011
 */
public class ErrorSkyMatchEngine extends AbstractSkyMatchEngine {

    private final DescribedValue[] matchParams_;

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
        SCALE_INFO.setUnitString( "radians" );
        SCALE_INFO.setUCD( "pos.angDistance" );
        SCALE_INFO.setNullable( false );

        ERR_INFO.setUnitString( "radians" );
        ERR_INFO.setUCD( "pos.angDistance" );
        ERR_INFO.setNullable( false );

        SCORE_INFO.setUnitString( "arcsec" );
        SCORE_INFO.setUCD( "pos.angDistance" );
    }

    /**
     * Constructor.
     *
     * @param  pixellator  handles sky pixellisation
     * @param  scale       initial value for length scale, in radians
     */
    public ErrorSkyMatchEngine( SkyPixellator pixellator, double scale ) {
        super( pixellator, scale );
        matchParams_ =
            new DescribedValue[] { new SkyScaleParameter( SCALE_INFO ) };
    }

    /**
     * Sets the length scale.
     *
     * @param  scale rough value of per-object errors, in radians
     */
    public void setScale( double scale ) {
        super.setScale( scale );
    }

    /**
     * Returns the length scale.
     *
     * @return  length scale value in radians
     */
    public double getScale() {
        return super.getScale();
    }

    public ValueInfo[] getTupleInfos() {
        return new ValueInfo[] {
            Tables.RA_INFO, Tables.DEC_INFO, ERR_INFO,
        };
    }

    public DescribedValue[] getMatchParameters() {
        return matchParams_;
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        return matchScore( getAlpha( tuple1 ), getDelta( tuple1 ),
                           getAlpha( tuple2 ), getDelta( tuple2 ),
                           getError( tuple1 ) + getError( tuple2 ) );
    }

    public Object[] getBins( Object[] tuple ) {
        return getBins( getAlpha( tuple ), getDelta( tuple ),
                        getError( tuple ) );
    }

    public boolean canBoundMatch() {
        return true;
    }

    public Comparable[][] getMatchBounds( Comparable[] minTuple,
                                          Comparable[] maxTuple ) {
        double maxError = getError( maxTuple );
        return createExtendedSkyBounds( minTuple, maxTuple, 0, 1,
                                        2 * maxError );
    }

    public String toString() {
        return "Sky with Errors";
    }

    /**
     * Extracts the RA value from a tuple.
     *
     * @param   tuple  object tuple intended for this matcher
     * @return  right ascension coordinate in radians
     */
    private double getAlpha( Object[] tuple ) {
        return getNumberValue( tuple[ 0 ] );
    }

    /**
     * Extracts the Declination value from a tuple.
     *
     * @param   tuple  object tuple intended for this matcher
     * @return  declination coordinate in radians
     */
    private double getDelta( Object[] tuple ) {
        return getNumberValue( tuple[ 1 ] );
    }

    /**
     * Extracts the per-object error radius from a tuple.
     *
     * @param   tuple  object tuple intended for this matcher
     * @return  error radius in radians
     */
    private double getError( Object[] tuple ) {
        return getNumberValue( tuple[ 2 ] );
    }
}
