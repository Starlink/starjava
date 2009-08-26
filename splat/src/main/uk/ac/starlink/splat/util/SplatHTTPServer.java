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
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.httpd.HttpServer;
import org.astrogrid.samp.httpd.ResourceHandler;
import org.astrogrid.samp.httpd.ServerResource;
import org.astrogrid.samp.httpd.UtilServer;

import uk.ac.starlink.splat.iface.images.ImageHolder;

/**
 * HTTP server used by SPLAT.
 * It is used both for SAMP's XML-RPC needs, and for serving dynamically
 * generated content as required.
 * This class is a singleton - see {@link #getInstance}.
 *
 * @author Mark Taylor
 * @version $Id$
 */
public class SplatHTTPServer
{
    protected UtilServer utilServer;
    protected ClientProfile profile;
    protected ResourceHandler resourceHandler;
    protected URL logoURL;
    private static SplatHTTPServer instance;

    /**
     * Private constructor prevents external instantiation.
     */
    private SplatHTTPServer()
        throws IOException
    {
        utilServer = UtilServer.getInstance();
        profile = DefaultClientProfile.getProfile();
        HttpServer httpServer = utilServer.getServer();

        //  Set up handler for custom resource serving.
        resourceHandler =
            new ResourceHandler( httpServer,
                                 utilServer.getBasePath( "/dynamic" ) );
        httpServer.addHandler( resourceHandler );

        //  Make logo available.
        URL internalLogoURL = ImageHolder.class.getResource( "hsplat.gif" );
        logoURL = utilServer.getMapperHandler().addLocalUrl( internalLogoURL );
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
