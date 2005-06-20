// NOTE: this class is currently unused - it contains functionality 
// which I started to write and didn't complete.  Although some of 
// the methods are known to work, logging seems tricky, so it may be
// that not everything does what you'd expect it to.

package uk.ac.starlink.topcat;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for setting logging levels.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Jun 2005
 * @see   java.util.logging
 */
public class LogConfig {

    private static final Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.util" );
    private static final Pattern ASSIGN_REGEX = 
        Pattern.compile( "\\s*(?:(\\w+(?:\\.\\w+)*)\\s*=)?" /* grp 1: name */
                       + "\\s*(\\w+)\\s*" );                /* grp 2: value */

    /**
     * Name of system property containing ({@link #setConfiguration}-style)
     * default logging configuration string.
     * @see   #initFromProperty
     */ 
    public static String CONFIG_PROP = "star.logconfig";

    /**
     * Sets the logging level(s) from a short string, the kind of thing
     * you might get from a command-line switch. 
     *
     * <p>The format of the string is a comma-separated list of zero or 
     * more directives, each of the form
     * <pre>
     *    [&lt;handlername&gt;=]&lt;level&gt;
     * </pre>
     * if &lt;handlername&gt; is omitted the root handler is assumed.
     * The &lt;level&gt; can be either one of the
     * {@link java.util.logging.Level} names (case-insensitive) 
     * or an equivalent integer value.
     *
     * @param   config   configure string
     * @throws  IllegalArgumentException  if <code>config</code> is of the
     *          wrong form
     */
    public static void setConfiguration( String config ) {

        /* Turn the configuration line into a serialized-properties-type
         * string suitable for feeding to the LogManager. */
        StringBuffer buf = new StringBuffer();
        for ( StringTokenizer stok = new StringTokenizer( config, "," );
              stok.hasMoreTokens(); ) {
            String token = stok.nextToken();
            Matcher matcher = ASSIGN_REGEX.matcher( token );
            if ( matcher.matches() ) {
                String domain = matcher.group( 1 );
                if ( domain == null ) {
                    domain = "";
                }
                String levid = matcher.group( 2 );
                Level level = Level.parse( levid.toUpperCase() );
                buf.append( domain )
                   .append( ".level" )
                   .append( " = " )
                   .append( level.toString() )
                   .append( '\n' );
            }
            else {
                throw new IllegalArgumentException( "Invalid log config " +
                                                    "string: " + config );
            }
        }
        String configProps = buf.toString();

        /* Preserve the handlers that the logging system knows about,
         * since the readConfiguration call wipes out this knowledge. */
        Logger rootLogger = Logger.getLogger( "" );
        Handler[] handlers = rootLogger.getHandlers();

        /* Do the reconfiguration. */
        try {
            InputStream propStream =
                new BufferedInputStream(
                    new ByteArrayInputStream( configProps.getBytes() ) );
            LogManager.getLogManager().readConfiguration( propStream );
        }
        catch ( IOException e ) {
            throw new AssertionError();
        }

        /* Restore the handlers. */
        for ( int i = 0; i < handlers.length; i++ ) {
            rootLogger.addHandler( handlers[ i ] );
        }

        /* Finally, log what we've done. */
        for ( StringTokenizer stok = new StringTokenizer( configProps, "\n" );
              stok.hasMoreTokens(); ) {
            logger_.config( stok.nextToken() );
        }
    }

    /**
     * Initializes logging from the value of the {@link #CONFIG_PROP} 
     * property if it is defined.  If it is not, and if neither of the
     * standard {@link java.util.logging.LogManager} control properties
     * (<code>java.util.logging.config.class</code>, 
     *  <code>java.util.logging.config.file</code>) are defined either,
     * then the logging system is initialized so that only messages
     * of 
     * <code>java.util.logging</code> properties are not defined either
     * (these
     * standard 
     */
    public static void initFromProperty() {
        String configLine = null;
        try {
            configLine = System.getProperty( CONFIG_PROP );
        }
        catch ( SecurityException e ) {
            logger_.info( "Can't get property " + CONFIG_PROP );
            return;
        }
        if ( configLine != null && configLine.trim().length() > 0 ) {
            try {
                setConfiguration( configLine );
            }
            catch ( IllegalArgumentException e ) {
                logger_.warning( "Illegal logging config string: " +
                                 configLine );
            }
        }
    }

    /**
     * Returns the stream which would be read for default configuration
     * under normal circumstances.
     *
     * @return    properties-style logging config stream
     */
    private static InputStream getDefaultPropertyStream() {
        try {

            /* This code mostly pinched from the LogManager source. */
            String fname =
                System.getProperty( "java.util.logging.config.file" );
            if ( fname == null ) {
                fname = System.getProperty( "java.home" );
                if ( fname != null ) {
                    File f = new File( fname, "lib" );
                    f = new File( f, "logging.properties" );
                    fname = f.getCanonicalPath();
                }
                else {
                    logger_.warning( "Can't find java.home ??" );
                }
            }
            if ( fname != null ) {
                return new FileInputStream( fname );
            }
        }
        catch ( SecurityException e ) {
            // never mind
        }
        catch ( IOException e ) {
            // never mind
        }
        return new ByteArrayInputStream( new byte[ 0 ] );
    }
}
