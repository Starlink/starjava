package uk.ac.starlink.treeview;

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
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * DataNode implementation which describes a top-level XML document.
 * DOM building is deferred.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DocumentDataNode extends DefaultDataNode {

    public final XMLDocument xdoc;
    private List parseLog;
    private DOMSource docsrc;
    private Exception parseError;
    private boolean doingParse;
    private final Object parseLock = new Object();

    public DocumentDataNode( XMLDocument xdoc ) {
        this.xdoc = xdoc;
        setName( "<" + xdoc.getTopLocalName() + ">" );
        setIconID( IconFactory.XML_DOCUMENT );
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

    protected Iterator getChildIterator( final Node parentNode ) {
        return new Iterator() {
            private Node next =
                XMLDataNode.firstUsefulSibling( parentNode.getFirstChild() );
            private String systemId = xdoc.getSystemId();
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
        String systemId = xdoc.getSystemId();
        String publicId = xdoc.getPublicId();
        String localName = xdoc.getTopLocalName();
        String namespaceURI = xdoc.getTopNamespaceURI();
        Attributes atts = xdoc.getTopAttributes();
        if ( systemId != null && systemId.trim().length() > 0 ) {
            dv.addKeyedItem( "System ID", systemId );
        }
        if ( publicId != null && publicId.trim().length() > 0 ) {
            dv.addKeyedItem( "Public ID", publicId );
        }
        dv.addSeparator();
        if ( namespaceURI == null || namespaceURI.trim().length() == 0 ) {
            dv.addKeyedItem( "Document Type", localName );
        }
        else {
            dv.addKeyedItem( "Document Type", localName );
            dv.addKeyedItem( "Root Namespace", namespaceURI );
        }
        if ( atts != null && atts.getLength() > 0 ) {
            dv.addSubHead( "Top element attributes" );
            int natt = atts.getLength();
            for ( int i = 0; i < natt; i++ ) {
                dv.addKeyedItem( atts.getQName( i ), atts.getValue( i ) );  
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
                        Iterator it = parseLog.iterator();
                        if ( it.hasNext() ) {
                            while ( it.hasNext() ) {
                                parseDoc.append( it.next().toString() + '\n' );
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

    /**
     * Returns a DOMSource corresponding to the XMLDocument underlying this
     * data node.  Its node will be a Document node.  If the DOM is not
     * already available a parse will be performed to construct it.
     */
    protected DOMSource getDocument() throws IOException, SAXException {
        synchronized ( parseLock ) {
            while ( doingParse ) {
                try {
                    parseLock.wait();
                }
                catch ( InterruptedException e ) {
                    throw (IOException) new IOException( "Interrupted?" )
                                       .initCause( e );
                }
            }
            if ( parseError != null ) {
                if ( parseError instanceof IOException ) {
                    throw (IOException) parseError;
                }
                else if ( parseError instanceof SAXException ) {
                    throw (SAXException) parseError;
                }
                else {
                    throw new SAXException( parseError );
                }
            }
            if ( docsrc == null ) {
                doParse();
            }
            assert docsrc != null;
            return docsrc;
        }
    }

    private void doParse() throws SAXException, IOException {
        synchronized ( parseLock ) {
            doingParse = true;
            parseLog = new ArrayList();
            ErrorHandler ehandler = new ErrorHandler() {
                public void error( SAXParseException e ) {
                    parseLog.add( e.toString() );
                }
                public void warning( SAXParseException e ) {
                    parseLog.add( e.toString() );
                }
                public void fatalError( SAXParseException e )
                        throws SAXException {
                    parseError = e;
                    parseLog.add( e.toString() );
                    throw e;
                }
            };
            try {
                docsrc = xdoc.parseToDOM( true, ehandler );
            }
            catch ( IOException e ) {
                parseError = e;
                throw e;
            }
            catch ( SAXException e ) {
                parseError = e;
                throw e;
            }
            doingParse = false;
            parseLock.notifyAll();
        }
    }
 
}
