package uk.ac.starlink.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class AuthTest {

    public static final AuthScheme[] SCHEMES = AuthManager.DFLT_SCHEMES;

    public static AuthScheme getSchemeByName( String name ) {
        for ( AuthScheme scheme : SCHEMES ) {
            if ( scheme.getName().equalsIgnoreCase( name ) ) {
                return scheme;
            }
        }
        for ( AuthScheme scheme : SCHEMES ) {
            if ( scheme.getClass().getName().replaceFirst( "^.*[.]", "" )
                .equals( name ) ) {
                return scheme;
            }
        }
        return null;
    }

    public static ContextFactory getContextFactory( Challenge ch, URL url )
            throws BadChallengeException {
        for ( AuthScheme scheme : SCHEMES ) {
            ContextFactory cfact = scheme.createContextFactory( ch, url );
            if ( cfact != null ) {
                return cfact;
            }
        }
        return null;
    }

    public static void main( String[] args )
            throws IOException, BadChallengeException {
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        
        AuthScheme scheme0 = null;
        String username = null;
        String password = null;
        URL url = null;
        String usage = AuthTest.class.getName()
                     + " [-scheme <scheme>]"
                     + " [-user <username>]"
                     + " [-pass <password>]"
                     + " <url>";
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( "-scheme".equals( arg ) ) {
                it.remove();
                String sname = it.next();
                scheme0 = getSchemeByName( sname );
                it.remove();
                if ( scheme0 == null ) {
                    StringBuffer sbuf = new StringBuffer()
                        .append( "No such scheme " )
                        .append( sname )
                        .append( ", options are:" );
                    for ( AuthScheme sch : SCHEMES ) {
                        sbuf.append( " " )
                            .append( sch.getName() );
                    }
                    System.err.println( sbuf.toString() );
                    System.exit( 1 );
                }
            }
            else if ( "-user".equals( arg ) ) {
                it.remove();
                username = it.next();
                it.remove();
            }
            else if ( arg.startsWith( "-pass" ) ) {
                it.remove();
                password = it.next();
                it.remove();
            }
            else if ( "-url".equals( arg ) ) {
                it.remove();
                url = new URL( it.next() );
                it.remove();
            }
            else if ( arg.charAt( 0 ) == '-' ) {
                System.err.println( usage );
                System.exit( 1 );
            }
        }
        if ( argList.size() > 0 && url == null ) {
            url = new URL( argList.remove( 0 ) );
        }
        if ( argList.size() > 0 || url == null ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        UserInterface ui = username != null && password != null
                         ? UserInterface.createFixed( username, password )
                         : UserInterface.CLI;

        URLConnection conn = url.openConnection();
        if ( conn instanceof HttpURLConnection ) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            hconn.connect();
            int code = hconn.getResponseCode();
            System.out.println( code + " " + hconn.getResponseMessage() );
            if ( code == 401 ) {
                Challenge[] challenges = AuthUtil.getChallenges( hconn );
                Challenge ch0 = null;
                ContextFactory cfact = null;
                if ( scheme0 != null ) {
                    for ( Challenge ch : challenges ) {
                        if ( cfact == null ) {
                            cfact = scheme0.createContextFactory( ch, url );
                            if ( cfact != null ) {
                                ch0 = ch;
                            }
                        }
                    }
                }
                else {
                    ch0 = challenges[ 0 ];
                    cfact = getContextFactory( ch0, url );
                }
                for ( Challenge ch : challenges ) {
                    if ( ch == ch0 ) {
                        System.out.print( "      *" );
                    }
                    System.out.println( "\t" + ch );
                }
                if ( cfact == null ) {
                    System.err.println( "No auth for challenge " + ch0 );
                }
                AuthContext context = cfact.createContext( ui );
                if ( context != null ) {
                    HttpURLConnection hc2 =
                        (HttpURLConnection) url.openConnection();
                    context.configureConnection( hc2 );
                    hc2.connect();
                    System.out.println( hc2.getResponseCode() + " "
                                      + hc2.getResponseMessage() );
                }
                else {
                    System.out.println( "No context" );
                }
            }
        }
    }
}
