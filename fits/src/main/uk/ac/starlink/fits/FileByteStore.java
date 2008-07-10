package uk.ac.starlink.fits;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * ByteStore implementation based on a temporary file.
 *
 * @author   Mark Taylor
 * @since    10 Jul 2008
 */
class FileByteStore implements ByteStore {

    private final File file_;
    private final DataOutputStream out_;
    private static final int BUFSIZ = 64 * 1024;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     */
    public FileByteStore() throws IOException {
        file_ = File.createTempFile( "FileByteStore", ".tmp" ); 
        logger_.info( "Creating new temporary file: " + file_ );
        file_.deleteOnExit();
        out_ = new DataOutputStream(
                   new BufferedOutputStream(
                       new FileOutputStream( file_ ), BUFSIZ ) );
    }

    public DataOutput getStream() {
        return out_;
    }

    public long getPosition() {
        return out_.size();
    }

    public void copy( DataOutput out ) throws IOException {
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
