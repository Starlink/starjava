/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *       11-NOV-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.vo;

import java.net.URL;
import java.net.MalformedURLException;

/**
 * Simple class for describing a Simple Spectral Access Protocol server.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class SSAServer
{
    /** The description of the service */
    private String description = null;

    /** The base URL of the service */
    private URL baseURL = null;

    /**
     * Create an instance and set the characteristics of this server.
     *
     * @param description a symbolic description of the server
     * @param baseURL the base URL of the service, including the trailing
     *                query 
     *
     * @throws MalformedURLException if the baseURL argument cannot be a
     *                               proper URL.
     */
    public SSAServer( String description, String baseURL )
        throws MalformedURLException
    {
        this.description = description;
        this.baseURL = new URL( baseURL );
    }

    /**
     * Return the description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Return the baseURL.
     */
    public URL getBaseURL()
    {
        return baseURL;
    }
}
