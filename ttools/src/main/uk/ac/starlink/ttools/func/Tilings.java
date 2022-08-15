// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import cds.healpix.Healpix;
import edu.jhu.htm.core.HTMException;
import edu.jhu.htm.core.HTMfunc;

/**
 * Pixel tiling functions for the celestial sphere.
 *
 * <p>The <code>k</code> parameter for the HEALPix functions is the
 * HEALPix order, which can be in the range 0&lt;=k&lt;=29.
 * This is the logarithm to base 2 of the HEALPix NSIDE parameter.
 * At order <code>k</code>, there are 12*4^k pixels on the sphere.
 *
 * @author   Mark Taylor
 * @since    6 Dec 2007
 * @see      <a href="http://www.skyserver.org/htm/">HTM web site</a>
 * @see      <a href="https://healpix.jpl.nasa.gov/">HEALPix web site</a>
 */
public class Tilings {

    /** Solid angle in steradians corresponding to 1 square degree. */
    public static final double SQDEG = ( Math.PI * Math.PI ) / ( 180. * 180. );

    /**
     * Private constructor prevents instantiation.
     */
    private Tilings() {
    }

    /**
     * Gives the HTM (Hierachical Triangular Mesh) pixel index for a given
     * sky position.
     *
     * @param   level   HTM level
     * @param   lon     longitude in degrees
     * @param   lat     latitude in degrees
     * @return   pixel index
     * @see      <a href="http://www.skyserver.org/htm/">HTM web site</a>
     */
    public static long htmIndex( int level, double lon, double lat ) {
        try {
            return HTMfunc.lookupId( lon, lat, level );
        }
        catch ( HTMException e ) {
            throw new IllegalArgumentException( e.getMessage() );
        }
    }

    /**
     * Gives the pixel index for a given sky position in the HEALPix 
     * NEST scheme.
     *
     * @param   k      HEALPix order (0..29)
     * @param   lon    longitude in degrees
     * @param   lat    latitude in degrees
     * @return  pixel index
     * @see     <a href="https://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static long healpixNestIndex( int k, double lon, double lat ) {
        return Healpix.getNestedFast( k )
              .hash( Math.toRadians( lon ), Math.toRadians( lat ) );
    }

    /**
     * Gives the pixel index for a given sky position in the HEALPix
     * RING scheme.
     *
     * @param   k      HEALPix order (0..29)
     * @param   lon    longitude in degrees
     * @param   lat    latitude in degrees
     * @return  pixel index
     * @see     <a href="https://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static long healpixRingIndex( int k, double lon, double lat ) {
        return healpixNestToRing( k, healpixNestIndex( k, lon, lat ) );
    }

    /**
     * Returns the longitude of the approximate center of the tile
     * with a given index in the HEALPix NEST scheme.
     *
     * <p>Note: the <code>index</code> parameter is 0-based,
     * unlike the table row index special token <code>$index</code>
     * (a.k.a. <code>$0</code>), which is 1-based.
     * So if the HEALpix index is implicitly determined by the table row,
     * the value <code>$index-1</code> should be used.
     *
     * @param   k      HEALPix order (0..29)
     * @param   index  healpix index
     * @return  longitude in degrees
     * @see     <a href="https://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static double healpixNestLon( int k, long index ) {
        return Math.toDegrees( Healpix.getNestedFast( k )
                                      .center( index )[ 0 ] );
    }

    /**
     * Returns the latitude of the approximate center of the tile
     * with a given index in the HEALPix NEST scheme.
     *
     * <p>Note: the <code>index</code> parameter is 0-based,
     * unlike the table row index special token <code>$index</code>
     * (a.k.a. <code>$0</code>), which is 1-based.
     * So if the HEALpix index is implicitly determined by the table row,
     * the value <code>$index-1</code> should be used.
     *
     * @param   k      HEALPix order (0..29)
     * @param   index  healpix index
     * @return  latitude in degrees
     * @see     <a href="https://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static double healpixNestLat( int k, long index ) {
        return Math.toDegrees( Healpix.getNestedFast( k )
                                      .center( index )[ 1 ] );
    }

    /**
     * Returns the longitude of the approximate center of the tile
     * with a given index in the HEALPix RING scheme.
     *
     * <p>Note: the <code>index</code> parameter is 0-based,
     * unlike the table row index special token <code>$index</code>
     * (a.k.a. <code>$0</code>), which is 1-based.
     * So if the HEALpix index is implicitly determined by the table row,
     * the value <code>$index-1</code> should be used.
     *
     * @param   k      HEALPix order (0..29)
     * @param   index  healpix index
     * @return  longitude in degrees
     * @see     <a href="https://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static double healpixRingLon( int k, long index ) {
        return healpixNestLon( k, healpixRingToNest( k, index ) );
    }

    /**
     * Returns the latitude of the approximate center of the tile
     * with a given index in the HEALPix RING scheme.
     *
     * <p>Note: the <code>index</code> parameter is 0-based,
     * unlike the table row index special token <code>$index</code>
     * (a.k.a. <code>$0</code>), which is 1-based.
     * So if the HEALpix index is implicitly determined by the table row,
     * the value <code>$index-1</code> should be used.
     *
     * @param   k      HEALPix order (0..29)
     * @param   index  healpix index
     * @return  latitude in degrees
     * @see     <a href="https://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static double healpixRingLat( int k, long index ) {
        return healpixNestLat( k, healpixRingToNest( k, index ) );
    }

    /**
     * Converts a healpix tile index from the NEST to the RING scheme
     * at a given order.
     *
     * <p>Note: the <code>nestIndex</code> parameter is 0-based,
     * unlike the table row index special token <code>$index</code>
     * (a.k.a. <code>$0</code>), which is 1-based.
     * So if the HEALpix index is implicitly determined by the table row,
     * the value <code>$index-1</code> should be used.
     *
     * @param   k      HEALPix order (0..29)
     * @param  nestIndex   pixel index in NEST scheme
     * @return  pixel index in RING scheme
     * @see     <a href="https://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static long healpixNestToRing( int k, long nestIndex ) {
        return Healpix.getNested( k ).toRing( nestIndex );
    }

    /**
     * Converts a healpix tile index from the RING to the NEST scheme
     * at a given order.
     *
     * <p>Note: the <code>ringIndex</code> parameter is 0-based,
     * unlike the table row index special token <code>$index</code>
     * (a.k.a. <code>$0</code>), which is 1-based.
     * So if the HEALpix index is implicitly determined by the table row,
     * the value <code>$index-1</code> should be used.
     *
     * @param   k      HEALPix order (0..29)
     * @param  ringIndex   pixel index in RING scheme
     * @return  pixel index in NEST scheme
     * @see     <a href="https://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static long healpixRingToNest( int k, long ringIndex ) {
        return Healpix.getNested( k ).toNested( ringIndex );
    }

    /**
     * Gives the HEALPix resolution parameter suitable for a given pixel size.
     * This <code>k</code> value (also variously known as order, level, depth)
     * is the logarithm to base 2 of the 
     * Nside parameter.
     *
     * @param   pixelsize   pixel size in degrees
     * @return  HEALPix order <code>k</code>
     * @see     <a href="https://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static int healpixK( double pixelsize ) {
        return Math.max( 0,
                         Healpix
                        .getBestStartingDepth( Math.toRadians( pixelsize ) ) );
    }

    /**
     * Gives the approximate resolution in degrees for a given HEALPix
     * resolution parameter <code>k</code>
     * This <code>k</code> value is the logarithm to base 2 of the 
     * Nside parameter.
     *
     * @param   k      HEALPix order (0..29)
     * @return  approximate angular resolution in degrees
     */
    public static double healpixResolution( int k ) {
        return Math.sqrt( healpixSqdeg( k ) ); 
    }

    /**
     * Returns the solid angle in steradians of each HEALPix pixel
     * at a given order.
     *
     * @example  <code>healpixSteradians(5) = 0.0010226538585904274</code>
     * @example  <code>4*PI/healpixSteradians(0) = 12.0</code>
     *
     * @param   k      HEALPix order (0..29)
     * @return  pixel size in steradians
     */
    public static double healpixSteradians( int k ) {
        return 4 * Math.PI / ( 12L << ( 2 * k ) );
    }

    /**
     * Returns the solid angle in square degrees of each HEALPix pixel
     * at a given order.
     *
     * @example  <code>healpixSqdeg(5) = 3.357174580844667</code>
     * @example  <code>round(12 * healpixSqdeg(0)) = 41253</code>
     *
     * @param   k      HEALPix order (0..29)
     * @return  pixel size in steradians
     */
    public static double healpixSqdeg( int k ) {
        return steradiansToSqdeg( healpixSteradians( k ) );
    }

    /**
     * Converts a solid angle from steradians to square degrees.
     *
     * <p>The unit sphere is 4*PI steradians = 360*360/PI square degrees.
     *
     * @example  <code>round(steradiansToSqdeg(4*PI)) = 41253</code>
     *
     * @param   sr   quantity in steradians
     * @return   quantity in sqare degrees
     */
    public static double steradiansToSqdeg( double sr ) {
        return sr / SQDEG;
    }

    /**
     * Converts a solid angle from square degrees to steradians.
     *
     * <p>The unit sphere is 4*PI steradians = 360*360/PI square degrees.
     *
     * @example  <code>round(sqdegToSteradians(41253)/PI) = 4</code> 
     *
     * @param sqdeg  quantity in square degrees
     * @return   quantity in steradians
     */
    public static double sqdegToSteradians( double sqdeg ) {
        return sqdeg * SQDEG;
    }

    /**
     * Gives the HTM <code>level</code> parameter suitable for a given 
     * pixel size.
     *
     * @param   pixelsize  required resolution in degrees
     * @return  HTM level parameter
     */
    public static int htmLevel( double pixelsize ) {

        /* HTM source code says this: 
         *     int lev =  5;
         *     double htmwidth = 2.8125;
         *     while (htmwidth > pixelsize  && lev < 25) {
         *         htmwidth /= 2;
         *         lev = lev+1;
         *     }
         *     if (htmwidth > degResolution) {error}
         */
        return (int) Math.ceil( 5 - Math.log( pixelsize / 2.8125 )
                                  / Math.log( 2 ) );
    }

    /**
     * Gives the approximate resolution in degrees for a given HTM depth level.
     *
     * @param   level  HTM depth
     * @return  approximate angular resolution in degrees
     */
    public static double htmResolution( int level ) {
        return 2.8125 * Math.pow( 2, 5 - level );
    }
}
