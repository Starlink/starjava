package uk.ac.starlink.treeview;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSReference;
import uk.ac.starlink.util.SourceReader;

/**
 * A DataNodeBuilder which tries to build a DataNode from a File object.
 * It examines the file and may invoke a constructor of a DataNode
 * subclass if it knows of one which is likely to be suitable.
 * Rather than trying everything, it will only attempt constructors
 * which it thinks has a good chance of succeeding.  Thus, if it works,
 * it will probably be faster than going through the whole list of
 * constructor-based builders.  It may also make smarter decisions than
 * doing it that way.
 */
public class FileDataNodeBuilder extends DataNodeBuilder {

    /** Singleton instance. */
    private static FileDataNodeBuilder instance = new FileDataNodeBuilder();

    /**
     * Obtains the singleton instance of this class.
     */
    public static FileDataNodeBuilder getInstance() {
        return instance;
    }

    /**
     * Private sole constructor.
     */
    private FileDataNodeBuilder() {
    }

    public boolean suitable( Class objClass ) {
        return File.class.isAssignableFrom( objClass );
    }

    public DataNode buildNode( Object obj ) {
        if ( ! ( obj instanceof File ) ) {
            return null;
        }
        File file = (File) obj;
        if ( ! file.exists() || ! file.canRead() || ! file.isFile() ) {
            return null;
        }

        InputStream strm; 
        try {
            strm = new FileInputStream( file );
            strm = new BufferedInputStream( strm );
        }
        catch ( FileNotFoundException e ) {
            e.printStackTrace(); // shouldn't happen
            return null;
        }

        try {
            strm.mark( 80 );
            byte[] magic = new byte[ 80 ];
            strm.read( magic );
            strm.reset();

            /* If it's a FITS file, make it an NDX (if it looks like it
             * was created as one) or a FITS node. */
            if ( FITSDataNode.isMagic( magic ) ) {
                ArrayDataInput istrm = new BufferedDataInputStream( strm );
                try {
                    Header hdr = new Header( istrm );
                    if ( hdr.containsKey( "NDX_XML" ) ) {
                        try {
                            return new NdxDataNode( file );
                        }
                        catch ( NoSuchDataException e ) {
                        }
                    }
                    else {
                        try {
                            return new FITSDataNode( file );
                        }
                        catch ( NoSuchDataException e ) {
                        }   
                    }
                    return null;
                }
                catch ( FitsException e ) {
                    return null;
                }
            }

            /* If it's an HDS file, make it an NDF (if it is one) 
             * or an HDS node. */
            if ( Driver.hasHDS && HDSDataNode.isMagic( magic ) ) {
                HDSObject hobj = null;
                try {
                    hobj = new HDSReference( file ).getObject( "READ" );
                    try {
                        DataNode dn = new NDFDataNode( hobj );
                        dn.setLabel( file.getName() );
                        return dn;
                    }
                    catch ( NoSuchDataException e ) {
                        DataNode dn = new HDSDataNode( hobj );
                        dn.setLabel( file.getName() );
                        return dn;
                    }
                }
                catch ( HDSException e ) {
                    return null;
                }
            }

            /* Zip/jar file? */
            if ( ZipFileDataNode.isMagic( magic ) ) {
                return new ZipFileDataNode( file );
            }

            /* XML file? */
            if ( XMLDataNode.isMagic( magic ) ) {

                /* NDX? */
                DOMSource xsrc = makeDOMSource( file );
                try {
                    DataNode dn = new NdxDataNode( xsrc );
                    dn.setLabel( file.getName() );
                    return dn;
                }
                catch ( NoSuchDataException e ) {
                }

                /* VOTable? */
                try {
                    DataNode dn = new VOTableDataNode( xsrc );
                    dn.setLabel( file.getName() );
                    return dn;
                }
                catch ( NoSuchDataException e ) {
                }

                /* Normal XML file? */
                try {
                    DataNode dn = new XMLDataNode( xsrc );
                    dn.setLabel( file.getName() );
                    return dn;
                }
                catch ( NoSuchDataException e ) {
                }
            }

            /* We don't know what it is.  Return null. */
            return null;
        }

        /* A NoSuchDataException means we couldn't construct a node which
         * the magic number looked like we could.  Abdicate responsibility. */
        catch ( NoSuchDataException e ) {
            return null;
        }

        /* IOException means some interesting I/O condition occurred.  
         * This shouldn't happen often - log to the user but return null
         * to allow other builders to have a go. */
        catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }

        /* Clear up. */
        finally {
            try {
                strm.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    public String toString() {
        return "special DataNodeBuilder (java.io.File)";
    }

    public static DOMSource makeDOMSource( File file )
            throws NoSuchDataException {

        /* Get a DocumentBuilder. */
        DocumentBuilderFactory dbfact = DocumentBuilderFactory.newInstance();
        dbfact.setValidating( false );
        DocumentBuilder parser;
        try {
            parser = dbfact.newDocumentBuilder();
        }
        catch ( ParserConfigurationException e ) {
            System.err.println( e.getMessage() );

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
            doc = parser.parse( file );
        }
        catch ( SAXException e ) {
            throw new NoSuchDataException( "XML parse error on file " + file,
                                           e );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( "I/O trouble during XML parse of " +
                                           "file " + file, e );
        }

        /* Turn it into a DOMSource. */
        DOMSource dsrc = new DOMSource( doc );
        dsrc.setSystemId( file.toString() );
        return dsrc;
    }

}
