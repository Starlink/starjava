package uk.ac.starlink.parquet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.parquet.io.DelegatingSeekableInputStream;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

/**
 * Parquet InputFile implementation based on a java.io.File.
 *
 * <p>Potentially useful as a replacement for HadoopInputFile,
 * but we still can't get rid of classes from hadoop-common,
 * since Path is required for ParquetWriter implementations.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2021
 */
public class FileInputFile implements InputFile {

    private final File file_;

    /**
     * Constructor.
     *
     * @param  file   file
     */
    public FileInputFile( File file ) {
        file_ = file;
    }

    public long getLength() {
        return file_.length();
    }

    public SeekableInputStream newStream() throws IOException {
        return new FileSeekableInputStream( new FileInputStream( file_ ) );
    }

    /**
     * SeekableInputStream implementation for use with FileInputFile.
     */
    private static class FileSeekableInputStream
            extends DelegatingSeekableInputStream {

        private final FileInputStream in_;
        private long pos_;

        /**
         * Constructor.
         *
         * @param  in  underlying stream
         */
        public FileSeekableInputStream( FileInputStream in ) {
            super( in );
            in_ = in;
        }

        public long getPos() {
            return pos_;
        }

        public void seek( long newPos ) throws IOException {
            in_.skip( newPos - pos_ );
            pos_ = newPos;
        }

        @Override
        public int read() throws IOException {
            int b = in_.read();
            if ( b >= 0 ) {
                pos_++;
            }
            return b;
        }

        @Override
        public int read( byte[] b, int off, int len ) throws IOException {
            int nb = in_.read( b, off, len );
            if ( nb > 0 ) {
                pos_ += nb;
            }
            return nb;
        }

        @Override
        public int read( byte[] b ) throws IOException {
            int nb = in_.read( b );
            if ( nb > 0 ) {
                pos_ += nb;
            }
            return nb;
        }

        @Override
        public long skip( long nreq ) throws IOException {
            long nb = in_.skip( nreq );
            pos_ += nb;
            return nb;
        }

        @Override
        public void readFully( byte[] bytes ) throws IOException {
            super.readFully( bytes );
            pos_ += bytes.length;
        }

        @Override
        public void readFully( byte[] bytes, int start, int len )
                throws IOException {
            super.readFully( bytes, start, len );
            pos_ += len;
        }

        @Override
        public void readFully( ByteBuffer bbuf ) throws IOException {
            int nb = bbuf.remaining();
            super.readFully( bbuf );
            pos_ += nb;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public void reset() throws IOException {
            throw new IOException( "Mark not supported" );
        }
    }
}
