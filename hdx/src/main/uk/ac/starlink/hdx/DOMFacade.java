package uk.ac.starlink.hdx;

import org.w3c.dom.*;

/**
 * A DOMFacade maintains an object's representation as a DOM.  As well
 * as generating the DOM representation, this type provides optional
 * support for altering the underlying object through a DOM interface.
 *
 * <p>Only the <code>getDOM</code> and <code>getObject</code> methods
 * need have non-trivial implementations.  The remaining methods, which mutate
 * the DOM, may have trivial implementations which always return null,
 * indicating that the DOM is effectively read-only.  If it is
 * appropriate for the facade to support modification, then the three
 * DOM-mutation methods are sufficient to support all the relevant
 * methods of the <code>Element</code> interface.
 *
 * @author Norman Gray
 * @version $Id$
 */
public interface DOMFacade {
    /**
     * Produces a DOM representing this object.  This facade is used by
     * being inserted into a DOM using {@link
     * HdxDocument#createElement(HdxResourceType,DOMFacade)}, and in
     * this case the element type returned by this method must match
     * the type declared in the call to <code>createElement</code>.
     *
     * <p>The returned DOM may be a snapshot or may be live.  It is
     * therefore not guaranteed that operations on the DOM are safe
     * under concurrent modifications of the underlying object.
     *
     * <p>The XML in general may contain URLs, for instance referencing the
     * array components of the NDX.  How these are written is determined
     * by the <code>base</code> parameter; URLs will be written as relative
     * URLs relative to <code>base</code> if this is possible (e.g. if they
     * share a part of their path).  If there is no common part of the
     * path, including the case in which <code>base</code> is <code>null</code>,
     * then an absolute reference will be written.
     *
     * <p><strong>Implementation note:</strong> This method is called
     * frequently and its results are not typically cached.
     * Implementors should bear this in mind if generating the DOM is
     * expensive.
     *
     * @param  base  the base URL against which URLs written within the XML
     *           are considered relative.  If null, all are written absolute.
     * @return an element representing the object
     */
    public Element getDOM(java.net.URL base);

    /**
     * Produces a <code>Source</code> representing the object.
     *
     * <p>This method, and {@link #getDOM} are partners, in the sense that
     * one is a convenience interface for the other, but which one is
     * which depends on the implementation.  Since the conversion between
     * a DOM and a Source might possibly require some processing, client
     * code should use the method which returns the type of object it
     * actually wants.
     *
     * <p>The XML in general may contain URLs, for instance referencing the
     * array components of the NDX.  How these are written is determined
     * by the <code>base</code> parameter; URLs will be written as relative
     * URLs relative to <code>base</code> if this is possible (e.g. if they
     * share a part of their path).  If there is no common part of the
     * path, including the case in which <code>base</code> is <code>null</code>,
     * then an absolute reference will be written.
     *
     * @param  base  the base URL against which URLs written within the XML
     *           are considered relative.  If null, all are written absolute.
     * @see     uk.ac.starlink.util.SourceReader
     * @return a Source representing the object
     */
    public javax.xml.transform.Source getSource(java.net.URL base);

    /**
     * Sets an attribute on an element.  If an attribute is `set' to a
     * null value, it is removed.
     *
     * <p>This is an optional operation.  An implementation may simply
     * return null to indicate that this operation is not supported.
     *
     * @param el the element which is to have the attribute set
     *
     * @param name the attribute which is to be set
     *
     * @param value the new value of the attribute.  If the value is
     * null, the attribute is removed.  Setting the value to the empty
     * string is allowed, and is not the same as setting it to null.
     *
     * @return true if the operation is supported and succeeded, or false 
     * if the operation is not supported in general, or failed in this case
     */
    public boolean setAttribute(Element el, String name, String value);

    /**
     * Adds a new child to an element.
     *
     * <p>This is an optional operation.  An implementation may simply
     * return null to indicate that this operation is not supported.
     *
     * @param parent the element which is to receive the new child
     *
     * @param newChild The child to be added
     *
     * @param refChild if non-<code>null</code>, the new child is to
     * be inserted before <code>refChild</code>.  If <code>refChild</code> is
     * <code>null</code>, the new child is to be appended to the end
     * of the list of children.
     *
     * @return true if the operation is supported and succeeded, or
     * false if the operation is not supported in general, or failed
     * in this case
     */
    public boolean addChildBefore(Element parent,
                                  Element newChild,
                                  Element refChild);

    /**
     * Replaces or removes a child.
     *
     * <p>This is an optional operation.  An implementation may simply
     * return null to indicate that this operation is not supported.
     *
     * @param parent the element which is to have its children changed
     *
     * @param oldChild the element which is to be replaced.
     *
     * @param newChild the replacement element.  If this is
     * <code>null</code>, then <code>oldChild</code> is deleted.
     *
     * @return true if the operation is supported and succeeded, or
     * false if the operation is not supported in general, or failed
     * in this case
     */
    public boolean replaceChild(Element parent,
                                Element oldChild,
                                Element newChild);

    /**
     * Obtains the object which the given Element is a facade for.  The caller
     * should know what (Java) type of object this will be (that is,
     * Ndx, HdxContainer, or the like), and will therefore be able to
     * cast the result appropriately.
     *
     * <p>The returned element must match the class appropriate for
     * the Hdx type this element corresponds to.  That is the return
     * value <code>obj</code> must be such that
     * <code>HdxResourceType.match(el).getConstructedClass().isInstance(obj)</code>
     * is true, if <code>getConstructedClass</code> returns non-null.
     *
     * @param el an Element which is to be transformed into an object
     *
     * @return the Java object which this element is a facade for.
     * The object must not be null.
     *
     * @throws HdxException if the facade does not know how to recover
     * the object type it has been asked for.  This will also happen
     * if the element <code>el</code> does not correspond to a known
     * <code>HdxResourceType</code>.
     */
    Object getObject(Element el) throws HdxException;
}
