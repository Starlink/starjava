package uk.ac.starlink.votable;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.QueueTableSequence;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.StarEntityResolver;

/**
 * Loads multiple tables from a VOTable document.
 * They are inserted into a supplied QueueTableSequence as they are
 * encountered.
 *
 * @author   Mark Taylor
 * @since    29 Sep 2010
 */
class MultiTableStreamer extends SkeletonDOMBuilder implements TableHandler {

    private final StoragePolicy storage_;
    private final QueueTableSequence tqueue_;
    private final Namespacing namespacing_;
    private volatile boolean isVotable_;
    private RowStore rowStore_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Constructor.
     *
     * @param  storage   storage policy
     * @param  tqueue    destination table queue for table results
     * @param  strict whether to effect a strict reading of the VOTable standard
     */
    public MultiTableStreamer( StoragePolicy storage, QueueTableSequence tqueue,
                               boolean strict ) {
        super( strict );
        storage_ = storage;
        tqueue_ = tqueue;
        namespacing_ = Namespacing.getInstance();
        setTableHandler( this );
    }

    public void startTable( StarTable meta ) throws SAXException {
        try {
            rowStore_ = storage_.makeRowStore();
            rowStore_.acceptMetadata( meta );
        }
        catch ( IOException e ) {
            tqueue_.addError( e );
            rowStore_ = null;
        }
    }

    public void rowData( Object[] row ) throws SAXException {
        if ( rowStore_ != null ) {
            try {
                rowStore_.acceptRow( row );
            }
            catch ( IOException e ) {
                tqueue_.addError( e );
                rowStore_ = null;
            }
        }
    }

    public void endTable() throws SAXException {
        if ( rowStore_ != null ) {
            StarTable table = null;
            try {
                rowStore_.endRows();
                tqueue_.addTable( rowStore_.getStarTable() );
            }
            catch ( IOException e ) {
                tqueue_.addError( e );
            }
            rowStore_ = null;
        }
    }

    protected void processBinaryHref( URL url, Attributes atts )
            throws SAXException {
        TableElement tableEl = getTableElement();
        if ( tableEl != null ) {
            try {
                String encoding = getAttribute( atts, "encoding" );
                Decoder[] decoders = getDecoders( tableEl.getFields() );
                TabularData tdata =
                    new TableBodies.HrefBinaryTabularData( decoders, url,
                                                           encoding );
                tableEl.setData( tdata );
                tqueue_.addTable( new VOStarTable( tableEl ) );
            }
            catch ( IOException e ) {
                tqueue_.addError( e );
            }
        }
    }

    protected void processFitsHref( URL url, String extnum, Attributes atts )
            throws SAXException {
        TableElement tableEl = getTableElement();
        if ( tableEl != null ) {
            try {
                DataSource datsrc = DataSource.makeDataSource( url );
                datsrc.setPosition( extnum );
                StarTable ft = new FitsTableBuilder()
                              .makeStarTable( datsrc, false, storage_ );
                TabularData tdata =
                    new TableBodies.StarTableTabularData( ft );
                tableEl.setData( tdata );
                tqueue_.addTable( new VOStarTable( tableEl ) );
            }
            catch ( IOException e ) {
                tqueue_.addError( e );
            }
        }
    }

    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts )
            throws SAXException {
        if ( ! isVotable_ &&
             "VOTABLE".equals( getVOTagName( namespaceURI, localName,
                                             qName ) ) ) {
            isVotable_ = true;
            synchronized ( this ) {
                notifyAll();
            }
        }
        super.startElement( namespaceURI, localName, qName, atts );
    }

    /**
     * Utility method which uses an instance of this class to turn a
     * SAX source into a table sequence.  The returned sequence will
     * be populated on a different thread.  It will only be returned
     * once it looks like the supplied stream does actually represent
     * a VOTable; if it does not, a TableFormatException will be thrown.
     *
     * @param  saxsrc  input SAX event stream
     * @param  storage  storage policy
     * @param  strict whether to effect a strict reading of the VOTable standard
     */
    public static TableSequence streamStarTables( final InputSource saxsrc,
                                                  StoragePolicy storage,
                                                  boolean strict )
            throws SAXException, IOException {

        /* Construct a table handler which can pull out tables from a
         * document and add them to a table queue. */
        final QueueTableSequence tqueue = new QueueTableSequence();
        final MultiTableStreamer streamer =
            new MultiTableStreamer( storage, tqueue, strict );

        /* Get a SAX parser. */
        final XMLReader parser;
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

        /* Perform the parse in a new thread. */
        final boolean[] doneHolder = new boolean[ 1 ];
        Thread worker = new Thread( "VOTable streamer" ) {
            public void run() {
                try {
                    parser.parse( saxsrc );
                }
                catch ( Throwable e ) {
                    tqueue.addError( e );
                }
                finally {
                    tqueue.endSequence();
                    synchronized ( streamer ) {
                        doneHolder[ 0 ] = true;
                        streamer.notifyAll();
                    }
                }
            }
        };
        worker.setDaemon( true );
        worker.start();

        /* Wait until we can see that the SAX stream does contain a VOTable
         * document; if it does, return the stream which continues to be
         * populated by the worker thread, otherwise, throw an exception
         * indicating that this is not suitable input. */
        synchronized ( streamer ) {
            while ( ! streamer.isVotable_ && ! doneHolder[ 0 ] ) {
                try {
                    streamer.wait();
                }
                catch ( InterruptedException e ) {
                    throw (IOException) new InterruptedIOException()
                                       .initCause( e );
                }
            }
        }
        if ( ! streamer.isVotable_ ) {
            throw new TableFormatException( "No VOTABLE element" );
        }
        else {
            return tqueue;
        }
    }
}
