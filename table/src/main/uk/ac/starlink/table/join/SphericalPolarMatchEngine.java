package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DefaultValueInfo;
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
public class SphericalPolarMatchEngine extends CartesianMatchEngine {

    private Double[] work0 = new Double[ 3 ];
    private Double[] work1 = new Double[ 3 ];
    private Double[] work2 = new Double[ 3 ];

    private static final DefaultValueInfo RA_INFO =
        new DefaultValueInfo( "RA", Number.class, "Right Ascension" );
    private static final DefaultValueInfo DEC_INFO =
        new DefaultValueInfo( "Dec", Number.class, "Declination" );
    private static final DefaultValueInfo R_INFO =
        new DefaultValueInfo( "Radius", Number.class, "Distance from Origin" );
    static {
        RA_INFO.setUnitString( "radians" );
        DEC_INFO.setUnitString( "radians" );
        RA_INFO.setNullable( false );
        DEC_INFO.setNullable( false );
        R_INFO.setNullable( false );
        RA_INFO.setUCD( "POS_EQ_RA" );
        DEC_INFO.setUCD( "POS_EQ_DEC" );
    }

    /**
     * Constructs a new match engine which will match on differences
     * not greater than a given number <tt>err</tt>, in the same units 
     * that the range part of the tuples is specified.
     * 
     * @param   err  maximum separation for a match
     */
    public SphericalPolarMatchEngine( double err ) {
        super( 3, err );
    }

    public boolean matches( Object[] tuple1, Object[] tuple2 ) {
        polarToCartesian( tuple1, work1 );
        polarToCartesian( tuple2, work2 );
        return super.matches( work1, work2 );
    }

    public Object[] getBins( Object[] tuple ) {
        polarToCartesian( tuple, work0 );
        return super.getBins( work0 );
    }

    public ValueInfo[] getTupleInfos() {
        return new ValueInfo[] { RA_INFO, DEC_INFO, R_INFO };
    }

    public String toString() {
        return "Spherical Polar";
    }

    /**
     * Sets the errors for matching.
     *
     * @param  errs  array with all the same elements 
     * @throws  IllegalArgumentException  if not all elements of 
     *          <tt>errs</tt> are equal (this would signal an 
     *          anisotropic error range, not supported by this class)
     */
    public void setErrors( double[] errs ) {
        if ( errs[ 1 ] != errs[ 0 ] || errs[ 2 ] != errs[ 0 ] ) {
            throw new IllegalArgumentException(
                "Only isotropic errors for spherical polars" );
        }
        super.setErrors( errs );
    }
   
    /**
     * Returns the isotropic error.
     * The units are the same as those that range is specified in.
     *
     * @return  error margin for matches
     */
    public double getError() {
        double[] errs = super.getErrors();
        assert errs[ 1 ] == errs[ 0 ] && errs[ 2 ] == errs[ 0 ];
        return errs[ 0 ];
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
