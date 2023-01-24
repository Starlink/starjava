package uk.ac.starlink.topcat.activate;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import uk.ac.starlink.fits.FitsUtil;
import uk.ac.starlink.topcat.TopcatCgiQuery;
import uk.ac.starlink.topcat.func.Image;
import uk.ac.starlink.ttools.func.CoordsDegrees;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * Specialist functions for use with data from the 2MASS surveys.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Nov 2004
 */
public class TwoMass {

    public static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.topcat.func" );

    /** Base URL for the 2MASS image server cutout service. */
    public static final String TWOMASS_BASE_URL =
        "http://irsa.ipac.caltech.edu/cgi-bin/Oasis/2MASSImg/nph-2massimg";

    /**
     * Displays a cutout image from the 2MASS image server around a given point.
     *
     * @param  label  viewer identification label
     * @param  ra     right ascension in degrees
     * @param  dec    declination in degrees
     * @param  npix   number of pixels in X and Y dimensions (0.4 arcsec square)
     * @param  band   band identifier: one of 'H', 'J' or 'K'
     * @return   short log message
     */
    public static String showCutout2Mass( final String label,
                                          double ra, double dec,
                                          int npix, char band ) {
        final TopcatCgiQuery query = (TopcatCgiQuery)
             new TopcatCgiQuery( TWOMASS_BASE_URL )
            .addArgument( "objstr",
                          CoordsDegrees.degreesToHms( ra, 5 ).trim() + " " +
                          CoordsDegrees.degreesToDms( dec, 4 ).trim() )
            .addArgument( "size", npix )
            .addArgument( "band", 
                          String.valueOf( Character.toLowerCase( band ) ) );
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
                        logger.warning( "Non-FITS return from 2MASS " +
                                        "cutout server" );
                    }
                }
                catch ( IOException e ) {
                    logger.warning( e.toString() );
                }
            }
        }.start();
        return query.toString();
    }
}
