package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.votable.VOElement;

/**
 * Node representing a top-level VOTable document.
 */
public class VOTableDataNode extends DocumentDataNode {

    private String name;

    public VOTableDataNode( XMLDocument xdoc ) throws NoSuchDataException {
        super( xdoc );
        String localName = xdoc.getTopLocalName();
        String namespaceURI = xdoc.getTopNamespaceURI();
        if ( localName != "VOTABLE" ) {
             throw new NoSuchDataException( "Document type is " + localName +
                                            " not VOTABLE" );
        }
        if ( namespaceURI != null && namespaceURI.trim().length() > 0 ) {
             throw new NoSuchDataException( "VOTABLE namespace is " + 
                                            namespaceURI + " not null" );
        }
        setIconID( IconFactory.VOTABLE );
    }

    public String getNodeTLA() {
        return "VOT";
    }

    public String getNodeType() {
        return "VOTable";
    }

    public boolean allowsChildren() {
        return true;
    }

    public String getPathSeparator() {
        return "#";
    }

    public Iterator getChildIterator() {
        try {
            Element votel = ((Document) getDocument().getNode())
                           .getDocumentElement();
            final Iterator baseIt = getChildIterator( votel );
            return new Iterator() {
                private DataNode next = nextUseful();
                public boolean hasNext() {
                    return next != null;
                }
                public Object next() {
                    if ( next == null ) {
                        throw new NoSuchElementException();
                    }
                    DataNode item = next;
                    next = nextUseful();
                    return item;
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                public DataNode nextUseful() {
                    if ( ! baseIt.hasNext() ) {
                        return null;
                    }
                    DataNode item = (DataNode) baseIt.next();
                    if ( item.getClass().equals( VOComponentDataNode.class ) ) {
                        Element el = ((VOComponentDataNode) item).getElement();
                        String name = el.getTagName();
                        if ( name.equals( "DESCRIPTION" ) ||
                             name.equals( "INFO" ) ) {
                            return nextUseful();
                        }
                    }
                    return item;
                }
            };
        }
        catch ( IOException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
        catch ( SAXException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
    }

    public void configureDetail( DetailViewer dv ) {
        super.configureDetail( dv );

        /* Add a new pane to contain miscellaneous document-level metadata.
         * We don't want to put this in the main display summary since it
         * requires a full parse, which we don't want to do unless extra
         * information has been required.  They could just go as children
         * of the document element, but some things (DESCRIPTION, INFOs)
         * are a bit too insignificant to warrant that. */
        dv.addPane( "Document Metadata", new ComponentMaker() {
            public JComponent getComponent() {
                Document doc;
                DOMSource docsrc;
                try {
                    docsrc = getDocument();
                }
                catch ( IOException e ) {
                    return new TextViewer( e );
                }
                catch ( SAXException e ) {
                    return new TextViewer( e );
                }
                StyledTextArea sta = new StyledTextArea();
                sta.setWrap( true );

                /* Get VOElement corresponding to VOTABLE element. */
                DOMSource dsrc = 
                    new DOMSource( ((Document) docsrc.getNode())
                                  .getDocumentElement(),
                                   docsrc.getSystemId() );
                VOElement vocel = VOElement.makeVOElement( dsrc );

                /* Deal with any DESCRIPTION. */
                String description = vocel.getDescription();
                if ( description != null ) {
                    sta.addSubHead( "Description" );
                    sta.addText( description );
                }

                /* Deal with any INFOs. */
                VOElement[] infos = vocel.getChildrenByName( "INFO" );
                if ( infos.length > 0 ) {
                    sta.addSubHead( "Infos" );
                    for ( int i = 0; i < infos.length; i++ ) {
                        String name = infos[ i ].getAttribute( "name" );
                        String value = infos[ i ].getAttribute( "value" );
                        if ( name != null ) {
                            sta.addKeyedItem( name, value != null ? value 
                                                                  : "null" );
                        }
                    }
                }

                /* Return text component. */
                return sta;
            }
        } );
    }

    public DataNodeFactory getChildMaker() {
        DataNodeFactory dfact = super.getChildMaker();
        if ( ! ( dfact.getBuilders().get( 0 ) instanceof VODataNodeBuilder ) ) {
            dfact = new DataNodeFactory( dfact );
            dfact.getBuilders().add( 0, new VODataNodeBuilder() );
        }
        return dfact;
    }
}
