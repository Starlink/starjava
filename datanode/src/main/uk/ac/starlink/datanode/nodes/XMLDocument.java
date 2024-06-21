package uk.ac.starlink.datanode.nodes;

import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.dom.DOMSource;
import uk.ac.starlink.util.DataSource;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Holds a DataSource which is known to contain an XML document.
 * DataNode implementations can provide constructors which take one
 * of these.  The idea is that the node building machinery finds out
 * whether a DataSource represents XML (perhaps by doing a SAX parse)
 * and stashes some basic information about it in this object - 
 * name of top-level element, IDs of the DTD, maybe a set of validation
 * errors etc - so that the quick view can display them without
 * any further work.  It deliberately doesn't store the DOM, since
 * keeping DOMs for all the nodes we've encountered can take up too
 * much memory - it violates DataNode's rule about allocating large 
 * memory resources in a node's constructor before <code>getChildren</code> or
 * maybe <code>configureDetail</code> have been called.  DataNode constructors
 * which do operate on an XMLDocument should not construct and cache
 * a DOM themselves, though other DataNode methods may do so.
 *
 * @author   Mark Taylor (Starlink)
 */
public class XMLDocument {

    private final DataSource datsrc;
    private String topLocalName;
    private String topNamespaceURI;
    private Attributes topAtts;
    private Set namespaces;
    private List messages;
    private String systemId;
    private String publicId;

    public static final String[] MAGICS = new String[] { "<!", "<?" };
    public static final String[] ENCODINGS =
        new String[] { "UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE" };

    /**
     * Constructs a new XMLDocument from a DataSource.  Enough work is
     * done to check that the document in question appears to contain XML -
     * if it does not, a NoSuchDataException is thrown.
     *
     * @param  datsrc  data source
     * @throws  NoSuchDataException  if <code>datsrc</code> doesn't contain XML
     */
    public XMLDocument( DataSource datsrc ) throws NoSuchDataException {
        this.datsrc = datsrc;
        InputStream datstrm = null;
        try {

            /* Get a SAX source from the DataSource. */
            datstrm = datsrc.getHybridInputStream();
            InputSource saxsrc = new InputSource( datstrm );
            saxsrc.setSystemId( datsrc.getSystemId() );

            /* Get a basic SAX parser. */
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setNamespaceAware( true );
            spfact.setValidating( false );
            SAXParser sparser = spfact.newSAXParser();
            XMLReader parser = sparser.getXMLReader();
            parser.setFeature( "http://xml.org/sax/features/namespaces", true );

            /* Install a custom entity resolver. */
            parser.setEntityResolver( NodeEntityResolver.getInstance() );

            /* Install a custom error handler. */
            messages = new ArrayList();
            parser.setErrorHandler( new ErrorHandler() {
                public void warning( SAXParseException e ) {
                    messages.add( e.toString() );
                }
                public void error( SAXParseException e ) {
                    messages.add( e.toString() );
                }
                public void fatalError( SAXParseException e )
                        throws SAXParseException {
                    throw e;
                }
            } );

            /* Install a custom content handler. */
            namespaces = new HashSet();
            parser.setContentHandler( new DefaultHandler() {
                boolean started;
                Locator locator;
                public void setDocumentLocator( Locator loc ) {
                    this.locator = loc;
                }
                public void startDocument() {
                    if ( locator != null ) {
                        systemId = locator.getSystemId();
                        publicId = locator.getPublicId();
                    }
                }
                public void startElement( String namespaceURI, String localName,
                                          String qName, Attributes atts ) {
                    if ( ! started ) { 
                        topLocalName = localName;
                        topNamespaceURI = namespaceURI;
                        topAtts = atts;
                        started = true;
                    }
                }
                public void startPrefixMapping( String prefix, String uri ) {
                    namespaces.add( uri );
                }
            } );

            /* Do the parse. */
            parser.parse( saxsrc );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( "I/O error during SAX parse", e );
        }
        catch ( SAXException e ) {
            throw new NoSuchDataException( "SAX parsing error", e );
        }
        catch ( ParserConfigurationException e ) {
            throw new NoSuchDataException( "Error getting default parser - " +
                                           "shouldn't happen", e );
        }
        finally {
            if ( datstrm != null ) {
                try {
                    datstrm.close();
                }
                catch ( IOException e ) {
                }
            }
        }
    }

    public DataSource getDataSource() {
        return datsrc;
    }

    public String getTopLocalName() {
        return topLocalName;
    }

    public String getTopNamespaceURI() {
        return topNamespaceURI;
    }

    public Attributes getTopAttributes() {
        return topAtts;
    }

    public Collection getNamespaces() {
        return namespaces;
    }

    public List getMessages() {
        return messages;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getName() {
        return datsrc.getName();
    }

    /**
     * Performs a parse on the data source represented by this object 
     * and returns the resulting DOM.  Note that this object does <em>not</em>
     * cache such a DOM (the point of it is to stop DOMs being held on to),
     * so only do this if you need it, and if you're going to carry on
     * needing it, cache it.
     *
     * @param    validate  whether the parse should be validating
     * @param    ehandler  handler for parse errors (may be null)
     * @return   DOM source containing the document this object describes
     */
    public DOMSource parseToDOM( boolean validate, ErrorHandler ehandler ) 
            throws SAXException, IOException {

        /* Get a document builder. */
        DocumentBuilder parser;
        try {
            DocumentBuilderFactory dbfact =
                DocumentBuilderFactory.newInstance();
            dbfact.setValidating( validate );
            parser = dbfact.newDocumentBuilder();
        }

        /* Rethrow ParserConfigurationException, which really shouldn't happen
         * under any normal circumstances, as a SAXException which is less
         * boring to catch. */
        catch ( ParserConfigurationException e ) {
            throw (SAXException) new SAXException( "Error configuring normal " +
                                                   "validating parser" )
                                .initCause( e );
        }

        /* Configure the parser. */
        parser.setEntityResolver( NodeEntityResolver.getInstance() );
        if ( ehandler != null ) {
            parser.setErrorHandler( ehandler );
        }

        /* Do the parse. */
        InputStream strm = getDataSource().getInputStream();
        Document doc = parser.parse( strm );
        strm.close();

        /* Return the resulting document as a source. */
        return new DOMSource( doc, datsrc.getSystemId() );
    }

    /**
     * Convenience method to get a DOM from this document, which either
     * succeeds or throws a NoSuchDataException.
     */
    public DOMSource constructDOM( boolean validate )
            throws NoSuchDataException {
        ErrorHandler ehandler = new ErrorHandler() {
            public void warning( SAXParseException e ) {
            }
            public void error( SAXParseException e ) {
            }
            public void fatalError( SAXParseException e ) throws SAXException {
                throw e;
            }
        };
        try {
            return parseToDOM( validate, ehandler );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( "I/O error during SAX parse", e );
        }
        catch ( SAXException e ) {
            throw new NoSuchDataException( "SAX parsing error", e );
        }
    }

    /**
     * This tests for the likely start of an XML file.  It's just a guess
     * though - it can come up with false positives and (worse) false
     * negatives.
     *
     * @param   magic  buffer containing the first few bytes of the stream
     * @return  <code>true</code> iff this looks like an XML file
     */
    public static boolean isMagic( byte[] magic ) {
        return getEncoding( magic ) != null;
    }

    /**
     * Returns what appears to be the encoding of the XML stream which
     * starts with a given magic number.  This is based on how we expect
     * an XML stream to start in terms of Unicode characters (one of the
     * strings {@link #MAGICS}).  The result will be one of the
     * encoding names listed in {@link #ENCODINGS}, or <code>null</code> if
     * it doesn't look like the start of an XML stream in any of these
     * encodings.
     *
     * @param   magic  buffer containing the first few bytes of the stream
     * @return  name of a supported encoding in which this looks like XML
     */
    public static String getEncoding( byte[] magic ) {
        for ( int i = 0; i < ENCODINGS.length; i++ ) {

            /* Decode the magic number into a Unicode string. */
            String encoding = ENCODINGS[ i ];
            String test;
            if ( Charset.isSupported( encoding ) ) {
                try {
                    test = new String( magic, encoding );
                }
                catch ( UnsupportedEncodingException e ) {
                    throw new AssertionError( "Encoding " + encoding
                                            + " not supported??" );
                }
            }
            else {  // bit surprising
                System.err.println( "Unsupported charset: " + encoding );
                break;
            }

            /* See if the decoded string looks like any of the possible starts
             * of an XML document. */
            for ( int j = 0; j < MAGICS.length; j++ ) {
                if ( test.startsWith( MAGICS[ j ] ) ) {

                    /* If it is HTML then take this to mean it's NOT XML -
                     * it is most likely to be not well-formed. */
                    if ( test.indexOf( "<HTML" ) > 0 ||
                         test.indexOf( "<html" ) > 0 ) {
                        return null;
                    }
                    else {
                        return encoding;
                    }
                }
            }
        }

        /* No matches, it's not XML then. */
        return null;
    }

}
