package uk.ac.starlink.hdx;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.File;
import java.util.Iterator;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

/**
 * This factory marshalls all the object creation factories involved
 * in the Hdx layer.
 *
 * <p>To create a new {@link HdxContainer} from a URI, use the
 * following paradigm:
 * <pre>
 * URI myData = new URI("...");
 * HdxFactory factory = HdxFactory.getInstance();
 * HdxContainer myHdx = factory.newHdxContainer(myData);
 * // process myHdx...
 * </pre>
 * If you already have a DOM obtained from an XML file, then you can extract 
 * the HdxContainer from it using the {@link #newHdxContainer(Element)} method.
 *
 * <p>This class is also the one responsible for validating DOMs as
 * being part of the HDX system.  This validation method is contained
 * in the static method {@link HdxResourceType#isValidHdx(Document)} and the validators for the individual types, {@link HdxResourceType#isValid(Element)}.
 *
 * @author Norman Gray (norman@astro.gla.ac.uk)
 * @version $Id$ 
 */
public class HdxFactory {

    /** Singleton instance of this class */
    private static HdxFactory instance = null;

    /** List of HdxDocumentFactory instances, tried in turn. */
    private java.util.List hdxFactoryList;

    /** XSLT transform engine. */
    private static javax.xml.transform.Transformer transformer;

    private HdxFactory()
            throws HdxException {
        // Private constructor
        hdxFactoryList = new java.util.ArrayList(3);

        // Add default HdxDocumentFactory instances
        //registerHdxDocumentFactory(HdsHdxDocumentFactory.getInstance());
        registerHdxDocumentFactory(XmlHdxDocumentFactory.getInstance());
    }

    /**
     * Obtains an instance of the <code>HdxFactory</code>.
     */
    public static HdxFactory getInstance()
            throws HdxException {
        if (instance == null)
            instance = new HdxFactory();
        return instance;
    }

    /**
     * Adds a {@link HdxDocumentFactory} to the list of factories
     * tried by {@link #newHdxContainer(Element)}.  The factory is
     * added at the beginning of the list of factories tried.
     *
     * @param factory an object implementing the {@link
     * HdxDocumentFactory} interface.
     */
    public void registerHdxDocumentFactory(HdxDocumentFactory factory) {
        hdxFactoryList.add(0, factory);
    }

    /**
     * Constructs a new {@link HdxContainer} from the supplied URI.
     *
     * <p>If there is no handler available to parse the given URI,
     * then we return null.
     *
     * @throws HdxException if there is an unexpected problem creating
     * the <code>HdxContainer</code> (that is, if we <em>ought</em> to
     * be able to handle this URI, but fail for some reason).
     */
    public HdxContainer newHdxContainer(URI uri)
            throws HdxException {
        Document hdxdom = null;
        URL url = fullyResolveURI(uri);
        for (Iterator fi = hdxFactoryList.iterator();
             hdxdom == null && fi.hasNext();
             ) {
            HdxDocumentFactory factory = (HdxDocumentFactory)fi.next();
            hdxdom = factory.makeHdx(url);
        }

//         System.err.println("HdxFactory.newHdxContainer(" + uri + "):"
//                            + (hdxdom == null ? "null" : hdxdom.toString()));

        if (hdxdom == null)
            // we fell out of the end of the loop -- no good handlers
            return null;

        Element docelem = hdxdom.getDocumentElement();
        System.err.println("  docelem="
                           + (docelem==null ? "null" : docelem.getTagName()));
        
        if (docelem == null)
            // It's XML Jim, but not as we know it
            return null;

        return newHdxContainer(docelem);
        
//         Element hdx = validateHdxDOM(docelem);
//         System.err.println("  validated="
//                            + (hdx==null ? "null" : hdx.getTagName()));
//         if (hdx == null)
//             return null;
//         else
//             return new DomHdxContainer(hdx);
    }

    /**
     * Constructs a new {@link HdxContainer} from the supplied DOM.
     * Searches through the tree beneath the given element to find an
     * element which is in the HDX namespace (for permissible syntax,
     * see {@link HdxResourceType}).  This method expects to find only
     * a single HDX element in the DOM tree it receives: if there is
     * more than one, it returns null.
     *
     * <p>The DOM which this builds contains only the element types
     * which are defined in the HDX namespace, but these elements are
     * not declared to be in any namespace within the normalised DOM
     * (that is, there are no prefixes).  The new DOM is backed by the
     * old one, so that changes in the new one also appear in the old
     * one.
     *
     * <p>The Element argument should contain one or more HDX objects
     * (that is, elements representing an <code>&lt;hdx&gt;</code>
     * element in the HDX namespace).  However, as a special case, if
     * it contains only elements which are the <em>content</em> of HDX
     * objects, then they are put inside a new HDX object if that can
     * be done unambiguously.  This is a heuristic fix, and its
     * behaviour may change in future.
     *
     * <p>If the element argument is both an HDX-type element (that
     * is, <code>&lt;hdx&gt;</code> or <code>&lt;ndx&gt;</code> or the
     * like) and is in no namespace, then this method will not look
     * for the HDX namespace in the elements below, and indeed will
     * ignore elements in the HDX namespace.  Otherwise, the method
     * will examine <em>only</em> elements and attributes in the HDX
     * namespace.
     *
     * @param el An element in or beneath which there is the XML which
     * represents a single HDX object.
     *
     * @return an HdxContainer, or null if there is not exactly one
     * HDX element in the DOM.
     *
     * @throws HdxException if there is an unexpected problem
     * normalizing the DOM.
     */
    public HdxContainer newHdxContainer(Element el)
            throws HdxException {
        // NB!!! The behaviour documented above is actually
        // implemented by the HdxElement.constructHdxElementTree
        // method, which is called if (and only if) the validateHdxDOM
        // method needs to normalize the tree.  That method must match
        // this documentation.
        Element hdx = validateHdxDOM(el);
        if (hdx == null)
            return null;
        else
        {
            if (hdx.getNextSibling() != null) {
                System.err.println
                    ("Warning: element given to newHdxContainer included more than one HDX");
                return null;
            }
            return new DomHdxContainer(hdx);
        }
    }

    /**
     * Registers a handler for {@link HdxResourceType}.  Handlers
     * should be installed this way rather than directly with the
     * corresponding object in <code>HdxResourceType</code>, so that
     * we can manage <em>de</em>registering them later.
     *
     * @param res The HdxResourceType which this handler will deal with
     *
     * @param factory The factory which will be invoked when this
     * handler is needed.
     */
    public static void registerHdxResourceFactory (HdxResourceType res,
                                        HdxResourceFactory factory) {
        res.registerHdxResourceFactory(factory);
    }

    /**
     * Constructs the Java object which corresponds to the given
     * element.  This finds the correct constructor, resolves the
     * necessary URIs and URLs, supplying any necessary context when
     * doing so, then invokes the real constructor.
     *
     * <p>The returned object is checked to correspond to the type registered
     * using {@link HdxResourceType#setConstructedClass}.
     *
     * @param el An element in the HDX DOM.
     *
     * @throws HdxException if the given element is somehow inconsistent
     * @return an object of the appropriate type, or null on any error
     */
    public Object getObject(Element el)
            throws HdxException {
        // We can keep a handle on any necessary additional context by
        // running up the element tree and keying further context on
        // the Document root.
        HdxResourceType hdxType = HdxResourceType.match(el);
        if (hdxType == HdxResourceType.NONE)
            return null;

        if (el.hasAttribute("uri") && !el.hasAttribute("url")) {
            // Try to resolve the URI to a URL, since it is the URL
            // that the handler is required to examine.  If we fail,
            // still pass the element to the handler, since the
            // handler is allowed to look at the URI as well, and it
            // might still work
            try {
                URI uri = new URI(el.getAttribute("uri"));
                URL url = fullyResolveURI(uri);
                if (url != null)
                    if (el instanceof HdxElement)
                        ((HdxElement)el)
                            .setAttribute("url", url.toString(), false);
                    else
                        el.setAttribute("url", url.toString());
            } catch (HdxException ex) {
                System.err.println("Failed to resolve URI: " + ex);
            } catch (URISyntaxException ex) {
                System.err.println("Invalid URI: " + ex);
            }
        }
        Object ret = null;
        if (el instanceof HdxNode)
            ret = ((HdxNode)el).getNodeObject();
        if (ret == null)
            ret = hdxType.getObject(el);

        return ret;
    }

    /**
     * Completely resolves a URI into a URL.  This performs both
     * relative to absolute URI resolution, and URI to URL resolution,
     * in both cases supplying all required context.  The result is an
     * absolute URL.
     *
     * <p>The natural-looking URI "file:something.xml" is formally
     * parsed as a <code>file</code> URI with a non-hierarchical
     * scheme-specific part.  As a special case, we interpret this
     * here as being relative to the current directory, and we allow
     * the <code>file</code> to be mixed-case.
     *
     * <p>XXX This will almost certainly need adjustment when we come
     * to deal with URIs which don't have absolute URLs definable
     * (such as the famous URI-in-a-jar case)
     *
     * <p>XXX Do we want to make this private?  Probably, when we add
     * the URI-to-URL resolution and have a bit of a rethink then.
     *
     * @return a URL corresponding to an absolute URI, or null if the
     * URI cannot be resolved for some reason.
     *
     * @throws HdxException if any of the URI or URL exceptions are
     * thrown, which should happen only if there is some syntactic
     * problem with the input URI.
     */
    public URL fullyResolveURI (URI uri)
            throws HdxException {
        try {
            // Basic resolution of the URI into an absolute one
            String scheme = uri.getScheme();
            URI absuri;
            if (scheme == null || scheme.toLowerCase().equals("file")) {
                URI dirbase;
                String ssp = uri.getSchemeSpecificPart();
                String frag = uri.getFragment();
                String currdir = new java.io.File("").getAbsolutePath();
                dirbase = new URI("file:" + currdir + '/');
                absuri = dirbase.resolve(frag==null
                                         ? ssp
                                         : ssp+'#'+frag);
            } else
                absuri = uri;

            assert absuri.isAbsolute();

            // Here we would resolve the URI to a URL

            return absuri.toURL();
        } catch (java.net.MalformedURLException ex) {
            throw new HdxException("Malformed URL: " + ex);
        } catch (java.net.URISyntaxException ex) {
            throw new HdxException ("URI syntax: " + ex);
        }
    }

    /**
     * Completely resolves a URI into a URL.  This performs both
     * relative to absolute URI resolution, and URI to URL resolution,
     * in both cases supplying all required context.  The result is an
     * absolute URL.
     *
     * @param uri A URI as a String
     *
     * @return a URL corresponding to an absolute URI, or null if the
     * URI cannot be resolved for some reason.
     *
     * @throws HdxException if any of the URI or URL exceptions are
     * thrown, which should happen only if there is some syntactic
     * problem with the input URI.
     *
     * @see #fullyResolveURI(URI)
     */
    public URL fullyResolveURI (String uri)
            throws HdxException {
        try {
            return fullyResolveURI(new URI(uri));
        } catch (java.net.URISyntaxException ex) {
            throw new HdxException ("URI syntax: " + ex);
        }
    }

    /** 
     * Checks that the given DOM is normalized, and normalizes it if
     * necessary.
     *
     * <p>This is the method which is the `gatekeeper' for the
     * package, in the sense that it is responsible for ensuring that
     * any DOM depended on by this package is valid.  That's easy,
     * because this method implicitly gets to define what counts as
     * valid.  This is why we don't need any DTD for the HDX XML
     * format.
     *
     * <p>We use {@link HdxElement#constructHdxElementTree}, which
     * returns an <code>HdxElement</code> which may have siblings.  We
     * don't remove these siblings here, but leave that to the caller
     * to do if it wants.
     *
     * <p>Valid, here, means acceptable to {@link
     * HdxResourceType#isValidHdx(Element)}.
     *
     * @param hdxElement an element to be checked, and normalized if
     * necessary.  The input DOM is unchanged.
     *
     * @return a valid HDX Element, which will not be the same as the
     * input one, if that had to be normalized.  If there is no HDX
     * structure in the DOM, even after normalization, then return
     * null.
     */
    private Element validateHdxDOM (Element hdxElement) {
        // We do not need to pay any attention to namespaces, since
        // the normalization process below should take care of all
        // that for us, so that after normalization, the only elements
        // left are elements in the HDX namespace.
        if (HdxResourceType.HDX.isValid(hdxElement))
            // nothing to do
            return hdxElement;

        // We haven't found anything immediately, so try
        // normalizing the DOM
        DocumentFragment df = HdxElement.constructHdxElementTree(hdxElement);
        if (df == null || !df.hasChildNodes())
            // There was no HDX structure in the DOM -- nothing in the
            // HDX namespace.
            return null;

        System.err.println("validateHdxDOM: DocumentFragment contents:");
        for (Node kid = df.getFirstChild();
             kid != null;
             kid = kid.getNextSibling()) {
            System.err.println("--->  "
                               + serializeDOM(kid));
        }
        System.err.println("...validateHdxDOM done");

        assert HdxResourceType.HDX.isValid((Element)df.getFirstChild());

        return (Element)df.getFirstChild();
    }


    /**
     * Serialize a DOM to a String.  Debugging method. 
     *
     * @return String, or an empty string on error.
     */
    public static String serializeDOM (Node n) {
        String ret;
        try {
            // Create a new transformer, rather than reusing the
            // global one, which is initialised to work with the
            // normalizing XSLT script.
            Transformer trans
                = TransformerFactory
                .newInstance()
                .newTransformer();
            java.io.StringWriter sw = new java.io.StringWriter();
            trans.transform(new DOMSource(n), new StreamResult(sw));
            ret = sw.toString();
        } catch (TransformerConfigurationException ex) {
            System.err.println("Can't transform DOM: " + ex);
            ret = "";
        } catch (TransformerException ex) {
            System.err.println("Can't transform DOM: " + ex);
            ret = "";
        }
        return ret;
    }

//     /**
//      * Normalises the DOM tree, exposing only elements in the NDX
//      * namespace.
//      *
//      * <p>This process results in a valid HDX DOM.
//      *
//      * <p>The transformation is done using XSLT.  The XSLT script is
//      * specified in the property <code>ndx.normalizer</code>, and
//      * should point to the file
//      * <code>.../support/normalise-ndx.xslt</code> relative to the
//      * distribution (at present).  This file can be specified in a
//      * property file called <code>ndx.prop</code> in the current
//      * directory, or specified as a system property on the java
//      * command line.
//      * 
//      * <p>This method, along with its companions {@link
//      * #initialiseTransformer} and {@link #transformNdx}, is not
//      * necessarily a final resolution to the problem of navigating
//      * the DOM tree in the presence of namespaces and the `virtual'
//      * elements represented by <code>ndx:name</code> and friends.  Its big
//      * advantage is that it's clear what's happening -- the document
//      * is being normalised to a view where only the ndx namespace
//      * elements are present, which means that navigating through it
//      * afterwards is programmatically and conceptually simple -- but
//      * the disadvantages are (a) it's rather slow to create the
//      * transformer; (b) we abandon the parts of the input document
//      * which aren't in the namespace, so we can't round-trip
//      * documents through this process; (c) there are a couple of
//      * surprises to do with default namespaces -- specifically, note
//      * that default namespaces do <em>not</em> apply to attributes:
//      * <pre>
//      * &lt;x xmlns="http://example.org/NS">
//      * &lt;foo bar="hello">
//      * &lt;/x>
//      * </pre>
//      * Element <code>foo</code> is in the given namespace, but
//      * attribute <code>bar</code> isn't.
//      * 
//      * <p>None of these are killing problems.  (a) we can cope with,
//      * and since the transformer is a static object, the start up
//      * cost could be amortized quite effectively; (b) isn't a problem 
//      * since this is only intended to be used for reading XML
//      * specifications, so we don't need to round-trip documents
//      * carrying other elements unknown to this system.  (c) is an
//      * unavoidable consequence of the XML Namespace definition, which 
//      * we simply have to be slightly careful of, and warn folk not to
//      * start being too clever.
//      * 
//      * <p>The alternative is to deal with the namespace trickery
//      * up-front, by making the methods which root around the tree very much
//      * cleverer.  The problem with that is that I believe we'd have
//      * to add similar cleverness in a variety of places, which is
//      * errorprone and potentially confusing.
//      * 
//      * @throws HdxException if there is a problem locating or using
//      * the XSLT transformation script.
//      *
//      * @return the normalised DOM tree.
//      */
//     private Document normalizeHdx (Document dom)
//             throws HdxException {
//         // The location of the XSLT script which the transformer uses.
//         URL normalizeHdxXslt = 
//             getClass().getResource("support/normalize-hdx.xslt");

//         if (normalizeHdxXslt == null)
//             throw new HdxException("No value for property hdx.normalizer");

//         Document newdom;
//         try {

//             if (transformer == null)
//                 initializeTransformer(new File(normalizeHdxXslt.getFile()));

//             System.err.println("HdxFactory.normalizeHdx: from DOM:"
//                                + serializeDOM(dom));

//             newdom = transformNdx (dom);

//             System.err.println("  ... to DOM: "+ serializeDOM(newdom));

//         } catch (javax.xml.transform.TransformerException e) {
//             throw new HdxException("XSLT error: " + e);
//         }

//         if (! HdxResourceType.isValidHdx(newdom))
//             throw new HdxException("Normalization failed");

//         return newdom;
//     }

//     /** Creates a new XSLT transformer.  
//      *
//      * <p>Constructs the transformer lazily.  Idempotent.
//      * @throws HdxException if there is a problem initialising the
//      * transformer.
//      */
//     private static void initializeTransformer (File stylesheet)
//             throws HdxException {

//         if (transformer != null)
//             return;             // let us be called repeatedly

//         try {

//             StreamSource xsltScript = new StreamSource(stylesheet);
//             transformer = TransformerFactory
//                 .newInstance()
//                 .newTransformer(xsltScript);

//         } catch (javax.xml.transform.TransformerConfigurationException e) {
//             throw new HdxException ("Error initialising Transformer: " + e);
//         }
//     }

//     /** Performs the XSLT transformation on the given DOM tree.
//      * @return a new transformed DOM tree.
//      * @throws javax.xml.transform.TransformerException if the
//      * transformer fails.
//      * @see javax.xml.transform.Transformer
//      */
//     private static Document transformNdx (Document doc)
//             throws TransformerException  {

//         DOMSource source = new DOMSource(doc);
//         DOMResult result = new DOMResult();

//         transformer.transform(source, result);
//         return (Document)result.getNode();
//     }
}
