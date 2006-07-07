package uk.ac.starlink.votable;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.votable.dom.DelegatingElement;

/**
 * Element implementation for use within VOTable documents.
 * This implements the DOM Element interface, and so can be used just 
 * as a normal DOM element (including adding/removing/editing nodes).  
 * However, it also provides a few extra
 * convenience methods, some of which are based on knowledge of
 * the nature of the elements within a VOTable document.
 *
 * <p>This class is extended to provide the classes associated with 
 * specific VOTable elements.  These in turn provide more additional
 * methods associated with the specific properties of such elements, 
 * for instance the {@link TableElement} class has a <tt>getData</tt>
 * which returns the actual table cell data.  Those element types
 * which don't require any extra associated functionality (such as
 * RESOURCE) don't have their own subclass, they are just represented
 * as <tt>VOElement</tt>s.  The class of each element in the DOM 
 * is determined by its tag name - so every TABLE element will be
 * represented in the DOM as a {@link TableElement} and so on.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOElement extends DelegatingElement {

    /**
     * Constructs a VOElement from a DOM element.
     *
     * @param   base  element in base DOM
     * @param   doc   owner document for new element
     */
    VOElement( Element base, VODocument doc ) {
        super( base, doc );
    }

    /**
     * Constructs a VOElement with a given tagname.
     * This constructor just provides an extra level of safety.
     * Since it's not accessible from outside the package you might even
     * call it paranoia.
     *
     * @param  base  element in base DOM
     * @param  doc   owner document for new element
     * @param  tagname  name which <tt>base</tt> is asserted to have
     */
    VOElement( Element base, VODocument doc, String tagname ) {
        this( base, doc );
        if ( ! getTagName().equals( tagname ) ) {
            throw new IllegalArgumentException( "Unsuitable Element: " + 
                getTagName() + " != " + tagname );
        }
    }

    /**
     * Returns the text of a DESCRIPTION element associated with this object,
     * or <tt>null</tt> if none exists.  The return value is a plain
     * text string - any XML tags (XHTML is allowed in the VOTable1.1
     * DESCRIPTION content model) are stripped out.
     * If you want the full XML structure of the DESCRIPTION tag,
     * use instead <code>getChildByName("DESCRIPTION")</code>.
     *
     * @return  the description
     */
    public String getDescription() {
        Element descEl = getChildByName( "DESCRIPTION" );
        return descEl == null
             ? null
             : DOMUtils.getTextContent( descEl );
    }

    /** 
     * Returns the <tt>ID</tt> attribute value for this element,
     * or <tt>null</tt> if none exists.
     *
     * @return  the ID
     */
    public String getID() {
        return hasAttribute( "ID" ) ? getAttribute( "ID" ) : null;
    }

    /**
     * Returns the <tt>name</tt> attribute value for this element,
     * or <tt>null</tt> if none exists.
     *
     * @return  the name
     */
    public String getName() {
        return hasAttribute( "name" ) ? getAttribute( "name" ) : null;
    }

    /**
     * Returns the parent element of this element as a VOElement.
     * Note that the returned object is not guaranteed to be one of
     * the elements in the VOTable DTD.  If this element is at the
     * root of the document, <tt>null</tt> will be returned.
     *
     * @return  parent VOElement
     */
    public VOElement getParent() {
        Node pnode = getParentNode();
        if ( pnode != null && pnode instanceof VOElement ) {
            return (VOElement) pnode;
        }
        else if ( pnode instanceof Element ) {
            throw new AssertionError();
        }
        else {
            return null;
        }
    }

    /**
     * Returns the child elements of this element.  Each is returned as
     * an instance of VOElement or one of its specific subclasses.
     *
     * @return  an array of VOElement children of this one
     */
    public VOElement[] getChildren() {
        List children = new ArrayList();
        for ( Node ch = getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof VOElement ) {
                children.add( (VOElement) ch );
            }
            else if ( ch instanceof Element ) {
                throw new AssertionError();
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
        for ( Node ch = getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof Element &&
                 ((Element) ch).getTagName().equals( tagname ) ) {
                assert ch instanceof VOElement;
                children.add( ch );
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
        for ( Node ch = getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof Element &&
                 ((Element) ch).getTagName().equals( tagname ) ) {
                assert ch instanceof VOElement;
                return (VOElement) ch;
            }
        }
        return null;
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
            handle = getAttribute( "name" );
        }
        if ( handle.length() == 0 ) {
            handle = getAttribute( "ID" );
        }
        if ( handle.length() == 0 ) {
            handle = getAttribute( "ucd" );
        }
        if ( handle.length() == 0 ) {
            handle = getTagName();
        }
        handle = handle.replaceFirst( "\n.*", "" );
        handle = handle.trim();
        return handle;
    }

    /**
     * Returns any system ID associated with this node.  It is the
     * system ID (basically, a relative or absolute location) of the
     * owner document.
     *
     * @return  system ID
     */
    public String getSystemId() {
        return ((VODocument) getOwnerDocument()).getSystemId();
    }

    /**
     * Returns a URL corresponding to a given <tt>href</tt> string in the
     * context of this document's system ID.
     *
     * @return  URL for <tt>href</tt> in the current context
     * @see   uk.ac.starlink.util.URLUtils
     */
    URL getContextURL( String href ) {
        return URLUtils.makeURL( ((VODocument) getOwnerDocument())
                                .getSystemId(), href );
    }

}
