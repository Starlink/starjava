/*
 * Copyright (C) 2000-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-SEP-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import jargs.gnu.CmdLineParser;

import java.io.File;
import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.ProxySetup;

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

    /** Command-line usage routine */
    private static void printUsage() 
    {
        System.err.println
            ( 
             "usage: " + 
             Utilities.getApplicationName() + 
             " [{-t,--type} type_string]" +
             " [{-n,--ndaction} c{ollapse}||e{xpand}||v{ectorize}]" +
             " [{-d,--dispax} axis_index]" +
             " [{-s,--selectax} axis_index]" +
             " [spectra1 spectra2 ...]" 
            );
    }

    /**
     * Create the main window adding any command-line spectra.
     * @param args list of input spectra
     */
    public SplatBrowserMain( String[] args )
    {
        String[] spectraArgs = null;
        String defaultType = null;
        String ndAction = null;
        Integer dispersionAxis = null;
        Integer selectAxis = null;
        if ( args != null && args.length != 0 && ! "".equals( args[0] ) ) {

            //  Parse the command-line.
            CmdLineParser parser = new CmdLineParser();
            CmdLineParser.Option type = parser.addStringOption( 't', "type" );
            CmdLineParser.Option ndaction = 
                parser.addStringOption( 'n', "ndaction" );
            CmdLineParser.Option dispax = 
                parser.addIntegerOption( 'd', "dispax" );
            CmdLineParser.Option selectax = 
                parser.addIntegerOption( 's', "selectax" );

            try {
                parser.parse( args );
            }
            catch ( CmdLineParser.OptionException e ) {
                System.err.println( e.getMessage() );
                printUsage();
                System.exit( 2 );
            }

            defaultType = (String) parser.getOptionValue( type );
            ndAction = (String) parser.getOptionValue( ndaction );

            //  The axes values need to really start at 0.
            dispersionAxis = (Integer) parser.getOptionValue( dispax );
            selectAxis = (Integer) parser.getOptionValue( selectax );
            if ( dispersionAxis != null ) {
                dispersionAxis = new Integer( dispersionAxis.intValue() - 1 );
            }
            if ( selectAxis != null ) {
                selectAxis = new Integer( selectAxis.intValue() - 1 );
            }

            //  Everything else should be spectra.
            spectraArgs = parser.getRemainingArgs();
            
            System.out.println( "defaultType = " + defaultType );
            System.out.println( "ndAction = " + ndAction );
            System.out.println( "dispersionAxis = " + dispersionAxis );
            System.out.println( "selectAxis = " + selectAxis );
            System.out.println( "spectra = " + spectraArgs );
        }

        //  Need final versions for use in thread.
        final String[] spectra = spectraArgs;
        final String type = defaultType;
        final String action = ndAction;
        final Integer dispax = dispersionAxis;
        final Integer selectax = selectAxis;

        //  Cause a load and/or guess of various properties that can
        //  be useful in locating resources etc.
        guessProperties();

        //  Make interface visible. Do this from an event thread as
        //  parts of the GUI could be realized before returning (not
        //  thread safe).
        SwingUtilities.invokeLater( new Runnable() {
                public void run()
                {
                    browser = new SplatBrowser( spectra, false, type,
                                                action, dispax, selectax );
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
