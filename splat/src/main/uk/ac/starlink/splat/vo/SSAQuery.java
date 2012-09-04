/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     10-NOV-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.vo;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLEncoder;

import jsky.coords.DMS;
import jsky.coords.HMS;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;

import uk.ac.starlink.vo.RegCapabilityInterface;
import uk.ac.starlink.vo.RegResource;

/**
 * Construct a URL query for contacting an SSA server. Also hold various
 * results from that query (the {@link StarTable} form of the downloaded
 * VOTable) and details of the originating server (the description).
 * <p>
 * The form of the query follows the Simple Spectral Access Protocol
 * standard developed by the IVOA.
 * <p>
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SSAQuery
{
    /** Assume we support SSAP version 1.0 */
    private static String SSAPVERSION = "VERSION=1.0";

    /** The SSA Service base URL */
    private String baseURL = null;

    /** The SSA Service description (not used as part of query) */
    private String description = null;

    /** RA of the query */
    private double queryRA = 0.0;

    /** Dec of the query */
    private double queryDec = 0.0;

    /** name of the target object, used if RA or Dec are null */
    private String targetName = null;

    /** Radius of the query */
    private double queryRadius = 0.0;

    /** The format of any returned spectra, we ask for this. */
    private String queryFormat = null;

    /** The wavelength calibration option. */
    private String waveCalib = null;

    /** The flux calibration option. */
    private String fluxCalib = null;

    /** Upper spectral wavelength in metres */
    private String queryBandUpper = null;

    /** Lower spectral wavelength in metres */
    private String queryBandLower = null;

    /** Upper time coverage in ISO 8601 format */
    private String queryTimeUpper = null;

    /** Lower time coveragre in ISO 8601 format */
    private String queryTimeLower = null;

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
    public SSAQuery( RegResource server )
    {
        RegCapabilityInterface[] rci = server.getCapabilities();
        this.baseURL = rci[0].getAccessUrl();  //  Fudge one capability per
                                               //  interface. 
        this.description = server.getShortName();
    }

    /**
     * Set the position used for the query. The values are in degrees and must
     * be in ICRS (FK5/J2000 will do if accuracy isn't a problem). To not
     * use a position (and use a target name), set queryRA to -1.
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
        if ( queryRA != null ) {
            HMS hms = new HMS( queryRA );
            DMS dms = new DMS( queryDec );
            setPosition( hms.getVal() * 15.0, dms.getVal() );
        }
        else {
            //  Null values, may be using targetName. Set some out of bounds
            //  values to raise an error if these are used.
            setPosition( -1.0, -91.0 );
        }
    }

    /**
     *  Set the name of the target object. Only used if RA and Dec are not
     *  set.
     */
    public void setTargetName( String targetName )
    {
        this.targetName = targetName;
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
     * Set the type of wavelength calibration required.
     */
    public void setWaveCalib( String waveCalib )
    {
        this.waveCalib = waveCalib;
    }

    /**
     * Set the type of flux calibration required.
     */
    public void setFluxCalib( String fluxCalib )
    {
        this.fluxCalib = fluxCalib;
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
     * Get the base URL for this service.
     */
    public String getBaseURL()
    {
        return baseURL;
    }

    /**
     * Set the query band. The strings must be in meters.
     */
    public void setBand( String lower, String upper )
    {
       
        queryBandLower = lower;
        queryBandUpper = upper;
    }

    /**
     * Set the query time coverage. The strings must be in ISO 8601 format.
     */
    public void setTime( String lower, String upper )
    {
        queryTimeLower = lower;
        queryTimeUpper = upper;
    }

    /**
     * Set the StarTable created as a result of downloading the VOTable.
     */
    public void setStarTable( StarTable starTable )
    {
        this.starTable = starTable;

        //  Add a parameter to describe the service (used when restoring
        //  associated query).
        ValueInfo shortNameInfo =
            new DefaultValueInfo( "ShortName", String.class,
                                  "Short description of the SSAP service" );
        starTable.setParameter( new DescribedValue( shortNameInfo, 
                                                    getDescription() ) );
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
        throws MalformedURLException, UnsupportedEncodingException
    {
        //  Note that some baseURLs may have an embedded ?.
        StringBuffer buffer = new StringBuffer( baseURL );
        if ( baseURL.indexOf( '?' ) == -1 ) {
            //  No ? in URL.
            buffer.append( "?" );
        }
        else if ( ! baseURL.endsWith( "?" ) ) {
            //  Have ? but not at end.
            buffer.append( "&" );
        }
        //  Else ends with a ?, so that's OK already.

        //  Start with "VERSION=1.0&REQUEST=queryData".
       // buffer.append( SSAPVERSION + "&REQUEST=queryData" );       
        // (MCN 04.2012) At the moment Splat will not send any version information, because
        // it causes more problems than it helps... To be changed later
        buffer.append( "REQUEST=queryData" );

        //  Add basic search parameters, POS or TARGETNAME, FORMAT and SIZE.
        if ( queryRA >= 0.0 ) {
            buffer.append( "&POS=" + queryRA + "," + queryDec );
        }
        else if ( targetName != null ) {
            buffer.append( "&TARGETNAME=" + URLEncoder.encode(targetName,"UTF-8"));
        }
        if ( queryFormat != null ) {
            buffer.append( "&FORMAT=" + queryFormat );
        }
        buffer.append( "&SIZE=" + queryRadius );

        //  The spectral bandpass. SSAP spec allows "lower/upper" range,
        //  or bounded from above or below.
        if ( queryBandUpper != null && queryBandLower != null ) {
            buffer.append( "&BAND=" + queryBandLower + "/" + queryBandUpper );
        }
        else if ( queryBandUpper != null ) {
            buffer.append( "&BAND=" + "/" + queryBandUpper );
        }
        else if ( queryBandLower != null ) {
            buffer.append( "&BAND=" + queryBandLower + "/");
        }

        //  The time coverage. Assume "lower/upper" range or includes
        //  a single value.
        if ( queryTimeUpper != null && queryTimeLower != null ) {
            buffer.append( "&TIME=" + queryTimeLower + "/" + queryTimeUpper );
        }
        else if ( queryTimeLower != null ) {
            buffer.append( "&TIME=" + queryTimeLower );
        }

        //  Wavelength and flux calibrations.
        if ( waveCalib != null ) {
            buffer.append( "&WAVECALIB=" + waveCalib );
        }
        if ( fluxCalib != null ) {
            buffer.append( "&FLUXCALIB=" + fluxCalib );
        }
        return new URL( buffer.toString() );
    }
}
