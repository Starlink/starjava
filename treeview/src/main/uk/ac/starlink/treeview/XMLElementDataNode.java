package uk.ac.starlink.treeview;

import java.io.*;
import java.util.*;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.jdom.*;
import org.jdom.output.XMLOutputter;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

/**
 * A {@link DataNode} representing an XML Element.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class XMLElementDataNode extends DefaultDataNode {

    public static final int MAX_LINES = 12;

    private Element el;
    private String name;
    private String description;
    private Icon icon;
    private JComponent fullView;

    /**
     * Initialises a new XMLElementDataNode from an Element object.
     *
     * @param  el  the Element from which the node will be constructed
     */
    public XMLElementDataNode( Element el ) {
        this.el = el;
        name = el.getName();
        setLabel( name );

        /* This is a bit adhoc, but will probably produce a more useful
         * description than none at all. */
        String nameAtt = el.getAttributeValue( "name" );
        String valueAtt = el.getAttributeValue( "value" );
        if ( nameAtt != null && valueAtt != null ) {
            description = nameAtt + "=\"" + valueAtt + "\"";
        }
        else if ( valueAtt != null ) {
            description = "\"" + valueAtt + "\"";
        }
        else if ( nameAtt != null ) {
            description = nameAtt;
        }
        else {
            description = "";
        }
    }

    public boolean allowsChildren() {
        return ( el.getContent().size() > 0 );
    }

    public Iterator getChildIterator() {

        /* Get the content of the Element. */
        List content = el.getContent();
        Iterator cit = content.iterator();

        /* Remove all the character data nodes which contain nothing but 
         * whitespace, since they clutter up the tree and you probably
         * don't need to see them. */
        while ( cit.hasNext() ) {
            Object ob = cit.next();
            if ( ob instanceof String ) {
                String s = (String) ob;
                if ( s.trim().length() == 0 ) {
                    try {
                        cit.remove();
                    }
                    catch ( UnsupportedOperationException e ) {
                        System.err.println( "Cannot remove whitespace nodes" );
                    }
                }
            }
        }
  
        /* Get a new iterator over the (possibly trimmed) List. */
        final Iterator lit = content.iterator();

        /* Return an iterator based on the content of this list. */
        return new Iterator() {
            public boolean hasNext() {
                return lit.hasNext();
            }
            public Object next() {
                Object ob = lit.next();
                try {
                    if ( ob instanceof Element ) {
                        return getChildMaker()
                              .makeDataNode( Element.class, (Element) ob );
                    }
                    else if ( ob instanceof ProcessingInstruction ) {
                        return new XMLProcessingInstructionDataNode(
                                     (ProcessingInstruction) ob );
                    }
                    else if ( ob instanceof Comment ) {
                        return new XMLCommentDataNode( (Comment) ob );
                    }
                    else if ( ob instanceof String ) {
                        return new XMLStringDataNode( (String) ob );
                    }
                    else if ( ob instanceof CDATA ) {
                        return new XMLCDATADataNode( (CDATA) ob );
                    }
                    else if ( ob instanceof EntityRef ) {
                        return new XMLEntityRefDataNode( (EntityRef) ob );
                    }
                    else {
                        throw new 
                            NoSuchDataException( "Unexpected XML node type" );
                    }
                }
                catch ( NoSuchDataException e ) {
                    return new DefaultDataNode( e );
                }
            }
            public void remove() {
                throw new UnsupportedOperationException( "No remove" );
            }
        };
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = iconMaker.getIcon( IconFactory.XML_ELEMENT );
        }
        return icon;
    }

    public String getNodeTLA() {
        return "ELE";
    }

    public String getNodeType() {
        return "XML element";
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {

        /* Construct a display view if it hasn't been done before. */
        if ( fullView == null ) {

            /* Set up the view panel. */
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();

            /* Write attribute information. */
            List atts = el.getAttributes();
            if ( atts.size() > 0 ) {

                /* Sort by attribute name. */
                SortedMap amap = new TreeMap();
                for ( int i = 0; i < atts.size(); i++ ) {
                    Attribute att = (Attribute) atts.get( i );
                    amap.put( att.getName(), att.getValue() );
                }
               
                /* Output the sorted attributes. */
                dv.addSubHead( "Attributes" );
                Iterator it = amap.entrySet().iterator();
                while ( it.hasNext() ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    dv.addKeyedItem( (String) entry.getKey(), 
                                     (String) entry.getValue() );
                }
            }
 
            /* Write namespace information. */
            Namespace space = el.getNamespace();
            if ( space != null && ! space.equals( Namespace.NO_NAMESPACE ) ) {
                dv.addSubHead( "Namespace" );
                dv.addKeyedItem( "Prefix", space.getPrefix() );
                dv.addKeyedItem( "URI", space.getURI() );
            }

            /* Write additional namespace information. */
            List nslist = el.getAdditionalNamespaces();
            if ( nslist.size() > 0 ) {
                dv.addSubHead( "Additional namespaces" );
                for ( int i = 0; i < nslist.size(); i++ ) {
                    space = (Namespace) nslist.get( i );
                    if ( i > 0 ) dv.addSeparator();
                    dv.addKeyedItem( "URI", space.getURI() );
                    dv.addKeyedItem( "Prefix", space.getPrefix() );
                }
            }

            /* Add a limited number of lines of the content of the element. */
            dv.addSubHead( "Content" );
            Writer writer = dv.limitedLineAppender( MAX_LINES );
            boolean incomplete = false;
            try {
                try {
                    new XMLOutputter().output( el, writer );
                }
                catch ( DetailViewer.MaxLinesWrittenException e ) {
                    dv.addText( "   ...." );
                    incomplete = true;
                }
                finally {
                    writer.close();
                }
            }
            catch ( IOException e ) {
                throw new Error( "Unexpected IOException" + e.getMessage() );
            }
            if ( incomplete ) {
                dv.addPane( "Full text", new ComponentMaker() {
                    public JComponent getComponent()
                            throws IOException, JDOMException,
                                   TransformerException {
                        org.w3c.dom.Element w3cel = 
                            new org.jdom.output.DOMOutputter().output( el );
                        return new TextViewer( new DOMSource( w3cel ) );
                    }
                } );
            }
        }
        return fullView;
    }
}
