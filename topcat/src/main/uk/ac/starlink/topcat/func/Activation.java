package uk.ac.starlink.topcat.func;

import java.awt.Component;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.sog.SOG;
import uk.ac.starlink.sog.SOGNavigatorImageDisplay;

/**
 * Class containing static methods suitable for use during point activation.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Aug 2004
 */
public class Activation {

    private static SOG sog_;
    private static List soggers_ = new ArrayList();
    
    /**
     * Outputs a string value to the user log.
     *
     * @param  val  value
     */
    public static String print( String val ) {
        return val;
    }

    /**
     * Outputs a numeric value to the user log.
     *
     * @param   val  value
     */
    public static String print( double val ) {

        /* Since this method is declared to take double, any numeric 
         * argument can be widened to fit in it, so we don't need to 
         * clutter up the public interface with lots of overloaded print()
         * methods.  The downcasting is done so that you don't get
         * extra decimal places when they didn't ought to be there. */
        if ( (double) (long) val == val ) {
            return Long.toString( (long) val );
        }
        else if ( (double) (float) val == val ) {
            return Float.toString( (float) val );
        }
        else {
            return Double.toString( val );
        }
    }

    /**
     * Displays the file at a given location as an image 
     * in a graphical (SoG) viewer.
     * <tt>val</tt> should be a filename or URL, pointing to an image in 
     * a format that SOG understands (this includes FITS, compressed FITS,
     * and NDFs).
     * 
     * @param  loc  image location
     * @return  short log message
     * @see  <http://www.starlink.ac.uk/sog/>
     */
    public static String sog( String loc ) {
        return multiSog( new String[] { loc } );
    }

    /**
     * Displays two files at given locations as images in two graphical
     * (SoG) viewers.  This may be useful to compare two images which 
     * correspond to the same table row.
     *
     * @param  loc1  location of first image
     * @param  loc2  location of second image
     * @see  #sog
     * @see  <http://www.starlink.ac.uk/sog/>
     */
    public static String sog2( String loc1, String loc2 ) {
        return multiSog( new String[] { loc1, loc2 } );
    }

    /**
     * Generic routine for displaying multiple images simultaneously in
     * SoG viewers.
     * 
     * @param  locs  array of image file locations
     * @return  short report message
     */
    private static String multiSog( String[] locs ) {
        if ( ! TopcatUtils.canSog() ) {
            return "Error: SOG classes not available";
        }
        int nsog = locs.length;
        SOGNavigatorImageDisplay[] sogs = getSoggers( nsog );
        String[] msgs = new String[ nsog ];
        for ( int i = 0; i < nsog; i++ ) {
            String loc = locs[ i ].trim();
            String msg;
            if ( loc == null || loc.length() == 0 ) {
                msg = null;
            }
            else {
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
     * @return  <tt>nsog</tt>-element array of SOG image displays
     */
    private static SOGNavigatorImageDisplay[] getSoggers( int nsog ) {
        assert TopcatUtils.canSog();
        if ( soggers_.size() < nsog ) {
            synchronized ( Activation.class ) {
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
        }
        return sogs;
    }

    public static String splat( Object val ) {
        if ( val == null ) {
            return "No SPLAT target";
        }
        else {
            return "splat " + val;
        }
    }

}
