package uk.ac.starlink.treeview;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import uk.ac.starlink.util.SourceReader;

public class XMLDataNode extends DefaultDataNode {

    protected final Node domNode;
    private final String name;
    private final String tla;
    private final String type;
    private final boolean allowsChildren;
    private final short iconId;
    private final String description;
    private Icon icon;
    private JComponent fullView;

    public static final int MAX_LINES = 12;

    public XMLDataNode( Node domNode ) {
        this( domNode, 
              domNode.getNodeName(), 
              "XML",
              "XML node",
              true,
              IconFactory.XML_ELEMENT,
              "" );
    }

    public XMLDataNode( Source xsrc ) throws NoSuchDataException {
        this( makeNode( xsrc ) );
    }

    protected XMLDataNode( Node domNode,
                           String name,
                           String tla,
                           String type,
                           boolean allowsChildren,
                           short iconId,
                           String description ) {
        this.domNode = domNode;
        this.name = name;
        this.tla = tla;
        this.type = type;
        this.allowsChildren = allowsChildren;
        this.iconId = iconId;
        this.description = description;
        setLabel( name );
    }

    public String getName() {
        return name;
    }

    public String getNodeTLA() {
        return tla;
    }

    public String getNodeType() {
        return type;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( iconId );
        }
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public boolean allowsChildren() {
        return allowsChildren;
    }

    public Iterator getChildIterator() {
        return new Iterator() {
            private Node next = firstUsefulSibling( domNode.getFirstChild() );
            private DataNodeFactory childMaker = getChildMaker();
            public boolean hasNext() {
                return next != null;
            }
            public Object next() {
                if ( next == null ) {
                    throw new NoSuchElementException();
                }
                Node nod = next;
                next = firstUsefulSibling( next.getNextSibling() );
                try {
                    return childMaker.makeDataNode( nod );
                }
                catch ( Exception e ) {
                    return new DefaultDataNode( e );
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
            addXMLViews( dv, domNode );
        }
        return fullView;
    }

    public static Node makeNode( Source xsrc ) throws NoSuchDataException {
        try {
            return new SourceReader().getDOM( xsrc );
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( e );
        }
    }

    /**
     * Add all the standard stuff for an XML node.
     */
    protected static void addXMLViews( DetailViewer dv, Node domNode ) {

        /* Namespaces. */
        String localName = domNode.getLocalName();
        String namespaceURI = domNode.getNamespaceURI();
        String prefix = domNode.getPrefix();
        if ( localName != null || namespaceURI != null || prefix != null ) {
            dv.addSubHead( "Namespaces" );
            if ( localName != null ) {
                dv.addKeyedItem( "Local name", localName );
            }
            if ( prefix != null ) {
                dv.addKeyedItem( "Prefix", prefix );
            }
            if ( namespaceURI != null ) {
                dv.addKeyedItem( "Namespace URI", namespaceURI );
            }
        }

        /* Add a limited number of lines of the content of the node. */
        dv.addSubHead( "Content" );
        Writer writer = dv.limitedLineAppender( MAX_LINES );
        SourceReader sr = new SourceReader();
        sr.setIncludeDeclaration( false );
        sr.setIndent( 0 );
        final Source xsrc = new DOMSource( domNode );
        try {
            sr.writeSource( xsrc, writer );
        }
        catch ( TransformerException e ) {
            dv.addText( e.getMessage() );
            e.printStackTrace();
        }
        finally {
            try {
                writer.close();
            }
            catch ( IOException e ) {}
        }

        /* Add a pane with the full content. */
        dv.addPane( "Full content", new ComponentMaker() {
            public JComponent getComponent() throws TransformerException {
                return new TextViewer( xsrc );
            }
        } );
    }

    /**
     * Takes a given node and returns it, or the first of its siblings
     * which is of interest to us.  Attribute nodes are ignored, as are
     * text nodes which contain only whitespace.
     * Also, adjacent text nodes will be concatenated.  Unlike
     * Node.normalize(), this takes care of CDATA nodes too.
     */
    private static Node firstUsefulSibling( Node sib ) {
        if ( sib == null ) {
            return null; 
        }
        else if ( sib instanceof Attr ) {
            return firstUsefulSibling( sib.getNextSibling() );
        }
        else if ( sib instanceof Text &&
                  ((Text) sib).getData().trim().length() == 0 ) {
            return firstUsefulSibling( sib.getNextSibling() );
        }
        else if ( sib instanceof Text && 
                  sib.getNextSibling() instanceof Text ) {
            StringBuffer buf = new StringBuffer();
            for ( Node tsib = sib; tsib instanceof Text;
                  tsib = tsib.getNextSibling() ) {
                buf.append( ((Text) tsib).getData() );
            }
            return sib.getOwnerDocument().createTextNode( buf.toString() );
        }
        else {
            return sib;
        }
    }             

}
