package uk.ac.starlink.plastic;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.votech.plastic.PlasticHubListener;

public class HubTest extends TestCase {

    public static boolean VERBOSE = false;
    private static File configFile =
        new File( System.getProperty( "user.home" ),
                  PlasticHubListener.PLASTIC_CONFIG_FILENAME );

    static {
        Logger.getLogger( "uk.ac.starlink.plastic" ).setLevel( Level.WARNING );
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

    private void exerciseHub( PlasticHubListener hub ) throws HubTestException {
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
        }
        finally {
            htest.dispose();
        }
    }
}
