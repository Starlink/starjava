/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-NOV-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.vo;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Container for a list of possible Simple Spectral Access Protocol
 * servers that can be used. In time this should be derived from a
 * query to a Registry.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SSAServerList
{
    private ArrayList serverList = new ArrayList();

    public SSAServerList()
    {
        addStaticServers();
    }

    /**
     * Add an SSA server to the known list.
     *
     * @param description a human readable description of the service.
     * @param baseURL the URL for accessing the service (including trailing ?).
     *
     * @throws MalFormedURLException is the URL is invalid.
     */
    public void addServer( String description, String baseURL )
        throws MalformedURLException
    {
        addServer( new SSAServer( description, baseURL ) );
    }

    /**
     * Add an SSA server to the known list.
     *
     * @param server an instance of {@link SSAServer}.
     *
     * @throws MalFormedURLException is the URL is invalid.
     */
    public void addServer( SSAServer server )
        throws MalformedURLException
    {
        serverList.add( server );
    }

    /**
     * Return an Iterator over the known servers. The objects iterated over
     * are instances of {@link SSAServer}.
     */
    public Iterator getIterator()
    {
        return serverList.iterator();
    }

    /**
     * Retrieve a server description. Returns null if not found.
     */
    public SSAServer matchDescription( String description )
    {
        Iterator i = getIterator();
        SSAServer server = null;
        while ( i.hasNext() ) {
            server = (SSAServer) i.next();
            if ( description.equals( server.getDescription() )) {
                return server;
            }
        }
        return null;
    }

    /**
     * Initialise the known servers as we don't have Registry access yet.
     */
    protected void addStaticServers()
    {
        //  Just use the ones from VOSpec.
        try {
            addServer ( "INES", "http://sdc.laeff.esa.es/ines/jsp/siap.jsp?" );
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            addServer( "HST/STECF FOS",
                       "http://archive.eso.org/bin/fos_ssap.pl?" );
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }

        //  Slightly challenging as doesn't end with a ?. Also doesn't like
        //  FORMAT=application/fits.
        try {
            addServer( "Infrared Space Observatory Archive",
                       "http://pma.iso.vilspa.esa.es:8080/aio/jsp/siap.jsp?" +
                       "imageType=spectrum" );
        }
        catch (MalformedURLException e) {
            //  Shouldn't happen, but just in case make somekind of report.
            e.printStackTrace();
        }

    }
}
