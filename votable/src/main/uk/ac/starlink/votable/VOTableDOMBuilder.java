package uk.ac.starlink.votable;

import java.io.IOException;
import java.net.URL;
import org.xml.sax.Attributes;
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
class VOTableDOMBuilder extends SkeletonDOMBuilder implements TableHandler {

    private final StoragePolicy storagePolicy_;
    private RowStore rowStore_;

    /**
     * Constructs a new builder.
     */
    public VOTableDOMBuilder( StoragePolicy storagePolicy, boolean strict ) {
        super( strict );
        storagePolicy_ = storagePolicy;
        setTableHandler( this );
    }

    /**
     * Returns the DOM document built by this parser, following a parse.
     * Each TableElement has a TabularData object attached from which
     * the actual table data can be retrieved.
     *
     * @return   DOM
     */
    public Document getDocument() {
        return super.getDocument();
    }

    protected void processBinaryHref( URL url, Attributes atts ) {
        TableElement tableEl = getTableElement();
        if ( tableEl != null ) {
            String encoding = getAttribute( atts, "encoding" );
            FieldElement[] fields = tableEl.getFields();
            Decoder[] decoders = 
                SkeletonDOMBuilder.getDecoders( tableEl.getFields() );
            TabularData tdata = 
                new TableBodies.HrefBinaryTabularData( decoders, url,
                                                       encoding );
            tableEl.setData( tdata );
        }
    }

    protected void processFitsHref( URL url, String extnum, Attributes atts )
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
                       new SAXParseException( e.getMessage(), getLocator() )
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
                  new SAXParseException( e.getMessage(), getLocator() )
                 .initCause( e );
        }
    }

    public void endTable() throws SAXException {
        try {
            rowStore_.endRows();
            TableElement tableEl = getTableElement();
            if ( tableEl != null ) {
                StarTable st = rowStore_.getStarTable();
                tableEl.setData( new TableBodies.StarTableTabularData( st ) );
            }
        }
        catch ( IOException e ) {
            throw (SAXException)
                  new SAXParseException( e.getMessage(), getLocator() )
                 .initCause( e );
        }
        rowStore_ = null;
    }
}
