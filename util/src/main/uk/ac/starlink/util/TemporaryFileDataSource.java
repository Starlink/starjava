package uk.ac.starlink.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * A DataSource which stores its data in a temporary file.  This can be
 * used to represent data which is only available to read once from an
 * intput stream.  The stream is read when this source is constructed,
 * and cached in a temporary file.  The temporary file is removed 
 * when this object is finalized or when the VM is terminated normally.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TemporaryFileDataSource extends FileDataSource {

    private File tempFile;

    /**
     * Constructs a new DataSource by reading the contents of an 
     * input stream.  The name of the source is also supplied; it does
     * not take the name (or URL) of the file, since that does not 
     * represent a persistent object.
     *
     * @param   baseStream  the stream which supplies this source's data
     * @param   name  the name of the source
     */
    public TemporaryFileDataSource( InputStream baseStream, String name )
            throws IOException {
        super( makeTempFile( baseStream ) );
        setName( name );
    }

    /**
     * Returns <tt>null</tt>, since the data is not represented by a
     * persistent object.
     */
    public URL getURL() {
        return null;
    }

    /**
     * Deletes the temporary data file.
     */
    public void finalize() {
        tempFile.delete();
    }

    /**
     * Creates a temporary file and fills it with the contents of a stream.
     *
     * @param   strm  the input stream, which will be read and closed
     */
    private static File makeTempFile( InputStream istrm ) throws IOException {
        File file = File.createTempFile( "StreamDataSource", null );
        try {
            file.deleteOnExit();
            OutputStream ostrm = new FileOutputStream( file );
            byte[] buf = new byte[ 4096 ];
            for ( int n; ( n = istrm.read( buf ) ) >= 0; ) {
                ostrm.write( buf, 0, n );
            } 
            istrm.close();
            ostrm.close();
            return file;
        }
        catch ( IOException e ) {
            file.delete();
            throw e;
        }
    }
}
