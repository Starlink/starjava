package uk.ac.starlink.votable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.StarEntityResolver;

/**
 * Provides methods for constructing VOElements from a variety
 * of sources.  A VOElement can be made either from an existing 
 * DOM {@link org.w3c.dom.Element} or from some non-DOM source such
 * as a file, input stream, or SAX stream.  
 * In the latter case a DOM is built using the 
 * {@link #transformToDOM(javax.xml.transform.Source,boolean)}
 * method with no validation.  There are several optimisations performed
 * by this method which distinguish it from a DOM that you'd get 
 * if you constructed it directly; the most important ones are that
 * the data-bearing parts (children of STREAM or TABLEDATA elements) 
 * of the XML document are not included in the built DOM, and that
 * any reference to the VOTable DTD is resolved locally rather than
 * making a potential network connection.  You almost certainly don't
 * need to worry about this; however if for some reason you want to
 * work on a 'normal' DOM, or if you want validation, you can construct
 * the DOM yourself and invoke one of the non-transforming 
 * <tt>makeVOElement</tt> methods on the result.
 * <p>
 * The various <tt>makeVOElement</tt> methods may return an object of class
 * <tt>VOElement</tt> or of one of its subclasses, according to the
 * name of the element in question; specific subclasses are provided only
 * where some extra functionality is available, for instance the
 * {@link TableElement#getData} method of the <tt>TableElement</tt> class.
 * One upshot of this is that a tree of VOElements need not conform to
 * the VOTable DTD, elements of any name may be in it.  Wherever an
 * element has a name which matches an element with specific significance
 * in a VOTable document however, such as "TABLE", it is handled 
 * accordingly.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOElementFactory {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );
    private StoragePolicy storagePolicy;

    /**
     * Constructs a new VOElementFactory with a given storage policy.
     * The StoragePolicy object is used to determine how row data which 
     * is found within the DOM will be cached.
     *
     * @param  policy  storage policy
     */
    public VOElementFactory( StoragePolicy policy ) {
        setStoragePolicy( policy );
    }

    /**
     * Constructs a new VOElementFactory with the default storage policy.
     *
     * @see   uk.ac.starlink.table.StoragePolicy#getDefaultPolicy
     */
    public VOElementFactory() {
        this( StoragePolicy.getDefaultPolicy() );
    }

    /**
     * Returns the storage policy currently in effect.
     * This is used to determine how row data found in the DOM will be 
     * stored.
     *
     * @return  current storage policy
     */
    public StoragePolicy getStoragePolicy() {
        return storagePolicy;
    }

    /**
     * Sets the storage policy.
     * This determines how row data found in the DOM will be stored.
     *
     * @param  policy  new storage policy
     */
    public void setStoragePolicy( StoragePolicy policy ) {
        this.storagePolicy = policy;
    }

    /**
     * Returns a new VOElement based on a given DOM element.
     * The systemId is also required since some elements (STREAM, LINK)
     * may need it for URL resolution.  It may be null however
     * (which is fine if there are no relative URLs used in the document).
     *
     * @param  el  DOM element on which the new object will be based
     * @param  systemId  the location of the document
     */
    public VOElement makeVOElement( Element el, String systemId ) {

        /* Get the tag name. */
        String name = el.getTagName();

        /* And build an appropriate element from the (possibly transformed)
         * source. */
        if ( name.equals( "FIELD" ) ) {
            return new FieldElement( el, systemId, this );
        }
        else if ( name.equals( "PARAM" ) ) {
            return new ParamElement( el, systemId, this );
        }
        else if ( name.equals( "LINK" ) ) {
            return new LinkElement( el, systemId, this );
        }
        else if ( name.equals( "VALUES" ) ) {
            return new ValuesElement( el, systemId, this );
        }
        else if ( name.equals( "TABLE" ) ) {
            return new TableElement( el, systemId, this );
        }
        else {
            return new VOElement( el, systemId, this );
        }
    }

    /**
     * Returns a new VOElement based on a DOM source.
     *
     * @param   dsrc   DOM source representing an Element or Document node
     * @return  VOElement based on <tt>dsrc</tt>
     */
    public VOElement makeVOElement( DOMSource dsrc ) {
        Node node = dsrc.getNode();
        Element el;
        if ( node instanceof Element ) {
            el = (Element) node;
        }
        else if ( node instanceof Document ) {
            el = ((Document) node).getDocumentElement();
        }
        else {
            throw new IllegalArgumentException( "Unsuitable DOM node " + node +
                                                "(not Element or Document)" );
        }
        return makeVOElement( el, dsrc.getSystemId() );
    }

    /**
     * Returns a new VOElement based on an XML Source.
     * If <tt>xsrc</tt> is not a DOMSource, it will be transformed to a DOM
     * using <tt>transformToDOM</tt> first.
     *
     * @param   xsrc  the XML source representing the element
     * @return  VOElement based on <tt>xsrc</tt>
     * @throws  SAXException  if <tt>xsrc</tt> is not a DOM source
     *          and there is a parse error transforming it to a DOM
     * @throws  IOException  if <tt>xsrc</tt> is not a DOM source
     *          and there is an I/O error transforming to a DOM
     */
    public VOElement makeVOElement( Source xsrc )
            throws SAXException, IOException {
        return makeVOElement( transformToDOM( xsrc, false ) );
    }

    /**
     * Returns a new VOElement based on a DOM Document node.
     * The systemId is also required since some elements (STREAM, LINK)
     * may need it for URL resolution.  It may be null however
     * (which is fine if there are no relative URLs used in the document).
     *
     * @param  doc  DOM document node
     * @param  systemId  the location of the document
     * @return  VOElement based on <tt>doc</tt>
     */
    public VOElement makeVOElement( Document doc, String systemId ) {
        return makeVOElement( doc.getDocumentElement(), systemId );
    }

    /**
     * Builds a custom DOM from an input stream and returns a new VOElement 
     * based on its top-level element.
     * The systemId is also required since some elements (STREAM, LINK)
     * may need it for URL resolution.  It may be null however
     * (which is fine if there are no relative URLs used in the document).
     *
     * @param strm  stream containing XML data
     * @param  systemId  the location of the document
     * @return  new VOElement
     */
    public VOElement makeVOElement( InputStream strm, String systemId )
            throws SAXException, IOException {
        InputSource insrc = new InputSource( strm );
        insrc.setSystemId( systemId );
        Source saxsrc = new SAXSource( insrc );
        saxsrc.setSystemId( systemId );
        return makeVOElement( saxsrc );
    }

    /**
     * Builds a custom DOM read from a URI and returns a new VOElement
     * based on its top-level element.
     *
     * @param  uri  location of the document
     * @return  new VOElement
     */
    public VOElement makeVOElement( String uri ) 
            throws SAXException, IOException {
        Source saxsrc = new SAXSource( new InputSource( uri ) );
        saxsrc.setSystemId( uri );
        return makeVOElement( saxsrc );
    }
 
    /**
     * Builds a custom DOM read from a URL and returns a new VOElement
     * based on its top-level element.
     * 
     * @param  url  location of the document
     * @return  new VOElement
     */
    public VOElement makeVOElement( URL url )
            throws SAXException, IOException {
        return makeVOElement( url.toExternalForm() );
    }

    /**
     * Builds a custom DOM read from a file and returns a new VOElement
     * based on its top-level element.
     *
     * @param  file  file containing XML document
     * @return  new VOElement
     */
    public VOElement makeVOElement( File file )
            throws SAXException, IOException {
        return makeVOElement( file.toURI().toString() );
    }

    /**
     * Builds a custom DOM read from a DataSource and returns a new VOElement
     * based on its top-level element.
     *
     * @param  datsrc  data source containing XML
     * @return  new VOElement
     */
    public VOElement makeVOElement( DataSource datsrc )
            throws SAXException, IOException {
        return makeVOElement( datsrc.getHybridInputStream(), 
                              datsrc.getSystemId() );
    }

    /**
     * Gets a DOMSource from a generic XML Source.  If the source is already
     * a DOMSource, there's nothing to do.  If it represents a stream
     * however, it parses it to produce a DOM, and wraps that up as a Source.
     * The clever bit is that it intercepts SAX events indicating the
     * start and end of any DATA elements it finds so that they are
     * not incorporated as part of the DOM.  Such elements it parses
     * directly on the basis of what it knows about items that crop up
     * in VOTables.  This keeps the resulting DOM to a reasonable size.
     *
     * @param   xsrc  input XML source
     * @param   validate  whether to use a validating parser if the 
     *          transformation needs to be done (that is, if <tt>xsrc</tt> 
     *          is not already a DOMSource)
     * @return  a DOMSource representing the XML document held by <tt>xsrc</tt>
     */
    public DOMSource transformToDOM( Source xsrc, boolean validate )
            throws SAXException, IOException {

        /* If it's a DOM source already, no problem. */
        if ( xsrc instanceof DOMSource ) {
            return (DOMSource) xsrc;
        }

        /* Otherwise we're going to need to do a custom parse of it. */
        String systemId = xsrc.getSystemId();
        InputSource insource;
        XMLReader parser = null;

        /* If it's a SAX source, mine it for its input source and parsing
         * engine. */
        if ( xsrc instanceof SAXSource ) {
            SAXSource saxsrc = (SAXSource) xsrc;
            insource = saxsrc.getInputSource();
            insource.setSystemId( systemId );
            parser = saxsrc.getXMLReader();
        }

        /* If it's a StreamSource, turn it into an input source and create
         * a default parsing engine. */
        else if ( xsrc instanceof StreamSource ) {
            StreamSource strmsrc = (StreamSource) xsrc;
            if ( strmsrc.getInputStream() != null ) {
                insource = new InputSource( strmsrc.getInputStream() );
                insource.setSystemId( systemId );
            }
            else if ( strmsrc.getReader() != null ) {
                insource = new InputSource( strmsrc.getReader() );
                insource.setSystemId( systemId );
            }
            else {
                insource = new InputSource( strmsrc.getSystemId() );
            }
        }

        /* I don't know of any other kinds of source, but if there is
         * one we'll have to transform it to DOM using brute force. */
        else {
            try {
                Node node = new SourceReader().getDOM( xsrc );
                return new DOMSource( node, systemId );
            }
            catch ( TransformerException e ) {
                throw (SAXException) new SAXException( e.getMessage() )
                                    .initCause( e );
            }
        }

        /* If we don't already have a parser, create one. */
        if ( parser == null ) {
            parser = makeParser( validate );
        }

        /* Operate on the input source with the parser. */
        Document node = parseToDOM( parser, insource );

        /* Return a DOM source based on this with the same System ID as the
         * original source. */
        return new DOMSource( node, systemId );
    }

    /**
     * Constructs a new default SAX parser suitable for reading VOTables.
     * You can choose whether you'd like a validating one.
     *
     * @param   validating  whether the returned parser ought to be validating
     * @return  new SAX parser
     */
    private static XMLReader makeParser( final boolean validating )
            throws SAXException {

        /* Get a SAX parser. */
        SAXParserFactory spfact = SAXParserFactory.newInstance();
        spfact.setValidating( validating );
        SAXParser sparser;
        try {
            sparser = spfact.newSAXParser();
        }
        catch ( ParserConfigurationException e ) {
            logger.config( "Parser configuration failed first time: " + e );

            /* Failed for some reason - try it with nothing fancy then. */
            try {
                sparser = SAXParserFactory.newInstance().newSAXParser();
            }
            catch ( ParserConfigurationException e2 ) {
                throw new SAXException( e2 );  // shouldn't happen?
            }
        }
        XMLReader parser = sparser.getXMLReader();

        /* Install a custom entity resolver. */
        parser.setEntityResolver(
                   new StarEntityResolver( parser.getEntityResolver() ) );

        /* Configure the error handler according to whether we are
         * validating or not. */
        parser.setErrorHandler( new ErrorHandler() {
            public void error( SAXParseException e ) throws SAXException {
                if ( validating ) {
                    throw e;
                }
            }
            public void fatalError( SAXParseException e )
                     throws SAXException {
                throw e;
            }
            public void warning( SAXParseException e )
                     throws SAXException {
                // no action
            }
        } );

        /* Return the parser. */
        return parser;
    }

    /**
     * Does a custom parse of an XML input source based on a given parser.
     * The parser's content handler is replaced with one which will
     * build a DOM, the parsing is initiated, and the resulting DOM
     * is returned.  A custom content handler is used which does a
     * selective parse, declining to install the bulk data (contents
     * of VOTable DATA elements) into the DOM itself.  The data
     * contained therein is instead parsed directly and stashed away
     * using attributes of the constructed DOM for later use.
     *
     * @param  parser  base parser - used to define entity resolution,
     *         error handling etc
     * @param  insource  input source containing the stream of XML
     */
    private Document parseToDOM( XMLReader parser, InputSource insource )
            throws IOException, SAXException {

        /* Parse using a custom handler. */
        VOTableDOMBuilder db = new VOTableDOMBuilder( getStoragePolicy() );
        parser.setContentHandler( db );
        try {
            parser.parse( insource );
        }
        catch ( SAXException e ) {
            throw fixStackTrace( e );
        }

        /* Return the built document. */
        return db.getDocument();
    }

    /**
     * Fixes up the stack trace for a SAXException.  SAXException's error
     * embedding mechanism predates the Java one, which means that although
     * the SAXException contains the cause of the error, it's not inserted
     * into the stack trace by the 1.4 JVM.  This method modifies a
     * SAXException so that it is.
     *
     * @param  e  exception
     * @return  tweaked <tt>e</tt>
     */
    private static SAXException fixStackTrace( SAXException e ) {
        if ( e.getException() != null && e.getCause() == null ) {
            e.initCause( e.getException() );
        }
        return e;
    }

}
