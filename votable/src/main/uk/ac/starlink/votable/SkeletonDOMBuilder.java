package uk.ac.starlink.votable;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataBufferedInputStream;
import uk.ac.starlink.util.PipeReaderThread;
import uk.ac.starlink.util.URLUtils;

/**
 * Builds a skeleton DOM based on the SAX parse of a VOTable document.
 * The DOM consists of everything in the input stream apart from the
 * contents of DATA elements.  Instead of populating these, a supplied
 * {@link uk.ac.starlink.votable.TableHandler} is notified of the
 * tabular content.  In case of STREAM content with href attributes
 * the abstract {@link #processFitsHref}/{@link #processBinaryHref}
 * method is called instead.  If the table handler is null, then the
 * table elements are just ignored.
 * 
 * <p>The skeleton DOM constructed
 *
 * @author   Mark Taylor (Starlink)
 * @since    13 Apr 2005
 */
abstract class SkeletonDOMBuilder extends CustomDOMBuilder {

    private final ContentHandler basicHandler_;
    private final ContentHandler defaultHandler_;
    private TableHandler tableHandler_;
    private String systemId_;
    private TableElement tableEl_;

    /**
     * Constructor.
     *
     * @param  strict whether to effect a strict reading of the VOTable standard
     */
    public SkeletonDOMBuilder( boolean strict ) {
        super( strict );
        basicHandler_ = new BasicContentHandler();
        defaultHandler_ = new DefaultContentHandler();
        setCustomHandler( basicHandler_ );
    }

    /**
     * Sets the table handler which will be notified of inline tables
     * encountered in the parse.
     *
     * @param  tableHandler   new table handler
     */
    public void setTableHandler( TableHandler tableHandler ) {
        tableHandler_ = tableHandler;
    }

    /**
     * Returns the table handler which is informed of inline tables
     * encountered during the parse.
     *
     * @return  table handler
     */
    public TableHandler getTableHandler() {
        return tableHandler_;
    }

    /**
     * Invoked if a FITS/STREAM element with a non-empty href attribute
     * is encountered.  In this case the TableHandler is not notified.
     *
     * @param   url   the URL corresponding to the href attribute
     * @param   extnum  the value of the extnum attribute on the FITS element
     * @param   atts  the full set of attributes on the STREAM element
     */
    protected abstract void processFitsHref( URL url, String extnum,
                                             Attributes atts )
            throws SAXException;

    /**
     * Invoked if a BINARY/STREAM or BINARY2/STREAM element with a
     * non-empty href attribute is encountered.
     * In this case the TableHandler is not notified.
     *
     * @param   url   the URL corresponding to the href attribute
     * @param   atts  the full set of attributes on the STREAM element
     * @param   isBinary2  true for BINARY2, false for BINARY
     */
    protected abstract void processBinaryHref( URL url, Attributes atts,
                                               boolean isBinary2 )
            throws SAXException;

    /**
     * Returns the TableElement corresponding to the TABLE currently
     * being processed.  If the parser is not currently inside a TABLE
     * it will be null.
     *
     * @return  current TABLE
     */
    protected TableElement getTableElement() {
        return tableEl_;
    }

    /**
     * Returns an array of Decoder objects for a given set of fields.
     *
     * @param   fields  array of FIELD elements to get decoders for
     * @return  array of corresponding decoders
     */
    static Decoder[] getDecoders( FieldElement[] fields ) {
        int ncol = fields.length;
        Decoder[] decoders = new Decoder[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            decoders[ icol ] = fields[ icol ].getDecoder();
        }
        return decoders;
    }

    /**
     * Marshalling handler - watches for elements which we know how to
     * do custom processing on, and installs the appropriate custom
     * handler on start element events.
     */
    private class BasicContentHandler extends DefaultContentHandler {

        public void startDocument() throws SAXException {
            super.startDocument();
            if ( getLocator() != null ) {
                systemId_ = getLocator().getSystemId();
            }
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
                throws SAXException {
            super.startElement( namespaceURI, localName, qName, atts );
            String tagName = getVOTagName( namespaceURI, localName, qName );
            if ( "TABLE".equals( tagName ) ) {
                tableEl_ = (TableElement) getNewestNode();
            }
            else if ( "DATA".equals( tagName ) ) {
                if ( tableHandler_ == null || tableEl_ == null ) {
                    Node dataNode = getNewestNode();
                    dataNode.getParentNode().removeChild( dataNode );
                    setCustomHandler( new IgnoreContentHandler( tagName ) );
                }
            }
            else if ( "TABLEDATA".equals( tagName ) ) {
                if ( tableEl_ != null ) {
                    setCustomHandler( new TabledataHandler() );
                }
            }
            else if ( "STREAM".equals( tagName ) ) {
                Element parent = (Element) getNewestNode().getParentNode();
                String parentName = getVOTagName( parent );
                DataFormat fmt = null;
                String extnum = null;
                if ( parentName.equals( "BINARY" ) ) {
                    fmt = DataFormat.BINARY;
                }
                else if ( parentName.equals( "BINARY2" ) ) {
                    fmt = DataFormat.BINARY2;
                }
                else if ( parentName.equals( "FITS" ) ) {
                    fmt = DataFormat.FITS;
                    if ( parent.hasAttribute( "extnum" ) ) {
                        extnum = parent.getAttribute( "extnum" );
                    }
                }
                String href = getAttribute( atts, "href" );
                if ( href != null && href.trim().length() > 0 ) {
                    URL url = URLUtils.makeURL( systemId_, href.trim() );
                    if ( fmt == DataFormat.BINARY ) {
                        processBinaryHref( url, atts, false );
                    }
                    else if ( fmt == DataFormat.BINARY2 ) {
                        processBinaryHref( url, atts, true );
                    }
                    else if ( fmt == DataFormat.FITS ) {
                        processFitsHref( url, extnum, atts );
                    }
                    setCustomHandler( new IgnoreContentHandler( "STREAM" ) );
                }
                else {
                    try {
                        if ( fmt == DataFormat.BINARY ) {
                            setCustomHandler(
                                new InlineBinaryStreamHandler( false ) );
                        }
                        else if ( fmt == DataFormat.BINARY2 ) {
                            setCustomHandler(
                                new InlineBinaryStreamHandler( true ) );
                        }
                        else if ( fmt == DataFormat.FITS ) {
                            setCustomHandler(
                                    new InlineFITSStreamHandler( extnum ) );
                        }
                    }
                    catch ( Throwable e ) {
                        throw (SAXException) 
                              new SAXParseException( e.getMessage(),
                                                     getLocator() )
                             .initCause( e );
                    }
                }
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            super.endElement( namespaceURI, localName, qName );
            String tagName = getVOTagName( namespaceURI, localName, qName );
            if ( "TABLE".equals( tagName ) ) {
                tableEl_ = null;
            }
        }
    }

    /**
     * Handler which just ignores everything until the end of the current
     * element.
     */
    private class IgnoreContentHandler extends NullContentHandler {
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
                      .equals( getVOTagName( namespaceURI, localName, qName ) );
                basicHandler_.endElement( namespaceURI, localName, qName );
                setCustomHandler( basicHandler_ );
            }
        }
    }

    /**
     * Custom handler for TABLEDATA element.
     */
    private class TabledataHandler extends NullContentHandler {
        final Decoder[] decoders_;
        final int ncol_;
        StringBuffer cell_;
        Object[] row_;
        int icol_;
        boolean inCell_;

        TabledataHandler() throws SAXException {
            FieldElement[] fields = tableEl_.getFields();
            ncol_ = fields.length;
            decoders_ = getDecoders( fields );
            cell_ = new StringBuffer();
            Element tabledataEl = (Element) getNewestNode();
            String comment = "Invisible data nodes were parsed directly";
            tabledataEl.appendChild( tabledataEl.getOwnerDocument()
                                                .createComment( comment ) );
            if ( tableHandler_ != null ) {
                tableHandler_.startTable( tableEl_.getMetadataTable() );
            }
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts ) {
            String tagName = getVOTagName( namespaceURI, localName, qName );
            if ( "TD".equals( tagName ) ) {
                cell_.setLength( 0 );
                inCell_ = true;
            }
            else if ( "TR".equals( tagName ) ) {
                row_ = new Object[ ncol_ ];
                icol_ = 0;
            }
        }

        public void characters( char[] ch, int start, int length ) {
            if ( inCell_ ) {
                cell_.append( ch, start, length );
            }
        }

        public void ignorableWhitespace( char[] ch, int start, int length ) {
            if ( inCell_ ) {
                cell_.append( ch, start, length );
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            String tagName = getVOTagName( namespaceURI, localName, qName );
            if ( inCell_ && "TD".equals( tagName ) && icol_ < ncol_
                         && row_ != null ) {
                row_[ icol_ ] = cell_.length() > 0
                         ? decoders_[ icol_ ].decodeString( cell_.toString() )
                         : null;
                icol_++;
                inCell_ = false;
            }
            else if ( "TR".equals( tagName ) ) {
                if ( tableHandler_ != null ) {
                    tableHandler_.rowData( row_ );
                }
            }
            else if (  "TABLEDATA".equals( tagName ) ) {
                defaultHandler_.endElement( namespaceURI, localName, qName );
                setCustomHandler( basicHandler_ );
                if ( tableHandler_ != null ) {
                    tableHandler_.endTable();
                }
            }
        }
    }

    /**
     * Custom handler for a STREAM child of a BINARY or BINARY2 element
     * which has data base64-encoded inline.
     */
    private class InlineBinaryStreamHandler extends NullContentHandler {

        final PipeReaderThread reader_;
        final Writer out_;

        /**
         * Constructor.
         *
         * @param  isBinary2  true for BINARY2, false for BINARY
         */
        public InlineBinaryStreamHandler( final boolean isBinary2 )
                throws IOException, SAXException {
            assert tableHandler_ != null;
            assert tableEl_ != null;

            /* Set up a thread which will read from the other end of the pipe
             * and write it into a row list. */
            FieldElement[] fields = tableEl_.getFields();
            final Decoder[] decoders = getDecoders( fields );
            tableHandler_.startTable( tableEl_.getMetadataTable() );
            reader_ = new PipeReaderThread() {
                protected void doReading( InputStream datain )
                        throws IOException {
                    InputStream in = new DataBufferedInputStream( datain );
                    RowSequence rseq =
                        new BinaryRowSequence( decoders, in, "base64",
                                               isBinary2 );
                    try {
                        while ( rseq.next() ) {
                            tableHandler_.rowData( rseq.getRow() );
                        }
                    }
                    catch ( SAXException e ) {
                        throw (IOException) new IOException( e.getMessage() )
                             .initCause( e );
                    }
                    catch ( OutOfMemoryError e ) {
                        tableHandler_ = null;
                        throw e;
                    }
                    finally {
                        rseq.close();
                    }
                }
            };

            /* Set up a pipe we can write encountered characters into. */
            OutputStream b64out = reader_.getOutputStream();
            out_ = new OutputStreamWriter( new BufferedOutputStream( b64out ) );

            /* Begin the read. */
            reader_.start();
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
                throws SAXException {
            String tagName = getVOTagName( namespaceURI, localName, qName );
            throw new SAXParseException( "Unwelcome child <" + tagName +
                                         "> of STREAM", getLocator() );
        }

        public void characters( char[] ch, int start, int length )
                throws SAXException {
            try {
                out_.write( ch, start, length );
            }
            catch ( IOException e ) {
                throw (SAXException)
                      new SAXParseException( e.getMessage(), getLocator(), e )
                     .initCause( e );
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            String tagName = getVOTagName( namespaceURI, localName, qName );
            if ( "STREAM".equals( tagName ) ) {
                defaultHandler_.endElement( namespaceURI, localName, qName );
                setCustomHandler( basicHandler_ );

                /* Tidy up, wait for the reader to finish reading, and
                 * rethrow any exceptions that we encounter in either thread. */
                try {
                    out_.close();
                    reader_.finishReading();
                    tableHandler_.endTable();
                }
                catch ( IOException e ) {
                    if ( e.getCause() instanceof SAXException ) {
                        throw (SAXException) e.getCause();
                    }
                    else {
                        throw (SAXException)
                              new SAXParseException( e.getMessage(), 
                                                     getLocator(), e )
                             .initCause( e );
                    }
                }
            }
        }
    }

    /**
     * Custom handler for a STREAM child of a FITS element which has
     * data base64-encoded inline.
     */
    private class InlineFITSStreamHandler extends NullContentHandler {

        final PipeReaderThread reader_;
        final Writer out_;

        public InlineFITSStreamHandler( String extnum ) 
                throws IOException, SAXException {
            assert tableHandler_ != null;
            assert tableEl_ != null;
            if ( extnum != null && ! extnum.matches( "[0-9]+" ) ) {
                extnum = null;
            }
            final TableSink sink = 
                new TableHandlerSink( tableHandler_,
                                      tableEl_.getMetadataTable() );

            /* Set up a thread which will read from the other end of the pipe
             * and write it into a row list. */
            final String ihdu = extnum;
            reader_ = new PipeReaderThread() {
                protected void doReading( InputStream datain )
                        throws IOException {
                    InputStream in =
                        Base64.getMimeDecoder()
                       .wrap( new DataBufferedInputStream( datain ) );
                    new FitsTableBuilder().streamStarTable( in, sink, ihdu );
                }
            };

            /* Set up a stream we can write encountered characters to. */
            OutputStream b64out = reader_.getOutputStream();
            out_ = new OutputStreamWriter( new BufferedOutputStream( b64out ) );

            /* Begin the read. */
            reader_.start();
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
                throws SAXException {
            String tagName = getVOTagName( namespaceURI, localName, qName );
            throw new SAXParseException( "Unwelcome child <" + tagName +
                                         "> of STREAM", getLocator() );
        }

        public void characters( char[] ch, int start, int length )
                throws SAXException {
            try {
                out_.write( ch, start, length );
            }
            catch ( IOException e ) {
                throw (SAXException)
                      new SAXParseException( e.getMessage(), getLocator(), e )
                     .initCause( e );
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            String tagName = getVOTagName( namespaceURI, localName, qName );
            if ( "STREAM".equals( tagName ) ) {
                defaultHandler_.endElement( namespaceURI, localName, qName );
                setCustomHandler( basicHandler_ );

                /* Tidy up, wait for the reader to finish reading, and
                 * rethrow any exceptions that we encounter in either thread. */
                try {
                    out_.close();
                    reader_.finishReading();
                }
                catch ( IOException e ) {
                    if ( e.getCause() instanceof SAXException ) {
                        throw (SAXException) e.getCause();
                    }
                    else {
                        throw (SAXException)
                              new SAXParseException( e.getMessage(),
                                                     getLocator(), e )
                             .initCause( e );
                    }
                }
            }
        }
    }
}
