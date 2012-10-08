/*
 * Copyright (C) 2001-2005 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     29-Feb-2012 (Margarida Castro Neves mcneves@ari.uni-heidelberg.de)
 *        Original version.
 */
package uk.ac.starlink.splat.vo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import uk.ac.starlink.splat.iface.ProgressPanel;
import uk.ac.starlink.vo.RegCapabilityInterface;
import uk.ac.starlink.vo.RegResource;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;


/**
 * Class SSAMetadataParser
 * 
 * This class handles  metadata requests to the SSA server.
 * It's methods perform queries to a SSA Server, parse the 
 * resulting VOTable and extract its metadata parameters.
 * 
 * @author Margarida Castro Neves
 */

class SSAMetadataParser 
{

    // Logger.
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.splat.vo.SSAMetadataParser" );

    /** The SSA Service base URL */
    private String baseURL = null;

    /** The SSA query  URL */
    private static String query = "REQUEST=queryData&FORMAT=METADATA";

    /** the server short name */
    private String server;
    /**
     * Constructor: 
     * @param  server  - is the SSA server to be queried for metadata
     */ 

    public SSAMetadataParser( SSAPRegResource server ) 
    {
        SSAPRegCapability[] rci = server.getCapabilities();
        this.baseURL = rci[0].getAccessUrl(); 
        this.server = server.getShortName();
     
    }


    /**
     * getQueryURL 
     * creates a metadata query url from the server base url.  
     * 
     * @return the query URL 
     * @throws MalformedURLException
     */
    public URL getQueryURL() throws MalformedURLException
    {
        //  Note that some baseURLs may have an embedded ?.
        StringBuffer buffer = new StringBuffer( baseURL );
        if ( baseURL.indexOf( '?' ) == -1 ) {
            //  No '?' in URL.
            buffer.append( "?" );
        }
        else if ( ! baseURL.endsWith( "?" ) ) {
            //  Have ? but not at end.  (  .../?a=b )
            if ( ! baseURL.endsWith( "&" ) )
                buffer.append( "&" );
            // do nothing if there's already a ? and a &  (  .../?a=b& )
        }
        buffer.append(query);
        logger.info("QUERY = "+ buffer.toString());
        return new URL( buffer.toString() );
    }


    /**
     * QueryMetadata 
     * makes a query and parses the resulting VOTable 
     * (no progress panel display)
     * 
     * @param url
     * @return the PARAM elements which are children of this table
     * @throws InterruptedException
     */
    public ParamElement[] queryMetadata( URL url )
            throws InterruptedException { //throws SAXException, ParserConfigurationException, IOException {

        return queryMetadata(  url , null );
    }


    /**
     * QueryMetadata 
     * makes a query and parses the resulting VOTable, updating a progress panel
     * 
     * @param url the ssa server url  to be  queried
     * @param progressPanel the progress panel to display the  query progress
     * @return the PARAM elements which are children of this table, or null if failed
     * @throws InterruptedException
     */
    public ParamElement[] queryMetadata( URL url , ProgressPanel progressPanel )
            throws InterruptedException { //throws SAXException, ParserConfigurationException, IOException {

        InputSource inSrc=null;
        VOElement voElement = null;

 
        // open the URL
        try {
           // inSrc = new InputSource( queryCon.getInputStream() );
            inSrc = new InputSource( url.openStream() );            
        } catch (IOException ioe) {
            if ( progressPanel != null )
                progressPanel.logMessage( ioe.getMessage() );
            logger.info( "RESPONSE Connection IOException from " + url + " "+ioe.getMessage() );
            return null;  
        } 


        SAXParserFactory spfact = SAXParserFactory.newInstance();
        try {  // don't load external validation dtds, avoiding so long parsing delays.
            spfact.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (SAXNotRecognizedException e) {
        } catch (SAXNotSupportedException e) {
        } catch (ParserConfigurationException e) {
        }

        XMLReader streamReader = null;

        // configure reader and parser
        try {
            streamReader = spfact.newSAXParser().getXMLReader();       
            // don't load external validation dtds, avoiding so long parsing delays.
            streamReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (SAXException se) {
            if ( progressPanel != null )
                progressPanel.logMessage( "SAX parser configuration error"+se.getMessage() );
            logger.info( "RESPONSE SAXException when configuring parser" + url );
            return null;
        } catch (ParserConfigurationException pce) {
            if ( progressPanel != null )
                progressPanel.logMessage( "Parser configuration error"+pce.getMessage());
            logger.info( "RESPONSE ParserConfigurationException when configuring " + url );
            return null;
        }

        Source xsrc = new SAXSource( streamReader, inSrc );

        VOElementFactory vofact = new VOElementFactory();

        // parse the VO Elements
        try {
            voElement = vofact.makeVOElement( xsrc );
            if ( Thread.interrupted() ) {
                throw new InterruptedException();
            }
        } catch (SAXException se) {
            if ( progressPanel != null )
                progressPanel.logMessage( "SAXException when parsing" );
            logger.info( "RESPONSE SAXException when parsing " + url );
            return null;
        } catch (IOException ioe2) {
            if ( progressPanel != null )
                progressPanel.logMessage( "IOException when parsing" );
            logger.info( "RESPONSE IOException when parsing " + url );          
            return null;
        }
        if ( Thread.interrupted() ) 
        {
            throw new InterruptedException();
        }
        // parse and return the relevant elements 
        VOElement resource = voElement.getChildByName("RESOURCE");
        if (resource != null) 
        {
            return getInputParams(resource);
        } else {
            progressPanel.logMessage( "No input parameters found");
            return null;
        }

    }


    /**
     * Returns the PARAM elements for this table. The server name is also added to the list.
     *
     * @param voe - the VO Element containing the parameters
     * @return the PARAM elements which are children of this table
     */
    public ParamElement[] getInputParams(VOElement voe) {
        List<ParamElement> paramList = new ArrayList<ParamElement>();
        VOElement[] voels = voe.getChildrenByName( "PARAM" );
        int i=0;
        while ( i < voels.length ) {
            if (voels[i].getName().startsWith("INPUT:")) {
                ParamElement pel = (ParamElement) voels[i];
                paramList.add(pel);
            }
            i++;
        }
        return (ParamElement[]) paramList.toArray( new ParamElement[ 0 ] );
    }

}
