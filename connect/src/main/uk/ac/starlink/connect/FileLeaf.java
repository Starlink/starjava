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
     * @throws  IllegalArgumentException  if <code>file</code> is a directory
     */
    public FileLeaf( File file ) {
        super( file );
        if ( file.isDirectory() ) {
            throw new IllegalArgumentException( file + " is directory" );
        }
    }

    public OutputStream getOutputStream() throws IOException {

        /* Delete the file first if it already exists.  This can prevent some 
         * problems - for instance in the case of a file which is mapped
         * and is being overwritten, failing to perform this step will
         * mess up the mapped copy.  This has the effect of causing
         * TOPCAT to crash if you attempt to save to a FITS file that
         * you're editing.  Deleting it first makes it OK, at least on
         * Linux.  Effect on other OSs is unpredictable, since file
         * mapping is unavoidably system dependent, but it's likely to
         * be a good bet. */
        if ( file_.exists() ) {
            file_.delete();
        }

        /* And then construct and return the output stream. */
        return new FileOutputStream( file_ );
    }

    public DataSource getDataSource() throws IOException {
        return new FileDataSource( file_ );
    }
}
