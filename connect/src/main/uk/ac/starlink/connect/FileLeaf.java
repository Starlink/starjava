package uk.ac.starlink.connect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * Leaf node representing a non-directory file in a local filesystem.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
class FileLeaf extends FileNode implements Leaf {

    /**
     * Constructs a new leaf node from a normal file object.
     *
     * @param  file  file
     * @throws  IllegalArgumentException  if <tt>file</tt> is a directory
     */
    public FileLeaf( File file ) {
        super( file );
        if ( file.isDirectory() ) {
            throw new IllegalArgumentException( file + " is directory" );
        }
    }

    public OutputStream getOutputStream() throws IOException {
        return new FileOutputStream( file_ );
    }

    public DataSource getDataSource() throws IOException {
        return new FileDataSource( file_ );
    }
}
