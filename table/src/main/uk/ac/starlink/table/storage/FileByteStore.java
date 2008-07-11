package uk.ac.starlink.table.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import uk.ac.starlink.table.ByteStore;

/**
 * ByteStore implementation which uses a temporary file.
 *
 * @author   Mark Taylor
 * @since    11 Jul 2008
 */
public class FileByteStore implements ByteStore {

    private final File file_;
    private final OutputStream out_;
    private final static int BUFSIZ = 64 * 1024;
    private final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.storage" );

    /**
     * Constructor.
     */
    public FileByteStore() throws IOException {
        file_ = File.createTempFile( "FileByteStore", ".tmp" );
        logger_.info( "Creating new temporary file: " + file_ );
        file_.deleteOnExit();
        out_ = new FileOutputStream( file_ );
    }

    public OutputStream getOutputStream() {
        return out_;
    }

    public void copy( OutputStream out ) throws IOException {
        out_.flush();
        InputStream in = new FileInputStream( file_ );
        byte[] buf = new byte[ BUFSIZ ];
        for ( int n; ( n = in.read( buf ) ) > 0; ) {
            out.write( buf, 0, n );
        }
    }

    public void close() {
        if ( file_.delete() ) {
            logger_.info( "Deleting temporary file: " + file_ );
        }
    }
}
