package uk.ac.starlink.hdx;

import java.util.*;
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
 * <li>Define a class which registers the new type, through a call to
 * {@link #newHdxResourceType}, in a static initialiser.
 *
 * <li>List the class in the property file
 * <code>HdxResourceType.properties</code>.  This file must be located
 * in the current directory, or in the directory named by the system
 * property <code>HdxResourceType.dir</code>.  The file should
 * list classes to be initialised, in the form
 * <code>HdxResourceType.0=full.class.name</code>,
 * <code>HdxResourceType.1=another.class</code>, and so on.
 * </ol>
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
 * @version $Id$ */
public class HdxResourceType {

    /** The namespace for HDX */
    public static final String HDX_NAMESPACE = "http://www.starlink.ac.uk/HDX";

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
     * Set of all possible type Strings which have been presented to
     * match(), and which are not in fact registered types.  This is a
     * cache, to prevent match needlessly re-searching for
     * definitions.
     */
    private static Set unmatchedTypes;

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
        unmatchedTypes = new HashSet();

        try {
            NONE = newHdxResourceType("none");
            resourceTypeMap.remove("none");

            HDX = newHdxResourceType("hdx");
            HDX.setElementValidator(new ElementValidator() {
                public boolean validateElement(Element el) {
                    // An HDX is valid if all of its children are Element
                    // nodes; each one is recognised by
                    // HdxResourceType.match (ie, all are registered); and
                    // each is valid.
                    System.err.println("validateElement("
                                       + HdxFactory.serializeDOM(el)
                                       + "):");
                    if (HdxResourceType.match(el) != HDX)
                        return false;
                    for (Node n = el.getFirstChild();
                         n != null;
                         n = n.getNextSibling()) {
                        if (n.getNodeType() != Node.ELEMENT_NODE) {
                            System.err.println("...child type=" + n.getNodeType()
                                               + " value=<"
                                               + n.getNodeValue() + ">");
                            return false;
                        }
                        HdxResourceType type = HdxResourceType.match((Element)n);
                        if (type == HdxResourceType.NONE) {
                            System.err.println("...unrecognised child type");
                            return false;
                        }
                        if (!type.isValid((Element)n)) {
                            System.err.println("...child not valid");
                            return false;
                        }
                    }
                    System.err.println("...OK");
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

        // Load properties which tell us which classes to load
        Properties hdxprops;
        Properties sysprops = System.getProperties();
        String propdir = System.getProperty("HdxResourceType.dir");
        if (propdir == null)
            propdir = System.getProperty("user.dir");
        String fn = propdir + "/HdxResourceType.properties";
        try {
            java.io.FileInputStream propfile = new java.io.FileInputStream(fn);
            hdxprops = new Properties(sysprops);
            hdxprops.load(propfile);
        } catch (java.io.FileNotFoundException e) {
            // Not a problem -- fix silently
            System.err.println("No file " + fn);
            hdxprops = sysprops;
        } catch (java.io.IOException e) {
            System.err.println
                ("IOException reading file " + fn + ": " + e);
            hdxprops = sysprops;
        } catch (java.lang.SecurityException e) {
            System.err.println
                ("Security exception opening " + fn + ":" + e);
            hdxprops = sysprops;
        }

        // Work through any list of properties HdxResourceType.0...
        for (int i=0; true; i++) {
            String propname = "HdxResourceType." + Integer.toString(i);
            String prop = hdxprops.getProperty(propname);
            System.err.println("Property " + propname + "="
                               + (prop == null ? "<null>" : prop));
            if (prop == null)
                break;          // JUMP OUT
            try {
                Class newclass = Class.forName(prop, true, null);
            } catch (ExceptionInInitializerError ex) {
                System.err.println
                    ("Failed to initialize class " + prop + " (" + ex + ")");
            } catch (LinkageError ex) {
                System.err.println
                    ("Failed to load class " + prop + " (" + ex + ")");
            } catch (ClassNotFoundException ex) {
                System.err.println
                    ("Failed to find class " + prop + " (" + ex + ")");
            }
        }
    }

    private static ClassLoader resourceDefinitionClassLoader = null;
    private static boolean matchCurrentlyBeingCalled = false;
    
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
        System.err.println("Registered new HDX resource type " + t);
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
    String getHoistAttribute() {
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
     * @throws HdxException if the specified class cannot be found.
     *
     * @see #setConstructedClass(Class)
     */
    public void setConstructedClass(String classname) 
            throws HdxException {
        try {
            Class c = Class.forName(classname);
            setConstructedClass(c);
        } catch (ClassNotFoundException ex) {
          throw new HdxException ("Class " + classname + " not registrable: "
                                  + ex);
        }
    }

    /**
     * Obtains the expected Java type corresponding to this Hdx type.
     *
     * @return the Class object registered with {@link
     * #setConstructedClass}, or null if no type was so registered.
     */
    public Class getConstructedClass() {
        return requiredClass;
    }

    /**
     * Checks that the Document is valid HDX.
     *
     * <p>This allows the following assertions for the Document
     * <code>doc</code>, any Element <code>el</code> immediately within it,
     * and each node <code>n</code> which is a child of <code>el</code>:
     * <pre>
     * assert doc.getDocumentElement().getTagName
     *    .equals(HdxResourceType.HDX.xmlName());
     * assert HdxResourceType.match(el) != HdxResourceType.NONE;
     * assert n.getNodeType() == Node.ELEMENT_NODE;
     * </pre>
     * XXX check this list of assertions -- does doc match code?
     *
     * There are other constraints which you might want to check: if
     * they are violated, you might want to throw an exception, but
     * until they are checked in this method, and the appropriate
     * assertion listed above, they should not be checked in
     * <code>assert</code> statements.
     *
     * @return true if the document does represent a valid HDX DOM
     * @see #isValid(Element) */
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
     * @return true if the element is a valid instance of this type.
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
     *
     * <p><strong>Note</strong>:This is a package-private method, but
     * although it is therefore accessible to other objects in this
     * package, it should only be called indirectly, via {@link
     * HdxFactory#registerHdxResourceFactory}.
     */
    public void registerHdxResourceFactory (HdxResourceFactory factory) {
        if (handlers == null)
            handlers = new LinkedList();
        System.err.println("HdxResourceType.registerHdxResourceFactory for " + toString()
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
        System.err.println("HdxResourceType.callHandler for " + toString()
                           + ": element=" + el.getTagName());
        if (handlers != null)
            for (ListIterator li = handlers.listIterator();
                 li.hasNext();
                 ) {
                System.err.println("  next handler...");
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
        System.err.println("  callHandler for " + toString()
                           + ": nothing matched!");
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
        System.err.println("HdxResourceType.match(" + gi + ")");
        if (gi == null || unmatchedTypes.contains(gi))
            result = NONE;
        else {
            result = (HdxResourceType)resourceTypeMap.get(gi);
            if (result == null) {
                // Rewrite resource loading code
                unmatchedTypes.add(gi);// don't come here again
                result = NONE;
            }
//                 if (matchCurrentlyBeingCalled)
//                     // Ooops!
//                     throw new PluginException
//                 ("A ResourceDefinition initialiser recursively called match!");
//                 matchCurrentlyBeingCalled = true;
//                 result = findResourceDefinition(gi);
//                 matchCurrentlyBeingCalled = false;
//                 if (result == null) {                    
//                     System.err.println("Failed to find a class defining <"
//                                        + gi + ">");
//                     unmatchedTypes.add(gi); // don't come here again
//                     result = NONE;
//                 } else {
//                     assert resourceTypeMap.containsKey(gi);
//                 }
//            }
        }
        System.err.println("match(" + gi + ") produces " + result);
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

//     /**
//      * Finds and initialises the class which defines the given type.
//      * This looks for a class called
//      * <code>&lt;gi&gt;ResourceDefinition</code>, which extends class
//      * {@link ResourceDefinition}, and calls its {@link
//      * ResourceDefinition#initializeResourceType} method.
//      *
//      * @return the new HdxResourceType, or null if the class is not found
//      *
//      * @throws PluginException (unchecked exception) if the
//      * ResourceDefinition initialiser is malformed or does not obey its
//      * contract.
//      */
//     private static HdxResourceType findResourceDefinition(String gi) {
//         // NB: don't call match() within here, or else we loop
//         assert !resourceTypeMap.containsKey(gi);

//         HdxResourceType returnValue = null;
//         try {
//             if (resourceDefinitionClassLoader == null) {
//                 URL[] urlList = new URL[1];
//                 urlList[0]
//                     = new URL("file:/home/norman/s/src/ndx/w/java/uk/ac/starlink/hdx/");
//                 // XXX ResourceDefinition path hardwired -- abstract it
//                 resourceDefinitionClassLoader
//                     = new java.net.URLClassLoader(urlList);
//             }
//             Class initialiser = Class.forName(gi + "ResourceDefinition",
//                                               true,
//                                               resourceDefinitionClassLoader);
//             Class baseClass
//                 = Class.forName("uk.ac.starlink.hdx.ResourceDefinition");
//             if (baseClass.isAssignableFrom(initialiser)) {
//                 ResourceDefinition def
//                     = (ResourceDefinition)initialiser.newInstance();
//                 if (def.initializeResourceType()) {
//                     // The initialiser _claims_ to have worked...
//                     if (resourceTypeMap.containsKey(gi)) {
//                         // ...good!
//                         returnValue = (HdxResourceType)resourceTypeMap.get(gi);
//                         assert returnValue != NONE;
//                         System.err.println
//                             ("initialised " + gi + "ResourceDefinition");
//                     } else {
//                         // ...but it didn't
//                         throw new PluginException
//                             ("ResourceDefinition for " + gi
//                              + " did not install a new type");   
//                     }
//                 } else {
//                     System.err.println("class " + gi + "ResourceDefinition"
//                                        + " failed to initialise");
//                 }
//             } else {
//                 throw new PluginException
//                     ("Class " + gi + "ResourceDefinition"
//                      + " is not an instance of ResourceDefinition");
//             }
//         } catch (ClassNotFoundException e) {
//             System.err.println("No class found to initialise " + gi
//                                + ": " + e);
//             Throwable cause = e.getCause();
//             System.err.println("...cause:" + (cause == null
//                                               ? "-null-"
//                                               : cause.toString()));
//         } catch (IllegalAccessException e) {
//             System.err.println("Illegal access initialising " + gi 
//                                + ": " + e);
//         } catch (InstantiationException e) {
//             System.err.println("Can't instantiate ResourceDefinition: " + e);
//         } catch (java.net.MalformedURLException e) {
//             System.err.println("Malformed URL in resource definition path: "
//                                + e);
//         }
//         return returnValue;
//     }
}
