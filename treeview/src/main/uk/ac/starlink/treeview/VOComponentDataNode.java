package uk.ac.starlink.treeview;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import uk.ac.starlink.treeview.votable.GenericElement;
import uk.ac.starlink.treeview.votable.Param;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;

/**
 * Generic node for representing VOTable elements.
 */
public class VOComponentDataNode extends DefaultDataNode {

    protected final Element vocel;
    protected final String systemId;
    private String name;
    private JComponent fullView;
    private Icon icon;

    public VOComponentDataNode( Source xsrc ) throws NoSuchDataException {
        Node domNode;
        try {
            domNode = new SourceReader().getDOM( xsrc );
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( e );
        }
        if ( domNode instanceof Element ) {
            vocel = (Element) domNode;
        }
        else if ( domNode instanceof Document ) {
            vocel = ((Document) domNode).getDocumentElement();
            if ( vocel.getTagName() != "VOTABLE" ) {
                throw new NoSuchDataException( "Document is not VOTABLE" );
            }
        }
        else {
            throw new NoSuchDataException( "Source is not an element" );
        }
        this.systemId = xsrc.getSystemId();

        String idval = vocel.getAttribute( "ID" );
        String nameval = vocel.getAttribute( "name" );
        if ( nameval.length() > 0 ) {
            name = nameval;
        }
        else if ( idval.length() > 0 ) {
            name = idval;
        }
        else {
            name = vocel.getTagName();
        }
        setLabel( name );
    }

    protected VOComponentDataNode( Source xsrc, String elname )
            throws NoSuchDataException {
        this( xsrc );
        if ( ! vocel.getTagName().equals( elname ) ) {
            throw new NoSuchDataException( "Not a " + elname + " element" );
        }
    }

    public String getName() {
        return name;
    }

    public String getNodeTLA() {
        return "VOC";
    }

    public String getNodeType() {
        return "VOTable component";
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( getIconId() );
        }
        return icon;
    }

    protected short getIconId() {
        return IconFactory.VOCOMPONENT;
    }

    public String getDescription() {
        return "";
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        return new Iterator() {
            private DataNodeFactory childMaker = getChildMaker();
            private Node next = firstUsefulSibling( vocel.getFirstChild() );
            public boolean hasNext() {
                return next != null;
            }
            public Object next() {
                if ( next == null ) {
                    throw new NoSuchElementException();
                }
                Node nd = next;
                next = firstUsefulSibling( nd.getNextSibling() );
                try {
                    Source xsrc = new DOMSource( nd, systemId );
                    return childMaker.makeDataNode( xsrc );
                }
                catch ( Exception e ) {
                    return new ErrorDataNode( e );
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addKeyedItem( "Element name", vocel.getTagName() );
            addVOComponentViews( dv, vocel );
        }
        return fullView;
    }

    public static void addVOComponentViews( DetailViewer dv, 
                                            final Element el ) {

        /* Collect useful children. */
        Map atts = new TreeMap();
        String description = null;
        List infonames = new ArrayList();
        List infovals = new ArrayList();
        List params = new ArrayList();
        int ninfo = 0;
        for ( Node child = el.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Attr ) {
                Attr att = (Attr) child;
                atts.put( att.getName(), att.getValue() );
            }
            else if ( child instanceof Element ) {
                Element childEl = (Element) child;
                String elname = childEl.getTagName();
                if ( elname.equals( "DESCRIPTION" ) ) {
                    description = DOMUtils.getTextContent( childEl ).trim();
                }
                else if ( elname.equals( "INFO" ) ) {
                    String infohandle = new GenericElement( el ).getHandle();
                    String infovalue = el.getAttribute( "value" );
                    infonames.add( infohandle );
                    infovals.add( infovalue );
                    ninfo++;
                }
                else if ( elname.equals( "PARAM" ) ) {
                    params.add( new Param( childEl ) );
                }
            }
        }

        /* Attributes. */
        if ( atts.size() > 0 ) {
            dv.addSubHead( "Attributes" );
            for ( Iterator it = atts.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry att = (Map.Entry) it.next();
                dv.addKeyedItem( (String) att.getKey(),
                                 (String) att.getValue() );
            }
        }

        /* Description. */
        if ( description != null && description.length() > 0 ) {
            dv.addSubHead( "Description" );
            dv.addText( description );
        }

        /* Infos. */
        if ( ninfo > 0 ) {
            dv.addSubHead( "Infos" );
            for ( int i = 0; i < ninfo; i++ ) {
                dv.addKeyedItem( (String) infonames.get( i ),
                                 (String) infovals.get( i ) );
            }
        }

        /* Params. */
        if ( params.size() > 0 ) {
            dv.addSubHead( "Params" );
            for ( Iterator it = params.iterator(); it.hasNext(); ) {
                Param param = (Param) it.next();
                String val = param.getValue();
                String unit = param.getUnit();
                if ( unit != null ) {
                    val += " " + unit;
                }
                dv.addKeyedItem( param.getHandle(), val );
            }
        }

        /* XML view. */
        dv.addPane( "XML content", new ComponentMaker() {
            public JComponent getComponent() throws TransformerException {
                return new TextViewer( new DOMSource( el ) );
            }
        } );
    }

    public DataNodeFactory getChildMaker() {
        DataNodeFactory dfact = new DataNodeFactory();
        dfact.setPreferredBuilder( new VODataNodeBuilder() );
        return dfact;
    }

 
    /**
     * Takes a given node and returns it, or the first of its siblings which
     * is of interest to this class.  To count as useful, it must be an
     * Element, and must not have the of one of the generic VOTable 
     * components which are treated internally.
     * These are currently INFO, PARAM and DESCRIPTION.
     */
    private static Node firstUsefulSibling( Node sib ) {
        if ( sib == null ) {
            return null;
        }
        if ( ! ( sib instanceof Element ) ) {
            return firstUsefulSibling( sib.getNextSibling() );
        }
        String elname = ((Element) sib).getTagName();
        if ( elname.equals( "DESCRIPTION" ) ||
             elname.equals( "INFO" ) ||
             elname.equals( "PARAM" ) ) {
            return firstUsefulSibling( sib.getNextSibling() );
        }
        return sib;
    }
}
