// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.PlotControlFrame;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.SplatBrowserMain;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.topcat.TopcatUtils;

/**
 * Functions for display of spectra in the external viewer SPLAT.
 *
 * @author  Mark Taylor (Starlink)
 */
public class Splat {

    private static SplatBrowser splat_;
    private static Map splatSpectra_ = new HashMap();
    private static Map splatPlotFrames_ = new HashMap();

    /**
     * Private constructor prevents instantiation.
     */
    private Splat() {
    }

    /**
     * Displays the resource at a given location as a spectrum in a
     * spectrum viewer program (SPLAT).
     * 
     * <code>label</code> may be any string which identifies the window
     * for display, so that multiple (sets of) spectra may be displayed 
     * in different
     * windows without getting in each others' way.
     * <code>loc</code> should be a filename pointing to a spectrum
     * in a format that SPLAT understands (includes FITS, NDF).
     * In some cases, a URL can be used too.
     *
     * @param   label  identifies the window in which the spectrum
     *          will be displayed
     * @param   loc  spectrum location
     * @return  short log message
     * @see     <http://www.starlink.ac.uk/splat/>
     */
    public static String splat( String label, String loc ) {
        return splatMulti( label, new String[] { loc } );
    }

    /**
     * Displays two spectra in the same (SPLAT) viewer.  This may be useful
     * to compare two spectra which correspond to the same table row.
     *
     * @param  label  identifies the window in which the spectra will be
     *         displayed
     * @param  loc1   location of the first spectrum
     * @param  loc2   location of the second spectrum
     * @return  short report message
     */
    public static String splat2( String label, String loc1, String loc2 ) {
        return splatMulti( label, new String[] { loc1, loc2 } );
    }

    /**
     * Generic routine for displaying multiple spectra simultaneously in the
     * same SPLAT plot.
     *
     * @param  label  identifies the window in which the spectra will be
     *         displayed
     * @param  locs  array of spectrum locations (file, or in some cases URL)
     * @return short report message
     */
    public static String splatMulti( String label, String[] locs ) {

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
                        splat.addSpectrum( spec );
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

        /* Get the plot window within which to do the display. */
        if ( specGroup.count() > 0 ) {
            PlotControlFrame plotFrame = getPlotFrame( label, specGroup );
            PlotControl splatPlot = plotFrame.getPlot();

            /* Configure the plot to contain the list of spectra for display. */
            if ( splatPlot != null ) {
                try {
                    splatPlot.setSpecDataComp( specGroup );
                    splatPlot.updateThePlot( null );
                }
                catch ( SplatException e ) {
                    return "<SPLAT Error: " + e.getMessage() + ">";
                }
            }

            /* Ensure the plot window is visible. */
            if ( ! plotFrame.isShowing() ) {
                plotFrame.show();
            }
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
            synchronized ( Splat.class ) {

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

    /**
     * Returns a labelled plot control frame.
     *
     * @param  label  label of display window
     * @param  specGroup  group of spectra to be displayed (not always used)
     * @return  new or old frame in which <tt>specGroup</tt> can be displayed
     */
    private static PlotControlFrame getPlotFrame( String label,
                                                  SpecDataComp specGroup ) {
        if ( ! splatPlotFrames_.containsKey( label ) ) {
            PlotControlFrame frame =
                getSplat().displaySpectrum( specGroup.get( 0 ) );
            splatPlotFrames_.put( label, frame );
        }
        return (PlotControlFrame) splatPlotFrames_.get( label );
    }

}
