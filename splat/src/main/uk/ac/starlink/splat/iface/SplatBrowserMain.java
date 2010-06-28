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
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;

import org.astrogrid.samp.client.DefaultClientProfile;

import uk.ac.starlink.plastic.PlasticUtils;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.NameParser;
import uk.ac.starlink.splat.util.PlasticCommunicator;
import uk.ac.starlink.splat.util.SampCommunicator;
import uk.ac.starlink.splat.util.SplatCommunicator;
import uk.ac.starlink.splat.util.SplatException;
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
    // Logger.
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.splat.iface.SplatBrowserMain" );

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
             " [{-n,--ndaction} c{ollapse}||e{xtract}||v{ectorize}]" +
             " [{-d,--dispax} axis_index]" +
             " [{-s,--selectax} axis_index]" +
             " [{-c,--clear}]" +
             " [{-k,--keepcoords}]" +
             " [{--interop s{amp}||p{lastic}]" +
             " [{--hub,--exthub}]" +
             " [{--debuglevel} level]" +
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
        String interopType = null;
        Boolean intHub = null;
        Boolean extHub = null;
        Integer dispersionAxis = null;
        Integer selectAxis = null;
        Boolean clearPrefs = Boolean.FALSE;
        Boolean keepCoords = null;
        Boolean ignoreErrors = Boolean.FALSE;
        Integer debugLevel = new Integer( 0 );
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
            CmdLineParser.Option clear =
                parser.addBooleanOption( 'c', "clear" );
            CmdLineParser.Option keep =
                parser.addBooleanOption( 'k', "keepcoords" );
            CmdLineParser.Option ignore =
                parser.addBooleanOption( 'i', "ignore" );
            CmdLineParser.Option interop =
                parser.addStringOption( '\0', "interop" );
            CmdLineParser.Option hub =
                parser.addBooleanOption( '\0', "hub" );
            CmdLineParser.Option exthub =
                parser.addBooleanOption( '\0', "exthub" );
            CmdLineParser.Option debug =
                parser.addIntegerOption( '\0', "debuglevel" );

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

            //  Clear preferences.
            clearPrefs = (Boolean) parser.getOptionValue( clear );
            if ( clearPrefs == null ) {
                clearPrefs = Boolean.FALSE;
            }

            //  Keep existing coordinates, even when non-spectral.
            keepCoords = (Boolean) parser.getOptionValue( keep );

            //  Ignore certain error conditions.
            ignoreErrors = (Boolean) parser.getOptionValue( ignore );
            if ( ignoreErrors == null ) {
                ignoreErrors = Boolean.FALSE;
            }

            //  Interop communications type.
            interopType = (String) parser.getOptionValue( interop );

            //  Interop hub options.
            intHub = (Boolean) parser.getOptionValue( hub );
            extHub = (Boolean) parser.getOptionValue( exthub );

            //  Show log with more information.
            debugLevel = (Integer) parser.getOptionValue( debug );
            if ( debugLevel == null ) {
                debugLevel = new Integer( 0 );
            }

            //  Everything else should be spectra.
            spectraArgs = parser.getRemainingArgs();

            //  If a type hasn't been given then parse the first name and see
            //  if it follows the usual extension type conventions, otherwise
            //  switch on type guessing.
            if ( defaultType == null && spectraArgs.length > 0 ) {
                defaultType = guessType( spectraArgs[0] );
            }
        }

        //  Need final versions for use in thread.
        final String[] spectra = spectraArgs;
        final String type = defaultType;
        final String action = ndAction;
        final Integer dispax = dispersionAxis;
        final Integer selectax = selectAxis;
        final SplatCommunicator communicator =
            createCommunicator( interopType );

        if ( keepCoords != null ) {
            //  This needs to become the application default, before any
            //  spectra are loaded and SplatBrowser is realised.
            boolean searchCoords = keepCoords.equals( Boolean.FALSE );
            SplatBrowser.setPreference( "SplatBrowser_searchcoords",
                                        searchCoords );
        }

        //  Cause a load and/or guess of various properties that can
        //  be useful in locating resources etc.
        guessProperties( clearPrefs.booleanValue() );

        //  Set up logging options, by default local tweaks are applied that
        //  limit the amount of information coming out on the terminal.
        //  When level is greater than 0 we give out more information.
        int level = debugLevel.intValue();
        if ( level == 0 ) {
            tweakLogging();
        }
        else {

            //  Show all local log messages.
            Logger.getLogger( "uk.ac.starlink.splat" ).setLevel( Level.ALL );
            if ( level >= 2 ) {
                //  Local plus STARJAVA messages.
                Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.ALL );
                if ( level > 2 ) {
                    //  Everyone's messages.
                    Logger.getLogger( "" ).setLevel( Level.ALL );
                }
            }

            //  All handlers also need to respect this.
            java.util.logging.Handler handler[] =
                Logger.getLogger( "" ).getHandlers();
            if ( handler != null ) {
                for ( int i = 0; i < handler.length; i++ ) {
                    handler[i].setLevel( Level.ALL );
                }
            }
        }

        //  Make interface visible. Do this from an event thread as
        //  parts of the GUI could be realized before returning (not
        //  thread safe).
        final boolean checkjniast = ! ignoreErrors.booleanValue();
        SwingUtilities.invokeLater( new Runnable() {
                public void run()
                {
                    browser = new SplatBrowser( spectra, false, type,
                                                action, dispax, selectax,
                                                communicator );
                    browser.setVisible( true );
                    if ( checkjniast ) {
                        if ( ! ASTJ.isAvailable() ) {
                            System.out.println( "No JNIAST support, " +
                                                "no point in continuing" +
                                                " (--ignore 1 to ignore)" );
                            System.exit( 1 );
                        }
                    }
                }
            });

        //  Start hub if required.  This may take time, and it is not
        //  critical to the GUI startup, which is why it is done after
        //  the interface startup is dispatched.
        if ( Boolean.TRUE.equals( intHub ) || Boolean.TRUE.equals( extHub ) ) {
            boolean external = Boolean.TRUE.equals( extHub );
            String hubtype = external ? "external" : "internal";
            logger.info( "Starting " + hubtype + " hub ..." );
            try {
                communicator.startHub( external );
                logger.info( "Started " + hubtype + " hub successfully" );
            }
            catch (IOException e) {
                logger.log( Level.WARNING,
                            "Failed to start " + hubtype + " hub", e );
            }
        }
    }

    /**
     * Load user properties and make guesses for any that are needed
     * and are not set.
     */
    public static void guessProperties( boolean clearPrefs )
    {
        //  Clear application preferences, if requested. Need to do this for
        //  each package that stores preferences. Or start walking the tree.
        //  AFAIK only iface stores preferences.
        if ( clearPrefs ) {
            Preferences prefs =
                Preferences.userNodeForPackage( SplatBrowserMain.class );
            try {
                prefs.clear();
            }
            catch (Exception e) {
                System.err.println( e.getMessage() );
            }
        }

        //  Options that must be established before the UI is started.
        Loader.tweakGuiForMac();

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
     * Guess the type of a spectrum. Does this by seeing if the default rules
     * work, if not returns guess, otherwise returns null.
     */
    protected String guessType( String specspec )
    {
        String result = null;
        try {
            NameParser namer = new NameParser( specspec );
            if ( "UNKNOWN".equals( namer.getFormat() ) ) {
                result = "guess";
            }
            else if ( "NDF".equals( namer.getFormat() ) ) {
                //  Files without extensions default to this.
                if ( namer.isRemote() ) {
                    //  Remote file. If no extension then the best we can do
                    //  is guess. Remote NDFs should have their file extension
                    //  in the URL. We don't deal with HDS paths for remote
                    //  NDFs.
                    String ext = Utilities.getExtension( specspec );
                    if ( ! ".sdf".equals( ext ) ) {
                        result = "guess";
                    }
                }
                else {
                    //  If the file is local and doesn't exist, switch to
                    //  guessing.
                    if ( !namer.exists() ) {
                        result = "guess";
                    }
                }
            }
        }
        catch (SplatException e) {

            //  Standard naming techniques failed, must use guessing.
            result = "guess";
        }
        return result;
    }


    /**
     *  Tweak the logging. Switch off various classes from outputting
     *  to the console.
     */
    protected void tweakLogging()
    {
        // Make the SOAPSERVER package and related quieter.
        Logger.getLogger( "uk.ac.starlink.soapserver" )
            .setLevel( Level.SEVERE );
        Logger.getLogger( "org.mortbay" ).setLevel( Level.SEVERE );
        Logger.getLogger( "org.apache.axis.utils.JavaUtils" )
            .setLevel( Level.SEVERE );
        Logger.getLogger( "uk.ac.starlink.soap.AppHttpSOAPServer" )
            .setLevel( Level.SEVERE );

        // Preferences system issue.
        Logger.getLogger( "java.utils.prefs" ).setLevel( Level.SEVERE );

        // Table chatter when trying out types.
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.SEVERE );
        Logger.getLogger( "uk.ac.starlink.srb" ).setLevel( Level.SEVERE );
        Logger.getLogger( "uk.ac.starlink.util" ).setLevel( Level.SEVERE );

        // JSAMP gives too much INFO.
        Logger.getLogger( "org.astrogrid.samp" ).setLevel( Level.WARNING );
    }

    /**
     * Get a reference to the SplatBrowser being used.
     */
    public SplatBrowser getSplatBrowser()
    {
        return browser;
    }

    /**
     * Returns a new SplatCommunicator object to handle inter-applcation
     * communications.
     *
     * @param interopType "s(amp)" or "p(lastic)", otherwise a default is used
     */
    public static SplatCommunicator createCommunicator( String interopType ) {

        //  SAMP mode explicitly requested.
        if ( "s".equals( interopType ) ||
             "samp".equalsIgnoreCase( interopType ) ) {
            logger.info( "Using SAMP communications by request" );
            try {
                return new SampCommunicator();
            }
            catch (IOException e) {
                throw new RuntimeException( "Failed to start up SAMP", e );
            }
        }

        //  PLASTIC mode explicitly requested.
        else if ( "p".equals( interopType ) ||
                  "plastic".equalsIgnoreCase( interopType ) ) {
            logger.info( "Using PLASTIC communications by request" );
            return new PlasticCommunicator();
        }

        //  No explicit request.
        else {

            //  If SAMP hub is running, run in SAMP mode.
            if ( DefaultClientProfile.getProfile().isHubRunning() ) {
                logger.info( "SAMP hub running - run in SAMP mode" );
                try {
                    return new SampCommunicator();
                }
                catch (IOException e) {
                    logger.warning( "Failed to start SAMP hub: " + e );
                    logger.info( "Falling back to PLASTIC" );
                    return new PlasticCommunicator();
                }
            }

            //  If PLASTIC hub is running, run in PLASTIC mode.
            else if ( PlasticUtils.isHubRunning() ) {
                logger.info( "PLASTIC hub running - run in PLASTIC mode" );
                return new PlasticCommunicator();
            }

            //  Otherwise default to SAMP.
            else {
                SplatCommunicator comm;
                try {
                    comm = new SampCommunicator();
                }
                catch (IOException e) {
                    logger.warning( "Failed to start SAMP"
                                  + " - fall back to PLASTIC (" + e + ")" );
                    comm = new PlasticCommunicator();
                }
                logger.info( "Run in " + comm.getProtocolName()
                           + " mode by default" );
                return comm;
            }
        }
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
