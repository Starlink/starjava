package uk.ac.starlink.treeview;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.hdx.HdxContainer;
import uk.ac.starlink.hdx.HdxException;
import uk.ac.starlink.hdx.HdxFactory;
import uk.ac.starlink.hdx.HdxResourceType;
import uk.ac.starlink.ndx.BridgeNdx;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.URLUtils;

/**
 * DataNode representing an HDX object.
 */
public class HDXDataNode extends DefaultDataNode {

    private final HdxContainer hdx;
    private URI baseUri;
    private String systemId;
    private String name;
    private Element hdxel;

    /*
     * Static initialiser registers all HDX-registerable elements that
     * we know about.
     */
    static {
        BridgeNdx.class.getName();
    }

    /**
     * Constructs an HDXDataNode from an XML Source.
     *
     * @param  xsrc  the Source
     */
    public HDXDataNode( Source xsrc ) throws NoSuchDataException {
        Element el;
        try {
            Node node = new SourceReader().getDOM( xsrc );
            if ( node instanceof Element ) {
                el = (Element) node;
            }
            else if ( node instanceof Document ) {
                el = ((Document) node).getDocumentElement();
            }
            else {
                throw new NoSuchDataException( "XML Source is not an element" );
            }
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( e );
        }
        systemId = xsrc.getSystemId();
        if ( systemId != null ) {
            try {
                URL url = URLUtils.makeURL( systemId );
                baseUri = new URI( url.toString() );
            }
            catch ( URISyntaxException e ) {
                baseUri = null;
            }
        }
        try {
            hdx = HdxFactory.getInstance().newHdxContainer( el, baseUri );
        }
        catch ( HdxException e ) {
            throw new NoSuchDataException( e );
        }
        if ( hdx == null ) {
            throw new NoSuchDataException( "No unique HDX in source" );
        }
        hdxel = hdx.getDOM( baseUri );
        Object title = hdx.get( HdxResourceType.TITLE );
        if ( name == null && title != null ) {
            name = title.toString();
        }
        if ( name == null && baseUri != null ) {
            name = baseUri.toString().replaceFirst( "#.*$", "" )
                                     .replaceFirst( "^.*/", "" );
        }
        if ( name == null ) {
            name = el.getTagName();
        }
        setLabel( name );
        setIconID( IconFactory.HDX_CONTAINER );
    }

    /**
     * Constructs an HDXDataNode from a String.  The string may represent
     * a URL or filename.
     *
     * @param  loc  the location of the HDX
     */
    public HDXDataNode( String loc ) throws NoSuchDataException {
        URL url = URLUtils.makeURL( loc );
        systemId = loc;
        try {
            hdx = HdxFactory.getInstance().newHdxContainer( url );
        }
        catch ( HdxException e ) {
            throw new NoSuchDataException( e );
        }
        if ( hdx == null ) {
            throw new NoSuchDataException( "No handler for " + loc );
        }
        try {
            baseUri = new URI( loc );
        }
        catch ( URISyntaxException e ) {
            baseUri = null;
        }
        hdxel = hdx.getDOM( baseUri );
        Object title = hdx.get( HdxResourceType.TITLE );
        if ( name == null && title != null ) {
            name = title.toString();
        }
        if ( name == null ) {
            name = loc.replaceFirst( "#.*$", "" )
                      .replaceFirst( "^.*/", "" );
        }
        setLabel( name );
    }

    /**
     * Constructs an HDXDataNode from a File.
     */
    public HDXDataNode( File file ) throws NoSuchDataException {
        this( file.toString() );
    }


    public String getName() {
        return name;
    }

    public String getNodeTLA() {
        return "HDX";
    }

    public String getNodeType() {
        return "HDX container";
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        return new Iterator() {
            Node next = XMLDataNode.firstUsefulSibling( hdxel.getFirstChild() );
            public boolean hasNext() {
                return next != null;
            }
            public Object next() {
                if ( next == null ) {
                    throw new NoSuchElementException();
                }
                Node nod = next;
                next = XMLDataNode.firstUsefulSibling( next.getNextSibling() );
                try {
                    Source xsrc = new DOMSource( nod, systemId );
                    return makeChild( xsrc );
                }
                catch ( Exception e ) {
                    return makeErrorChild( e );
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Provide a custom DataNodeFactory for making children.  An HDX
     * should never be found inside an HDX, although the HDX library is
     * capable of interpreting structures in this way when used in the
     * way that Treeview uses it.  Here we remove the possibility that
     * an HDXDataNode can have an HDXDataNode as a child node.
     */
    public DataNodeFactory getChildMaker() {
        DataNodeFactory dfact = new DataNodeFactory( super.getChildMaker() );
        dfact.removeNodeClass( HDXDataNode.class );
        return dfact;
    }

    public void configureDetail( DetailViewer dv ) {
        if ( baseUri != null ) {
            dv.addKeyedItem( "Base URI", baseUri );
        }
        dv.addPane( "XML view", new ComponentMaker() {
            public JComponent getComponent() {
                return new TextViewer( new DOMSource( hdxel ) );
            }
        } );
    }


}
