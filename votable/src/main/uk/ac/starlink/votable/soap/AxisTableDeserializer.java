package uk.ac.starlink.votable.soap;

import java.io.IOException;
import org.apache.axis.encoding.DeserializerImpl;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.message.SOAPHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VODocument;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOSAXDocumentBuilder;
import uk.ac.starlink.votable.VOStarTable;

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
public class AxisTableDeserializer extends DeserializerImpl {

    private StarTable starTable_;
    private VOSAXDocumentBuilder voParser_;

    public SOAPHandler onStartChild( String namespace, String localName,
                                     String prefix, Attributes atts,
                                     DeserializationContext context )
            throws SAXException {
        if ( "VOTABLE".equals( localName ) ) {
            voParser_ = new VOSAXDocumentBuilder( true );
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
        if ( componentsReady() ) {
            valueComplete();
        }
    }

    public Object getValue() {
        return starTable_;
    }

    public boolean componentsReady() {
        return voParser_ != null
            && voParser_.getDocument() != null
            && voParser_.getDocument().getDocumentElement() != null;
    }

    public void valueComplete() throws SAXException {
        if ( voParser_ != null && starTable_ == null ) {
            voParser_.endDocument();
            VODocument doc = (VODocument) voParser_.getDocument();
            VOElement docEl = (VOElement) doc.getDocumentElement();
            TableElement tabEl = 
                (TableElement) docEl.getChildByName( "RESOURCE" )
                                    .getChildByName( "TABLE" );
            try {
                starTable_ = new VOStarTable( tabEl );
                setValue( starTable_ );
            }
            catch ( IOException e ) {
                throw (SAXException) 
                      new SAXException( "VOTable deserialization failed" )
                     .initCause( e );
            }
        }
        super.valueComplete();
    }
}
