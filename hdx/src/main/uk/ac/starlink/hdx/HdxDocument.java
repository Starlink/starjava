// $Id$

package uk.ac.starlink.hdx;

import uk.ac.starlink.util.DOMUtils;

import java.net.URI;
import java.util.logging.Level;

import org.w3c.dom.*;

/**
 * Hdx implementation of the <code>org.w3c.dom.Document</code>
 * interface.  This implements allows clients to create mutually
 * consistent implementations of the various <code>org.w3c.dom</code>
 * interfaces.  It also extends the interface by adding creation
 * method {@link #createElement(HdxFacade)}.
 *
 * <p>To avoid confusion, note that, despite its name,
 * <code>HdxDocumentFactory</code> is <em>not</em> a general factory
 * for creating empty instances of {@link HdxDocument}.  If you wish
 * to create a blank <code>HdxDocument</code> (perhaps because you are
 * implementing {@link HdxDocumentFactory#makeHdxDocument}), you do so
 * simply with a call to
 * <pre>
 *   HdxDOMImplementation.getInstance().createDocument(null,&lt;el&gt;,null)
 * </pre>
 * (where <code>&lt;el&gt;</code> represents the name of the document element.
 *
 * <p>Note that only DOM Level 2 methods are currently implemented.
 * If this class is built using JDK1.5, then the DOM Level 3 methods
 * will be present, but they do not implement the functionality
 * defined by the DOM Level 3 specification (mostly they throw
 * NOT_SUPPORTED_ERR type DOMExceptions).
 *
 * @author Norman Gray, Starlink
 * @version $Id$
 */
public class HdxDocument
        extends HdxNode
        implements Document {

    private static java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger("uk.ac.starlink.hdx");

    public HdxDocument() {
        super(Node.DOCUMENT_NODE, null);
    }

    public DocumentType getDoctype() {
        return null;
    }

    public String getNodeName() {
        return "#document";
    }
    
    public DOMImplementation getImplementation() {
        return HdxDOMImplementation.getInstance();
    }

    public Element getDocumentElement() {
        for (Node kid=getFirstChild(); kid!=null; kid=kid.getNextSibling())
            if (kid.getNodeType() == Node.ELEMENT_NODE) {
                assert kid instanceof HdxElement;
                return (Element)kid;
            }
        return null;
    }
    
    public Element createElement(String tagName)
            throws DOMException {
        return new HdxElement(tagName, this);
    }

    /**
     * Creates an element which manages its children using a
     * <code>HdxFacade</code>.  The <code>tagName</code> of the
     * resulting element is that corresponding to the <code>HdxResourceType</code>.
     *
     * <p>This is an extension to the <code>org.w3c.dom.Document</code>
     * interface.
     *
     * <p>The resulting element has the property that if it is cloned
     * using <code>clone()</code>, the resulting element is also a
     * facade for the underlying object; if it is cloned using
     * <code>cloneNode</code>, however, the resulting element is an
     * ordinary element, which is now <em>independent</em> of the
     * underlying object.
     *
     * @param facade an implementation of the <code>HdxFacade</code>
     * interface
     *
     * @throws IllegalArgumentException if the facade is invalid.
     */
    public Element createElement(HdxFacade facade)
            throws DOMException {
        return new HdxFacadeElement(facade.getHdxResourceType(), this, facade);
    }
    
    public DocumentFragment createDocumentFragment() {
        final class HdxDocumentFragment
            extends HdxNode
            implements DocumentFragment {
            HdxDocumentFragment(Document owner) {
                super(Node.DOCUMENT_FRAGMENT_NODE, owner);
            }
            public String getNodeName() {
                return "#document-fragment";
            }
            public String toString() {
                StringBuffer sb = new StringBuffer();
                sb.append("HdxDocumentFragment(");
                for (Node kid = getFirstChild();
                     kid != null;
                     kid = kid.getNextSibling()) {
                    sb.append(((HdxNode)kid).getNodeName());
                    sb.append(',');
                }
                sb.append(')');
                return sb.toString();
            }
        }
        return new HdxDocumentFragment(this);
    }
    
    public Text createTextNode(String data) {
        return new HdxText(Node.TEXT_NODE, this, data);
    }

    public Comment createComment(String data) {
        class HdxComment extends HdxCharacterData implements Comment {
            HdxComment (String data) {
                super(Node.COMMENT_NODE, HdxDocument.this, data);
            }
            public String getNodeName() {
                return "#comment";
            }
        }
        
        return new HdxComment(data);
    }
    
    public CDATASection createCDATASection(String data)
            throws DOMException {
        class HdxCDATASection extends HdxText implements CDATASection {
            HdxCDATASection(Document owner, String data) {
                super(Node.CDATA_SECTION_NODE, owner, data);
            }
            public String getNodeName() {
                return "#cdata-section";
            }
        }
        return new HdxCDATASection(this, data);
    }
    
    public ProcessingInstruction createProcessingInstruction (String target, 
                                                              String data)
            throws DOMException {
        class HdxProcessingInstruction
                extends HdxNode
                implements ProcessingInstruction {
            private String target;
            private String data;
            HdxProcessingInstruction(Document owner,
                                     String target,
                                     String data) {
                super(Node.PROCESSING_INSTRUCTION_NODE, owner);
                this.target = target;
                this.data = data;
            }
            public String getTarget() {
                return target;
            }
            public String getData() {
                return data;
            }
            public void setData(String data) {
                this.data = data;
            }
            public String getNodeName() {
                return target;
            }
            public String getNodeValue() {
                return data;
            }
            public void setNodeValue(String nodeValue) {
                data = nodeValue;
            }
            public Object clone() {
                HdxProcessingInstruction c
                        = (HdxProcessingInstruction)super.clone();
                c.target = new String(target);
                c.data = new String(data);
                return c;
            }
            public boolean equals(Object o) {
                if (o == null || !(o instanceof HdxProcessingInstruction))
                    return false;
                HdxProcessingInstruction ho = (HdxProcessingInstruction)o;
                return target.equals(ho.target) && data.equals(ho.data);
            }
            public int hashCode() {
                return new HdxNode.HashCode(target.hashCode())
                        .add(data.hashCode())
                        .value();
            }
        }
        return new HdxProcessingInstruction(this, target, data);
    }
        
    public Attr createAttribute(String name)
            throws DOMException {
        return HdxElement.newHdxAttr(name, this);
    }

    public EntityReference createEntityReference(String name)
            throws DOMException {
        throw new DOMException
            (DOMException.NOT_SUPPORTED_ERR,
             "HdxDocument does not support Entity References");
    }
        
    public NodeList getElementsByTagName(String tagname) {
        return getDocumentElement().getElementsByTagName(tagname);
    }
    
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

   public Node importNode(Node importedNode, boolean deep) 
            throws DOMException {
        Node retNode = null;
        switch (importedNode.getNodeType()) {
            // Although the Hdx DOMs (normally created using this class) do
            // not usually have any nodes other than Elements, there is no
            // reason to make it impossible to create DOMs with other
            // node types.  Plus, all the types other than Document,
            // Element and Attr are pretty trivial to implement.

          case Node.ATTRIBUTE_NODE:
              {
                  Attr a = (Attr)importedNode;
                  retNode = createAttribute(a.getName());
                  ((Attr)retNode).setValue(a.getValue());
                  // An Attr node may have either Text or
                  // EntityReference children; HdxAttr does not
                  // support EntityReference children.  The Text child
                  // is generated on the fly by HdxAttr, so doesn't
                  // need to be copied explicitly here.
                  break;
              }

          case Node.CDATA_SECTION_NODE:
            retNode = createCDATASection
                    (((CDATASection)importedNode).getData());
            break;

          case Node.COMMENT_NODE:
            retNode = createComment(((Comment)importedNode).getData());
            break;

          case Node.DOCUMENT_FRAGMENT_NODE:
            // The org.w3c.dom.Document#importNode documentation is
            // rather ambiguous about what happens to the children.  I
            // read it as indicating that the result should be a DocumentFragment
            retNode = createDocumentFragment();
            break;
            
          case Node.DOCUMENT_NODE:
          case Node.DOCUMENT_TYPE_NODE:
            throw new DOMException
                    (DOMException.NOT_SUPPORTED_ERR,
                     "Nodes of type "
                     + DOMUtils.mapNodeType(importedNode.getNodeType())
                     + " cannot be imported");
            // NOT REACHED            

          case Node.ELEMENT_NODE:
              {
                  retNode = createElement(importedNode.getNodeName());
                  NamedNodeMap atts = importedNode.getAttributes();
                  for (int i=0; i<atts.getLength(); i++)
                      ((Element)retNode).setAttributeNode
                              ((Attr)importNode(atts.item(i), deep));
                  break;
              }

          case Node.ENTITY_NODE:
          case Node.ENTITY_REFERENCE_NODE:
          case Node.NOTATION_NODE:
            throw new DOMException
                    (DOMException.NOT_SUPPORTED_ERR,
                     "HdxDocument.importNode: importing node type "
                     + importedNode.getNodeType()
                     + '='
                     + DOMUtils.mapNodeType(importedNode.getNodeType())
                     + " not supported");
            // NOT REACHED

          case Node.PROCESSING_INSTRUCTION_NODE:
              {
                  ProcessingInstruction pi
                          = (ProcessingInstruction)importedNode;
                  retNode = createProcessingInstruction(pi.getTarget(),
                                                        pi.getData());
                  break;
              }
              
          case Node.TEXT_NODE:
            retNode = createTextNode(((Text)importedNode).getData());
            break;

            
          default:
            // This is a can't-happen error -- the above list of cases
            // should include all of the node types documented in the
            // Node interface.
            throw new DOMException
                    (DOMException.NOT_SUPPORTED_ERR,
                     "HdxDocument.importNode: Unrecognised node type "
                     + importedNode.getNodeType()
                     + '='
                     + DOMUtils.mapNodeType(importedNode.getNodeType())
                     + " not supported");
            // NOT REACHED
        }

        if (deep)
            for (Node n=importedNode.getFirstChild();
                 n != null;
                 n = n.getNextSibling())
                retNode.appendChild(importNode(n, true));
        return retNode;
    }
    
    public Element createElementNS(String namespaceURI, 
                                   String qualifiedName)
            throws DOMException {
        throw new DOMException (DOMException.NOT_SUPPORTED_ERR,
                                "createElementNS not supported");
    }

    public Attr createAttributeNS(String namespaceURI, 
                                  String qualifiedName)
            throws DOMException {
        throw new DOMException (DOMException.NOT_SUPPORTED_ERR,
                                "createAttributeNS not supported");
    }

    public Element getElementById(String elementId) {
        return null;
    }

//DOM3     /* ** DOM Level 3 Not Implemented ** */
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public Node renameNode( Node n, String namespaceURI,
//DOM3                             String qualifiedName ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "renameNode not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public void normalizeDocument() {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "normalizeDocument not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public DOMConfiguration getDomConfig() {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "getDomConfig not implemented" );
//DOM3     }
//DOM3 
//DOM3     public  Node adoptNode( Node source ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "adoptNode not implemented" );
//DOM3         // Could be a call to importNode( source, true ),
//DOM3         // but we need to remove from other document?
//DOM3     }
//DOM3 
//DOM3     public void setDocumentURI( String documentURI ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "setDocumentURI not implemented" );
//DOM3     }
//DOM3 
//DOM3     public String getDocumentURI() {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "getDocumentURI not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public void setStrictErrorChecking(boolean check) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                       "setStrictErrorChecking not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public boolean getStrictErrorChecking() {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                       "getStrictErrorChecking not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public void setXmlVersion(String value) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "setXmlVersion not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public String getXmlVersion() {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "getXmlVersion not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public void setXmlStandalone(boolean value) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "setXmlStandalone not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public boolean getXmlStandalone() {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "getXmlStandalone not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public void setXmlEncoding(String value) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "setXmlEncoding not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public String getXmlEncoding() {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "getXmlEncoding not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public String getInputEncoding() {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "getInputEncoding not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public void setInputEncoding(String value) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "setInputEncoding not implemented" );
//DOM3     }


    private static class HdxFacadeElement
            extends HdxElement implements Cloneable {

        private final HdxResourceType resourceType;
        private HdxFacade facade; // really final, but assigned in clone()
        private boolean suppressFacade;
        private Object facadeToken;
        
        HdxFacadeElement(HdxResourceType resourceType,
                         Document owner,
                         HdxFacade facade) {
            super(resourceType == null ? null : resourceType.xmlName(),
                  owner);

            if (resourceType == null
                || resourceType == HdxResourceType.NONE
                || facade == null)
                throw new IllegalArgumentException 
                    ("HdxFacadeElement: null Hdx type or facade");

            this.resourceType = resourceType;
            this.facade = facade;
            assert this.resourceType != null && this.facade != null;
            suppressFacade = false;
            facadeToken = null;
        }

        /**
         * Determines whether we're to use a facade when mutating the
         * element, below.  If <code>suppressFacade</code> is true on
         * this or any ancestor <code>HdxFacadeElement</code>, then we
         * don't use the facade; if not, we do.
         *
         * @return true if we should use the facade when we mutate
         * this element
         * @see #updateDOM
         */
        private boolean useFacade() {
            if (suppressFacade)
                return false;
            for (Node n = getParentNode(); n != null; n = n.getParentNode()) {
                assert n instanceof HdxNode;
                if (n instanceof HdxFacadeElement
                    && ((HdxFacadeElement)n).suppressFacade)
                    return false;
            }
            return true;
        }

        /** 
         * Obtains the Java object corresponding to this node, by
         * obtaining it in turn from the facade.
         *
         * @return a Java object, which will always be non-null
         * @throws PluginException (unchecked exception) if the facade's 
         * {@link HdxFacade#getObject} method fails.
         * @see HdxNode#getNodeObject
         */
        public Object getNodeObject() {
            assert facade != null;
            try {
                Object ret = facade.getObject(this);
                assert ret != null;
                return ret;
            } catch (HdxException ex) {
                // oh dear, the implementation of HdxFacade is faulty
                // -- convert this to an error
                throw new PluginException
                      ("facade.getObject() returned a null object for element "
                       + getTagName());
            }
        }

        /**
         * Calls <code>synchronizeElement</code> to make sure that
         * this element is up-to-date with respect to the element.
         * This method is (currently) only called within methods which
         * do not throw <code>DOMException</code>s, so if
         * <code>synchronizeElement</code> throws its
         * <code>HdxException</code> (which is documented in {@link
         * HdxFacade} to be tantamount to a can't-happen error), we
         * have little option but to convert it to a (unchecked)
         * {@link PluginException}.
         *
         * <p>This method brackets the call to
         * <code>synchronizeElement</code> by setting
         * <code>suppressFacade</code> to be true.  This means that when
         * the implementation of that method calls the DOM-mutator
         * methods below, we do not recursively invoke
         * <code>synchronizeElement</code>.
         */
        private void updateDOM() {
            try {
                if (! useFacade())
                    return;
                suppressFacade = true;
                facadeToken = facade.synchronizeElement(this, facadeToken);
                suppressFacade = false;
            } catch (HdxException ex) {
                throw new PluginException(ex);
            }
        }

        /* ******************** Element interface ******************** */
        public boolean hasAttribute(String name) {
            if (name == null)
                throw new IllegalArgumentException("name is null");
            updateDOM();
            return super.hasAttribute(name);
        }
        
        public String getAttribute(String name) {
            if (name == null)
                throw new IllegalArgumentException("name is null");
            updateDOM();
            return super.getAttribute(name);
        }
        public Attr getAttributeNode(String name) {
            if (name == null)
                throw new IllegalArgumentException("name is null");
            updateDOM();
            return super.getAttributeNode(name);
        }
        public String getAttributeNS(String namespaceURI, String name) {
            if (name == null)
                throw new IllegalArgumentException("name is null");
            if (namespaceURI == null)
                updateDOM();
            return super.getAttributeNS(namespaceURI, name);
        }
        public Attr getAttributeNodeNS(String namespaceURI, String name) {
            if (name == null)
                throw new IllegalArgumentException("name is null");
            if (namespaceURI == null)
                updateDOM();
            return super.getAttributeNodeNS(namespaceURI, name);
        }
        
        void setAttribute(String name, String value, boolean useBacking)
                throws DOMException {
            if (name == null || value == null)
                throw new IllegalArgumentException("name or value is null");
            if (useBacking)
                setAttribute(name, value);
            else
                super.setAttribute(name, value);
        }

        public void setAttribute(String name, 
                                 String value)
                throws DOMException {
            if (name == null || value == null)
                throw new IllegalArgumentException("name or value is null");
            if (useFacade()
                && !facade.setAttribute(this, name, value))
                    // Either the setAttribute failed or (more likely)
                    // this facade does not support modifications by this
                    // route.
                    readonlyFacade();
            super.setAttribute(name, value);
        }
        public Attr setAttributeNode(Attr att)
                throws DOMException {
            if (att == null)
                throw new IllegalArgumentException("att is null");
            if (useFacade()
                && !facade.setAttribute(this, att.getName(), att.getValue()))
                    readonlyFacade();
            return super.setAttributeNode(att);
        }
        public void setAttributeNS(String namespaceURI,
                                   String name, 
                                   String value)
                throws DOMException {
            if (name == null || value == null)
                throw new IllegalArgumentException("name or value is null");
            if (useFacade()
                && namespaceURI != null
                && !facade.setAttribute(this, name, value))
                    // Either the setAttribute failed or (more likely)
                    // this facade does not support modifications by this
                    // route.
                    readonlyFacade();
            super.setAttributeNS(namespaceURI, name, value);
        }
        public Attr setAttributeNodeNS(Attr att)
                throws DOMException {
            if (att == null)
                throw new IllegalArgumentException("att is null");
            if (useFacade()
                && att.getNamespaceURI() != null
                && !facade.setAttribute(this, att.getName(), att.getValue()))
                    readonlyFacade();
            return super.setAttributeNodeNS(att);
        }
        
        public void removeAttribute(String name)
                throws DOMException {
            if (name == null)
                throw new IllegalArgumentException("name is null");
            if (useFacade()
                && !facade.setAttribute(this, name, null))
                readonlyFacade();
            super.removeAttribute(name);
        }
        public Attr removeAttributeNode(Attr oldAttr)
                throws DOMException {
            if (oldAttr == null)
                throw new IllegalArgumentException("oldAttr is null");
            if (useFacade()
                && !facade.setAttribute(this, oldAttr.getName(), null))
                readonlyFacade();
            return super.removeAttributeNode(oldAttr);
        }
        public void removeAttributeNS(String namespaceURI, String name)
                throws DOMException {
            if (name == null)
                throw new IllegalArgumentException("name is null");
            if (useFacade()
                && namespaceURI != null
                && !facade.setAttribute(this, name, null))
                readonlyFacade();
            super.removeAttributeNS(namespaceURI, name);
        }
        
        /* Following override selected methods in HdxNode */
        public NamedNodeMap getAttributes() {
            updateDOM();
            return super.getAttributes();
        }
        
        public Node insertBefore(Node newChild, Node refChild)
                throws DOMException {
            if (newChild == null)
                throw new IllegalArgumentException("newChild is null");
            if (useFacade()
                && !facade.addChildBefore(this,
                                          (Element)newChild,
                                          (Element)refChild))
                readonlyFacade();
            return super.insertBefore(newChild, refChild);
        }
            
        public Node appendChild(Node newChild)
                throws DOMException {
            if (newChild == null)
                throw new IllegalArgumentException("newChild is null");
            if (useFacade()
                && !facade.addChildBefore(this, (Element)newChild, null))
                readonlyFacade();
            return super.appendChild(newChild);
        }
        
        public Node replaceChild(Node newChild, Node oldChild)
                throws DOMException {
            if (newChild == null)
                throw new IllegalArgumentException("newChild is null");
            if (useFacade()
                && !facade.replaceChild(this,
                                        (Element)oldChild,
                                        (Element)newChild))
                readonlyFacade();
            return super.replaceChild(newChild, oldChild);
        }
        
        public Node removeChild(Node oldChild)
                throws DOMException {
            if (oldChild == null)
                throw new IllegalArgumentException("oldChild is null");
            if (useFacade()
                && !facade.replaceChild(this, (Element)oldChild, null))
                readonlyFacade();
            return super.removeChild(oldChild);
        }

        private void readonlyFacade()
                throws DOMException {
            throw new DOMException
                    (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                     "HdxFacadeElement: no modification supported");
        }

        public Node getFirstChild() {
            updateDOM();
            return super.getFirstChild();
        }
        
        public Node getLastChild() {
            updateDOM();
            return super.getLastChild();
        }
        
        public boolean hasChildNodes() {
            updateDOM();
            return super.hasChildNodes();
        }
        

        /**
         * Returns a duplicate of this Node.
         *
         * <p>For Facade elements, this method has significantly
         * different behaviour from <code>clone()</code>.  If we clone
         * the Node using this method, then the result is <em>not</em>
         * a <code>HdxFacade</code> element, and is independent of the
         * object which backs it.  With <code>clone()</code> the
         * resulting object is also a Facade.
         *
         * @param deep if true, recursively clone the subtree under the
         * specified node; if false, clone only the node itself (and its
         * attributes, if it is an Element)
         * @return the duplicate node
         */
        public Node cloneNode(boolean deep) {
            return super.cloneNode(deep); // NOT a HdxFacadeElement
//             HdxElement newel =
//                     (HdxElement)getOwnerDocument().createElement(getTagName());
//             // Following copied from HdxElement.cloneNode(boolean)
//             newel.attributeMap = (AttributeMap)attributeMap.clone();
//             for (Iterator ai = newel.attributeMap.iterator(); ai.hasNext(); ) {
//                 String n = (String)ai.next();
//                 HdxAttr attr = (HdxAttr)el.attributeMap.get(n);
//                 attr.setOwnerElement(newel);
//             }
//             // Clone children.
//             // Following copied from HdxNode.cloneNode(boolean).
//             //
//             // XXX should we refactor these two cloneNode methods to
//             // make them usable from here?  The reason why we can't
//             // use them from here _right_ now is that
//             // HdxNode.cloneNode() still uses HdxNode.clone(false)
//             // rather than creating a new element using
//             // HdxDocument.createElement.  Is that a bad thing?
//             newel.parent = null;    // cloned node has no parent
//             newel.nextSibling = null; // ...and no siblings
//             newel.previousSibling = null;
//             if (deep) {
//                 for (HdxNode kid = (HdxNode)getFirstChild(); 
//                      kid != null;
//                      kid = (HdxNode)kid.getNextSibling())
//                 {
//                     newel.appendChild(kid.cloneNode(true));
//                 }
//             }
//             return newel;
        }
        
        public boolean hasAttributes() {
            updateDOM();
            return super.hasAttributes();
        }

        public Object clone() {
            HdxFacadeElement fe = (HdxFacadeElement)super.clone(false);
            fe.facade = (HdxFacade)facade.clone();
            if (facadeToken != null) {
                try {
                    // The following is essentially
                    //      fe.facadeToken = facadeToken.clone();
                    // but elaborately wrapped since clone is a
                    // protected method of Object.
                    Class ftc = facadeToken.getClass();
                    assert ftc != null; // getClass() always succeeds
                    java.lang.reflect.Method ftclone
                            = ftc.getMethod("clone", null);
                    System.err.println("cloning " + ftc.getName()
                                       + " with method " + ftclone
                                       + ", is accessible? "
                                       + ftclone.isAccessible());
                    fe.facadeToken = ftclone.invoke(this, null);
                } catch (IllegalAccessException ex) {
                    throw new IllegalArgumentException
                            ("Illegal access to clone: " + ex);
                } catch (Exception ex) {
                    // These are all `can't-happen' errors, because of
                    // broken coding above, so treat them as such (not
                    // sure whether IllegalArgumentException is best, but
                    // it suffices)
                    throw new IllegalArgumentException
                            ("Coding error: can't call clone!? " + ex);
                }
            }
            // No need to make a clone of resourceType -- should be singleton
            return fe;
        }

        public boolean equals(Object t) {
            if (t == null)
                return false;
            if (! (t instanceof HdxFacadeElement))
                return false;
            HdxFacadeElement tfe = (HdxFacadeElement)t;
            return resourceType == tfe.resourceType
                    && facade.equals(tfe.facade);
        }
        
        public int hashCode() {
            return new HdxNode.HashCode(resourceType.hashCode())
                    .add(facade.hashCode()).value();
        }

        public String toString() {
            return "HdxFacadeElement(" + resourceType + ")";
        }
    }
    
    private class HdxCharacterData
            extends HdxNode
            implements CharacterData {
        protected String content;
        private String chdataType;    // is this CharacterData, Comment, Text...?
        private StringBuffer sb;
        
        HdxCharacterData(short nodeType, Document owner, String content) {
            super(nodeType, owner);
            switch (nodeType) {
              case Node.COMMENT_NODE:
                chdataType = "Comment";
                break;
              case Node.CDATA_SECTION_NODE: // unused at present
                chdataType = "CDATA section";
                break;
              case Node.TEXT_NODE:
                chdataType = "Text";
                break;
              default:
                // This shouldn't happen -- these are the only types
                // of node which should inherit from CharacterData
                throw new AssertionError
                        ("HdxCharacterData does not support nodes of type "
                         + uk.ac.starlink.util.DOMUtils.mapNodeType(nodeType));
            }
            this.content = content;
        }

        public String getData()
                throws DOMException {
            if (sb != null) {
                content = sb.toString();
                sb = null;
            }
            return content;
        }

        public String getNodeValue() {
            // Override HdxNode.getNodeValue
            return getData();
        }

        public void setNodeValue(String value) // override HdxNode
                throws DOMException {
            setData(value);
        }
            
        public void setData(String data)
                throws DOMException {
            sb = null;
            content = data;
        }
            
        public int getLength() {
            return getData().length();
        }

        public String substringData(int offset, 
                                    int count)
                throws DOMException {
            if (sb != null) {
                content = sb.toString();
                sb = null;
            }
            if (offset < 0 || offset >= content.length() || count < 0)
                throw new DOMException
                    (DOMException.INDEX_SIZE_ERR,
                     chdataType + ": invalid offset");
            int end = offset + count;
            if (end > content.length())
                end = content.length();
            return content.substring(offset, end);
        }
            
        public void appendData(String arg)
                throws DOMException {
            if (sb == null) sb = new StringBuffer(content);
            sb.append(arg);
        }

        public void insertData(int offset, String arg)
                throws DOMException {
            if (sb == null) sb = new StringBuffer(content);
            sb.insert(offset, arg);
        }

        public void deleteData(int offset, int count)
                throws DOMException {
            if (sb == null) sb = new StringBuffer(content);
            sb.delete(offset, offset+count);
        }

        public void replaceData(int offset, int count, String arg)
                throws DOMException {
            if (sb == null) sb = new StringBuffer(content);
            sb.replace(offset, offset+count, arg);
        }

        // Object methods
        public Object clone() {
            Object c = super.clone();
            HdxCharacterData d = (HdxCharacterData)c;
            d.content = new String(getData());
            d.sb = null;
            return d;
        }
        public boolean equals(Object o) {
            if (o == null || !(o instanceof HdxCharacterData))
                return false;
            HdxCharacterData co = (HdxCharacterData)o;
            return getNodeType() == co.getNodeType()
                    && getData().equals(co.getData());
        }
        public int hashCode() {
            return getData().hashCode();
        }
    }

    private class HdxText
            extends HdxCharacterData
            implements Text {
        HdxText(short nodeType, Document owner, String content) {
            super(nodeType, owner, content);
        }
        public Text splitText(int offset) {
            HdxText newText = new HdxText(Node.TEXT_NODE,
                                          getOwnerDocument(),
                                          content.substring(offset));
            content = content.substring(0, offset);
            Node parent = getParentNode();
            if (parent != null)
                parent.insertBefore(newText, getNextSibling());
            return newText;
        }
        public String getNodeName() {
            return "#text";
        }

//DOM3         /* ** DOM Level 3 Not implemented ** */
//DOM3 
//DOM3         /** Not implemented */
//DOM3         public Text replaceWholeText(String content)
//DOM3             throws DOMException
//DOM3         {
//DOM3             throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                     "replaceWholeText not implemented" );
//DOM3         }
//DOM3 
//DOM3         /** Not implemented */
//DOM3         public String getWholeText() {
//DOM3             throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                     "getWholeText not implemented" );
//DOM3         }
//DOM3 
//DOM3         /** Not implemented */
//DOM3         public boolean isElementContentWhitespace() {
//DOM3             throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                       "isElementContentWhitespace not implemented" );
//DOM3         }
        

    }

    /**
     * Collection of rather ragged Node utilities.  These are
     * primarily convenience methods for debugging.
     */
    static public class NodeUtil {

        private static javax.xml.transform.Transformer trans;

        /**
         * Serialize a DOM to a String.  Debugging method. 
         *
         * @return String, or an empty string on error.
         */
        static public String serializeNode (Node n) {
            String ret;
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                if (trans == null) {
                    trans = javax.xml.transform.TransformerFactory
                            .newInstance()
                            .newTransformer();
                    trans.setOutputProperty
                          (javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION,
                           "yes");
                }
                trans.transform
                        (new javax.xml.transform.dom.DOMSource(n),
                         new javax.xml.transform.stream.StreamResult(sw));
                ret = sw.toString();
            } catch (javax.xml.transform.TransformerException ex) {
                if (logger.isLoggable(Level.WARNING))
                    logger.warning("Can't transform DOM: " + ex);
                ret = "";
            }
            return ret;
        }
    }
}
