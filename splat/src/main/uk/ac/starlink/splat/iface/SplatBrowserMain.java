/*
 * Copyright (C) 2000-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-SEP-200 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import uk.ac.starlink.splat.util.Utilities;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Main class for the SPLAT, Spectral Analysis Tool, application.
 * This application displays and controls a list of known spectra.
 * These can be displayed in their own window, or in an existing window with
 * other spectra. Various display properties of the spectrum can be
 * set using this browser (i.e. the line colour, width and style).
 *
 * @author Peter W. Draper (Starlink, Durham University)
 * @version $Id$
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

        //  Make splash screen and interface visible. Do these from
        //  event threads as parts of GUI could be realized before
        //  returning (not thread safe). SplatSplash dies after a
        //  given time.
        final SplatSplash splashFrame = new SplatSplash();
        SwingUtilities.invokeLater( new Runnable() {
                public void run()
                {
                    splashFrame.setVisible( true );
                }
            });
        SwingUtilities.invokeLater( new Runnable() {
                public void run()
                {
                    SplatBrowser frame = new SplatBrowser( spectra );
                    frame.setVisible( true );
                    try {
                        splashFrame.setVisible( false );
                    }
                    catch (Exception e) {
                        // Do nothing, could have been disposed already.
                    }
                }
            });
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
