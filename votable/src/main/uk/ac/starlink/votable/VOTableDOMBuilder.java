package uk.ac.starlink.votable;

import java.io.IOException;
import java.net.URL;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;

/**
 * Custom DOM builder for parsing VOTable documents or fragments.
 * For the most part it builds a DOM, but within data-heavy elements
 * (those which represent table data) it intercepts SAX events
 * directly to construct the table data
 * which it stores in the corresponding TableElement node,
 * rather than installing bulk data
 * as CDATA or Element nodes within the resulting DOM.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOTableDOMBuilder implements ContentHandler {

    private final Worker worker_;

    /**
     * Constructor.
     *
     * @param  storagePolicy  policy for bulk data storage
     * @param  strict  true iff you want strict enforcement of VOTable standard
     */
    public VOTableDOMBuilder( StoragePolicy storagePolicy, boolean strict ) {

        /* All ContentHandler methods are delegated to an instance of an
         * inner class. */
        worker_ = new Worker( storagePolicy, strict );
    }

    /**
     * Returns the DOM document built by this parser, following a parse.
     * Each TableElement has a TabularData object attached from which
     * the actual table data can be retrieved.
     *
     * @return   VOTable DOM
     */
    public VODocument getDocument() {
        return (VODocument) worker_.getDocument();
    }

    public void setDocumentLocator( Locator locator ) {
        worker_.setDocumentLocator( locator );
    }

    public void startDocument() throws SAXException {
        worker_.startDocument();
    }

    public void endDocument() throws SAXException {
        worker_.endDocument();
    }

    public void startPrefixMapping( String prefix, String uri )
            throws SAXException {
        worker_.startPrefixMapping( prefix, uri );
    }

    public void endPrefixMapping( String prefix ) throws SAXException {
        worker_.endPrefixMapping( prefix );
    }

    public void startElement( String uri, String localName, String qName,
                              Attributes atts ) throws SAXException {
        worker_.startElement( uri, localName, qName, atts );
    }

    public void endElement( String uri, String localName, String qName )
            throws SAXException {
        worker_.endElement( uri, localName, qName );
    }

    public void characters( char[] ch, int start, int length )
            throws SAXException {
        worker_.characters( ch, start, length );
    }

    public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException {
        worker_.ignorableWhitespace( ch, start, length );
    }

    public void processingInstruction( String target, String data )
            throws SAXException {
        worker_.processingInstruction( target, data );
    }

    public void skippedEntity( String name ) throws SAXException {
        worker_.skippedEntity( name );
    }

    /**
     * Inner class which does all the work for the VOTableDOMBuilder.
     * The point of delegating to this inner class is to hide the
     * implementation details from the public interface.
     */
    private static class Worker extends SkeletonDOMBuilder
                                implements TableHandler {
        private final StoragePolicy storagePolicy_;
        private RowStore rowStore_;

        Worker( StoragePolicy storagePolicy, boolean strict ) {
            super( strict );
            storagePolicy_ = storagePolicy;
            setTableHandler( this );
        }

        protected void processBinaryHref( URL url, Attributes atts ) {
            TableElement tableEl = getTableElement();
            if ( tableEl != null ) {
                String encoding = getAttribute( atts, "encoding" );
                Decoder[] decoders = 
                    SkeletonDOMBuilder.getDecoders( tableEl.getFields() );
                TabularData tdata = 
                    new TableBodies.HrefBinaryTabularData( decoders, url,
                                                           encoding );
                tableEl.setData( tdata );
            }
        }

        protected void processFitsHref( URL url, String extnum,
                                        Attributes atts )
                throws SAXException {
             TableElement tableEl = getTableElement();
             if ( tableEl != null ) {
                 try {
                     DataSource datsrc = DataSource.makeDataSource( url );
                     datsrc.setPosition( extnum );
                     StarTable startab = new FitsTableBuilder()
                                        .makeStarTable( datsrc, false,
                                                        storagePolicy_ );
                     TabularData tdata = 
                         new TableBodies.StarTableTabularData( startab );
                     tableEl.setData( tdata );
                 }
                 catch ( IOException e ) {
                     throw (SAXException)
                           new SAXParseException( e.getMessage(),
                                                  getLocator(), e )
                          .initCause( e );
                 }
             }
        }

        public void startTable( StarTable meta ) throws SAXException {
            rowStore_ = storagePolicy_.makeConfiguredRowStore( meta );
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
            try {
                rowStore_.endRows();
                TableElement tableEl = getTableElement();
                if ( tableEl != null ) {
                    StarTable st = rowStore_.getStarTable();
                    tableEl.setData( new TableBodies
                                        .StarTableTabularData( st ) );
                }
            }
            catch ( IOException e ) {
                throw (SAXException)
                      new SAXParseException( e.getMessage(), getLocator(), e )
                     .initCause( e );
            }
            rowStore_ = null;
        }
    }
}
