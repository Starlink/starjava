// Copyright (C) 2002 Central Laboratory of the Research Councils

import java.net.URL;
import uk.ac.starlink.soap.AppHttpSOAPServer;

/**
 * Demonstration server for application builtin SOAP services.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since 23-MAY-2002
 */
public class EchoServer
{
    // Port number for our application.
    public static final int PORT_NUMBER = 8080;

    //  The end point for our SOAP services.
    public static String ENDPOINT =
        "http://localhost:" + PORT_NUMBER + "/services/EchoServer";

    //  Start the application and run the services.
    public static void main( String[] args )
    {
        //  Get a URL to the local "deploy.wsdd" file. This defines
        //  our SOAP services.
        URL deployURL = EchoServer.class.getResource( "deploy.wsdd" );
        System.out.println( deployURL );

        //  Create the HTTP/SOAP server. This initializes a HTTP
        //  server and adds the Axis SOAP services.
        try {
            AppHttpSOAPServer server = new AppHttpSOAPServer(PORT_NUMBER);
            server.start();
            server.addSOAPService( deployURL );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            throw new RuntimeException( "Failed to start SOG SOAP services" );
        }
    }

    //  Define the SOAP RPC method. This is specified in the
    //  "deplay.wsdd" file. The message "exit" causes an exit!
    public String echo( String message )
    {
        if ( message.equals( "exit" ) ) {

            //  Need to exit after a while...
            java.util.Timer timer = new java.util.Timer();
            java.util.TimerTask task =
                new java.util.TimerTask() {
                    public void run() 
                    {
                        System.exit( 0 );
                    }
                };
            // Say a second...
            timer.schedule( task, 1000L );
            message = message + " (OK, I'm off)";
        } 
        return "You say: " + message;
    }
}
