package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import uk.ac.starlink.util.SourceReader;

public class XMLDataNode extends DefaultDataNode {

    private final Node domNode;
    private final String systemId;
    private final String tla;
    private final String type;
    private final String description;
    private final boolean allowsChildren;
    private final short iconId;
    private String name;
    private JComponent fullView;
    private Icon icon;

    public static final int MAX_LINES = 12;

    public XMLDataNode( Source xsrc ) throws NoSuchDataException {
        try {
            this.domNode = new SourceReader().getDOM( xsrc );
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( e );
        }
        this.systemId = xsrc.getSystemId();

        name = domNode.getNodeName();
        switch ( domNode.getNodeType() ) {

            case Node.DOCUMENT_NODE:
                tla = "DOC";
                type = "XML document";
                allowsChildren = true;
                iconId = IconFactory.XML_DOCUMENT;
                name = '<' + ((Document) domNode)
                            .getDocumentElement().getTagName() + '>';
                description = "";
                break;

            case Node.ELEMENT_NODE:
                tla = "ELE";
                type = "XML element";
                allowsChildren = true;
                iconId = IconFactory.XML_ELEMENT;
                Element el = (Element) domNode;
                name = '<' + ( ( el.getLocalName() == null ) ? el.getTagName()
                                                     : el.getLocalName() )
                           + '>';
                description = elementDescription( el );
                break;

            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                tla = "TXT";
                type = "XML text node";
                allowsChildren = false;
                iconId = IconFactory.XML_STRING;
                name = "";
                description = contentSummary( "\"", ((Text) domNode).getData(),
                                              "\"" );
                break;

            case Node.COMMENT_NODE:
                tla = "COM";
                type = "XML comment node";
                allowsChildren = false;
                iconId = IconFactory.XML_COMMENT;
                name = "";
                description = contentSummary( "<!-- ", 
                                              ((Comment) domNode).getData(),
                                              " -->" );
                break;

            case Node.PROCESSING_INSTRUCTION_NODE:
                tla = "XPI";
                type = "XML processing instruction";
                allowsChildren = false;
                iconId = IconFactory.XML_PI;
                ProcessingInstruction pi = (ProcessingInstruction) domNode;
                name = "<?" + pi.getTarget() + "?>";
                description = contentSummary( "\"", pi.getData(), "\"" );
                break;

            default:
                tla = "XML";
                type = "XML node";
                allowsChildren = true;
                iconId = IconFactory.XML_ELEMENT;
                name = domNode.getNodeName();
                description = "";
                break;
        }
        setLabel( name );
    }

    public XMLDataNode( File file ) throws NoSuchDataException {
        this( new StreamSource( file ) );
        this.name = file.getName();
        setLabel( name );
    }

    public XMLDataNode( String loc ) throws NoSuchDataException {
        this( new File( loc ) );
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
        if ( icon == null ){
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
                    Source xsrc = new DOMSource( nod, systemId );
                    return childMaker.makeDataNode( xsrc );
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

            /* If we are an element with attributes, write them. */
            if ( domNode instanceof Element ) {
                Element el = (Element) domNode;
                if ( el.hasAttributes() ) {

                    /* Assemble a list sorted by attribute name. */
                    NamedNodeMap atts = el.getAttributes();
                    int natt = atts.getLength();
                    SortedMap amap = new TreeMap();
                    for ( int i = 0; i < natt; i++ ) {
                        Attr att = (Attr) atts.item( i );
                        String aname = att.getName();
                        String aval = att.getValue();
                        if ( ! att.getSpecified() ) {
                            aval += " (auto value)";
                        }
                        amap.put( aname, aval );
                    }

                    /* Output the sorted attributes. */
                    dv.addSubHead( "Attributes" );
                    for ( Iterator it = amap.entrySet().iterator();
                          it.hasNext(); ) {
                        Map.Entry entry = (Map.Entry) it.next();
                        dv.addKeyedItem( (String) entry.getKey(),
                                         (String) entry.getValue() );
                    }
                }
            }

            /* Generic XML info. */
            addXMLViews( dv, domNode );
        }
        return fullView;
    }


    public static boolean isMagic( byte[] magic ) {
        int pos;

        /* UTF-8. */
        pos = 0;
        if ( magic.length > 5 && 
             magic[ pos++ ] == (byte) '<' &&
             magic[ pos++ ] == (byte) '?' &&
             magic[ pos++ ] == (byte) 'x' &&
             magic[ pos++ ] == (byte) 'm' &&
             magic[ pos++ ] == (byte) 'l' ) {
            return true;
        }

        /* Big endian UCS-2. */
        pos = 0;
        if ( magic.length > 12 &&
             magic[ pos++ ] == 0xfe && magic[ pos++ ] == 0xff &&
             magic[ pos++ ] == 0x00 && magic[ pos++ ] == (byte) '<' &&
             magic[ pos++ ] == 0x00 && magic[ pos++ ] == (byte) '?' &&
             magic[ pos++ ] == 0x00 && magic[ pos++ ] == (byte) 'x' &&
             magic[ pos++ ] == 0x00 && magic[ pos++ ] == (byte) 'm' &&
             magic[ pos++ ] == 0x00 && magic[ pos++ ] == (byte) 'l' ) {
            return true;
        }

        /* Little endian UCS-2. */
        pos = 0;
        if ( magic.length > 12 && 
             magic[ pos++ ] == 0xff && magic[ pos++ ] == 0xfe &&
             magic[ pos++ ] == (byte) '<' && magic[ pos++ ] == 0x00 &&
             magic[ pos++ ] == (byte) '?' && magic[ pos++ ] == 0x00 &&
             magic[ pos++ ] == (byte) 'x' && magic[ pos++ ] == 0x00 &&
             magic[ pos++ ] == (byte) 'm' && magic[ pos++ ] == 0x00 &&
             magic[ pos++ ] == (byte) 'l' && magic[ pos++ ] == 0x00 ) {
            return true;
        }

        /* Nope. */
        return false;
    }


    private static String contentSummary( String pre, String data, 
                                          String post ) {
        StringBuffer summ = new StringBuffer();
        summ.append( pre );
        String content = data.trim();
        if ( content.length() <= 30 ) {
            summ.append( content );
            summ.append( post );
        }
        else {
            summ.append( content.substring( 0, 30 ) );
            summ.append( "..." );
        }
        return summ.toString();
    }


    /**
     * Provide ad-hoc descriptive text for an element.
     * Just intended to be sortof more informative than not having it.
     */
    private static String elementDescription( Element el ) {
        String desc = "";
        boolean hasAtts = el.hasAttributes();
        int natts = hasAtts ? el.getAttributes().getLength() : 0;
        int nchild = el.getChildNodes().getLength();
        boolean isEmpty = ( nchild - natts ) == 0;

        /* Empty element and one attribute, use that. */
        if ( isEmpty && natts == 1 ) {
            Attr att = (Attr) el.getAttributes().item( 0 );
            desc = att.getName() + "=\"" + att.getValue() + "\"";
        }

        /* Try name/value pairs. */
        else if ( el.hasAttribute( "name" ) && el.hasAttribute( "value" ) ) {
             desc = el.getAttribute( "name" ) + "=\""
                  + el.getAttribute( "value" ) + "\"";
        }

        /* Try content, if it's short. */
        else if ( nchild - natts == 1 &&
                  el.getChildNodes().item( 0 ) instanceof Text ) {
            Text tnode = (Text) el.getChildNodes().item( 0 );
            String val = tnode.getData().trim();
            desc = '"' + val.substring( 0, Math.min( 80, val.length() ) ) + '"';
        }

        if ( desc.length() > 42 ) {
            desc = desc.substring( 0, 40 ) + "...";
        }
        return desc;
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

}
