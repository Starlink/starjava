package uk.ac.starlink.hdx;

import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.URLUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.File;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

/**
 * Marshalls all the object creation factories involved
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
 * the HdxContainer from it using the {@link
 * #newHdxContainer(Element)} method.</p>
 *
 * <p>This class, or rather the group of <code>newHdxContainer</code>
 * methods, is also the gatekeeper for the Hdx system, in the sense
 * that it is this class which ensures that the
 * <code>HdxContainer</code> which it produces is in a normalised
 * form, and is valid.  The Hdx validation is that defined by the
 * static method {@link HdxResourceType#isValidHdx(Document)} and the
 * validators for the individual types, {@link
 * HdxResourceType#isValid(Element)}.</p>
 *
 * <p>Instances of this class also provide the facilities to resolve
 * URIs into URLs, supplying whatever context is required in the form
 * of base URIs.  They do this using the {@link #fullyResolveURI}
 * method.  Since the context required will typically depend on the
 * DOM context, you should find an appropriate factory using {@link
 * #findFactory}.</p>
 *
 * <p>To add an extension class, which handles a new file type, you
 * must do two things:
 * <ol>
 *
 * <li>Define a class which implements {@link HdxDocumentFactory}, and
 * which registers itself as such a factory, through a call to {@link
 * #registerHdxDocumentFactory}, in a static initialiser.
 *
 * <li>If the new class is called <code>my.new.type</code>, and if
 * there is an `Hdx property'
 * <code>HdxDocumentFactory.load.my.new.type</code> with the value
 * <code>true</code> (or anything which the <code>Boolean</code> class
 * evaluates to true), then the specified class is loaded during
 * initialization of this <code>HdxFactory</code> class.  This is an
 * `Hdx property', which means that it may be specified as either a
 * System property, or in an Hdx property file as described in {@link
 * HdxProperties}.</p>
 *
 * </ol>
 * @author Norman Gray
 * @version $Id$ 
 */
public class HdxFactory {

    /** Singleton instance of this class */
    private static HdxFactory defaultInstance = null;

    /** List of HdxDocumentFactory instances, tried in turn. */
    private static java.util.List hdxFactoryList = new java.util.ArrayList(3);

    private static Logger logger = Logger.getLogger("uk.ac.starlink.hdx");

    /**
     * Maps Node instances (usually Elements) to a factory which can
     * look after the trees beneath them, and resolve URIs with the
     * correct context.  This is a {@link WeakHashMap} so that the
     * presence of a Node as a key in this map doesn't prevent that
     * Node from being garbage-collected.
     *
     * <p>This isn't ideal, because this map is keyed on object
     * identity (that is, <code>==</code> rather than
     * <code>.equals()</code>), so it loses if we clone the element in
     * question.
     */
    private static java.util.Map factoryMap = new java.util.WeakHashMap();

    /**
     * The base URI which this factory uses to resolve URIs.
     */
    private static URI factoryBaseURI;
    
    static {
        // Any exceptions are converted to (unchecked) PluginException
        String classname = "...eh?";// obligatory initialisation
        try {
            /*
             * Add default HdxDocumentFactory instances.  Note that we
             * force this property to be true, so that there's no way
             * of avoiding this DocumentFactory from being loaded.
             * This is probably a good idea, but if we decided this
             * should preserve any preexisting value, there's nothing
             * that would break simply because of that.
             */
            HdxProperties.setProperty
          ("HdxDocumentFactory.load.uk.ac.starlink.hdx.XmlHdxDocumentFactory",
           "true");
        
            // Work through any list of properties HdxDocumentFactory.load.*...
            String prefix = "HdxDocumentFactory.load.";
            for (java.util.Enumeration e
                         = HdxProperties.getProperties().propertyNames();
                 e.hasMoreElements(); ) {
                String propname = (String)e.nextElement();
                if (propname.startsWith(prefix)) {
                    classname = propname.substring(prefix.length());
                    Boolean loadflag = Boolean.valueOf
                            (HdxProperties.getProperty(propname));
                    if (logger.isLoggable(Level.CONFIG))
                        logger.config
                                ("HdxDocumentFactory class " + classname + ":"
                                 + (loadflag.booleanValue()?"LOAD":"NOLOAD"));
                    if (loadflag.booleanValue()) {
                        /*
                         * Get the Class object corresponding to the
                         * class named in this property.  If the class
                         * has not already been loaded, then this will
                         * find, load, link, and initialise the class.
                         * It is the initialisation of the class that
                         * does the work of registration.
                         *
                         * The call to getSystemClassLoader looks
                         * redundant, as forName is documented to use
                         * this class loader if the third argument is
                         * null.  However, this appears not to work
                         * for some reason.  It's no problem, though,
                         * since (a) it's good to make this explicit,
                         * (b) this should work better in the somewhat
                         * odd environment of JUnit regression tests,
                         * and (c) in the potentially very odd
                         * environment of dynamic network class
                         * loading.
                         */
                        Class newclass = 
                            Class.forName (classname, // class name
                                           true,      // yes, initialise it
                                           Thread.currentThread().getContextClassLoader());
                        //ClassLoader.getSystemClassLoader());


                    }
                }
            }
        } catch (HdxException ex) {
            throw new PluginException("Static initialiser threw HdxException: "
                                      + ex);
        } catch (java.util.NoSuchElementException ex) {
            throw new PluginException("Ooops, runaway Enumeration!:" + ex);
        } catch (ExceptionInInitializerError ex) {
            throw new PluginException
                ("Failed to initialize class " + classname + " (" + ex + ")");
        } catch (LinkageError ex) {
            throw new PluginException
                ("Failed to load class " + classname + " (" + ex + ")");
        } catch (ClassNotFoundException ex) {
            throw new PluginException
                ("Failed to find class " + classname + " (" + ex + ")");
        }
    }

    private HdxFactory(URI base) {
        factoryBaseURI = base;
    }

    /**
     * Obtains an instance of the <code>HdxFactory</code>.
     */
    public static HdxFactory getInstance() {
        if (defaultInstance == null)
            defaultInstance = new HdxFactory(null);
        return defaultInstance;
    }

    /**
     * Obtains the factory instance which should handle a particular
     * element.
     *
     * @param el indicates the DOM context which the returned factory
     * is to service
     *
     * @return a HdxFactory instance which can resolve URIs appropriately
     */
    public static HdxFactory findFactory(Node el) {
        for (Node n = el; n != null; n = n.getParentNode()) {
            if (factoryMap.containsKey(el)) {
                return (HdxFactory)factoryMap.get(el);
            }
        }
        return getInstance();
    }

    /**
     * Adds a {@link HdxDocumentFactory} to the list of factories
     * tried by {@link #newHdxContainer}.  The factory is
     * added at the beginning of the list of factories tried.
     *
     * @param factory an object implementing the {@link
     * HdxDocumentFactory} interface.
     */
    public static void registerHdxDocumentFactory(HdxDocumentFactory factory) {
        hdxFactoryList.add(0, factory);
        if (logger.isLoggable(Level.CONFIG))
            logger.config("Registered HdxDocumentFactory "
                          + factory.getClass().getName());
    }


    /**
     * Constructs a new {@link HdxContainer} from the supplied URI.
     * The resulting <code>HdxContainer</code> is normalised as
     * described in {@link #newHdxContainer(Element,URI)}.
     *
     * @param uri a <code>URI</code> pointing to the resource
     * @return a new <code>HdxContainer</code>, or null if there
     *         is no handler available to parse the given URI
     * @throws HdxException if there is an unexpected problem creating
     *         the <code>HdxContainer</code> (that is, if we
     *         <em>ought</em> to be able to handle this URI, but fail
     *         for some reason)
     */
    public HdxContainer newHdxContainer(URI uri)
            throws HdxException {
        return newHdxContainer(fullyResolveURI(uri, null));
    }

    /**
     * Constructs a new {@link HdxContainer} from the supplied URL.
     * The resulting <code>HdxContainer</code> is normalised as
     * described in {@link #newHdxContainer(Element,URI)}.
     *
     * @param url a <code>URL</code> pointing to the resource
     * @return a new <code>HdxContainer</code>, or null if there
     *         is no handler available to parse the given URI
     * @throws HdxException if there is an unexpected problem creating
     *         the <code>HdxContainer</code> (that is, if we
     *         <em>ought</em> to be able to handle this URL, but fail
     *         for some reason)
     */
    public HdxContainer newHdxContainer(URL url)
            throws HdxException {
        try {
            Document hdxdom = null;
            // In case url is relative, resolve it
            url = fullyResolveURI(URLUtils.urlToUri(url), null);
        
            for (Iterator fi = hdxFactoryList.iterator();
                 hdxdom == null && fi.hasNext();
                 ) {
                HdxDocumentFactory factory = (HdxDocumentFactory)fi.next();
                hdxdom = factory.makeHdxDocument(url);
                if (hdxdom != null && logger.isLoggable(Level.FINE))
                    logger.fine("newHdxDocumentFactory(url=" + url
                                + "): success from HdxDocumentFactory "
                                + factory.getClass().getName());
            }

            if (hdxdom == null)
                // we fell out of the end of the loop -- no good handlers
                return null;

            Element docelem = hdxdom.getDocumentElement();
            if (logger.isLoggable(Level.FINE))
                logger.fine("newHdxContainer(" + url + ")  docelem="
                            + (docelem==null ? "null" : docelem.getTagName()));
        
            if (docelem == null)
                // It's XML Jim, but not as we know it
                return null;

            return newHdxContainer(docelem, URLUtils.urlToUri(url));
        } catch (java.net.MalformedURLException e) {
            throw new HdxException("URL " + url
                                   + " is malformed");
        }
    }

    /**
     * Constructs a new {@link HdxContainer} from the supplied DOM.
     *
     * <p>Equivalent to <code>newHdxContainer(el, null)</code>
     *
     * @param el an element in or beneath which there is the XML which
     * represents a single HDX object
     *
     * @return an <code>HdxContainer</code>, or null if there is not exactly one
     * HDX element in the DOM
     *
     * @see #newHdxContainer(Element,URI)
     */
    public HdxContainer newHdxContainer(Element el)
            throws HdxException {
        return newHdxContainer(el, null);
    }
    
    /**
     * Constructs a new {@link HdxContainer} from the supplied DOM.
     * Searches through the tree beneath the given element to find an
     * element which is in the HDX namespace (for permissible syntax,
     * see {@link HdxResourceType}).  This method expects to find only
     * a single HDX element in the DOM tree it receives: if there is
     * more than one, it returns null.
     *
     * <p>The DOM which this builds is normalised, in the sense
     * that:</p>
     * <ul>
     *
     * <li>it contains only the element types
     * which are defined in the HDX namespace; and</li>
     *
     * <li>these elements are not declared to be in any namespace
     * (that is, there are no prefixes).</li>
     *
     * </ul>
     *
     * <p>The new DOM is backed by the old one, so that changes in the
     * new one also appear in the old one.
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
     * namespace.  As a special case, if the element is in the HDX namespace,
     * then any unprefixed attributes on the element are taken to be
     * in that namespace also.  This is a contradiction to the XML
     * standard, which states that unprefixed attributes are in
     * <em>no</em> namespace, not even the default one.  However,
     * there are no reasonable cases where this behaviour is useful,
     * and it is massively confusing, so this constitutes best
     * practice.
     *
     * @param el an element in or beneath which there is the XML which
     * represents a single HDX object
     * @param systemId the system identifier for this element, or null
     * if none is defined
     *
     * @return an <code>HdxContainer</code>, or null if there is not exactly one
     * HDX element in the DOM
     *
     * @throws HdxException if there is an unexpected problem
     * normalizing the DOM
     */
    public HdxContainer newHdxContainer(Element el, URI systemId)
            throws HdxException {
        /*
         * NB!!! The behaviour documented above is actually
         * implemented by the (package-private)
         * HdxElement.constructHdxElementTree method, which is called
         * by validateHdxDOM if (and only if) it needs to normalize
         * the tree, and which it does only if the tree is not
         * currently valid.  THAT method must match this
         * documentation.
         *
         * If there are any further assertions that should be
         * guaranteed, then they should be done by modifying the HDX
         * validator in HdxResourceType.HDX.setElementValidator to
         * make the HDX invalid unless those assertions are true.
         */
        Element hdx = validateHdxDOM(el);

        if (hdx == null)
            return null;
        else
        {
            if (hdx.getNextSibling() != null) {
                if (logger.isLoggable(Level.WARNING))
                    logger.warning
                            ("Warning: element given to newHdxContainer included more than one HDX");
                return null;
            }
            if (systemId != null) {
                /*
                 * This factory, with its baseURI context, should look
                 * after resolution/creation of the tree under this
                 * element
                 */
                HdxFactory newfactory = new HdxFactory(systemId);
                factoryMap.put(el, newfactory);
            }
            return new DomHdxContainer(hdx);
        }
    }

    /**
     * Constructs a new <code>HdxContainer</code> which wraps an
     * <code>HdxFacade</code>.
     *
     * <p>Unlike the other <code>newHdxContainer</code> methods, this
     * does not guarantee to normalise the (implied) input DOM
     * immediately, and this may be postponed until the (implied)
     * resulting Hdx DOM is produced at a later stage.  It is only
     * whenever this normalisation is finally done that any errors in
     * the input DOM will materialise.  However, since the
     * <code>HdxFacade</code> is a synthetic view of an underlying
     * structure, under control of the same code which is ultimately
     * responsible for validating the input DOM, one can presume that
     * any normalisation errors are bugs in that code.  Thus this
     * method either succeeds or thows an exception, and does not
     * return null on any error.
     *
     * <p>This method is part of the preferred route for generating
     * XML from a Java object which is part of the HDX world.  Given
     * that the object, <code>obj</code> has a
     * <code>getHdxFacade</code> method, or equivalent, the route is:
     * <pre>
     * Element el = HdxFactory
     *     .getInstance()  // or some suitable .findFactory(...)
     *     .newHdxContainer(obj.getHdxFacade())
     *     .getDOM(null) ; // or a suitable base URI
     * </pre>
     * Although this seems roundabout, (a) this method is written to
     * be efficient with this route in mind, and (b) this ensures that
     * the DOM or Source produced by the <code>HdxFacade</code> is
     * processed for output in a way consistent with other XML
     * generated by this package.
     *
     * @param facade an <code>HdxFacade</code> which represents an
     * underlying object
     *
     * @return an <code>HdxContainer</code>
     *
     * @throws HdxException if there is some problem constructing the DOM
     */
    public HdxContainer newHdxContainer(HdxFacade facade)
            throws HdxException {
        if (facade == null)
            throw new IllegalArgumentException
                    ("null facade given to newHdxContainer");
        HdxDocument hdxdoc = (HdxDocument)HdxDOMImplementation
                .getInstance()
                .createDocument(null, "hdx", null);
        Element hdx = hdxdoc.createElement("hdx");
        hdxdoc.appendChild(hdx);
        Element kid = hdxdoc.createElement(facade);
        hdx.appendChild(kid);

        /*
         * We return this directly, by calling DomHdxContainer(hdx).
         * That doesn't (at present) walk through the DOM, thus
         * constructing the DOM corresponding to the Facade.  We don't
         * have to validate it, since one supposes that the facade
         * represents a valid DOM.
         *
         * There's some potential for inefficiency here, especially if
         * the implied DOM is large.  It will not be hard to postpone
         * this, so that if the HdxContainer is asked for a Source,
         * say, then we can immediately extract a Source from the
         * facade.  The disavowal of any normalisation guarantee in
         * the method docs gives us this freedom.
         *
         * This method is documented as being the preferred (since
         * efficient) way of creating XML from objects, but it has not
         * yet received much attention to efficiency in fact.
         */

        return new DomHdxContainer(hdx);
    }

    /**
     * Recovers the Java object which corresponds to the given
     * element.  This finds the correct underlying method, resolves the
     * necessary URIs and URLs, supplying any necessary context when
     * doing so, then invokes the real accessor.
     *
     * <p>The DOM element should be regarded as the master
     * representation of the data object, thus it is the Element which
     * client code should generally hold on to, rather than the Java object,
     * which can be extracted from the element using this method at
     * any time.  That is why this is a <code>get...</code> method
     * rather than being referred to as a constructor.
     *
     * <p>The returned object is checked to correspond to the type registered
     * using {@link HdxResourceType#setConstructedClass}.
     *
     * @param el an element in the Hdx DOM
     *
     * @return an object of the appropriate type, or null on any error
     *
     * @throws HdxException if the given element is somehow inconsistent
     *
     * @throws PluginException (unchecked exception) if the
     * underlying code returned an object which did not match the
     * registered target class obtained from {@link
     * HdxResourceType#getConstructedClass}, if that is non-null
     */
    public Object getObject(Element el)
            throws HdxException {
        /*
         * We can keep a handle on any necessary additional context by
         * running up the element tree and keying further context on
         * the Document root.
         */
        HdxResourceType hdxType = HdxResourceType.match(el);
        if (hdxType == HdxResourceType.NONE)
            return null;

        if (el.hasAttribute("uri") && !el.hasAttribute("url")) {
            /*
             * Try to resolve the URI to a URL, since it is the URL
             * that the handler is required to examine.  If we fail,
             * still pass the element to the handler, since the
             * handler is allowed to look at the URI as well, and it
             * might still work
             */
            try {
                URI uri = new URI(el.getAttribute("uri"));
                URL url = fullyResolveURI(uri, el);
                if (url != null)
                    if (el instanceof HdxElement)
                        /*
                         * we are able to call the
                         * setAttribute(String,String,boolean) method
                         * which avoids changing the backing DOM
                         */
                        ((HdxElement)el)
                            .setAttribute("url", url.toString(), false);
                    else
                        el.setAttribute("url", url.toString());
            } catch (HdxException ex) {
                if (logger.isLoggable(Level.WARNING))
                    logger.warning("Failed to resolve URI: " + ex);
            } catch (URISyntaxException ex) {
                if (logger.isLoggable(Level.WARNING))
                    logger.warning("Invalid URI: " + ex);
            }
        }
        Object ret = null;
        if (el instanceof HdxNode)
            ret = ((HdxNode)el).getNodeObject();
        if (ret == null)
            /*
             * It's this call which can throw PluginException if the
             * (Java) type of the returned object doesn't match the
             * target type required by the HdxResourceType we're
             * aiming for.
             */
            ret = hdxType.getObject(el);

        return ret;
    }

    /**
     * Completely resolves a URI into a URL.  This performs both
     * relative to absolute URI resolution (if required), and URI to URL conversion,
     * in both cases supplying all required context.  The result is an
     * absolute URL.
     *
     * <p>The relative-to-absolute resolution is performed using the
     * base URI corresponding to the given context node, as specified
     * by <a href='http://www.w3.org/TR/xmlbase/#resolution' >XML Base
     * Recommendation, section 4</a>.
     *
     * <p>If no base URI can be determined, then the method uses a
     * last-ditch base URI consisting of the <code>file:</code> URI
     * corresponding to the current directory (as obtained from the
     * System property <code>user.dir</code>).
     *
     * <p>Note that the natural-looking URI
     * <code>file:filename.xml</code> is an <em>absolute</em> URI,
     * according to <a href='http://www.ietf.org/rfc/rfc2396.txt' >RFC
     * 2396</a>, <em>Uniform Resource Identifiers (URI): Generic
     * Syntax</em> (since it has a non-null <code>scheme</code> part).
     * Such URIs should be avoided, but since they are common, and
     * intended to be relative URIs, we special-case this, in a
     * <strong>deviation from RFC 2396</strong> by removing the
     * <code>scheme</code> part (this conforms with the remarks on
     * backward compatibility in RFC 2396, section 5.2, step 3,
     * although, as blessed in that section) we do not extend this
     * latitude to other schemes).  We allow the <code>file</code>
     * scheme-specifier to be mixed-case.
     *
     * @param uri the URI which is to be resolved.  If this is
     * absolute already, it is merely converted to a URL.
     * @param context a DOM Node providing the context for any
     * resolution required; this may be null
     *
     * @return a URL corresponding to an absolute URI, or null if the
     * URI cannot be resolved for some reason.
     *
     * @throws HdxException if any of the URI or URL exceptions are
     * thrown, which should happen only if there is some syntactic
     * problem with the input URI.  Also thrown if the input URI was
     * relative, and it is impossible to determine a base URI to
     * resolve it against.
     */
    public URL fullyResolveURI (URI uri, Node context)
            throws HdxException {
        /*
         * This will need adjustment when we come to deal with URIs
         * which don't have absolute base URIs definable (such as the
         * famous URI-in-a-jar case).  Instead of adjusting this,
         * however, it might make more sense to enhance getBaseURI so
         * that it guarantees that it returns an absolute URI, if
         * necessary with some odd scheme to deal with the
         * URI-in-a-jar case.
         */
        try {
            URI baseURI;
            String scheme = uri.getScheme();

            if (scheme != null && scheme.toLowerCase().equals("file")) {
                // special case
                // replace uri with a new one, which doesn't have the scheme
                String ssp = uri.getSchemeSpecificPart();
                String frag = uri.getFragment();
                uri = new URI(null, ssp, frag);
            }

            if (uri.isAbsolute()) {
                return uri.toURL();
            }
            
            baseURI = getBaseURI(context);

            if (baseURI == null) {
                // last-ditch default: file URI referring to current directory,
		baseURI = new java.io.File("").getAbsoluteFile().toURI();
            }

            if (! baseURI.isAbsolute()) {
                // Not supported at present
                throw new HdxException("Can't find any base URI");
            }

            URI resolvedURI = baseURI.resolve(uri);
            if (logger.isLoggable(Level.FINE))
                logger.fine("fullyResolveURI: uri<" + uri
                            + "> + base<" + baseURI
                            + "> = <" + resolvedURI + ">");

            return resolvedURI.toURL();
            //return baseURI.resolve(uri).toURL();

        } catch (java.net.MalformedURLException ex) {
            throw new HdxException("Malformed URL constructed from URI!: "
                                   + ex);
        } catch (java.net.URISyntaxException ex) {
            throw new HdxException ("URI syntax: " + ex);
        }
    }

    /**
     * Completely resolves a URI, expressed as a string, into a URL.
     * This performs both relative to absolute URI resolution, and URI
     * to URL resolution, in both cases supplying all required
     * context.  The result is an absolute URL.
     *
     * @param uri a String holding the URI
     * @param context a DOM Node providing the context for any
     * resolution required; this may be null
     *
     * @return a URL corresponding to an absolute URI, or null if the
     * URI cannot be resolved for some reason.
     *
     * @throws HdxException if any of the URI or URL exceptions are
     * thrown, which should happen only if there is some syntactic
     * problem with the input URI.
     *
     * @see #fullyResolveURI(URI,Node)
     */
    public URL fullyResolveURI(String uri, Node context)
            throws HdxException {
        try {
            return fullyResolveURI(new URI(uri), context);
        } catch (java.net.URISyntaxException ex) {
            throw new HdxException ("URI syntax: " + ex);
        }
    }

    /**
     * Determines the base URI for a Node.  This is established using
     * the procedure defined in <a
     * href='http://www.ietf.org/rfc/rfc2396.txt' >RFC 2396</a>,
     * <em>Uniform Resource Identifiers (URI): Generic Syntax</em>,
     * section 5.1, summarised in the <a
     * href='http://www.w3.org/TR/xmlbase/#resolution' >XML Base
     * Recommendation, section 4</a>.
     *
     * <p>Somewhat unexpectedly, the definition of
     * relative-to-absolute resolution in RFC 2396 implies that if a
     * URI such as <code>file.xml</code>, starting with a relative path, is
     * resolved against a base URI with no path, such as
     * <code>http://x.org</code>, then the relative URI is simply
     * appended to the base URI's authority component, producing
     * <code>http://x.orgfile.xml</code>.  We avoid the inevitable
     * resulting problems by ensuring, here, that the returned base
     * URI does have a path component, by inserting <code>/</code> if
     * necessary.
     *
     * <p>This method is defined in this factory rather than in, say,
     * <code>HdxElement</code> because (a) it is applicable to any DOM
     * node, not just Hdx ones, and (b) it is this factory which
     * formally establishes the `context of the application' in
     * resolution step 4.
     *
     * @param the node whose base URI is to be established.  If null,
     * the method returns null without error
     * @return a base URI for the node.  This will typically be an
     * absolute URI, but will not necessarily be so.  If no base URI
     * can be determined, this will be null
     * @throws HdxException if there is some reason why the base URI
     * cannot be defined, or if one of the <code>xml:base</code>
     * attributes specifies a syntactically invalid URI
     */
    URI getBaseURI(Node n)
            throws HdxException {

        if (n == null)
            return null;

        URI base = null;
        Element referenceElement;
        
        /*
         * XML Base, 4.2: ``Relative URIs appearing in an XML document
         * are always resolved relative to either an element, a
         * document entity, or an external entity.'' Determine the
         * referenceElement, or leave this null to use the
         * document's base URI.
         */
        switch (n.getNodeType()) {
          case Node.ELEMENT_NODE:
            referenceElement = (Element)n;
            break;

          case Node.ATTRIBUTE_NODE:
            if (n.getNodeName().equals("xml:base")) {
                /*
                 * XML Base, 4.3: The base URI for a URI reference
                 * appearing in an xml:base attribute is the base URI
                 * of the parent element of the element bearing the
                 * xml:base attribute, if one exists within the
                 * document entity or external entity, otherwise the
                 * base URI of the document entity or external entity
                 * containing the element.
                 */
                Node owner = ((Attr)n).getOwnerElement();
                assert owner != null;
                referenceElement = (Element)owner.getParentNode();
                // may be null, if this attribute was on the document element
            } else {
                /*
                 * XML Base, 4.3: The base URI for a URI reference
                 * appearing in any other attribute value, including
                 * default attribute values, is the base URI of the
                 * element bearing the attribute.
                 */
                referenceElement = ((Attr)n).getOwnerElement();
                assert referenceElement != null;
            }
            break;

          case Node.CDATA_SECTION_NODE:
          case Node.TEXT_NODE:
              {
                  /*
                   * XML Base, 4.3: The base URI for a URI reference
                   * appearing in text content is the base URI of the
                   * element containing the text.
                   */
                  Node parent = n.getParentNode();
                  assert parent != null;
                  assert parent.getNodeType() == Node.ELEMENT_NODE;
                  referenceElement = (Element)parent;
                  /*
                   * It's possible referenceElement is null after
                   * this, if this text was freestanding within the
                   * `XML' file.  Something bad is quite likely to
                   * happen to someone soon, but it's not our fault,
                   * and not our problem....
                   */
                  break;
              }

          case Node.PROCESSING_INSTRUCTION_NODE:
              {
                  /*
                   * XML Base, 4.3: The base URI for a URI reference
                   * appearing in the content of a processing
                   * instruction is the base URI of the parent element
                   * of the processing instruction, if one exists
                   * within the document entity or external entity,
                   * otherwise the base URI of the document entity or
                   * external entity containing the processing
                   * instruction.
                   */
                  Node parent = n.getParentNode();
                  if (parent == null) {
                      referenceElement = null;
                  } else {
                      assert parent.getNodeType() == Node.ELEMENT_NODE;
                      referenceElement = (Element)parent;
                  }
                  break;
              }

          case Node.DOCUMENT_NODE:
            referenceElement = null;
            break;

          case Node.ENTITY_NODE:
            // This is definable, but not at present supported
            return null;        // JUMP OUT
            
            // Odd or unsupported cases
          case Node.COMMENT_NODE:
          case Node.DOCUMENT_FRAGMENT_NODE:
          case Node.DOCUMENT_TYPE_NODE:
          case Node.ENTITY_REFERENCE_NODE:
          case Node.NOTATION_NODE:
            throw new HdxException
                    ("HdxFactory.getBaseURI: there is no base URI defined for nodes of type "
                     + DOMUtils.mapNodeType(n.getNodeType()));
            
          default:
            throw new HdxException("Impossible node type "
                                   + n.getNodeType() + '='
                                   + DOMUtils.mapNodeType(n.getNodeType()));
        }

        try {
            /*
             * Work up the tree, until we either run out of tree, or
             * find an absolute base URI
             */
             while (referenceElement != null) {
                String baseAtt = referenceElement.getAttribute("xml:base");
                if (baseAtt.length() != 0) {
                    /*
                     * base is either null (we haven't been here
                     * before) or it is a relative URI
                     */
                    if (base == null)
                        base = new URI(baseAtt);
                    else {
                        // resolve base against this new URI
                        URI newbase = new URI(baseAtt);
                        base = newbase.resolve(base);
                    }
                    if (base.isAbsolute()) {
                        // found it!
                        break; // JUMP OUT
                    }
                }
                
                Node parent = referenceElement.getParentNode();
                if (parent.getNodeType() == Node.ELEMENT_NODE) {
                    // normal case
                    referenceElement = (Element)parent;
                } else {
                    /*
                     * This might be a Document or possibly a
                     * DocumentFragment.  That's fine -- it means
                     * we've got to the top of this particular tree.
                     * Simply stop the search here. 
                     */
                    referenceElement = null;
                }
            }
        } catch (java.net.URISyntaxException ex) {
            throw new HdxException("Syntactically invalid URI in xml:base: "
                                   + ex);
        }
            
        if ((base == null || !base.isAbsolute()) && factoryBaseURI != null) {
            if (base == null)
                base = factoryBaseURI;
            else
                base = factoryBaseURI.resolve(base);
        }
        
        if (base == null)
            throw new HdxException
                    ("Can't determine base URI for document node");

        String basepath = base.getPath();
        if (basepath == null || basepath.length() == 0) {
            try {
                // Make sure that the base URI does have a path component
                base = new URI(base.getScheme(),
                               base.getAuthority(),
                               "/",
                               base.getQuery(),
                               base.getFragment());
            } catch (java.net.URISyntaxException ex) {
                throw new HdxException
                        ("Error inserting path to URI (shouldn't happen)"
                         + ex);
            }
        }

        return base;
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
     * <p>Valid, here, means acceptable to the {@link
     * HdxResourceType#isValid(Element)} method on
     * {@link HdxResourceType#HDX}.
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
        /*
         * We do not need to pay any attention to namespaces, since
         * the normalization process below should take care of all
         * that for us, so that after normalization, the only elements
         * left are elements in the HDX namespace.
         */
         if (HdxResourceType.HDX.isValid(hdxElement))
            // nothing to do
            return hdxElement;

        // We haven't found anything immediately, so try
        // normalizing the DOM
        DocumentFragment df = HdxElement.constructHdxElementTree(hdxElement);
        if (df == null || !df.hasChildNodes())
            /*
             * There was no HDX structure in the DOM -- nothing in the
             * HDX namespace. 
             */
            return null;

        if (logger.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer
                    ("validateHdxDOM: DocumentFragment contents:");
            for (Node kid = df.getFirstChild();
                 kid != null;
                 kid = kid.getNextSibling()) {
                sb.append("   ");
                sb.append(HdxDocument.NodeUtil.serializeNode(kid));
            }
            logger.fine(sb.toString());
        }

        HdxElement ret = (HdxElement)df.getFirstChild();
        assert HdxResourceType.HDX.isValid(ret);

        /*
         * We return this element still with its link to its parent.
         * This means (a) the DocumentFragment cannot yet be garbage
         * collected, but more importantly (b) if we are ever walking
         * up the tree below this, we will find ourselves in the
         * DocumentFragment, rather than in a Document.
         */
        return ret;
    }
}
