/*
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     06-MAR-2009 (Mark Taylor):
 *        Original version.
 */
package uk.ac.starlink.splat.util;

import java.io.IOException;
import java.net.URL;

import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.httpd.HttpServer;
import org.astrogrid.samp.httpd.ResourceHandler;
import org.astrogrid.samp.httpd.ServerResource;
import org.astrogrid.samp.httpd.URLMapperHandler;
import org.astrogrid.samp.xmlrpc.internal.InternalClientFactory;
import org.astrogrid.samp.xmlrpc.internal.InternalServer;
import org.astrogrid.samp.xmlrpc.SampXmlRpcClientFactory;
import org.astrogrid.samp.xmlrpc.SampXmlRpcServer;
import org.astrogrid.samp.xmlrpc.SampXmlRpcServerFactory;
import org.astrogrid.samp.xmlrpc.StandardClientProfile;

import uk.ac.starlink.splat.iface.images.ImageHolder;

/**
 * HTTP server used by SPLAT.
 * It is used both for SAMP's XML-RPC needs, and for serving dynamically
 * generated content as required.
 * The HTTP server itself is started lazily.
 * This class is a singleton - see {@link #getInstance}.
 *
 * @author Mark Taylor
 * @version $Id$
 */
public class SplatHTTPServer
{
    protected HttpServer server;
    protected ResourceHandler resourceHandler;
    protected ClientProfile profile;
    protected boolean started = false;
    protected URL logoURL;
    private static SplatHTTPServer instance;

    /**
     * Private constructor prevents instantiation.
     */
    private SplatHTTPServer()
        throws IOException
    {
        server = new HttpServer();
        server.setDaemon( true );

        //  Set up handler for custom resource serving.
        resourceHandler = new ResourceHandler( server, "/dynamic" );
        server.addHandler( resourceHandler );

        //  Set up handler for logo.
        URL internalLogoURL = ImageHolder.class.getResource( "hsplat.gif" );
        URLMapperHandler logoHandler = 
            new URLMapperHandler( server, "/logo", internalLogoURL, false );
        server.addHandler( logoHandler );
        logoURL = logoHandler.getBaseUrl();

        //  Set up handler for XML-RPC.
        SampXmlRpcClientFactory xClientFactory = new InternalClientFactory();
        final SampXmlRpcServer xServer =
            new InternalServer( server, "/xmlrpc" );
        SampXmlRpcServerFactory xServerFactory = new SampXmlRpcServerFactory()
        {
            public SampXmlRpcServer getServer()
            {
                checkStarted();
                return xServer;
            }
        };
        profile = new StandardClientProfile( xClientFactory, xServerFactory );
    }

    /**
     * Returns a SAMP client profile which uses this server.
     *
     * @return   profile
     */
    public ClientProfile getSampProfile()
    {
        return profile;
    }

    /**
     * Returns a URL for the SPLAT logo.  This is an http-protocol URL not
     * jar-protocol one, so it may be used outside the JVM.
     */
    public URL getLogoURL()
    {
        return logoURL;
    }

    /**
     * Makes a resource available for retrieving from this internal HTTP server.
     * A <code>name</code> may be supplied which will appear at the end of
     * the URL, but this is just for cosmetic purposes.  The URL at which
     * the resource is available will provided as the return value.
     *
     * @param   name   filename identifying the resource
     * @param   resource   resource to make available
     * @return    URL at which <code>resource</code> can be found
     */
    public URL addResource( String name, ServerResource resource )
    {
        checkStarted();
        return resourceHandler.addResource( name == null ? "" : name,
                                            resource );
    }

    /**
     * Removes a resource from this server.
     *
     * @param  url  URL returned by a previous addResource call
     */
    public void removeResource( URL url )
    {
        resourceHandler.removeResource( url );
    }

    /**
     * Ensures that the server has been started since it was created.
     * May harmlessly be called multiple times.
     */
    private void checkStarted()
    {
        if ( ! started ) {
            synchronized ( server ) {
                if ( ! started ) {
                    started = true;
                    server.start();
                }
            }
        }
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return   instance
     */
    public static SplatHTTPServer getInstance()
        throws IOException
    {
        if ( instance == null ) {
            synchronized ( SplatHTTPServer.class ) {
                if ( instance == null ) {
                    instance = new SplatHTTPServer();
                }
            }
        }
        return instance;
    }
}
