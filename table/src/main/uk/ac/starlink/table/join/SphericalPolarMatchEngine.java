package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Match engine which works with tuples representing RA, Dec and range.
 * Each tuple must be a 3-element array of {@link java.lang.Number} objects:
 * first element is Right Ascension in radians,
 * second element is Declination in radians,
 * third element is range (units are arbitrary, but will be the same as
 * the error supplied in the constructor).
 *
 * @author   Mark Taylor (Starlink)
 */
public class SphericalPolarMatchEngine implements MatchEngine {

    private Double[] work0_ = new Double[ 3 ];
    private Double[] work1_ = new Double[ 3 ];
    private Double[] work2_ = new Double[ 3 ];
    private final IsotropicCartesianMatchEngine spaceEngine_;

    private static final DefaultValueInfo R_INFO =
        new DefaultValueInfo( "Distance", Number.class,
                              "Distance along the line of sight" );
    private static final DefaultValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Cartesian distance between matched points" );
    static {
        R_INFO.setNullable( false );
    }

    /**
     * Constructs a new match engine which will match on differences
     * not greater than a given number <tt>err</tt>, in the same units 
     * that the range part of the tuples is specified.
     * 
     * @param   err  maximum separation for a match
     */
    public SphericalPolarMatchEngine( double err ) {
        spaceEngine_ = new IsotropicCartesianMatchEngine( 3, err, false );
        ((DefaultValueInfo) spaceEngine_.errorParam_.getInfo())
                           .setUnitString( "Units of distance" );
    }

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        polarToCartesian( tuple1, work1_ );
        polarToCartesian( tuple2, work2_ );
        return spaceEngine_.matchScore( work1_, work2_ );
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public Object[] getBins( Object[] tuple ) {
        if ( tuple[ 0 ] instanceof Number &&
             tuple[ 1 ] instanceof Number &&
             tuple[ 2 ] instanceof Number ) {
            polarToCartesian( tuple, work0_ );
            return spaceEngine_.getBins( work0_ );
        }
        else {
            return NO_BINS;
        }
    }

    public ValueInfo[] getTupleInfos() {
        return new ValueInfo[] { Tables.RA_INFO, Tables.DEC_INFO, R_INFO };
    }

    public DescribedValue[] getMatchParameters() {
        return spaceEngine_.getMatchParameters();
    }

    /**
     * Returns false.  It would probably be possible to implement this,
     * but not very easy.
     */
    public boolean canBoundMatch() {
        return false;
    }
    public Comparable[][] getMatchBounds( Comparable[] min, Comparable[] max ) {
        throw new UnsupportedOperationException();
    }


    public String toString() {
        return "Sky 3D";
    }

    /**
     * Converts spherical polar coordinates to Cartesian ones.
     *
     * @param  polar  array of Numbers specified as input: ra, dec, range
     * @param  cartesian  array filled with Doubles as output: x, y, z
     */
    private static void polarToCartesian( Object[] polar, Object[] cartesian ) {
        double ra = ((Number) polar[ 0 ]).doubleValue();
        double dec = ((Number) polar[ 1 ]).doubleValue();
        double r = ((Number) polar[ 2 ]).doubleValue();

        double cd = Math.cos( dec );
        double sd = Math.sin( dec );
        double cr = Math.cos( ra );
        double sr = Math.sin( ra );

        double x = r * cr * cd;
        double y = r * sr * cd;
        double z = r * sd;

        cartesian[ 0 ] = new Double( x );
        cartesian[ 1 ] = new Double( y );
        cartesian[ 2 ] = new Double( z );
    }
}
