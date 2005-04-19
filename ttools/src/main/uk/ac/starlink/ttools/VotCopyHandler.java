package uk.ac.starlink.ttools;

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
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.TableContentHandler;
import uk.ac.starlink.votable.TableHandler;
import uk.ac.starlink.votable.VOSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

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
class VotCopyHandler implements ContentHandler, LexicalHandler, TableHandler {

    private final DataFormat format_;
    private final boolean inline_;
    private final String baseLoc_;
    private final TableContentHandler votParser_;
    private final SAXWriter saxWriter_;
    private final ContentHandler discardHandler_;
    private final HandlerStack handlerStack_;
    private ContentHandler handler_;
    private Thread streamThread_;
    private Throwable streamError_;
    private StreamRowStore streamStore_;
    private BufferedWriter out_;
    private Locator locator_;
    private int iTable_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools" );

    /**
     * Constructor.
     *
     * @param  strict  whether to effect strict interpretation of the
     *                 VOTable standard
     * @param  format  encoding type for output DATA elements; may be null
     *                 for DATA-less output
     * @param  inline  true for tables written inline, false for tables written
     *                 to an href-referenced stream
     * @param  base    base table location; used to construct URIs for
     *                 out-of-line table streams (only used if inline=false)
     */
    public VotCopyHandler( boolean strict, DataFormat format, boolean inline,
                           String base ) {
        if ( ! inline && base == null ) {
            throw new IllegalArgumentException( "Must specify base location " +
                                                "for out-of-line tables" );
        }
        format_ = format;
        inline_ = inline;
        baseLoc_ = base;
        votParser_ = new TableContentHandler( strict );
        votParser_.setReadHrefTables( true );
        votParser_.setTableHandler( this );
        saxWriter_ = new SAXWriter();
        discardHandler_ = new DefaultHandler();
        handlerStack_ = new HandlerStack();
        handler_ = saxWriter_;
        setOutput( new OutputStreamWriter( System.out ) );
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
        iTable_++;
        assert handler_ == discardHandler_;
        assert streamThread_ == null;
        streamStore_ = new StreamRowStore();
        streamThread_ = new Thread( "Table Streamer" ) {
          
            public void run() {
                try {
                    if ( format_ != null ) {
                        assert handler_ == discardHandler_;
                        streamStore_.acceptMetadata( meta );
                        writeDataElement( streamStore_.getStarTable() );
                        assert handler_ == discardHandler_;
                    }
                    else {
                        out_.write( "<!-- No data -->" );
                    }
                }
                catch ( IOException e ) {
                    streamError_ = e;
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
        try {
            streamStore_.endRows();
            streamThread_.join();
        }
        catch ( InterruptedException e ) {
            throw (SAXException)
                  new SAXParseException( e.getMessage(), locator_ )
                 .initCause( e );
        }
        assert handler_ == discardHandler_;
        streamThread_ = null;

        /* If an error was encountered during writing the table (at the
         * other end of the stream), rethrow it here. */
        if ( streamError_ != null ) {
            String msg;
            if ( streamError_ instanceof StreamRereadException ) {
                msg = "Can't stream, " +
                      "table requires multiple reads for metadata";
            }
            else {
                msg = streamError_.getMessage();
            }
            throw (SAXException) new SAXParseException( msg, locator_ )
                                .initCause( streamError_ );
        }
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
        }
        else if ( "FIELD".equals( localName ) ) {

            /* Unfortunately we have to translate unsignedByte datatypes
             * to short ones here.  This is because the serializers in the
             * VOTable package all use short as an internal representation
             * for unsignedByte because of the difficulties of representing
             * an unsigned value in Java. */
            if ( "unsignedByte".equals( atts.getValue( "datatype" ) ) ) {
                AttributesImpl newAtts = new AttributesImpl( atts );
                int itype = newAtts.getIndex( "datatype" );
                newAtts.setValue( itype, "short" );
                atts = newAtts;
                log( Level.WARNING, "FIELD datatype has been changed from " +
                                    "unsignedByte to short" );
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
    private void writeDataElement( StarTable table ) throws IOException {

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
            String href = file.isAbsolute() ? file.toString()
                                            : file.getName();
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
            buf.append( msg );
        }
        logger_.log( level, buf.toString() );
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
