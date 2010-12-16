package uk.ac.starlink.votable.soap;

import java.io.IOException;
import org.apache.axis.encoding.DeserializerImpl;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.message.SOAPHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Element;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.votable.TableContentHandler;
import uk.ac.starlink.votable.TableHandler;

/**
 * Custom deserializer for VOTables.
 * The serialized stream is assumed to be a valid VOTABLE element containing
 * a RESOURCE element containing a TABLE element.
 *
 * <p>The implementation of this class is tailored to various 
 * ill-documented idiosyncracies of AXIS's deserialization machinery.
 * Tinker at your peril.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Mar 2005
 */
public class AxisTableDeserializer extends DeserializerImpl
                                   implements TableHandler {

    private final StoragePolicy storagePolicy_;
    private StarTable starTable_;
    private TableContentHandler voParser_;
    private RowStore rowStore_;
    private boolean done_;

    /**
     * Constructor.
     *
     * @param  storagePolicy  policy for storing streamed table data
     */
    public AxisTableDeserializer( StoragePolicy storagePolicy ) {
        storagePolicy_ = storagePolicy;
    }

    public SOAPHandler onStartChild( String namespace, String localName,
                                     String prefix, Attributes atts,
                                     DeserializationContext context )
            throws SAXException {
        if ( "VOTABLE".equals( localName ) ) {
            voParser_ = new TableContentHandler( true );
            voParser_.setTableHandler( this );
            voParser_.startDocument();
            return new DelegatingSOAPHandler( voParser_ );
        }
        else {
            throw new SAXException( "Unexpected child element " + localName );
        }
    }

    public void onEndElement( String namespace, String localName,
                              DeserializationContext context )
            throws SAXException {
        if ( starTable_ != null ) {
            valueComplete();
        }
    }

    public void startTable( StarTable meta ) {
        rowStore_ = storagePolicy_.makeConfiguredRowStore( meta );
    }

    public void rowData( Object[] row ) throws SAXException {
        try {
            rowStore_.acceptRow( row );
        }
        catch ( IOException e ) {
            if ( e.getCause() instanceof SAXException ) {
                throw (SAXException) e.getCause();
            }
            else {
                throw (SAXException)
                      new SAXParseException( e.getMessage(),
                                             voParser_.getLocator(), e )
                     .initCause( e );
            }
        }
    }

    public void endTable() throws SAXException {
        try {
            rowStore_.endRows();
        }
        catch ( IOException e ) {
            if ( e.getCause() instanceof SAXException ) {
                throw (SAXException) e.getCause();
            }
            else {
                throw (SAXException)
                      new SAXParseException( e.getMessage(),
                                             voParser_.getLocator(), e )
                     .initCause( e );
            }
        }
        starTable_ = rowStore_.getStarTable();
    }

    public Object getValue() {
        return starTable_;
    }

    public boolean componentsReady() {
        return done_;
    }

    public void valueComplete() throws SAXException {
        if ( ! done_ ) {
            voParser_.endDocument();
            setValue( starTable_ );
            done_ = true;
        }
        super.valueComplete();
    }
}
