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
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
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

    private DataNodeBuilder sourceBuilder = SourceDataNodeBuilder.getInstance();

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

    public DataNode buildNode( Object obj ) throws NoSuchDataException {

        /* Should be a file. */
        File file = (File) obj;
        if ( ! file.exists() ) {
            throw new NoSuchDataException( "File " + file + " does not exist" );
        }
        if ( ! file.canRead() ) {
            throw new NoSuchDataException( "File " + file + " not readable" );
        }

        /* See if it's a directory. */
        if ( file.isDirectory() ) {
            return configureNode( new FileDataNode( file ), file );
        }

        DataSource datsrc = null;
        try {

            /* Make a DataSource from the file. */
            datsrc = new FileDataSource( file );
            datsrc.setName( file.getName() );

            /* If there is compression, pass it to the handler for streams. */
            Compression compress = datsrc.getCompression();
            if ( datsrc.getCompression() != Compression.NONE ) {
                return configureNode( sourceBuilder.buildNode( datsrc ), file );
            }

            /* Get the magic number. */
            byte[] magic = new byte[ 300 ];
            int nGot = datsrc.getMagic( magic );

            /* If it's a FITS file, make it an NDX (if it looks like it
             * was created as one) or a FITS node. */
            if ( FITSDataNode.isMagic( magic ) ) {
                ArrayDataInput istrm = null;
                try {
                    InputStream strm1 = datsrc.getInputStream();
                    istrm = new BufferedDataInputStream( strm1 );
                    Header hdr = new Header( istrm );
                    if ( hdr.containsKey( "NDX_XML" ) ) {
                        return configureNode( new NdxDataNode( file ), file );
                    }
                    else {
                        return configureNode( new FITSFileDataNode( file ), 
                                              file );
                    }
                }
                catch ( FitsException e ) {
                    throw new NoSuchDataException( e );
                }
                finally {
                    if ( istrm != null ) {
                        istrm.close();
                    }
                }
            }

            /* If it's an HDS file, make it an NDF (if it is one) 
             * or an HDS node. */
            if ( Driver.hasHDS && HDSDataNode.isMagic( magic ) ) {
                HDSObject hobj = null;
                try {
                    hobj = new HDSReference( file ).getObject( "READ" );
                    try {
                        return configureNode( new NDFDataNode( hobj ), file );
                    }
                    catch ( NoSuchDataException e ) {
                        return configureNode( new HDSDataNode( hobj ), file );
                    }
                }
                catch ( HDSException e ) {
                    throw new NoSuchDataException( e );
                }
            }

            /* Zip/jar file? */
            if ( ZipArchiveDataNode.isMagic( magic ) ) {
                return configureNode( new ZipFileDataNode( file ), file );
            }

            /* Tar file? */
            if ( TarStreamDataNode.isMagic( magic ) ) {
                return configureNode( new TarStreamDataNode( datsrc ), file );
            }

            /* XML file? */
            if ( XMLDataNode.isMagic( magic ) ) {
                DOMSource xsrc = makeDOMSource( file );

                /* NDX? */
                try {
                    return configureNode( new NdxDataNode( xsrc ), file );
                }
                catch ( NoSuchDataException e ) {
                }

                /* VOTable? */
                try {
                    return configureNode( new VOTableDataNode( xsrc ), file );
                }
                catch ( NoSuchDataException e ) {
                }

                /* Normal XML file? */
                return configureNode( new XMLDataNode( xsrc ), file );
            }

            /* We don't know what it is. */
            throw new NoSuchDataException( this + ": don't know" );
        }

        /* IOException means some interesting I/O condition occurred.  
         * This shouldn't happen often - log to the user but return null
         * to allow other builders to have a go. */
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }

        /* Clear up. */
        finally {
            if ( datsrc != null ) {
                datsrc.close();
            }
        }
    }

    public String toString() {
        return "FileDataNodeBuilder(java.io.File)";
    }

    public static DOMSource makeDOMSource( File file )
            throws NoSuchDataException {
        try {
            FileDataSource datsrc = new FileDataSource( file );
            datsrc.setName( file.getName() );
            return SourceDataNodeBuilder.makeDOMSource( datsrc );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
    }
}
