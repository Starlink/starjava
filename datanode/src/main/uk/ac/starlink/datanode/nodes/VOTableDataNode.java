package uk.ac.starlink.datanode.nodes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.factory.VODataNodeBuilder;
import uk.ac.starlink.datanode.viewers.StyledTextArea;
import uk.ac.starlink.datanode.viewers.TextViewer;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;

/**
 * Node representing a top-level VOTable document.
 */
public class VOTableDataNode extends DocumentDataNode {

    private String name;
    private VOElement vocel;
    private static VOElementFactory vofact = new VOElementFactory();

    public VOTableDataNode( XMLDocument xdoc ) throws NoSuchDataException {
        super( xdoc );
        checkTopLocalName( "VOTABLE" );
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

    private VOElement getVOElement() throws IOException, SAXException {
        if ( vocel == null ) {
            vocel = vofact.makeVOElement( getDocument() );
        }
        return vocel;
    }

    public Iterator getChildIterator() {
        List children = new ArrayList();
        try {
            for ( Iterator it = VOComponentDataNode
                               .getChildElements( getVOElement() ).iterator(); 
                  it.hasNext(); ) {
                VOElement el = (VOElement) it.next();
                Source xsrc = new DOMSource( el, el.getSystemId() );
                children.add( makeChild( xsrc ) );
            }
        }
        catch ( IOException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
        catch ( SAXException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
        return children.iterator();
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
                VOElement voel;
                try {
                    voel = getVOElement();
                }
                catch ( IOException e ) {
                    return new TextViewer( e );
                }
                catch ( SAXException e ) {
                    return new TextViewer( e );
                }
                StyledTextArea sta = new StyledTextArea();
                sta.setWrap( true );

                /* Deal with any DESCRIPTION. */
                String description = voel.getDescription();
                if ( description != null ) {
                    sta.addSubHead( "Description" );
                    sta.addText( description );
                }

                /* Deal with any INFOs. */
                VOElement[] infos = voel.getChildrenByName( "INFO" );
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
