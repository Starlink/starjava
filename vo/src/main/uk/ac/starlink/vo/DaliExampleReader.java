package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
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
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.DOMUtils;

/**
 * Extracts DaliExample objects from a document.
 * The document is probably XHTML with RDFa markup.
 *
 * <p>Instances of this class are not thread-safe.
 *
 * @author   Mark Taylor
 * @since    12 May 2015
 * @see  <a href="http://www.ivoa.net/documents/DALI/index.html"
 *          >DALI v1.0 sec 2.3</a>
 */
public class DaliExampleReader {

    private final XPath xpath_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public DaliExampleReader() {
        xpath_ = XPathFactory.newInstance().newXPath();
    }

    /**
     * Reads a list of examples from a document at a given URL.
     * Any fragment identifier on the URL is ignored.
     *
     * @param  url  location of examples document
     * @return   list of examples
     */
    public DaliExample[] readExamples( URL url ) throws IOException {
        InputStream in = url.openStream();
        Document doc;
        try {
            DocumentBuilder db =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();

            /* Treat external entities as blank.  This is mainly to avoid
             * trying to download the DTD declared in the Document Type
             * Declaration, which is typically from w3.org and has
             * a long timeout.  It's possible that it might cause trouble
             * for documents that use XInclude or something. */
            db.setEntityResolver( new EntityResolver() {
                public InputSource resolveEntity( String publicId,
                                                  String systemId ) {
                    logger_.config( "Ignoring external entity "
                                  + "\"" + publicId + "\", \"" + systemId + "\""
                                  + " - treat as empty" );
                    return new InputSource( new StringReader( "" ) );
                }
            } );
            doc = db.parse( in );
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException) new IOException( "Parser config" )
                               .initCause( e );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "XML error in examples document at "
                                 + url + ": " + e )
                 .initCause( e );
        }
        String exPath = "//"
                      + "*[@vocab='ivo://ivoa.net/std/DALI-examples#']"
                      + "//"
                      + "*[@typeof='example']";
        List<DaliExample> list = new ArrayList<DaliExample>();
        for ( Element exampleEl :
              findElements( doc.getDocumentElement(), exPath ) ) {
            list.add( createExample( exampleEl, url ) );
        }
        return list.toArray( new DaliExample[ 0 ] );
    }

    /**
     * Creates an example from a given DOM element.
     *
     * @param  exEl  element node with DALI RDFa example content
     * @param  docUrl   base URL of host document
     * @return   example object
     */
    public DaliExample createExample( final Element exEl, URL docUrl )
            throws IOException {
        final String idAtt = exEl.getAttribute( "id" );
        final String resourceAtt = exEl.getAttribute( "resource" );
        final URL exUrl = new URL( docUrl, "#" + idAtt );
        final String name = getPropertyText( exEl, "name" );
        final String capability = getPropertyText( exEl, "capability" );
        String paramPath =
            ".//*[@property='generic-parameter' and @typeof='keyval']";
        final Map<String,String> paramMap = new LinkedHashMap<String,String>();
        for ( Element paramEl : findElements( exEl, paramPath ) ) {
            String key = getPropertyText( paramEl, "key" );
            String value = getPropertyText( paramEl, "value" );
            paramMap.put( key, value );
        }
        return new DaliExample() {
            public URL getUrl() {
                return exUrl;
            }
            public Element getElement() {
                return exEl;
            }
            public String getId() {
                return idAtt;
            }
            public String getCapability() {
                return capability;
            }
            public String getName() {
                return name;
            }
            public Map<String,String> getGenericParameters() {
                return Collections.unmodifiableMap( paramMap );
            }
        };
    }

    /**
     * Extracts elements from a given ancestor as located using a given
     * XPath string.
     *
     * @param  contextEl  context (ancestor) element
     * @param  findPath   XPath string resolving to a list of element nodes
     */
    private Element[] findElements( Element contextEl, String findPath )
            throws IOException {
        NodeList nl;
        try {
            nl = (NodeList) xpath_.evaluate( findPath, contextEl,
                                             XPathConstants.NODESET );
        }
        catch ( XPathExpressionException e ) {
            throw (IOException) new IOException( "Xpath: " + findPath )
                               .initCause( e );
        }
        int nn = nl.getLength();
        List<Element> elList = new ArrayList<Element>( nn );
        for ( int i = 0; i < nn; i++ ) {
            Node node = nl.item( i );
            if ( node instanceof Element ) {
                elList.add( (Element) node );
            }
        }
        return elList.toArray( new Element[ 0 ] );
    }

    /**
     * Extracts plain text content from a node with a given RDFa property
     * attribute which is found within a given ancestor.
     * The RDFa @content attribute is used in preference to actual element
     * content if present.
     *
     * @param   contextEl   context (ancestor) element
     * @param   propName   value of property attribute identifying
     *                     target element
     * @return  plain text content of target element, or null if no target node
     */
    private String getPropertyText( Element contextEl, String propName )
            throws IOException {

        /* Identify the target element. */
        String propPath = ".//*[@property='" + propName + "']";
        Element propEl;
        try {
            propEl = (Element) xpath_.evaluate( propPath, contextEl,
                                                XPathConstants.NODE );
        }
        catch ( XPathExpressionException e ) {
            throw (IOException) new IOException( "XPath: " + propPath )
                               .initCause( e );
        }
        if ( propEl == null ) {
            return null;
        }

        /* If RDFa content attribute is present, use its value, otherwise
         * extract the plain text content of the element and use that. */
        return propEl.hasAttribute( "content" )
             ? propEl.getAttribute( "content" )
             : DOMUtils.getTextContent( propEl );
    }

    /**
     * Reports basic information about the examples in a document at a
     * URL supplied on the command line.
     */
    public static void main( String[] args ) throws IOException {
        for ( DaliExample ex :
              new DaliExampleReader().readExamples( new URL( args[ 0 ] ) ) ) {
            System.out.println( ex.getId() + ": " + ex.getName() );
            for ( Map.Entry<String,String> entry :
                  ex.getGenericParameters().entrySet() ) {
                System.out.println( "\t" + entry.getKey()
                                  + "\t" + entry.getValue() );
            }
            System.out.println();
        }
    }
}
