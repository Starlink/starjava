package uk.ac.starlink.plastic;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import junit.framework.TestCase;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.util.LogUtils;

public class HubTest extends TestCase {

    public static boolean VERBOSE = false;
    private static File configFile =
        new File( System.getProperty( "user.home" ),
                  PlasticHubListener.PLASTIC_CONFIG_FILENAME );

    static {
        LogUtils.getLogger( "uk.ac.starlink.plastic" )
                .setLevel( Level.WARNING );
    }

    public HubTest( String name ) {
        super( name );
    }

    public void testHub() throws HubTestException, IOException {

//      if ( ! configFile.exists() ) {
//          ServerSet servers = new ServerSet( configFile );
//          MinimalHub minHub = new MinimalHub( servers );
//          exerciseHub( minHub );
//          minHub.stop();
//          servers.stop();
//      }

        PlasticHub jvmHub = configFile.exists()
            ? null
            : PlasticHub.startHub( VERBOSE ? System.out : null,
                                   VERBOSE ? System.out : null );
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
            throws HubTestException, IOException {
        HubTester htest = new HubTester( hub );
        try {

            /* Do the tests. */
            htest.exerciseHub();

            /* Check/report on hub implementation. */
            String hubDescrip = (String)
                hub.requestToSubset(
                        htest.getId(), MessageId.INFO_GETDESCRIPTION,
                        new ArrayList(),
                        Collections.singletonList( hub.getHubId() ) )
                   .get( hub.getHubId() );
            String hubVers = (String)
                hub.requestToSubset(
                        htest.getId(), MessageId.INFO_GETVERSION,
                        new ArrayList(),
                        Collections.singletonList( hub.getHubId() ) )
                   .get( hub.getHubId() );
            if ( hubDescrip != null && hubDescrip.indexOf( "PlasKit" ) >= 0 ) {
                assertEquals( PlasticUtils.PLASTIC_VERSION, hubVers );
            }
            else {
                System.out.println( "Someone else's hub is running: "
                                  + hubDescrip );
            }

            /* Check monitor. */
            PlasticMonitor mon = 
                new PlasticMonitor( "monitor", false, null, null );
            PlasticConnection moncon = PlasticUtils.registerRMI( mon );
            String iconloc = (String)
                PlasticUtils.targetRequest( "query", MessageId.INFO_GETICONURL,
                                            new ArrayList(), moncon.getId() );
            moncon.unregister();
            assertNull(
                PlasticUtils.targetRequest( "query", MessageId.INFO_GETICONURL,
                                            new ArrayList(), moncon.getId() ) );
            URL iconUrl = new URL( iconloc );

            /* Fragile - waits forever if web server is down, which is
             * hard to diagnose. */
        //  if ( Boolean.getBoolean( "tests.withnet" ) ) {
        //      Icon icon = new ImageIcon( iconUrl );
        //      assertEquals( 19, icon.getIconWidth() );
        //  }
        //  else {
        //      System.out.println( "Skipping network-dependent tests "
        //                        + "(tests.withnet not set)" );
        //  }
        }
        finally {
            htest.dispose();
        }
    }

    public static void main( String[] args ) throws Exception {
        new HubTest( "test" ).testHub();
        System.exit( 0 );
    }
}
