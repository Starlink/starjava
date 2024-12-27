package uk.ac.starlink.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.function.UnaryOperator;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class Base64Test {

    private final Random rnd_;
    private final byte[] lineEnd_ = new byte[] { (byte) '\n' };
    private final UnaryOperator<OutputStream> legacy_;
    private final UnaryOperator<OutputStream> j2se_;
    private final UnaryOperator<OutputStream> testing_;

    public Base64Test() {
        rnd_ = new Random( 893325 );
        legacy_ = out -> new LegacyBase64OutputStream( out, 16 );
        Base64.Encoder j2seEnc =
            Base64.getMimeEncoder( 64, new byte[] { (byte) '\n' } );
        j2se_ = out -> j2seEnc.wrap( out );
        testing_ =
           out -> new BufferedBase64OutputStream( out, 16, lineEnd_, 128 );
    }

    @Test
    public void test64() throws IOException {
        int bufsiz = ((BufferedBase64OutputStream) testing_.apply( null ))
                    .getOutputBufferSize();
        for ( int i = 0; i < 257; i++ ) {
            exercise64( i, testing_ );
        }
        for ( int i = bufsiz - 100; i < bufsiz + 100; i++ ) {
            exercise64( i, testing_ );
        }
        for ( int i = 3 * bufsiz - 10; i < 3 * bufsiz + 10; i++ ) {
            exercise64( i, testing_ );
        }
    }

    private void exercise64( int nbyte, UnaryOperator<OutputStream> b64wrap )
            throws IOException {
        byte[] inbuf = new byte[ nbyte ];
        rnd_.nextBytes( inbuf );
        byte[] outbuf = toBase64( inbuf, b64wrap );
        assertArrayEquals( inbuf, Base64.getMimeDecoder().decode( outbuf ) );
        byte[] outbufLegacy = toBase64( inbuf, legacy_ );
        byte[] outbufJ2se = toBase64( inbuf, j2se_ );
        assertArrayEquals( goldfarb( outbufJ2se ), outbuf );
        if ( nbyte > 0 ) {
            assertArrayEquals( goldfarb( outbufLegacy ), outbuf );
        }
    }

    private byte[] toBase64( byte[] inbuf, UnaryOperator<OutputStream> b64wrap )
            throws IOException {
        int leng = inbuf.length;
        ByteArrayOutputStream bout = new ByteArrayOutputStream( 2 * leng );
        OutputStream b64out = b64wrap.apply( bout );
        int ipos = 0;
        int nsingle = leng > ipos ? rnd_.nextInt( Math.min( leng - ipos, 10 ) )
                                  : 0;
        for ( int i = 0; i < nsingle; i++ ) {
            b64out.write( inbuf[ ipos++ ] );
        }
        for ( int i = 0; ipos < leng; i++ ) {
            int n = Math.min( leng - ipos, i );
            b64out.write( inbuf, ipos, n );
            ipos += n;
        }
        assertEquals( ipos, leng );
        b64out.close();
        bout.close();
        return bout.toByteArray();
    }

    /**
     * Applies some minor end-of-line whitespace adjustment to byte buffer.
     */
    private static byte[] goldfarb( byte[] buf ) {
        int n = buf.length;
        if ( n == 0 ) {
            return buf;
        }
        int nln = 0;
        while ( nln < n && buf[ n - 1 - nln ] == '\n' ) {
            nln++;
        }
        if ( nln == 1 ) {
            return buf;
        }
        else {
            byte[] buf1 = new byte[ n - nln + 1 ];
            System.arraycopy( buf, 0, buf1, 0, n - nln );
            buf1[ buf1.length - 1 ] = '\n';
            return buf1;
        }
    }
}
