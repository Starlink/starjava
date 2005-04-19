package uk.ac.starlink.ttools;

import java.io.IOException;
import java.util.Arrays;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOStarTable;

/**
 * Can walk a DOM and output its content in a VOTable-aware fashion to
 * a {@link VotCopyHandler}.
 *
 * @author   Mark Taylor (Starlink)
 * @since    19 Apr 2005
 */
public class DOMWriter {

    private final VotCopyHandler handler_;
    private boolean indent_ = true;
    private int level_;

    /**
     * Constructor.
     *
     * @param  handler  destination SAX stream
     */
    public DOMWriter( VotCopyHandler handler ) {
        handler_ = handler;
    }

    /**
     * Outputs a node.
     * In most cases the DOM nodes are converted recursively to SAX
     * ContentHandler or LexicalHandler events in a straightforward way.
     *
     * @param  node to write
     */
    public void writeNode( Node node ) throws SAXException, IOException {
        if ( node instanceof Document ) {
            writeDocument( (Document) node );
        }
        else if ( node instanceof Element ) {
            writeElement( (Element) node );
        }
        else if ( node instanceof CDATASection ) {
            writeCDATA( (CDATASection) node );
        }
        else if ( node instanceof Comment ) {
            writeComment( (Comment) node );
        }
        else if ( node instanceof Text ) {
            writeText( (Text) node );
        }
        else if ( node instanceof EntityReference ) {
            writeEntityReference( (EntityReference) node );
        }
        else if ( node instanceof ProcessingInstruction ) {
            writeProcessingInstruction( (ProcessingInstruction) node );
        }
        else if ( node instanceof DocumentType ) {
            writeDocumentType( (DocumentType) node );
        }
        else if ( node instanceof Attr ) {
        }
        else {
            assert false;
        }
    }

    private void writeDocument( Document doc )
            throws SAXException, IOException{
        handler_.startDocument();
        for ( Node child = doc.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            writeNode( child );
        }
        handler_.endDocument();
    }

    private void writeText( Text text ) throws SAXException {
        char[] data = text.getData().toCharArray();
        handler_.characters( data, 0, data.length );
    }

    private void writeCDATA( CDATASection cdata ) throws SAXException {
        handler_.startCDATA();
        char[] data = cdata.getData().toCharArray();
        handler_.characters( data, 0, data.length );
        handler_.endCDATA();
    }

    private void writeComment( Comment comment ) throws SAXException {
        char[] data = comment.getData().toCharArray();
        handler_.comment( data, 0, data.length );
    }

    private void writeEntityReference( EntityReference ref )
            throws SAXException {
        handler_.skippedEntity( ref.getNodeName() );
    }

    private void writeProcessingInstruction( ProcessingInstruction pi )
            throws SAXException {
        handler_.processingInstruction( pi.getTarget(), pi.getData() );
    }

    private void writeDocumentType( DocumentType dtype ) throws SAXException {
        handler_.startDTD( dtype.getName(), dtype.getPublicId(),
                           dtype.getSystemId() );
        handler_.endDTD();
    }

    private void writeElement( Element el ) throws SAXException, IOException {

        /* If the element is a DATA element, circumvent the usual output
         * procedures and instead write VOTable DATA output given the
         * table data which is stored in the parent TABLE element. */
        if ( "DATA".equals( el.getTagName() ) && 
             el.getParentNode() instanceof TableElement ) {
            StarTable st = new VOStarTable( (TableElement) el.getParentNode() );
            handler_.writeDataElement( st );
        }

        /* Otherwise, process children. */
        else {
            String namespaceURI = el.getNamespaceURI();
            String localName = el.getLocalName();
            String qName = el.getTagName();
            doIndent();
            level_++;
            handler_.startElement( namespaceURI, localName, qName,
                                   getAttributes( el ) );
            int nchild = 0;
            for ( Node child = el.getFirstChild(); child != null; 
                  child = child.getNextSibling() ) {
                writeNode( child );
                nchild++;
            }
            level_--;
            if ( nchild > 0 ) {
                doIndent();
            }
            handler_.endElement( namespaceURI, localName, qName );
        }
    }

    /**
     * Indent as suitable for an element start/end tag.
     */
    private void doIndent() throws SAXException {
        if ( indent_ ) {
            char[] buf = new char[ level_ * 2 + 1 ];
            Arrays.fill( buf, ' ' );
            buf[ 0 ] = '\n';
            handler_.ignorableWhitespace( buf, 0, buf.length );
        }
    }

    /**
     * Returns a SAX Attributes object got by reading the attribute
     * children of a DOM Element.
     * 
     * @param   el  element
     * @return   attributes
     */
    private static Attributes getAttributes( Element el ) {
        AttributesImpl atts = new AttributesImpl();
        NamedNodeMap nmap = el.getAttributes();
        for ( int i = 0; i < nmap.getLength(); i++ ) {
            Attr att = (Attr) nmap.item( i );
            atts.addAttribute( att.getNamespaceURI(), att.getLocalName(),
                               att.getNodeName(), "CDATA", att.getValue() );
        }
        return atts;
    }

}
