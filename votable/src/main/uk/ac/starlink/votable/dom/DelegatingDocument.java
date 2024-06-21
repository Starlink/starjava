package uk.ac.starlink.votable.dom;

import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Notation;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * DOM Document implementation which delegates its operations to a
 * base Document instance.  This is the basic class which must be
 * used and extended when using the delegate DOM package.
 *
 * <p>In order to specialise the nodes which appear in a DOM, 
 * you have to override the protected <code>createDelegating*</code> 
 * methods to return your own custom DelegatingNode subclasses.
 *
 * @author   Mark Taylor (Starlink)
 * @since    14 Sep 2004
 */
public class DelegatingDocument extends DelegatingNode implements Document {

    private final Document base_;
    private final Map<Node,DelegatingNode> delegates_ =
        new HashMap<Node,DelegatingNode>();
    private String documentURI_;
 
    /**
     * Constructs a new document which delegates its behaviour to a
     * <code>Document</code> from another DOM.
     *
     * @param   base  delegate document node
     * @param   documentURI   location of the document, or <code>null</code>
     */
    public DelegatingDocument( Document base, String documentURI ) {
        super( base );
        setDocument( this );
        if ( base == null ) {
            throw new NullPointerException();
        }
        documentURI_ = documentURI;
        base_ = base;
    }

    /**
     * Constructs a new empty document based on a new empty document
     * got from the default DOM implementation.
     * This can theoretically result in a ParserConfigurationException, 
     * but shouldn't do for any sensibly set up JVM -
     * any such condition is rethrown as a RuntimeException.
     *
     * @param   documentURI   location of the document, or <code>null</code>
     */
    public DelegatingDocument( String documentURI ) {
        this( makeEmptyDocument(), documentURI );
    }

    /**
     * Returns the node in this document which delegates to (is based on)
     * a given node in the base document.  If this node has not previously
     * been encountered, it will be created here.
     * 
     * @param  baseNode  node in the base document
     * @return   corresponding node in this document
     */
    protected DelegatingNode getDelegator( Node baseNode ) {
        DelegatingNode delegator;
        if ( baseNode == null ) {
            delegator = null;
        }
        else {
            delegator = delegates_.get( baseNode );
            if ( delegator == null ) {
                delegator = createDelegator( baseNode );
                delegates_.put( baseNode, delegator );
            }
        }
        return delegator;
    }

    /**
     * Returns the base document (the one to which this delegates).
     *
     * @return   base document
     */
    protected Document getBaseDocument() {
        return base_;
    }

    /**
     * Creates a delegator node from a base node, by invoking one of the
     * <code>createDelegating*</code> mehtods.
     *
     * @param  baseNode   base node 
     * @return  a new node in this model which delegates
     *          to <code>baseNode</code>
     */
    private DelegatingNode createDelegator( Node baseNode ) {
        if ( baseNode == null ) {
            throw new NullPointerException();
        }
        else if ( baseNode instanceof CDATASection ) {
            return createDelegatingCDATASection( (CDATASection) baseNode );
        }
        else if ( baseNode instanceof Comment ) {
            return createDelegatingComment( (Comment) baseNode );
        }
        else if ( baseNode instanceof Text ) {
            return createDelegatingText( (Text) baseNode );
        }
        else if ( baseNode instanceof CharacterData ) {
            return createDelegatingCharacterData( (CharacterData) baseNode );
        }
        else if ( baseNode instanceof Attr ) {
            return createDelegatingAttr( (Attr) baseNode );
        }
        else if ( baseNode instanceof DocumentFragment ) {
            return createDelegatingDocumentFragment( (DocumentFragment)
                                                   baseNode );
        }
        else if ( baseNode instanceof DocumentType ) {
            return createDelegatingDocumentType( (DocumentType) baseNode );
        }
        else if ( baseNode instanceof Element ) {
            return createDelegatingElement( (Element) baseNode );
        }
        else if ( baseNode instanceof Entity ) {
            return createDelegatingEntity( (Entity) baseNode );
        }
        else if ( baseNode instanceof EntityReference ) {
            return createDelegatingEntityReference( (EntityReference) 
                                                    baseNode );
        }
        else if ( baseNode instanceof Notation ) {
            return createDelegatingNotation( (Notation) baseNode );
        }
        else if ( baseNode instanceof ProcessingInstruction ) {
            return createDelegatingProcessingInstruction( 
                       (ProcessingInstruction) baseNode );
        }
        else {
            return createDelegatingSimpleNode( baseNode );
        }
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.Node} (not one of its subclasses) 
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingNode createDelegatingSimpleNode( Node baseNode ) {
        return baseNode == null ? null 
                                : new DelegatingNode( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.Attr}
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingAttr createDelegatingAttr( Attr baseNode ) {
        return baseNode == null ? null
                                : new DelegatingAttr( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.Attr} and knows whether it is an
     * ID-type attribute or not.  This is not called by any method of
     * this class, but can be used by subclass implementations of
     * {@link #createDelegatingAttr(org.w3c.dom.Attr)}.
     * 
     * @param   baseNode  delegate node
     * @param   isId  true if this node knows it is an ID, false if it knows
     *          it isn't
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingAttr createDelegatingAttr( Attr baseNode,
                                                   boolean isId ) {
        return baseNode == null ? null
                                : new DelegatingAttr( baseNode, this, isId );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.CDATASection}
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingCDATASection 
              createDelegatingCDATASection( CDATASection baseNode ) {
        return baseNode == null ? null
                                : new DelegatingCDATASection( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.CharacterData} (not one of its subclasses)
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingCharacterData 
              createDelegatingCharacterData( CharacterData baseNode ) {
        return baseNode == null ? null
                                : new DelegatingCharacterData( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.Comment}
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingComment createDelegatingComment( Comment baseNode ) {
        return baseNode == null ? null
                                : new DelegatingComment( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.DocumentFragment}
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingDocumentFragment 
              createDelegatingDocumentFragment( DocumentFragment baseNode ) {
        return baseNode == null
             ? null
             : new DelegatingDocumentFragment( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.DocumentType}
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingDocumentType
              createDelegatingDocumentType( DocumentType baseNode ) {
        return baseNode == null
             ? null
             : new DelegatingDocumentType( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.Element}
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingElement createDelegatingElement( Element baseNode ) {
        return baseNode == null ? null
                                : new DelegatingElement( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.Entity}
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingEntity createDelegatingEntity( Entity baseNode ) {
        return baseNode == null ? null
                                : new DelegatingEntity( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.EntityReference}
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingEntityReference 
              createDelegatingEntityReference( EntityReference baseNode ) {
        return baseNode == null
             ? null
             : new DelegatingEntityReference( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.Notation}
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingNotation createDelegatingNotation( Notation baseNode ) {
        return baseNode == null ? null
                                : new DelegatingNotation( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.ProcessingInstruction}
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingProcessingInstruction
             createDelegatingProcessingInstruction( 
                 ProcessingInstruction baseNode ) {
        return baseNode == null 
             ? null
             : new DelegatingProcessingInstruction( baseNode, this );
    }

    /**
     * Creates a new node in this document that delegates to an object
     * of class {@link org.w3c.dom.Text} (not one of its subclasses)
     * in the base model.  This may be overridden to create specialised
     * node types.
     * 
     * @param   baseNode  delegate node
     * @return  new node in this model based on <code>baseNode</code>
     */
    protected DelegatingText createDelegatingText( Text baseNode ) {
        return baseNode == null ? null
                                : new DelegatingText( baseNode, this );
    }

    /**
     * Returns a NodeList which will dispense nodes in this document.
     *
     * @param  baseList  node list which dispenses nodes in the base document
     * @return  equivalent of <code>baseList</code> in this document
     */
    protected NodeList createDelegatingNodeList( NodeList baseList ) {
        return baseList == null ? null
                                : new DelegatingNodeList( baseList, this );
    }

    /**
     * Returns a NamedNodeMap which will dispense nodes in this document.
     *
     * @param  baseMap  node map which dispenses nodes in the base document
     * @return  equivalent of <code>baseMap</code> in this document
     */
    protected NamedNodeMap 
            createDelegatingNamedNodeMap( NamedNodeMap baseMap ) {
        return baseMap == null ? null
                               : new DelegatingNamedNodeMap( baseMap, this );
    }

    /**
     * Constructs a new empty document from JAXP's default DOM implementation.
     *
     * @return  new Document
     */
    private static Document makeEmptyDocument() {
        try {
            return DocumentBuilderFactory
                  .newInstance()
                  .newDocumentBuilder()
                  .newDocument();
        }
        catch ( ParserConfigurationException e ) {
            throw (RuntimeException) 
                  new IllegalStateException( "Can't create a new Document" )
                 .initCause( e );
        }
    }

    //
    // Level 2 implementation.
    //

    /**
     * Returns the implementation of the base document.
     */
    public DOMImplementation getImplementation() {
        return base_.getImplementation();
    }

    public DocumentType getDoctype() {
        return (DocumentType) getDelegator( base_.getDoctype() );
    }

    public Element getDocumentElement() {
        return (Element) getDelegator( base_.getDocumentElement() );
    }

    public Element createElement( String tagName ) {
        return (Element) getDelegator( base_.createElement( tagName ) );
    }

    public DocumentFragment createDocumentFragment() {
        return (DocumentFragment) 
               getDelegator( base_.createDocumentFragment() );
    }

    public Text createTextNode( String data ) {
        return (Text) getDelegator( base_.createTextNode( data ) );
    }

    public Comment createComment( String data ) {
        return (Comment) getDelegator( base_.createComment( data ) );
    }

    public CDATASection createCDATASection( String data ) {
        return (CDATASection) getDelegator( base_.createCDATASection( data ) );
    }

    public ProcessingInstruction createProcessingInstruction( String target,
                                                              String data ) {
        return (ProcessingInstruction) 
              getDelegator( base_.createProcessingInstruction( target, data ) );
    }

    public Attr createAttribute( String name ) {
        return (Attr) getDelegator( base_.createAttribute( name ) );
    }

    public EntityReference createEntityReference( String name ) {
        return (EntityReference) 
               getDelegator( base_.createEntityReference( name ) );
    }

    public NodeList getElementsByTagName( String tagname ) {
        return createDelegatingNodeList( base_
                                        .getElementsByTagName( tagname ) );
    }

    public Node importNode( Node importedNode, boolean deep ) {
        return getDelegator( base_.importNode( importedNode, deep ) );
    }

    public Element createElementNS( String namespaceURI, 
                                    String qualifiedName ) {
        return (Element) getDelegator( base_.createElementNS( namespaceURI,
                                                              qualifiedName ) );
    }

    public Attr createAttributeNS( String namespaceURI, String qualifiedName ) {
        return (Attr) getDelegator( base_.createAttributeNS( namespaceURI,
                                                             qualifiedName ) );
    }

    public NodeList getElementsByTagNameNS( String namespaceURI,
                                            String localName ) {
        return base_.getElementsByTagNameNS( namespaceURI, localName );
    }

    public Element getElementById( String elementId ) {
        return (Element) getDelegator( base_.getElementById( elementId ) );
    }

    //
    // Level 3 implementation.
    //

    public String getInputEncoding() {
        return base_.getInputEncoding();
    }

    public String getXmlEncoding() {
        return base_.getXmlEncoding();
    }

    public boolean getXmlStandalone() {
        return base_.getXmlStandalone();
    }

    public void setXmlStandalone( boolean xmlStandalone ) {
        base_.setXmlStandalone( xmlStandalone );
    }

    public String getXmlVersion() {
        return base_.getXmlVersion();
    }

    public void setXmlVersion( String xmlVersion ) {
        base_.setXmlVersion( xmlVersion );
    }

    public boolean getStrictErrorChecking() {
        return base_.getStrictErrorChecking();
    }

    public void setStrictErrorChecking( boolean strictErrorChecking ) {
        base_.setStrictErrorChecking( strictErrorChecking );
    }

    public String getDocumentURI() {
        return documentURI_ == null ? base_.getDocumentURI()
                                    : documentURI_;
    }

    public void setDocumentURI( String documentURI ) {
        documentURI_ = null;
        base_.setDocumentURI( documentURI );
    }

    public Node adoptNode( Node source ) {

        /* We are permitted to refuse to do this, so do that.
         * Doing it properly would raise some minor problems. */
        return null;
    }

    public DOMConfiguration getDomConfig() {
        return base_.getDomConfig();
    }

    public void normalizeDocument() {
        base_.normalizeDocument();
    }

    public Node renameNode( Node node, String namespaceURI, 
                            String qualifiedName ) {
        return getDelegator(
            base_.renameNode( DelegatingNode.getBaseNode( node, this ),
                              namespaceURI, qualifiedName ) );
    }
}
