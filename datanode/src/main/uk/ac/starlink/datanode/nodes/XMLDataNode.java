package uk.ac.starlink.datanode.nodes;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Notation;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import uk.ac.starlink.datanode.viewers.TextViewer;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.SourceReader;

public class XMLDataNode extends DefaultDataNode {

    private final Node domNode;
    private final String systemId;
    private final String tla;
    private final String type;
    private final String description;
    private final boolean allowsChildren;
    private String sep;
    private String pathEl;
    private String name;

    public static final int MAX_LINES = 12;

    private static final String NAME_KEY = "Name";
    private static final String SID_KEY = "System ID";
    private static final String PID_KEY = "Public ID";
    private static final String NOTATION_KEY = "Notation";

    public XMLDataNode( Source xsrc ) throws NoSuchDataException {
        try {
            this.domNode = new SourceReader().getDOM( xsrc );
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( e );
        }
        this.systemId = xsrc.getSystemId();

        name = domNode.getNodeName();
        pathEl = domNode.getNodeName();
        sep = ".";
        short iconId;
        switch ( domNode.getNodeType() ) {

            case Node.DOCUMENT_NODE:
                tla = "DOC";
                type = "XML document";
                allowsChildren = true;
                iconId = IconFactory.XML_DOCUMENT;
                name = '<' + ((Document) domNode)
                            .getDocumentElement().getTagName() + '>';
                description = "";
                sep = "#";
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

            case Node.DOCUMENT_TYPE_NODE:
                tla = "DTD";
                type = "Document Type Declaration";
                allowsChildren = true;
                iconId = IconFactory.XML_DTD;
                DocumentType dtd = (DocumentType) domNode;
                name = "<!DOCTYPE " + dtd.getName() + ">";
                description = "";
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
        if ( pathEl.charAt( 0 ) == '#' ) {
            pathEl = '[' + pathEl.substring( 1 ) + ']';
        }
        setLabel( name );
        setIconID( iconId );
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

    public String getPathElement() {
        if ( domNode.getNodeType() == Node.DOCUMENT_NODE ) {
            return getLabel();
        }
        else {
            return pathEl;
        }
    }

    public String getPathSeparator() {
        return sep;
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

    public void configureDetail( DetailViewer dv ) {

        /* Write system ID if there is one. */
        if ( systemId != null && systemId.trim().length() > 0 ) {
            dv.addKeyedItem( "System ID", systemId );
        }

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

        /* DTD specific things. */
        if ( domNode instanceof DocumentType ) {
            DocumentType dtd = (DocumentType) domNode;

            /* Identifiers. */
            dv.addKeyedItem( "System ID", dtd.getSystemId() );
            dv.addKeyedItem( "Public ID", dtd.getPublicId() );

            /* Internal subset. */
            final String isubset = dtd.getInternalSubset();
            if ( isubset != null && isubset.trim().length() > 0 ) {
                dv.addPane( "Internal subset", new ComponentMaker() {
                    public JComponent getComponent() 
                            throws TransformerException {
                        Source ssrc = 
                            new StreamSource( new StringReader( isubset ) );
                        ssrc.setSystemId( systemId );
                        return new TextViewer( ssrc );
                    }
                } );
            }

            /* Entities. */
            final NamedNodeMap entities = dtd.getEntities();
            final int nent = entities.getLength();
            if ( nent > 0 ) {
                dv.addPane( "Entities", new ComponentMaker() {
                    public JComponent getComponent() {
                        MetamapGroup mmg = new MetamapGroup( nent );
                        for ( int i = 0; i < nent; i++ ) {
                            Entity ent = (Entity) entities.item( i );
                            mmg.addEntry( i, NAME_KEY, ent.getNodeName() );
                            mmg.addEntry( i, SID_KEY, ent.getSystemId() );
                            mmg.addEntry( i, PID_KEY, ent.getPublicId() );
                            mmg.addEntry( i, NOTATION_KEY, 
                                          ent.getNotationName() );
                        }
                        mmg.setKeyOrder( Arrays.asList( new String[] {
                            NAME_KEY, SID_KEY, PID_KEY, NOTATION_KEY,
                        } ) );
                        return new MetaTable( mmg );
                    }
                } );
            }

            /* Notations. */
            final NamedNodeMap notations = dtd.getNotations();
            final int nnot = notations.getLength();
            if ( nnot > 0 ) {
                dv.addPane( "Notations", new ComponentMaker() {
                    public JComponent getComponent() {
                        MetamapGroup mmg = new MetamapGroup( nnot );
                        for ( int i = 0; i < nnot; i++ ) {
                            Notation not = (Notation) notations.item( i );
                            String systemId = not.getSystemId();
                            String publicId = not.getPublicId();
                            mmg.addEntry( i, NAME_KEY, not.getNodeName() );
                            mmg.addEntry( i, SID_KEY, not.getSystemId() );
                            mmg.addEntry( i, PID_KEY, not.getPublicId() );
                        }
                        mmg.setKeyOrder( Arrays.asList( new String[] {
                            NAME_KEY, SID_KEY, PID_KEY,
                        } ) );
                        return new MetaTable( mmg );
                    }
                } );
            }
        }

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

        /* Add a pane with the full content. */
        dv.addPane( "Full content", new ComponentMaker() {
            public JComponent getComponent() throws TransformerException {
                return new TextViewer( new DOMSource( domNode ) );
            }
        } );
    }


    private static String contentSummary( String pre, String data, 
                                          String post ) {
        StringBuffer summ = new StringBuffer();
        summ.append( pre );
        String content = data.trim();
        if ( content.length() == 0 ) {
            return "";
        }
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

        /* How about a name on its own. */
        else if ( el.hasAttribute( "name" ) ) {
            desc = '"' + el.getAttribute( "name" ) + '"';
        }

        /* Try content, if it's short. */
        else if ( nchild - natts == 1 &&
                  el.getChildNodes().item( 0 ) instanceof Text ) {
            Text tnode = (Text) el.getChildNodes().item( 0 );
            String val = tnode.getData().trim();
            if ( val.length() > 0 ) {
                desc = '"' + 
                val.substring( 0, Math.min( 80, val.length() ) ) + '"';
            }
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
    static Node firstUsefulSibling( Node sib ) {
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
