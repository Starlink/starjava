// $Id$

package uk.ac.starlink.hdx;

import org.w3c.dom.*;

/**
 * Hdx implementation of the <code>org.w3c.dom.Document</code>
 * interface.  This implements allows clients to create mutually
 * consistent implementations of the various <code>org.w3c.dom</code>
 * interfaces.  It also extends the interface by adding creation
 * method {@link #createElement(HdxResourceType,DOMFacade)}.
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
 */
public class HdxDocument
        extends HdxNode
        implements Document {

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
     * <code>DOMFacade</code>.  The <code>tagName</code> of the
     * resulting element is that corresponding to the <code>HdxResourceType</code>.
     *
     * <p>This is an extension to the <code>org.w3c.dom.Document</code>
     * interface.
     *
     * @param type the type of the element to create, which must not be null,
     * nor {@link HdxResourceType#NONE}
     *
     * @param facade an implementation of the <code>DOMFacade</code>
     * interface
     *
     * @throws IllegalArgumentException if the type or facade is invalid.
     */
    public Element createElement(HdxResourceType type, DOMFacade facade)
            throws DOMException {
        return new HdxFacadeElement(type, this, facade);
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
                    //sb.append(((HdxNode)kid).toXML());
                    sb.append(((HdxNode)kid).getNodeName());
                    sb.append("+++");
                }
                sb.append(')');
                return sb.toString();
            }
        }
        return new HdxDocumentFragment(this);
    }
    
    public Text createTextNode(String data) {
        class HdxText extends HdxCharacterData implements Text {
            HdxText(String data) {
                super(Node.TEXT_NODE, HdxDocument.this, data);
            }
            public String getNodeName() {
                return "#text";
            }
            public Text splitText(int offset) 
                    throws DOMException {
                throw new DOMException
                    (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                     "HdxText: text node read-only");
            }
//             public String toXML() {
//                 return content;
//             }
        }
        return new HdxText(data);
    }

    public Comment createComment(String data) {
        class HdxComment extends HdxCharacterData implements Comment {
            HdxComment (String data) {
                super(Node.COMMENT_NODE, HdxDocument.this, data);
            }
            public String getNodeName() {
                return "#comment";
            }
//             public String toXML() {
//                 return "<!--" + content + "-->";
//             }
        }
        
        return new HdxComment(data);
    }
    
    public CDATASection createCDATASection(String data)
            throws DOMException {
        throw new DOMException
            (DOMException.NOT_SUPPORTED_ERR,
             "HdxDocument does not support CDATA sections");
    }
    
    public ProcessingInstruction createProcessingInstruction (String target, 
                                                              String data)
            throws DOMException {
        throw new DOMException
            (DOMException.NOT_SUPPORTED_ERR,
             "HdxDocument does not support Processing Instructions");
    }
        
    public Attr createAttribute(String name)
            throws DOMException {
        final class HdxAttr extends HdxNode implements Attr {
            private String name;
            private String value;
            public HdxAttr(String name, Document owner) {
                super(Node.ATTRIBUTE_NODE, owner);
                this.name = name;
                value = null;
            }
            public String getName() { return name; }
            public String getLocalName() { return name; } // override HdxNode
            public String getNodeName() { return name; } // override HdxNode
            public Element getOwnerElement() {
                Node n = getParentNode();
                assert n instanceof HdxElement;
                return (Element)n;
            }
            public boolean getSpecified() { return true; }
            public String getValue() { return value; }
            public String getNodeValue() { return value; } // override HdxNode
            public void setValue(String value) {
                this.value = value;
            }
        }
        Attr att = new HdxAttr(name, this);
        //System.err.println("createAttribute("+name+")");
        return att;
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
          case Node.ELEMENT_NODE: 
              {
                  retNode = createElement(importedNode.getNodeName());
                  NamedNodeMap atts = importedNode.getAttributes();
                  for (int i=0; i<atts.getLength(); i++)
                      ((Element)retNode).setAttributeNode((Attr)atts.item(i));
                  break;
              }

          case Node.TEXT_NODE:
            retNode = createTextNode(((Text)importedNode).getData());
            break;
          
          default:
            retNode = createTextNode( importedNode.getNodeValue() );
          //   System.out.println( "Failed to import node:" + importedNode );
          //  throw new DOMException
          //      (DOMException.NOT_SUPPORTED_ERR,
          //       "HdxDocument.importNode: importing node type "
          //       + importedNode.getNodeType() + " not supported");
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
    
//     public String toXML() {
//         HdxElement el = (HdxElement)getDocumentElement();
//         if (el == null)
//             return "";
//         else
//             return el.toXML();
//     }

    static private class HdxFacadeElement
            extends HdxNode implements Element {

        HdxResourceType resourceType;
        DOMFacade facade;

        /**
         * Creates a new HdxFacadeElement.  The {@link
         * DOMFacade#getDOM} of the <code>facade</code> object must
         * return an element which matches <code>resourceType</code>.
         *
         */
        HdxFacadeElement (HdxResourceType resourceType,
                          Document owner,
                          DOMFacade facade) {
            super(Node.ELEMENT_NODE, owner);
            if (resourceType == null
                || resourceType == HdxResourceType.NONE
                || facade == null)
                throw new IllegalArgumentException 
                    ("HdxFacadeElement: null Hdx type or facade");
            this.resourceType = resourceType;
            this.facade = facade;
        }

        /** 
         * Obtains the Java object corresponding to this node, by
         * obtaining it in turn from the facade.
         *
         * @return a Java object, which will always be non-null
         * @throws PluginException (unchecked exception) if the facade's 
         * {@link DOMFacade#getObject} method fails.
         * @see HdxNode#getNodeObject
         */
        public Object getNodeObject() {
            assert facade != null;
            try {
                Object ret = facade.getObject(this);
                assert ret != null;
                System.err.println
                  ("HdxDocument.HdxFacadeElement.getNodeObject returned class "
                   + ret.getClass().getName());
                return ret;
            } catch (HdxException ex) {
                // oh dear, the implementation of DOMFacade is faulty
                // -- convert this to an error
                throw new PluginException
                      ("facade.getObject() returned a null object for element "
                       + getTagName());
            }
        }

        /**
         * Gets the DOM from the facade.
         *
         * @throws PluginException (unchecked exception) if the object
         * returned by {@link DOMFacade#getDOM} does not match the type
         * {@link #resourceType} declared in the constructor.
         */
        private HdxElement getDOM() {
            Element el = facade.getDOM(null);
            assert el instanceof HdxElement;
            if (! el.getTagName().equals(resourceType.xmlName()))
                throw new PluginException
                    ("Facade.getDOM(null) returned " + el.getTagName()
                     + ", not " + resourceType.xmlName() + " as required");
            return (HdxElement)el;
        }

        public String getTagName() {
            return resourceType.xmlName();
        }

        public String getAttribute(String name) {
            return getDOM().getAttribute(name);
        }
        
        public Attr getAttributeNode(String name) {
            return getDOM().getAttributeNode(name);
        }
        
        public NodeList getElementsByTagName(String name) {
            return getDOM().getElementsByTagName(name);
        }
        
        public String getAttributeNS(String namespaceURI, 
                                     String localName) {
            return getDOM().getAttributeNS(namespaceURI, localName);
        }
        
        public Attr getAttributeNodeNS(String namespaceURI, 
                                       String localName) {
            return getDOM().getAttributeNodeNS(namespaceURI, localName);
        }
        
        public NodeList getElementsByTagNameNS(String namespaceURI, 
                                               String localName) {
            return getDOM().getElementsByTagNameNS(namespaceURI, localName);
        }
        
        public boolean hasAttribute(String name) {
            return getDOM().hasAttribute(name);
        }
        
        public boolean hasAttributeNS(String namespaceURI, 
                                      String localName) {
            return getDOM().hasAttributeNS(namespaceURI, localName);
        }
        
        public void setAttribute(String name, 
                                 String value)
                throws DOMException {
            if (! facade.setAttribute(this, name, value))
                // Either the setAttribute failed or (more likely)
                // this facade does not support modifications by this
                // route.
                throw new DOMException
                    (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                     "HdxFacadeElement: facade does not support modification");
        }
        
        public void removeAttribute(String name)
                throws DOMException {
            // XXX the org.w3c.dom.Element documentation does not say what to 
            // do when the attribute does not exist
            if (! facade.setAttribute(this, name, null))
                // Either the setAttribute failed or (more likely)
                // this facade does not support modifications by this
                // route.
                throw new DOMException
                    (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                     "HdxFacadeElement: facade does not support modification");
        }
        
        public Attr setAttributeNode(Attr newAttr)
                throws DOMException {
            Attr orig = getAttributeNode(newAttr.getName());
            setAttribute(newAttr.getName(), newAttr.getValue());
            return orig;
        }
        
        public Attr removeAttributeNode(Attr oldAttr)
                throws DOMException {
            Attr old = getAttributeNode(oldAttr.getName());
            if (old == null)
                throw new DOMException
                    (DOMException.NOT_FOUND_ERR,
                     "Attribute " + oldAttr + " not found");
            removeAttribute(oldAttr.getName());
            return old;
        }
        
        public void setAttributeNS(String namespaceURI, 
                                   String qualifiedName, 
                                   String value)
                throws DOMException {
            throw new DOMException
                (DOMException.NOT_SUPPORTED_ERR,
                 "HdxFacadeElement does not support namespaces");
        }
        
        public void removeAttributeNS(String namespaceURI, 
                                      String localName)
                throws DOMException {
            throw new DOMException
                (DOMException.NOT_SUPPORTED_ERR,
                 "HdxFacadeElement does not support namespaces");
        }
        
        public Attr setAttributeNodeNS(Attr newAttr)
                throws DOMException {
            throw new DOMException
                (DOMException.NOT_SUPPORTED_ERR,
                 "HdxFacadeElement does not support namespaces");
        }
            

        /* Following override selected methods in HdxNode */
        public String getNodeName() {
            return getTagName();
        }
        
        public String getNodeValue()
                throws DOMException {
            return null;
        }
        
        public void setNodeValue(String nodeValue)
                throws DOMException {
            return;
        }
        
        public NodeList getChildNodes() {
            return getDOM().getChildNodes();
        }
        
        public Node getFirstChild() {
            return getDOM().getFirstChild();
        }
        
        public Node getLastChild() {
            return getDOM().getLastChild();
        }
        
        public NamedNodeMap getAttributes() {
            return getDOM().getAttributes();
        }
        
        public Node insertBefore(Node newChild, Node refChild)
                throws DOMException {
            if (! facade.addChildBefore(this,
                                        (Element)newChild, (Element)refChild))
                throw new DOMException
                    (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                     "Can't HdxFacadeElement.insertBefore: is read-only");
            return newChild;
        }
            
        public Node appendChild(Node newChild)
                throws DOMException {
            if (! facade.addChildBefore(this, (Element)newChild, null))
                throw new DOMException
                    (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                     "Can't HdxFacadeElement.appendChild: is read-only");
            return newChild;
        }
        
        public Node replaceChild(Node newChild, Node oldChild)
                throws DOMException {
            if (! facade.replaceChild(this,
                                      (Element)oldChild, (Element)newChild))
                throw new DOMException
                    (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                     "Can't HdxFacadeElement.replaceChild: is read-only");
            return oldChild;
        }
        
        public Node removeChild(Node oldChild)
                throws DOMException {
            if (! facade.replaceChild(this, (Element)oldChild, null))
                throw new DOMException
                    (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                     "Can't HdxFacadeElement.removeChild: is read-only");
            return oldChild;
        }
        
        public boolean hasChildNodes() {
            return getDOM().hasChildNodes();
        }
        
        public Node cloneNode(boolean deep) {
            throw new UnsupportedOperationException
                ("HdxFacadeElement: cloneNode not supported yet");
        }
        
        public boolean hasAttributes() {
            return getDOM().hasAttributes();
        }
    }

    private class HdxCharacterData
            extends HdxNode
            implements CharacterData {
        String content;
        HdxCharacterData(short nodeType, Document owner, String content) {
            super(nodeType, owner);
            this.content = content;
        }

        public String getData()
                throws DOMException {
            return content;
        }

        public String getNodeValue() {
            // Override HdxNode.getNodeValue
            return getData();
        }
            
        public void setData(String data)
                throws DOMException {
            throw new DOMException
                (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                 "HdxComment is unmodifiable");
        }
            
        public int getLength() {
            return content.length();
        }

        public String substringData(int offset, 
                                    int count)
                throws DOMException {
            if (offset < 0 || offset >= content.length() || count < 0)
                throw new DOMException
                    (DOMException.INDEX_SIZE_ERR,
                     "HdxComment: invalid offset");
            int end = offset + count;
            if (end > content.length())
                end = content.length();
            return content.substring(offset, end);
        }
            
        public void appendData(String arg)
                throws DOMException {
            throw new DOMException
                (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                 "HdxComment is unmodifiable");
        }

        public void insertData(int offset, String arg)
                throws DOMException {
            throw new DOMException
                (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                 "HdxComment is unmodifiable");
        }

        public void deleteData(int offset, int count)
                throws DOMException {
            throw new DOMException
                (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                 "HdxComment is unmodifiable");
        }

        public void replaceData(int offset, int count, String arg)
                throws DOMException {
            throw new DOMException
                (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                 "HdxComment is unmodifiable");
        }
    }
    
}
