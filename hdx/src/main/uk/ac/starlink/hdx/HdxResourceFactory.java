package uk.ac.starlink.hdx;

/**
 * Unifies the various factories for constructing HDX
 * and related objects from DOM elements and the URIs contained within
 * them.
 *
 * <p>Implementations of this interface should be registered by the
 * {@link HdxFactory#registerHdxResourceFactory registerHdxResourceFactory} method of
 * <code>HdxFactory</code>
 *
 * @version $Id$
 */
public interface HdxResourceFactory {
    /**
     * Constructs an object from the URL given in the `url' attribute
     * of the given {@link org.w3c.dom.Element}, and returns it cast
     * to Object.  If the URL is not one that this constructor can
     * handle, then it should signal this by returning null promptly,
     * without error.
     *
     * <p>The URL will typically be an absolute URL (but not
     * necessarily, since there are cases where there is no absolute
     * URL readily definable), so that this method will not generally
     * need to do any relative-to-absolute or URI-to-URL resolution.
     * It is free to do so if this makes the method more robust, but
     * in this case it is permitted to return null.
     * There will also be a `uri' attribute on the element; <em>if
     * there is no `url' attribute</em>, the method is free to use the
     * `uri' attribute, but is not required to, and may ignore it.
     *
     * <p>An object may act as a factory for more than one element
     * type, distinguishing between them by testing the name of the
     * element parameter.
     *
     * <p>The Element will correspond to the {@link HdxResourceType}
     * which this element was registered as handling (or one of them,
     * if it was registered as handling more than one type).
     *
     * <p>XXX Note that this method will not work for URIs referring to
     * the contents of transient jar files (for example): for that we
     * will shortly add a similar method which is additionally passed
     * an InputStream.
     *
     * @param el an element containing the information required to
     * construct a Java object.
     *
     * @return an object of the correct type, as an Object, or null if
     * this constructor cannot handle this URL
     *
     * @throws HdxException if the constructor should be able to
     * handle this element but is unable to.  This should also be
     * thrown for any this-can't-happen errors, such as the element
     * argument being an unrecognised type.
     */
    public Object getObject(org.w3c.dom.Element el)
            throws HdxException;
}
