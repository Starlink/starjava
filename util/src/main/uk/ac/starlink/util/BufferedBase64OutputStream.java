package uk.ac.starlink.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * OutputStream that encodes to Base64 with buffering.
 * Considerably faster than unbuffered implementations.
 *
 * <p>Note that the {@link #endBase64} method should be called
 * once at the end of writing to flush the input and ensure that
 * the output is ended correctly.
 * Closing the stream will call this if it has not been called already.
 *
 * @author   Mark Taylor
 * @since    31 Mar 2022
 */
public class BufferedBase64OutputStream extends FilterOutputStream {

    private static final byte[] B64MAP = getBase64Map();
    private static final byte[] DEFAULT_END_LINE = getDefaultEndLine();

    private final OutputStream out_;
    private final int quadsPerLine_;
    private final int linesPerBuf_;
    private final byte[] lineEnd_;
    private final byte[] inBuf_;
    private final byte[] outBuf_;
    private final int inLeng_;
    private final int outLeng_;
    private int ipos_;

    /**
     * Constructor with default characteristics.
     *
     * @param  out  underlying output stream, doesn't need to be buffered
     */
    public BufferedBase64OutputStream( OutputStream out ) {
        this( out, 16, DEFAULT_END_LINE, 128 );
    }

    /**
     * Custom constructor.
     *
     * @param  out  underlying output stream, doesn't need to be buffered
     * @param  quadsPerLine  number of 4-byte groups per output line
     * @param  lineEnd  sequence of bytes to be written after each output line
     * @param  linesPerBuf  number of lines buffered before an actual write
     *                      to the underlying stream is performed
     */
    public BufferedBase64OutputStream( OutputStream out, int quadsPerLine,
                                       byte[] lineEnd, int linesPerBuf ) {
        super( out );
        out_ = out;
        quadsPerLine_ = quadsPerLine;
        lineEnd_ = lineEnd == null ? new byte[ 0 ] : lineEnd.clone();
        linesPerBuf_ = linesPerBuf;
        inLeng_ = 3 * quadsPerLine_ * linesPerBuf_;
        outLeng_ = ( inLeng_ / 3 ) * 4 + linesPerBuf_ * lineEnd_.length;
        inBuf_ = new byte[ inLeng_ ];
        outBuf_ = new byte[ outLeng_ ];
    }

    @Override
    public void write( int b ) throws IOException {
        if ( ipos_ >= inLeng_ ) {
            writeFullBuffer();
        }
        inBuf_[ ipos_++ ] = (byte) b;
    }

    @Override
    public void write( byte[] b, int off, int len ) throws IOException {
        while ( len >= inLeng_ - ipos_ ) {
            int ncopy = inLeng_ - ipos_;
            System.arraycopy( b, off, inBuf_, ipos_, ncopy );
            ipos_ += ncopy;
            off += ncopy;
            len -= ncopy;
            writeFullBuffer();
            assert ipos_ == 0;
        }
        System.arraycopy( b, off, inBuf_, ipos_, len );
        ipos_ += len;
    }

    @Override
    public void write( byte[] b ) throws IOException {
        write( b, 0, b.length );
    }

    /**
     * Flushes any data in the buffer and terminates the Base64 output
     * correctly.  This method must be called, once, after all writes
     * have been completed, otherwise the output will likely not be
     * valid base64.  This method will be called by {@link #close}
     * if it has not been done already.
     * The effect of further writes to this stream following a call
     * to this method is undefined.
     */
    public void endBase64() throws IOException {
        writePartialBuffer();
    }

    /**
     * Returns the size of the output buffer; output will be written to
     * the underlying stream in chunks of this size.
     *
     * @return   output buffer length in bytes
     */
    public int getOutputBufferSize() {
        return outLeng_;
    }

    /**
     * This calls flush on the underlying stream, but does <em>not</em>
     * flush this stream itself.
     * The {@link #endBase64} method must be called to do that.
     */
    @Override
    public void flush() throws IOException {
        super.flush();
    }

    /**
     * Calls {@link #endBase64} if required before closing.
     */
    @Override
    public void close() throws IOException {
        endBase64();
        super.close();
    }

    /**
     * Called when the buffer is exactly full to flush it to the output
     * as Base64 bytes.
     */
    private void writeFullBuffer() throws IOException {
        assert ipos_ == inLeng_;
        int ip = 0;
        int op = 0;
        for ( int il = 0; il < linesPerBuf_; il++ ) {
            for ( int iq = 0; iq < quadsPerLine_; iq++ ) {
                int i0 = inBuf_[ ip++ ];
                int i1 = inBuf_[ ip++ ];
                int i2 = inBuf_[ ip++ ];
                outBuf_[ op++ ] = B64MAP[ (i0>>>2) & 0x3f ];
                outBuf_[ op++ ] = B64MAP[ (i0<<4) & 0x30 | (i1>>>4) & 0xf ];
                outBuf_[ op++ ] = B64MAP[ (i1<<2) & 0x3c | (i2>>>6) & 0x3 ];
                outBuf_[ op++ ] = B64MAP[ i2 & 0x3f ];
            }
            for ( int j = 0; j < lineEnd_.length; j++ ) {
                outBuf_[ op++ ] = lineEnd_[ j ];
            }
        }
        assert op == outBuf_.length;
        out_.write( outBuf_ );
        ipos_ = 0;
    }

    /**
     * Called when Base64 output has finished to flush any unwritten content
     * from the buffer and terminate the Base64 output correctly.
     */
    private void writePartialBuffer() throws IOException {
        int ip = 0;
        int op = 0;
        int npad = new int[] { 0, 2, 1 }[ ipos_ % 3 ];
        for ( int il = 0; il < linesPerBuf_ && ipos_ > ip; il++ ) {
            for ( int iq = 0; iq < quadsPerLine_ && ipos_ > ip; iq++ ) {
                int i0 = ip < ipos_ ? inBuf_[ ip++ ] : 0;
                int i1 = ip < ipos_ ? inBuf_[ ip++ ] : 0;
                int i2 = ip < ipos_ ? inBuf_[ ip++ ] : 0;
                outBuf_[ op++ ] = B64MAP[ (i0>>>2) & 0x3f ];
                outBuf_[ op++ ] = B64MAP[ (i0<<4) & 0x30 | (i1>>>4) & 0xf ];
                outBuf_[ op++ ] = B64MAP[ (i1<<2) & 0x3c | (i2>>>6) & 0x3 ];
                outBuf_[ op++ ] = B64MAP[ i2 & 0x3f ];
            }
            if ( ip >= ipos_ ) {
                for ( int ipad = 0; ipad < npad; ipad++ ) {
                    outBuf_[ op - ipad - 1 ] = (byte) '=';
                }
            }
            for ( int j = 0; j < lineEnd_.length; j++ ) {
                outBuf_[ op++ ] = lineEnd_[ j ];
            }
        }
        out_.write( outBuf_, 0, op );
        ipos_ = 0;
    }

    /**
     * Returns a string giving the mapping of 6-bit byte values to
     * Base64 output bytes.
     *
     * @return  byte map
     */
    private static byte[] getBase64Map() {
        return toAscii(
            "ABCDEFGHIJKLMNOP" +
            "QRSTUVWXYZabcdef" +
            "ghijklmnopqrstuv" +
            "wxyz0123456789+/"
        );
    }

    /**
     * Converts an innocuous string to an byte array of the same length
     * by ignoring bits above 7.
     *
     * @param   txt  input string
     * @return  equivalent byte array
     */
    private static byte[] toAscii( String txt ) {
        return txt.getBytes( StandardCharsets.US_ASCII );
    }

    /**
     * Returns the end-of-line string for this platform.
     *
     * @return  EOL byte sequence
     */
    private static byte[] getDefaultEndLine() {
        try {
            return toAscii( System.getProperty( "line.separator" ) );
        }
        catch ( Throwable e ) {
            return new byte[] { (byte) '\n' };
        }
    }
}
