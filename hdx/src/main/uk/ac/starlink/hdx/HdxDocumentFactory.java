package uk.ac.starlink.hdx;

/**
 * The <code>HdxDocumentFactory</code> represents a factory which may
 * be able to convert a given URL into a DOM.  Objects implementing
 * this interface are registered with {@link HdxFactory} using the
 * {@link HdxFactory#registerHdxDocumentFactory registerHdxDocumentFactory} method on the
 * <code>HdxFactory</code>.
 */
public interface HdxDocumentFactory {

    /**
     * Constructs a DOM based on the URL, which represents the data
     * structure of the resource contained within the URL, in terms of
     * the HDX data model.  If the resource pointed to by the URL is
     * not of a type that this factory can handle, then it returns null
     * promptly.  That is, an object implementing this interface has
     * not committed itself to constructing the object, but merely offered to
     * make an attempt.
     *
     * <p>The returned DOM does not have to be normalized in any way,
     * as it will be subject to a subsequent normalization and
     * validation step, to resolve namespace and hierarchy issues.
     * That is, the returned DOM can consist of
     * <em>either</em> the HDX element types alone, not declared in
     * any namespace, <em>or</em> the HDX elements, declared in the
     * HDX namespace, possibly along with other elements not in that
     * namespace.
     *
     * <p>The returned DOM should generally be as complete as possible.  For
     * example, if the URL corresponds to a FITS file which contains
     * both Data and Variance information in HDUs (and so corresponds
     * to an NDX in HDX terms), the factory should create both Data
     * and Variance Element nodes, the URLs of which refer to the
     * corresponding HDUs, and not simply the NDX node which the FITS
     * file as a whole corresponds to.
     *
     * <p>However it is often not appropriate or convenient to
     * construct the whole DOM at once, and in this case some parts of
     * the DOM might be better handled by a separate object.  An
     * element created by the (extension) method {@link
     * HdxDocument#createElement(HdxResourceType,DOMFacade)} acts
     * fully as an element in the DOM, but hands off the actual
     * processing to an object which understands the underlying data
     * format, and implements the {@link DOMFacade} interface.
     *
     * @return a DOM representing the data in the URL, or null if we
     * cannot handle this type of URL.
     *
     * @throws HdxException if we ought to be able to read this type
     * of URL, but processing fails for some reason.  That is, do not
     * simply fail silently in this situation.
     *
     * @see HdxResourceType
     */
    public org.w3c.dom.Document makeHdx(java.net.URL url)
            throws HdxException;
}
