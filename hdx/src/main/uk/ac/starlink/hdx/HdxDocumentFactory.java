package uk.ac.starlink.hdx;

/**
 * The <code>HdxDocumentFactory</code> represents a factory which may
 * be able to convert a given URL into a DOM or an Java/XML {@link
 * javax.xml.transform.Source}.  Objects implementing this interface
 * are registered with {@link HdxFactory} using the {@link
 * HdxFactory#registerHdxDocumentFactory registerHdxDocumentFactory}
 * method on the <code>HdxFactory</code>.
 *
 * <p>The two methods in this interface construct a DOM/Source based
 * on the URL, which represents the data structure of the resource
 * contained within the URL, in terms of the HDX data model.  If the
 * resource pointed to by the URL is not of a type that this factory
 * can handle, then it returns null promptly.  That is, an object
 * implementing this interface has not committed itself to
 * constructing the object, but merely offered to make an attempt.
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
 * HdxDocument#createElement(HdxFacade)} acts
 * fully as an element in the DOM, but hands off the actual
 * processing to an object which understands the underlying data
 * format, and implements the {@link HdxFacade} interface.
 *
 * <p>For example, the complete implementation of
 * <code>makeHdxDocument</code> in the class which handles FITS files
 * is:
 * <pre>
 * public org.w3c.dom.Document makeHdxDocument(java.net.URL url)
 *         throws HdxException {
 *     try {
 *         Ndx fitsNdx = makeNdx(url, AccessMode.READ);
 *         if (fitsNdx == null)
 *             // clearly not a FITS file -- nothing to do with us
 *             return null;
 *     
 *         HdxDocument doc = (HdxDocument)HdxDOMImplementation
 *                 .getInstance()
 *                 .createDocument(null, "hdx", null);
 *         Element el = doc.createElement("hdx");
 *         doc.appendChild(el);
 *         Element ndxEl = doc.createElement(fitsNdx.getHdxFacade());
 *         el.appendChild(ndxEl);
 *         return doc;
 *     } catch (IOException ex) {
 *         // Method makeNdx thought it should have been able to
 *         // handle this, but processing failed.  We reprocess this
 *         // into an HdxException.
 *         throw new HdxException("Failed to handle URL " + url
 *                                + " (" + ex + ")");
 *     }
 * }
 * </pre>
 * Here, the method does the bulk of the work using the appropriate
 * <code>makeNdx</code> which was defined in the same class (as part
 * of that class's implementation of the {@link
 * uk.ac.starlink.ndx.NdxHandler} interface).  After that, it creates
 * a DOM consisting of only two elements, one of which is created
 * using the <code>HdxDocument</code> extension method creating an
 * element using a {@link HdxFacade}.  There was no need in this case
 * to implement the <code>HdxFacade</code> interface, since this is
 * handled by the {@link uk.ac.starlink.ndx.BridgeNdx} object which
 * <code>makeNdx</code> returns.
 *
 * <p>Of the two methods in this interface, one is a convenience
 * interface for the other, but which one is which depends on the
 * implementation.  Since the conversion between a DOM and a Source
 * might possibly require some processing, client code should use the
 * method which returns the type of object it actually wants.
 *
 * <p><strong>Note</strong> to implementors of this interface: To
 * avoid confusion, note that, despite its name,
 * <code>HdxDocumentFactory</code> is <em>not</em> a general factory
 * for creating empty instances of {@link HdxDocument}.  The
 * <code>makeHdxDocument</code> method below returns a
 * <code>org.w3c.dom.Document</code> which, in many cases, will
 * actually be an instance of <code>HdxDocument</code>; however, the
 * code which implements that method will create empty
 * <code>HdxDocument</code> instances using {@link HdxDOMImplementation}.
 *
 * @author Norman Gray
 * @version $Id$
 */
public interface HdxDocumentFactory {

    /**
     * Obtains a Document representing the data referred to by the URL.
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
    public org.w3c.dom.Document makeHdxDocument(java.net.URL url)
            throws HdxException;

    /**
     * Obtains a Source which can produce the data referred to by the
     * URL.
     *
     * @return a Source representing the data, or null if we cannot
     * handle this type of URL
     *
     * @throws HdxException if we ought to be able to read this type
     * of URL, but processing fails for some reason.  That is, do not
     * simply fail silently in this situation.
     */
    public javax.xml.transform.Source makeHdxSource(java.net.URL url)
            throws HdxException;
}
