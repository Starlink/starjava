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
 * <p>
 * Note this entry point doesn't provide a splash screen see the
 * {@link uk.ac.starlink.splat.SplatMain} class if you want that
 * ability.
 *
 * @author Peter W. Draper (Starlink, Durham University)
 * @version $Id$
 */
public class SplatBrowserMain
{
    /**
     * Reference to the SplatBrowser that is created.
     */
    protected SplatBrowser browser = null;

    /**
     * Create the main window adding any command-line spectra.
     * @param args list of input spectra
     */
    public SplatBrowserMain( String[] args )
    {
        String[] realArgs = null;
        if ( args != null && args.length != 0 && ! "".equals( args[0] ) ) {
            realArgs = args;
        }
        final String[] spectra = realArgs;

        //  Cause a load and/or guess of various properties that can
        //  be useful in locating resources etc.
        guessProperties();

        //  Make interface visible. Do this from an event thread as
        //  parts of the GUI could be realized before returning (not
        //  thread safe).
        SwingUtilities.invokeLater( new Runnable() {
                public void run()
                {
                    browser = new SplatBrowser( spectra );
                    browser.setVisible( true );
                }
            });
    }

    /**
     * Load user properties and make guesses for any that are needed
     * and are not set.
     */
    public static void guessProperties()
    {
        Loader.loadProperties();
        Properties props = System.getProperties();

        // Locate the line identifiers.
        try {
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
        }
        catch (Exception e) {
            System.err.println( "Failed to load line identifiers" );
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
     * Get a reference to the SplatBrowser being used.
     */
    public SplatBrowser getSplatBrowser()
    {
        return browser;
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
