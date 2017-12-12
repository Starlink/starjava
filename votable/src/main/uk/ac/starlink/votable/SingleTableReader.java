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
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.StarEntityResolver;

/**
 * ContentHandler which builds a DOM from a SAX stream including
 * only the data from a single TABLE element.
 *
 * <p>Currently it goes all the way through the SAX stream building the DOM.
 * It could bail out some time before the end once it's got its table.
 * However, this is tricky, since sometimes some elements after the
 * end of the TABLE in question are important for the table metadata,
 * so for now don't do that.
 *
 * @author   Mark Taylor
 * @since    13 Dec 2017
 */
class SingleTableReader extends VOTableParser implements TableHandler {

    private final StoragePolicy storage_;
    private final Namespacing namespacing_;
    private RowStore rowStore_;
    private boolean isVotable_;
    private TableElement tableEl_;
    private int skipTables_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Constructor.
     *
     * @param   storage   storage policy
     * @param   itable   index of table to be streamed - 0 means the first
     *          TABLE element encountered, 1 means the second etc
     * @param  strict whether to enforce strict reading of the VOTable standard
     */
    public SingleTableReader( StoragePolicy storage, int itable,
                              boolean strict ) {
        super( strict );
        storage_ = storage;
        skipTables_ = itable;
        setTableHandler( null );
        setReadHrefTables( false );
        namespacing_ = Namespacing.getInstance();
    }

    /**
     * Returns the TABLE element requested, including its TabularData.
     *
     * @return  TABLE element if one has been found, otherwise null
     */
    public TableElement getTargetTableElement() {
        return tableEl_;
    }

    /**
     * Indicates whether the parse so far appears to be of a VOTABLE XML
     * document.
     *
     * @return  true iff XML looks like VOTable
     */
    public boolean isVotable() {
        return isVotable_;
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
        if ( "TABLE".equals( tagName ) ) {

            /* Prepare to process table data only if we have reached the
             * start of the target table. */
            if ( skipTables_-- == 0 ) {
                setReadHrefTables( true );
                setTableHandler( this );
            }
        }
    }

    public void startTable( StarTable meta ) throws SAXException {
        rowStore_ = storage_.makeConfiguredRowStore( meta );
    }

    public void rowData( Object[] row ) throws SAXException {
        try {
            rowStore_.acceptRow( row );
        }
        catch ( IOException e ) {
            throw (SAXException)
                  new SAXParseException( e.getMessage(), getLocator(), e )
                 .initCause( e );
        }
    }

    public void endTable() throws SAXException {

        /* Ignore table data once we have processed one table. */
        setTableHandler( null );
        setReadHrefTables( false );

        /* Package the gathered data as a TableElement for later use. */
        try {
            rowStore_.endRows();
            tableEl_ = getTableElement();
            TabularData tdata =
                new TableBodies.StarTableTabularData( rowStore_
                                                     .getStarTable() );
            tableEl_.setData( tdata );
        }
        catch ( IOException e ) {
            throw (SAXException)
                  new SAXParseException( e.getMessage(), getLocator(), e )
                 .initCause( e );
        }
    }

    /**
     * Acquires a single StarTable gathered from parsing a VOTable document.
     *
     * @param  saxsrc   SAX source from which the VOTable document
     *                  will be supplied
     * @param  storage  storage policy for row data
     * @param  itable index of the table in the document to be read
     *                (0-based)
     * @param  strict whether to enforce strict reading of the VOTable standard
     */
    public static VOStarTable readStarTable( final InputSource saxsrc,
                                             int itable, StoragePolicy storage,
                                             boolean strict )
            throws IOException, SAXException {

        /* Construct a reader instance for the requested table. */
        SingleTableReader reader =
            new SingleTableReader( storage, itable, strict );

        /* Get a SAX parser. */
        final XMLReader parser;
        try {
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setValidating( false );
            reader.namespacing_.configureSAXParserFactory( spfact );
            parser = spfact.newSAXParser().getXMLReader();
        }
        catch ( ParserConfigurationException e ) {
            throw (SAXException) new SAXException( e.getMessage(), e )
                                .initCause( e );
        }

        /* Install the content handler. */
        parser.setContentHandler( reader );

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

        /* Do the parse. */
        try {
            parser.parse( saxsrc );
        }
        catch ( CharConversionException e ) {
            if ( reader.isVotable_ ) {
                throw e;
            }
            else {
                throw new TableFormatException( "Bad XML characters", e );
            }
        }
        catch ( SAXException e ) {
            e = VOElementFactory.fixStackTrace( e );
            if ( reader.isVotable_ ) {
                throw e;
            }
            else {
                throw new TableFormatException( e );
            }
        }

        /* Return the result or signal an error. */
        if ( reader.isVotable_ ) {
            TableElement tableEl = reader.getTargetTableElement();
            if ( tableEl != null ) {
                return new VOStarTable( tableEl );
            }
            else {
                throw new IOException( "No TABLE element found" );
            }
        }
        else {
            throw new TableFormatException( "No VOTABLE element" );
        }
    }
}
