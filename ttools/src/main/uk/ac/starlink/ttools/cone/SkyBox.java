package uk.ac.starlink.ttools.cone;

/**
 * Defines a region of the celestial sphere which is rectangular in 
 * Right Ascension and Declination coordinates.
 *
 * <p>This class just aggregates a two-element double[] array (x1,x2)
 * for each of right ascension and declination.  
 * The meaning in each case is as follows:
 * <ul>
 * <li>x1&lt;x2: included range (x1 &lt;= value &lt;= x2)</li>
 * <li>x1&gt;x2: excluded range (value &lt;= x1 or value &gt;= x2)</li>
 * </ul>
 * The degenerate case x1==x2 can be viewed as either.
 *
 * <p>Values should be in the range 0&lt;=ra&lt;2*PI and -PI/2&lt;dec&lt;+PI
 * (or equivalent in degrees if degrees are being used).
 *
 * <p>The static {@link #getConeBox} method is provided to generate a SkyBox
 * suitable for cone search queries.
 *
 * @author   Mark Taylor
 * @since    9 Jan 2008
 */
public class SkyBox {

    private final double[] raRange_;
    private final double[] decRange_;

    /**
     * Constructor.
     * See class documentation for range semantics.
     *
     * @param   raRange  (ra1,ra2) array
     * @param   decRange  (dec1,dec2) array
     */
    SkyBox( double[] raRange, double[] decRange ) {
        raRange_ = raRange;
        decRange_ = decRange;
    }

    /**
     * Returns right ascension range.
     * See class documentation for range semantics.
     *
     * @return  (ra1,ra2) array or null
     */
    public double[] getRaRange() {
        return raRange_;
    }

    /**
     * Returns declination range.
     * See class documentation for range semantics.
     *
     * @return (dec1,dec2) array or null
     */
    public double[] getDecRange() {
        return decRange_;
    }

    /**
     * Converts the units of this SkyBox from radians to degrees.
     *
     * @return  new sky box with converted range values
     */
    public SkyBox toDegrees() {
        return new SkyBox( new double[] { Math.toDegrees( raRange_[ 0 ] ),
                                          Math.toDegrees( raRange_[ 1 ] ) },
                           new double[] { Math.toDegrees( decRange_[ 0 ] ),
                                          Math.toDegrees( decRange_[ 1 ] ) } );
    }

    /**
     * Converts the units of this SkyBox from degrees to radians.
     *
     * @return  new sky box with converted range values
     */
    public SkyBox toRadians() {
        return new SkyBox( new double[] { Math.toRadians( raRange_[ 0 ] ),
                                          Math.toRadians( raRange_[ 1 ] ) },
                           new double[] { Math.toRadians( decRange_[ 0 ] ),
                                          Math.toRadians( decRange_[ 1 ] ) } );
    }

    /**
     * Returns a minimal SkyBox which encloses a given cone using radians.
     * The declination range of the result will be of included type,
     * but the right ascension range may be either included or excluded.
     *
     * @param   ra  cone centre right ascension in radians
     * @param   dec cone centre declination in radians
     * @param   sr  cone radius in radians
     * @return  sky box enclosing cone, with angles in radians
     */
    public static SkyBox getConeBox( double ra, double dec, double sr ) {

        /* Declination range calculation is straightforward, just limit to
         * +/- PI/2. */
        double deltaDec = sr;
        double minDec = Math.max( dec - deltaDec, -Math.PI / 2 );
        double maxDec = Math.min( dec + deltaDec, +Math.PI / 2 );
        double[] decRange = new double[] { minDec, maxDec };
        assert decRange[ 0 ] >= - Math.PI / 2 && decRange[ 0 ] <= + Math.PI / 2;
        assert decRange[ 1 ] >= - Math.PI / 2 && decRange[ 1 ] <= + Math.PI / 2;

        /* Right ascension range is more involved.  First find out the range
         * in RA covered by the radius at given declination. */
        double deltaRa = calculateDeltaRa( dec, sr );
        double minRa = ra - deltaRa;
        double maxRa = ra + deltaRa; 
        double[] raRange;

        /* If it's more than PI, the whole RA range is covered. */
        if ( deltaRa >= Math.PI ) { 
            raRange = new double[] { 0, 2 * Math.PI };
            assert raRange[ 0 ] <= raRange[ 1 ];
        }

        /* If it does not straddle the line RA=0, it's an included range. */
        else if ( minRa >= 0 && maxRa <= 2 * Math.PI ) {
            raRange = new double[] { minRa, maxRa };
            assert raRange[ 0 ] <= raRange[ 1 ];
        }

        /* If it does straddle the line RA=0, it's an excluded range. */
        else {
            double min = maxRa % ( 2 * Math.PI );
            double max = ( minRa + 2 * Math.PI ) % ( 2 * Math.PI );
            raRange = new double[] { max, min };
            assert raRange[ 0 ] >= raRange[ 1 ];
        }
        assert raRange[ 0 ] >= 0 && raRange[ 0 ] <= 2 * Math.PI;
        assert raRange[ 1 ] >= 0 && raRange[ 1 ] <= 2 * Math.PI;

        /* Return a sky box with the results. */
        return new SkyBox( raRange, decRange );
    }

    /**
     * Works out the minimum change in Right Ascension which will encompass
     * all points within a given search radius at a given central declination.
     *
     * @param   dec  declination of the centre of the search region
     *               in radians
     * @param   sr   radius of the search region in radians
     * @return  minimum change in radians of RA from the central value
     *          which will contain the entire search region
     */
    public static double calculateDeltaRa( double dec, double sr ) {

        /* Get the arc angle between the pole and the cone centre. */
        double hypArc = Math.PI / 2 - Math.abs( dec );

        /* If the search radius is greater than this, then all right
         * ascensions must be included. */
        if ( sr >= hypArc ) {
            return Math.PI;
        }

        /* In the more general case, we need a bit of spherical trigonometry.
         * Consider a right spherical triangle with one vertex at the pole,
         * one vertex at the centre of the search circle, and the right angle
         * vertex at the tangent between the search circle and a line of
         * longitude; then apply Napier's Pentagon.  The vertex angle at the
         * pole is the desired change in RA. */
        return Math.asin( Math.cos( Math.PI / 2 - sr ) / Math.sin( hypArc ) );
    }
}
