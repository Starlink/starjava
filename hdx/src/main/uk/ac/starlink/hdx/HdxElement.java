// $Id$

package uk.ac.starlink.hdx;

import org.w3c.dom.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Provides a normalized view of another DOM.  In this view, all of
 * the elements in the DOM are element types recognised by HDX (or
 * rather, by {@link HdxResourceType}), and no elements have a
 * namespace declaration.  Any changes to the attributes on the
 * elements in this DOM, either adding or altering them, are echoed in
 * the backing DOM.
 *
 * <p>Objects of this class are manipulated purely through the {@link
 * org.w3c.dom.Element} interface (the sole exception is the
 * package-private {@link #setAttribute(String,String,boolean)}) --
 * there are no non-private methods which return objects of type
 * <code>HdxElement</code>.
 *
 * <p>This implements a slightly cut-down version of the Element
 * interface, with the attribute-removal methods throwing {@link
 * DOMException}s, as unsupported operations; this latter behaviour
 * might change.
 *
 * @author Norman Gray (norman@astro.gla.ac.uk)
 * @version $Id$
 */
class HdxElement
        extends HdxNode
        implements Element, Cloneable {

    /*
     * As explained above, various of the attribute-removal methods
     * here throw NOT_SUPPORTED_ERR DOMExceptions.  This is (a)
     * because I'm not sure I've implemented them correctly, but
     * mostly (b) because I'm not sure just what the semantics should
     * be in respect of the backing DOM.  Should the methods delete
     * the attribute from the backing DOM, or just from the current
     * element?  And so on.
     *
     * Various other operations need further thought, in those places
     * marked `XXX SEMANTICS'.
     */

//     /** 
//      * The HdxResourceType which this element corresponds to.
//      */
//     private HdxResourceType type;

    /**
     * An element in the backing DOM which holds the attributes used
     * to back the attributes in this element (got that?).  We don't
     * change this element other than to possibly alter the set of
     * attributes on it.
     *
     * <p><strong>REDUNDANT NOTE</strong>: this field is used <em>only</em> by
     * the class {@link HdxElement}, which extends this class.
     * However, it is declared here so that we can have the
     * Node-insertion methods (which are not overridden by
     * <code>HdxElement</code>) check its value, and refuse to proceed
     * if it is non-null.  This means that <code>HdxElement</code>
     * instances which do have backing elements are not
     * tree-modifiable (though they are attribute-modifiable).  If
     * this does in fact turn out to become a problem, then the
     * solution is <em>either</em> to rewrite <code>HdxElement</code>
     * to override the appropriate methods (unsightly), <em>or</em> to
     * rewrite <code>HdxNode</code> and <code>HdxElement</code> to
     * move the backing-node functionality into <code>HdxNode</code>
     * (heavy lifting).  If it isn't a problem in fact, then that
     * functionality should remain in <code>HdxElement</code>.
     */
    protected Element backingNode;

    /**
     * A prefix in the backingElement which has been declared as the
     * prefix for the HDX namespace.  Method {@link #setAttributeNS}
     * needs this.  It isn't necessarily the only prefix so declared.
     * 
     * <p>If this is null, then it indicates that the current object
     * was constructed from a tag name and/or attributes which were
     * not in any namespace.  
     * XXX is this still true?
     */
    protected String backingNodeNSPrefix;

    /**
     * The name of this element.  Since this class supports element
     * types which are not registered with {@link HdxResourceType},
     * this will not necessarily correspond to a registered type.
     */
    private String giName;

    /** 
     * Mapping of attribute names to Attr objects.  The keys are
     * attribute names in the HDX namespace <em>without</em> any
     * prefix; the values are Attr elements, the names of which
     * <em>may</em> have a namespace prefix, which should therefore
     * be ignored.
     */
    private java.util.Map attMap;

    private static java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger( "uk.ac.starlink.hdx" );
    // How the hell do I enable fine logging?
//     static {
//         logger.setLevel(java.util.logging.Level.FINE);
//         java.util.logging.Handler[] h = logger.getHandlers();
//         for (int i=0; i<h.length; i++) {
//             System.err.println("Changing handler level from "
//                                + h[i].getLevel() + " to FINE");
//             h[i].setLevel(java.util.logging.Level.FINE);
//         }
//     }

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
    }
    
        

    /**
     * Constructs an HdxElement from a plain Element, where the {@link
     * HdxResourceType} has already been determined.
     *
     * <p>If the <code>setType</code> parameter is null, then the
     * element type is to be determined.  The element is an HDX
     * element if its GI is in the HDX namespace (depending on the
     * value of parameter <code>inNamespace</code>) and is one of the
     * recognised types (as determined by {@link
     * HdxResourceType#match}), or else it has the
     * <code>HDX:name</code> attribute (if both are present, and
     * disagree, the GI is silently preferred).
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
     * without necessarily claiming it as best practice.
     *
     * @param el element to extract information from.  It's OK for
     * this to be null -- this is so that this class can construct
     * dummy HdxElement instances for its own use.  
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
        
        if (setType == null) {
            if (el == null)
                throw new IllegalArgumentException
                    ("HdxElement(null,null,...)");
            
            ElementTypeInfo inf = getElementTypeInfo(el,
                                                     (inNamespace ? 1 : -1));
            giName = inf.getName();
            //type = inf.getType();
            backingNodeNSPrefix = inf.getPrefix();
        } else {
            //type = setType.getType();
            giName = setType.getName();
            backingNodeNSPrefix = setType.getPrefix();
        }
        assert giName != null;
        logger.info("HdxElement: giName=" + this.giName
                    + " => type=" + getHdxType());

        backingNode = el;        
        if (backingNode == null) {
            assert backingNodeNSPrefix == null;
            //logger.fine
                System.err.println("HdxElement: no backing node");
            return;
        }
        assert el != null;

        boolean noNSAttsAreHdx;
        if (el.getNamespaceURI() != null
            && el.getNamespaceURI().equals(HdxResourceType.HDX_NAMESPACE))
            noNSAttsAreHdx = true;
        else
            noNSAttsAreHdx = false;
        
         // Work through the attributes on the backing object, adding
         // `our' attributes to the attMap on this object.  Depending
         // on the value of inNamespace, `our attributes' means either
         // attributes in the HDX namespace or attributes in no
         // namespace.
         // 
         // Don't reexamine "name", since it was already processed by
         // getElementTypeInfo().
        NamedNodeMap nodemap = el.getAttributes();
        for (int i=0; i<nodemap.getLength(); i++) {
            Attr att = (Attr) nodemap.item(i);
            assert att != null;
            String ns = att.getNamespaceURI();
            String hdxName = null;
            if (inNamespace) {
                if (ns == null && noNSAttsAreHdx)
                    hdxName = att.getName();
                else if (ns != null
                         && ns.equals(HdxResourceType.HDX_NAMESPACE)) {
                    hdxName = att.getLocalName();
                }
            } else {
                if (ns == null) {
                    hdxName = att.getName();
                }
            }
            logger.info("Attr: " + att.getName() + "=" + att.getValue()
                        + " (ns=" + inNamespace +
                        ") -> hdxname=" + hdxName);
            if (hdxName != null && !hdxName.equals("name"))
                addAtt(hdxName, att);
        }

        // If this type has a `hoist' attribute, and this attribute
        // does not already have a value, then concatenate the
        // immediate children text nodes to find a value for this
        // attribute.
        String hoist = getHdxType().getHoistAttribute();
        System.err.println("HdxElement logger at: " + logger.getLevel());
        //logger.fine
        System.err.println("Hoist attribute for " + getHdxType() + ": " + hoist
                    + ", test="
                    + (hoist != null)
                    + "&" + !hasAttribute(hoist)
                    + "&" + el.hasChildNodes());
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
                setAttribute(hoist, textval);
        }
    }

    /**
     * Implements HDX namespace logic.
     * Examines the first of the following objects to be present
     * (subject to constraints from parameter <code>nsswitch</code>): (i) the
     * element's GI if that's in the HDX namespace, (ii) the attribute
     * <code>name</code> in the HDX namespace, and (iii) the element's GI
     * not in any namespace.
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
        //return new ElementTypeInfo(HdxResourceType.NONE, null);

        String gins = el.getNamespaceURI();
        if (nsswitch >= 0) {
            if (gins != null && gins.equals(HdxResourceType.HDX_NAMESPACE))
                return new ElementTypeInfo(el.getLocalName(), el.getPrefix());
        
            Attr hdxname = el.getAttributeNodeNS(HdxResourceType.HDX_NAMESPACE,
                                                 "name");
            if (hdxname != null)
                return new ElementTypeInfo(hdxname.getValue(),
                                           hdxname.getPrefix());
        }

        if (nsswitch <= 0 && gins == null)
            return new ElementTypeInfo(el.getTagName(), null);

        return new ElementTypeInfo(null, null);
        //return new ElementTypeInfo(HdxResourceType.NONE, null);
    }

    /** Handles a (<code>HdxResourceType, namespace-prefix</code>) pair. */
    private static class ElementTypeInfo {
        private HdxResourceType t;
        private String hdxname;
        private String prefix;
        public ElementTypeInfo(String typename, String prefix) {
            this.t = HdxResourceType.match(typename);
            this.hdxname = typename;
            this.prefix = prefix;
        }
        /** Get the registered resource type that this element corresponds to.
         * 
         * @return HdxResourceType.NONE if the element has no registered type.
         */
        public HdxResourceType getType() { return t; }
        public String getName() { return hdxname; }
        public String getPrefix() { return prefix; }
        /** Was this element from the HDX namespace? */
        public boolean fromNamespace() { return prefix != null; }
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
     * <p><strong>Note</strong> It is this method which implements the
     * behaviour documented in {@link
     * HdxFactory#newHdxContainer(Element)}, and if this method is
     * changed, then that method's documentation <em>must</em> be
     * changed also.
     *
     * @return a new DocumentFragment representing an HDX, or null if none
     * could be found.
     */
    static DocumentFragment constructHdxElementTree(Element el) {
        ElementTypeInfo inf = getElementTypeInfo(el, 0);
        boolean useNS = (inf.getType() == HdxResourceType.NONE
                         || inf.fromNamespace());
        logger.info("constructHdxElementTree:" + el.toString()
                           + ", useNS=" + useNS);
        Document tdoc = HdxDOMImplementation
            .getInstance()
            .createDocument(null, "dummy", null);
        DocumentFragment df = tdoc.createDocumentFragment();

        addHdxChildren(df, el, useNS); // add all the children of this node

        if (!df.hasChildNodes())
            // Found nothing!
            return null;
        else if (((HdxElement)df.getFirstChild()).getHdxType()
                 == HdxResourceType.HDX) {
            for (HdxElement kid = (HdxElement)df.getFirstChild();
                 kid != null;
                 kid = (HdxElement)kid.getNextSibling()) {
                if (kid.getHdxType() != HdxResourceType.HDX) {
                    logger.info
                        ("constructHdxElementTree: not all children are HDX");
                    return null;
                }
            } 
            return df;
        } else {
            logger.info("df=" + df);
            DocumentFragment newfrag = tdoc.createDocumentFragment();
            Element newhdx
                = tdoc.createElement(HdxResourceType.HDX.xmlName());
            // XXX We should check here that all of the elements in
            // the DocumentFragment are indeed legal children of HDX.
            // How best?
            newhdx.appendChild(df);
            newfrag.appendChild(newhdx);
            logger.info("newfrag=" + newfrag);
            return newfrag;
        }
    }
    
    private static void addHdxChildren(Node n, Element el, boolean useNS) {
        assert el.getNodeType() == Node.ELEMENT_NODE;
        
        ElementTypeInfo elType = getElementTypeInfo(el, (useNS ? 1 : -1));
        logger.info("addHdxChildren("
                    + ((HdxNode)n).toString() + ",\n\t"
                    + el.getTagName() + '(' + elType + "),\n\t"
                    + useNS + ')');
        Node fosterParent;
        if (elType.getName() == null)
            fosterParent = n;
        else {
            fosterParent
                = new HdxElement(el, elType, useNS, n.getOwnerDocument());
            n.appendChild(fosterParent);
        }
//         if (elType.getType() == HdxResourceType.NONE)
//             fosterParent = n;
//         else {
//             fosterParent
//                 = new HdxElement(el, elType, useNS, n.getOwnerDocument());
//             n.appendChild(fosterParent);
//         }
        for (Node kid=el.getFirstChild();
             kid!=null;
             kid=kid.getNextSibling())
            if (kid.getNodeType() == Node.ELEMENT_NODE)
                addHdxChildren(fosterParent, (Element)kid, useNS);
    }

//     public String toXML() {
//         StringBuffer sb = new StringBuffer();
//         sb.append('<').append(getTagName());
//         NamedNodeMap nodemap = getAttributes();
//         assert nodemap != null; // getAttributes on elements always non-null
//         for (int i=0; i<nodemap.getLength(); i++) {
//             Attr att = (Attr)nodemap.item(i);
//             sb.append(' ')
//                 .append(att.getName())
//                 .append("=\"")
//                 .append(att.getValue())
//                 .append('"');
//         }
//         NodeList nl = getChildNodes();
//         assert nl != null;      // getChildNodes always non-null
//         if (nl.getLength() == 0)
//             // empty element
//             sb.append("/>");
//         else {
//             sb.append('>');
//             for (int i=0; i<nl.getLength(); i++)
//                 sb.append(((HdxNode)nl.item(i)).toXML());
//             sb.append("</").append(getTagName()).append('>');
//         }
        
//         return sb.toString();
//     }            

    private HdxResourceType getHdxType() {
        return HdxResourceType.match(giName);
    }

    /*
     * Following methods constitute the implementation of the Element
     * interface.
     */

    public String getTagName() {
        return giName;
    }

    /*
     * GET attributes...
     */

    public String getAttribute(String name) {
        Attr att = getAttributeNode(name);
        if (att != null)
            return att.getValue();
        else
            return "";
    }

    /**
     * Retrieves an attribute node by name.  The Node is `live' -- any
     * changes to the Node are reflected in any backing DOM.
     */
    public Attr getAttributeNode(String name) {
        if (hasAttribute(name)) {
            assert attMap != null;
            return (Attr)attMap.get(name);
        } else {
            return null;
        }
    }

    public String getAttributeNS(String namespaceURI, 
                                 String localName) {
        if (namespaceURI == null || namespaceURI.length() == 0)
            return getAttribute(localName);
        else
            return "";
    }

    public Attr getAttributeNodeNS(String namespaceURI, 
                                   String localName) {
        if (namespaceURI == null || namespaceURI.length() == 0)
            return getAttributeNode(localName);
        else
            return null;
    }

    /*
     * HAS attributes...
     */

    public boolean hasAttribute(String name) {
        if (attMap == null)
            return false;
        else
            return attMap.containsKey(name);
    }
    

    public boolean hasAttributeNS(String namespaceURI, 
                                  String localName) {
        if (namespaceURI == null || namespaceURI.length() == 0)
            return hasAttribute(localName);
        else
            return false;
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
     */
    public void setAttribute(String name, String value)
            throws DOMException {
        setAttribute(name, value, true);
    }
    
    /**
     * Sets the given attribute, optionally using the backing element.
     *
     * <p>This has package-access only, so that clients of this class
     * cannot know about the backing DOM.
     *
     * @param name the name of an attribute, which is added to the
     * element if it did not already exist.
     *
     * @param value the value of the attribute
     *
     * @param useBackingNode if true, and if this is a new attribute,
     * and if there is a backing element, then the attribute is added
     * to the backing element; if false, it is local.  If this is not
     * a new attribute, this parameter does not change whether or not
     * the attribute is backed.
     *
     * @throws DOMException - NO_MODIFICATION_ALLOWED_ERR: if this
     * element is read-only.
     */
    void setAttribute(String name, String value, boolean useBackingNode)
            throws DOMException {
        // Following no longer true: we _are_ allowing NONE elements in a DOM:
//         // Assert that we are not trying to add attributes to an
//         // element of type NONE.  It should never be possible to have
//         // an HdxElement of type NONE in a DOM, so if we find
//         // one such here, then either there _is_ such an element
//         // wandering around, or else the code in this class/package
//         // has got itself in a twist.
//         assert getHdxType() != HdxResourceType.NONE;

        Attr att = getAttributeNode(name);
        if (att == null) {
             // Attribute didn't already exist -- add a new one.  If
             // backingNode is non-null, this may be added to the
             // backing DOM depending on the value of
             // useBackingNode. We still need to use the backing
             // document to create the Attr object, so the created
             // Attr will have that as its getOwnerDocument.
             //
             // If backingNode is null, then use the HdxElement's
             // owner document to create the Attr.
            if (backingNode == null)
                useBackingNode = false;

            Attr newAtt;
            if (useBackingNode && backingNodeNSPrefix != null) {
                assert backingNode != null;
                Document doc = backingNode.getOwnerDocument();
                newAtt = backingNode
                    .getOwnerDocument()
                    .createAttributeNS(HdxResourceType.HDX_NAMESPACE,
                                       backingNodeNSPrefix + ":" + name);
                newAtt.setValue(value);
                backingNode.setAttributeNodeNS(newAtt);
            } else {
                Document doc;
                if (useBackingNode)
                    doc = backingNode.getOwnerDocument();
                else {
                    doc = getOwnerDocument();
                    if (doc == null) {
                        // This element wasn't created by
                        // HdxDocument.createElement(), so it must
                        // have been created along with a backing
                        // element.
                        assert backingNode != null;
                        doc = backingNode.getOwnerDocument();
                    }
                }
                newAtt = doc.createAttribute(name);
                newAtt.setValue(value);
                if (useBackingNode)
                    backingNode.setAttributeNode(newAtt);
            }            
            addAtt(name, newAtt);
        } else
            att.setValue(value);
            
        return;
    }

    public Attr setAttributeNode(Attr newAttr)
            throws DOMException {
        Attr att = getAttributeNode(newAttr.getName());
        if (att == null)
            setAttribute(newAttr.getName(), newAttr.getValue(), true);
        else
            att.setValue(newAttr.getValue());
        return att;
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
     * XXX SEMANTICS This currently throws an exception if
     * namespaceURI is null, blank, or in the HDX namespace.  Should it?
     */
    public void setAttributeNS(String namespaceURI, 
                               String qualifiedName, 
                               String value)
            throws DOMException {
        if (namespaceURI == null
            || namespaceURI.length() == 0
            || namespaceURI.equals(HdxResourceType.HDX_NAMESPACE))
            throw new DOMException
                (DOMException.NAMESPACE_ERR,
                 "HdxElement attributes are in the empty namespace");
        
        assert backingNode != null;
        backingNode.setAttributeNS(namespaceURI, qualifiedName, value);
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
     * XXX SEMANTICS This currently throws an exception if
     * namespaceURI is null, blank, or in the HDX namespace.  Should it?
     */
    public Attr setAttributeNodeNS(Attr newAttr)
            throws DOMException {
        String namespaceURI = newAttr.getNamespaceURI();
        if (namespaceURI == null
            || namespaceURI.length() == 0
            || namespaceURI.equals(HdxResourceType.HDX_NAMESPACE))
            throw new DOMException
                (DOMException.NAMESPACE_ERR,
                 "HdxElement attributes are in the empty namespace");

        assert backingNode != null;
        return backingNode.setAttributeNodeNS(newAttr);
    }

    /*
     * REMOVE attributes...
     */

    /**
     * Removes an attribute from this HdxElement.  The attribute is also
     * removed from the backing DOM.
     *
     * @param name the name of the attribute to be removed.
     *
     * @throws DOMException - NO_MODIFICATION_ALLOWED_ERR: if this
     * element is read-only.
     */
    public void removeAttribute(String name)
            throws DOMException {
        removeAttribute(name, true);
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
     */
    public Attr removeAttributeNode(Attr oldAtt)
            throws DOMException {
        Attr att = getAttributeNode(oldAtt.getName());
        if (att == null)
            throw new DOMException
                (DOMException.NOT_FOUND_ERR,
                 "the attribute " + oldAtt.getName()
                 + " is not present on that element");
        removeAttribute(oldAtt.getName(), true);
        return oldAtt;
    }

    /**
     * Removes the given attribute, optionally using the backing element.
     *
     * <p>This has package-access only, so that clients of this class
     * cannot know about the backing DOM.
     *
     * @param name the name of an attribute, which is removed from the
     * element if it is present.
     *
     * @param useBackingNode if true, and if there is a backing
     * element, then the attribute is removed from the backing element
     * as well.
     *
     * @throws DOMException - NO_MODIFICATION_ALLOWED_ERR: if this
     * element is read-only.
     */
    void removeAttribute(String name, boolean useBackingNode)
            throws DOMException {
        Attr att = getAttributeNode(name);
        if (att == null)
            return;             // nothing to do
        
        else if (useBackingNode && backingNode != null)
            backingNode.removeAttribute(name);

        assert attMap != null;
        attMap.remove(att.getName());
        return;
    }

    public void removeAttributeNS(String namespaceURI, 
                                  String localName)
            throws DOMException {
        notSupportedException("removeAttributeNS");
//         if (namespaceURI.equals(HdxResourceType.HDX_NAMESPACE))
//             removeAttribute(localName);
        return;
    }
    

    public NodeList getElementsByTagName(String name) {
        final ArrayList elementList = new ArrayList();
        String spec = name.trim();
        if (spec.equals("*"))   // special case
            spec = null;
        addElementToList(spec, elementList);

        return new NodeList() {
            private ArrayList l = elementList;
            public int getLength() { return l.size(); }
            public Node item(int index) {
                if (index < 0 || index >= l.size())
                    return null;
                else
                    return (Node)l.get(index);
            }
        };
    }

    /**
     * Conditionally adds the current element and its HdxElement
     * children to the given list.  The element is added if either
     * parameter <code>spec</code> is null or else
     * <code>spec</code> matches the element name.
     */
    private void addElementToList(String spec, ArrayList list) {
        if (spec == null || spec.equals(getTagName()))
            list.add(this);
        for (Node n = getFirstChild(); n != null; n = n.getNextSibling())
            ((HdxElement)n).addElementToList(spec, list);
        return;
    }

//     public NodeList getElementsByTagName(String name) {
//         final ArrayList elementList = new ArrayList();
//         HdxResourceType type;
//         if (name.trim().equals("*"))   // special case
//             type = null;
//         else
//             type = HdxResourceType.match(name);
//         if (type != HdxResourceType.NONE)
//             addElementToList(type, elementList);

//         return new NodeList() {
//             private ArrayList l = elementList;
//             public int getLength() { return l.size(); }
//             public Node item(int index) {
//                 if (index < 0 || index >= l.size())
//                     return null;
//                 else
//                     return (Node)l.get(index);
//             }
//         };
//     }

//     /**
//      * Conditionally adds the current element and its HdxElement
//      * children to the given list.  The element is added if either
//      * parameter <code>matchType</code> is null or else
//      * <code>matchType</code> matches <code>getHdxType</code>.
//      */
//     private void addElementToList(HdxResourceType matchType, ArrayList list) {
//         if (matchType == null || matchType == getHdxType())
//             list.add(this);
//         for (Node n = getFirstChild(); n != null; n = n.getNextSibling())
//             if (n instanceof HdxElement)
//                 ((HdxElement)n).addElementToList(matchType, list);
//         return;
//     }

    /** 
     * Returns a NodeList of all the descendant Elements with a given
     * local name and namespace URI in the order in which they are
     * encountered in a preorder traversal of this Element tree.  In
     * this implementation this is always an empty list, since this
     * tree always represents elements in the empty namespace.
     */
    public NodeList getElementsByTagNameNS(String namespaceURI, 
                                           String localName) {
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

    public String getLocalName() {
        return giName;
    }
    
    public boolean hasAttributes() {
        return attMap != null && attMap.size() != 0;
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
                /*
                 * We can't just produce a list of the Attr nodes in
                 * attMap, since their `name' fields may have
                 * namespace prefixes (correctly, since they're
                 * probably in a backing DOM), and the elements and
                 * attributes in this Hdx DOM are required to be in no
                 * namespace.  So make new Attr nodes on the fly.
                 */
                if (attMap != null) {
                    Document doc = getOwnerDocument();
                    assert doc != null;
                    for (Iterator mi = attMap.keySet().iterator();
                         mi.hasNext();
                         ) {
                        String key = (String)mi.next();
                        Attr att = doc.createAttribute(key);
                        att.setValue(((Attr)attMap.get(key)).getValue());
                        list.add(att);
                    }
                }
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
     * Does a deep copy of this Object
     */
    public Object clone() {
        HdxElement n = (HdxElement)super.clone();
        if (attMap != null)
            for (java.util.Iterator ai = attMap.keySet().iterator();
                 ai.hasNext(); ) {
                String key = (String)ai.next();
                Attr att = (Attr)attMap.get(key);
                n.addAtt(key, (Attr)att.cloneNode(false));
            }
        return n;
    }

    /**
     * Add a mapping to the attMap, first making sure that it's non-null.
     */
    private void addAtt(String name, Attr value) {
        if (attMap == null)
            attMap = new java.util.HashMap();
        attMap.put(name, value);
    }
        
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
}
