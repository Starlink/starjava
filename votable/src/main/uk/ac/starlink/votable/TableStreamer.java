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
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.PipeReaderThread;
import uk.ac.starlink.util.StarEntityResolver;
import uk.ac.starlink.util.URLUtils;

/**
 * Content handler which goes through a SAX stream and just extracts
 * the data from a single TABLE element, copying its metadata and data
 * to a given TableSink.
 * <p>
 * It builds a minimal DOM, consisting of not much more than the 
 * requested TABLE element, its FIELDs, and its parents.  
 * In particular, no DOM node corresponding to the DATA element of the
 * required TABLE is constructed, and neither are any nodes representing
 * non-requested TABLEs.  This should result in a very lean DOM, hence
 * very low memory usage.
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
     */
    public TableStreamer( TableSink sink, int itable ) {
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
     */
    public static void streamStarTable( InputSource saxsrc, TableSink sink, 
                                        int itable )
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
        parser.setContentHandler( new TableStreamer( sink, itable ) );

        /* Do the parse.  We expect to be signalled by the handler with a
         * SuccessfulCompletionException if the table gets copied.
         * Otherwise, it hasn't happened. */
        try {
            parser.parse( saxsrc );
        }
        catch ( SuccessfulCompletionException e ) {
            return;
        }
        throw new IOException( "No TABLE element found" );
    }

    /**
     * ContentHandler callback methods invoke this method when they have
     * completed sending all the rows to the sink.
     */
    private void finished() throws SuccessfulCompletionException {
        sink.endRows();
        throw new SuccessfulCompletionException();
    }

    /**
     * Handler which is active initially, and when the DOM is to be built
     * selectively as usual.
     */
    class BasicContentHandler extends DefaultContentHandler {

        /** These parts of the DOM will be built as normal. */
        private List normalNodes = Arrays.asList( new String[] {
            "VOTABLE", "RESOURCE", "PARAM", "INFO",
        } );

        public void startDocument() throws SAXException {
            super.startDocument();
            systemId = getLocator().getSystemId();
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts ) 
                throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );

            /* If it's the start of a legitimate parent of a TABLE, build
             * the DOM node as usual. */
            if ( normalNodes.contains( tagName ) ) {
                super.startElement( namespaceURI, localName, qName, atts );
            }

            /* If it's the start of the table we're after, hand over to the
             * table processing handler. */
            else if ( "TABLE".equals( tagName ) && 
                      skipTables-- == 0 ) {
                super.startElement( namespaceURI, localName, qName, atts );
                setCustomHandler( new TableContentHandler() );
            }

            /* Otherwise ignore everything until the end of this element. */
            else {
                setCustomHandler( new IgnoreContentHandler( tagName ) );
            }
        }

        public void endElement( String namespaceURI, String localName, 
                                String qName ) throws SAXException {
            super.endElement( namespaceURI, localName, qName );
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
                                String qName ) {
            if ( --level == 0 ) {
                assert tagName
                      .equals( getTagName( namespaceURI, localName, qName ) );
                setCustomHandler( basicHandler );
            }
        }
    }

    /**
     * Handler which writes the current TABLE element to a sink.
     * Should be installed just after a TABLE has been started.
     */
    class TableContentHandler extends DefaultContentHandler {

        Element tableEl = (Element) getNewestNode();
        List fieldList = new ArrayList();
        FieldElement[] fields;
        Decoder[] decoders;

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts ) 
                throws SAXException {

            /* If it's a DATA element, this signals the end of the FIELDS.
             * Acquire and submit the StarTable representing table metadata
             * and then invoke the superclass method to add the DOM element.
             * Need to do it in this order so the Table constructor sees
             * a TABLE element with no DATA. */
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "DATA".equals( tagName ) ) {
                int ncol = fieldList.size();
                fields = new FieldElement[ ncol ];
                decoders = new Decoder[ ncol ];
                for ( int icol = 0; icol < ncol; icol++ ) {
                    fields[ icol ] = 
                        new FieldElement( (Element) fieldList.get( icol ),
                                          systemId );
                    decoders[ icol ] = fields[ icol ].getDecoder();
                }
                StarTable startable = 
                    new VOStarTable( new TableElement( tableEl, systemId ) ) {
                        public long getRowCount() {
                            return -1L;
                        }
                    };
                sink.acceptMetadata( startable );
                super.startElement( namespaceURI, localName, qName, atts );
                return;
            }

            /* Otherwise, call the superclass method to add this node to 
             * the DOM, and then do node-specific processing. */
            super.startElement( namespaceURI, localName, qName, atts );
            Element el = (Element) getNewestNode();
            if ( "FIELD".equals( tagName ) ) {
                fieldList.add( el );
            }
            else if ( "TABLEDATA".equals( tagName ) ) {
                setCustomHandler( new TabledataHandler( decoders ) );
            }
            else if ( "STREAM".equals( tagName ) ) {
                Element parent = (Element) el.getParentNode();
                String parentName = parent.getTagName();
                if ( ! parentName.equals( "BINARY" ) && 
                     ! parentName.equals( "FITS" ) ) {
                    throw new SAXParseException( "STREAM has unknown parent " +
                                                 parentName, getLocator() );
                }
                String href = getAttribute( atts, "href" );
                String encoding = getAttribute( atts, "encoding" );
                final String extnum = parentName.equals( "FITS" )
                             && parent.hasAttribute( "extnum" )
                                    ? parent.getAttribute( "extnum" )
                                    : null;

                /* If the stream has externally referenced data, get a 
                 * corresponding RowStepper and copy rows to the sink. */
                if ( href != null ) {
                    try {
                        URL url = URLUtils.makeURL( systemId, href );
                        if ( parentName.equals( "BINARY" ) ) {
                            RowStepper rstep =
                                new BinaryRowStepper( decoders,
                                                      url.openStream(), 
                                                      encoding );
                            Object[] row;
                            while ( ( row = rstep.nextRow() ) != null ) {
                                sink.acceptRow( row );
                            }
                        }
                        else {
                            assert parentName.equals( "FITS" );
                            new FitsTableBuilder()
                               .streamStarTable( url.openStream(), 
                                                 sink, extnum );
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
                        if ( parentName.equals( "BINARY" ) ) {
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
                            assert parentName.equals( "FITS" );
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
            super.endElement( namespaceURI, localName, qName );
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "TABLE".equals( tagName ) ) {
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
