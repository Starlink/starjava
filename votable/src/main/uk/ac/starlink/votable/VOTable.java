package uk.ac.starlink.votable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Class representing the top-level VOTABLE element of a VOTable document.
 * As well as providing several overloaded constructors for creating a VOTable
 * from files etc, it can provide its own copy of the VOTable DTD,
 * which means that no network connection/local DTD copy is required.
 *
 * <p>The constructors which cause parsing of XML text or SAX events
 * take a <tt>validate</tt> parameter which determines whether 
 * the document is validated against the VOTable DTD.  If set true,
 * then at any validation error a SAXException will be thrown and
 * parsing will cease.  If false, then validation errors will be ignored.
 * For more fine control over validation behaviour (e.g. to log all 
 * validation errors) you can parse the document yourself and use
 * a constructor which works on a parsed document.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOTable extends VOElement {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Constructs a VOTable from an XML Source.
     *
     * @param  xsrc  the Source
     */
    public VOTable( Source xsrc ) {
        super( xsrc, "VOTABLE" );
    }

    /**
     * Constructs a VOTable from a DOM Document element.
     *
     * @param  doc  the DOM Document
     */
    public VOTable( Document doc ) {
        this( new DOMSource( doc.getDocumentElement() ) );
    }

    /**
     * Constructs a VOTable from a URI.
     *
     * @param  uri  the location of the content to be parsed
     * @param  validate  whether to do a validating parse
     * @throws IOException if there is an I/O error
     * @throws SAXException if there is an error in XML parsing, including
     *         a validation error in the case that <tt>validate</tt> is true
     */
    public VOTable( String uri, boolean validate ) 
            throws IOException, SAXException {
        this( new DOMSource( getParser( validate ).parse( uri ), uri ) );
    }

    /**
     * Constructs a VOTable from a URL.
     *
     * @param  url  the location of the content to be parsed
     * @param  validate  whether to do a validating parse
     * @throws IOException if there is an I/O error
     * @throws SAXException if there is an error in XML parsing, including
     *         a validation error in the case that <tt>validate</tt> is true
     */
    public VOTable( URL url, boolean validate )
            throws IOException, SAXException {
        this( url.toExternalForm(), validate );
    }

    /**
     * Constructs a VOTable from an InputStream.
     *
     * @param  strm  the input stream supplying the XML
     * @param  validate  whether to do a validating parse
     * @throws IOException if there is an I/O error
     * @throws SAXException if there is an error in XML parsing, including
     *         a validation error in the case that <tt>validate</tt> is true
     */
    public VOTable( InputStream strm, boolean validate ) 
            throws IOException, SAXException {
        this( new DOMSource( getParser( validate ).parse( strm ) ) );
    }

    /**
     * Constructs a VOTable from an InputStream and System ID.
     *
     * @param  strm  the input stream supplying the XML
     * @param  systemId  a base for resolving relative URIs
     * @param  validate  whether to do a validating parse
     * @throws IOException if there is an I/O error
     * @throws SAXException if there is an error in XML parsing, including
     *         a validation error in the case that <tt>validate</tt> is true
     */
    public VOTable( InputStream strm, String systemId, boolean validate )
            throws IOException, SAXException {
        this( new DOMSource( getParser( validate ).parse( strm, systemId ),
                             systemId ) );
    }

    /**
     * Constructs a VOTable from an InputSource object.
     *
     * @param  isrc  the input source
     * @param  validate  whether to do a validating parse
     * @throws IOException if there is an I/O error
     * @throws SAXException if there is an error in XML parsing, including
     *         a validation error in the case that <tt>validate</tt> is true
     */
    public VOTable( InputSource isrc, boolean validate )
            throws IOException, SAXException {
        this( new DOMSource( getParser( validate ).parse( isrc ),
                             isrc.getSystemId() ) );
    }

    /**
     * Constructs a VOTable from a file.
     *
     * @param  the file containing the XML document
     * @param  validate  whether to do a validating parse
     * @throws IOException if there is an I/O error
     * @throws SAXException if there is an error in XML parsing, including
     *         a validation error in the case that <tt>validate</tt> is true
     */
    public VOTable( File file, boolean validate ) 
            throws IOException, SAXException {
        this( new DOMSource( getParser( validate ).parse( file ),
                             file.toString() ) );
    }

    /**
     * Obtains a parser which can parse a VOTable document using a local
     * version of the VOTable DTD.
     *
     * @param  validate  whether the parser should be validating or not
     * @return  a document parser
     */
    private static DocumentBuilder getParser( final boolean validate ) {

        /* Get a DocumentBuilder. */
        DocumentBuilderFactory dbfact = DocumentBuilderFactory.newInstance();
        dbfact.setValidating( validate );
        DocumentBuilder parser;
        try {
            parser = dbfact.newDocumentBuilder();
        }
        catch ( ParserConfigurationException e ) {
            logger.warning( "Parser configuration failed first time: " + e );

            /* Failed for some reason - try it with nothing fancy then. */
            try {
                parser = DocumentBuilderFactory.newInstance()
                                               .newDocumentBuilder();
            }
            catch ( ParserConfigurationException e2 ) {
                throw new RuntimeException( e2 );  // shouldn't happen?
            }
        }
        parser.setEntityResolver( VOTableEntityResolver.getInstance() );

        /* Configure the error handler according to whether we are validating
         * or not. */
        parser.setErrorHandler( new ErrorHandler() {
            public void error( SAXParseException e ) 
                    throws SAXException {
                if ( validate ) {
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

}
