package uk.ac.starlink.treeview;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import javax.xml.transform.dom.DOMSource;
import uk.ac.starlink.util.DataSource;

/**
 * A DataNodebuilder which tries to build a DataNode froma DataSource object.
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

    /** Error handler used for parsing XML. */
    private static ErrorHandler xhandler = new ErrorHandler() {
        public void warning( SAXParseException e ) {
            // ignore it
        }
        public void error( SAXParseException e ) {
            // ignore it
        }
        public void fatalError( SAXParseException e ) throws SAXParseException {
            // bail out
            throw e;
        }
    };

    private static XMLDataNodeBuilder xmlBuilder = 
        XMLDataNodeBuilder.getInstance();

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
        byte[] magic = new byte[ 300 ];
        int minsize;
        try {
            minsize = datsrc.getMagic( magic );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }

        /* Zip stream? */
        if ( ZipArchiveDataNode.isMagic( magic ) ) {
            return configureNode( new ZipStreamDataNode( datsrc ), datsrc );
        }

        /* FITS stream? */
        if ( FITSDataNode.isMagic( magic ) ) {
            return configureNode( new FITSStreamDataNode( datsrc ), datsrc );
        }

        /* Tar stream? */
        if ( TarStreamDataNode.isMagic( magic ) ) {
            return configureNode( new TarStreamDataNode( datsrc ), datsrc );
        }

        /* If it's an XML stream delegate to the XMLbuilder. */
        if ( XMLDataNode.isMagic( magic ) ) {
            DOMSource xsrc = makeDOMSource( datsrc );
            return configureNode( xmlBuilder.buildNode( xsrc ), datsrc );
        }

        /* Don't know what it is. */
        throw new NoSuchDataException( this + ": don't know" );
    }

    public String toString() {
        return "SourceDataNodeBuilder(uk.ac.starlink.util.DataSource)";
    }

    public static DOMSource makeDOMSource( DataSource datsrc ) 
            throws NoSuchDataException {

        /* See whether it is worth the effort. */
        try {
            byte[] magic = new byte[ 100 ];
            datsrc.getMagic( magic );
            if ( ! XMLDataNode.isMagic( magic ) ) {
                throw new NoSuchDataException( "Doesn't look like XML" );
            }
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }

        /* Get a DocumentBuilder. */
        DocumentBuilderFactory dbfact = DocumentBuilderFactory.newInstance();
        dbfact.setValidating( false );
        DocumentBuilder parser;
        try {
            parser = dbfact.newDocumentBuilder();
        }
        catch ( ParserConfigurationException e ) {

            /* Failed for some reason - try it with nothing fancy then. */
            try {
                parser = DocumentBuilderFactory.newInstance()
                          .newDocumentBuilder();
            }
            catch ( ParserConfigurationException e2 ) {
                throw new NoSuchDataException( e2 );  // give up then
            }
        }
        parser.setEntityResolver( TreeviewEntityResolver.getInstance() );
        parser.setErrorHandler( xhandler );

        /* Parse the XML file. */
        Document doc;
        try {
            InputStream strm = datsrc.getInputStream();
            doc = parser.parse( strm );
            strm.close();
        }
        catch ( SAXException e ) {
            throw new NoSuchDataException( "XML parse error on source " +
                                           datsrc, e );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( "I/O trouble during XML parse of " +
                                           " source " + datsrc, e );
        }

        /* Turn it into a DOMSource. */
        DOMSource domsrc = new DOMSource( doc );
        String sysid;
        URL url = datsrc.getURL();
        if ( url != null ) {
            if ( url.getProtocol().equals( "file" ) ) {
                sysid = url.getPath();
            }
            else {
                sysid = url.toString();
            }
        }
        else {
            sysid = datsrc.getName();
        }
        domsrc.setSystemId( sysid );
        return domsrc;
    }

}
