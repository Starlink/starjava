package uk.ac.starlink.hdx;

import java.net.URL;
import java.net.URI;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.*;

import uk.ac.starlink.util.URLUtils;

/** 
 * Skeletal implementation of the {@link HdxFacade} interface.
 *
 * <p>This includes trivial implementations of
 * <code>addChildBefore</code>, <code>replaceChild</code> and
 * <code>setAttribute</code>, which act as a read-only DOM.  It also
 * implements the <code>getSource</code> method in terms of the
 * corresponding <code>getDOM</code> method, which is in turn
 * implemented using <code>synchronizeElement</code>.
 *
 * <p>XXX <code>getDOM</code> method is almost certainly disappearing!
 * Thus these class comments need a rewrite.
 *
 * <p>As noted here, this abstract class implements
 * <code>getSource</code> in terms of <code>getDOM</code>, because
 * this is more often useful than the other way around.  Recall,
 * however, that the base interface {@link HdxFacade} asserts that
 * these two methods are complementary, and an extending class may
 * perfectly reasonably implement the latter in terms of the former.
 *
 * <p>Thus, the only methods which an implementing class must itself
 * implement are <code>getHdxResourceType()</code>,
 * <code>synchronizeElement()</code> and <code>getObject()</code>.
 *
 * <p>The <code>clone()</code> method which this class implements is a
 * simple one, which will be generally appropriate, since the facade
 * has no instance variables.
 *
 * @author Norman Gray
 * @version $Id$
 */
public abstract class AbstractHdxFacade implements HdxFacade {
    public abstract Object synchronizeElement(Element el, Object memento)
            throws HdxException;
    public abstract Object getObject(Element el)
            throws HdxException;
    public abstract HdxResourceType getHdxResourceType();

    private Document dom;

    /**
     * Produces a DOM representing this object, by creating a new
     * <code>HdxContainer</code> and then extracting the first child,
     * which is a DOM representation of this object.
     * @throws HdxException if it cannot get the
     * <code>HdxFactory</code> (which should mean, never)
     */
    private Element getDOM(URI base) 
            throws HdxException {
        HdxContainer hdx = HdxFactory.getInstance().newHdxContainer(this);
        Element thisElement = (Element)hdx.getDOM(null).getFirstChild();
        assert HdxResourceType.match(thisElement) == getHdxResourceType();
        
        if (base == null)
            return thisElement;
        else
            // clone thisElement, and return a relativized version of it
            return (Element)uk.ac.starlink.util.DOMUtils.relativizeDOM
                    (thisElement.cloneNode(true), base, null);
    }

//     /**
//      * Produces a DOM representing this object, by creating a new top
//      * element, and calling <code>synchronizeElement</code> to acquire
//      * children which match the current state of the element.
//      */
//     public Element getDOM(URI base) {
//         Element de;
//         if (dom == null) {
//             String elementname = getHdxResourceType().xmlName();
//             dom = HdxDOMImplementation
//                     .getInstance()
//                     .createDocument(null, elementname, null);
//             de = dom.createElement(elementname);
//             dom.appendChild(de);
//         } else {
//             de = dom.getDocumentElement();
//         }
//         try {
//             synchronizeElement(de, null);
//         } catch (HdxException e) {
//             // This is a can't happen error -- so ...
//             throw new PluginException
//                     ( "unable to synchronize Elements: " + e );
//         }
        
//         if (base == null)
//             return de;
//         else
//             // clone de, and return a relativized version of it
//             return (Element)uk.ac.starlink.util.DOMUtils.relativizeDOM
//                     (de.cloneNode(true), base, null);
//     }
    
    /**
     * Produces a Source representing this object.
     * @throws HdxException if the <code>Source</code> cannot be
     * generated for some reason
     */
    public Source getSource(URI base) 
            throws HdxException {
        return (base == null)
                ? new DOMSource(getDOM(null))
                : new DOMSource(getDOM(base), base.toString());
    }

    /**
     * Adds a new child to an element.
     * Trivial implementation, which does not allow DOM modification
     *
     * @return false, indicating that the DOM cannot be modified
     */
    public boolean addChildBefore(Element parent,
                                  Element newChild,
                                  Element refChild) {
        return false;
    }
    
    /**
     * Replaces or removes a child.
     * Trivial implementation, which does not allow DOM modification
     *
     * @return false, indicating that the DOM cannot be modified
     */
    public boolean replaceChild(Element parent,
                                Element newChild,
                                Element oldChild) {
        return false;
    }

    /**
     * Sets an attribute on an element.
     * Trivial implementation, which does not allow DOM modification
     *
     * @return false, indicating that the DOM cannot be modified
     */
    public boolean setAttribute(Element el, String name, String value) {
        return false;
    }

    /** 
     * Creates and returns a copy of this element
     *
     * @return a copy of this element
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // From super.clone().  This can't happen: Since HdxFacade
            // does extend Cloneable, Object.clone() does not in
            // fact throw this exception.
            throw new AssertionError
                    ("Can't happen: Object.clone() threw exception in AbstractHdxFacade");
        }
    }

    // Don't override equals() or hashCode(), since the defaults are
    // not wrong.
}
