package uk.ac.starlink.hdx;

import org.w3c.dom.*;

/**
 * A DOMFacade maintains an object's representation as a DOM.  As well
 * as generating the DOM representation, this type provides optional
 * support for altering the underlying object through a DOM interface.
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
     * <p><strong>Implementation note:</strong> This method is called
     * frequently and its results are not typically cached.
     * Implementors should bear this in mind if generating the DOM is
     * expensive.
     *
     * @return an element representing the object.
     */
    public HdxElement toDOM();

    /**
     * Sets an attribute on an element.  If an attribute is `set' to a
     * null value, it is removed.
     *
     * <p>This is an optional operation.  An implementation may simply
     * return null to indicate that this operation is not supported.
     *
     * @param el The element which is to have the attribute set.
     *
     * @param name The attribute which is to be set
     *
     * @param value The new value of the attribute.  If the value is
     * null, the attribute is removed.  Setting the value to the empty
     * string is allowed, and is not the same as setting it to null.
     *
     * @return True if the operation is supported and succeeded, or false 
     * if the operation is not supported in general, or failed in this case.
     */
    public boolean setAttribute(Element el, String name, String value);

    /**
     * Adds a new child to an element.
     *
     * <p>This is an optional operation.  An implementation may simply
     * return null to indicate that this operation is not supported.
     *
     * @param parent The element which is to receive the new child
     *
     * @param newChild The child to be added
     *
     * @param refChild If non-<code>null</code>, the new child is to
     * be inserted before <code>refChild</code>.  If <code>refChild</code> is
     * <code>null</code>, the new child is to be appended to the end
     * of the list of children.
     *
     * @return True if the operation is supported and succeeded, or
     * false if the operation is not supported in general, or failed
     * in this case.
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
     * @param parent The element which is to have its children changed
     *
     * @param oldChild The element which is to be replaced.
     *
     * @param newChild The replacement element.  If this is
     * <code>null</code>, then <code>oldChild</code> is deleted.
     *
     * @return True if the operation is supported and succeeded, or
     * false if the operation is not supported in general, or failed
     * in this case.
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
     * @return The Java object which this element is a facade for.
     * The object must not be null.
     */
    Object getObject(Element el);
}
