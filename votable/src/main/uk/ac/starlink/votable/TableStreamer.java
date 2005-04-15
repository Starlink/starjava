package uk.ac.starlink.votable;

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
import uk.ac.starlink.table.TableSink;

class TableStreamer extends TableXMLReader implements TableHandler {

    private int skipTables_;
    private final TableSink sink_;
    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.votable" );

    public TableStreamer( XMLReader parent, TableSink sink, int itable,
                          boolean strict ) {
        super( parent, strict );
        setTableHandler( null );
        setReadHrefTables( false );
        sink_ = sink;
        skipTables_ = itable;
    }

    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts ) 
            throws SAXException {
        super.startElement( namespaceURI, localName, qName, atts );
        if ( "TABLE".equals( localName ) || 
             "TABLE".equals( qName ) ||
             qName != null && qName.endsWith( ":TABLE" ) ) {
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
                  new SAXParseException( e.getMessage(), getLocator() )
                 .initCause( e );
        }
    }

    public void rowData( Object[] row ) throws SAXException {
        try {
            sink_.acceptRow( row );
        }
        catch ( IOException e ) {
            throw (SAXException)
                  new SAXParseException( e.getMessage(), getLocator() )
                 .initCause( e );
        }
    }

    public void endTable() throws SAXException {
        try {
            sink_.endRows();
        }
        catch ( IOException e ) {
            throw (SAXException)
                  new SAXParseException( e.getMessage(), getLocator() )
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

        /* Get a SAX parser. */
        XMLReader parser;
        try {
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setValidating( false );
            parser = spfact.newSAXParser().getXMLReader();
        }
        catch ( ParserConfigurationException e ) {
            throw (SAXException) new SAXException( e.getMessage() )
                                .initCause( e );
        }

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

        /* Filter the SAX parse so that it pulls out data from one table
         * as required. */
        TableStreamer tableParser = 
            new TableStreamer( parser, sink, itable, strict );

        /* Do the parse.  We expect to be signalled by the handler with a
         * SuccessfulCompletionException if the table gets copied.
         * Otherwise, it hasn't happened. */
        try {
            tableParser.parse( saxsrc );
        }
        catch ( SuccessfulCompletionException e ) {
            return;
        }
        catch ( SAXException e ) {
            throw VOElementFactory.fixStackTrace( e );
        }
        throw new IOException( "No TABLE element found" );
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
