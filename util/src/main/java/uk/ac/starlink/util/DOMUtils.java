package uk.ac.starlink.util;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.net.URI;

/**
 * Provides convenience methods for handling DOMs.
 */
public class DOMUtils {

    /** Private dummy sole constructor. */
    private DOMUtils() {}

    /** Maps node type codes to names.  Used by {@link #mapNodeType} */
    static private String[] nodeTypeMap;
    static {
        // The following appears nasty and errorprone, and vulnerable
        // to updates in the DOM spec.  However, it's not as bad as it
        // looks.  The DOM spec
        // <http://www.w3.org/TR/DOM-Level-2-Core> includes the
        // numerical values of the Node constants in the definition of
        // the Node interface, so they can't change.  The outside
        // possibility of change is held open within the spec, by the
        // statement reserving the first 200 such codes for use by
        // W3C, but if such a change were to come about, and affect
        // us, this is the least of the things which would have to
        // be modified.  It's possible to use a hash map to make this
        // completely general, but that gains us very little beyond
        // paranoid generality.
        //
        // See also nodeToMaskMap in NodeDescendants.java
        nodeTypeMap = new String[16];
        
        assert Node.ATTRIBUTE_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.ATTRIBUTE_NODE] = "Attribute";

        assert Node.CDATA_SECTION_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.CDATA_SECTION_NODE] = "CDATASection";

        assert Node.COMMENT_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.COMMENT_NODE] = "Comment";

        assert Node.DOCUMENT_FRAGMENT_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.DOCUMENT_FRAGMENT_NODE] = "DocumentFragment";

        assert Node.DOCUMENT_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.DOCUMENT_NODE] = "Document";

        assert Node.DOCUMENT_TYPE_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.DOCUMENT_TYPE_NODE] = "DocumentType";

        assert Node.ELEMENT_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.ELEMENT_NODE] = "Element";

        assert Node.ENTITY_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.ENTITY_NODE] = "Entity";

        assert Node.ENTITY_REFERENCE_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.ENTITY_REFERENCE_NODE] = "EntityReference";

        assert Node.NOTATION_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.NOTATION_NODE] = "Notation";

        assert Node.PROCESSING_INSTRUCTION_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.PROCESSING_INSTRUCTION_NODE]
                = "ProcessingInstruction";

        assert Node.TEXT_NODE < nodeTypeMap.length;
        nodeTypeMap[Node.TEXT_NODE] = "Text";
    }
    
    /**
     * Returns the first child element of a node which has a given name.
     *
     * @param  parent  the node whose children are to be searched
     * @param  name    the name of the element being searched for
     * @return the first child of <code>parent</code> which is
     *         an <code>Element</code> and has the tagname <code>name</code>,
     *         or <code>null</code> if none match
     */
    public static Element getChildElementByName( Node parent, String name ) {
        for ( Node child = parent.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Element ) {
                Element childEl = (Element) child;
                String childName = childEl.getTagName();
                if ( childName.equals( name ) ) {
                    return childEl;
                }
            }
        }
        return null;
    }

    /**
     * Returns all child elements of a node with a given name.
     *
     * @param  parent  the node whose children are to be searched
     * @param  name    the name of the element being searched for
     * @return  array of child elements of <code>parent</code> with tagname
     *          <code>name</code>;
     *          if <code>name</code> is null, all child elements are returned
     */
    public static Element[] getChildElementsByName( Node parent, String name ) {
        List<Element> els = new ArrayList<Element>();
        for ( Node child = parent.getFirstChild(); child != null;
              child  = child.getNextSibling() ) {
            if ( child instanceof Element ) {
                Element childEl = (Element) child;
                if ( name == null || name.equals( childEl.getTagName() ) ) {
                    els.add( childEl );
                }
            }
        }
        return els.toArray( new Element[ 0 ] );
    }

    /**
     * Returns a string representing the plain text content of an element.
     * Any comments, attributes, elements or other non-text children 
     * are ignored, and all CDATA and Text nodes are merged to 
     * give a single string.
     * 
     * @param   el  the element whose text content is wanted
     * @return  the pure text content.  If there is none, an empty 
     *          string is returned.
     */
    public static String getTextContent( Element el ) {
        StringBuffer sb = new StringBuffer();
        for ( Node child = el.getFirstChild(); child != null; 
              child = child.getNextSibling() ) {
            if ( child instanceof Text ) {
                Text childText = (Text) child;
                sb.append( childText.getData() );
            }
        }
        return sb.toString();
    }

    /**
     * Returns the first subsequent sibling of a given node which is an Element.
     * This is useful for naviating a DOM as a tree of elements when
     * the presence of text or attribute children is a distraction.
     *
     * @param  node  the node whose siblings (including itself) you are
     *         interested in.  May be <code>null</code>
     * @return the first sibling of <code>node</code> which is an Element.
     *         If <code>node</code> itself is an element, that is returned.
     *         If <code>node</code> has no subsequent siblings which are 
     *         elements, or if it is <code>null</code>,
     *         then <code>null</code> is returned.
     */
    public static Element getFirstElementSibling( Node node ) {
        return ( node == null || node instanceof Element ) 
             ? (Element) node
             : getFirstElementSibling( node.getNextSibling() );
    }

    /**
     * Traverses the given DOM, relativising all the URIs in the
     * <code>uri</code> attributes of each <code>Element</code>.
     *
     * <p>The (uri-attribute) nodes in the input DOM are modified by this
     * method; if this is a problem, use {@link
     * org.w3c.dom.Node#cloneNode} first.
     *
     * @param n a node containing the DOM whose URIs are to be
     * relativized.  If this is null, the method immediately returns null
     * @param baseURI the URI relative to which the DOM is to be
     * relativised.  If this is null, then the input node is
     * immediately returned unchanged.
     * @param attname the attribute name to be used.  If null, this
     * defaults to <code>uri</code>
     * @return the input node
     * @see java.net.URI#relativize
     */
    public static Node relativizeDOM(Node n, URI baseURI, String attname) {
        if (n == null || baseURI == null)
            return n;
        if (attname == null)
            attname = "uri";
        NamedNodeMap nm = n.getAttributes();
        if (nm != null)
            for (int i=0; i<nm.getLength(); i++) {
                Attr att = (Attr)nm.item(i);
                if (att.getName().equals(attname)) {
                    String oldAttValue = att.getValue();
                    try {
                        att.setValue
                                (baseURI.relativize(new URI(oldAttValue))
                                 .toString());
                    } catch (java.net.URISyntaxException ex) {
                        // Malformed URI -- restore the attribute to its original value
                        att.setValue(oldAttValue);
                    }
                }
            }
        for (Node kid=n.getFirstChild(); kid!=null; kid=kid.getNextSibling())
            relativizeDOM(kid, baseURI, attname);
        return n;
    }

    /**
     * Maps a node type, as returned by to a name.
     * The node types returned by {@link Node#getNodeType()} are
     * numeric and are therefore inconveniently opaque.
     *
     * @param nodeType a numeric Node type, one of the node type
     * constants defined in <code>Node</code>
     * @return a string name for the type
     */
    static public String mapNodeType(short nodeType) {
        // Mostly for debugging -- the numeric node types are pretty
        // useless in any sort of log message.  Yes, this _is_
        // more elaborate than you'd guess it'd have to be, and no,
        // there's no other way to debug node types other than by grubbing
        // through org.w3c.dom.Node.java
        assert nodeType < nodeTypeMap.length;
        String val = nodeTypeMap[nodeType];
        if (val == null)
            val = "UNKNOWN!!!";
        return val;
    }

    /**
     * Returns a new <code>Document</code> instance.
     * This method just does all the tedious business of mucking about
     * with factories for you.
     *
     * @return   an empty Document
     */
    public static Document newDocument() {
        try {
            return DocumentBuilderFactory
                  .newInstance()
                  .newDocumentBuilder()
                  .newDocument();
        }
        catch ( ParserConfigurationException e ) {
            throw new RuntimeException( "Unexpected error constructing "
                                      + "default document factory", e );
        }
    }
}
