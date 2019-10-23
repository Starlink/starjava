package uk.ac.starlink.topcat.activate;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.function.Predicate;
import java.util.logging.Logger;
import uk.ac.starlink.topcat.ImageWindow;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.ttools.func.URLs;

/**
 * Activation type that acquires an image from the CDS Hips2fits service
 * and displays it in the basic image viewer.
 *
 * @author   Mark Taylor
 * @since    23 Oct 2019
 */
public class ViewHips2fitsActivationType implements ActivationType {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.activate" );

    public ViewHips2fitsActivationType() {
    }

    public String getName() {
        return "Display HiPS cutout";
    }

    public String getDescription() {
        return "Displays a cutout from a chosen HiPS survey."
             + " This uses the Hips2Fits service provided by CDS.";
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.getSkySuitability();
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        final String tcLabel = tinfo.getTopcatModel().toString();
        final Predicate<HipsSurvey> filter = new Predicate<HipsSurvey>() {
            public boolean test( HipsSurvey hips ) {
                return true;
            }
        };
        return new Hips2fitsConfigurator( tinfo, filter ) {
            ImageWindow imwin_;

            protected Outcome useHips( String hipsId,
                                       double raDeg, double decDeg,
                                       double fovDeg, int npix ) {
                if ( imwin_ == null ) {
                    imwin_ = new ImageWindow( null );
                }
                String url = URLs.hips2fitsUrl( hipsId, "png", raDeg, decDeg,
                                                fovDeg, npix );
                logger_.info( "View image " + url );
                final BufferedImage image;
                try {
                    image = ImageWindow.createImage( url, false );
                }
                catch ( IOException e ) {
                    return Outcome.failure( e );
                }
                if ( image == null ) {
                    return Outcome.failure( "No image " + url );
                }
                imwin_.setTitle( tcLabel + ": " + hipsId );
                imwin_.setImage( image );
                imwin_.resizeToFitImage();
                imwin_.makeVisible();
                return Outcome.success( url.toString() );
            }
        };
    }
}
