package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * A DataNode representing a zip archive stored in a file.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ZipFileDataNode extends ZipArchiveDataNode {

    private ZipFile zfile;
    private File file;

    /**
     * Initialises a <code>ZipFileDataNode</code> from a
     * <code>File</code> object.
     *
     * @param  file  a <code>File</code> object representing the file from
     *               which the node is to be created
     */
    public ZipFileDataNode( File file ) throws NoSuchDataException {
        super( file.getName(), getMagic( file ) );
        try {
            zfile = new ZipFile( file );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
        this.file = file;
        setLabel( file.getName() );
    }

    /**
     * Initialises a <code>ZipFileDataNode</code> from a <code>String</code>.
     *
     * @param  fileName  the absolute or relative name of the zip file.
     */
    public ZipFileDataNode( String fileName ) throws NoSuchDataException {
        this( new File( fileName ) );
    }

    public boolean hasParentObject() {
        return file.getAbsoluteFile().getParentFile() != null;
    }

    public Object getParentObject() {
        return file.getAbsoluteFile().getParentFile();
    }

    protected InputStream getEntryInputStream( ZipEntry zent )
            throws IOException {
        return zfile.getInputStream( zent );
    }

    protected List getEntries() throws IOException {
        List entries = new ArrayList();
        for ( Enumeration enEn = zfile.entries(); enEn.hasMoreElements(); ) {
            entries.add( enEn.nextElement() );
        }
        return entries;
    }

    private static byte[] getMagic( File file ) throws NoSuchDataException {
        try {
            return startBytes( file, 8 );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
    }

}
