package uk.ac.starlink.table.join;

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

    /**
     * Converts spherical polar coordinates to cartesian ones.
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
