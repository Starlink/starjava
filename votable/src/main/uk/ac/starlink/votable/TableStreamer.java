package uk.ac.starlink.votable;

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
import uk.ac.starlink.util.StarEntityResolver;

/**
 * ContentHandler which goes through a SAX stream and just extracts
 * the data from a single TABLE element, copying its metadata and data
 * to a given TableSink.
 *
 * <p>A previous version of this class skipped all elements until it
 * found the TABLE element it was looking for.  This is a bit too
 * drastic - other parts of the DOM may be required in case the
 * required TABLE references them using by ID using <code>ref</code> attributes.
 * So it was changed to pull out everything until the end of the TABLE
 * element of interest.
 *
 * <p>That still won't get metadata that comes from SAX events after
 * the end of the TABLE element.  But given the TableSink interface,
 * in which the metadata is submitted before any of the row data,
 * there's nothing that can be done about that.
 *
 * @author   Mark Taylor
 */
class TableStreamer extends TableContentHandler implements TableHandler {

    private int skipTables_;
    private final TableSink sink_;
    private final Namespacing namespacing_;
    private boolean isVotable_; 
    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Constructor.
     *
     * @param   sink  table destination
     * @param   itable   index of table to be streamed - 0 means the first
     *          TABLE element encountered, 1 means the second etc
     * @param  strict whether to enforce strict reading of the VOTable standard
     */
    public TableStreamer( TableSink sink, int itable, boolean strict ) {
        super( strict );
        setTableHandler( null );
        setReadHrefTables( false );
        sink_ = sink;
        skipTables_ = itable;
        namespacing_ = Namespacing.getInstance();
    }

    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts ) 
            throws SAXException {
        super.startElement( namespaceURI, localName, qName, atts );
        String tagName =
            namespacing_.getVOTagName( namespaceURI, localName, qName );
        if ( "VOTABLE".equals( tagName ) ) {
            isVotable_ = true;
        }
        if ( "TABLE".equals( tagName ) ) {
            if ( skipTables_-- == 0 ) {
                setReadHrefTables( true );
                setTableHandler( this );
            }
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
        }
        catch ( IOException e ) {
            throw (SAXException)
                  new SAXParseException( e.getMessage(), getLocator(), e )
                 .initCause( e );
        }
        throw new SuccessfulCompletionException();
    }

    /**
     * Acquires the data from a single TABLE element in a VOTable document,
     * writing the result to a sink.  The rest of the SAX stream is ignored.
     *
     * @param  istrm  stream from which the VOTable document will be supplied
     * @param  sink   callback interface into which the table metadata and
     *                data will be dumped
     * @param  itable index of the table in the document to be read
     *                (0-based)
     * @param  strict whether to enforce strict reading of the VOTable standard
     */
    public static void streamStarTable( InputSource saxsrc, TableSink sink,
                                        int itable, boolean strict )
            throws IOException, SAXException {

        /* Construct a table handler which can pull out data from one
         * table as required. */
        TableStreamer streamer = new TableStreamer( sink, itable, strict );

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

        /* Do the parse.  We expect to be signalled by the handler with a
         * SuccessfulCompletionException if the table gets copied.
         * Otherwise, it hasn't happened. */
        try {
            parser.parse( saxsrc );
        }
        catch ( SuccessfulCompletionException e ) {
            return;
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
            e = VOElementFactory.fixStackTrace( e );
            if ( streamer.isVotable_ ) {
                throw e;
            }
            else {
                throw new TableFormatException( e );
            }
        }
        throw streamer.isVotable_
            ? new IOException( "No TABLE element found" )
            : new TableFormatException( "No VOTABLE element" );
    }

    /**
     * This private exception signals that the TABLE element has been
     * identified and extracted.
     */
    private static class SuccessfulCompletionException extends SAXException {
        SuccessfulCompletionException() {
            super( "Table extraction complete" );
        }
    }
}
