package uk.ac.starlink.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A DataSource implementation based on a {@link @java.io.File}.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FileDataSource extends DataSource {

    private File file;

    /**
     * Creates a new FileDataSource from a File object.
     *
     * @param  file  the file
     * @throws  IOException  if <tt>file</tt> does not exist, cannot be read,
     *          or is a directory
     */
    public FileDataSource( File file ) throws IOException {
        if ( ! file.exists() ) {
            throw new FileNotFoundException( "No such file " + file );
        }
        else if ( ! file.canRead() ) {
            throw new IOException( "No read permission on file " + file );
        }
        else if ( file.isDirectory() ) {
            throw new IOException( file + " is a directory" );
        }
        this.file = file;
        setName( file.toString() );
    }

    protected InputStream getRawInputStream() throws IOException {
        return new FileInputStream( file );
    }

    /**
     * Returns the length of this file.
     *
     * return  file length
     */
    protected long getRawLength() {
        return file.length();
    }

    /**
     * Returns the File object on which this <tt>FileDataSource</tt> is based.
     *
     * @return  the file
     */
    public File getFile() {
        return file;
    }
}
