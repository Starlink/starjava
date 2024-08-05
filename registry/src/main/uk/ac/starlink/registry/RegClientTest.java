package uk.ac.starlink.registry;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Example standalone utility which uses the registry client code.
 * The main method allows you to make a registry query and prints 
 * a basic summary of the results.
 * Run it with the "-help" flag for usage information.
 *
 * @author   Mark Taylor
 */
public class RegClientTest {

    /**
     * Main method.  The "-help" flag will print usage information.
     */
    public static void main( String[] args )
            throws IOException, URISyntaxException {
        Map<String, URL> knownRegMap = getKnownRegMap();

        /* Construct usage message. */
        StringBuffer ubuf = new StringBuffer();
        ubuf.append( RegClientTest.class.getName() )
            .append( " [-e <reg-endpoint-url>" );
        for ( String nickname : knownRegMap.keySet() ) {
            ubuf.append( '|' )
                .append( nickname );
        }
        ubuf.append( ']' );
        ubuf.append( " [-v]" );
        ubuf.append( " [-adqls <adqls-query> | -keywords <keywords>]" );
        String usage = ubuf.toString();

        /* Set up default flag values. */
        URL endpoint = knownRegMap.get( "ag" );
        String adqls = "capability/@standardID = 'ivo://ivoa.net/std/SSA'";
        String keywords = "lockman 2mass";
        boolean verbose = false;

        /* Process command-line flags. */
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.equals( "-e" ) && it.hasNext() ) {
                it.remove();
                String e = it.next();
                it.remove();
                endpoint = knownRegMap.containsKey( e )
                         ? knownRegMap.get( e )
                         : new URI( e ).toURL();
            }
            else if ( arg.equals( "-adqls" ) && it.hasNext() ) {
                keywords = null;
                it.remove();
                adqls = it.next();
                it.remove();
            }
            else if ( arg.equals( "-keywords" ) && it.hasNext() ) {
                adqls = null;
                it.remove();
                keywords = it.next();
                it.remove();
            }
            else if ( arg.equals( "-v" ) ) {
                it.remove();
                verbose = true;
            }
            else if ( arg.startsWith( "-h" ) ) {
                System.out.println( "\n" + usage + "\n" );
                return;
            }
            else {
                System.err.println( "\n" + usage + "\n" );
                System.exit( 1 );
            }
        }

        /* Set up registry client. */
        SoapClient sclient = new SoapClient( endpoint );
        if ( verbose ) {
            sclient.setEchoStream( System.err );
        }
        BasicRegistryClient rclient = new BasicRegistryClient( sclient );

        /* Construct the SOAP request corresponding to the query that
         * we want to make. */
        SoapRequest req;
        if ( adqls != null ) {
            req = RegistryRequestFactory.adqlsSearch( adqls );
        }
        else if ( keywords != null ) {
            req = RegistryRequestFactory
                 .keywordSearch( keywords.split( "\\s+" ), false );
        }
        else {
            throw new AssertionError( "No search term" );
        }

        /* Make the request in such a way that the results are streamed. */
        Iterator<BasicResource> it = rclient.getResourceIterator( req );

        /* Print results. */
        while ( it.hasNext() ) {
            BasicResource res = it.next();
            BasicCapability[] caps = res.getCapabilities();
            System.out.println( formatResource( res ) );
            for ( int ic = 0; ic < caps.length; ic++ ) {
                System.out.println( "\t" + formatCapability( caps[ ic ] ) );
            }
        }
    }

    /**
     * Returns a one-line summary of a BasicResource.
     *
     * @param  res  resource
     * @return  string
     */
    private static String formatResource( BasicResource res ) {
        return new StringBuffer()
           .append( res.getIdentifier() )
           .append( "  ---  " )
           .append( res.getShortName() )
           .toString();
    }

    /**
     * Returns a one-line summary of a BasicCapability.
     *
     * @param  cap  capability
     * @return  string
     */
    private static String formatCapability( BasicCapability cap ) {
        return new StringBuffer()
            .append( cap.getStandardId() )
            .append( '\t' )
            .append( cap.getAccessUrl() )
            .toString();
    }

    /**
     * Returns a map containing nickname-&gt;service endpoint pairs
     * for some popular registries, for convenience.
     *
     * @return    map of nicknames to registry service endpoints
     */
    public static Map<String, URL> getKnownRegMap() {
        Map<String, URL> map = new HashMap<String, URL>();
        try {
            map.put( "ag",
                     new URI( "http://registry.astrogrid.org/"
                            + "astrogrid-registry/services/"
                            + "RegistryQueryv1_0" ).toURL() );
            map.put( "nvo",
                     new URI( "http://nvo.stsci.edu/vor10/"
                            + "ristandardservice.asmx" ).toURL() );
            map.put( "euro",
                     new URI( "http://registry.euro-vo.org/services/"
                            + "RegistrySearch" ).toURL() );
        }
        catch ( MalformedURLException | URISyntaxException e ) {
            throw (Error) new AssertionError( "do what?" ).initCause( e );
        }
        return map;
    }
}
