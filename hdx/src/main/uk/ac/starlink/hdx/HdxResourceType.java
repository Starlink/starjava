package uk.ac.starlink.hdx;

import java.util.*;
import java.util.logging.Level;
import java.net.URL;
import org.w3c.dom.*;

/**
 * Encapsulates the types of data contained within the HDX namespace.
 * Each of the instances of this class can be used as a type
 * indicator, which additionally encapsulates the various properties
 * that type has.
 *
 * <p>To add an extension class, which registers a new
 * HdxResourceType, you must do two things:
 * <ol>
 *
 * <li>Define a class which registers the new type, through a call to
 * {@link #newHdxResourceType}, in a static initialiser.
 *
 * <li>If the new class is called <code>my.new.type</code>, and if
 * there is an `Hdx property'
 * <code>HdxResourceType.load.my.new.type</code> with the value
 * <code>true</code> (or anything which the <code>Boolean</code> class
 * evaluates to true), then the specified class is loaded during
 * initialization of this <code>HdxResourceType</code> class.  This is an
 * `Hdx property', which means that it may be specified as either a
 * System property, or in an Hdx property file as described in {@link
 * HdxProperties}.</p>
 *
 * </ol>
 *
 * <p>Alternatively, you may register the type by explicitly loading
 * and initialising the class (and thus executing its static
 * initialiser).  This can happen as a result of a call to some static
 * method in the class, or explicitly via
 * <code>java.lang.Class.forName</code>.</p>
 *
 * <p>The structure of the elements within the HDX namespace is as
 * follows: 
 * XXX ADD STRUCTURE DEFINITION -- WHAT FORMAT?
 *
 * <p>There is no DTD which corresponds to this document structure.
 * The method which checks this is {@link #isValidHdx(Document)},
 * which is equivalent to a call to {@link #isValid(Element)} on
 * object {@link #HDX}.
 *
 * @author Norman Gray (norman@astro.gla.ac.uk)
 * @version $Id$
 */
public class HdxResourceType {

    private static java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger("uk.ac.starlink.hdx");

    /** The namespace for HDX */
    public static final String HDX_NAMESPACE = "http://www.starlink.ac.uk/HDX";

    /**
     * Name of attribute which indicates an HDX-namespace element.  If
     * an element has an attribute with this name, and in the HDX
     * namespace, then that element is taken to be an element with a
     * name given by the value of this attribute.  Thus the start-tag
     * <pre>
     * &lt;stuff xmlns:x="http://www.starlink.ac.uk/HDX"
     *   x:hdxname="ndx"
     *   x:uri="..." &gt;
     * </pre>
     * is taken to be equivalent to
     * <pre>
     * &lt;x:ndx x:uri="..."
     * xmlns:x="http://www.starlink.ac.uk/HDX"&gt;
     * </pre>
     * thus implementing a simple form of architectural processing,
     * which allows HDX-namespaced elements to be smuggled into
     * otherwise separate documents.  See also the {@link
     * #setHoistAttribute hoist attribute} processing.
     */
    static final String HDX_ARCHATT = "hdxname";

    /** The overall container of HDX objects */
    public static HdxResourceType HDX;

    /** A generic type for titles of HDX objects */
    public static HdxResourceType TITLE;

    /** Not a HDX-registered object, used as error/unknown type. */
    public static HdxResourceType NONE;
    
    /** 
     * Map of all types except NONE.  The map is keyed on (String)
     * <code>HdxResourceType.name</code>, with values of type
     * <code>HdxResourceType</code>.
     */
    private static Map resourceTypeMap;

    /**
     * The Java class which constructed objects must be assignable to.
     * Null if unknown.
     *
     * @see #getObject
     */
    private Class requiredClass;

    static {
        // Add HDX, TITLE and NONE, but don't put the latter into
        // resourceTypeMap.
        resourceTypeMap = new HashMap();

        try {
            NONE = newHdxResourceType("none");
            resourceTypeMap.remove("none");
            // XXX Should we add a `validator' to the NONE type, if
            // for no other reason than to document whether a NONE
            // object (ie, an unregistered object) is deemed valid or
            // not?  If so, should NONE be always-valid or
            // always-invalid?  Since a NONE element is by definition
            // unknown, we can't reasonably assert that it's invalid;
            // however, if we take isValid to be an assertion that `we
            // know about this element and it's OK', then it doesn't
            // make sense to call it valid.  Choosing a sensible
            // default here could short-circuit some tests.

            HDX = newHdxResourceType("hdx");
            HDX.setElementValidator(new ElementValidator() {
                public boolean validateElement(Element el) {
                    // An HDX is valid if all of its children are Element
                    // nodes; each one is recognised by
                    // HdxResourceType.match (ie, all are registered); and
                    // each is valid according to _its_ validator.
                    boolean log = logger.isLoggable(Level.FINE);
                    if (el == null)
                        throw new IllegalArgumentException
                                ("HDX.validateElement received null argument");
                    if (log)
                        logger.fine("validateElement("
                                    + HdxDocument.NodeUtil.serializeNode(el)
                                    + "):");
                    if (HdxResourceType.match(el) != HDX)
                        return false;
                    for (Node n = el.getFirstChild();
                         n != null;
                         n = n.getNextSibling()) {
                        if (n.getNodeType() != Node.ELEMENT_NODE) {
                            logger.fine("...child type=" + n.getNodeType()
                                        + " value=<"
                                        + n.getNodeValue() + ">");
                            return false;
                        }
                        HdxResourceType type = HdxResourceType.match((Element)n);
                        if (type == HdxResourceType.NONE) {
                            if (log)
                                logger.fine("...unrecognised child type <"
                                            + n.getNodeName() + ">");
                            return false;
                        }
                        if (!type.isValid((Element)n)) {
                            if (log) logger.fine("...child not valid");
                            return false;
                        }
                    }
                    if (log) logger.fine("...OK");
                    return true;
                }
            });
            HDX.setConstructedClass("uk.ac.starlink.hdx.HdxContainer");

            TITLE = newHdxResourceType("title");
            TITLE.setHoistAttribute("value");
            TITLE.setElementValidator(new ElementValidator() {
                public boolean validateElement(Element el) {
                    // A TITLE element is valid if it has an attribute "value"
                    return HdxResourceType.match(el) == TITLE
                        && el.hasAttribute(TITLE.getHoistAttribute());
                }
            });
        } catch (HdxException ex) {
            throw new PluginException
                ("Failed to initialise HDX properly.  What now?!");
        }

        // Work through any list of properties HdxResourceType.load.*...
        // [It might seem more natural to have the class to be loaded
        // as the _value_ of some property.  However, since property
        // names must be unique, we must distinguish the property
        // keys by some means.  A number is one possibility, but
        // using the class name is more robust.  In that case, the
        // information we actually want is now in the key and needn't
        // be duplicated in the value.  Having the true/false value
        // means that we can switch loading _off_ if that turns out to
        // be useful.
        String classname = "...eh?";// obligatory initialisation
        try {
            String prefix = "HdxResourceType.load.";
            for (Enumeration e = HdxProperties.getProperties().propertyNames();
                 e.hasMoreElements(); ) {
                String propname = (String)e.nextElement();
                if (propname.startsWith(prefix)) {
                    classname = propname.substring(prefix.length());
                    Boolean loadflag = Boolean.valueOf
                            (HdxProperties.getProperty(propname));
                    logger.config
                            ("HdxResourceType class " + classname + ":"
                             + (loadflag.booleanValue() ? "LOAD" : "NOLOAD"));
                    if (loadflag.booleanValue()) {
                        // Get the Class object corresponding to the class
                        // named in this property.  If the class has not
                        // already been loaded, then this will find, load,
                        // link, and initialise the class.  It is the
                        // initialisation of the class that does the work of
                        // registration.
                        //
                        // The call to getSystemClassLoader looks redundant,
                        // as forName is documented to use this class loader
                        // if the third argument is null.  However, this
                        // appears not to work for some reason.  It's no
                        // problem, though, since (a) it's good to make this
                        // explicit, (b) this should work better in the
                        // somewhat odd environment of JUnit regression tests,
                        // and (c) in the potentially very odd environment of
                        // dynamic network class loading.
                        //Class newclass = Class.forName
                        //        (classname, // class name
                        //         true, // yes, initialise it
                        //         ClassLoader.getSystemClassLoader());
                        Class.forName (classname, // class name
                                       true,      // yes, initialise it
                                       Thread.currentThread().getContextClassLoader());
                    
                    }    
                }
            }
        } catch (NoSuchElementException ex) {
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

    /**
     * The name of the resource type.  This (currently) serves for its
     * printable name as well as its XML element GI.
     */
    private final String name;

    /**
     * An attribute which is defaulted from the element content.
     * If this is null, no attribute is defaulted.
     *
     * @see #setHoistAttribute
     */
    private String hoistAttribute;

    /**
     * The list of object-construction handlers which is tried for
     * this resource type.
     */
    private List handlers;

    /**
     * An object which can validate Elements which claim to be of this type.
     */
    private ElementValidator validator;

    /**
     * Constructs a new Hdx resource type.  This is a private
     * constructor, so that all instances of this class are forced to
     * be made via {@link #newHdxResourceType}, ensuring that they are
     * properly registered, and that there are no two instances with
     * the same name.
     */
    private HdxResourceType(String name) {
        this.name = name;
    }

    /**
     * Creates and returns a new resource type.
     *
     * @param name The name of the new resource type, which will also
     * become its XML element name.  It is an error for this resource
     * type to already exist.
     *
     * @return a new resource object, or null if a type with that name
     * already exists.
     */
    public static HdxResourceType newHdxResourceType(String name) {
        if (resourceTypeMap.containsKey(name))
            return null;
        HdxResourceType t = new HdxResourceType(name);
        resourceTypeMap.put(name, t);
        logger.config("Registered new HDX resource type " + t);
        return t;
    }

    /** Returns a printable version of the resource type */
    public String toString() {
        return name;
    }

    /** 
     * Gives the XML element name corresponding to the resource type.
     */
    public String xmlName() {
        return name;
    }

    /**
     * Retrieves the value of the `hoist' attribute.
     *
     * @return the String value of the hoist attribute, or null if
     * none has been defined.
     * @see #setHoistAttribute
     */
    public String getHoistAttribute() {
        return hoistAttribute;
    }
    
    /**
     * Defines the `hoist' attribute.  This is an attribute which is
     * defaulted from the element content when it is read in.
     *
     * <p>For example if the <code>&lt;mytype&gt;</code> type has a
     * hoist attribute of <code>value</code>, then the XML
     * <code>&lt;mytype&gt; something &lt;/mytype&gt;</code> is deemed
     * equivalent to <code>&lt;mytype value="something"/&gt;</code>.
     * This is never put in element content when objects are written
     * to XML, but is only used when objects are constructed from
     * `foreign' DOMs.  If this is null, no attribute is defaulted.
     */
    public void setHoistAttribute(String s) {
        hoistAttribute = s;
    }

    /**
     * Sets the class or interface which constructed objects of this
     * type must be assignable to.
     *
     * @param c a class which constructed objects must match.  This
     * may be null to turn off such verification, but that is probably
     * a bad idea.
     *
     * @see #setConstructedClass(String)
     */
    public void setConstructedClass(Class c) {
        requiredClass = c;
    }

    /**
     * Sets the class of interface which constructed objects of this
     * type must be assignable to.
     *
     * @param classname the name of a class which constructed objects
     * must match.  This may be null to turn off such verification,
     * but that is probably a bad idea.
     *
     * @throws HdxException if the specified class cannot be found
     *
     * @see #setConstructedClass(Class)
     */
    public void setConstructedClass(String classname) 
            throws HdxException {
        try {
            Class c = this.getClass().forName(classname);
            setConstructedClass(c);
        } catch (ClassNotFoundException ex) {
          throw new HdxException ("Class " + classname + " not registrable: "
                                  + ex);
        }
    }

    /**
     * Obtains the expected Java type corresponding to this Hdx type.
     *
     * <p>If no such type has been registered, this returns the class
     * object for the <code>Object</code> class,
     * <code>Object.class</code>.  You may compare the result of this
     * method for equality with <code>Object.class</code> to determine
     * if a type was registered, or use it directly, since
     * <code>Object.class.isInstance(obj)</code> is true for any Java object.
     *
     * @return the Class object registered with {@link
     * #setConstructedClass}, or <code>Object.class</code> if no type
     * was so registered
     */
    public Class getConstructedClass() {
        if (requiredClass == null)
            return Object.class;
        else
            return requiredClass;
    }

    /**
     * Checks that the Document is valid HDX.
     *
     * <p>This allows the following assertions for the Document
     * <code>doc</code>, and any node <code>n</code> immediately
     * contained within it
     * <pre>
     * assert doc.getDocumentElement().getTagName
     *    .equals(HdxResourceType.HDX.xmlName());
     * assert n.getNodeType() == Node.ELEMENT_NODE; // only text nodes
     * assert HdxResourceType.match(n) != HdxResourceType.NONE; // all registered
     * </pre>
     *
     * There are other constraints which you might want to check: if
     * they are violated, you might want to throw an exception, but
     * until they are checked in this method, and the appropriate
     * assertion listed above, they should not be checked in
     * <code>assert</code> statements.
     *
     * @return true if the document does represent a valid HDX DOM
     * @see #isValid(Element)
     */
    public static boolean isValidHdx(Document doc) {
        return HDX.isValid(doc.getDocumentElement());
    }

    /**
     * Tests whether the given element is a valid instance of this
     * type.  The actual validation work is performed by the {@link
     * ElementValidator} instance registered with method {@link
     * #setElementValidator}.
     *
     * @param el an element to be validated.
     *
     * @return true if the element is a valid instance of this type;
     *         if the type has no validator, then return true always.
     */
    public boolean isValid(Element el) {
        return (validator == null ? true : validator.validateElement(el));
    }

    /** 
     * Registers an instance of the {@link ElementValidator} type,
     * which will do the work of validating this type of resource.
     */
    public void setElementValidator(ElementValidator validator) {
        this.validator = validator;
    }

    /**
     * Registers a handler for this type.  New handlers are added at
     * the front of the list.
     */
    public void registerHdxResourceFactory (HdxResourceFactory factory) {
        /* At one time, it was recommended that this method should be
         * called only from {@link
         * HdxFactory#registerHdxResourceFactory}.  This was in order
         * to permit easy _de_registration of resource factories.
         * This seems unnecessary, however, so it seems best to remove
         * the advice.  If this turns out to be useful in fact, then
         * that is where to restore the indirection.
         */
        if (handlers == null)
            handlers = new LinkedList();
        if (logger.isLoggable(Level.CONFIG))
            logger.config("HdxResourceType.registerHdxResourceFactory for "
                          + toString()
                          + " type=" + factory.getClass().getName());
        handlers.add(0, factory);
    }

    /**
     * Handles the construction of the given element.  This finds the
     * first handler in the list which can handle the given element,
     * calls it, and returns the result, cast to Object.  The objects
     * in the <code>handlers</code> list are implementations of
     * interface {@link HdxResourceFactory}.
     *
     * <p>The returned object must be assignable to the class
     * registered using method {@link #setConstructedClass}.  If no
     * such class has been assigned, no check is made.
     *
     * <p><strong>Note</strong>:This is a package-private method, but
     * although it is therefore accessible to other objects in this
     * package, it should only be called indirectly, via {@link
     * HdxFactory#getObject}.
     *
     * @throws HdxException if one of the registered constructors for
     * this type throws it.
     *
     * @throws PluginException (unchecked exception) if the
     * constructor returned an object which did not match the required
     * class registered using {@link #setConstructedClass}.
     */
    Object getObject (Element el)
            throws HdxException {
//         logger.fine("HdxResourceType.callHandler for " + toString()
//                     + ": element=" + el.getTagName());
        if (handlers != null)
            for (ListIterator li = handlers.listIterator();
                 li.hasNext();
                 ) {
                logger.fine("  next handler...");
                Object ret = ((HdxResourceFactory)li.next()).getObject(el);
                if (ret != null) {
                    if (requiredClass == null)
                        return ret;
                    else {
                        if (requiredClass.isInstance(ret))
                            return ret;
                        else
                            throw new PluginException
                                ("Constructor for element " + el.getTagName()
                                 + " returned an object of class "
                                 + ret.getClass()
                                 + " and not class "
                                 + requiredClass + " as required");
                    }   
                }
            }
//         if (logger.isLoggable(Level.FINE))
//             logger.fine("  callHandler for " + toString()
//                         + ": nothing matched!");
        return null;            // nothing matched
    }

    /**
     * Returns a HdxResourceType object which matches the specified
     * Element.
     *
     * <p>This matches only elements which are in no namespace.  That
     * is, if the given element is in any namespace,
     * <em>including</em> the HDX namespace, this does not match it.
     *
     * @return one of the static resource type constants, or a `type'
     * NONE if the string does not match anything.
     *
     * @see #match(String)
     */
    public static HdxResourceType match(Element el) {
        return match(el == null ? null : el.getTagName());
    }

    /**
     * Returns a HdxResourceType object which matches the specified
     * element name.
     *
     * @param gi an element name.  This may be null, in which case
     * this `matches' type NONE.
     *
     * @return one of the static resource type constants, or a `type'
     * NONE if the string does not match anything.
     */
    public static HdxResourceType match(String gi) {
	HdxResourceType result;
        if (gi == null)
            result = NONE;
        else {
            result = (HdxResourceType)resourceTypeMap.get(gi);
            if (result == null)
                result = NONE;
        }
	return result;
    }

    /**
     * Obtains an iterator containing all the
     * <code>HdxResourceType</code> types which are defined.  The list
     * does not include type <code>NONE</code>, and is in an arbitrary order.
     */
    public static Iterator getAllTypes() {
        return resourceTypeMap.values().iterator();
    }
}
