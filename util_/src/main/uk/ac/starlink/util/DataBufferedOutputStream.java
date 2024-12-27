package uk.ac.starlink.util;

import java.io.DataOutput;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;

/**
 * BufferedOutputStream subclass implementing the DataOutput interface.
 * This is considerably faster than simply layering a DataOutputStream
 * on top of a BufferedOutputStream.
 *
 * <p>The implementation was largely copied from
 * <code>nom.tam.util.BufferedDataOutputStream</code>.
 * 
 * @author   Mark Taylor
 * @author   Tom McGlynn
 */
public class DataBufferedOutputStream extends BufferedOutputStream
                                      implements DataOutput {

    /** Default buffer size {@value}, same as java.io.BufferedOutputStream. */
    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    /**
     * Constructs a stream with a default buffer size.
     * The default buffer size is currently 8k.
     *
     * @param  out  base output stream
     */
    public DataBufferedOutputStream( OutputStream out ) {
        this( out, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Constructs a stream with a given buffer size.
     *
     * @param  out  base output stream
     * @param  size  buffer size in bytes, must be &gt;=8
     */
    public DataBufferedOutputStream( OutputStream out, int size ) {
        super( out, size );
        if ( buf.length < 8 ) {
            throw new IllegalArgumentException( "Buffer too small ("
                                              + buf.length + "<8)" );
        }
    }

    public void writeBoolean( boolean b ) throws IOException {
        checkBuf( 1 );
        buf[ count++ ] = b ? (byte) 1 : (byte) 0;
    }

    public void writeByte( int b ) throws IOException {
        checkBuf( 1 );
        buf[ count++ ] = (byte) b;
    }

    public void writeShort( int v ) throws IOException {
        checkBuf( 2 );
        buf[ count++ ] = (byte) ( v >>> 8 );
        buf[ count++ ] = (byte) v;
    }

    public void writeInt( int v ) throws IOException {
        checkBuf( 4 );
        buf[ count++ ] = (byte) ( v >>> 24 );
        buf[ count++ ] = (byte) ( v >>> 16 );
        buf[ count++ ] = (byte) ( v >>>  8 );
        buf[ count++ ] = (byte) v;
    }

    public void writeLong( long v ) throws IOException {
        checkBuf( 8 );
        buf[ count++ ] = (byte) ( v >>> 56 );
        buf[ count++ ] = (byte) ( v >>> 48 );
        buf[ count++ ] = (byte) ( v >>> 40 );
        buf[ count++ ] = (byte) ( v >>> 32 );
        buf[ count++ ] = (byte) ( v >>> 24 );
        buf[ count++ ] = (byte) ( v >>> 16 );
        buf[ count++ ] = (byte) ( v >>>  8 );
        buf[ count++ ] = (byte) v;
    }

    public void writeChar( int v ) throws IOException {
        checkBuf( 2 );
        buf[ count++ ] = (byte) ( v >>> 8 );
        buf[ count++ ] = (byte) v;
    }

    public void writeFloat( float v ) throws IOException {
        writeInt( Float.floatToIntBits( v ) );
    }

    public void writeDouble( double v ) throws IOException {
        writeLong( Double.doubleToLongBits( v ) );
    }

    public void writeBytes( String s ) throws IOException {
        int len = s.length();
        checkBuf( len );
        if ( len < buf.length ) {
            for ( int i = 0; i < len; i++ ) {
                buf[ count++ ] = (byte) s.charAt( i );
            }
        }
        else {
            assert count == 0;
            byte[] cbuf = new byte[ len ];
            for ( int i = 0; i < len; i++ ) {
                cbuf[ i ] = (byte) s.charAt( i );
            }
            out.write( cbuf );
        }
    }

    public void writeChars( String s ) throws IOException {
        int len = s.length();
        int len2 = 2 * len;
        checkBuf( len2 );
        if ( len2 < buf.length ) {
            for ( int i = 0; i < len; i++ ) {
                char c = s.charAt( i );
                buf[ count++ ] = (byte) ( c >>> 8 );
                buf[ count++ ] = (byte) c;
            }
        }
        else {
            assert count == 0;
            byte[] cbuf = new byte[ len2 ];
            for ( int i = 0; i < len; i++ ) {
                char c = s.charAt( i );
                cbuf[ 2 * i + 0 ] = (byte) ( c >>> 8 );
                cbuf[ 2 * i + 1 ] = (byte) c;
            }
            out.write( cbuf );
        }
    }

    public void writeUTF( String s ) throws IOException {
        writeUTF( s, this );
    }

    /**
     * Writes a single character in UTF8 format.
     *
     * @param   c  character to write
     */
    public void writeCharUTF8( char c ) throws IOException {

        // Implementation copied from existing static writeUTF method.
        if ( ( c >= 0x0001 ) && ( c <= 0x007F ) ) {
            checkBuf( 1 );
            buf[ count++ ] = (byte) c;
        }
        else if ( c > 0x07FF ) {
            checkBuf( 3 );
            buf[ count++ ] = (byte) (0xE0 | ((c >> 12) & 0x0F));
            buf[ count++ ] = (byte) (0x80 | ((c >>  6) & 0x3F));
            buf[ count++ ] = (byte) (0x80 | ((c >>  0) & 0x3F));
        }
        else {
            checkBuf( 2 );
            buf[ count++ ] = (byte) (0xC0 | ((c >>  6) & 0x1F));
            buf[ count++ ] = (byte) (0x80 | ((c >>  0) & 0x3F));
        }
    }

    /**
     * Try to ensure there is a given number of bytes in the buffer.
     * If it's nearly full, it will be flushed ready for more data.
     * This call does not guarantee that the requested number of bytes
     * are free in the buffer; if the request is more than the buffer length,
     * the buffer is simply flushed.
     *
     * @param  need  number of bytes required
     */
    protected void checkBuf( int need ) throws IOException {
        if ( count + need > buf.length ) {
            out.write( buf, 0, count );
            count = 0;
        }
    }

    /**
     * This method copied almost verbatim from java.io.DataOutputStream.
     */
    private static int writeUTF(String str, DataOutput out) throws IOException {
        int strlen = str.length();
        int utflen = 0;
        int c, count = 0;

        /* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        if (utflen > 65535)
            throw new UTFDataFormatException(
                "encoded string too long: " + utflen + " bytes");

        byte[] bytearr = new byte[utflen+2];

        bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
        bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);

        int i=0;
        for (i=0; i<strlen; i++) {
           c = str.charAt(i);
           if (!((c >= 0x0001) && (c <= 0x007F))) break;
           bytearr[count++] = (byte) c;
        }

        for (;i < strlen; i++){
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                bytearr[count++] = (byte) c;

            } else if (c > 0x07FF) {
                bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytearr[count++] = (byte) (0x80 | ((c >>  6) & 0x3F));
                bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            } else {
                bytearr[count++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
                bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            }
        }
        out.write(bytearr, 0, utflen+2);
        return utflen + 2;
    }
}
