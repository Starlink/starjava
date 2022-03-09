// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import uk.ac.starlink.fits.FitsUtil;
import uk.ac.starlink.ttools.func.CoordsDegrees;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * Specialist display functions for use with the SuperCOSMOS survey.
 * These functions display cutout images from the various 
 * archives hosted at the SuperCOSMOS Sky Surveys
 * (<a href="http://www-wfau.roe.ac.uk/sss/">http://www-wfau.roe.ac.uk/sss/</a>).
 * In most cases these cover the whole of the southern sky.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Oct 2004
 */
public class SuperCosmos {

    /** Base URL for SuperCOSMOS image cutout service. */
    public static final String SSS_BASE_URL =
        "http://www-wfau.roe.ac.uk/~sss/cgi-bin/sss_topcat_pix.cgi";

    private static final Band UKST_B = new Band( 1, "UK Schmidt Blue" );
    private static final Band UKST_R = new Band( 2, "UK Schmidt Red" );
    private static final Band UKST_I = new Band( 3, "UK Schmidt Infrared" );
    private static final Band ESO_R = new Band( 4, "ESO Red" );
    private static final Band POSS_E = new Band( 5, "Palomar E" );
    private static final Band BLUE = new Band( "blue", "Blue" );
    private static final Band RED = new Band( "red", "Red" );

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.topcat.func" );

    /**
     * Private constructor prevents instantiation.
     */
    private SuperCosmos() {
    }

    /**
     * Displays a cutout image in one of the available bands from
     * the SuperCOSMOS Sky Surveys.
     * The displayed image is square, and <code>pixels</code> pixels in
     * the X and Y dimensions.  Pixels are approximately 0.67 arcsec square.
     * Sky coverage is complete.
     *
     * @param  ra  right ascension of image centre in degrees
     * @param  dec  declination of image centre in degrees
     * @param  pixels  dimension of the displayed image
     * @return  short log message
     */
    public static String sssShowCutout( double ra, double dec, int pixels ) {
        return sssCutout( ra, dec, pixels, null );
    }

    /**
     * Displays a cutout image of default size in one of the available
     * bands from the SuperCOSMOS Sky Surveys.
     * Sky coverage is complete.
     *
     * @param  ra  right ascension of image centre in degrees
     * @param  dec  declination of image centre in degrees
     * @return  short log message
     */
    public static String sssShowCutout( double ra, double dec ) {
        return sssShowCutout( ra, dec, 128 );
    }

    /**
     * Displays a cutout image of default size from one of the blue-band 
     * surveys from SuperCOSMOS.
     * Sky coverage is complete.
     *
     * @param  ra  right ascension of image centre in degrees
     * @param  dec  declination of image centre in degrees
     * @param  pixels  dimension of the displayed image
     * @return  short log message
     */
    public static String sssShowBlue( double ra, double dec, int pixels ) {
        return sssCutout( ra, dec, pixels, BLUE );
    }

    /**
     * Displays a cutout image of default size from one of the red-band 
     * surveys from SuperCOSMOS.
     * Sky coverage is complete.
     *
     * @param  ra  right ascension of image centre in degrees
     * @param  dec  declination of image centre in degrees
     * @param  pixels  dimension of the displayed image
     * @return  short log message
     */
    public static String sssShowRed( double ra, double dec, int pixels ) {
        return sssCutout( ra, dec, pixels, RED );
    }

    /**
     * Displays a cutout image taken from the SuperCOSMOS Sky Surveys
     * UK Schmidt Telescope Bj-band survey.
     * The displayed image is square, and <code>pixels</code> pixels in the
     * X and Y dimensions.  Pixels are approximately 0.67 arcsec square.
     *
     * <p>Sky coverage is -90&lt;Dec&lt;+2.5 (degrees).
     *
     * @param  ra  right ascension of image centre in degrees
     * @param  dec  declination of image centre in degrees
     * @param  pixels  dimension of the displayed image
     * @return  short log message
     */
    public static String sssShowUkstB( double ra, double dec, int pixels ) {
        return sssCutout( ra, dec, pixels, UKST_B );
    }

    /**
     * Displays a cutout image taken from the SuperCOSMOS Sky Surveys
     * UK Schmidt Telescope R-band survey.
     * The displayed image is square, and <code>pixels</code> pixels in the
     * X and Y dimensions.  Pixels are approximately 0.67 arcsec square.
     *
     * <p>Sky coverage is -90&lt;Dec&lt;+2.5 (degrees).
     *
     * @param  ra  right ascension of image centre in degrees
     * @param  dec  declination of image centre in degrees
     * @param  pixels  dimension of the displayed image
     * @return  short log message
     */
    public static String sssShowUkstR( double ra, double dec, int pixels ) {
        return sssCutout( ra, dec, pixels, UKST_R );
    }

    /**
     * Displays a cutout image taken from the SuperCOSMOS Sky Surveys
     * UK Schmidt Telescope I-band survey.
     * The displayed image is square, and <code>pixels</code> pixels in the
     * X and Y dimensions.  Pixels are approximately 0.67 arcsec square.
     *
     * <p>Sky coverage is -90&lt;Dec&lt;+2.5 (degrees).
     *
     * @param  ra  right ascension of image centre in degrees
     * @param  dec  declination of image centre in degrees
     * @param  pixels  dimension of the displayed image
     * @return  short log message
     */
    public static String sssShowUkstI( double ra, double dec, int pixels ) {
        return sssCutout( ra, dec, pixels, UKST_I );
    }

    /**
     * Displays a cutout image taken from the SuperCOSMOS Sky Surveys
     * ESO R-band survey.
     * The displayed image is square, and <code>pixels</code> pixels in the
     * X and Y dimensions.  Pixels are approximately 0.67 arcsec square.
     *
     * <p>Sky coverage is -90&lt;Dec&lt;+2.5 (degrees).
     *
     * @param  ra  right ascension of image centre in degrees
     * @param  dec  declination of image centre in degrees
     * @param  pixels  dimension of the displayed image
     * @return  short log message
     */
    public static String sssShowEsoR( double ra, double dec, int pixels ) {
        return sssCutout( ra, dec, pixels, ESO_R );
    }

    /**
     * Displays a cutout image taken from the SuperCOSMOS Sky Surveys
     * Palomar E-band survey.
     * The displayed image is square, and <code>pixels</code> pixels in the
     * X and Y dimensions.  Pixels are approximately 0.67 arcsec square.
     *
     * <p>Sky coverage is -20.5&lt;Dec&lt;+2.5 (degrees).
     *
     * @param  ra  right ascension of image centre in degrees
     * @param  dec  declination of image centre in degrees
     * @param  pixels  dimension of the displayed image
     * @return  short log message
     */
    public static String sssShowPossE( double ra, double dec, int pixels ) {
        return sssCutout( ra, dec, pixels, POSS_E );
    }

    /**
     * Displays a cutout image from the SuperCOSMOS Sky Surveys server.
     *
     * @param  ra  right ascension in degrees
     * @param  dec  declination in degrees
     * @param  pixels   image dimension in units of (approx) 0.67 arcsec
     * @param  waveband  waveband for image
     * @return  short log message
     */
    private static String sssCutout( double ra, double dec, int pixels,
                                     Band waveband ) {
        int irange = (int) ( pixels * 100.0 / 90.0 );
        double range = irange / 100.0;
        return sssCutout( ra, dec, "image/x-gfits", range, range, waveband );
    }

    /**
     * Displays a cutout image from the SuperCOSMOS Sky Surveys server.
     *
     * @param  ra  right ascension in degrees
     * @param  dec  declination in degrees
     * @param  mimeType  MIME type for return (though it doesn't seem to
     *         make any difference?)
     * @param  x   horizontal dimension of requested image in arcmin
     * @param  y   vertical dimension of requested image in arcmin
     * @param  waveband  waveband
     * @return   short log message
     */
    private static String sssCutout( double ra, double dec, String mimeType,
                                     double x, double y, Band waveband ) {
        final TopcatCgiQuery query = (TopcatCgiQuery)
             new TopcatCgiQuery( SSS_BASE_URL )
            .addArgument( "ra", CoordsDegrees.degreesToHms( ra, 5 ).trim() )
            .addArgument( "dec", CoordsDegrees.degreesToDms( dec, 4 ).trim() )
            .addArgument( "mime-type", mimeType )
            .addArgument( "x", x )
            .addArgument( "y", y );
        if ( waveband != null ) {
            query.addArgument( "waveband", waveband.id_ );
        }
        final String label = waveband == null 
                           ? "SuperCOSMOS Sky Surveys"
                           : "SuperCOSMOS Sky Surveys (" + waveband.name_ + ")";
        new Thread() {
            public void run() {
                try {
                    final File file = query.executeAsLocalFile( ".fits.gz" );
                    DataSource datsrc = new FileDataSource( file );
                    if ( FitsUtil.isMagic( datsrc.getIntro() ) ) {
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                Image.displayImage( label, file.toString() );
                            }
                        } );
                    }
                    else {
                        String msg = new String( datsrc.getIntro() );
                        msg = msg.replaceAll( "\\s+", " " )
                                 .replaceFirst( "<br>.*", "" );
                        logger.warning( msg );
                    }
                }
                catch ( IOException e ) {
                    logger.warning( e.toString() );
                }
            }
        }.start();
        return query.toString();
    }

    /**
     * Enumeration class for known wavebands.
     */
    private static class Band {
        final String id_;
        final String name_;
        Band( int id, String name ) {
            id_ = Integer.toString( id );
            name_ = name;
        }
        Band( String id, String name ) {
            id_ = id;
            name_ = name;
        }
    }

}
