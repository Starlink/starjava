package uk.ac.starlink.datanode.factory;

import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DocumentDataNode;
import uk.ac.starlink.datanode.nodes.HDXDataNode;
import uk.ac.starlink.datanode.nodes.NdxDataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.datanode.nodes.VOTableDataNode;
import uk.ac.starlink.datanode.nodes.XMLDocument;

public class DocumentDataNodeBuilder extends DataNodeBuilder {

    /** Singleton instance. */
    private static DocumentDataNodeBuilder instance = 
        new DocumentDataNodeBuilder();

    /**
     * Obtains the singleton instance of this class.
     */
    public static DocumentDataNodeBuilder getInstance() {
        return instance;
    }

    /**
     * Private sole constructor.
     */
    private DocumentDataNodeBuilder() {
    }

    public boolean suitable( Class objClass ) {
        return XMLDocument.class.isAssignableFrom( objClass );
    }

    public DataNode buildNode( Object obj ) throws NoSuchDataException {

        /* It should be an XMLDocument. */
        XMLDocument xdoc = (XMLDocument) obj;

        /* Find out what the top-level element is. */
        String elName = xdoc.getTopLocalName();
        String elname = elName.toLowerCase();
        String elURI = xdoc.getTopNamespaceURI();

        /* If this suggests that it is a DataNode that we know about, 
         * pass it to the appropriate constructor. */
        if ( elname.equals( "hdx" ) ) {
            return new HDXDataNode( xdoc );
        }
        else if ( elname.equals( "ndx" ) ) {
            return new NdxDataNode( xdoc );
        }
        else if ( elName.equals( "VOTABLE" ) ) {
            return new VOTableDataNode( xdoc );
        }

        /* Otherwise, just make plain XML out of it. */
        return new DocumentDataNode( xdoc );
    }

    public String toString() {
        return "XMLDataNodeBuilder(javax.xml.transform.XMLDocument)";
    }
}
