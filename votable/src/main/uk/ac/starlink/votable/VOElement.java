package uk.ac.starlink.votable;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.URLUtils;

/**
 * Generic element in a VOTable document.  This class is extended to
 * provide the classes associated with specific VOTable elements.
 * It provides handling for some of the characteristics common to
 * various VOTable elements, for instance DESCRIPTION elements.
 *
 * <p>This element and its subclasses provide some methods for returning
 * the values of specific attributes from the element in question, such as
 * <tt>getName</tt>, <tt>getID</tt>.  This is either for convenience
 * or in order to return some non-String object from these attributes.
 * For attribute values which are not covered by specific methods in
 * this way, the {@link #getAttribute} method can be used.
 *
 * <p>The recommended way of navigating round a VOTable document tree 
 * is by using the <tt>getChild*</tt> and <tt>getParent</tt> methods
 * which return other VOElement objects.  If you like doing things
 * the hard way however, you can access the DOM {@link org.w3c.dom.Element}
 * underlying each VOElement and navigate round the underlying DOM 
 * structure itself, creating a new VOElement when you get to the 
 * node you're after so you can use the methods provided by VOElement
 * (such as <tt>TableElement</tt>'s {@link TableElement#getData} method).
 *
 * <p>You should in general get instances of VOElement using the
 * {@link VOElementFactory} class.
 *
 * <p><i>Note: An alternative implementation would have VOElement 
 * implement the {@link org.w3c.dom.Element} interface itself.
 * There are a couple of reasons this would be a bit problematic, but
 * is probably doable.  If anyone can persuade me it's a useful thing
 * to do, I might go ahead).</i>
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOElement {

    private final Element el;
    private final String systemId;
    private URL context;
    private String id;
    private String name;
    private String description;

    /**
     * Constructs a VOElement from a DOM element.
     * The systemId is also required since some elements (STREAM, LINK) 
     * may need it for URL resolution.  It may be null however
     * (which is fine if there are no relative URLs used in the document).
     * In general application code should use one of the static methods in 
     * {@link VOElementFactory} in preference to this constructor.
     *
     * @param  el  DOM element on which the new object will be based
     * @param  systemId  the location of the document
     */
    public VOElement( Element el, String systemId ) {
        this.el = el;
        this.systemId = systemId;
        this.context = URLUtils.makeURL( systemId );

        /* Store items which are generic to most/all VOTable elements. */
        if ( el.hasAttribute( "ID" ) ) {
            id = el.getAttribute( "ID" );
        }
        if ( el.hasAttribute( "name" ) ) {
            name = el.getAttribute( "name" );
        }
        Element descEl = DOMUtils.getChildElementByName( el, "DESCRIPTION" );
        if ( descEl != null ) {
            description = DOMUtils.getTextContent( descEl );
        }
    }

    /**
     * Constructs a VOElement from a DOM element which is required to 
     * have a given tagname.
     *
     * @param  el  DOM element on which the new object will be based
     * @param  systemId  the location of the document
     * @param  tagname  the name that <tt>el</tt> is required to have
     * @throws  IllegalArgumentException  if <tt>el</tt>
     *          has a name other than <tt>tagname</tt>
     */
    protected VOElement( Element el, String systemId, String tagname ) {
        this( el, systemId );
        if ( ! getTagName().equals( tagname ) ) {
            throw new IllegalArgumentException(
                "Unsuitable element: " + getTagName() + " != " + tagname );
        }
    }

    /**
     * Returns the text of a DESCRIPTION element associated with this object,
     * or <tt>null</tt> if none exists.
     *
     * @return  the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the <tt>ID</tt> attribute value for this element,
     * or <tt>null</tt> if none exists.
     *
     * @return  the ID
     */
    public String getID() {
        return id;
    }

    /**
     * Returns the <tt>name</tt> attribute value for this element,
     * or <tt>null</tt> if none exists.
     *
     * @return  the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value of a named attribute for this element,
     * or <tt>null</tt> if there is no such attribute.
     *
     * @param   attname  the name of the attribute to retrieve
     * @return  the value of attribute <tt>attname</tt>
     */
    public String getAttribute( String attname ) {
        return el.hasAttribute( attname ) ? el.getAttribute( attname ) : null;
    }

    /**
     * Returns the value of a named attribute for this element,
     * or a default ('#IMPLIED') value if the attribute is not present.
     *
     * @param  attname  the name of the attribute to retrieve
     * @param  implied  the default value
     * @return the value of the attribute <tt>attname</tt> if present,
     *         or <tt>implied</tt> if not
     */
    public String getAttribute( String attname, String implied ) {
        return hasAttribute( attname ) ? getAttribute( attname ) : implied;
    }

    /**
     * Indicates whether this element has a value for a given attribute.
     *
     * @param   attname the name of an attribute
     * @return  true iff this element has an attribute called <tt>attname</tt>
     */
    public boolean hasAttribute( String attname ) {
        return el.hasAttribute( attname );
    }

    /**
     * Returns the parent element of this element as a VOElement.
     * Note that the returned object is not guaranteed to be one of
     * the elements in the VOTable DTD.  If this element is at the
     * root of the document, <tt>null</tt> will be returned.
     */
    public VOElement getParent() {
        Node pnode = el.getParentNode();
        if ( pnode != null && pnode instanceof Element ) {
            return VOElementFactory.makeVOElement( (Element) pnode, systemId );
        }
        else {
            return null;
        }
    }

    /**
     * Returns the child elements of this element.  Each is returned as
     * a VOElement or the appropriate specific VOElement subclass.
     *
     * @return  an array of VOElement children of this one
     */
    public VOElement[] getChildren() {
        List children = new ArrayList();
        for ( Node ch = el.getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof Element ) {
                children.add( VOElementFactory
                             .makeVOElement( (Element) ch, systemId ) );
            }
        }
        return (VOElement[]) children.toArray( new VOElement[ 0 ] );
    }

    /**
     * Returns all the child elements of this element which have a given
     * name.  Each is returned as a VOElement or the appropriate
     * specific VOElement subclass.
     *
     * @param  tagname  the element name required
     * @return an array of VOElement children of this one, all with element
     *         name <tt>tagname</tt>
     */
    public VOElement[] getChildrenByName( String tagname ) {
        List children = new ArrayList();
        for ( Node ch = el.getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof Element &&
                 ((Element) ch).getTagName().equals( tagname ) ) {
                children.add( VOElementFactory
                             .makeVOElement( (Element) ch, systemId ) );
            }
        }
        return (VOElement[]) children.toArray( new VOElement[ 0 ] );
    }

    /**
     * Returns the first child element of this element which has a given
     * name.  If there are more than one with the given name, later ones
     * are ignored.  If there are none, <tt>null</tt> is returned.
     * The element is returned as a VOElement or the appropriate specific
     * VOElement subclass.
     *
     * @param  tagname  the element name required
     * @return  the first child of this one with element name <tt>tagname</tt>
     */
    public VOElement getChildByName( String tagname ) {
        for ( Node ch = el.getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof Element &&
                 ((Element) ch).getTagName().equals( tagname ) ) {
                return VOElementFactory.makeVOElement( (Element) ch, systemId );
            }
        }
        return null;
    }

    /**
     * Returns all the descendants of this element which have a given name.
     * Each is returned as a VOElement or the appropriate specific VOElement
     * subclass.  They are returned in preorder traversal order.
     * 
     * @param  tagname  the element name required
     * @return  an array of all the VOElement descendants (to any level) 
     *          of this one which have the element name <tt>tagname</tt>
     */
    public VOElement[] getDescendantsByName( String tagname ) {
        NodeList nodes = el.getElementsByTagName( tagname );
        int nnode = nodes.getLength();
        VOElement[] selected = new VOElement[ nnode ];
        for ( int i = 0; i < nnode; i++ ) {
            Element node = (Element) nodes.item( i );
            assert node.getTagName().equals( tagname );
            selected[ i ] = VOElementFactory.makeVOElement( node, systemId );
        }
        return selected;
    }

    /**
     * Returns the tagname of the Element on which this object is based,
     * that is the element's name, like "RESOURCE" or "FIELD".
     *
     * @return  the element's name (note, not the value of the <tt>name</tt>
     *          attribute
     */
    public String getTagName() {
        return el.getTagName();
    }

    /**
     * Returns the DOM element on which this VOElement object is based.
     *
     * @return  the DOM element
     */
    public Element getElement() {
        return el;
    }

    /**
     * Returns an XML Source corresponding to this element.
     *
     * @return  a source containing the XML content of this element
     */
    public Source getSource() {
        return new DOMSource( el, systemId );
    }

    /**
     * Returns the system identifier against which relative URIs should
     * be resolved.
     *
     * @return   the system ID, or <tt>null</tt>
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Returns the base URL associated with this VOElement.  This will
     * normally be the same for the entire VOTable, namely the
     * URL of the VOTable document itself, and it will typically be
     * got from the System ID supplied at VOTable creation time.
     * Or, it may not be known at all, in which case <tt>null</tt> is returned.
     *
     * @return  the base URL associated with this element
     */
    public URL getContext() {
        return context;
    }

    /**
     * Returns the same value as {@link #getHandle}.
     *
     * @return  a string representation of this object
     */
    public String toString() {
        return getHandle();
    }

    /**
     * Returns something that can be used informally as a name for this
     * element.  May be ID or something other than the value of the
     * name attribute itself if no name exists.
     *
     * @return  a label for this element
     */
    public String getHandle() {
        String handle = "";
        if ( handle.length() == 0 ) {
            handle = el.getAttribute( "name" );
        }
        if ( handle.length() == 0 ) {
            handle = el.getAttribute( "ID" );
        }
        if ( handle.length() == 0 ) {
            handle = el.getAttribute( "ucd" );
        }
        if ( handle.length() == 0 ) {
            handle = el.getTagName();
        }
        handle = handle.replaceFirst( "\n.*", "" );
        handle = handle.trim();
        return handle;
    }

    /**
     * Returns the text contained in this element.
     * Any text elements are concatenated and other elements such as
     * element structure is ignored.  This method is only designed to
     * be used on elements which have a CDATA content model, but it
     * will return a usable result for others.
     *
     * @return  the text content
     */
    public String getTextContent() {
        return DOMUtils.getTextContent( el );
    }

    /**
     * Indicates whether this VOElement is equivalent to another.
     *
     * @param  other  comparison object
     * @return  <tt>true</tt> iff <tt>other</tt> is based on the same
     *          DOM {@link org.w3c.dom.Element} as this is
     */
    public boolean equals( Object other ) {
        return other instanceof VOElement 
            && getElement().equals( ((VOElement) other).getElement() );
    }

    /**
     * Returns the hash code of the DOM Element.
     */
    public int hashCode() {
        return getElement().hashCode();
    }
}
