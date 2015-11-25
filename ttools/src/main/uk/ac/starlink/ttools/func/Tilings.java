// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import edu.jhu.htm.core.HTMException;
import edu.jhu.htm.core.HTMfunc;
import gov.fnal.eag.healpix.PixTools;
import javax.vecmath.Vector3d;

/**
 * Pixel tiling functions for the celestial sphere.
 *
 * @author   Mark Taylor
 * @since    6 Dec 2007
 */
public class Tilings {

    private static final PixTools pixTools_ = PixTools.getInstance();

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
     * @param   ra      right ascension in degrees
     * @param   dec     declination in degrees
     * @return   pixel index
     * @see      <a href="http://www.sdss.jhu.edu/htm/">HTM web site</a>
     */
    public static long htmIndex( int level, double ra, double dec ) {
        try {
            return HTMfunc.lookupId( ra, dec, level );
        }
        catch ( HTMException e ) {
            throw new IllegalArgumentException( e.getMessage() );
        }
    }

    /**
     * Gives the pixel index for a given sky position in the HEALPix 
     * NEST scheme.
     *
     * @param   k      resolution parameter - log to base 2 of nside
     * @param   ra     right ascension in degrees
     * @param   dec    declination in degrees
     * @return  pixel index
     * @see     <a href="http://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static long healpixNestIndex( int k, double ra, double dec ) {
        if ( k > 63 ) {
            throw new IllegalArgumentException( "k " + k + " too large" );
        }
        long nside = 1L << k;
        return pixTools_.vect2pix_nest( nside, toVector( ra, dec ) );
    }

    /**
     * Gives the pixel index for a given sky position in the HEALPix
     * RING scheme.
     *
     * @param   k      resolution parameter - log to base 2 of nside
     * @param   ra     right ascension in degrees
     * @param   dec    declination in degrees
     * @return  pixel index
     * @see     <a href="http://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static long healpixRingIndex( int k, double ra, double dec ) {
        if ( k > 63 ) {
            throw new IllegalArgumentException( "k " + k + " too large" );
        }
        long nside = 1L << k;
        return pixTools_.vect2pix_ring( nside, toVector( ra, dec ) );
    }

    /**
     * Gives the HEALPix resolution parameter suitable for a given pixel size.
     * This <code>k</code> value is the logarithm to base 2 of the 
     * Nside parameter.
     *
     * @param   pixelsize   pixel size in degrees
     * @return  HEALPix resolution parameter <code>k</code>
     * @see     <a href="http://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static int healpixK( double pixelsize ) {
        long nside = pixTools_.GetNSide( pixelsize * 60 * 60 );
        return (int) ( Math.log( nside ) / Math.log( 2 ) );
    }

    /**
     * Gives the approximate resolution in degrees for a given HEALPix
     * resolution parameter <code>k</code>
     * This <code>k</code> value is the logarithm to base 2 of the 
     * Nside parameter.
     *
     * @param   k  HEALPix resolution parameter <code>k</code>
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
     * @param   k  HEALPix resolution parameter <code>k</code>
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
     * @param   k  HEALPix resolution parameter <code>k</code>
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

    /**
     * Turns an RA, Dec sky position into a Vector3d as used by HEALPix
     * routines.
     *
     * @param   ra   right ascension in degrees
     * @param   dec  declincation in degrees
     * @return  vector representation of sky position
     * @see      <a href="http://www.sdss.jhu.edu/htm/">HTM web site</a>
     */
    private static Vector3d toVector( double ra, double dec ) {
        double theta = Math.PI * 0.5 - Math.toRadians( dec );
        return pixTools_.Ang2Vec( theta, Math.toRadians( ra ) );
    }
}
