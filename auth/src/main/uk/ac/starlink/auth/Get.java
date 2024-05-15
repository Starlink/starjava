package uk.ac.starlink.auth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Retrieves a resource from a URL using the authentication system.
 *
 * <p>This utility may be useful on its own, but it also forms a template
 * for other applications that wish to use the AUTH package.
 *
 * <p>This class is used via its main() method.
 * For usage information, invoke it with the <code>-help</code> flag.
 *
 * @author   Mark Taylor
 * @since    15 May 2024
 */
public class Get {

    private static final int BUFSIZ = 16 * 1024;
    private static final Logger rootLogger_ = Logger.getLogger( "" );
    private static final Logger logger_ =
        Logger.getLogger( Get.class.getName() );

    public static void main( String[] args ) throws IOException  {
        if ( ! runMain( args ) ) {
            System.exit( 1 );
        }
    }

    /**
     * Does the work for the main method.
     *
     * @param  args  command-line arguments
     * @return   true for successful completion, false for error
     */
    public static boolean runMain( String[] args ) throws IOException {
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );

        /* Special undocumented flag to change the command name as cited in
         * the usage message.  This is not intended for normal users. */
        String cmdName = Get.class.getName();
        if ( argList.size() > 1 && "--cmd-name".equals( argList.get( 0 ) ) ) {
            argList.remove( 0 );
            cmdName = argList.remove( 0 );
        }

        /* Parse command-line arguments. */
        String usage = String.join( "\n   ",
           "",
           "Usage: " + cmdName + " [options...] <url>",
           "",
           "   -o, --output <file>           output to file",
           "   -u, --username <txt>|@<file>  supply username",
           "   -p, --password <txt>|@<file>  supply password",
           "   -c, --curl                    report equivalent curl command",
           "   -s, --show-secret             "
                                 + "include secret information in curl report",
           "   -n, --no-download             "
                                 + "don't actually read the resource",
           "   -v, --verbose                 "
                                 + "increase verbosity (may be repeated)",
           "   +v, --no-verbose              "
                                 + "decrease verbosity (may be repeated)",
           "   -h, --help                    display this message",
        "" );
        URL url = null;
        File outfile = null;
        boolean showCurl = false;
        boolean showSecret = false;
        boolean isDownload = true;
        String username = null;
        String password = null;
        int verbosity = 0;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( flagMatch( arg, "h", "help" ) ) {
                it.remove();
                System.err.println( usage );
                return true;
            }
            else if ( flagMatch( arg, "o", "output" ) && it.hasNext() ) {
                it.remove();
                outfile = new File( it.next() );
                it.remove();
            }
            else if ( flagMatch( arg, "c", "curl" ) ) {
                it.remove();
                showCurl = true;
            }
            else if ( flagMatch( arg, "n", "no-download" ) ) {
                it.remove();
                isDownload = false;
            }
            else if ( flagMatch( arg, "v", "verbose" ) ) {
                it.remove();
                verbosity++;
            }
            else if ( flagMatch( arg, "u", "username" ) && it.hasNext() ) {
                it.remove();
                System.setProperty( UserInterface.USERNAME_PROP, it.next() );
                it.remove();
            }
            else if ( flagMatch( arg, "p", "password" ) && it.hasNext() ) {
                it.remove();
                System.setProperty( UserInterface.PASSWORD_PROP, it.next() );
                it.remove();
            }
            else if ( "+v".equals( arg ) || "--no-verbose".equals( arg ) ) {
                it.remove();
                verbosity--;
            }
            else if ( flagMatch( arg, "s", "show-secret" ) ) {
                it.remove();
                showSecret = true;
            }
            else {
                try {
                    url = new URL( arg );
                    it.remove();
                }
                catch ( MalformedURLException e ) {
                    System.err.println( usage );
                    return false;
                }
            }
        }
        if ( url == null || ! argList.isEmpty() ) {
            System.err.println( usage );
            return false;
        }

        /* Configure logging. */
        int verbInt = Math.max( Level.ALL.intValue(),
                                Level.WARNING.intValue()
                                - verbosity *
                                  ( Level.WARNING.intValue() -
                                    Level.INFO.intValue() ) );
        Level verbLevel = Level.parse( Integer.toString( verbInt ) );
        Handler[] rootHandlers = rootLogger_.getHandlers();
        if ( rootHandlers.length > 0 ) {
            rootHandlers[ 0 ].setLevel( verbLevel );
        }
        rootLogger_.setLevel( verbLevel );

        /* Set up AuthManager for command-line use. */
        AuthManager manager = AuthManager.getInstance();
        UserInterface propsUi = UserInterface.getPropertiesUi();
        manager.setUserInterface( propsUi == null ? UserInterface.CLI
                                                  : propsUi );

        /* Connect to requested resource. */
        UrlConnector connector = null;  // default
        Redirector redirector = Redirector.DEFAULT;
        AuthConnection authConn =
            manager.makeConnection( url, connector, redirector );

        /* Report curl(1) equivalent if required. */
        if ( showCurl ) {
            AuthContext context = authConn.getContext();
            List<String> curlWords = new ArrayList<>();
            curlWords.add( "curl" );
            curlWords.addAll( Arrays.asList( context
                                            .getCurlArgs( url, showSecret ) ) );
            if ( outfile != null ) {
                curlWords.add( "-o" );
                curlWords.add( outfile.toString() );
            }
            curlWords.add( url.toString() );
            System.err.println( curlWords.stream()
                               .map( Get::shellEscape )
                               .collect( Collectors.joining( " " ) ) );
        }

        /* Copy resource contents to output. */
        if ( isDownload ) {
            long nbyte = 0;
            try ( InputStream in = authConn.getConnection().getInputStream() ) {
                OutputStream out = outfile == null
                                 ? System.out
                                 : new FileOutputStream( outfile );
                byte[] buf = new byte[ BUFSIZ ];
                for ( int n; ( n = in.read( buf ) ) > 0; ) {
                    out.write( buf, 0, n );
                    nbyte += n;
                }
                out.flush();
                if ( outfile != null ) {
                    out.close();
                }
                logger_.info( nbyte + " bytes written" );
            }
        }
        return true;
    }

    /**
     * Matches a supplied command-line argument with a flag name.
     *
     * @param   arg  supplied argument
     * @param   flagAlternatives  one or more forms of the flag,
     *                            without leading minus signs
     * @return  true iff arg consists of one or two minus signs followed by
     *          one of the supplied flag forms
     */
    private static boolean flagMatch( String arg, String... flagAlternatives ) {
        String baseArg;
        if ( arg.startsWith( "--" ) ) {
            baseArg = arg.substring( 2 );
        }
        else if ( arg.startsWith( "-" ) ) {
            baseArg = arg.substring( 1 );
        }
        else {
            return false;
        }
        return Arrays.stream( flagAlternatives )
                     .anyMatch( flag -> baseArg.equals( flag ) );
    }

    /**
     * Escapes a given text string to make it usable as a word in a
     * (generic) un*x shell-scripting language.
     * Implementation may not be bullet-proof.
     *
     * @param  txt  raw text
     * @return  escaped text
     */
    private static String shellEscape( String txt ) {
        txt = txt.trim().replaceAll( "\\s+", " " );
        if ( ! txt.matches( ".*[ $?'\"].*" ) ) {
            return txt;
        }
        if ( txt.indexOf( "'" ) < 0 ) {
            return "'" + txt + "'";
        }
        if ( txt.indexOf( '"' ) < 0 ) {
            return '"' + txt + '"';
        }
        return "'" + txt.replaceAll( "'", "'\"'\"'" ) + "'";
    }
}
