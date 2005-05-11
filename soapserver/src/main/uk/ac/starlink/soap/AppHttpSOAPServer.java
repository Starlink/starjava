/*
 * Copyright (C) 2002-2004 Central Laboratory of the Research Councils
 */

package uk.ac.starlink.soap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.Socket;
import java.util.Vector;

import org.apache.axis.client.Call;
import org.apache.axis.AxisEngine;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.message.SOAPBodyElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.jetty.servlet.ServletHttpContext;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.xml.XmlConfiguration;

/**
 * A HTTP and SOAP server for embedding in applications.
 * <p>
 * This should be used in applications that want to offer web services for
 * clients to use. The web services offered are SOAP RPC requests for the
 * activation of registered methods in classes that are part of the
 * application.
 * <p>
 * A special property of this server over the standard Jetty one is that it
 * requires only resources that are available locally and are located using
 * the classloader (so the resource files can be packaged in jar files).
 * <p>
 * If the given port number is in use, then a search for a free port will be
 * undertaken by incrementing the given port number.
 * <p>
 * The configuration of this server is builtin to provide Axis SOAP services.
 * <p>
 * Start up sequence:
 * <pre>
 *    AppHttpSOAPServer server = new AppHttpSOAPServer( port_number );
 *    server.start();
 *    server.addSOAPServices( <URL of "deploy.wsdd" file> );
 * </pre>
 * <p>
 *
 * @author Peter W. Draper, Alasdair Allan
 * @version $Id$
 * @since 22-MAY-2002
 */
public class AppHttpSOAPServer
    extends HttpServer
{
    /**
     * Resource name of the configuration file. This is local (to this
     * class) and cannot be overridden.
     */
    private URL configFile =
        AppHttpSOAPServer.class.getResource( "jetty.xml" );

    /**
     * The port number being used. This should always be re-defined.
     */
    private int portNum = 8080;

    /**
     * Logger. Output control of this is performed using the logging.properties
     * file of the Starlink util package (when applications are started from
     * the command-line using the "starjava" command).
     */
    private static Log log = LogFactory.getLog( AppHttpSOAPServer.class );

    /**
     * Constructor loads the default configuration and starts a HTTP/SOAP
     * server on a given port. To complete this sequence you should "start()"
     * the server and then load any application SOAP services.
     *
     * @param portNum the requested port on which to establish the HTTP
     *                services, this may not be the port actually used.
     */
    public AppHttpSOAPServer( int portNum )
        throws IOException
    {
        //  Define the port number.
        setDefaultPort( portNum );

        //  Never validate the configuration files, avoids warnings.
        System.setProperty("org.mortbay.xml.XmlParser.NotValidating", "true");

        try {
            XmlConfiguration config = new XmlConfiguration( configFile );
            config.configure( this );
        }
        catch( IOException e ) {
            throw e;
        }
        catch( Exception e ) {
            e.printStackTrace();
            throw new IOException( "Jetty configuration problem: " + e );
        }
    }

    /**
     * Create a new ServletHttpContext.
     *
     * This method is called by HttpServer to create new contexts. Thus calls
     * to addContext or getContext that result in a new Context being created
     * will return an {@link ServletHttpContext} instance (rather than a
     * non-servlet type).
     *
     * @param contextPathSpec
     *
     * @return {@link ServletHttpContext}
     */
    protected HttpContext newHttpContext()
    {
        return new ServletHttpContext();
    }

    /**
     * Add the Axis SOAP services (this is loaded when parsing the "jetty.xml"
     * file). The WAR file may be stored in a jar file.
     *
     * @param direct name of the Axis war file.
     *
     * @return {@link WebApplicationContext}
     * @exception IOException
     */
    public WebApplicationContext addAxisSOAPServices( String direct )
        throws IOException
    {
        URL axisURL = AxisEngine.class.getResource( direct );
        WebApplicationContext soapContext = new WebApplicationContext();

        //  Set the configuration classes. If these are null then we cannot
        //  use a HttpServer, but use a Server instead (and move over to the
        //  full Jetty servlet-based system).
        soapContext.setConfigurationClassNames( new String[] {
            "org.mortbay.jetty.servlet.XMLConfiguration",
            "org.mortbay.jetty.servlet.JettyWebConfiguration" } );

        //  Set the Axis war file.
        soapContext.setWAR( axisURL.toString() );
        soapContext.setContextPath( "/" );
        addContext( soapContext );
        log.info( "Web Application "+ soapContext + " added" );
        return soapContext;
    }

    /**
     * Add new services to the SOAP server. These are defined in a "Web
     * Service Deployment Descriptor" (wsdd) file that is located using a URL
     * (which can consequently be in a jar file, use the "getResource" method
     * of "Class" to locate a file stored with the application classes).
     *
     * @param deployURL URL of a wsdd file containing deployment
     *                  descriptions for the SOAP services to be
     *                  offered by the application.
     */
    public void addSOAPService( URL deployURL )
    {
        // Deploy any SOAP services using the client mechanism, this makes
        // sure that the connection is working and makes sure that the correct
        // initialisations are performed. A problem with this is that Axis
        // wants to use any proxy server information, so we need to make sure
        // that localhost is not proxied (note: used to do this by setting
        // http.proxyHost to blank, but that value is cached by Axis for all
        // subsequent sessions).
        String nonProxyHosts = System.getProperty( "http.nonProxyHosts" );
        if ( nonProxyHosts == null || nonProxyHosts.equals( "" ) ) {
            System.setProperty( "http.nonProxyHosts", "localhost" );
        }
        else if ( nonProxyHosts.indexOf( "localhost" ) == -1 ) {
            System.setProperty( "http.nonProxyHosts", 
                                nonProxyHosts + "|localhost" );
        }
        try {
            String endpoint =
                "http://localhost:" + portNum + "/services/AdminService";

            Service service = new Service();
            Call call = (Call) service.createCall();

            call.setTargetEndpointAddress( new java.net.URL( endpoint ) );
            call.setOperationName( "AdminService" );

            Vector result = null;
            InputStream input = deployURL.openStream();
            Object[] params = new Object[] { new SOAPBodyElement(input) };
            result = (Vector) call.invoke( params );

            input.close();

            SOAPBodyElement body = (SOAPBodyElement) result.elementAt(0);
            log.info( body.toString() );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the default port number to use, will try the suggested port number
     * and if free will set the port number to that. However if that port is
     * in use it will in incremement the number until it finds a free port
     * (within the next 1000 ports). It will then put the server on that port.
     *
     * @param portNum the port number.
     */
    protected void setDefaultPort( int portNum )
    {
        int startPort = portNum;
        int usingPort = startPort;
        for ( int i = startPort; i < startPort+1000; i++ ) {

            // check port
            boolean open = true;
            try {
                Socket tempSocket = new Socket( "localhost", i );
                tempSocket.close();
            }
            catch (Exception any) {
                // Fails if not already in use, which is good.
                open = false;
            }

            if( !open) {
                usingPort = i;
                break;
            }
        }
        System.setProperty( "jetty.port", Integer.toString( usingPort ) );
        this.portNum = usingPort;
    }

    /**
     * Return the port number being used. This may differ from the port number
     * asked for.
     */
    public int getPort( )
    {
       return portNum;
    }

    /* ------------------------------------------------------------ */
    public static void main(String[] arg)
    {
        AppHttpSOAPServer tempserver = null;
        URL deployURL = AppHttpSOAPServer.class.getResource("deploy.wsdd");
        try {
            tempserver = new AppHttpSOAPServer( 8080 );
            tempserver.start();
            tempserver.addSOAPService( deployURL );
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
                        log.warn( "Error shutting down", e );
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
