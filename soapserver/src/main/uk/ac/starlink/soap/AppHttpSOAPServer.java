/*
 *Copyright (C) 2002 Central Laboratory of the Research Councils
 */

package uk.ac.starlink.soap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Vector;

import javax.servlet.Servlet;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Call;
import org.apache.axis.AxisEngine;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.transport.http.AxisServlet;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletHttpContext;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.xml.XmlConfiguration;

/**
 * A builtin in HTTP and SOAP server for applications.
 * <p>
 *
 * This should be used in applications that want to offer web services
 * for clients to use. The web services offered are SOAP RPC requests
 * for the activation of registered methods in classes that are part
 * of the application.
 *
 * <p>
 * A special property of this server over the standard Jetty one is
 * that it requires on resources that are available locally and are
 * located using the classloader (so the resource files can be
 * packaged in jar files).
 * <p>
 * The configuration of this server is builtin to provide Axis SOAP
 * services.
 * <p>
 * Start up sequence:
 * <pre>
 *    AppHttpSOAPServer server = new AppHttpSOAPServer( port_number );
 *    server.start();
 *    server.addSOAPServices( <Url of "deploy.wsdd" file> );
 * </pre>
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since 22-MAY-2002
 */
public class AppHttpSOAPServer extends HttpServer
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
     * Constructor loads the default configuration and starts a
     * HTTP/SOAP server on a given port. To complete this sequence you
     * should "start()" the server and then load any application SOAP
     * services.
     *
     * @param portNum the port on which to establish the HTTP services
     */
    public AppHttpSOAPServer( int portNum )
        throws IOException
    {
        //  Define the port number.
        setDefaultPort( portNum );

        //  Set the Log system to use our logger.
        Log.instance().add( new AppLogSink() );

        try {
            XmlConfiguration config = new XmlConfiguration( configFile );
            config.configure( this );
        }
        catch( IOException e ) {
            throw e;
        }
        catch( Exception e ) {
            Code.warning( e );
            e.printStackTrace();
            throw new IOException( "Jetty configuration problem: " + e );
        }
    }

    /**
     * Create a new ServletHttpContext.
     *
     * This method is called by HttpServer to create new contexts.
     * Thus calls to addContext or getContext that result in a new
     * Context being created will return an
     * org.mortbay.jetty.servlet.ServletHttpContext instance (rather
     * than a non-servlet type).
     *
     * @param contextPathSpec
     *
     * @return ServletHttpContext
     */
    protected HttpContext newHttpContext( String contextPathSpec )
    {
        return new ServletHttpContext( this, contextPathSpec );
    }

    /**
     * Add the Axis SOAP services (this is loaded when parsing the
     * "jetty.xml" file).
     *
     * @param direct name of the Axis war file.
     *
     * @return The WebApplicationContext
     * @exception IOException
     */
    public WebApplicationContext addAxisSOAPServices( String direct )
        throws IOException
    {
        URL axisURL = AxisEngine.class.getResource( direct );
        WebApplicationContext soapContext = 
            new WebApplicationContext( this, "/", axisURL.toString() );
        addContext( null, soapContext );
        Log.event( "Web Application "+ soapContext + " added" );
        return soapContext;
    }

    /**
     * Add new services to the SOAP server. These are defined in a
     * "Web Service Deployment Descriptor" (wsdd) file that is located
     * using a URL (which can consequently be in a jar file, use the
     * "getResource" method of "Class" to locate a file stored with
     * the application classes).
     *
     * @param deployURL URL of a wsdd file containing deployment
     *                  descriptions for the SOAP services to be
     *                  offered by the application.
     */
    public void addSOAPService( URL deployURL )
    {
        
        // Deploy any SOAP services using the client mechanism, this
        // makes sure that the connection is working and makes sure
        // that the correct initialisations are performed. A problem
        // with this is that Axis wants to use any proxy server
        // information, so we need to disable it and then make certain
        // that we re-enable it.
        String proxyHost = System.getProperty( "http.proxyHost" );
        if ( proxyHost != null ) {
            System.setProperty( "http.proxyHost", "" );
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
            System.out.println( body.toString() );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if ( proxyHost != null ) {
            System.setProperty( "http.proxyHost", proxyHost );
        }
    }

    /**
     * Set the default port number to use.
     *
     * @param portNum the port number.
     */
    protected void setDefaultPort( int portNum )
    {
        System.setProperty( "jetty.port", Integer.toString( portNum ) );
        this.portNum = portNum;
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
                    Log.event( "Shutdown hook executing" );
                    try {
                        server.stop();
                    }
                    catch(Exception e) {
                        Code.warning( e );
                    }

                    // Try to avoid JVM crash
                    try {
                        Thread.sleep( 1000 );
                    }
                    catch( Exception e ) {
                        Code.warning( e );
                    }
                }
            };
        Runtime.getRuntime().addShutdownHook( hook );
    }
}
