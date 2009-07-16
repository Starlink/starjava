package uk.ac.starlink.ttools.copy;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.starlink.table.OnceRowPipe;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.UnrepeatableSequenceException;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.TableContentHandler;
import uk.ac.starlink.votable.TableHandler;
import uk.ac.starlink.votable.VOSerializer;

/**
 * SAX content handler which takes SAX events and converts them to
 * an output stream in a VOTable-sensitive way.  As far as is possible
 * given the SAX model, each input SAX event is sent to the output 
 * unchanged apart from events within a DATA element, which are written
 * in one of the VOTable encodings as selected by the user.
 *
 * <p>One exception to the rule is that, for implementation-specific 
 * reasons, FIELD elements with <tt>datatype="unsignedByte"</tt> are
 * changed to have <tt>datatype="short"</tt> instead.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Apr 2005
 */
public class VotCopyHandler
        implements ContentHandler, LexicalHandler, TableHandler {

    private final DataFormat format_;
    private final boolean inline_;
    private final String baseLoc_;
    private final boolean strict_;
    private final TableContentHandler votParser_;
    private final SAXWriter saxWriter_;
    private final ContentHandler discardHandler_;
    private final HandlerStack handlerStack_;
    private final TableHandler tableHandler_;
    private ContentHandler handler_;
    private BufferedWriter out_;
    private Locator locator_;
    private int iTable_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools" );

    /**
     * Constructor.  The copy can be done in either cached or streamed
     * mode, determined by the <tt>cache</tt> parameter.
     * In streamed mode, each row encountered in the input SAX stream
     * is copied to the output stream as soon as it is encountered.
     * In cached mode, the whole table is assembled first, and then 
     * written out at the end of the input.  Streamed mode is more efficient,
     * but may not be possible under some circumstances, e.g. for FITS
     * output when the number of rows is not known in advance.  
     * If a streamed copy is attempted when it's not possible,
     * it will fail with a 
     * {@link uk.ac.starlink.table.UnrepeatableSequenceException}
     * (wrapped in a SAXException).
     *
     * @param  strict  whether to effect strict interpretation of the
     *                 VOTable standard
     * @param  format  encoding type for output DATA elements; may be null
     *                 for DATA-less output
     * @param  inline  true for tables written inline, false for tables written
     *                 to an href-referenced stream
     * @param  base    base table location; used to construct URIs for
     *                 out-of-line table streams (only used if inline=false)
     * @param  cache   whether tables will be cached prior to writing
     * @param  policy  storage policy for cached tables
     */
    public VotCopyHandler( boolean strict, DataFormat format, boolean inline,
                           String base, boolean cache, StoragePolicy policy ) {
        if ( ! inline && base == null ) {
            throw new IllegalArgumentException( "Must specify base location " +
                                                "for out-of-line tables" );
        }
        format_ = format;
        inline_ = inline;
        baseLoc_ = base;
        strict_ = strict;
        votParser_ = new TableContentHandler( strict );
        votParser_.setReadHrefTables( true );
        votParser_.setTableHandler( this );
        saxWriter_ = new SAXWriter();
        discardHandler_ = new DefaultHandler();
        handlerStack_ = new HandlerStack();
        handler_ = saxWriter_;
        setOutput( new OutputStreamWriter( System.out ) );

        /* Set up a handler to which table events will be forwarded. */
        if ( format_ == null ) {
            tableHandler_ = new EmptyTableHandler();
        }
        else if ( cache ) {
            tableHandler_ = new CacheTableHandler( policy );
        }
        else {
            tableHandler_ = new StreamTableHandler();
        }
    }

    /**
     * Sets the output stream for output.  By default output is written to
     * standard output using the platform's default encoding.
     *
     * @param   out  output writer
     */
    public void setOutput( Writer out ) {
        out_ = new BufferedWriter( out );
        saxWriter_.setOutput( out_ );
    }

    public void startTable( final StarTable meta ) throws SAXException {
        assert handler_ == discardHandler_;
        tableHandler_.startTable( meta );
    }

    public void rowData( Object[] row ) throws SAXException {
        assert handler_ == discardHandler_;
        tableHandler_.rowData( row );
    }

    public void endTable() throws SAXException {
        assert handler_ == discardHandler_;
        tableHandler_.endTable();
    }

    public void setDocumentLocator( Locator locator ) {
        locator_ = locator;
        votParser_.setDocumentLocator( locator );
        saxWriter_.setDocumentLocator( locator );
    }

    public void startDocument() throws SAXException {
        votParser_.startDocument();
        saxWriter_.startDocument();
        iTable_ = 0;
    }

    public void endDocument() throws SAXException {
        votParser_.endDocument();
        saxWriter_.endDocument();
    }

    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts )
            throws SAXException {
        votParser_.startElement( namespaceURI, localName, qName, atts );
        if ( "DATA".equals( localName ) ) {
            handlerStack_.push( handler_ );
            handler_ = discardHandler_;
            saxWriter_.flush();
        }
        else if ( "FIELD".equals( localName ) ) {
            String datatype = atts.getValue( "datatype" );

            /* Unfortunately we have to translate unsignedByte datatypes
             * to short ones here.  This is because the serializers in the
             * VOTable package all use short as an internal representation
             * for unsignedByte because of the difficulties of representing
             * an unsigned value in Java. */
            if ( "unsignedByte".equals( datatype ) ) {
                AttributesImpl newAtts = new AttributesImpl( atts );
                int itype = newAtts.getIndex( "datatype" );
                newAtts.setValue( itype, "short" );
                atts = newAtts;
                log( Level.WARNING, "FIELD datatype has been changed from " +
                                    "unsignedByte to short" );
            }

            /* Fix up arraysize values here. */
            if ( ( "char".equals( datatype ) ||
                   "unsignedChar".equals( datatype ) ) &&
                 atts.getValue( "arraysize" ) == null ) {
                String arraysize;
                if ( strict_ ) {
                    arraysize = "1";
                    log( Level.INFO, "Inserted arraysize=\"1\" attribute " +
                                     "to reduce confusion" );
                }
                else {
                    arraysize = "*";
                    log( Level.WARNING, "Inserted assumed arrraysize=\"*\"" +
                                        "attribute" );
                }
                AttributesImpl newAtts = new AttributesImpl( atts );
                newAtts.addAttribute( "", "arraysize", "arraysize", "CDATA",
                                      arraysize );
                atts = newAtts;
            }
        }
        handler_.startElement( namespaceURI, localName, qName, atts );
    }

    public void endElement( String namespaceURI, String localName,
                            String qName ) throws SAXException {
        votParser_.endElement( namespaceURI, localName, qName );
        handler_.endElement( namespaceURI, localName, qName );
        if ( "DATA".equals( localName ) ) {
            handler_ = handlerStack_.pop();
        }
    }

    public void characters( char[] ch, int start, int length )
            throws SAXException {
        votParser_.characters( ch, start, length );
        handler_.characters( ch, start, length );
    }

    public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException {
        votParser_.ignorableWhitespace( ch, start, length );
        handler_.ignorableWhitespace( ch, start, length );
    }

    public void startPrefixMapping( String prefix, String uri )
            throws SAXException {
        votParser_.startPrefixMapping( prefix, uri );
        handler_.startPrefixMapping( prefix, uri );
    }

    public void endPrefixMapping( String prefix ) throws SAXException {
        votParser_.endPrefixMapping( prefix );
        handler_.endPrefixMapping( prefix );
    }

    public void skippedEntity( String name ) throws SAXException {
        votParser_.skippedEntity( name );
        handler_.skippedEntity( name );
    }

    public void processingInstruction( String target, String data )
            throws SAXException {
        votParser_.processingInstruction( target, data );
        handler_.processingInstruction( target, data );
    }

    public void comment( char[] ch, int start, int length )
            throws SAXException {
        if ( handler_ instanceof LexicalHandler ) {
            ((LexicalHandler) handler_).comment( ch, start, length );
        }
    }

    public void startCDATA() throws SAXException {
        if ( handler_ instanceof LexicalHandler ) {
            ((LexicalHandler) handler_).startCDATA();
        }
    }

    public void endCDATA() throws SAXException {
        if ( handler_ instanceof LexicalHandler ) {
            ((LexicalHandler) handler_).endCDATA();
        }
    }

    public void startDTD( String name, String publicId, String systemId )
            throws SAXException {
        handlerStack_.push( handler_ );
        handler_ = discardHandler_;
        if ( handler_ instanceof LexicalHandler ) {
            ((LexicalHandler) handler_).startDTD( name, publicId, systemId );
        }
    }

    public void endDTD() throws SAXException {
        if ( handler_ instanceof LexicalHandler ) {
            ((LexicalHandler) handler_).endDTD();
        }
        handler_ = handlerStack_.pop();
    }

    public void startEntity( String name ) throws SAXException {
        if ( handler_ instanceof LexicalHandler ) {
            ((LexicalHandler) handler_).startEntity( name );
        }
    }

    public void endEntity( String name ) throws SAXException {
        if ( handler_ instanceof LexicalHandler ) {
            ((LexicalHandler) handler_).endEntity( name );
        }
    }

    /**
     * Outputs a DATA element representing a table to the destination stream
     * according to the current settings.
     *
     * @param   table  table to write
     */
    public void writeDataElement( StarTable table ) throws IOException {
        iTable_++;

        /* Construct a serializer which can write the table data. */
        VOSerializer voser = VOSerializer.makeSerializer( format_, table );

        /* If it's out-of-line, open a new file for output and write data
         * to it. */
        if ( ( format_ == DataFormat.BINARY || format_ == DataFormat.FITS ) &&
             ! inline_ && baseLoc_ != null ) {
            String ext = format_ == DataFormat.BINARY ? ".bin" : ".fits";
            File file = new File( baseLoc_ + "-" + iTable_ + ext );
            if ( file.exists() ) {
                log( Level.WARNING, "Overwriting file " + file + " for table " +
                                    iTable_ + " data" );
            }
            else {
                log( Level.INFO, "Writing data for table " + iTable_ + 
                                 " in file " + file );
            }
            String href = file.toString();
            DataOutputStream datstrm = new DataOutputStream(
                                           new BufferedOutputStream(
                                               new FileOutputStream( file ) ) );
            voser.writeHrefDataElement( out_, href, datstrm );
            datstrm.flush();
            datstrm.close();
        }

        /* Otherwise, just write the data inline. */
        else {
            voser.writeInlineDataElement( out_ );
        }
    }

    /**
     * Writes a message through the log system.
     *
     * @param  level   log level
     * @param  msg    message
     */
    private void log( Level level, String msg ) {
        StringBuffer buf = new StringBuffer();
        if ( locator_ != null ) {
            int line = locator_.getLineNumber();
            int col = locator_.getColumnNumber();
            if ( line >= 0 ) {
                buf.append( "l." + line );
                if ( col >= 0 ) {
                    buf.append( ", c." + col );
                }
                buf.append( ": " );
            }
        }
        buf.append( msg );
        logger_.log( level, buf.toString() );
    }

    /**
     * Table handler implementation which writes no DATA element.
     */
    private class EmptyTableHandler implements TableHandler {

        public void startTable( StarTable meta ) {
            try {
                out_.write( "<!-- no data -->" );
            }
            catch ( IOException e ) {
                // doesn't really matter
            }
        }

        public void rowData( Object[] row ) {
        }

        public void endTable() {
        }
    }

    /**
     * Table handler implementation which copies table data from a stream
     * as it comes in.  This is only any good if the output can be written
     * using a one-pass stream.
     */
    private class StreamTableHandler implements TableHandler {

        private Thread streamThread_;
        private OnceRowPipe streamStore_;
        private IOException error_;

        public void startTable( final StarTable meta ) throws SAXException {
            assert streamThread_ == null;
            streamStore_ = new OnceRowPipe();
            streamStore_.acceptMetadata( meta );
            streamThread_ = new Thread( "Table Streamer" ) {
                public void run() {
                    try {
                        writeDataElement( streamStore_.waitForStarTable() );
                    }
                    catch ( IOException e ) {
                        error_ = e;
                    }
                }
            };
            streamThread_.start();
        }

        public void rowData( Object[] row ) throws SAXException {
            try {
                streamStore_.acceptRow( row );
            }
            catch ( IOException e ) {
                throw (SAXException)
                      new SAXParseException( e.getMessage(), locator_ )
                     .initCause( e );
            }
        }

        public void endTable() throws SAXException {
            streamStore_.endRows();
            try {
                streamThread_.join();
            }
            catch ( InterruptedException e ) {
                throw (SAXException)
                      new SAXParseException( "Interrupted", locator_ )
                     .initCause( e );
            }
            streamThread_ = null;
            streamStore_ = null;

            /* If an error was encountered during writing the table (at the
             * other end of the stream), rethrow it here. */
            if ( error_ != null ) {
                String msg;
                if ( error_ instanceof UnrepeatableSequenceException ) {
                    msg = "Can't stream, " +
                          "table requires multiple reads for metadata - " +
                          "try with caching";
                }
                else {
                    msg = error_.getMessage();
                }
                throw (SAXException) new SAXParseException( msg, locator_ )
                                    .initCause( error_ );
            }
        }
    }

    /**
     * Table handler implementation which writes the table to a data cache
     * (as determined by a StoragePolicy object) and then copies it to
     * output at the end.
     */
    private class CacheTableHandler implements TableHandler {

        private final StoragePolicy policy_;
        private RowStore rowStore_;
 
        public CacheTableHandler( StoragePolicy policy ) {
            policy_ = policy;
        }

        public void startTable( StarTable meta ) throws SAXException {
            assert rowStore_ == null;
            rowStore_ = policy_.makeRowStore();
            try {
                rowStore_.acceptMetadata( meta );
            }
            catch ( IOException e ) {
                throw (SAXException)
                      new SAXParseException( e.getMessage(), locator_ )
                     .initCause( e );
            }
        }

        public void rowData( Object[] row ) throws SAXException {
            try {
                rowStore_.acceptRow( row );
            }
            catch ( IOException e ) {
                throw (SAXException)
                      new SAXParseException( e.getMessage(), locator_ )
                     .initCause( e );
            }
        }

        public void endTable() throws SAXException {
            try {
                rowStore_.endRows();
                writeDataElement( rowStore_.getStarTable() );
                rowStore_ = null;
            }
            catch ( IOException e ) {
                throw (SAXException)
                      new SAXParseException( e.getMessage(), locator_ )
                     .initCause( e );
            }
        }
    }

    /**
     * Helper class for saving ContentHandler context.
     * This may be overkill; as currently
     * implemented all events are passed to a SAX copier under normal
     * circumstances, or ignored within a DATA element, so there's only
     * ever either zero or one element on the stack.
     */
    private static class HandlerStack {
        private final List stack_ = new ArrayList();
        private ContentHandler top_;

        /**
         * Pushes a new handler on the stack.
         *
         * @param  handler  new top handler
         */
        public void push( ContentHandler handler ) {
            stack_.add( handler );
            top_ = handler;
        }

        /**
         * Pops a handler off the stack.
         *
         * @return  newly-removed handler
         */
        public ContentHandler pop() {
            int n = stack_.size();
            top_ = n > 1 ? (ContentHandler) stack_.get( n - 2 )
                         : null;
            return (ContentHandler) stack_.remove( n - 1 );
        }
    }
}
