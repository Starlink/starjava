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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import uk.ac.starlink.auth.AuthManager;
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
     * The result may be a hierarchical tree if continuation nodes
     * are present in the document.
     *
     * <p>Any fragment identifier on the URL is ignored.
     *
     * @param  url  location of examples document
     * @return   list of examples
     */
    public List<Tree<DaliExample>> readExamples( URL url ) throws IOException {
        InputStream in = AuthManager.getInstance().openStream( url );
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

        /* Pick up example and continuation nodes.
         * Earlier versions of this class required that the @typeof='example'
         * element occurred in the scope of an element with a suitable
         * @vocab attribute.  But the requirement on the content of that
         * vocab is really a mess: the TAP Implementation Note,
         * DALI 1.0 and DALI 1.1 all say different things, and at time
         * of writing most known services have something else again.
         * Following discussion with Markus Demleitner, we now just ignore
         * the RDFa vocab altogether, and pick up anything marked with a
         * @typeof='example' attribute.  This is very likely to do the
         * right thing. */
        String exPath = "//*[@typeof='example']|//*[@property='continuation']";
        List<Tree<DaliExample>> list = new ArrayList<>();
        int nLeaf = 0;
        int nBranch = 0;
        for ( Element el : findElements( doc.getDocumentElement(), exPath ) ) {

            /* Example element turns into a leaf node. */
            if ( "example".equals( el.getAttribute( "typeof" ) ) ) {
                DaliExample daliEx = createExample( el, url );
                list.add( new Tree.Leaf<DaliExample>( daliEx ) );
                nLeaf++;
            }

            /* Continuation element turns into a branch node. */
            else if ( "continuation".equals( el.getAttribute( "property" ) ) ) {

                /* Read sub-list of examples from continuation document. */
                List<Tree<DaliExample>> contList;
                String href = el.getAttribute( "href" );
                if ( href == null || href.trim().length() == 0 ) {
                    contList = null;
                    logger_.warning( "No href attribute for examples "
                                   + "continuation in " + url );
                }
                else {
                    try {
                        contList = readExamples( new URL( url, href ) );
                    }
                    catch ( IOException e ) {
                        contList = null;
                        logger_.log( Level.WARNING,
                                     "Failed to read examples continuation at "
                                   + href, e );
                    }
                }

                /* Add a branch node containing the continuation examples
                 * to the current list of nodes. */
                if ( contList != null && contList.size() > 0 ) {

                    /* Try to come up with a suitable label.
                     * This is not very strongly constrained by the standard,
                     * but the example in DALI 1.1 sec 2.3.4 has
                     * suitable label text as the content of the
                     * property="continuation"-bearing element. */
                    String label = getElementText( el );
                    if ( label == null || label.trim().length() == 0 ) {
                        label = "Continuation";
                    }
                    if ( label != null && label.length() > 50 ) {
                        label = label.substring( 0, 40 ) + "...";
                    }
                    list.add( new Tree.Branch<DaliExample>( contList, label ) );
                    nBranch++;
                }
            }
        }
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "Read examples document from " )
            .append( url )
            .append( ": " )
            .append( nLeaf )
            .append( " examples" );
        if ( nBranch > 0 ) {
            sbuf.append( ", " )
                .append( nBranch )
                .append( " continuations" );
        }
        logger_.info( sbuf.toString() );
        return list;
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
        String name0 = getPropertyText( exEl, "name" );
        final String name = name0 == null
                          ? null
                          : name0.trim().replaceAll( "\\s+", " " );
        final String capability = getPropertyText( exEl, "capability" );
        String paramPath =
            ".//*[@property='generic-parameter' and @typeof='keyval']";
        final Map<String,String> paramMap = new LinkedHashMap<String,String>();
        for ( Element paramEl : findElements( exEl, paramPath ) ) {
            String key = getPropertyText( paramEl, "key" );
            String value = getPropertyText( paramEl, "value" );
            paramMap.put( key, value );
        }
        final Map<String,String> propMap = new LinkedHashMap<String,String>();
        String propPath = ".//*[@property"
                            + " and not(@property='generic-parameter')"
                            + " and not(../@property='generic-parameter')]";
        for ( Element propEl : findElements( exEl, propPath ) ) {
            String key = propEl.getAttribute( "property" );
            String value = getElementText( propEl );
            propMap.put( key, value );
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
            public Map<String,String> getProperties() {
                return Collections.unmodifiableMap( propMap );
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
            logger_.log( Level.WARNING,
                         "Bad XPath expression: " + findPath, e );
            return new Element[ 0 ];
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
        String propPath = ".//*[@property='" + propName + "']";
        Element propEl;
        try {
            propEl = (Element) xpath_.evaluate( propPath, contextEl,
                                                XPathConstants.NODE );
        }
        catch ( XPathExpressionException e ) {
            logger_.log( Level.WARNING,
                         "Bad XPath expression: " + propPath, e );
            return null;
        }
        return getElementText( propEl );
    }

    /**
     * Returns the text content of an RDFa element.
     * If the element sports the (RDFa) @content attribute, its value is used.
     * Otherwise, all descendent text nodes are concatenated
     * (ignoring markup).
     *
     * @param  el  element
     * @return   text content
     */
    private String getElementText( Element el ) {
        if ( el == null ) {
            return null;
        }

        /* If RDFa content attribute is present, use its value, otherwise
         * extract the plain text content of the element and use that. */
        else if ( el.hasAttribute( "content" ) ) {
            return el.getAttribute( "content" );
        }
        else {
            NodeList nl;
            final String txtPath = ".//text()";
            try {
                nl = (NodeList) xpath_.evaluate( txtPath, el,
                                                 XPathConstants.NODESET );
            }

            /* XPath error shouldn't happen, but if it does fall back on
             * a safer approach. */
            catch ( XPathExpressionException e ) {
                logger_.log( Level.WARNING,
                             "Bad XPath expression: " + txtPath, e );
                return DOMUtils.getTextContent( el );
            }
            StringBuffer sbuf = new StringBuffer();
            int nn = nl.getLength();
            for ( int i = 0; i < nn; i++ ) {
                sbuf.append( nl.item( i ).getTextContent() );
            }
            return sbuf.toString();
        }
    }

    /**
     * Reports basic information about the examples in a document at a
     * URL supplied on the command line.
     */
    public static void main( String[] args ) throws IOException {
        for ( Tree<DaliExample> tree :
              new DaliExampleReader().readExamples( new URL( args[ 0 ] ) ) ) {
            writeTree( 0, tree );
        }
    }

    /**
     * Recursively writes a representation of a tree node to standard output.
     *
     * @param  level  indentation level for output
     * @param  tree  node to write
     */
    private static void writeTree( int level, Tree<DaliExample> tree ) {
        if ( tree.isLeaf() ) {
            DaliExample ex = tree.asLeaf().getItem();
            output( level, ex.getId() + ": " + ex.getName() );
            output( level + 1, "generic-parameters:" );
            for ( Map.Entry<String,String> entry :
                  ex.getGenericParameters().entrySet() ) {
                output( level + 2, entry.getKey() + " -> " + entry.getValue() );
            }
            output( level + 1, "properties:" );
            for ( Map.Entry<String,String> entry :
                  ex.getProperties().entrySet() ) {
                output( level + 2, entry.getKey() + " -> " + entry.getValue() );
            }
            output( 0, "" );
        }
        else {
            Tree.Branch<DaliExample> branch = tree.asBranch();
            String label = branch.getLabel();
            if ( label != null && label.trim().length() > 0 ) {
                output( level + 1, label + ":" );
                output( level + 1, Stream.generate( () -> "-" )
                                         .limit( label.length() + 1 )
                                         .collect( Collectors.joining() ) );
            }
            for ( Tree<DaliExample> child : branch.getChildren() ) {
                writeTree( level + 1, child );
            }
        }
    }

    /**
     * Writes text with a given indentation level to standard output.
     *
     */
    private static void output( int level, String txt ) {
        String prefix = Stream.generate( () -> "  " )
                       .limit( level )
                       .collect( Collectors.joining() );
        System.out.println( prefix +
                            txt.replaceAll( "\n", "\n" + prefix + "  " ) );
    }
}
