/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-NOV-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.vo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.SourceReader;


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
        throws SplatException
    {
        restoreKnownServers();
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
        addServer( description, baseURL, true );
    }

    /**
     * Add an SSA server to the known list.
     *
     * @param description a human readable description of the service.
     * @param baseURL the URL for accessing the service (including trailing ?).
     * @param save if true then the backing store of servers should be updated.
     *
     * @throws MalFormedURLException is the URL is invalid.
     */
    protected void addServer( String description, String baseURL,
                              boolean save )
        throws MalformedURLException
    {
        addServer( new SSAServer( description, baseURL ), save );
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
        addServer( server, true );
    }

    /**
     * Add an SSA server to the known list.
     *
     * @param server an instance of {@link SSAServer}.
     * @param save if true then the backing store of servers should be updated.
     *
     * @throws MalFormedURLException is the URL is invalid.
     */
    protected void addServer( SSAServer server, boolean save )
        throws MalformedURLException
    {
        serverList.add( server );
        if ( save ) {
            saveServers();
        }
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
     * Initialise the known servers as we don't have Registry access
     * yet. These are kept in a resource file along with SPLAT. The format of
     * this file is XML with root element <serverlist> containing only
     * elements <server> with attributes "description" and "baseurl".
     */
    protected void restoreKnownServers()
        throws SplatException
    {
        //  Locate the description file. This may exist in the user's
        //  application specific directory or, the first time, as part of the
        //  application resources.
        File backingStore = Utilities.getConfigFile( "SSAPServerList.xml" );
        InputStream inputStream = null;
        boolean needSave = false;
        if ( backingStore.canRead() ) {
            try {
                inputStream = new FileInputStream( backingStore );
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if ( inputStream == null ) {
            //  Look for the built-in version.
            inputStream =
                SSAServerList.class.getResourceAsStream( "serverlist.xml" );
            needSave = true;
        }

        if ( inputStream == null ) {
            // That's bad. Need to complain.
            throw new SplatException( "Failed to find a SSAP server listing" );
        }

        //  Read the stream into a DOM.
        StreamSource saxSource = new StreamSource( inputStream );
        Document document = null;
        try {
            document = (Document) new SourceReader().getDOM( saxSource );
        }
        catch ( Exception e ) {
            document = null;
            throw new SplatException( e );
        }

        //  And extract the <server> tags, with attributes description and
        //  baseurl.
        Element rootElement = document.getDocumentElement();
        NodeList nodeList = rootElement.getChildNodes();
        Element element;
        String description;
        String baseURL;
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
            if ( nodeList.item( i ) instanceof Element ) {
                element = (Element) nodeList.item( i );
                description = element.getAttribute( "description" );
                baseURL = element.getAttribute( "baseurl" );
                try {
                    addServer( description, baseURL, false );
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            inputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Save the current state back to disk if we're using the resource
        // file.
        if ( needSave ) {
            saveServers();
        }
    }

    /**
     * Save the current list of servers to the backing store configuration
     * file.
     */
    protected void saveServers()
    {
        File backingStore = Utilities.getConfigFile( "SSAPServerList" );
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream( backingStore );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            return;
        }

        //  Create a DOM from the serverList.
        Document document = null;
        try {
            DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.newDocument();
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        
        Element rootElement = document.createElement( "serverlist" );
        document.appendChild( rootElement );

        Iterator i = serverList.iterator();
        SSAServer server = null;
        Element element = null;
        while ( i.hasNext() ) {
            server = (SSAServer) i.next();
            element = document.createElement( "server" );
            element.setAttribute( "description", server.getDescription() );
            element.setAttribute( "baseurl", server.getBaseURL().toString() );
            rootElement.appendChild( element );
        }

        StreamResult out = new StreamResult( outputStream );
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = null;
        try {
            t = tf.newTransformer();
        }
        catch (TransformerConfigurationException e) {
            e.printStackTrace();
            return;
        }

        //?? User can type in funny characters??
        t.setOutputProperty( OutputKeys.ENCODING, "ISO-8859-1" );
        t.setOutputProperty( OutputKeys.INDENT, "yes" );
        t.setOutputProperty( OutputKeys.STANDALONE, "yes" );

        try {
            t.transform( new DOMSource( document ), out );
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
