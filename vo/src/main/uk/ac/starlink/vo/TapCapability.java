package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.DOMUtils;

/**
 * Describes the capabilities of a TAP service as serialized by the
 * TAPRegExt schema.
 *
 * @author   Mark Taylor
 * @since    7 Mar 2011
 */
public abstract class TapCapability {

    /**
     * Base URI for standard uploads string {@value}.
     * Standard upload methods are formed by appending the upload type,
     * for instance the HTTP upload capability is indicated by the
     * string <code>TapCapability.UPLOADS_URL+"http"</code>.
     */ 
    public static final String UPLOADS_URI =
        "ivo://ivoa.net/std/TAPRegExt#upload-";

    /**
     * Returns an array of upload methods known by this capability.
     *
     * @return  uploadMethod element ivo-id attribute values
     */
    public abstract String[] getUploadMethods();

    /**
     * Returns an array of query language specifiers known by this capability.
     *
     * @return  array of lang-version strings
     */
    public abstract String[] getLanguages();

    /**
     * Returns an array of data models known by this capability.
     *
     * @return   dataModel element ivo-id attribute values
     */
    public abstract String[] getDataModels();

    /**
     * Returns an array of limit values representing the data limits for
     * result tables.
     * Legal values for limit units are "rows" or "bytes".
     *
     * @return   output table limits
     */
    public abstract TapLimit[] getOutputLimits();

    /**
     * Returns an array of limit values representing the data limits for
     * uploaded tables.
     * Legal values for limit units are "rows" or "bytes".
     *
     * @return   upload table limits
     */
    public abstract TapLimit[] getUploadLimits();

    /**
     * Returns an array of limit values representing the time limits for
     * query execution.
     * The limit units will be "seconds".
     *
     * @return   execution time limits
     */
    public abstract TapLimit[] getExecutionLimits();

    /**
     * Returns an array of limit values representing the time limits for
     * query retention.
     * The limit units will be "seconds".
     *
     * @return   retention time limits
     */
    public abstract TapLimit[] getRetentionLimits();

    /**
     * Reads a TAPRegExt document from a given URL and returns a TapCapability
     * object based on it.
     *
     * @param   url   location of document
     * @return  capability object
     */
    public static TapCapability readTapCapability( URL url )
            throws IOException, SAXException {
        try {
            return attemptReadTapCapability( url );
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException) new IOException( "Parser setup failed" )
                               .initCause( e );
        }
        catch ( XPathExpressionException e ) {
            throw (IOException) new IOException( "XPath programming error?" )
                               .initCause( e );
        }
    }

    /**
     * Attempts to read a TAPRegExt document from a given URL.
     *
     * @param   url   location of document
     * @return  capability object
     */
    private static TapCapability attemptReadTapCapability( URL url )
            throws ParserConfigurationException, XPathExpressionException,
                   IOException, SAXException {

        /* Parse and prepare for document interrogation. */
        Document capsDoc = DocumentBuilderFactory.newInstance()
                          .newDocumentBuilder()
                          .parse( new BufferedInputStream( url.openStream() ) );
        XPath xpath = XPathFactory.newInstance().newXPath();
        String capXpath = "capability[@standardID='ivo://ivoa.net/std/TAP']";
        Node capNode =
            (Node) xpath.evaluate( capXpath, capsDoc.getDocumentElement(),
                                   XPathConstants.NODE );
        if ( capNode == null ) {
            throw new IOException( "No element \"" + capXpath + "\""
                                 + " at " + url );
        }

        /* Get upload methods. */
        NodeList upNodeList =
            (NodeList) xpath.evaluate( "uploadMethod/@ivo-id",
                                       capNode, XPathConstants.NODESET );
        List<String> upList = new ArrayList<String>();
        for ( int i = 0; i < upNodeList.getLength(); i++ ) {
            upList.add( upNodeList.item( i ).getNodeValue() );
        }
        final String[] uploadMethods = upList.toArray( new String[ 0 ] );

        /* Get data models. */
        NodeList dmNodeList =
            (NodeList) xpath.evaluate( "dataModel/@ivo-id",
                                       capNode, XPathConstants.NODESET );
        List<String> dmList = new ArrayList<String>();
        for ( int i = 0; i < dmNodeList.getLength(); i++ ) {
            dmList.add( dmNodeList.item( i ).getNodeValue() );
        }
        final String[] dataModels = dmList.toArray( new String[ 0 ] );

        /* Get languages. */
        NodeList langNodeList =
            (NodeList) xpath.evaluate( "language",
                                       capNode, XPathConstants.NODESET );
        List<String> langList = new ArrayList<String>();
        for ( int il = 0; il < langNodeList.getLength(); il++ ) {
            Node lang = langNodeList.item( il );
            String name =
                (String) xpath.evaluate( "./name/text()",
                                         lang, XPathConstants.STRING );
            NodeList versNodeList =
                (NodeList) xpath.evaluate( "./version/text()",
                                           lang, XPathConstants.NODESET );
            for ( int iv = 0; iv < versNodeList.getLength(); iv++ ) {
                String version = versNodeList.item( iv ).getNodeValue();
                langList.add( name + "-" + version );
            }
        }
        final String[] languages = langList.toArray( new String[ 0 ] );

        /* Get various limits. */
        final TapLimit[] outputLimits =
            getLimits( (NodeList) xpath.evaluate( "outputLimit/*", capNode,
                                                  XPathConstants.NODESET ),
                       null );
        final TapLimit[] uploadLimits =
            getLimits( (NodeList) xpath.evaluate( "uploadLimit/*", capNode,
                                                  XPathConstants.NODESET ),
                       null );
        final TapLimit[] retentionLimits =
            getLimits( (NodeList) xpath.evaluate( "retentionPeriod/*", capNode,
                                                  XPathConstants.NODESET ),
                       TapLimit.SECONDS );
        final TapLimit[] executionLimits =
            getLimits( (NodeList) xpath.evaluate( "executionDuration/*",
                                                  capNode,
                                                  XPathConstants.NODESET ),
                       TapLimit.SECONDS );

        /* Construct and return a new TapCapability. */
        return new TapCapability() {
            public String[] getUploadMethods() {
                return uploadMethods;
            }
            public String[] getLanguages() {
                return languages;
            }
            public String[] getDataModels() {
                return dataModels;
            }
            public TapLimit[] getOutputLimits() {
                return outputLimits;
            }
            public TapLimit[] getUploadLimits() {
                return uploadLimits;
            }
            public TapLimit[] getExecutionLimits() {
                return executionLimits;
            }
            public TapLimit[] getRetentionLimits() {
                return retentionLimits;
            }
            public String toString() {
                return "uploadMethods: " + Arrays.asList( uploadMethods ) + "; "
                     + "languages: " + Arrays.asList( languages ) + "; "
                     + "dataModels: " + Arrays.asList( dataModels ) + "; "
                     + "outputLimits: " + Arrays.asList( outputLimits ) + "; "
                     + "uploadLimits: " + Arrays.asList( uploadLimits ) + "; "
                     + "execLimits: " + Arrays.asList( executionLimits ) + "; "
                     + "retentLimits: " + Arrays.asList( retentionLimits );
            }
        };
    }

    /**
     * Reads a list of nodes representing limits in TAPRegExt format
     * and returns an array of corresponding objects.
     *
     * @param  nodeList  list of <code>hard</code> or <code>default</code>
     *                   elements
     * @param  fixedUnit  unit string if one is implicit, or null if it is
     *                    represented by a <code>unit</code> attribute
     * @return   array of limit objects
     */
    private static TapLimit[] getLimits( NodeList nodeList, String fixedUnit ) {
        List<TapLimit> limitList = new ArrayList<TapLimit>();
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
            Node node = nodeList.item( i );
            if ( node instanceof Element ) {
                Element el = (Element) node;
                String tagName = el.getTagName();
                Boolean isHard = null;
                if ( "hard".equals( tagName ) ) {
                    isHard = Boolean.TRUE;
                }
                else if ( "default".equals( tagName ) ) {
                    isHard = Boolean.FALSE;
                }
                if ( isHard != null ) {
                    String unit = fixedUnit == null ? el.getAttribute( "unit" )                                                     : fixedUnit;
                    String text = DOMUtils.getTextContent( el );
                    try {
                        long value = Long.parseLong( text.trim() );
                        limitList.add( new TapLimit( value,
                                                     isHard.booleanValue(),
                                                     unit ) );
                    }
                    catch ( NumberFormatException e ) {
                        // too bad
                    }
                }
            }
        }
        return limitList.toArray( new TapLimit[ 0 ] );
    }

    public static void main( String[] args ) throws IOException, SAXException {
        System.out.println( readTapCapability( new URL( args[ 0 ] ) ) );
    }
}
