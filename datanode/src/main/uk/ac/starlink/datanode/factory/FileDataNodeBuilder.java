package uk.ac.starlink.datanode.factory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.HDSDataNode;
import uk.ac.starlink.datanode.nodes.FITSFileDataNode;
import uk.ac.starlink.datanode.nodes.FileDataNode;
import uk.ac.starlink.datanode.nodes.NDFDataNode;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.datanode.nodes.TarStreamDataNode;
import uk.ac.starlink.datanode.nodes.TfitsDataNode;
import uk.ac.starlink.datanode.nodes.XMLDocument;
import uk.ac.starlink.datanode.nodes.ZipArchiveDataNode;
import uk.ac.starlink.datanode.nodes.ZipFileDataNode;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSReference;
import uk.ac.starlink.fits.FitsUtil;
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
    private DataNodeBuilder docBuilder = DocumentDataNodeBuilder.getInstance();

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
            return new FileDataNode( file );
        }

        DataSource datsrc = null;
        try {

            /* Make a DataSource from the file. */
            datsrc = new FileDataSource( file );
            datsrc.setName( file.getName() );

            /* If there is compression, pass it to the handler for streams. */
            Compression compress = datsrc.getCompression();
            if ( datsrc.getCompression() != Compression.NONE ) {
                return sourceBuilder.buildNode( datsrc );
            }

            /* Get the magic number. */
            byte[] magic = datsrc.getIntro();

            /* If it's a FITS file, make it an NDX (if it looks like it
             * was created as one) or a FITS node. */
            if ( FitsUtil.isMagic( magic ) ) {
                return NodeUtil.hasTAMFITS()
                     ? TamFitsUtil.getFitsDataNode( file, magic, datsrc )
                     : new TfitsDataNode( datsrc );
            }

            /* If it's an HDS file, make it an NDF (if it is one) 
             * or an HDS node. */
            String fname = file.getName();
            if ( ( fname.endsWith( ".sdf" ) || fname.endsWith( ".SDF" ) ) &&
                 NodeUtil.hasHDS() && 
                 HDSDataNode.isMagic( magic ) ) {
                HDSObject hobj = null;
                try {
                    hobj = new HDSReference( file ).getObject( "READ" );
                    try {
                        return new NDFDataNode( hobj );
                    }
                    catch ( NoSuchDataException e ) {
                        return new HDSDataNode( hobj );
                    }
                }
                catch ( HDSException e ) {
                    throw new NoSuchDataException( e );
                }
            }

            /* Zip/jar file? */
            if ( ZipArchiveDataNode.isMagic( magic ) ) {
                return new ZipFileDataNode( file );
            }

            /* Tar file? */
            if ( TarStreamDataNode.isMagic( magic ) ) {
                return new TarStreamDataNode( datsrc );
            }

            /* If it's an XML file delegate it to the XML builder. */
            if ( XMLDocument.isMagic( magic ) ) {
                XMLDocument xdoc = new XMLDocument( datsrc );
                return docBuilder.buildNode( xdoc );
            }

            /* We don't know what it is. */
            throw new NoSuchDataException( "No recognised magic number" );
        }

        /* IOException means some interesting I/O condition occurred.  */
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

}
