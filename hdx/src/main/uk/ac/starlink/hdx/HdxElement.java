// $Id$

package uk.ac.starlink.hdx;

import uk.ac.starlink.util.NodeDescendants;

import org.w3c.dom.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

/**
 * Provides a normalized/canonicalized view of another DOM.  In this view, 
 * <ul>
 * <li>the HDX is valid (according to {@link HdxResourceType#isValidHdx}
 * (which means amongst other things that each of the elements
 * immediately contained within the HDX element are element types
 * recognised by HDX, or rather, by {@link HdxResourceType});
 *
 * <li>if any element has a <code>hoistAttribute</code> defined (see
 * {@link HdxResourceType#setHoistAttribute}), then: if the hoist
 * attribute does not have a value and the element has text content,
 * the hoist attribute is set to the text content and that content
 * deleted;
 *
 * <li>no elements have a namespace declaration.
 * </ul>
 *
 * <p>If an Hdx DOM is constructed from a `foreign' tree using {@link
 * HdxFactory#newHdxContainer(Element)}, then the elements in the new
 * DOM will be `shadowed' by the elements in the old one, in the sense
 * that any changes or additions to the attributes on the new DOM
 * will, by default, also appear in the original DOM.  This behaviour
 * can be controlled using the method {@link #setShadowAttributes}.
 *
 * <p>Objects of this class are manipulated purely through the {@link
 * org.w3c.dom.Element} interface (the sole exception is the
 * package-private {@link #setAttribute(String,String,boolean)}) --
 * there are no non-private methods which return objects of type
 * <code>HdxElement</code>.
 *
 * <p>All of the elements and attributes which are significant to the
 * Hdx system should be in no namespace, and the namespace-aware
 * methods in this class, <code>...NS</code>, throw a
 * <code>NAMESPACE_ERR</code> DOMException if they are asked to
 * operate on the Hdx namespace or a namespace consisting of an empty
 * string, though a null namespace argument to these methods is fine,
 * and equivalent to the corresponding no-namespace method.  The
 * namespace-aware methods may be passed other namespaces <em>if</em>
 * there is a shadowing element; they throw a
 * <code>NAMESPACE_ERR</code> if there is no such element.
 *
 * <p>Any <code>xml:base</code> attributes on elements which
 * <em>are</em> incorporated into the HDX view of the DOM are
 * respected; such attributes on elements which are not incorporated
 * are <em>ignored</em>.  This means that in the case
 * <pre>
 * &lt;rubbish xml:base="http://x.org"&gt;
 *   &lt;h:ndx xmlns:h="http://www.starlink.ac.uk/HDX"&gt;
 *     &lt;h:image uri="file.fits" /&gt;
 *   &lt;/h:ndx&gt;
 * &lt;/rubbish&gt;
 * </pre>
 * the <code>xml:base</code> attribute has no effect, since it does
 * not appear in the HDX DOM which this is normalised into.  Whether this is
 * counterintuitive or not probably depends on your intuitions.
 * This is consistent with the view that such
 * elements must be invisible to the HDX DOM.  If you have an argument
 * that this is inconsistent with the XML Base specification, or
 * otherwise undesirable, Norman would like to hear it.
 *
 * <p>Although this is not required by the <code>Element</code>
 * interface, all of the <code>Element</code> methods below check that
 * their arguments are not <code>null</code>, and throw an
 * <code>IllegalArgumentException</code> if they are; the only
 * exception is <code>namespaceURI</code>, where a null argument is
 * documented to correspond to no namespace.
 *
 * <p>Note that only DOM Level 2 methods are currently implemented.
 * If this class is built using JDK1.5, then the DOM Level 3 methods
 * will be present, but they do not implement the functionality
 * defined by the DOM Level 3 specification (mostly they throw
 * NOT_SUPPORTED_ERR type DOMExceptions).
 *
 * @author Norman Gray (norman@astro.gla.ac.uk)
 * @version $Id$
 */
class HdxElement
        extends HdxNode
        implements Element, Cloneable {

    /*
     * As explained above, various of the attribute-removal methods
     * here throw <code>NOT_SUPPORTED_ERR</code> DOMExceptions.  This
     * is (a) because I'm not sure I've implemented them correctly,
     * but mostly (b) because I'm not sure just what the semantics
     * should be in respect of the backing DOM.  Should the methods
     * delete the attribute from the backing DOM, or just from the
     * current element?  And so on.
     *
     * Various other operations need further thought, in those places
     * marked `XXX SEMANTICS'.
     */

    /**
     * The name of this element.  Since this class supports element
     * types which are not registered with {@link HdxResourceType},
     * this will not necessarily correspond to a registered type.
     */
    private String giName;

    /** 
     * Mapping of attribute names to Attr objects.
     */
    private AttributeMap attributeMap;

    private static java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger("uk.ac.starlink.hdx");

    /**
     * Constructs an HdxElement with the given GI name.
     *
     * @param giName the name of the element, which must
     * correspond to one of the types registered with {@link
     * HdxResourceType}.
     *
     * @param owner The document which owns this element.
     *
     * @throws DOMException.SYNTAX_ERR if the GI name is an invalid one.
     */
    HdxElement(String giName, Document owner)
            throws DOMException {
        this(null,
             new ElementTypeInfo(giName, null),
             false,
             owner);
        this.giName = giName;
        assert this.giName != null;
    }
    
        

    /**
     * Constructs an HdxElement from a plain Element, where the {@link
     * HdxResourceType} has already been determined.  This is a
     * <em>private</em> constructor.
     *
     * <p>If the <code>setType</code> parameter is null, then the
     * element type is to be determined.  The element is an HDX
     * element if its GI is in the HDX namespace (depending on the
     * value of parameter <code>inNamespace</code>) and is one of the
     * recognised types (as determined by {@link
     * HdxResourceType#match}), or else it has the
     * <code>HDX:hdxname</code> attribute (the attribute named by {@link
     * HdxResourceType#HDX_ARCHATT}).  If both are present, and
     * disagree, the GI is silently preferred.
     *
     * <p>As a special case, if the element is in the HDX namespace,
     * then any unprefixed attributes on the element are taken to be
     * in that namespace also.  This is a contradiction to the XML
     * standard, which states that unprefixed attributes are in
     * <em>no</em> namespace, not even the default one.  However,
     * there are no reasonable cases where this behaviour is useful,
     * and it is massively confusing, so this constitutes best
     * practice.  <code>xml-dev</code> discussed this at horrific
     * length: see the long thread which started with <a
     * href="http://lists.xml.org/archives/xml-dev/200207/msg01376.html"
     * >Simon St Laurent</a>, and see <a
     * href="http://lists.xml.org/archives/xml-dev/200207/msg01551.html"
     * >David Carlisle</a> for a point of view which rationalises this
     * without necessarily claiming it as best practice.  The {@link
     * HdxFactory#newHdxContainer(Element,URI)} method contracts this behaviour.
     *
     * @param el element to extract information from.  It's OK for
     * this to be null -- this is purely so that this class can also service
     * the {@link #HdxElement(String,Document)} constructer.
     *
     * @param setType the HdxResourceType type which the new
     * HdxElement is representative of.  If this is null, then the
     * type is to be determined from the element.  If this cannot be
     * done, then the type is set to NONE.
     *
     * @param inNamespace if true, we examine only GI names and attributes
     * in the HDX namespace; if false, <em>only</em> those in no namespace.
     *
     * @param owner the Document which is to own this new element, or null.
     */
    private HdxElement(Element el,
                       ElementTypeInfo setType,
                       boolean inNamespace,
                       Document owner) {
        super(Node.ELEMENT_NODE, owner);
        assert owner != null;
        
        String backingNodeNSPrefix;
        Element backingNode;
        int attsToUse;
        
        ElementTypeInfo inf;
        if (setType == null) {
            if (el == null)
                throw new IllegalArgumentException
                        ("HdxElement(null,null,...)");
            
            inf = getElementTypeInfo(el, (inNamespace ? 1 : -1));
        } else {
            inf = setType;
        }
        giName = inf.getName();
        backingNodeNSPrefix = inf.getPrefix();

        // which attributes are we to use?
        if (inf.fromArch())
            attsToUse = AttributeMap.ATTS_NS;
        else if (inf.fromNamespace())
            // ...but not from the architecture attribute
            attsToUse = AttributeMap.ATTS_NS | AttributeMap.ATTS_NONS;
        else
            attsToUse = AttributeMap.ATTS_NONS;
        
        assert giName != null;
        backingNode = el;        

        logger.fine("HdxElement: giName=" + this.giName
                    + " => type=" + getHdxType()
                    + ", backing node?=" + (backingNode!=null)
                    + ", prefix=" + backingNodeNSPrefix);

        attributeMap = new AttributeMap((HdxDocument)owner,
                                        backingNode,
                                        backingNodeNSPrefix,
                                        attsToUse);

        if (backingNode == null) {
            assert backingNodeNSPrefix == null;
            return;
        }
        assert el != null;

        // If this type has a `hoist' attribute, and this attribute
        // does not already have a value, then concatenate the
        // immediate children text nodes to find a value for this
        // attribute.  These text nodes will disappear from the
        // newly-constructed HdxElement when the DOM is constructed in
        // addHdxChildren below.  This code might better belong in addHdxChildren
        String hoist = getHdxType().getHoistAttribute();
//         if (logger.isLoggable(Level.FINE))
//             logger.fine("    hoist for " + getHdxType() + " is " + hoist
//                         + ", test="
//                         + (hoist != null)
//                         + "&" + (hoist!=null && !hasAttribute(hoist))
//                         + "&" + el.hasChildNodes());
        if (hoist != null
            && !hasAttribute(hoist)
            && el.hasChildNodes()) {
            NodeList nl = el.getChildNodes();
            StringBuffer sb = new StringBuffer();
            for (int i=0; i<nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.TEXT_NODE)
                    sb.append(n.getNodeValue());
            }
            String textval = sb.toString().trim();
            if (textval.length() != 0)
                setAttribute(hoist, textval, false);
        }
    }

    /**
     * Implements HDX namespace logic.
     * Examines the first of the following objects to be present
     * (subject to constraints from parameter <code>nsswitch</code>): (i) the
     * element's GI if that's in the HDX namespace, (ii) the attribute
     * <code>hdxname</code> in the HDX namespace (as named by {@link
     * HdxResourceType#HDX_ARCHATT}), and (iii) the element's GI not
     * in any namespace.
     *
     * @param el the element to examine
     *
     * @param nsswitch if this is positive, look only for GIs and
     * attributes in the HDX namespace; if negative, look only for GIs
     * in no namespace; if zero, look for both.
     *
     * @return the ElementTypeInfo from this element, containing the
     * appropriate HdxResourceType, or {@link HdxResourceType#NONE}
     * if there is no match.
     */
    private static ElementTypeInfo getElementTypeInfo(Element el,
                                                      int nsswitch) {
        if (el == null)
            return new ElementTypeInfo(null, null);

        String gins = el.getNamespaceURI();
        if (nsswitch >= 0) {
            if (gins != null && gins.equals(HdxResourceType.HDX_NAMESPACE))
                return new ElementTypeInfo(el.getLocalName(), el.getPrefix());
        
            Attr hdxname = el.getAttributeNodeNS(HdxResourceType.HDX_NAMESPACE,
                                                 HdxResourceType.HDX_ARCHATT);
            if (hdxname != null) {
                ElementTypeInfo inf = new ElementTypeInfo(hdxname.getValue(),
                                                      hdxname.getPrefix());
                inf.setArch(true);
                return inf;
            }
//             if (hdxname != null)
//                 return new ElementTypeInfo(hdxname.getValue(),
//                                            hdxname.getPrefix());
        }

        if (nsswitch <= 0 && gins == null)
            return new ElementTypeInfo(el.getTagName(), null);

        return new ElementTypeInfo(null, null);
    }

    /** Handles a <code>(HdxResourceType, namespace-prefix)</code> pair. */
    private static class ElementTypeInfo {
        private HdxResourceType t;
        private String hdxname;
        private String prefix;
        private boolean hadArchAttribute;
        public ElementTypeInfo(String typename, String prefix) {
            this.t = HdxResourceType.match(typename);
            this.hdxname = typename;
            this.prefix = prefix;
            this.hadArchAttribute = false;
        }
        /** Get the registered resource type that this element corresponds to.
         * @return HdxResourceType.NONE if the element has no registered type.
         */
        public HdxResourceType getType() { return t; }
        public String getName() { return hdxname; }
        public String getPrefix() { return prefix; }
        /** Was this element from the HDX namespace? */
        public boolean fromNamespace() { return prefix != null; }
        /** Was the Element type specified on an architectural
            attribute? */
        public boolean fromArch() {
            return hadArchAttribute;
        }
        public void setArch(boolean is) {
            hadArchAttribute = is;
        }
        public String toString() {
            return (prefix == null ? "" : prefix) + ':' + hdxname;
        }
    }            

    /**
     * Constructs one or more HdxElement trees from a plain Element.  The
     * returned DocumentFragment may have more than one HdxElement in it, if
     * there was more than one HDX object implied by the given Element
     * tree.
     *
     * <p>The DOM which this builds contains only the element types
     * which are defined in the HDX namespace, but these elements are
     * not declared to be in any namespace within the normalised DOM
     * (that is, there are no prefixes).  The new DOM is backed by the
     * old one, so that attribute changes in the new one also appear in the old
     * one.
     *
     * <p>The Element argument should contain one or more Hdx objects
     * (that is, elements representing an <code>&lt;hdx&gt;</code>
     * element in the HDX namespace).  However, as a special case, if
     * it contains only elements which are the <em>content</em> of Hdx
     * objects (that is, if they are Hdx-registered types, since Hdx
     * elements can contain all such types), then they are put inside
     * a new Hdx object.  This is a slightly heuristic fix, and its
     * behaviour may possibly change in future.
     *
     * <p>If the element argument is both an HDX-type element (that
     * is, <code>&lt;hdx&gt;</code> or <code>&lt;ndx&gt;</code> or the
     * like) and is in no namespace, then this method will not look
     * for the HDX namespace in the elements below, and indeed will
     * ignore elements in the HDX namespace.  Otherwise, the method
     * will examine <em>only</em> elements and attributes in the HDX
     * namespace.
     *
     * <p><strong>Note</strong> It is this method which implements the
     * behaviour documented in {@link
     * HdxFactory#newHdxContainer(Element)}, and if this method is
     * changed, then that method's documentation <em>must</em> be
     * changed also.
     *
     * @return a new DocumentFragment representing one or more HDXes,
     * or null if none could be found
     */
    static DocumentFragment constructHdxElementTree(Element el) {
        ElementTypeInfo inf = getElementTypeInfo(el, 0);
        boolean useNS = (inf.getType() == HdxResourceType.NONE
                         || inf.fromNamespace());
        if (logger.isLoggable(Level.FINE))
            logger.fine("constructHdxElementTree:" + el.toString()
                        + ", useNS=" + useNS);
        Document tdoc = HdxDOMImplementation
            .getInstance()
            .createDocument(null, "dummy", null);
        DocumentFragment df = tdoc.createDocumentFragment();

        // Add this element and its children to the DocumentFragment.
        // If el is itself an Hdx element, this will result in only a
        // single child in the document fragment; otherwise, this can
        // result in more than one Hdx element child.
        addHdxChildren(df, el, useNS);

        if (!df.hasChildNodes())
            // Found nothing!
            return null;

        else if (((HdxElement)df.getFirstChild()).getHdxType()
                 == HdxResourceType.HDX) {
            // The first child of this document fragment is an Hdx --
            // nothing more to do
            for (HdxElement kid = (HdxElement)df.getFirstChild();
                 kid != null;
                 kid = (HdxElement)kid.getNextSibling()) {
                if (kid.getHdxType() != HdxResourceType.HDX) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine
                        ("constructHdxElementTree: not all children are HDX!");
                    return null;
                }
            } 
            return df;

        } else {
            // The first child of this document fragment is _not_ an
            // Hdx -- make a new Hdx document and return a fragment
            // containing that.  First, we check that _all_ the
            // children are valid Hdx children: if any is an Hdx, then
            // we can say that the input DOM was invalid;
            // addHdxChildren should guarantee that none is an
            // unregistered type.
            for (HdxElement kid = (HdxElement)df.getFirstChild();
                 kid != null;
                 kid = (HdxElement)kid.getNextSibling()) {
                if (kid.getHdxType() == HdxResourceType.HDX) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine
                                ("constructHdxElementTree: unexpected Hdx");
                    return null;
                }
                if (kid.getHdxType() == HdxResourceType.NONE) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine
                                ("constructHdxElementTree: unregistered type");
                    return null;
                }
            }
            DocumentFragment newfrag = tdoc.createDocumentFragment();
            Element newhdx
                    = tdoc.createElement(HdxResourceType.HDX.xmlName());
            newfrag.appendChild(newhdx);
            newhdx.appendChild(df);
            return newfrag;
        }
    }

    /** 
     * Extracts all of the Hdx elements in the given tree, and adds
     * them as children of the node <code>parent</code>.  The search
     * for Hdx nodes starts with the element <code>el</code>.
     * Elements are Hdx elements if {@link #getElementType} says they
     * are.
     */    
    private static void addHdxChildren
            (Node parent, Element el, boolean useNS) {
        assert parent instanceof HdxNode; // we always add to one of `our' Nodes
        ElementTypeInfo elType = getElementTypeInfo(el, (useNS ? 1 : -1));
        if (logger.isLoggable(Level.FINE))
            logger.fine("addHdxChildren("
                        + ((HdxNode)parent).toString() + ", "
                        + el.getTagName() + '(' + elType + "), "
                        + useNS + ')');
        Node fosterParent;      // the node any children will be added to
        if (elType.getName() == null)
            fosterParent = parent;
        else {
            fosterParent
                = new HdxElement(el, elType, useNS, parent.getOwnerDocument());
            parent.appendChild(fosterParent);
        }
        // Now call addHdxChildren on each of the Element children of
        // this current node.  Note that Text nodes are not included here.
        for (Node kid=el.getFirstChild();
             kid!=null;
             kid=kid.getNextSibling())
            if (kid.getNodeType() == Node.ELEMENT_NODE)
                addHdxChildren(fosterParent, (Element)kid, useNS);
    }

    /**
     * Returns the Hdx type of the current element.  Note that we do
     * not assert that the current element is a registered type --
     * <code>getHdxType()</code> may return
     * <code>HdxResourceType.NONE</code>.
     */
    private HdxResourceType getHdxType() {
        return HdxResourceType.match(giName);
    }

    /**
     * Determines whether attribute values in this DOM are shadowed by
     * attributes in the elements of the DOM from which it was
     * constructed.  This affects only new attributes -- if you change
     * a preexisting attribute, it will always affect the shadowing element.
     *
     * <p>You are unlikely to need to be aware this behaviour often, and this
     * functionality may change or disappear in a future release.
     *
     * @param shadow if true, then any <em>new</em> attributes will be
     * added to the backing element as well as the Hdx one.
     */
    public void setShadowAttributes(boolean shadow) {
        attributeMap.shadowNewAttributes = shadow;
    }

    /*
     * Following methods constitute the implementation of the Element
     * interface.
     */

    public String getTagName() {
        return giName;
    }

    /*
     * HAS attributes...
     */

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if name is null
     */
    public boolean hasAttribute(String name) {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        return attributeMap.containsKey(name);
    }
    

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if localName is null
     */
    public boolean hasAttributeNS(String namespaceURI, 
                                  String localName) {
        if (localName == null)
            throw new IllegalArgumentException("localName is null");
        try {
            trapHdxNamespace(namespaceURI);
            if (namespaceURI == null)
                return hasAttribute(localName);
            else
                return attributeMap.containsKey(namespaceURI, localName);
        } catch (DOMException ex) {
            // do nothing
        }
        return false;
    }

    /*
     * GET attributes...
     */

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if name is null
     */
    public String getAttribute(String name) {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        Attr a = attributeMap.get(name);
        return (a == null ? "" : a.getValue());
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if name is null
     */
    public Attr getAttributeNode(String name) {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        return attributeMap.get(name);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if localName is null
     */
    public String getAttributeNS(String namespaceURI, 
                                 String localName) {
        if (localName == null)
            throw new IllegalArgumentException("localName is null");
        try {
            trapHdxNamespace(namespaceURI);
        } catch (DOMException ex) {
            return "";
        }
        return attributeMap.getString(namespaceURI, localName);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if localName is null
     */
    public Attr getAttributeNodeNS(String namespaceURI, 
                                   String localName) {
        if (localName == null)
            throw new IllegalArgumentException("localName is null");
        try {
            trapHdxNamespace(namespaceURI);
        } catch (DOMException ex) {
            return null;
        }
        return attributeMap.get(namespaceURI, localName);
    }

    /*
     * SET attributes...
     */

    /** 
     * Sets the given attribute.  If the attribute is backed by an
     * attribute in another DOM, then it is that backing attribute
     * which is changed.  If this is a new attribute, then a
     * corresponding attribute is created in the element which backs
     * this one.
     *
     * @param name the name of an attribute, which is added to the
     * element if it did not already exist.
     *
     * @param value the value of the attribute
     * @throws IllegalArgumentException (unchecked) if name or value is null
     */
    public void setAttribute(String name, String value)
            throws DOMException {
        if (name == null || value == null)
            throw new IllegalArgumentException("name or value is null");
        attributeMap.set(name, value);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if name or value is null
     */
    void setAttribute(String name, String value, boolean useBacking) 
            throws DOMException {
        if (name == null || value == null)
            throw new IllegalArgumentException("name or value is null");
        boolean tshadow = attributeMap.shadowNewAttributes;
        attributeMap.shadowNewAttributes = false;
        setAttribute(name, value);
        attributeMap.shadowNewAttributes = tshadow;
    }
    
    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if newAttr is null
     */
    public Attr setAttributeNode(Attr newAttr)
            throws DOMException {
        if (newAttr == null)
            throw new IllegalArgumentException("newAttr is null");
        Attr oldAtt = getAttributeNode(newAttr.getName());
        attributeMap.set(newAttr);
        return oldAtt;
    }

    /**
     * Adds a new attribute.  If the given namespace is not the HDX
     * namespace, then the backing DOM alone is modified.
     *
     * <p>If the given namespace is blank, null <em>or the HDX
     * namespace</em> then the method throws a
     * <code>NAMESPACE_ERR</code> DOM Exception.  The HDX elements and
     * attributes are in the empty namespace, and so such
     * modifications should be done only using the {@link
     * #setAttribute} method.
     *
     * <p>If the Element is shadowed by an Element in a `foreign'
     * tree, then this call will be passed on to that element; if
     * there is no such shadowing Element, then we throw a
     * <code>NAMESPACE_ERR</code> DOM Exception.
     *
     * <p>XXX SEMANTICS This currently throws an exception if
     * namespaceURI is blank, or in the HDX namespace.  Should it?
     *
     * @throws IllegalArgumentException (unchecked) if qualifiedName or value is null
     */
    public void setAttributeNS(String namespaceURI, 
                               String qualifiedName, 
                               String value)
            throws DOMException {
        if (qualifiedName == null || value == null)
            throw new IllegalArgumentException
                    ("qualifiedName or value is null");
        trapHdxNamespace(namespaceURI);

        if (namespaceURI == null)
            setAttribute(qualifiedName, value);
        else
            throw new DOMException
                    (DOMException.NAMESPACE_ERR,
                     "HdxElement elements can't take namespaced attributes");
        return;
    }

    /**
     * Adds a new attribute.  If the given namespace is not the HDX
     * namespace, then the backing DOM alone is modified.
     *
     * <p>If namespace specified by the new attribute is blank, null
     * <em>or the HDX namespace</em> then the method throws a
     * <code>NAMESPACE_ERR</code> DOM Exception.  The HDX elements and
     * attributes are in the empty namespace, and so such
     * modifications should be done only using the {@link
     * #setAttributeNode} method.
     *
     * <p>If the Element is shadowed by an Element in a `foreign'
     * tree, then this call will be passed on to that element; if
     * there is no such shadowing Element, then we throw a
     * <code>NAMESPACE_ERR</code> DOM Exception.
     *
     * <p>XXX SEMANTICS This currently throws an exception if
     * namespaceURI is blank, or in the HDX namespace.  Should it?
     *
     * @throws IllegalArgumentException (unchecked) if newAttr is null
     */
    public Attr setAttributeNodeNS(Attr newAttr)
            throws DOMException {
        if (newAttr == null)
            throw new IllegalArgumentException("newAttr is null");
        String namespaceURI = newAttr.getNamespaceURI();
        trapHdxNamespace(namespaceURI);

        if (namespaceURI == null)
            return setAttributeNode(newAttr);

        else
            throw new DOMException
                    (DOMException.NAMESPACE_ERR,
                     "HdxElement elements can't take namespaced attributes");
    }

    /*
     * REMOVE attributes...
     */

    /**
     * Removes an attribute from this HdxElement.  The attribute is also
     * removed from the backing DOM.  It is not an error to attempt to
     * remove an attribute which does not exist on the element (the
     * {@link org.w3c.dom.Element} documentation makes no
     * statement about this).
     *
     * @param name the name of the attribute to be removed.
     *
     * @throws DOMException - NO_MODIFICATION_ALLOWED_ERR: if this
     * element is read-only.
     * @throws IllegalArgumentException (unchecked) if name is null
     */
    public void removeAttribute(String name)
            throws DOMException {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        attributeMap.remove(name);
        return;
    }

    /** 
     * Removes an attribute from this HdxElement.  The attribute is
     * also removed from the backing DOM. 
     *
     * @param oldAtt the attribute node to be removed
     *
     * @return the Attr node that was removed.
     *
     * @throws DOMException - NO_MODIFICATION_ALLOWED_ERR: if this
     * element is read-only.  
     * @throws DOMException - NOT_FOUND_ERR: if
     * oldAttr is not an attribute of the element.
     * @throws IllegalArgumentException (unchecked) if oldAtt is null
     */
    public Attr removeAttributeNode(Attr oldAtt)
            throws DOMException {
        if (oldAtt == null)
            throw new IllegalArgumentException("oldAtt is null");
        Attr att = getAttributeNode(oldAtt.getName());
        if (att == null)
            throw new DOMException
                (DOMException.NOT_FOUND_ERR,
                 "the attribute " + oldAtt.getName()
                 + " is not present on that element");
        removeAttribute(oldAtt.getName());
        return oldAtt;
    }

    /**
     * Removes an attribute by local name and namespace URI.
     *
     * <p>If namespace specified by the new attribute is blank, 
     * <em>or the HDX namespace</em> then the method throws a
     * <code>NAMESPACE_ERR</code> DOM Exception.  The HDX elements and
     * attributes are in the empty namespace, and so such
     * modifications should be done only using the {@link
     * #setAttributeNode} method.
     *
     * <p>If the Element is shadowed by an Element in a `foreign'
     * tree, then this call will be passed on to that element; if
     * there is no such shadowing Element, then we throw a
     * <code>NAMESPACE_ERR</code> DOM Exception.
     * @throws IllegalArgumentException (unchecked) if localName is null
     */
    public void removeAttributeNS(String namespaceURI, 
                                  String localName)
            throws DOMException {
        if (localName == null)
            throw new IllegalArgumentException("localName is null");
        trapHdxNamespace(namespaceURI);

        if (namespaceURI == null) {
            removeAttribute(localName);
            return;
        }
        
        else
            throw new DOMException
                    (DOMException.NAMESPACE_ERR,
                     "HdxElement elements can't take namespaced attributes");
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if name is null
     */
    public NodeList getElementsByTagName(final String name) {

        class TagnameNodeList implements NodeList, NodeDescendants.Visitor {
            private final String selector;
            private ArrayList l;
            TagnameNodeList() {
                l = new ArrayList();
                if (name.equals("*")) // special case
                    selector = null;
                else
                    selector = name;
            }
            public int getLength() { return l.size(); }
            public Node item(int index) {
                if (index < 0 || index >= l.size())
                    return null;
                else
                    return (Node)l.get(index);
            }
            public Object visitNode(Node n) {
                if (selector == null || n.getNodeName().equals(selector))
                    l.add(n);
                return null;
            }
        }

        if (name == null)
            throw new IllegalArgumentException("name is null");

        TagnameNodeList tnl = new TagnameNodeList();
        NodeDescendants nd
                = new NodeDescendants(this, NodeDescendants.SHOW_ELEMENT);
        nd.visitTree(tnl);
        return tnl;
    }
            
    /** 
     * Returns a NodeList of all the descendant Elements with a given
     * local name and namespace URI in the order in which they are
     * encountered in a preorder traversal of this Element tree.  In
     * this implementation this is always an empty list, since this
     * tree always represents elements in the empty namespace.
     * @throws IllegalArgumentException (unchecked) if localName is null
     */
    public NodeList getElementsByTagNameNS(String namespaceURI, 
                                           String localName) {
        if (localName == null)
            throw new IllegalArgumentException("localName is null");
        return new NodeList() {
            public int getLength() { return 0; }
            public Node item(int index) { return null; }
        };
    }
    

    /*
     * Following methods override methods in HdxNode.
     */

    public String getNodeName() {
        return giName;
    }

    public boolean hasAttributes() {
        return attributeMap.size() != 0;
    }
    
    /**
     * A NamedNodeMap containing the attributes of this node.  The
     * nodes in the map are not `live' -- altering them does not
     * change any backing DOM.  If you need to change them, use {@link
     * #getAttribute}.
     */
    public NamedNodeMap getAttributes() {
        return new NamedNodeMap() {
            private List list = new ArrayList();
            {
                for (Iterator mi = attributeMap.iterator();
                     mi.hasNext(); )
                    list.add(attributeMap.get((String)mi.next()));
            }
            public int getLength() {
                return list.size();
            }
            public Node item(int index) {
                if (index < 0 || index >= getLength())
                    return null;
                else
                    return (Node)list.get(index);
            }
            public Node getNamedItem(String name) {
                for (int i=0; i<list.size(); i++) {
                    Attr att = (Attr)list.get(i);
                    if (att.getName().equals(name))
                        return att;
                }
                return null;
            }
            public Node getNamedItemNS(String namespaceURI, String localName) {
                return getNamedItem(localName);
            }
            public Node removeNamedItem(String name)
                throws DOMException {
                cannotModifyException("removeNamedItem");
                return null;
            }
            public Node removeNamedItemNS(String ns, String name)
                throws DOMException {
                cannotModifyException("removeNamedItemNS");
                return null;
            }
            public Node setNamedItem(Node n)
                throws DOMException {
                cannotModifyException("setNamedItem");
                return null;
            }
            public Node setNamedItemNS(Node n)
                throws DOMException {
                cannotModifyException("setNamedItemNS");
                return null;
            }
        };
    }

    /*
     * Override Object methods.
     */

    /**
     * Represents the object as an XML string.  There is no namespace
     * declaration.
     */
    public String toString() {
        // return "<" + getTagName() + ">";
        StringBuffer sb = new StringBuffer("<");
        sb.append(getTagName());
        NamedNodeMap nodemap = getAttributes();
        for (int i=0; i<nodemap.getLength(); i++) {
            Attr att = (Attr)nodemap.item(i);
            sb.append(' ');
            sb.append(att.getName());
            sb.append("=\"");
            sb.append(att.getValue());
            sb.append('"');
        }
        sb.append('>');
        return sb.toString();
    }
    
    /**
     * Returns a duplicate of this Node.
     * 
     * <p>This is just like <code>clone(deep)</code>, except that we call
     * <code>super.clone()</code> first.
     *
     * @param deep if true, recursively clone the subtree under the
     * specified node; if false, clone only the node itself (and its
     * attributes, if it is an Element)
     * @return the duplicate node
     * @see HdxNode#cloneNode(boolean)
     */
    public Node cloneNode(boolean deep) {
        HdxElement el = (HdxElement)super.clone(deep);
        // ....clones the children, too

        // We don't need to clone giName, since that can't be changed.
        // Note that we copy attributes irrespective of the value of deep.
        el.attributeMap = (AttributeMap)attributeMap.clone();
        for (Iterator ai = el.attributeMap.iterator(); ai.hasNext(); ) {
            String n = (String)ai.next();
            HdxAttr attr = (HdxAttr)el.attributeMap.get(n);
            attr.setOwnerElement(el);
        }
        return el;
    }

//DOM3     /*
//DOM3      * DOM Level 3 not implemented.
//DOM3      */
//DOM3     
//DOM3     /** Not implemented */
//DOM3     public void setIdAttributeNode( Attr at, boolean makeId ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "setIdAttributeNode not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public void setIdAttribute( String name, boolean makeId ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "setIdAttribute not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public void setIdAttributeNS( String namespaceURI, String localName,
//DOM3                                   boolean makeId ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "setIdAttributeNS not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public TypeInfo getSchemaTypeInfo() {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "getSchemaTypeInfo not implemented" );
//DOM3     }


    /**
     * Creates and returns a copy of this Element and its attributes.
     * The difference between this method and a deep clone using
     * {@link #cloneNode} is that the clone and the original
     * <em>share</em> a reference to their parent.
     *
     * @param deep if true, recursively clone the subtree under this
     * node; if false, clone only the node itself.  In either case, we
     * do clone the attributes on this element.
     * @returns a clone of this instance
     */
    protected Object clone(boolean deep) {
        HdxElement el = (HdxElement)super.clone(deep);
        // ...clones the children, too

        // We don't need to clone giName, since that can't be changed.
        // Note that we copy attributes irrespective of the value of deep.
        el.attributeMap = (AttributeMap)attributeMap.clone();
        for (Iterator ai = el.attributeMap.iterator(); ai.hasNext(); ) {
            String n = (String)ai.next();
            HdxAttr attr = (HdxAttr)el.attributeMap.get(n);
            attr.setOwnerElement(el);
        }
        return el;
    }

    /**
     * Indicates whether some other object is `equal to' this one.
     * Two elements are equal if they have the same tag name and
     * exactly the same attribute set, and if each of their children
     * is equal according to this method or {@link HdxNode#equals}.
     *
     * @param t an object to be tested for equality with this one
     * @return true if the Nodes should be regarded as equivalent
     */
    public boolean equals(Object t) {
        if (t == null)
            return false;
        if (! (t instanceof HdxElement))
            return false;
        HdxElement te = (HdxElement)t;
        if (! giName.equals(te.giName))
            return false;
        if (! attributeMap.equals(te.attributeMap))
            return false;

        // Check children -- copy of code in HdxNode#equals
        Node mykid = getFirstChild();
        Node tkid  = te.getFirstChild();
        while (mykid != null) {
            if (tkid == null)
                return false;
            if (! mykid.equals(tkid))
                return false;
            mykid = mykid.getNextSibling();
            tkid  = tkid.getNextSibling();
        }
        if (tkid != null)
            return false;

        return true;            // whew!
    }

    public int hashCode() {
        // We rely on the attributes being presented in a predictable
        // order.  This will be more than satisfied if the attMap is
        // a Map implementation which additionally implements
        // SortedMap.
        HdxNode.HashCode hc = new HdxNode.HashCode(giName.hashCode());
        hc.add(attributeMap.hashCode());
        for (Node kid=getFirstChild(); kid!=null; kid=kid.getNextSibling())
            hc.add(kid.hashCode());
        return hc.value();
    }
    

    /* ******************** PRIVATE HELPER METHODS ******************** */

    /**
     * Throws a DOMException, explaining that HdxElement is read-only.
     * HdxElement isn't really read-only, but can't be modified using
     * the method that invoked this.
     */
    private void cannotModifyException(String methodName)
            throws DOMException {
        throw new DOMException
            (DOMException.NO_MODIFICATION_ALLOWED_ERR,
             "HdxElement can't be modified using method " + methodName);
    }

    private void notSupportedException(String methodName)
            throws DOMException {
        throw new DOMException
            (DOMException.NOT_SUPPORTED_ERR,
             "HdxElement doesn't support method " + methodName);
    }

    /**
     * Enforces the namespace constraint for Hdx DOMs.  Throws a
     * DOMException if the namespace is given, but is either the empty
     * string or the Hdx namespace.
     */
    private void trapHdxNamespace(String namespaceURI)
            throws DOMException {
        if (namespaceURI != null
            && (namespaceURI.length() == 0
                || namespaceURI.equals(HdxResourceType.HDX_NAMESPACE)))
            throw new DOMException
                (DOMException.NAMESPACE_ERR,
                 "HdxElement attributes must be in the empty namespace");
        return;
    }

    static Attr newHdxAttr(String name, HdxDocument ownerDocument) {
        return newHdxAttr(name, "", null, ownerDocument, null);
    }
    
    private static Attr newHdxAttr(String name, String value,
                                   Attr shadow,
                                   HdxDocument ownerDocument,
                                   HdxElement ownerElement) {
        HdxAttr a = new HdxAttr(name, value, shadow, ownerDocument);
        a.setOwnerElement(ownerElement);
        return a;
    }

    /**
     * Represents an attribute in a <code>HdxElement</code> object.
     * This implements the {@link org.w3c.dom.Attr} interface.  Package-only class.
     *
     * <p>Although this class will preserve a link to a parent
     * element, it is a static class rather than an inner class,
     * because that relationship to the parent is not permanent, but
     * might change over the life of the object.
     *
     * <p>According to the DOM2 standard, Attr nodes <em>may</em> have
     * <code>Text</code> or <code>EntityReference</code> children.
     * This Hdx implementation, however, does not support
     * <code>EntityReference</code> nodes, so the only child of a
     * <code>HdxAttr</code> will be a single <code>Text</code> node.
     */
    private static final class HdxAttr
            extends HdxNode implements Attr {
        /** The name of this attribute */
        private final String name;
        /** The value of this attribute */
        private String value;
        /**
         * Reference to the Attr which this shadows.  This `ought' to
         * be final, but if it were, it couldn't be cloned as below.
         */
        private Attr shadowAttr;
        /** The owner element of this attribute */
        private HdxElement ownerElement; // initially null
        /** The attribute value as a Text node. */
        private Node textNode;

        public HdxAttr(String name,
                       String value,
                       Attr shadowAttr,
                       Document ownerDocument) {
            super(Node.ATTRIBUTE_NODE, ownerDocument);
            this.name = name;
            this.value = value;
            this.shadowAttr = shadowAttr;
            assert this.value!=null ^ this.shadowAttr!=null;
            // Check name!=null.  We could thrown an
            // IllegalArgumentException, but since this is a private
            // class, this can only be our fault.
            assert this.name != null;
        }
        void setOwnerElement(HdxElement ownerElement) {
            this.ownerElement = ownerElement;
        }
        public String getName() { return name; }
        public String getNodeName() { return getName(); } // override HdxNode
        public Element getOwnerElement() {
            return ownerElement;
        }
        public boolean getSpecified() { return true; }
        public String getValue() { // override HdxNode
            if (value == null)
                return shadowAttr.getValue();
            else
                return value;
        }
        public String getNodeValue() { return getValue(); } // override HdxNode
        public void setValue(String newValue) {
            if (value == null)
                shadowAttr.setValue(newValue);
            else
                value = newValue;
            if (logger.isLoggable(Level.FINE))
                logger.fine("HdxAttr.setValue: " + name + '=' + getValue());
            textNode = null;
        }
        public void setNodeValue(String newValue) {
            setValue(newValue);
        }
        public Node getFirstChild() {
            // Return the value as a Text node
            if (textNode == null)
                // create and cache it
                textNode = getOwnerDocument().createTextNode(getValue());
            return textNode;
        }
        public Node getLastChild() {
            return getFirstChild();
        }

//DOM3         /*
//DOM3          * DOM Level 3, not implemented.
//DOM3          */
//DOM3 
//DOM3         /** Not implemented */
//DOM3         public boolean isId() {
//DOM3             throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                     "isId not implemented" );
//DOM3         }
//DOM3 
//DOM3         /** Not implemented */
//DOM3         public TypeInfo getSchemaTypeInfo(){
//DOM3             throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                    "getSchemaTypeInfo not implemented" );
//DOM3         }

        public Node cloneNode(boolean deep) { return (Node)clone(); }
        /**
         * Return a copy of this object.  If this Attr is backed by an
         * Attr in another Document, then we set the value of the new
         * Attr to the <em>getValue()</em> of this Attr.  This seems
         * to be the Right Thing: I'm sure it's possible to construct
         * an argument which says that if this <code>HdxAttr</code> is
         * backed by another one, then so should the clone, but that
         * feels like unexpected behaviour to me, and the principle of
         * least surprise tells us that the two should be entirely distinct.
         */
        public Object clone() {
            HdxAttr a = (HdxAttr)super.clone();
            a.value = new String(getValue());
            a.shadowAttr = null;
            return a;
        }
        public boolean equals(Object a) {
            // Note that we compare attribute values irrespective of
            // whether the values are Strings or Attrs.
            if (a == null || !(a instanceof HdxAttr))
                return false;
            if (a == this)
                return true;
            HdxAttr hdxa = (HdxAttr)a;
            return name.equals(hdxa.name)
                    && getValue().equals(hdxa.getValue());
        }
        public int hashCode() {
            return new HdxNode.HashCode()
                    .add(name.hashCode())
                    .add(getValue().hashCode())
                    .value();
        }
        public String toString() {
            return "["+name+'='+getValue()+']';
        }
    }

    private class AttributeMap 
            implements Cloneable {
        public static final int ATTS_NS = 1;
        public static final int ATTS_NONS = 2;
        private java.util.Map myMap;
        final private HdxDocument doc;
        /**
         * The shadow element, initialised from the constructor.
         * <p>The support for backing nodes is <em>only</em> for
         * attributes -- there is no support for any corresponding shadowing of
         * operations which modify the tree, and no need for that either
         * (at present).  Adding that sort of thing would really require
         * non-trivial redesign of the {@link HdxNode} class. 
         */
        final private Element shadowElement;

        /**
         * Prefix used for this namespace when setting attributes on
         * the shadow element.  Non-null also indicates that these
         * attributes are in a namespace.  This prefix has been
         * declared in the backingElement as the prefix for the HDX
         * namespace.  We need this when creating new backed
         * attributes since they must be given this prefix in the
         * backing DOM.  This isn't necessarily the only prefix so
         * declared.
         */
        final private String shadowElementPrefix;

        /** If true, new attributes are added to the shadow node, too */
        public boolean shadowNewAttributes;

        /**
         * Creates a new map of HDX-namespace attributes, optionally
         * backed by the attributes in a given element.
         *
         * @param doc the Document which owns these attributes
         * @param shadowElement
         * an Element in another DOM which backs the attributes in this map.  Non-null
         * indicates that there is such an element.  We don't
         * change this element other than to possibly alter the set of
         * attributes on it.
         * @param shadowElementPrefix the prefix declared in the
         * shadow element to correspond to the HDX namespace.  If
         * null, no such prefix was declared (or there is no shadow element).
         * @param attsToUse flags composed of the ATTS_NS or ATTS_NONS
         * constants OR-ed together.  If ATTS_NS is set, then use
         * attributes which are in the HDX namespace; if ATTS_NONS is
         * set, use attributes in no namespace.  Either or both may be
         * set, but assert that one or the other is.
         */
        public AttributeMap(HdxDocument doc,
                            Element shadowElement,
                            String shadowElementPrefix,
                            int attsToUse) {
            this.doc = doc;
            this.shadowElement = shadowElement;
            this.shadowElementPrefix = shadowElementPrefix;
            shadowNewAttributes = true; // default


            // Create the map.
            // Could do this lazily, but that's more work.
            // If we change this Map implementation, we also need to change
            // the clone() method below.
            myMap = new java.util.TreeMap();
            assert myMap instanceof java.util.SortedMap;

            // If there is a shadowElement, then import each of that
            // element's attributes at this point.
            if (shadowElement != null) {
                NamedNodeMap shAtts = shadowElement.getAttributes();
                String elNS = shadowElement.getNamespaceURI();
                for (int i=0; i<shAtts.getLength(); i++) {
                    Attr a = (Attr)shAtts.item(i);
                    String ns = a.getNamespaceURI();

                    assert attsToUse != 0;
                    boolean oneOfOurs = 
                            ( (attsToUse & ATTS_NS) != 0
                              && ns != null
                              && ns.equals(HdxResourceType.HDX_NAMESPACE))
                            ||
                            ( (attsToUse & ATTS_NONS) != 0
                              && ns == null);

                    String myname = (ns==null
                                     ? a.getName()
                                     : a.getLocalName());
                    if (myname.equals(HdxResourceType.HDX_ARCHATT)) {
                        // Carefully avoid including the Hdx name
                        // attribute, since this is only to
                        // indicate the Hdx type which this shadow
                        // Element corresponds do.
                        oneOfOurs = false;
                    }
                             
                    if (oneOfOurs) {
                        Attr newAttr = newHdxAttr
                                (myname, null, a, doc, HdxElement.this);
                        myMap.put(myname, newAttr);
                    } else if (a.getName().equals("xml:base")) {
                        // special-case this.  Note that we _ignore_
                        // any xml:base attributes which are on
                        // elements which this method does not see.
                        myMap.put("xml:base", a);
                    }
                }
            }
        }

        public Attr get(String name) {
            return (Attr)myMap.get(name);
        }
        public Attr get(String namespaceURI, String localName) {
            if (namespaceURI == null)
                return get(localName);
            
            if (shadowElement != null)
                return shadowElement
                        .getAttributeNodeNS(namespaceURI, localName);
            return null;
        }
        public String getString(String name) {
            Attr a = get(name);
            return (a == null ? "" : a.getValue());
        }
        public String getString(String namespaceURI, String name) {
            Attr a = get(namespaceURI, name);
            return (a == null ? "" : a.getValue());
        }
        public void set(String name, String value)
                throws DOMException {
            Attr attr = (Attr)myMap.get(name);
            if (attr == null) {
                // Make a new Attr.  
                boolean shadowIt
                        = shadowNewAttributes && shadowElement != null;
                Attr newAttr;
                if (shadowIt)
                    newAttr = newHdxAttr(name,
                                         null,
                                         newShadowAttr(name, value),
                                         doc,
                                         HdxElement.this);
                else
                    newAttr = newHdxAttr(name,
                                         value,
                                         null,
                                         doc,
                                         HdxElement.this);
                myMap.put(name, newAttr);
            } else {
                assert attr instanceof HdxAttr;
                attr.setValue(value);
            }
        }
        public void set(Attr att)
                throws DOMException {
            if (!(att instanceof HdxAttr)) {
                // Not one of ours -- why have we been given this!?
                StringBuffer sb = new StringBuffer();
                sb.append("Attribute ")
                        .append(att.getName())
                        .append('=')
                        .append(att.getValue())
                        .append(" is class ")
                        .append(att.getClass().getName())
                        .append(", not Hdx");
                throw new DOMException
                        (DOMException.WRONG_DOCUMENT_ERR,
                         sb.toString());
            }
            
            assert att.getNamespaceURI() == null;
            // We can assert this because
            // HdxDocument.createAttributeNS is not supported.  Ie,
            // this will become false if we do decide to add that in future


            // Since this is in no namespace, it's definitely this
            // class which should handle it, so we oughtn't to go via
            // get(), but instead look up myMap directly.
            boolean shadowIt;
            Attr curr = (Attr)myMap.get(att.getName());
            if (curr == null) {
                // Easy -- just create a new one
                set(att.getName(), att.getValue());
            } else {
                ((HdxAttr)att).setOwnerElement(HdxElement.this);
                myMap.put(att.getName(), att);
            }
        }

        private Attr newShadowAttr(String name, String value)
                throws DOMException {
            assert shadowElement != null;
            Attr shAttr;
            
            if (shadowElementPrefix == null) {
                assert !shadowElement.hasAttribute(name);
                shadowElement.setAttribute(name, value);
                shAttr = shadowElement.getAttributeNode(name);
            } else {
                assert !shadowElement.hasAttributeNS
                        (HdxResourceType.HDX_NAMESPACE, name);
                shadowElement.setAttributeNS
                        (HdxResourceType.HDX_NAMESPACE,
                         shadowElementPrefix+':'+name,
                         value);
                shAttr = shadowElement.getAttributeNodeNS
                        (HdxResourceType.HDX_NAMESPACE,
                         name);
            }
            return shAttr;
        }

        public void remove(String name) {
            // Don't remove shadow attributes (for now)
            myMap.remove(name);
        }

        /** The number of attributes in the map */
        public int size() {
            return myMap.size();
        }

        /** Returns true if the given attribute is present in the map */
        public boolean containsKey(String name) {
            return get(name) != null;
        }

        /** Returns true if the given attribute is present in the map */
        public boolean containsKey(String namespaceURI, String localName) {
            return get(namespaceURI, localName) != null;
        }

        /** Clears all the attributes from the map */
        public void clear() {
            myMap.clear();
        }

        /** Returns an iterator containing all the attribute names */
        public Iterator iterator() {
            return myMap.keySet().iterator();
        }

        public String toString() { // debugging only!!!!
            StringBuffer sb = new StringBuffer();
            String nl = System.getProperty("line.separator");
            sb.append("AttributeMap:").append(nl);
            if (myMap == null)
                sb.append("    <null>").append(nl);
            else
                for (Iterator i = myMap.entrySet().iterator(); i.hasNext(); ) {
                    java.util.Map.Entry e = (java.util.Map.Entry)i.next();
                    sb.append("    ")
                            .append(e.getKey())
                            .append('=')
                            .append(e.getValue())
                            .append(nl);
                }
            return sb.toString();
        }

        public Object clone() {
            try {
                AttributeMap a = (AttributeMap)super.clone();
                // We cannot simply clone the map, since that does a
                // shallow copy.  The Map(Map) constructor does not
                // specify whether it clones the entries, so we must
                // assume it does not.  This Map implementation must match
                // that in the AttributeMap constructor above.
                a.myMap = new java.util.TreeMap();
                for (Iterator i = myMap.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry e = (Map.Entry)i.next();
                    a.myMap.put(e.getKey(),
                                ((HdxAttr)e.getValue()).clone());
                }
                return a;
            } catch (CloneNotSupportedException e) {
                // Can't happen -- Object.clone() does not in fact
                // throw this
                throw new AssertionError("Can't happen: Object.clone() seemed to throw CloneNotSupportedException");
            }
        }

        public int hashCode() {
            assert myMap instanceof java.util.SortedMap;
            HdxNode.HashCode hc = new HdxNode.HashCode();
            for (Iterator i=iterator(); i.hasNext(); ) {
                Object val = get((String)i.next());
                assert val instanceof HdxAttr;
                hc.add(val.hashCode());
            }
            return hc.value();
        }

        public boolean equals(Object t) {
            if (t == null || !(t instanceof AttributeMap))
                return false;
            if (t == this)
                return true;
            AttributeMap ta = (AttributeMap)t;
            if (myMap == null && ta.myMap != null)
                    return false;
            if (ta.myMap == null)
                return false;
            if (myMap.size() != ta.myMap.size())
                return false;
            for (Iterator i=iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                // just testing equality of getAttribute(key) wouldn't
                // work, since that can't distinguish unset attributes
                // from attributes with an empty value
                if (!(ta.containsKey(key) 
                      && getString(key).equals(ta.getString(key))))
                    return false;
            }
            return true;
        }
    }
}
