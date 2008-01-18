package uk.ac.starlink.plastic;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * Lists can be generated using the construction "(item1, item2, ...)".
     * This application does not claim to make it possible to execute 
     * requests of arbitrary complexity from the command line.
     *
     * <p>If the <code>-targetName</code> or <code>-targetId</code> flags
     * are specified, then the request will only be sent to the 
     * listener(s) so identified.  Otherwise, it will be sent to all
     * registered listeners. 
     *
     * <h2>Flags</h2>
     * <dl>
     * <dt>-sync</dt>
     * <dd>Force synchronous operation (the default).</dd>
     * <dt>-async</dt>
     * <dd>Force asynchronous operation.</dd>
     * <dt>-targetName name</dt>
     * <dd>Specifies the application name of the app to which the request
     *     will be delivered.  May be given more than once.
     * <dt>-targetId id</dt>
     * <dd>Specifies the application ID of the app to which the request
     *     will be delivered.  May be given more than once.
     * <dt>-regName name</dt>
     * <dd>Specify the generic application name by which the reqestor 
     *     registers with the hub.</dd>
     * <dt>-clientId id</dt>
     * <dd>Specifies the ID of the client which claims to be sending the
     *     request.  In this case the request will not register with the
     *     hub prior to making the request.  You may not be making a 
     *     legal, decent, honest and truthful request if you use
     *     this flag.</dd>
     * <dt>--</dt>
     * <dd>Anything after this is not a flag, even if it looks like one.</dd>
     * </dl>
     */
    public static void main( String[] args ) throws IOException {

        String usage = "\nUsage:"
                     + "\n       "
                     + PlasticRequest.class.getName()
                     + "\n           "
                     + " [-sync|-async]"
                     + " [-regName name]"
                     + "\n           "
                     + " [-targetName name ...]"
                     + " [-targetId id ...]"
                     + " [-targetHub]"
                     + "\n           "
                     + " [-clientId id]"
                     + " [--]"
                     + "\n           "
                     + " messsageId"
                     + " [args ...]"
                     + "\n";

        String appName = "request";
        boolean sync = true;
        Set targetNameSet = new HashSet();
        List targetIdList = new ArrayList();
        boolean targetHub = false;
        URI spoofId = null;

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
            else if ( arg.equals( "-regName" ) && it.hasNext() ) {
                it.remove();
                appName = (String) it.next();
                it.remove();
            }
            else if ( arg.equals( "-targetName" ) && it.hasNext() ) {
                it.remove();
                targetNameSet.add( it.next() );
                it.remove();
            }
            else if ( arg.equals( "-targetHub" ) ) {
                it.remove();
                targetHub = true;
            }
            else if ( arg.equals( "-targetId" ) && it.hasNext() ) {
                it.remove();
                String uri = (String) it.next();
                it.remove();
                try {
                    targetIdList.add( new URI( uri ) );
                }
                catch ( URISyntaxException e ) {
                    throw (IllegalArgumentException)
                          new IllegalArgumentException( "Badly formed URI: " 
                                                      + uri )
                         .initCause( e );
                }
            }
            else if ( arg.equals( "-clientId" ) && it.hasNext() ) {
                it.remove();
                String uri = (String) it.next();
                it.remove();
                try {
                    spoofId = new URI( uri );
                }
                catch ( URISyntaxException e ) {
                    throw (IllegalArgumentException)
                          new IllegalArgumentException( "Badly formed URI: "
                                                      + uri )
                         .initCause( e );
                }
            }
            else if ( arg.startsWith( "-h" ) ) {
                it.remove();
                System.out.println( usage );
                return;
            }
            else if ( arg.startsWith( "-" ) ) {
                it.remove();
                System.err.println( usage );
                System.exit( 1 );
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
        URI id = spoofId == null
               ? hub.registerNoCallBack( appName )
               : spoofId;

        /* Identify the list of applications to which we will send. */
        List targetList;
        boolean sendAll;
        if ( targetNameSet.isEmpty() && targetIdList.isEmpty() && !targetHub ) {
            targetList = null;
        }
        else {
            targetList = new ArrayList();
            if ( targetHub ) {
                URI hubId = hub.getHubId();
                if ( hubId != null ) { 
                    targetList.add( hubId );
                }
                else {
                    throw new IOException( "No hub ID available" );
                }
            }
            targetList.addAll( targetIdList );
            if ( ! targetNameSet.isEmpty() ) {
                for ( Iterator it = hub.getRegisteredIds().iterator();
                      it.hasNext(); ) {
                    URI appId = (URI) it.next();
                    String name = hub.getName( appId );
                    if ( targetNameSet.contains( hub.getName( appId ) ) ) {
                        targetList.add( appId );
                    }
                }
            }
        }

        /* Execute the request. */
        try {
            if ( sync ) {
                Map map = targetList == null
                        ? hub.request( id, msgId, msgParams )
                        : hub.requestToSubset( id, msgId, msgParams,
                                               targetList );
                showMap( map, System.out );
            }
            else {
                if ( targetList == null ) {
                    hub.requestAsynch( id, msgId, msgParams );
                }
                else {
                    hub.requestToSubsetAsynch( id, msgId, msgParams,
                                               targetList );
                }
            }
        }

        /* Unregister. */
        finally {
            if ( spoofId == null ) {
                hub.unregister( id );
            }
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
            outList.add( decodeArg( (String) it.next() ) );
        }
        return outList;
    }

    /**
     * Turns a single string from the command line into a typed value
     * suitable for passing as an argument of a PLASTIC request.
     * Uses ad hoc guesses.
     *
     * @param  inArg  string
     * @return   object
     */
    private static Object decodeArg( String inArg ) {
        int leng = inArg.length();

        /* Forced string (single or double quoted). */
        if ( ( inArg.charAt( 0 ) == '"' &&
               inArg.charAt( leng - 1 ) == '"' ) ||
             ( inArg.charAt( 0 ) == '\'' &&
               inArg.charAt( leng - 1 ) == '\'' ) ) {
            return inArg.substring( 1, leng - 1 );
        }

        /* List (parenthesised and comma-separated). */
        else if ( ( inArg.charAt( 0 ) == '(' &&
                    inArg.charAt( leng - 1 ) == ')' ) ) {
            String[] items = inArg.substring( 1, leng - 1 )
                            .split( " *, *" );
            return decodeArgs( Arrays.asList( items ) );
        }

        /* Map (in curly brackets; {a=>b,c=>d,...}) */
        else if ( ( inArg.charAt( 0 ) == '{' &&
                    inArg.charAt( leng - 1 ) == '}' ) ) {
            if ( leng == 2 ) {
                return new HashMap();
            }
            String[] items = inArg.substring( 1, leng - 1 )
                            .split( " *, *" );
            Map map = new HashMap();
            boolean ok = true;
            for ( int i = 0; ok && ( i < items.length ); i++ ) {
                int sepPos = items[ i ].indexOf( "=>" );
                if ( sepPos > 0 ) {
                    Object key =
                        decodeArg( items[ i ].substring( 0, sepPos ) );
                    Object value =
                        decodeArg( items[ i ].substring( sepPos + 2 ) );
                    map.put( key, value );
                }
                else {
                    ok = false;
                }
            }
            return ok ? (Object) map : (Object) inArg;
        }

        /* Boolean. */
        else if ( "true".equals( inArg ) ) {
            return Boolean.TRUE;
        }
        else if ( "false".equals( inArg ) ) {
            return Boolean.FALSE;
        }

        /* Try integer. */
        else {
            try {
                return Integer.valueOf( inArg );
            }

            /* Try double. */
            catch ( NumberFormatException e1 ) {
                try {
                    return Double.valueOf( inArg );
                }
                catch ( NumberFormatException e2 ) {
                    return inArg;
                }
            }
        }
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
