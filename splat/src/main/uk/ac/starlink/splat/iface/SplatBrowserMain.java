/*
 * Copyright (C) 2000-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-SEP-200 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.io.File;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.Loader;

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

        //  Cause a load and/or guess of various properties that can
        //  be useful in locating resources etc.
        guessProperties();

        //  Make splash screen and interface visible. Do these from
        //  event threads as parts of GUI could be realized before
        //  returning (not thread safe). SplatSplash dies after a
        //  given time.
        final SplatSplash splashFrame = new SplatSplash();
        try {
            SwingUtilities.invokeAndWait( new Runnable() {
                    public void run()
                    {
                        // Sometimes this appears as an empty window!
                        splashFrame.repaint();
                        splashFrame.setVisible( true );
                        splashFrame.repaint();
                    }
                });
        }
        catch (Exception e) {
            e.printStackTrace();
        }

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
     * Load user properties and make guesses for any that are needed
     * and are not set.
     */
    private void guessProperties()
    {
        Loader.loadProperties();
        Properties props = System.getProperties();

        // Locate the line identifiers.
        File sdir = Loader.starjavaDirectory();
        if ( sdir != null ) {
            String stardir = sdir.toString() + File.separatorChar;
            if ( ! props.containsKey( "splat.etc" ) ) {
                props.setProperty( "splat.etc", stardir + "etc" );
            }
        }
        if ( props.containsKey( "splat.etc" ) ) {
            String etcdir = props.getProperty( "splat.etc" );
            props.setProperty( "splat.etc.ids", etcdir +
                               File.separatorChar + "splat" +
                               File.separatorChar + "ids" );
        }

        //  Web service defaults.
        if ( ! props.containsKey( "axis.EngineConfigFactory" ) ) {
            props.setProperty( "axis.EngineConfigFactory",
                  "uk.ac.starlink.soap.AppEngineConfigurationFactory" );
        }
        if ( ! props.containsKey( "axis.ServerFactory" ) ) {
            props.setProperty( "axis.ServerFactory",
                               "uk.ac.starlink.soap.AppAxisServerFactory" );
        }
 
        //  Load the proxy server configuration, if set.
        ProxySetup.getInstance().restore();
   }

    /**
     * Main method. Starting point for SPLAT application.
     * @param args list of input spectra
     */
    public static void main( String[] args )
    {
        new SplatBrowserMain( args );
    }
}
