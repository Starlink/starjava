// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.topcat.ImageWindow;
import uk.ac.starlink.topcat.TopcatUtils;

/**
 * Functions for display of graphics-format images in a window.
 * Supported image formats include GIF, JPEG, PNG and FITS,
 * which may be compressed.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Oct 2004
 */
public class Image {

    private static List viewers_ = new ArrayList();

    /**
     * Private constructor prevents instantiation.
     */
    private Image() {
    }

    /**
     * Displays the file at the given location in an image viewer;
     * if SoG is available that will be used, otherwise a basic image window.
     * <code>loc</code> should be a filename or URL, pointing to an
     * image in one of the supported formats.
     *
     * @param  loc  image location
     * @return  short log message
     */
    public static String displayImage( String loc ) {
        if ( TopcatUtils.canSog() ) {
            return Sog.sog( loc );
        }
        else {
            return displayBasicImage( loc );
        }
    }

    /**
     * Displays two files at given locations as images in two image viewers;
     * if SoG is available that will be used, otherwise a basic image window.
     * This may be useful to compare two images which correspond to
     * the same table row.
     *
     * @param  loc1  location of first image
     * @param  loc2  location of second image
     * @return  short report message
     */
    public static String displayImage2( String loc1, String loc2 ) {
        if ( TopcatUtils.canSog() ) {
            return Sog.sog2( loc1, loc2 );
        }
        else {
            return displayBasicImage2( loc1, loc2 );
        }
    }

    /**
     * Generic routine for displaying multiple images simultaneously in
     * image viewers; 
     * if SoG is available that will be used, otherwise a basic image window.
     *
     * @param  locs  array of image file locations (file/URL)
     * @return  short log message
     */
    public static String displayImageMulti( String[] locs ) {
        if ( TopcatUtils.canSog() ) {
            return Sog.sogMulti( locs );
        }
        else {
            return displayBasicImageMulti( locs );
        }
    }

    /**
     * Displays the file at a given location in a basic Image Window.
     * <code>loc</code> should be a filename or URL, pointing to an
     * image in one of the supported formats.
     *
     * @param  loc  image location
     * @return  short log message
     */
    public static String displayBasicImage( String loc ) {
        return displayBasicImageMulti( new String[] { loc } );
    }

    /**
     * Displays two files at given locations as images in two basic 
     * Image Windows.
     * This may be useful to compare two images which correspond to
     * the same table row.
     *
     * @param  loc1  location of first image
     * @param  loc2  location of second image
     * @return  short report message
     */
    public static String displayBasicImage2( String loc1, String loc2 ) {
        return displayBasicImageMulti( new String[] { loc1, loc2 } );
    }

    /**
     * Generic routine for displaying multiple images simultaneously in
     * basic Image Windows.
     *
     * @param  locs  array of image file locations (file/URL)
     * @return  short log message
     */
    public static String displayBasicImageMulti( String[] locs ) {
        int nimage = locs.length;
        ImageWindow[] viewers = getViewers( nimage );
        String[] msgs = new String[ nimage ];
        for ( int i = 0; i < nimage; i++ ) {
            String loc = locs[ i ];
            String msg;
            if ( loc == null || loc.trim().length() == 0 ) {
                msg = null;
            }
            else {
                loc = loc.trim();
                viewers[ i ].setImage( loc );
                msg = "displayImage(\"" + loc + "\")";
            }
            msgs[ i ] = msg;
        }
        if ( nimage == 1 ) {
            return msgs[ 0 ];
        }
        else {
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 0; i < nimage; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( "; " );
                }
                sbuf.append( msgs[ i ] );
            }
            return sbuf.toString();
        }
    }

    /**
     * Returns an array of image viewers.  New ones are only created if
     * you're asking for more than you've asked for before.
     *
     * @param  nimage  number of viewers required
     * @return  <code>nimage</code>-element array of image windows
     */
    private static ImageWindow[] getViewers( int nimage ) {
        if ( viewers_.size() < nimage ) {
            synchronized ( Image.class ) {
                for ( int i = viewers_.size(); i < nimage; i++ ) {
                    viewers_.add( new ImageWindow( null ) );
                }
            }
        }
        ImageWindow[] viewers = new ImageWindow[ nimage ];
        for ( int i = 0; i < nimage; i++ ) {
            viewers[ i ] = (ImageWindow) viewers_.get( i );
            if ( ! viewers[ i ].isShowing() ) {
                viewers[ i ].setVisible( true );
            }
        }
        return viewers;
    }
}
