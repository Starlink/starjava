package uk.ac.starlink.splat.iface;


import javax.swing.SwingUtilities;

/**
 * Main class for the ASDAT browser application. This application
 * displays and controls a list of known spectra. These can be
 * displayed in their own window, or in an existing window with
 * other spectra. Various display properties of the spectrum can be
 * set using this browser (i.e. the line colour, width and style).
 *
 * @since $Date$
 * @since 27-SEP-2000
 * @author Peter W. Draper (Starlink, Durham University)
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public class SplatBrowserMain
{
    /**
     * Create the main window adding any command-line spectra.
     * @param args list of input spectra
     */
    public SplatBrowserMain( String[] args ) 
    {
        String[] realArgs = null;
        if ( args.length != 0 && ! "".equals( args[0] ) ) {
            realArgs = args;
        }
        final String[] spectra = realArgs;

        //  Make interface visible. Do from event thread as GUI could
        //  be realized before returning (not thread safe).
        Runnable doWork = new Runnable() {
                public void run() {
                    //try {
                    //UIManager.setLookAndFeel(new com.incors.plaf.kunststoff.KunststoffLookAndFeel());
                    //} catch (Exception e) {
                    //e.printStackTrace();
                    //}
                    SplatBrowser frame = new SplatBrowser( spectra );
                    frame.setVisible( true );
                }
            };
        SwingUtilities.invokeLater( doWork );
    }

    /**
     * Main method. Starting point for splat application.
     * @param args list of input spectra
     */
    public static void main( String[] args ) 
    {
        new SplatBrowserMain( args );
    }
}
