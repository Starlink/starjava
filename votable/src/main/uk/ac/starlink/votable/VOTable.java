package uk.ac.starlink.votable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import uk.ac.starlink.util.DataSource;

/**
 * Class representing the top-level VOTABLE element of a VOTable document.
 * As well as providing several overloaded constructors for creating a VOTable
 * from files etc, it can provide its own copy of the VOTable DTD,
 * which means that no network connection/local DTD copy is required.
 *
 * <p>The constructors which cause parsing of XML text or SAX events
 * take a <tt>validate</tt> parameter which determines whether 
 * the document is validated against the VOTable DTD.  If set true,
 * then at any validation error a TransformerException will be thrown and
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
     * @throws  TransformerException  if <tt>xsrc</tt> is not a 
     *          <tt>DOMSource</tt> and there is an error transforming 
     *          it to a DOM
     * 
     */
    public VOTable( Source xsrc ) throws TransformerException {
        super( xsrc, "VOTABLE" );
    }

    /**
     * Constructs a VOTable from a DOM source.
     *
     * @param  dsrc  the Source
     */
    public VOTable( DOMSource dsrc ) {
        super( dsrc, "VOTABLE" );
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
     * @throws TransformerException if there is an error in XML parsing, 
     *         including a validation error in the case that 
     *         <tt>validate</tt> is true
     */
    public VOTable( String uri, boolean validate ) 
            throws TransformerException {
        this( new SAXSource( makeParser( validate ), new InputSource( uri ) ) );
    }

    /**
     * Constructs a VOTable from a URL.
     *
     * @param  url  the location of the content to be parsed
     * @param  validate  whether to do a validating parse
     * @throws TransformerException if there is an error in XML parsing, 
     *         including a validation error in the case that 
     *         <tt>validate</tt> is true
     */
    public VOTable( URL url, boolean validate )
            throws TransformerException {
        this( url.toExternalForm(), validate );
    }

    /**
     * Constructs a VOTable from an InputStream.
     *
     * @param  strm  the input stream supplying the XML
     * @param  validate  whether to do a validating parse
     * @throws TransformerException if there is an error in XML parsing, 
     *         including a validation error in the case that 
     *         <tt>validate</tt> is true
     */
    public VOTable( InputStream strm, boolean validate ) 
            throws TransformerException {
        this( new SAXSource( makeParser( validate ),
                             new InputSource( strm ) ) );
    }

    /**
     * Constructs a VOTable from an InputStream and System ID.
     *
     * @param  strm  the input stream supplying the XML
     * @param  systemId  a base for resolving relative URIs
     * @param  validate  whether to do a validating parse
     * @throws TransformerException if there is an error in XML parsing, 
     *         including a validation error in the case that 
     *         <tt>validate</tt> is true
     */
    public VOTable( InputStream strm, String systemId, boolean validate )
            throws TransformerException {
        this( new SAXSource( makeParser( validate ),
                             identifySource( new InputSource( strm ),
                                             systemId ) ) );
    }

    /**
     * Constructs a VOTable from a file.
     *
     * @param  the file containing the XML document
     * @param  validate  whether to do a validating parse
     * @throws TransformerException if there is an error in XML parsing, 
     *         including a validation error in the case that 
     *         <tt>validate</tt> is true
     */
    public VOTable( File file, boolean validate ) 
            throws TransformerException {
        this( file.toURI().toString(), validate );
    }

    /**
     * Constructs a VOTable from a DataSource.
     *
     * @param  the datasource pointing to the XML document
     * @param  validate  whether to do a validating parse
     * @throws TransformerException if there is an error in XML parsing, 
     *         including a validation error in the case that 
     *         <tt>validate</tt> is true
     */
    public VOTable( DataSource datsrc, boolean validate )
            throws TransformerException {
        this( getInputStream( datsrc ), datsrc.getSystemId(), validate );
    }

    /**
     * Convenience method to decorate an InputSource with a SystemID and
     * return the same object.  Used in constructors.
     *
     * @param  source  XML input source
     * @param  systemId  system ID string
     * @return  <tt>source</tt> with a new system ID
     */
    private static InputSource identifySource( InputSource source, 
                                               String systemId ) {
        source.setSystemId( systemId );
        return source;
    }

    /**
     * Gets a suitable stream from a data source to use for parsing.
     * This method does two things: first it converts IOExceptions to
     * TransformerExceptions for convenience, and second it gets an
     * input stream in a sensible way - for performance reasons this
     * is currently implemented using DataSource's hybrid stream.
     *
     * @param  datsrc  data source
     * @return  stream suitable for parsing
     */
    private static InputStream getInputStream( DataSource datsrc )
            throws TransformerException {
        try {
            return datsrc.getHybridInputStream();
        }
        catch ( IOException e ) {
            throw (TransformerException) 
                  new TransformerException( e.getMessage() )
                 .initCause( e );
        }
    }
}
