package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.datanode.viewers.TextViewer;
import uk.ac.starlink.util.SourceReader;

/**
 * DataNode implementation which describes a top-level XML document.
 * DOM building is deferred.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DocumentDataNode extends DefaultDataNode {

    private final XMLDocument xdoc_;
    private final String systemId_;
    private final String publicId_;
    private final String topLocalName_;
    private final String topNamespaceURI_;
    private final Attributes topAttributes_;
    private List parseLog_;
    private DOMSource docsrc_;
    private Exception parseError_;
    private boolean doingParse_;
    private final Object parseLock_ = new Object();

    public DocumentDataNode( XMLDocument xdoc ) {
        xdoc_ = xdoc;
        systemId_ = xdoc.getSystemId();
        publicId_ = xdoc.getPublicId();
        topLocalName_ = xdoc.getTopLocalName();
        topNamespaceURI_ = xdoc.getTopNamespaceURI();
        topAttributes_ = xdoc.getTopAttributes();
        setName( xdoc.getName() );
        setIconID( IconFactory.XML_DOCUMENT );
        registerDataObject( DataType.DATA_SOURCE, xdoc.getDataSource() );
    }

    public DocumentDataNode( Source xsrc ) throws NoSuchDataException {
        xdoc_ = null;
        if ( xsrc instanceof DOMSource ) {
            docsrc_ = (DOMSource) xsrc;
        }
        else {
            try {
                docsrc_ = new DOMSource( new SourceReader().getDOM( xsrc ) );
                docsrc_.setSystemId( xsrc.getSystemId() );
            }
            catch ( TransformerException e ) {
                throw new NoSuchDataException( "Parse failed", e );
            }
        }
        systemId_ = docsrc_.getSystemId();
        publicId_ = null;
        Node topNode = docsrc_.getNode();
        Element topEl;
        if ( topNode instanceof Document ) {
            topEl = ((Document) topNode).getDocumentElement();
        }
        else if ( topNode instanceof Element ) {
            topEl = (Element) topNode;
        }
        else {
            throw new NoSuchDataException( "Strange node " + topNode );
        }
        topLocalName_ = topEl.getLocalName();
        topNamespaceURI_ = topEl.getNamespaceURI();
        topAttributes_ = new AttributesImpl();
        for ( Node node = topEl.getFirstChild(); node != null;
              node = node.getNextSibling() ) {
            if ( node instanceof Attr ) {
                Attr att = (Attr) node;
                ((AttributesImpl) topAttributes_)
                    .addAttribute( att.getNamespaceURI(), att.getLocalName(),
                                   att.getName(), "CDATA", att.getValue() );
            }
        }
    }

    public boolean allowsChildren() {
        return true;
    }

    public String getNodeTLA() {
        return "DOC";
    }

    public String getNodeType() {
        return "XML document";
    }

    public String getPathSeparator() {
        return "#";
    }

    protected void checkTopElement( String localName, String namespaceURI ) 
            throws NoSuchDataException {
        checkTopLocalName( localName );
        boolean reqBlank = namespaceURI == null 
                         || namespaceURI.trim().length() == 0;
        boolean gotBlank = topNamespaceURI_ == null
                         || topNamespaceURI_.trim().length() == 0;
        if ( ! ( reqBlank && gotBlank ||
                 namespaceURI.equals( topNamespaceURI_ ) ) ) {
            throw new NoSuchDataException( "Namespace is " + topNamespaceURI_ +
                                           " not " + namespaceURI );
        }
    }

    protected void checkTopLocalName( String localName ) 
            throws NoSuchDataException {
        if ( ! localName.equals( topLocalName_ ) ) {
            throw new NoSuchDataException( "Document type is " + topLocalName_ +
                                           " not " + localName );
        }
    }

    protected Iterator getChildIterator( final Node parentNode ) {
        return new Iterator() {
            private Node next =
                XMLDataNode.firstUsefulSibling( parentNode.getFirstChild() );
            private String systemId = xdoc_.getSystemId();
            public boolean hasNext() {
                return next != null;
            }
            public Object next() {
                if ( next == null ) {
                    throw new NoSuchElementException();
                }
                Node nod = next;
                next = XMLDataNode.firstUsefulSibling( next.getNextSibling() );
                try {
                    Source xsrc = new DOMSource( nod, systemId );
                    return makeChild( xsrc );
                }
                catch ( Exception e ) {
                    return makeErrorChild( e );
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Iterator getChildIterator() {
        try {
            DOMSource dsrc = getDocument();
            Document doc = (Document) dsrc.getNode();
            return getChildIterator( doc );
        }
        catch ( IOException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
        catch ( SAXException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
    }

    public void configureDetail( DetailViewer dv ) {
        if ( systemId_ != null && systemId_.trim().length() > 0 ) {
            dv.addKeyedItem( "System ID", systemId_ );
        }
        if ( publicId_ != null && publicId_.trim().length() > 0 ) {
            dv.addKeyedItem( "Public ID", publicId_ );
        }
        dv.addSeparator();
        if ( topNamespaceURI_ == null || 
             topNamespaceURI_.trim().length() == 0 ) {
            dv.addKeyedItem( "Document Type", topLocalName_ );
        }
        else {
            dv.addKeyedItem( "Document Type", topLocalName_ );
            dv.addKeyedItem( "Root Namespace", topNamespaceURI_ );
        }
        if ( topAttributes_ != null && topAttributes_.getLength() > 0 ) {
            dv.addSubHead( "Top element attributes" );
            int natt = topAttributes_.getLength();
            for ( int i = 0; i < natt; i++ ) {
                String attName = topAttributes_.getQName( i );
                if ( attName == null ) {
                    attName = topAttributes_.getLocalName( i );
                }
                dv.addKeyedItem( attName, topAttributes_.getValue( i ) );  
            }
        }

        /* Add a pane containing the raw input text. */
        dv.addPane( "XML Text", new ComponentMaker() {
            public JComponent getComponent() {
                try {
                    return new TextViewer( getDocument() );
                }
                catch ( IOException e ) {
                    return new TextViewer( e );
                }
                catch ( SAXException e ) {
                    return new TextViewer( e );
                }
            };
        } );

        /* Add a pane with parse results. */
        if ( xdoc_ != null ) {
            dv.addPane( "Parse results", new ComponentMaker() {
                public JComponent getComponent() {
                    final JTextArea parseDoc = new JTextArea();
                    new Thread( "Document parser" ) {
                        public void run() {
                            try {
                                doParse();
                            }
                            catch ( SAXException e ) {
                            }
                            catch ( IOException e ) {
                            }
                            Iterator it = parseLog_.iterator();
                            if ( it.hasNext() ) {
                                while ( it.hasNext() ) {
                                    parseDoc.append( it.next() + "\n" );
                                }
                            }
                            else {
                                parseDoc.append( "Parse successful" );
                            }
                        }
                    }.start();
                    return parseDoc;
                }
            } );
        }
    }

    /**
     * Returns a DOMSource corresponding to the XMLDocument underlying this
     * data node.  Its node will be a Document node.  If the DOM is not
     * already available a parse will be performed to construct it.
     */
    protected DOMSource getDocument() throws IOException, SAXException {
        synchronized ( parseLock_ ) {
            while ( doingParse_ ) {
                try {
                    parseLock_.wait();
                }
                catch ( InterruptedException e ) {
                    throw (IOException) new IOException( "Interrupted?" )
                                       .initCause( e );
                }
            }
            if ( parseError_ != null ) {
                if ( parseError_ instanceof IOException ) {
                    throw (IOException) parseError_;
                }
                else if ( parseError_ instanceof SAXException ) {
                    throw (SAXException) parseError_;
                }
                else {
                    throw new SAXException( parseError_ );
                }
            }
            if ( docsrc_ == null ) {
                assert xdoc_ != null;
                doParse();
            }
            assert docsrc_ != null;
            return docsrc_;
        }
    }

    private void doParse() throws SAXException, IOException {
        synchronized ( parseLock_ ) {
            doingParse_ = true;
            parseLog_ = new ArrayList();
            ErrorHandler ehandler = new ErrorHandler() {
                public void error( SAXParseException e ) {
                    parseLog_.add( e.toString() );
                }
                public void warning( SAXParseException e ) {
                    parseLog_.add( e.toString() );
                }
                public void fatalError( SAXParseException e )
                        throws SAXException {
                    parseError_ = e;
                    parseLog_.add( e.toString() );
                    throw e;
                }
            };
            try {
                docsrc_ = xdoc_.parseToDOM( true, ehandler );
            }
            catch ( IOException e ) {
                parseError_ = e;
                throw e;
            }
            catch ( SAXException e ) {
                parseError_ = e;
                throw e;
            }
            doingParse_ = false;
            parseLock_.notifyAll();
        }
    }
 
}
