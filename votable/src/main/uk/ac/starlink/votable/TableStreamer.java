package uk.ac.starlink.votable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.Base64InputStream;
import uk.ac.starlink.util.PipeReaderThread;
import uk.ac.starlink.util.StarEntityResolver;
import uk.ac.starlink.util.URLUtils;

/**
 * Content handler which goes through a SAX stream and just extracts
 * the data from a single TABLE element, copying its metadata and data
 * to a given TableSink.
 * <p>
 * It builds a minimal DOM, consisting of the non-data bearing parts,
 * and ignoring any DATA element until it comes to the one in the table
 * it's after.  It then streams the data of this element to the sink
 * it's been given, and when it reaches the end of that DATA element,
 * it quits, discarding the partial DOM it has built.  The DOM that
 * gets constructed should therefore be pretty lean, hence very low
 * memory usage.
 * <p>
 * A previous version of this class skipped all elements until it
 * found the TABLE element it was looking for.  This is a bit too
 * drastic - other parts of the DOM may be required in case the
 * required TABLE references them using by ID using <tt>ref</tt> attributes.
 *
 * @author   Mark Taylor
 */
class TableStreamer extends CustomDOMBuilder {

    private TableSink sink;
    private int skipTables;
    private String systemId;
    private ContentHandler basicHandler;

    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.ac.votable" );

    /**
     * Constructs a new table streaming handler.
     *
     * @param   sink  the sink to which a table will be written when it's found
     * @param   itable  index of the TABLE element to write (0-based)
     * @param   strict  whether to enforce a strict reading of the VOTable
     *          standard
     */
    public TableStreamer( TableSink sink, int itable, boolean strict ) {
        super( strict );
        this.sink = sink;
        this.skipTables = itable;
        basicHandler = new BasicContentHandler();
        setCustomHandler( basicHandler );
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

        /* Install a custom entity resolver. */
        parser.setEntityResolver( StarEntityResolver.getInstance() );

        /* Install an error handler. */
        parser.setErrorHandler( new ErrorHandler() {
            public void error( SAXParseException e ) {
                logger.warning( e.toString() );
            }
            public void warning( SAXParseException e ) {
                logger.warning( e.toString() );
            }
            public void fatalError( SAXParseException e ) throws SAXException {
                throw e;
            }
        } );

        /* Install a content handler which can pull out data from one table
         * as required. */
        parser.setContentHandler( new TableStreamer( sink, itable, strict ) );

        /* Do the parse.  We expect to be signalled by the handler with a
         * SuccessfulCompletionException if the table gets copied.
         * Otherwise, it hasn't happened. */
        try {
            parser.parse( saxsrc );
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
     * ContentHandler callback methods invoke this method when they have
     * completed sending all the rows to the sink.
     */
    private void finished() throws SAXException {
        try {
            sink.endRows();
        }
        catch ( IOException e ) {
            throw new SAXParseException( e.getMessage(), getLocator(), e );
        }
        throw new SuccessfulCompletionException();
    }

    /**
     * Handler which is active initially, and when the DOM is to be built
     * selectively as usual.  It basically performs default DOM-building
     * actions except if it encounters a DATA element, whose content 
     * it arranges either to ignore or to stream from, according to 
     * whether it's from the requested table.
     */
    class BasicContentHandler extends DefaultContentHandler {

        public void startDocument() throws SAXException {
            super.startDocument();
            systemId = getLocator().getSystemId();
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
                throws SAXException {
            super.startElement( namespaceURI, localName, qName, atts );
            String tagName = getTagName( namespaceURI, localName, qName );

            /* DATA element. */
            if ( "DATA".equals( tagName ) ) {

                /* If it's not the table we're after, ignore it. */
                if ( skipTables != 0 ) {
                    setCustomHandler( new IgnoreContentHandler( tagName ) );
                }

                /* If it is the table we're after, try to set a custom content 
                 * handler which will stream its data. */
                else {
                    Node parent = getNewestNode().getParentNode();
                    if ( parent instanceof TableElement ) {
                        TableElement tableEl = (TableElement) parent;
                        setCustomHandler( new DataContentHandler( tableEl ) );
                    }
                    else {
                        logger.warning( "DATA element is not TABLE child" );
                        setCustomHandler( new IgnoreContentHandler( tagName ) );
                    }
                }
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            super.endElement( namespaceURI, localName, qName );
            String tagName = getTagName( namespaceURI, localName, qName );

            /* If it's a TABLE element, keep count of ones we've seen so far. */
            if ( "TABLE".equals( tagName ) ) {
                skipTables--;
            }
        }

        /* If we've reached the end of the document and this handler is 
         * still active, we never found the TABLE element. */
        public void endDocument() throws SAXException {
            throw new SAXException( "TABLE element not found in document" );
        }
    }

    /**
     * Handler which just ignores everything until the end of the current
     * element.
     */
    class IgnoreContentHandler extends NullContentHandler {
        int level;
        String tagName;
        IgnoreContentHandler( String tagName ) {
            this.tagName = tagName;
            level = 1;
        }
        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts ) {
            level++;
        }
        public void endElement( String namespaceURI, String localName,
                                String qName ) 
                throws SAXException {
            if ( --level == 0 ) {
                assert tagName
                      .equals( getTagName( namespaceURI, localName, qName ) );
                basicHandler.endElement( namespaceURI, localName, qName );
                setCustomHandler( basicHandler );
            }
        }
    }

    /**
     * Handler which writes the content of a DATA element to a sink.
     */
    class DataContentHandler extends DefaultContentHandler {

        FieldElement[] fields;
        Decoder[] decoders;
        String mode;
        String extnum;

        /**
         * Constructs a new handler.
         *
         * @param  tableEl  TABLE element within which this DATA element has
         *         been found
         */
        public DataContentHandler( TableElement tableEl ) 
                throws SAXException {

            /* Set up the fields and decoders for this table. */
            fields = tableEl.getFields();
            int ncol = fields.length;
            decoders = new Decoder[ ncol ];
            for ( int i = 0; i < ncol; i++ ) {
                decoders[ i ] = fields[ i ].getDecoder();
            }

            /* Remove the DATA element from the TABLE.  This will ensure that
             * the metadata StarTable acts like it's empty. */
            Node dataEl = getNewestNode();
            assert dataEl.getNodeName().equals( "DATA" );
            tableEl.removeChild( dataEl );

            /* Construct a metadata table and use it to prime the sink. */
            StarTable meta;
            try {

                /* If the table doesn't think it has any rows, it's just
                 * because it thinks it has no DATA (we have withdrawn it) -
                 * so in this case force it to report an unknown number
                 * instead. */
                meta = new VOStarTable( tableEl ) {
                    public long getRowCount() {
                        long nrow = super.getRowCount();
                        return nrow > 0L ? nrow : -1L;
                    }
                };
            }
            catch ( IOException e ) {
                throw new AssertionError();
            }
            try {
                sink.acceptMetadata( meta );
            }
            catch ( TableFormatException e ) {
                throw new SAXParseException( e.getMessage(), getLocator(), e );
            }
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
                throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );

            /* FITS element - remember we're expecting a FITS stream. */
            if ( "FITS".equals( tagName ) ) {
                mode = "FITS";
                extnum = getAttribute( atts, "extnum" );
            }

            /* BINARY element - remember we're expecting a BINARY stream. */
            else if ( "BINARY".equals( tagName ) ) {
                mode = "BINARY";
            }

            /* TABLEDATA element - install new handler to read TR/TD 
             * elements.*/
            else if ( "TABLEDATA".equals( tagName ) ) {
                setCustomHandler( new TabledataHandler( decoders ) );
            }

            /* STREAM element - get ready to stream rows. */
            else if ( "STREAM".equals( tagName ) ) {

                /* Check we are ready for FITS or BINARY streamed data. */
                if ( ! "FITS".equals( mode ) && ! "BINARY".equals( mode ) ) {
                    throw new SAXParseException( 
                        "STREAM is not BINARY or FITS!", getLocator() );
                }
                String href = getAttribute( atts, "href" );
                String encoding = getAttribute( atts, "encoding" );

                /* If the stream has externally referenced data, get a 
                 * corresponding RowStepper and copy rows to the sink. */
                if ( href != null ) {
                    try {
                        URL url = URLUtils.makeURL( systemId, href );
                        if ( mode.equals( "BINARY" ) ) {
                            RowStepper rstep =
                                new BinaryRowStepper( decoders,
                                                      url.openStream(), 
                                                      encoding );
                            Object[] row;
                            while ( ( row = rstep.nextRow() ) != null ) {
                                sink.acceptRow( row );
                            }
                        }
                        else if ( mode.equals( "FITS" ) ) {
                            new FitsTableBuilder()
                               .streamStarTable( url.openStream(), 
                                                 sink, extnum );
                        }
                        else {
                            assert false;
                        }
                    }
                    catch ( IOException e ) {
                        throw (SAXException)
                              new SAXParseException( e.getMessage(),
                                                     getLocator(), e )
                             .initCause( e );
                    }
                    finished();
                }

                /* Otherwise the data is inline and base64-encoded.  Delegate
                 * further SAX processing to a handler which will write
                 * base64 text down a stream, and set up threads at the
                 * other end of the stream which will copy the data to 
                 * the sink. */
                else {
                    try {
                        PipeReaderThread reader;
                        if ( mode.equals( "BINARY" ) ) {
                            reader = new PipeReaderThread() {
                                protected void doReading( InputStream datain )
                                        throws IOException {
                                    InputStream in = 
                                        new BufferedInputStream( datain );
                                    RowStepper rs = 
                                        new BinaryRowStepper( decoders, in,
                                                              "base64" );
                                    Object[] row;
                                    while ( ( row = rs.nextRow() ) != null ) {
                                        sink.acceptRow( row );
                                    }
                                }
                            };
                        }
                        else {
                            assert mode.equals( "FITS" );
                            reader = new PipeReaderThread() {
                                protected void doReading( InputStream datain )
                                        throws IOException {
                                    InputStream in = 
                                        new BufferedInputStream(
                                            new Base64InputStream( datain ) );
                                    new FitsTableBuilder()
                                       .streamStarTable( in, sink, extnum );
                                }
                            };
                        }
                        OutputStream b64out = reader.getOutputStream();
                        Writer out = new OutputStreamWriter( 
                                         new BufferedOutputStream( b64out ) );
                        setCustomHandler( new CDATASinkHandler( out, reader ) );
                    }
                    catch ( IOException e ) {
                        throw (SAXException)
                              new SAXParseException( "Trouble setting up data "
                                                   + "handler", getLocator(),
                                                     e )
                             .initCause( e );
                    }
                }
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "FITS".equals( tagName ) ) {
                extnum = null;
                mode = null;
            }
            else if ( "BINARY".equals( tagName ) ) {
                mode = null;
            }
            else if ( "DATA".equals( tagName ) ) {
                finished();
            }
        }
    }

    /**
     * Custom handler for TABLEDATA content.
     */
    class TabledataHandler extends NullContentHandler {
        final Decoder[] decoders;
        final int ncol;
        int icol;
        Object[] row;
        StringBuffer cell;
        boolean inCell;

        TabledataHandler( Decoder[] decoders ) {
            this.decoders = decoders;
            ncol = decoders.length;
            cell = new StringBuffer();
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts ) {
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "TD".equals( tagName ) ) {
                cell.setLength( 0 );
                inCell = true;
            }
            else if ( "TR".equals( tagName ) ) {
                row = new Object[ ncol ];
                icol = 0;
            }
        }

        public void characters( char[] ch, int start, int length ) {
            if ( inCell ) {
                cell.append( ch, start, length );
            }
        }

        public void ignorableWhitespace( char[] ch, int start, int length ) {
            if ( inCell ) {
                cell.append( ch, start, length );
            }
        }

        public void endElement( String namespaceURI, String localName, 
                                String qName ) throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( inCell && "TD".equals( tagName ) && icol < ncol ) {
                row[ icol ] = cell.length() > 0
                            ? decoders[ icol ].decodeString( cell.toString() )
                            : null;
                icol++;
                inCell = false;
            }
            else if ( "TR".equals( tagName ) ) {
                try {
                    sink.acceptRow( row );
                }
                catch ( IOException e ) {
                    throw (SAXException) 
                          new SAXParseException( e.getMessage(), getLocator(),
                                                 e ).initCause( e );
                }
            }
            if ( "TABLEDATA".equals( tagName ) ) {
                finished();
            }
        }
    }

    /**
     * Handler which writes character data to an output stream, which is
     * being consumed by a thread.
     */
    class CDATASinkHandler extends NullContentHandler {

        final Writer out;
        final PipeReaderThread reader;

        CDATASinkHandler( Writer out, PipeReaderThread reader ) {
            this.out = out;
            this.reader = reader;
            reader.start();
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts ) 
                throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );
            throw new SAXParseException( "Unwelcome child <" + tagName +
                                         "> of STREAM", getLocator() );
        }

        public void characters( char[] ch, int start, int length )
                throws SAXException {
            try {
                out.write( ch, start, length );
            }
            catch ( IOException e ) {
                // ignore a write exception - it will get picked up by
                // finishReading if it's important
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "STREAM".equals( tagName ) ) {
                try {
                    out.close();
                    reader.finishReading();
                }
                catch ( IOException e ) {
                    throw (SAXException)
                          new SAXParseException( "Error closing",
                                                 getLocator(), e )
                         .initCause( e );
                }
            }
            finished();
        }
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
