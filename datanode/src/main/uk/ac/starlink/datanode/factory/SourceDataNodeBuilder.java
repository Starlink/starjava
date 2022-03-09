package uk.ac.starlink.datanode.factory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import javax.xml.transform.dom.DOMSource;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.FITSDataNode;
import uk.ac.starlink.datanode.nodes.FITSStreamDataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.datanode.nodes.TarStreamDataNode;
import uk.ac.starlink.datanode.nodes.TfitsDataNode;
import uk.ac.starlink.datanode.nodes.XMLDocument;
import uk.ac.starlink.datanode.nodes.ZipArchiveDataNode;
import uk.ac.starlink.datanode.nodes.ZipStreamDataNode;
import uk.ac.starlink.fits.FitsUtil;
import uk.ac.starlink.util.DataSource;

/**
 * A DataNodebuilder which tries to build a DataNode from a DataSource object.
 * It examines the file and may invoke a constructor of a DataNode 
 * subclass if it knows of one which is likely to be suitable.
 * It will only try constructors which might have a chance.
 * <p>
 * Part of its duties involve constructing a DOM from a DataSource which
 * looks like XML and offering it to known XML consumers.
 */
public class SourceDataNodeBuilder extends DataNodeBuilder {

    /** Singleton instance. */
    private static SourceDataNodeBuilder instance = new SourceDataNodeBuilder();

    private static DocumentDataNodeBuilder docBuilder = 
        DocumentDataNodeBuilder.getInstance();

    /**
     * Obtains the singleton instance of this class.
     */
    public static SourceDataNodeBuilder getInstance() {
        return instance;
    }

    /**
     * Private sole constructor.
     */
    private SourceDataNodeBuilder() {
    }

    public boolean suitable( Class objClass ) {
        return DataSource.class.isAssignableFrom( objClass );
    }

    public DataNode buildNode( Object obj ) throws NoSuchDataException {

        /* Should be a DataSource. */
        DataSource datsrc = (DataSource) obj;

        /* Get the magic number. */
        byte[] magic;
        int minsize;
        try {
            magic = datsrc.getIntro();
            minsize = magic.length;
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }

        /* Zip stream? */
        if ( ZipArchiveDataNode.isMagic( magic ) ) {
            return new ZipStreamDataNode( datsrc );
        }

        /* FITS stream? */
        if ( FitsUtil.isMagic( magic ) ) {
            return NodeUtil.hasTAMFITS()
                 ? TamFitsUtil.getFitsStreamDataNode( datsrc )
                 : new TfitsDataNode( datsrc );
        }

        /* Tar stream? */
        if ( TarStreamDataNode.isMagic( magic ) ) {
            return new TarStreamDataNode( datsrc );
        }

        /* If it's an XML stream delegate to the XMLbuilder. */
        if ( XMLDocument.isMagic( magic ) ) {
            XMLDocument xdoc = new XMLDocument( datsrc );
            return docBuilder.buildNode( xdoc );
        }

        /* Don't know what it is. */
        throw new NoSuchDataException( "No recognised magic number" );
    }

    public String toString() {
        return "SourceDataNodeBuilder(uk.ac.starlink.util.DataSource)";
    }

}
