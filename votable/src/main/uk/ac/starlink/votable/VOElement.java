package uk.ac.starlink.votable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;

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
 * <p>The implementation of VOElement and its subclasses is currently
 * in terms of the {@link org.w3c.dom} classes, but this may possibly change 
 * in the future.  The {@link #getChildren} and {@link #getAttribute} methods
 * allow implementation-neutral navigation through the VOTable tree.
 * For more fine control of the underlying document you can maintain
 * your own XML document representation and create VOElement objects
 * from selected nodes of it using the {@link #makeVOElement} method.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOElement {

    private String description;
    private String id;
    private String name;
    private Element el;
    private String systemId;
    private URL context;
 
    /**
     * Constructs a VOElement from an XML Source.
     *
     * @param  xsrc  the XML source
     */
    protected VOElement( Source xsrc ) {
        try {
            this.el = new SourceReader().getElement( xsrc );
        }
        catch ( TransformerException e ) {
            throw new IllegalArgumentException( "Unsuitable source" );
        }
        this.systemId = xsrc.getSystemId();
        if ( systemId != null ) {
            try {
                context = new URL( new File( "." ).toURI().toURL(), systemId );
            }
            catch ( MalformedURLException e ) {
                context = null;  // never mind
            }
            catch ( SecurityException e ) {
                context = null;  // never mind
            }
        }
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
     * Constructs a VOElement from an XML source which is required to 
     * contain an Element with a given tag name.
     *
     * @param  the XML source
     * @param  tagname  the name that the element contained in <tt>xsrc</tt>
     *         is required to have
     * @throws  IllegalArgumentException  if the element in <tt>xsrc</tt>
     *          has a name other than <tt>tagname</tt>
     */
    protected VOElement( Source xsrc, String tagname ) {
        this( xsrc );
        if ( ! getTagName().equals( tagname ) ) {
            throw new IllegalArgumentException( 
                "Unsuitable source: " + getTagName() + " != " + tagname );
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
            Source psrc = new DOMSource( (Element) pnode, systemId );
            return VOElement.makeVOElement( psrc );
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
                Source chsrc = new DOMSource( ch, systemId );
                children.add( VOElement.makeVOElement( chsrc ) );
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
                Source chsrc = new DOMSource( ch, systemId );
                children.add( VOElement.makeVOElement( chsrc ) );
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
                Source chsrc = new DOMSource( ch, systemId );
                return VOElement.makeVOElement( chsrc );
            }
        }
        return null;
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
    Element getElement() {
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
        if ( handle.length() == 0 && description != null ) {
            handle = description;
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
     * Constructs a VOElement object of the most specific type possible
     * from an XML Source.
     *
     * @param   xsrc  the XML source representing the element
     * @return  a VOElement of the most specific kind available
     */
    public static VOElement makeVOElement( Source xsrc ) {

        /* Get the tag name. */
        String name;
        if ( xsrc instanceof DOMSource ) {
            Node node = ((DOMSource) xsrc).getNode();
            if ( node instanceof Element ) {
                name = ((Element) node).getTagName();
            }
            else if ( node instanceof Document ) {
                name = ((Document) node).getDocumentElement().getTagName();
            }
            else {
                throw new IllegalArgumentException( "Unsuitable source" );
            }
        }
        else {
            try {
                Element el = new SourceReader().getElement( xsrc );
                name = el.getTagName();
                xsrc = new DOMSource( el, xsrc.getSystemId() );
            }
            catch ( TransformerException e ) {
                throw new RuntimeException( e );
            }
        }

        /* And build an appropriate VOElement. */
        if ( name.equals( "VOTABLE" ) ) {
            return new VOTable( xsrc );
        }
        else if ( name.equals( "FIELD" ) ) {
            return new Field( xsrc );
        }
        else if ( name.equals( "PARAM" ) ) {
            return new Param( xsrc );
        }
        else if ( name.equals( "LINK" ) ) {
            return new Link( xsrc );
        }
        else if ( name.equals( "VALUES" ) ) {
            return new Values( xsrc );
        }
        else if ( name.equals( "TABLE" ) ) {
            return Table.makeTable( xsrc );
        }
        else {
            return new VOElement( xsrc );
        }
    }

}
