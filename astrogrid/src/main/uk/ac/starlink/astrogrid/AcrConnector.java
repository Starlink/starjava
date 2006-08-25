package uk.ac.starlink.astrogrid;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import uk.ac.starlink.connect.AuthKey;
import uk.ac.starlink.connect.Connection;
import uk.ac.starlink.connect.Connector;

/**
 * Connector which connects to MySpace using the ACR
 * (Astronomy/Astrogrid Client Runtime) server written by Noel Winstanley.
 * This currently uses the XML-RPC interface to the ACR, since this
 * is presumed to make these classes less vulnerable to changes in
 * the ACR interface, and we're only using a small amount of the
 * functionality.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2005
 * @see  <a href="http://www.astrogrid.org/maven/docs/HEAD/desktop/multiproject/acr-interface/apidocs/index.html">ACR</a>
 */
public class AcrConnector implements Connector {

    /**
     * Boolean-valued property which determines whether MySpace directories
     * are to be cached.  Setting this true will probably lead to 
     * incorrect behaviour (directories not being updated with new files)
     * but may be worthwhile as a workaround for the atrocious performance
     * exhibited by MySpace at time of writing (April 2006).
     */
    public static final String CACHE_PROPERTY = "myspace.cache";

    private static Icon icon_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.astrogrid" );

    public String getName() {
        return "MySpace";
    }

    /**
     * Returns an empty array.  This connector needs no authentication,
     * since any authentication which has to be done is taken care of
     * by the ACR itself at the other end of a remote connection.
     *
     * @return  empty key array
     */
    public AuthKey[] getKeys() {
        return new AuthKey[ 0 ];
    }

    public Icon getIcon() {
        if ( icon_ == null ) {
            URL url = getClass().getResource( "AGlogo.gif" );
            if ( url != null ) {
                icon_ = new ImageIcon( url );
            }
        }
        return icon_;
    }

    public Connection logIn( Map authValues ) throws IOException {
        return logIn();
    }

    /**
     * Attempts to open a connection.
     *
     * @return  a live connection object
     * @throws  IOException  if there was some error,
     *          for instance authorization failure
     */
    public Connection logIn() throws IOException {
        AcrConnection connection = new AcrConnection( this );
        boolean cache = getCacheDirectories();
        if ( cache ) {
            logger_.warning( "Caching MySpace directories - better performance "
                           + "but may give out of date information" );
        }
        connection.setCacheDirectories( cache );
        return connection;
    }

    /**
     * Determines whether directories should be cached by default in
     * connections produced by this connector.
     * Result is based on the (boolean) value of the 
     * {@link #CACHE_PROPERTY} property.
     * If the property is unset, it will return false.
     *
     * @return  true  iff directories should be cached by default
     */
    public static boolean getCacheDirectories() {
        try {
            return Boolean.valueOf( System.getProperty( CACHE_PROPERTY ) )
                          .booleanValue();
        }
        catch ( Throwable e ) {
            return false;
        }
    }

    /**
     * Utility method which calls one of the ACR services using XML-RPC
     * and prints the result to standard output.
     * The first argument is the fully-qualified name of the service,
     * and any subsequent ones are arguments to it.
     *
     * @param  args  argument
     */
    public static void main( String[] args ) throws Exception {
        AcrConnection conn = (AcrConnection) new AcrConnector()
                                            .logIn( new HashMap() );
        Object[] acrArgs = new Object[ args.length - 1 ];
        System.arraycopy( args, 1, acrArgs, 0, args.length - 1 );
        Object result = conn.execute( args[ 0 ], acrArgs );
        if ( result instanceof Object[] ) {
            Object[] results = (Object[]) result;
            for ( int i = 0; i < results.length; i++ ) {
                Object res = results[ i ];
                System.out.print( "   " + ( i + 1 ) + ": " + res );
                if ( res != null ) {
                    System.out.print( "  (" + res.getClass().getName() + ")" );
                }
                System.out.println();
            }
        }
        else {
            System.out.print( "   " + result );
            if ( result != null ) {
                System.out.print( "  (" + result.getClass().getName() + ")" );
            }
            System.out.println();
        }
    }
}
