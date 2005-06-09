/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     10-NOV-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.vo;

import java.net.URL;
import java.net.MalformedURLException;

import jsky.coords.DMS;
import jsky.coords.HMS;

import org.us_vo.www.SimpleResource;

import uk.ac.starlink.table.StarTable;

/**
 * Construct a URL query for contacting an SSA server. Also hold various
 * results from that query (the {@link StarTable} form of the downloaded
 * VOTable) and details of the originating server (the description).
 * <p>
 * The form of the query follows the Simple Spectral Access Protocol
 * standard developed by the IVOA.
 * <p>
 * Still to do: the optional query parameters, band, verb, TemporalCoverage &
 * SpatialResolution. Are these the right names, that's not clear from the
 * IVOA poster-v-standard.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class SSAQuery
{
    /** The SSA Service base URL */
    private String baseURL = null;

    /** The SSA Service description (not used as part of query) */
    private String description = null;

    /** RA of the query */
    private double queryRA = 0.0;

    /** Dec of the query */
    private double queryDec = 0.0;

    /** Radius of the query */
    private double queryRadius = 0.0;

    /** The format of any returned spectra */
    private String queryFormat = null;

    /** The StarTable formed from the results of the query */
    private StarTable starTable = null;

    /**
     * Create an instance with the given base URL for an SSA service.
     */
    public SSAQuery( String baseURL )
    {
        this.baseURL = baseURL;
    }

    /**
     * Create an instance with the given SSA service.
     */
    public SSAQuery( SimpleResource server )
    {
        this.baseURL = server.getServiceURL();
        this.description = server.getShortName();
    }

    /**
     * Set the position used for the query. The values are in degrees and must
     * be in ICRS (FK5/J2000 will do if accuracy isn't a problem).
     */
    public void setPosition( double queryRA, double queryDec )
    {
        this.queryRA = queryRA;
        this.queryDec = queryDec;
    }

    /**
     * Set the position used for the query. The values are in sexigesimal
     * be in ICRS (FK5/J2000 will do if accuracy isn't a problem).
     */
    public void setPosition( String queryRA, String queryDec )
    {
        HMS hms = new HMS( queryRA );
        DMS dms = new DMS( queryDec );
        setPosition( hms.getVal() * 15.0, dms.getVal() );
    }

    /**
     * Set the radius of the query. The actual value is in decimal degrees,
     * but we accept the more human scale arcminutes.
     */
    public void setRadius( double queryRadius )
    {
        this.queryRadius = queryRadius / 60.0;
    }

    /**
     * Set the format that we want the spectra in. For SPLAT we clearly
     * require spectral data in FITS (VOTable will also do when we know how to
     * deal with spectral coordinates), but we cannot use that by default as
     * one of the servers currently doesn't support that query. Formats are
     * MIME types (application/fits, application/x-votable+xml etc.).
     */
    public void setFormat( String queryFormat )
    {
        this.queryFormat = queryFormat;
    }

    /**
     * Set the description of the service, just some human readable format
     * String. 
     */
    public void setDescription( String description )
    {
        this.description = description;
    }

    /**
     * Get the description of the service.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Set the StarTable created as a result of downloading the VOTable.
     */
    public void setStarTable( StarTable starTable )
    {
        this.starTable = starTable;
    }

    /**
     * Get then StarTable, if defined, if not return null.
     */
    public StarTable getStarTable()
    {
        return starTable;
    }

    /**
     * Get the constructed query as a URL. This should be used to contact the
     * server and the content downloaded as a VOTable (which should then be
     * used to create a StarTable).
     */
    public URL getQueryURL()
        throws MalformedURLException
    {
        //  Note that some baseURLs may have an embedded ?, in which case we
        //  need to use & to append the pos argument.
        StringBuffer buffer = new StringBuffer( baseURL );
        if ( baseURL.indexOf( '?' ) == -1 ) {
            buffer.append( "?" );
        }
        else if ( ! baseURL.endsWith( "?" ) ) {
            buffer.append( "&" );
        }

        // Servers may have a case sensitivity issue! 
        // INES requires uppercase.
        buffer.append( "POS=" + queryRA + "," + queryDec );
        if ( queryFormat != null ) {
            buffer.append( "&FORMAT=" + queryFormat );
        }
        buffer.append( "&SIZE=" + queryRadius );
        return new URL( buffer.toString() );
    }
}
