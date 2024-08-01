package uk.ac.starlink.vo;

import java.io.CharConversionException;
import java.io.IOException;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.votable.Namespacing;
import uk.ac.starlink.votable.TableContentHandler;
import uk.ac.starlink.votable.TableHandler;
import uk.ac.starlink.util.StarEntityResolver;

/**
 * SAX table handler for processing VOTable documents returned from DAL
 * services.  The result table has to be in a type="results" RESOURCE
 * following DAL rules.  The QUERY_STATUS INFO elements are understood,
 * and a table marked with error status causes an exception to be thrown
 * by the SAX parser.
 * A successful parse streams the table data to a supplied sink.
 *
 * @author   Mark Taylor
 * @since    10 Apr 2013
 */
public class DalResultStreamer extends TableContentHandler
                               implements TableHandler {

    private final TableSink sink_;
    private final Namespacing namespacing_;
    private boolean resultsStarted_;
    private boolean gotTable_;
    private boolean isVotable_;
    private boolean isResults_;
    private boolean isStatus_;
    private String statusValue_;
    private StringBuffer statusContent_;
    private boolean overflow_;

    private static final boolean STRICT = false;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param  sink  table destination
     */
    @SuppressWarnings("this-escape")
    public DalResultStreamer( TableSink sink ) {
        super( STRICT );
        setTableHandler( null );
        setReadHrefTables( false );
        sink_ = sink;
        namespacing_ = Namespacing.getInstance();
    }

    /**
     * Indicates whether the DAL result was marked as overflowing
     * (with QUERY_STATUS INFO).  Will be set or not set after a
     * successful parse.
     *
     * @return  true iff an overflow marker has been encountered
     */
    public boolean getOverflow() {
        return overflow_;
    }

    @Override
    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts )
            throws SAXException {
        super.startElement( namespaceURI, localName, qName, atts );
        String tagName =
            namespacing_.getVOTagName( namespaceURI, localName, qName );
        if ( "VOTABLE".equals( tagName ) ) {
            isVotable_ = true;
        }
        if ( "RESOURCE".equals( tagName ) &&
             "results".equals( atts.getValue( "type" ) ) ) {
            if ( isResults_ || resultsStarted_ ) {
                String msg = "Multiple RESOURCE/@type='results' elements";
                throw new SAXParseException( msg, getLocator() );
            }
            resultsStarted_ = true;
            isResults_ = true;
            setReadHrefTables( true );
            setTableHandler( this );
        }
        else if ( "INFO".equals( tagName ) && isResults_ &&
                  "QUERY_STATUS".equals( atts.getValue( "name" ) ) ) {
            if ( isStatus_ ) {
                String msg = "Nested INFO/@name='QUERY_STATUS' elements";
                throw new SAXParseException( msg, getLocator() );
            }
            isStatus_ = true;
            statusValue_ = atts.getValue( "value" );
            statusContent_ = new StringBuffer();
        }
    }

    @Override
    public void endElement( String namespaceURI, String localName,
                            String qName ) throws SAXException {
        super.endElement( namespaceURI, localName, qName );
        String tagName =
            namespacing_.getVOTagName( namespaceURI, localName, qName );
        if ( "RESOURCE".equals( tagName ) && isResults_ ) {
            isResults_ = false;
            setReadHrefTables( false );
            setTableHandler( null );
        }
        else if ( "INFO".equals( tagName ) && isStatus_ ) {
            isStatus_ = false;
            if ( "ERROR".equals( statusValue_ ) ) {
                String msg = "Service Error: \""
                           + statusContent_.toString().trim() + "\"";
                throw new SAXParseException( msg, getLocator() );
            }
            else if ( "OK".equals( statusValue_ ) ) {
                // good
            }
            else if ( "OVERFLOW".equals( statusValue_ ) ) {
                overflow_ = true;
            }
        }
    }

    @Override
    public void characters( char[] ch, int start, int length )
            throws SAXException {
        super.characters( ch, start, length );
        if ( isStatus_ ) {
            statusContent_.append( ch, start, length );
        }
    }

    public void startTable( StarTable meta ) throws SAXException {
        try {
            sink_.acceptMetadata( meta );
        }
        catch ( IOException e ) {
            throw (SAXException)
                  new SAXParseException( e.getMessage(), getLocator(), e )
                 .initCause( e );
        }
    }

    public void rowData( Object[] row ) throws SAXException {
        try {
            sink_.acceptRow( row );
        }
        catch ( IOException e ) {
            throw (SAXException)
                  new SAXParseException( e.getMessage(), getLocator(), e )
                 .initCause( e );
        }
    }

    public void endTable() throws SAXException {
        try {
            sink_.endRows();
            gotTable_ = true;
        }
        catch ( IOException e ) {
            throw new SAXParseException( e.getMessage(), getLocator(), e );
        }
    }

    /**
     * Streams a DAL result table from a SAX source to a table sink.
     *
     * @param  saxsrc  SAX event source
     * @param  sink  table destination
     * @return   true iff the result was marked as overflowed
     */
    public static boolean streamResultTable( InputSource saxsrc,
                                             TableSink sink )
            throws IOException, SAXException {

        /* Construct a table handler which can pull out data from the
         * results table. */
        DalResultStreamer streamer = new DalResultStreamer( sink );

        /* Get a SAX parser. */
        XMLReader parser;
        try {
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setValidating( false );
            streamer.namespacing_.configureSAXParserFactory( spfact );
            parser = spfact.newSAXParser().getXMLReader();
        }
        catch ( ParserConfigurationException e ) {
            throw (SAXException) new SAXException( e.getMessage(), e )
                                .initCause( e );
        }

        /* Install the content handler. */
        parser.setContentHandler( streamer );

        /* Install a custom entity resolver. */
        parser.setEntityResolver( StarEntityResolver.getInstance() );

        /* Install an error handler. */
        parser.setErrorHandler( new ErrorHandler() {
            public void error( SAXParseException e ) {
                logger_.warning( e.toString() );
            }
            public void warning( SAXParseException e ) {
                logger_.warning( e.toString() );
            }
            public void fatalError( SAXParseException e ) throws SAXException {
                throw e;
            }
        } );

        /* Do the parse, handling errors appropriately. */
        try {
            parser.parse( saxsrc );
        }
        catch ( CharConversionException e ) {
            if ( streamer.isVotable_ ) {
                throw e;
            }
            else {
                throw new TableFormatException( "Bad XML characters", e );
            }
        }
        catch ( SAXException e ) {
            if ( streamer.isVotable_ ) {
                throw e;
            }
            else {
                throw new TableFormatException( e );
            }
        }

        /* Assess final status and generate messages appropriately. */
        if ( ! streamer.isVotable_ ) {
            throw new IOException( "No VOTABLE element found" );
        }
        else if ( ! streamer.gotTable_ ) {
            throw new IOException( "No TABLE element found" );
        }
        return streamer.overflow_;
    }
}
