package uk.ac.starlink.treeview;

import java.io.*;
import java.util.*;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

/**
 * A {@link DataNode} representing an XML Document.  
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class XMLDocumentDataNode extends DefaultDataNode {

    public static final int MAX_LINES = 12;

    private File file;
    private String name;
    private Icon icon;
    private Document doc;
    private JComponent fullView;

    /**
     * Initialises a new XMLDocumentDataNode</code> from a <code>File</code>
     * object.
     * 
     * @param  file   a File from which the node is to be created
     */
    public XMLDocumentDataNode( File file ) throws NoSuchDataException {
        this.file = file;
        name = file.getName();
        setLabel( name );

        SAXBuilder sbuild = new SAXBuilder();
        sbuild.setExpandEntities( false );
        try {
           doc = (Document) sbuild.build( file );
        }
        catch ( JDOMException e ) {
           throw new NoSuchDataException( e.getMessage() );
        }
    }

    /**
     * Initialises a new XMLDocumentDataNode from a String representing its
     * filename. 
     *
     * @param  fileName  the name of the file containing the XML
     */
    public XMLDocumentDataNode( String fileName ) throws NoSuchDataException {
        this( new File( fileName ) );
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        final Iterator lit = doc.getContent().iterator();
        return new Iterator() {
            public boolean hasNext() {
                return lit.hasNext();
            }
            public Object next() {
                Object ob = lit.next();
                try {
                    if ( ob instanceof Element ) {
                        return getChildMaker()
                              .makeDataNode( Element.class, (Element) ob );
                    }
                    else if ( ob instanceof ProcessingInstruction ) {
                        return new XMLProcessingInstructionDataNode( 
                                     (ProcessingInstruction) ob );
                    }
                    else if ( ob instanceof Comment ) {
                        return new XMLCommentDataNode( (Comment) ob );
                    }
                    else {
                        throw new 
                            NoSuchDataException( "Unexpected XML node type" );
                    }
                }
                catch ( NoSuchDataException e ) {
                    return new DefaultDataNode( e );
                }
            }
            public void remove() {
                throw new UnsupportedOperationException( "No remove" );
            }
        };
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = iconMaker.getIcon( IconFactory.XML_DOCUMENT );
        }
        return icon;
    }

    public String getNodeTLA() {
        return "XML";
    }

    public String getNodeType() {
        return "XML document";
    }

    public String getDescription() {
        return "<" + doc.getRootElement().getName() + ">";
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {

        /* Construct a display view if it hasn't been done before. */
        if ( fullView == null ) {

            /* Set up the view panel. */
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();

            /* Add info text. */
            dv.addKeyedItem( "Root element", doc.getRootElement().getName() );
            DocType dt = doc.getDocType();
            if ( dt != null ) {
                String pubID = dt.getPublicID();
                String sysID = dt.getSystemID();
                if ( sysID != null ) {
                    dv.addKeyedItem( "DTD system identifier", sysID );
                }
                if ( pubID != null ) {
                    dv.addKeyedItem( "DTD public identifier", pubID );
                }
            }

            /* Add a limited number of lines of the content of the document. */
            dv.addSubHead( "Content" );
            Writer writer = dv.limitedLineAppender( MAX_LINES );
            boolean incomplete = false;
            try {
                try {
                    new XMLOutputter().output( doc, writer );
                }
                catch ( DetailViewer.MaxLinesWrittenException e ) {
                    dv.addText( "   ...." );
                    incomplete = true;
                }
                finally {
                    writer.close();
                }
            }
            catch ( IOException e ) {
                throw new Error( "Unexpected IOException" + e.getMessage() );
            }
            if ( incomplete ) {
                dv.addPane( "Full text", new ComponentMaker() {
                    public JComponent getComponent() 
                            throws IOException, JDOMException, 
                                   TransformerException {
                        org.w3c.dom.Document w3cdoc = 
                            new org.jdom.output.DOMOutputter().output( doc );
                        return new TextViewer( new DOMSource( w3cdoc ) );
                    }
                } );
            }
        }
        return fullView;
    }
}
