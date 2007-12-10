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

    private static final PixTools pixTools_ = new PixTools();

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
     * @param   nside  resolution parameter
     * @param   ra     right ascension in degrees
     * @param   dec    declination in degrees
     * @return  pixel index
     * @see     <a href="http://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static long healpixNestIndex( long nside, double ra, double dec ) {
        return pixTools_.vect2pix_nest( nside, toVector( ra, dec ) );
    }

    /**
     * Gives the pixel index for a given sky position in the HEALPix
     * RING scheme.
     *
     * @param   nside  resolution parameter
     * @param   ra     right ascension in degrees
     * @param   dec    declination in degrees
     * @return  pixel index
     * @see     <a href="http://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static long healpixRingIndex( long nside, double ra, double dec ) {
        return pixTools_.vect2pix_ring( nside, toVector( ra, dec ) );
    }

    /**
     * Gives the HEALPix <code>nside</code> parameter suitable for a given
     * pixel size.
     *
     * @param   pixelsize   pixel size in degrees
     * @return  HEALPix nside parameter
     * @see     <a href="http://healpix.jpl.nasa.gov/">HEALPix web site</a>
     */
    public static long healpixNside( double pixelsize ) {
        return pixTools_.GetNSide( pixelsize * 60 * 60 );
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
        return pixTools_.Ang2Vec( theta, ra );
    }
}
