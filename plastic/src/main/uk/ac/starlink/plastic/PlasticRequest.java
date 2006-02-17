package uk.ac.starlink.plastic;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.votech.plastic.PlasticHubListener;

/**
 * PLASTIC client designed for use from the command line which can execute
 * a request.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2006
 */
public class PlasticRequest {

    /**
     * Executes a simple PLASTIC request.
     * This method registers with the hub (if there is one), executes a
     * request, and unregisters again.  If the request is synchronous,
     * then the results are written to standard output.
     *
     * <h2>Usage</h2>
     * <pre>
     *    request [flags] messageId [args ...]
     * </pre>
     * The args are turned from strings into typed values in an ad hoc way -
     * things that look like doubles are turned into Doubles, things that
     * look like integers are turned into Integers, and the rest are Strings.
     * You can surround a literal in quotes to force stringiness.
     * This application does not claim to make it possible to execute 
     * requests of arbitrary complexity from the command line.
     *
     * <h2>Flags</h2>
     * <dl>
     * <dt>-sync</dt>
     * <dd>Force synchronous operation (the default).</dd>
     * <dt>-async</dt>
     * <dd>Force asynchronous operation.</dd>
     * <dt>-name name</dt>
     * <dd>Specify the generic application name by which the reqestor 
     *     registers with the hub.</dd>
     * </dl>
     */
    public static void main( String[] args ) throws IOException {

        String usage = "\nUsage: " + PlasticRequest.class.getName()
                     + "\n        "
                     + " [-sync|-async]"
                     + " [-name appName]"
                     + " messsageId"
                     + " [args ...]"
                     + "\n";

        String appName = "request";
        boolean sync = true;

        /* Parse flags. */
        List argList = new ArrayList( Arrays.asList( args ) );
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.length() == 0 || arg.charAt( 0 ) != '-' ) {
                break;
            }
            else if ( arg.equals( "--" ) ) {
                it.remove();
                break;
            }
            else if ( arg.startsWith( "-sync" ) ) {
                it.remove();
                sync = true;
            }
            else if ( arg.startsWith( "-async" ) ) {
                it.remove();
                sync = false;
            }
            else if ( arg.equals( "-name" ) && it.hasNext() ) {
                it.remove();
                appName = (String) it.next();
                it.remove();
            }
            else if ( arg.startsWith( "-h" ) ) {
                it.remove();
                System.out.println( usage );
                System.exit( 0 );
            }
        }
        if ( argList.isEmpty() ) {
            System.err.println( usage );
            System.exit( 1 );
        }

        /* Parse message URI and trailing arguments. */
        URI msgId;
        List msgParams;
        try {
            msgId = new URI( (String) argList.get( 0 ) );
            argList.remove( 0 );
            msgParams = decodeArgs( argList );
        }
        catch ( URISyntaxException e ) {
            System.err.println( "Bad message ID: " + argList.get( 0 ) );
            System.exit( 1 );
            throw new AssertionError();
        }
        catch ( IndexOutOfBoundsException e ) {
            System.err.println( usage );
            System.exit( 1 );
            throw new AssertionError();
        }

        /* Register with the hub. */
        PlasticHubListener hub = PlasticUtils.getLocalHub();
        URI id = hub.registerNoCallBack( appName );

        /* Execute the request. */
        try {
            if ( sync ) {
                showMap( hub.request( id, msgId, msgParams ), System.out );
            }
            else {
                hub.requestAsynch( id, msgId, msgParams );
            }
        }

        /* Unregister. */
        finally {
            hub.unregister( id );
        }
    }

    /**
     * Turns a list of strings from the command line into a list of 
     * typed values suitable for passing as the argument list of a 
     * PLASTIC request.  Uses ad hoc guesses.
     *
     * @param  inList  list of strings
     * @return   list of objects
     */
    private static List decodeArgs( List inList ) {
        List outList = new ArrayList();
        for ( Iterator it = inList.iterator(); it.hasNext(); ) {
            String inArg = (String) it.next();
            int leng = inArg.length();
            Object outArg;
            if ( ( inArg.charAt( 0 ) == '"' &&
                   inArg.charAt( leng - 1 ) == '"' ) ||
                 ( inArg.charAt( 0 ) == '\'' &&
                   inArg.charAt( leng - 1 ) == '\'' ) ) {
                outArg = inArg.substring( 1, leng - 2 );
            }
            else {
                try {
                    outArg = Double.valueOf( inArg );
                }
                catch ( NumberFormatException e1 ) {
                    try {
                        outArg = Integer.valueOf( inArg );
                    }
                    catch ( NumberFormatException e2 ) {
                        outArg = inArg;
                    }
                }
            }
            outList.add( outArg );
        }
        return outList;
    }

    /**
     * Formats an ID->result map for output in a human readable form.
     *
     * @param   map   result map
     * @param   out   destination stream
     */
    private static void showMap( Map map, PrintStream out ) {
        List keys = new ArrayList( map.keySet() );
        Collections.sort( keys );
        for ( Iterator it = keys.iterator(); it.hasNext(); ) {
            Object key = it.next();
            out.println( key + ": " );
            out.println( "    " + map.get( key ) );
        }
    }
}
