// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.sog;

import uk.ac.starlink.soap.AppHttpSOAPServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.net.URL;

/**
 * @author Peter W. Draper
 * @version $Id$
 * @since 22-MAY-2002
 */
public class SimpleTest
{
    private static Log log = LogFactory.getLog( SimpleTest.class );

    public static void main(String[] arg)
    {
        AppHttpSOAPServer tempserver = null;
        URL deployURL = SimpleTest.class.getResource( "deploy.wsdd" );
        try {
            tempserver = new AppHttpSOAPServer( 8082 );
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit( 1 );
        }
        final AppHttpSOAPServer server = tempserver;

        // Create and add a shutdown hook
        Thread hook = new Thread() {
                public void run()
                {
                    setName( "Shutdown" );
                    log.info( "Shutdown hook executing" );
                    try {
                        server.stop();
                    }
                    catch(Exception e) {
                        log.warn( e.getMessage(), e );
                    }

                    // Try to avoid JVM crash
                    try {
                        Thread.sleep( 1000 );
                    }
                    catch( Exception e ) {
                        log.warn( e.getMessage(), e );
                    }
                }
            };
        Runtime.getRuntime().addShutdownHook( hook );
    }
}
