package uk.ac.starlink.datanode.factory;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Element;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.HDXDataNode;
import uk.ac.starlink.datanode.nodes.NdxDataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.datanode.nodes.VOComponentDataNode;
import uk.ac.starlink.datanode.nodes.XMLDataNode;
import uk.ac.starlink.util.SourceReader;

public class XMLDataNodeBuilder extends DataNodeBuilder {

    /** Singleton instance. */
    private static XMLDataNodeBuilder instance = new XMLDataNodeBuilder();

    /**
     * Obtains the singleton instance of this class.
     */
    public static XMLDataNodeBuilder getInstance() {
        return instance;
    }

    /**
     * Private sole constructor.
     */
    private XMLDataNodeBuilder() {
    }

    public boolean suitable( Class objClass ) {
        return Source.class.isAssignableFrom( objClass );
    }

    public DataNode buildNode( Object obj ) throws NoSuchDataException {

        /* It should be a Source. */
        Source xsrc = (Source) obj;

        /* Get the top-level element. */
        Element el;
        try {
            el = new SourceReader().getElement( xsrc );
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( e );
        }
        catch ( IllegalArgumentException e ) {
            throw new NoSuchDataException( e );
        }

        /* Get the tagname of the top-level element. */
        String tagName = el.getTagName();
        String localName = el.getLocalName();
        String elName = localName == null ? tagName : localName;
        String elname = elName.toLowerCase();
        
        /* If this suggests that it is a DataNode that we know about, 
         * pass it to the appropriate constructor. */
        if ( elname.equals( "hdx" ) ) {
            return new HDXDataNode( xsrc );
        }
        else if ( elname.equals( "ndx" ) ) {
            return new NdxDataNode( xsrc );
        }
        else if ( elName.equals( "VOTABLE" ) ) {
            return new VOComponentDataNode( xsrc );
        }

        /* Otherwise, just make plain XML out of it. */
        return new XMLDataNode( xsrc );
    }

    public String toString() {
        return "XMLDataNodeBuilder(javax.xml.transform.Source)";
    }
}
