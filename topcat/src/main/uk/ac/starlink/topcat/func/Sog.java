// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.sog.SOG;
import uk.ac.starlink.sog.SOGNavigatorImageDisplay;
import uk.ac.starlink.topcat.TopcatUtils;

/**
 * Functions for display of images in external viewer SOG.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Aug 2004
 */
public class Sog {

    private static SOG sog_;
    private static List soggers_ = new ArrayList();

    /**
     * Private constructor prevents instantiation.
     */
    private Sog() {
    }

    /**
     * Displays the file at a given location as an image
     * in a graphical (SoG) viewer.
     * <code>loc</code> should be a filename or URL, pointing to an image in
     * a format that SOG understands (this includes FITS, compressed FITS,
     * and NDFs).
     *
     * @param  loc  image location
     * @return  short log message
     * @see  <http://www.starlink.ac.uk/sog/>
     */
    public static String sog( String loc ) {
        return sogMulti( new String[] { loc } );
    }

    /**
     * Displays two files at given locations as images in two graphical
     * (SoG) viewers.  This may be useful to compare two images which
     * correspond to the same table row.
     *
     * @param  loc1  location of first image
     * @param  loc2  location of second image
     * @return  short report message
     * @see  #sog
     * @see  <http://www.starlink.ac.uk/sog/>
     */
    public static String sog2( String loc1, String loc2 ) {
        return sogMulti( new String[] { loc1, loc2 } );
    }

    /**
     * Generic routine for displaying multiple images simultaneously in
     * SoG viewers.
     *
     * @param  locs  array of image file locations (file/URL)
     * @return  short report message
     */
    public static String sogMulti( String[] locs ) {
        if ( ! TopcatUtils.canSog() ) {
            return "Error: SOG classes not available";
        }
        int nsog = locs.length;
        SOGNavigatorImageDisplay[] sogs = getSoggers( nsog );
        String[] msgs = new String[ nsog ];
        for ( int i = 0; i < nsog; i++ ) {
            String loc = locs[ i ];
            String msg;
            if ( loc == null || loc.trim().length() == 0 ) {
                msg = null;
            }
            else {
                loc = loc.trim();
                try {
                    sogs[ i ].setFilename( loc, false );
                    msg = "sog(\"" + loc + "\")";
                }
                catch ( Exception e ) {
                    msg = "<Error: " + e.getMessage() + ">";
                }
            }
            msgs[ i ] = msg;
        }
        if ( nsog == 1 ) {
            return msgs[ 0 ];
        }
        else {
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 0; i < nsog; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( "; " );
                }
                sbuf.append( msgs[ i ] );
            }
            return sbuf.toString();
        }
    }

    /**
     * Returns an array of SOG viewers.  New ones are only created if
     * you're asking for more than you've asked for before.
     *
     * @param  nsog  number of viewers required
     * @return  <code>nsog</code>-element array of SOG image displays
     */
    private static SOGNavigatorImageDisplay[] getSoggers( int nsog ) {
        assert TopcatUtils.canSog();
        if ( soggers_.size() < nsog ) {
            synchronized ( Sog.class ) {
                if ( sog_ == null ) {
                    sog_ = new SOG();
                }
                for ( int i = soggers_.size(); i < nsog; i++ ) {
                    soggers_.add( (SOGNavigatorImageDisplay)
                                  sog_.getImageDisplay() );
                }
            }
        }
        SOGNavigatorImageDisplay[] sogs = new SOGNavigatorImageDisplay[ nsog ];
        for ( int i = 0; i < nsog; i++ ) {
            sogs[ i ] = (SOGNavigatorImageDisplay) soggers_.get( i );
            if ( ! sogs[ i ].isShowing() ) {
                sogs[ i ].setVisible( true );
            }
        }
        return sogs;
    }
}

