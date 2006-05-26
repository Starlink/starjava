package uk.ac.starlink.plastic;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.util.TestCase;

public class PlasticTest extends TestCase {

    public static boolean VERBOSE = false;

    private static final URI CALC = PlasticUtils.createURI( "msg:calc" );
    private static final String PLUS = "plus";
    private static final String MINUS = "minus";
    private static File configFile =
        new File( System.getProperty( "user.home" ),
                  PlasticHubListener.PLASTIC_CONFIG_FILENAME );
    private final Object lock_ = new Object();

    static {
        Logger.getLogger( "uk.ac.starlink.plastic" ).setLevel( Level.WARNING );
    }

    public PlasticTest( String name ) {
        super( name );
    }

    public void testHub() throws IOException, InterruptedException {
        PlasticHub jvmHub = configFile.exists()
            ? null
            : PlasticHub.startHub( VERBOSE ? System.out : null );
        if ( jvmHub != null ) {
            exerciseHub( jvmHub );
        }

        PlasticHubListener rmiHub = PlasticUtils.getLocalHub();
        exerciseHub( rmiHub );
        PlasticHubListener xmlrpcHub =
            new XmlRpcHub( PlasticUtils.getXmlRpcUrl(), rmiHub );
        exerciseHub( xmlrpcHub );

        if ( jvmHub != null ) {
            assertTrue( configFile.exists() );
            jvmHub.stop();
            assertTrue( ! configFile.exists() );
        }
    }

    private void exerciseHub( PlasticHubListener hub ) 
            throws IOException, InterruptedException {
        URI id = hub.registerNoCallBack( "test-driver" );
        try {
            exerciseHub( hub, id );
        }
        finally {
            hub.unregister( id );
        }
    }

    private void exerciseHub( PlasticHubListener hub, URI id ) 
            throws IOException, InterruptedException {

        /* Get a list of applications which is listening to all messages.
         * This may be non-empty if the hub already exists with some 
         * monitoring listeners registered.  We will need this to elimintate
         * some returned messages from the hub later. */
        List monitors =
            hub.getMessageRegisteredIds( PlasticUtils
                                        .createURI( "not-a-real-message" ) );

        assertTrue( configFile.exists() );
        Counter c1 = new Counter();
        Counter c2 = new Counter();
        Counter c3 = new Counter();
        URI id1 = PlasticUtils.registerRMI( c1 );
  
        Object soleId = 
            removeMonitorsList( hub.getMessageRegisteredIds( CALC ),
                                monitors ).get( 0 );
        assertEquals( id1, soleId );
        assertEquals(
            Collections.singletonList( id1 ),
            removeMonitorsList( hub.getMessageRegisteredIds( CALC ),
                                monitors ) );
        assertEquals(
            intMap( new URI[] { id1 }, new int[] { 0 } ),
            removeMonitorsMap( hub.request( id, CALC, calcArgs( PLUS, 0 ) ),
                               monitors ) );

        URI id2 = PlasticUtils.registerXMLRPC( c2 );
        assertEquals(
            new HashSet( Arrays.asList( new URI[] { id1, id2 } ) ),
            new HashSet( removeMonitorsList( hub
                                            .getMessageRegisteredIds( CALC ),
                                             monitors ) ) );
        assertEquals(
            intMap( new URI[] { id1, id2 }, new int[] { 10, 10 } ),
            removeMonitorsMap( hub.request( id, CALC, calcArgs( PLUS, 10 ) ),
                               monitors ) );
        assertEquals(
            intMap( new URI[] { id1, id2 }, new int[] { 5, 5 } ),
            removeMonitorsMap( hub.request( id, CALC, calcArgs( MINUS, 5 ) ),
                               monitors ) );
        assertEquals( 5, c1.sum_ );
        assertEquals( 5, c2.sum_ );

        assertEquals( intMap( new URI[] { id1 }, new int[] { 95 } ),
                  hub.requestToSubset( id, CALC, calcArgs( PLUS, 90 ),
                                       Arrays.asList( new URI[] { id1 } ) ) );
        assertEquals( 95, c1.sum_ );
        assertEquals( 5, c2.sum_ );

        assertEquals( intMap( new URI[] { id2 }, new int[] { 85 } ),
                  hub.requestToSubset( id, CALC, calcArgs( PLUS, 80 ),
                                       Arrays.asList( new URI[] { id2 } ) ) );
        assertEquals( 95, c1.sum_ );
        assertEquals( 85, c2.sum_ );

        synchronized ( lock_ ) {
            int n1 = c1.nreq_;
            int n2 = c2.nreq_;
            PlasticUtils.singleRequestAsynch( "poke", CALC,
                                              calcArgs( MINUS, 5 ) );
            while ( c1.nreq_ == n1 || c2.nreq_ == n2 ) {
                lock_.wait();
            }
        }
        assertEquals( 90, c1.sum_ );
        assertEquals( 80, c2.sum_ );

        synchronized ( lock_ ) {
            int n1 = c1.nreq_;
            hub.requestToSubsetAsynch( id, CALC, calcArgs( MINUS, 10 ),
                                       Arrays.asList( new URI[] { id1 } ) );
            while ( c1.nreq_ == n1 ) {
                lock_.wait();
            }
        }
        assertEquals( 80, c1.sum_ );
        assertEquals( 80, c2.sum_ );

        synchronized ( lock_ ) {
            int n1 = c1.nreq_;
            int n2 = c2.nreq_;
            hub.requestAsynch( id, CALC, calcArgs( PLUS, 21 ) );
            while ( c1.nreq_ == n1 || c2.nreq_ == n2 ) {
                lock_.wait();
            }
        }
        assertEquals( 101, c1.sum_ );
        assertEquals( 101, c2.sum_ );

        assertEquals(
            objMap( new URI[] { id1 }, new String[] { "counter" } ),
            hub.requestToSubset( id,
                                 PlasticUtils
                                .createURI( "ivo://votech.org/info/getName" ),
                                 new ArrayList(),
                                 Arrays.asList( new URI[] { id1 } ) )
        );

        hub.unregister( id1 );
        hub.unregister( id2 );
    }

    private List calcArgs( String op, int value ) {
        return Arrays.asList( new Object[] { op, new Integer( value ) } );
    }

    private Map intMap( URI[] ids, int[] values ) {
        Map map = new HashMap();
        for ( int i = 0; i < ids.length; i++ ) {
            map.put( ids[ i ], new Integer( values[ i ] ) );
        }
        return map;
    }

    private Map objMap( URI[] ids, String[] values ) {
        Map map = new HashMap();
        for ( int i = 0; i < ids.length; i++ ) {
            map.put( ids[ i ], values[ i ] );
        }
        return map;
    }

    private Map removeMonitorsMap( Map map, Collection monitors ) {
        if ( map == null ) {
            return null;
        }
        for ( Iterator it = monitors.iterator(); it.hasNext(); ) {
            map.remove( (URI) it.next() );
        }
        return map;
    }

    private List removeMonitorsList( List list, Collection monitors ) {
        if ( list == null ) {
            return null;
        }
        list.removeAll( monitors );
        return list;
    }

    private class Counter implements PlasticApplication {
        private int sum_;
        private int nreq_;
        public String getName() {
            return "counter";
        }
        public URI[] getSupportedMessages() {
            return new URI[] { CALC };
        }
        public Object perform( URI sender, URI message, List args ) {
            if ( message.equals( CALC ) ) {
                String mode = (String) args.get( 0 );
                int value = ((Integer) args.get( 1 )).intValue();
                assertEquals( 2, args.size() );
                if ( mode.equals( PLUS ) ) {
                    sum_ += value;
                }
                else if ( mode.equals( MINUS ) ) {
                    sum_ -= value;
                }
                else {
                    fail();
                }
            }
            nreq_++;
            synchronized ( lock_ ) {
                lock_.notifyAll();
            }
            return new Integer( sum_ );
        }
    }

    public static void main( String[] args ) throws Exception {
        new PlasticTest( "test" ).testHub();
    }
}
