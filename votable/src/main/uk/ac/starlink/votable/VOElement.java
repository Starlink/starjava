package uk.ac.starlink.votable;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

    private final int iseq_;

    /**
     * Constructs a VOElement from a DOM element.
     *
     * @param   base  element in base DOM
     * @param   doc   owner document for new element
     */
    VOElement( Element base, VODocument doc ) {
        super( base, doc );
        iseq_ = doc.getElementCount( doc.getVOTagName( this ) ) - 1;
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
        String voTagname = doc.getVOTagName( this );
        if ( ! voTagname.equals( tagname ) ) {
            throw new IllegalArgumentException( "Unsuitable Element: " + 
                voTagname + " != " + tagname );
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
     * name in the VOTable namespace.  
     * Each is returned as a VOElement or the appropriate
     * specific VOElement subclass.
     *
     * <p>Note that since STIL v2.8, but not before, the namespacing of
     * the argument to this method is influenced by the
     * default {@link Namespacing} class.
     *
     * @param  votagname  the unqualified element name in the VOTable 
     *         namespace required (such as "TABLE")
     * @return an array of VOElement children of this one, all with element
     *         name <tt>tagname</tt>
     */
    public VOElement[] getChildrenByName( String votagname ) {
        List children = new ArrayList();
        for ( Node ch = getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof Element &&
                 getVOTagName( (Element) ch ).equals( votagname ) ) {
                assert ch instanceof VOElement;
                children.add( ch );
            }
        }
        return (VOElement[]) children.toArray( new VOElement[ 0 ] );
    }

    /**
     * Returns the first child element of this element which has a given
     * name in the VOTable namespace.  If there are more than one with 
     * the given name, later ones are ignored.
     * If there are none, <tt>null</tt> is returned.
     * The element is returned as a VOElement or the appropriate specific
     * VOElement subclass.
     *
     * <p>Note that since STIL v2.8, but not before, the namespacing of
     * the argument to this method is influenced by the
     * default {@link Namespacing} class.
     *
     * @param  votagname  the unqualified element name in the VOTable
     *         namespace required (such as "TABLE")
     * @return  the first child of this one with element name <tt>tagname</tt>
     */
    public VOElement getChildByName( String votagname ) {
        for ( Node ch = getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof Element &&
                 getVOTagName( (Element) ch ).equals( votagname ) ) {
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
            handle = getVOTagName();
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

    /**
     * Returns the name of this element in the VOTable namespace.
     *
     * @return  unqualified VOTable element name for this element,
     *          such as "TABLE"
     */
    public String getVOTagName() {
        return getVOTagName( this );
    }

    /**
     * Returns the number of elements with the same tag name as this one
     * which were present in the document when this one was added to it.
     *
     * @return   sequence number of this element among similarly named ones
     */
    public int getElementSequence() {
        return iseq_;
    }

    /**
     * Returns a NodeList of all descendant Elements with a given
     * unqualified tag name in the VOTable namespace, in the order
     * in which they are encountered in a preorder traversal of this
     * Element tree.
     * This does the same as {@link org.w3c.dom.Element#getElementsByTagName},
     * but takes care of VOTable namespacing issues; 
     * calling it with the argument "TABLE" will find all VOTable TABLE
     * descendants.
     *
     * @param  voTagName  unqualified element name in VOTable namespace
     * @return  list of matching element nodes
     */
    public NodeList getElementsByVOTagName( String voTagName ) {
        final List findList = new ArrayList();
        addChildrenByVOTagName( this, voTagName, findList );
        return new NodeList() {
            public int getLength() {
                return findList.size();
            }
            public Node item( int i ) {
                return (Node) findList.get( i );
            }
        };
    }

    /**
     * Recursive routine used by getElementsByVOTagName.
     *
     * @param   el   element whose descendants to add
     * @param   voTagName  tag name in VOTable namespace
     * @return  elList  list of Elements to append new selected descendants to
     */
    private void addChildrenByVOTagName( Element el, String voTagName,
                                         List elList ) {
         
        for ( Node child = el.getFirstChild(); child != null;
              child = child.getNextSibling() ) { 
            if ( child instanceof Element ) {
                Element childEl = (Element) child;
                if ( voTagName.equals( getVOTagName( childEl ) ) ) {
                    elList.add( childEl );
                }
                addChildrenByVOTagName( childEl, voTagName, elList );
            }
        }
    }

    /**
     * Returns the VOTable tagname taking care of namespaces.
     * For instance will always return "TABLE" for a VOTable TABLE element.
     *
     * @param   el  element
     * @return   VOTable tag name
     */
    private String getVOTagName( Element el ) {
        Document doc = el.getOwnerDocument();
        return doc instanceof VODocument
             ? ((VODocument) doc).getVOTagName( el )
             : el.getTagName();
    }
}
