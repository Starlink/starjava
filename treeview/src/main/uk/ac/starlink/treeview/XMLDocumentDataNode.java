package uk.ac.starlink.treeview;

import java.io.File;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import uk.ac.starlink.util.SourceReader;

public class XMLDocumentDataNode extends XMLDataNode {

    private Document doc;
    private String desc;

    public XMLDocumentDataNode( File file ) throws NoSuchDataException {
        super( getDocumentNode( makeDomSource( file ) ), 
               file.getName(),
               "DOC",
               "XML document",
               true,
               IconFactory.XML_DOCUMENT,
               "" );
        assert domNode instanceof Document;
        doc = (Document) domNode;
        desc = "<" + doc.getDocumentElement().getTagName() + ">";
    }

    public XMLDocumentDataNode( String loc ) throws NoSuchDataException {
        this( new File( loc ) );
    }

    public XMLDocumentDataNode( Document doc ) {
        super( doc, 
               doc.getDocumentElement().getTagName(),
               "DOC",
               "XML document",
               true,
               IconFactory.XML_DOCUMENT,
               "" );
    }

    public XMLDocumentDataNode( Source xsrc ) throws NoSuchDataException {
        this( getDocumentNode( xsrc ) );
        setSystemId( xsrc.getSystemId() );
    }
        

    public String getDescription() {
        return desc;
    }

    public static DOMSource makeDomSource( File file )
            throws NoSuchDataException {
        SourceReader sr = new SourceReader();
        Source xsrc = new StreamSource( file );
        try {
            Node domnode = sr.getDOM( xsrc );
            DOMSource dsrc = new DOMSource( domnode );
            dsrc.setSystemId( file.toString() );
            return dsrc;
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( "Couldn't get Node from file "
                                         + file, e );
        }
    }

    public static Document getDocumentNode( Source xsrc ) 
            throws NoSuchDataException {
        SourceReader sr = new SourceReader();
        try {
            Node domnode = sr.getDOM( xsrc );
            if ( domnode instanceof Document ) {
                return (Document) domnode;
            }
            else {
                throw new NoSuchDataException( "Not a document node" );
            }
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( e );
        }
    }

    public static boolean isMagic( byte[] magic ) {
        return new String( magic ).startsWith( "<?xml" );
    }
}
