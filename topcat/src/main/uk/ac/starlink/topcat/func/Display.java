// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import java.awt.Component;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.sog.SOG;
import uk.ac.starlink.sog.SOGNavigatorImageDisplay;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.PlotControlFrame;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.SplatBrowserMain;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;

/**
 * Functions for display of spectra and images in external viewer applications.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Aug 2004
 */
public class Display {

    private static SOG sog_;
    private static List soggers_ = new ArrayList();
    private static SplatBrowser splat_;
    private static Map splatSpectra_ = new HashMap();
    private static PlotControlFrame splatPlotFrame_;
    private static PlotControl splatPlot_;

    /**
     * Private constructor prevents instantiation.
     */
    private Display() {
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
            synchronized ( Display.class ) {
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

    /**
     * Displays the resource at a given location as a spectrum in a 
     * spectrum viewer program (SPLAT).
     * <code>loc</code> should be a filename pointing to a spectrum
     * in a format that SPLAT understands (includes FITS, NDF).
     * In some cases, a URL can be used too.
     *
     * @param   loc  spectrum location
     * @return  short log message
     * @see     <http://www.starlink.ac.uk/splat/>
     */
    public static String splat( String loc ) {
        return splatMulti( new String[] { loc } );
    }

    /**
     * Displays two spectra in the same (SPLAT) viewer.  This may be useful
     * to compare two spectra which correspond to the same table row.
     *
     * @param  loc1   location of the first spectrum
     * @param  loc2   location of the second spectrum
     * @return  short report message
     */
    public static String splat2( String loc1, String loc2 ) {
        return splatMulti( new String[] { loc1, loc2 } );
    }

    /**
     * Generic routine for displaying multiple spectra simultaneously in the
     * same SPLAT plot.
     * 
     * @param  locs  array of spectrum locations (file, or in some cases URL)
     * @return short report message
     */
    public static String splatMulti( String[] locs ) {

        /* Check that classes are available. */
        if ( ! TopcatUtils.canSplat() ) {
            return "Error: SPLAT classes not available";
        }
        SplatBrowser splat = getSplat();

        /* Get the SpecData objects corresponding to the locations we have. */
        int nsplat = locs.length;
        String[] msgs = new String[ nsplat ];
        SpecDataFactory splatFac = SpecDataFactory.getInstance();
        SpecDataComp specGroup = new SpecDataComp();
        for ( int i = 0; i < nsplat; i++ ) {
            String loc = locs[ i ];
            if ( loc == null || loc.trim().length() == 0 ) {
                msgs[ i ] = null;
            }
            else {
                loc = loc.trim();
                if ( ! splatSpectra_.containsKey( loc ) ) {
                    try {
                        SpecData spec = splatFac.get( loc );
                        splat_.addSpectrum( spec );
                        splatSpectra_.put( loc, spec );
                        specGroup.add( spec );
                        msgs[ i ] = loc;
                    }
                    catch ( Exception e ) {
                        splatSpectra_.put( loc, null );
                        msgs[ i ] = "<Error: " + e.getMessage() + ">";
                    }
                }
                else {
                    if ( loc != null ) {
                        SpecData spec = (SpecData) splatSpectra_.get( loc );
                        if ( spec != null ) {
                            msgs[ i ] =  loc;
                            specGroup.add( spec );
                        }
                        else {
                            msgs[ i ] = "<No data>";
                        }
                    }
                    else {
                        msgs[ i ] = "<No data>";
                    }
                }
            }
        }

        /* Work out the successful plot output message. */
        StringBuffer sbuf = new StringBuffer()
                           .append( "splat(" );
        for ( int i = 0; i < nsplat; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ',' );
            }
            sbuf.append( msgs[ i ] );
        }
        sbuf.append( ")" );
        String msg = sbuf.toString();

        /* If we don't already have a plot, get one now.  We initialize it
         * with the first spectrum for display, but this is just a dummy. */
        if ( splatPlot_ == null && specGroup.count() > 0 ) {
            synchronized ( Display.class ) {
                splatPlotFrame_ = splat.displaySpectrum( specGroup.get( 0 ) );
                splatPlot_ = splatPlotFrame_.getPlot();
            }
        }

        /* Configure the plot to contain the list of spectra for display. */
        if ( splatPlot_ != null ) {
            try {
                splatPlot_.setSpecDataComp( specGroup );
                splatPlot_.updateThePlot( null );
            }
            catch ( SplatException e ) {
                return "<SPLAT Error: " + e.getMessage() + ">";
            }
        }

        /* Ensure the plot window is visible. */
        if ( ! splatPlotFrame_.isShowing() ) {
            splatPlotFrame_.show();
        }

        /* Return the log message. */
        return msg;
    }

    /**
     * Returns a SplatBrowser window, creating one lazily if necessary.
     *
     * @return  splat top-level window
     */
    private static SplatBrowser getSplat() {
        if ( splat_ == null ) {
            synchronized ( Display.class ) {

                /* Application-level initialisations for SPLAT. */
                SplatBrowserMain.guessProperties();

                /* Create the browser. */
                splat_ = new SplatBrowser( true );

                /* But iconize this so that we see only the plots. */
                splat_.setExtendedState( splat_.ICONIFIED );
            }
        }
        splat_.setVisible( true );
        return splat_;
    }

}
