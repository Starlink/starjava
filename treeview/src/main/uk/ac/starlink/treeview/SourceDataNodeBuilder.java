package uk.ac.starlink.treeview;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import javax.xml.transform.dom.DOMSource;
import uk.ac.starlink.util.DataSource;

/**
 * A DataNodebuilder which tries to build a DataNode froma DataSource object.
 * It examines the file and may invoke a constructor of a DataNode 
 * subclass if it knows of one which is likely to be suitable.
 * It will only try constructors which might have a chance.
 */
public class SourceDataNodeBuilder extends DataNodeBuilder {

    /** Singleton instance. */
    private static SourceDataNodeBuilder instance = new SourceDataNodeBuilder();

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

    public DataNode buildNode( Object obj ) {
        if ( ! ( obj instanceof DataSource ) ) {
            return null;
        }
        DataSource datsrc = (DataSource) obj;

        /* Get the magic number. */
        byte[] magic = new byte[ 300 ];
        int minsize;
        try {
            minsize = datsrc.getMagic( magic );
        }
        catch ( IOException e ) {
            return null;
        }

        try {

            /* Zip file? */
            if ( ZipArchiveDataNode.isMagic( magic ) ) {
                return new ZipStreamDataNode( datsrc );
            }

            /* FITS file? */
            if ( FITSDataNode.isMagic( magic ) ) {
                return new FITSStreamDataNode( datsrc );
            }

            /* Tar file? */
            if ( TarStreamDataNode.isMagic( magic ) ) {
                return new TarStreamDataNode( datsrc );
            }

            /* XML file? */
            else if ( XMLDataNode.isMagic( magic ) ) {
                String path = DefaultDataNode.getPath( datsrc );
                String label = DefaultDataNode.getName( datsrc );
                DOMSource xsrc = makeDOMSource( datsrc );

                /* NDX? */
                try {
                    DataNode dn = new NdxDataNode( xsrc );
                    dn.setLabel( label );
                    if ( path != null ) {
                        dn.setPath( path );
                    }
                    return dn;
                }
                catch ( NoSuchDataException e ) {
                    if ( verbose ) {
                        e.printStackTrace( verbStream );
                    }
                }

                /* VOTable? */
                try {
                    DataNode dn = new VOTableDataNode( xsrc );
                    dn.setLabel( label );
                    if ( path != null ) {
                        dn.setPath( path );
                    }
                    return dn;
                }
                catch ( NoSuchDataException e ) {
                    if ( verbose ) {
                        e.printStackTrace( verbStream );
                    }
                }

                /* Normal XML file? */
                DataNode dn = new XMLDataNode( xsrc );
                dn.setLabel( label );
                if ( path != null ) {
                    dn.setPath( path );
                }
                return dn;
            }

            /* Don't know what it is.  Return null. */
            return null;
        }

        /* A NoSuchDataException means we couldn't construct a node which
         * the magic number looked like we could.  Abdicate responsibility. */
        catch ( NoSuchDataException e ) {
            if ( verbose ) {
                e.printStackTrace( verbStream );
            }
            return null;
        }
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
        domsrc.setSystemId( datsrc.getName() );
        return domsrc;
    }

}
