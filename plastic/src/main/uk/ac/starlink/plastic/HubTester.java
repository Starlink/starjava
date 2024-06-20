package uk.ac.starlink.plastic;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.votech.plastic.PlasticHubListener;

/**
 * Runs tests on an existing PlasticHubListener object.
 * The easiest way to use this class is to call one of the static methods
 * {@link #main}, {@link #testHub()} or 
 * {@link #testHub(org.votech.plastic.PlasticHubListener)}.
 *
 * @author   Mark Taylor
 * @since    4 Aug 2006
 */
public class HubTester {

    private final PlasticHubListener hub_;
    private final URI id_;
    private List monitors_;
    private final Object lock_ = new Object();
    private final boolean abuse_ = false;

    private static final String CLIENT_NAME = "test-driver";
    private static final URI CALC_MSG = 
        PlasticUtils.createURI( "ivo://plastic.starlink.ac.uk/msg/calc" );
    private static final URI DUMMY_MSG =
        PlasticUtils
       .createURI( "ivo://plastic.starlink.ac.uk/msg/not-a-message" );
    private static final String PLUS = "plus";
    private static final String MINUS = "minus";

    /** Plastic rendezvous file. */
    private static final File CONFIG_FILE = 
        new File( System.getProperty( "user.home" ),
                  PlasticUtils.PLASTIC_FILE );

    /**
     * Constructor.
     *
     * @param   hub  hub to test
     */
    public HubTester( PlasticHubListener hub ) throws HubTestException {
        hub_ = hub;
        id_ = hub.registerNoCallBack( CLIENT_NAME );
        assertTrue( id_ != null );
        monitors_ = getMonitors();
    }

    /**
     * Returns the registered client ID for this tester.
     *
     * @return  client ID
     */
    public URI getId() {
        return id_;
    }

    /**
     * Returns hub object with which this tester is registered.
     *
     * @return  hub
     */
    public PlasticHubListener getHub() {
        return hub_;
    }

    /**
     * Disposes of this tester, clearing up as appropriate.
     */
    public void dispose() throws HubTestException {
        hub_.unregister( id_ );
    }

    /**
     * Runs most of the actual tests.
     */
    public void exerciseHub() throws HubTestException {
        if ( hub_ == null || id_ == null ) {
            throw new IllegalStateException( "Not initialised" );
        }
        assertTrue( CONFIG_FILE.exists() );
        try {
            exerciseCounters();
            exerciseMonitors();
        }
        catch ( IOException e ) {
            throw new HubTestException( "Hub failed", e );
        }
        catch ( InterruptedException e ) {
            throw new HubTestException( "Hub failed", e );
        }
    }

    /**
     * Runs tests using Counter client applications.
     */
    private void exerciseCounters()
            throws HubTestException, IOException, InterruptedException {
        Counter c1 = new Counter();
        Counter c2 = new Counter();
        PlasticConnection conn1 = PlasticUtils.registerRMI( c1 );
        URI id1 = conn1.getId();

        assertTrue( hub_.getMessageRegisteredIds( CALC_MSG ) != null );
        assertEquals(
            removeMonitorsList( hub_.getMessageRegisteredIds( CALC_MSG ) ),
            Collections.singletonList( id1 ) );
        assertEquals( 
            1, 
            removeMonitorsList( hub_.getMessageRegisteredIds( CALC_MSG ) )
                             .size() );
        Object soleId =
            removeMonitorsList( hub_.getMessageRegisteredIds( CALC_MSG ) )
           .get( 0 );
        assertEquals( id1, soleId );

        assertEquals(
            intMap( new URI[] { id1 }, new int[] { 0 } ),
            removeMonitorsMap( hub_.request( id_, CALC_MSG,
                                             calcArgs( PLUS, 0 ) ) ) );

        PlasticConnection conn2 = PlasticUtils.registerXMLRPC( c2 );
        URI id2 = conn2.getId();
        assertEquals(
            new HashSet( Arrays.asList( new URI[] { id1, id2 } ) ),
            new HashSet( removeMonitorsList(
                             hub_.getMessageRegisteredIds( CALC_MSG ) ) ) );
        assertEquals(
            intMap( new URI[] { id1, id2 }, new int[] { 10, 10 } ),
            removeMonitorsMap( hub_.request( id_, CALC_MSG,
                                             calcArgs( PLUS, 10 ) ) ) );
        assertEquals(
            intMap( new URI[] { id1, id2 }, new int[] { 5, 5 } ),
            removeMonitorsMap( hub_.request( id_, CALC_MSG,
                                             calcArgs( MINUS, 5 ) ) ) );
        assertEquals( 5, c1.sum_ );
        assertEquals( 5, c2.sum_ );

        assertEquals( intMap( new URI[] { id1 }, new int[] { 95 } ),
                  hub_.requestToSubset( id_, CALC_MSG, calcArgs( PLUS, 90 ),
                                        Arrays.asList( new URI[] { id1 } ) ) );
        assertEquals( 95, c1.sum_ );
        assertEquals( 5, c2.sum_ );

        assertEquals( intMap( new URI[] { id2 }, new int[] { 85 } ),
                  hub_.requestToSubset( id_, CALC_MSG, calcArgs( PLUS, 80 ),
                                        Arrays.asList( new URI[] { id2 } ) ) );
        assertEquals( 95, c1.sum_ );
        assertEquals( 85, c2.sum_ );

        synchronized ( lock_ ) {
            int n1 = c1.nreq_;
            int n2 = c2.nreq_;
            PlasticUtils.singleRequestAsynch( "poke", CALC_MSG,
                                              calcArgs( MINUS, 5 ) );
            while ( c1.nreq_ == n1 || c2.nreq_ == n2 ) {
                lock_.wait();
            }
        }
        assertEquals( 90, c1.sum_ );
        assertEquals( 80, c2.sum_ );

        synchronized ( lock_ ) {
            int n1 = c1.nreq_;
            hub_.requestToSubsetAsynch( id_, CALC_MSG, calcArgs( MINUS, 10 ),
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
            hub_.requestAsynch( id_, CALC_MSG, calcArgs( PLUS, 21 ) );
            while ( c1.nreq_ == n1 || c2.nreq_ == n2 ) {
                lock_.wait();
            }
        }
        assertEquals( 101, c1.sum_ );
        assertEquals( 101, c2.sum_ );

        assertEquals(
            objMap( new URI[] { id1 }, new String[] { "counter" } ),
            hub_.requestToSubset( id_,
                                  PlasticUtils
                                 .createURI( "ivo://votech.org/info/getName" ),
                                  new ArrayList(),
                                  Arrays.asList( new URI[] { id1 } ) )
        );

        assertTrue( hub_.getRegisteredIds().contains( id1 ) );
        assertTrue( hub_.getMessageRegisteredIds( CALC_MSG ).contains( id1 ) );
        assertTrue( ! hub_.getMessageRegisteredIds( DUMMY_MSG )
                          .contains( id1 ) );
        conn1.unregister();
        assertTrue( ! hub_.getRegisteredIds().contains( id1 ) );
        assertTrue( ! hub_.getMessageRegisteredIds( CALC_MSG )
                          .contains( id1 ) );

        assertTrue( hub_.getRegisteredIds().contains( id2 ) );
        assertTrue( hub_.getMessageRegisteredIds( CALC_MSG ).contains( id2 ) );
        conn2.unregister();
        assertTrue( ! hub_.getRegisteredIds().contains( id2 ) );
        assertTrue( ! hub_.getMessageRegisteredIds( CALC_MSG )
                          .contains( id2 ) );

        c1.dispose();
        c2.dispose();
    }

    private void exerciseMonitors()
            throws IOException, HubTestException, InterruptedException {
        PlasticMonitor mon1 = new PlasticMonitor( "mon1", true, null, null );
        PlasticMonitor mon2 = new PlasticMonitor( "mon2", false, null, null );
        ApplicationItem[] regApps =
            PlasticUtils.getRegisteredApplications( hub_ );
        ApplicationListModel appList1 = new ApplicationListModel( regApps );
        ApplicationListModel appList2 = new ApplicationListModel( regApps );
        mon1.setListModel( appList1 );
        mon2.setListModel( appList2 );
        mon1.setHub( hub_ );
        mon2.setHub( hub_ );
        PlasticConnection conn1 = PlasticUtils.registerRMI( mon1 );
        URI id1 = conn1.getId();
        assertTrue( hub_.getRegisteredIds().contains( id1 ) );
        PlasticConnection conn2 = PlasticUtils.registerXMLRPC( mon2 );
        URI id2 = conn2.getId();
        assertTrue( hub_.getRegisteredIds().contains( id2 ) );

        Thread.sleep( 2000 );

        assertTrue( hub_.getRegisteredIds().contains( id1 ) );
        assertTrue( hub_.getRegisteredIds().contains( id2 ) );
        conn1.unregister();
        assertTrue( ! hub_.getRegisteredIds().contains( id1 ) );
        conn2.unregister();
        assertTrue( ! hub_.getRegisteredIds().contains( id2 ) );
    }

    /**
     * Returns a list of applications which are listening to all messages.
     * This may be non-empty if the hub already exists with some
     * monitoring listeners registered.  This may be needed to elimintate
     * some returned messages from the hub later.  This behaviour
     * should only be encountered in hubs implementing PLASTIC
     * versions 0.4 and below.
     *
     * @return   list of application IDs (URIs) which claim to respond to
     *           all messages
     */
    private final List getMonitors() throws HubTestException {

        /* Get a list of applications which are listening to all messages.
         * This may be non-empty if the hub already exists with some
         * monitoring listeners registered.  We will need this to elimintate
         * some returned messages from the hub later.  This behaviour
         * should only be encountered in hubs implementing PLASTIC
         * versions 0.4 and below. */
        List monitors = hub_.getMessageRegisteredIds( DUMMY_MSG );
        assertTrue( monitors != null );
        float hubVers = getHubVersion();
        if ( hubVers > 0.49 ) {
            assertTrue( monitors.isEmpty() );
        }
        return monitors;
    }

    /**
     * Modifies a list so that any monitor-type entries are removed.
     *
     * @param  list  input list of URIs
     * @return  <code>list</code> with any elements of <code>monitors_</code>
     *          removed
     */
    private List removeMonitorsList( List list ) {
        if ( list == null ) {
            return null;
        }
        if ( ! abuse_ ) {
            list = new ArrayList( list );
        }
        list.removeAll( monitors_ );
        return list;
    }

    /**
     * Modifies a map so that any monitor-type entries are removed.
     *
     * @param  map  input URI->Object map
     * @return  <code>map</code> with any elements whose keys are elements
     *          if <code>monitors_</code> removed
     */
    private Map removeMonitorsMap( Map map ) {
        if ( map == null ) {
            return null;
        }
        if ( ! abuse_ ) {
            map = new HashMap( map );
        }
        for ( Iterator it = monitors_.iterator(); it.hasNext(); ) {
            map.remove( (URI) it.next() );
        }
        return map;
    }

    /**
     * Returns the version of the running hub.
     *
     * @return  version number as a floating point number.
     */
    private final float getHubVersion() throws HubTestException {
        URI hubId = hub_.getHubId();
        Map vMap = hub_.requestToSubset( id_, MessageId.INFO_GETVERSION,
                                         new ArrayList(),
                                         Collections.singletonList( hubId ) );
        String shubVers = (String) vMap.get( hubId );
        assertTrue( shubVers != null );
        try {
            return Float.parseFloat( shubVers );
        }
        catch ( NumberFormatException e ) {
            throw new HubTestException( "Version string \"" + shubVers
                                      + "\" doesn't have numeric form", e );
        }
    }

    /**
     * Tests an assertion.
     *
     * @param   test   asserted condition
     * @throws  HubTestException  if <code>test</code>  is false
     */
    private void assertTrue( boolean test ) throws HubTestException {
        if ( ! test ) {
            throw new HubTestException( "Hub test failed" );
        }
    }

    /**
     * Tests object equality.
     *
     * @param   o1  object 1
     * @param   o2  object 2
     * @throws  HubTestException  unless <code>o1</code> and <code>o2</code>
     *          are both <code>null</code> or are equal in the sense of
     *          {@link java.lang.Object#equals}
     */
    private void assertEquals( Object o1, Object o2 )
            throws HubTestException {
        if ( o1 == null && o2 == null ) {
        }
        else if ( o1 == null || ! o1.equals( o2 ) ) {
            throw new HubTestException(
                "Hub test failed: " +  o1 + " != " + o2 );
        }
    }

    /**
     * Tests integer equality.
     *
     * @param  i1  integer 1
     * @param  i2  integer 2
     * @throws  HubTestException  iff <code>i1</code> != <code>i2</code>
     */
    private void assertEquals( int i1, int i2 ) throws HubTestException {
        if ( i1 != i2 ) {
            throw new HubTestException(
                "Hub test failed: " + i1 + " != " + i2 );
        }
    }

    /**
     * Utility method to produce an argument list for the CALC_MSG message.
     *
     * @param   op  operation name
     * @param   value  operand
     * @return  argument list
     */
    private static List calcArgs( String op, int value ) {
        return Arrays.asList( new Object[] { op, Integer.valueOf( value ) } );
    }

    /**
     * Utility method to produce a URI->Integer map.
     *
     * @param  ids  array of URIs
     * @param  values  array of integer values (same length as <code>ids</code>)
     * @return  URI->Integer map
     */
    private static Map intMap( URI[] ids, int[] values ) {
        Map map = new HashMap();
        for ( int i = 0; i < ids.length; i++ ) {
            map.put( ids[ i ], Integer.valueOf( values[ i ] ) );
        }
        return map;
    }

    /**
     * Utility method to produce a URI->Object map.
     *
     * @param  ids  array of URIs
     * @param  values  array of objects (same length as <code>ids</code>)
     * @return  URI->Object map
     */
    private static Map objMap( URI[] ids, Object[] values ) {
        Map map = new HashMap();
        for ( int i = 0; i < ids.length; i++ ) {
            map.put( ids[ i ], values[ i ] );
        }
        return map;
    }
    
    /**
     * Tests the currently running hub.
     *
     * @throws  HubTestException   if the hub is faulty, or if no hub
     *          is running
     */
    public static void testHub() throws HubTestException {
        PlasticHubListener rmiHub; 
        PlasticHubListener xmlrpcHub;
        try {
            rmiHub = PlasticUtils.getLocalHub();
            xmlrpcHub = new XmlRpcHub( PlasticUtils.getXmlRpcUrl(), rmiHub );
        }
        catch ( IOException e ) {
            throw new HubTestException( "Trouble acquiring hub", e );
        }
        testHub( rmiHub );
        testHub( xmlrpcHub );
        testHub( rmiHub );
    }

    /**
     * Tests a given hub object.
     *
     * @throws  HubTestException  if the hub is faulty
     */
    public static void testHub( PlasticHubListener hub )
            throws HubTestException {
        HubTester htest = new HubTester( hub );
        try {
            htest.exerciseHub();
        }
        finally {
            htest.dispose();
        }
    }

    /**
     * Runs tests on a currently running hub.
     *
     * <h2>Flags</h2>
     * <dl>
     * <dt>-help</dt>
     * <dd>Prints a help message and exits.</dd>
     * </dl>
     *
     * @throws   HubTestException   if no hub is running, or if the hub
     *           fails any of the tests
     */
    public static void main( String[] args ) throws HubTestException {
        String usage = "\nUsage:"
                     + "\n      "
                     + HubTester.class.getName()
                     + "\n";
        if ( args.length == 1 && args[ 0 ].startsWith( "-h" ) ) {
            System.out.println( usage );
            return;
        }
        else if ( args.length > 0 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        try {
            testHub();
            System.exit( 0 );
        }
        catch ( Throwable error ) {
            error.printStackTrace();
            System.exit( 1 );
        }
    }

    /**
     * PlasticApplication which performs some simple arithmetic.
     */
    private class Counter implements PlasticApplication {
        private int sum_;
        private int nreq_;
        private Throwable error_;
        public String getName() {
            return "counter";
        }
        public URI[] getSupportedMessages() {
            return new URI[] { CALC_MSG };
        }
        public Object perform( URI sender, URI message, List args ) {
            try {
                if ( message.equals( CALC_MSG ) ) {
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
                        throw new HubTestException(
                            "Mangled mode \"" + mode + "\"?" );
                    }
                }
                nreq_++;
                synchronized ( lock_ ) {
                    lock_.notifyAll();
                }
                return Integer.valueOf( sum_ );
            }
            catch ( Throwable error ) {
                error_ = error;
                return Integer.valueOf( sum_ );
            }
        }
        public void dispose() throws HubTestException {
            if ( error_ != null ) {
                throw new HubTestException( "Deferred error from client: " + 
                                            error_.getMessage(), error_ );
            }
        }
    }
}
