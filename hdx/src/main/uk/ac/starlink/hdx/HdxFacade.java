package uk.ac.starlink.hdx;

import org.w3c.dom.*;

/**
 * An <code>HdxFacade</code> maintains an object's representation as a
 * DOM.  As well as generating the DOM representation, this type
 * provides optional support for altering the underlying object
 * through a DOM interface.  Although it is implementations of this
 * interface which are ultimately responsible for generating XML in
 * the HDX system, the methods here are <em>not</em> intended to be
 * invoked directly by application code: instead, use the method {@link
 * HdxFactory#newHdxContainer(HdxFacade)}, and use the DOM and Source
 * methods on the resulting <code>HdxContainer</code>.  Though this
 * may seem roundabout, it means that the resulting XML has been
 * through whatever normalization or relativization steps are
 * required.
 *
 * <p>XXX <code>getDOM</code> method is almost certainly disappearing!
 * Thus these class comments need a rewrite.
 *
 * <p>Only the <code>synchronizeElement</code>,
 * <code>getObject</code> and <code>getHdxResourceType</code> methods
 * need have non-trivial implementations.  This interface can most
 * efficiently be implemented by extending <code>AbstractHdxFacade</code>.
 *
 * <p>The <code>getDOM</code> and <code>getSource</code> methods, can
 * be straightforwardly implemented in terms of
 * <code>synchronizeElement</code>, and this is what is done in
 * <code>AbstractHdxFacade</code>.  These two methods,
 * <code>getDOM</code> and <code>getSource</code>, are complementary:
 * one is a convenience interface for the other.  Which is which,
 * however, will vary between implementations, and client code should
 * call whichever one of the two is actually required.
 *
 * <p>The remaining methods, <code>addChildBefore</code>,
 * <code>replaceChild</code> and <code>setAttribute</code>, which mutate
 * the DOM, may have trivial implementations which always return null,
 * indicating that the DOM is effectively read-only.  If it is
 * appropriate for the facade to support modification, then these three
 * DOM-mutation methods are sufficient to support all the relevant
 * methods of the <code>Element</code> interface.
 *
 *
 * @author Norman Gray
 * @version $Id$
 */
public interface HdxFacade extends Cloneable {
    /*
     * <p>XXX do we need to worry about the fact that the object which
     * implements this will hang around at least as long as the
     * HdxFacadeElement which holds a reference to it, which might be
     * problematic if that object is very large.  Should we distinguish
     * getDOM from synchronizeElement, stating that the latter should be
     * used for any persistent DOMs.  Or can we just use cloneNode
     * cleverly somewhere?  I did at one point worry that the implementor
     * of synchronizeElement oughtn't to return itself as the memento, for
     * just this sort of reason; but that's fine in fact, since that
     * memento would then merely be a duplicate reference to the reference
     * to the object as an HdxFacade.
     *
     * <p>XXX The methods in this class throw very few exceptions: this
     * may not be appropriate, and this might change.
     */

//     /**
//      * Produces a DOM representing this object..  The element type
//      * returned by this method must match the type returned by the
//      * facade's {@link #getHdxResourceType}.
//      *
//      * <p>The returned DOM may be a snapshot or may be live.  It is
//      * therefore not guaranteed that operations on the DOM are safe
//      * under concurrent modifications of the underlying object.
//      *
//      * <p>The XML in general may contain URLs, for instance referencing the
//      * array components of the NDX.  How these are written is determined
//      * by the <code>base</code> parameter; URLs will be written as relative
//      * URLs relative to <code>base</code> if this is possible (e.g. if they
//      * share a part of their path).  If there is no common part of the
//      * path, including the case in which <code>base</code> is <code>null</code>,
//      * then an absolute reference will be written.
//      *
//      * @param  base  the base URI against which URIs written within the XML
//      *           are considered relative.  If null, all are written absolute.
//      * @return an element representing the object
//      */
//     public Element getDOM(java.net.URI base);

    /**
     * Adds attributes and children to the given element, to represent
     * the current state of the object as a DOM.  The implementing
     * object should add or update elements and attributes on the given element
     * using the <code>Document</code> obtained by invoking
     * {@link Node#getOwnerDocument} on the element.  This
     * <code>Document</code> will in fact be an instance of 
     * {@link uk.ac.starlink.hdx.HdxDocument}, which implements the standard
     * <code>Document</code> interface with the addition of {@link
     * HdxDocument#createElement(HdxFacade)}, which the implementing
     * code is therefore free to use (this is an understatement -- the
     * entire point of this interface is to give an implementing class the
     * opportunity of using this method).
     *
     * <p>The element will have the type
     * <code>this.getHdxResourceType().xmlname()</code>.
     *
     * <p>This method is called frequently, and the implementing
     * object should therefore take care to do no more work than is
     * necessary.  To assist in this, the caller will preserve a
     * <em>memento</em> on behalf of the implementor.  This memento is
     * the object returned by this method, which is re-presented when
     * the <code>synchronizeElement</code> method is next called (this
     * is an example of `memento' pattern, although that is more
     * commonly associated with undo actions than the cache-like
     * operation described here).  If the implementing object needs to
     * preserve some state, to remind it of its own state when it last
     * synchronised the element, then it should wrap that state in
     * some object or other and return it from this method.  This
     * might be as simple as a hash-code.
     *
     * <p>The first time this method is called, the memento will be null.
     *
     * <p>The returned memento may be null if, for example, the
     * implementor can extract all its relevant state information from
     * the DOM it receives in the Element; or if the object is
     * immutable, so that the Element attributes and children are
     * correct if they are there at all.  This null memento will be
     * duly returned to the implementor on any future invocation.  In
     * such a case, the implementor might need to be careful to distinguish
     * this returned null memento from the null memento provided when
     * the method is called the first time.
     *
     * <p>It is perfectly feasible for the implementor to
     * return <em>itself</em> as the memento.
     *
     * @param el an element which is to be made consistent with the
     * current state of the object
     * @param memento either null, if this is the first time this
     * method has been called, or the object which was returned by
     * this method last time it was called (which may be null if that
     * is what the method chose to return)
     * @return an object or null, which is to be returned to the
     * method next time it is called
     * @throws HdxException if it is for some reason impossible to
     * update the DOM.  The method should regard this as something
     * akin to a `can't happen' error: this thrown exception will
     * be converted to a <code>DOMException</code> if that is
     * reasonable for the caller, but if not, may be converted to a
     * {@link PluginException}.
     */
    public Object synchronizeElement(Element el, Object memento)
            throws HdxException;

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
     * @param  base  the base URI against which URIs written within the XML
     *               are considered relative.  If null, all are
     *               written absolute.
     * @return a Source representing the object
     * @throws HdxException if the <code>Source</code> cannot be
     *               generated fo some reason
     * @see     uk.ac.starlink.util.SourceReader
     */
    public javax.xml.transform.Source getSource(java.net.URI base)
            throws HdxException;

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
     * @param newChild the replacement element.  If this is
     * <code>null</code>, then <code>oldChild</code> is deleted.
     *
     * @param oldChild the element which is to be replaced.
     *
     * @return true if the operation is supported and succeeded, or
     * false if the operation is not supported in general, or failed
     * in this case
     */
    public boolean replaceChild(Element parent,
                                Element newChild,
                                Element oldChild);

    /**
     * Obtains the object which the given Element is a facade for.  The caller
     * should know what (Java) type of object this will be (that is,
     * Ndx, HdxContainer, or the like), and will therefore be able to
     * cast the result appropriately.
     *
     * <p>The returned element must match the class appropriate for
     * the Hdx type this element corresponds to.  That is the return
     * value <code>obj</code> must be such that the following are true
     * <ul>
     * <li><code>HdxResourceType.match(el).getConstructedClass().isInstance(obj)</code>
     * <li><code>HdxResourceType.match(el) == facade.getHdxResourceType()</code>
     * </ul>
     * for any instance <code>facade</code> of this interface,  and if
     * <code>el</code> is the <code>Element</code> which this facade is linked to.
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

    /**
     * Obtains the <code>HdxResourceType</code> which this is a facade for.
     *
     * <p>This is not (just) a convenience method.  Although the same
     * information is retrievable by calling {@link #getSource}, and
     * calling {@link HdxResourceType#match(Element)} on the top
     * element obtained from it, 
     * <ol>
     * <li>calling <code>getSource</code> may involve significant
     * processing, and
     * <li>it is not trivial to retrieve the top element from a
     * <code>Source</code>, since the root of the resulting document
     * (matched by the XPath <code>/</code>) is not guaranteed to be
     * the `top' node of the DOM (see {@link
     * javax.xml.transform.dom.DOMSource#DOMSource(Node)}) 
     * </ol>
     *
     * @return the <code>HdxResourceType</code> this facade represents
     */
    HdxResourceType getHdxResourceType();

    /**
     * Creates and returns a copy of this object.
     *
     * <p>The {@link AbstractHdxFacade} skeleton implementation
     * implements this as simply
     * <pre>
     * public Object clone() { return super.clone(); }
     * </pre>
     * which is generally suitable.  If, however, the underlying object, or its
     * DOM, is mutable, and particularly if the
     * <code>addChildBefore()</code>, <code>replaceChild()</code> and
     * <code>setAttribute()</code> methods are implemented, the
     * implementation should very probably add a cleverer
     * <code>clone()</code> method.  Recall that
     * if <code>clone()</code> is overridden, <code>equals()</code>
     * and <code>hashCode()</code> should be overridden also.
     */
    public Object clone();
}
