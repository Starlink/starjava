package uk.ac.starlink.oldfits;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.lang.reflect.Array;
import java.util.logging.Logger;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;

/*
 * Much of the implementation here is pinched from the source code of
 * nom.tam.util.BufferedFile.  That file contains the following notice:
 *
 *     Copyright: Thomas McGlynn 1997-1999.
 *     This code may be used for any purpose, non-commercial
 *     or commercial so long as this copyright notice is retained
 *     in the source code or included in or referred to in any
 *     derived software.
 *
 */

/**
 * Abstract implementation of an implementation of a
 * <code>nom.tam.util</code>-compatible FITS I/O handler.
 *
 * <p>The ArrayDataInput interface contains a lot of methods declared like
 * <pre>
 *     int read(type[]) throws IOException;
 * </pre>
 * whose behaviour is not documented - when do they throw an exception
 * and what do they return?  The behaviour implemented here follows that
 * of the <code>BufferedFile</code> implementation (which is similarly
 * undocumented).  It is as follows:
 * <ul>
 * <li>The methods read as many items as there are left, up to the
 *     requested maximum or the end of file.
 * <li>The return value is the number of bytes read
 * <li>An EOFException is thrown only if no items could be read
 * </ul>
 * Consult the <code>BufferedFile</code> implementation for more details.
 *
 * @author   Mark Taylor
 * @author   Tom McGlynn
 * @since    5 Jan 2007
 */
public abstract class AbstractArrayDataIO
        implements ArrayDataInput, ArrayDataOutput {

    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.oldfits");

    /**
     * Reads one byte from the current position.
     *
     * @return  next byte
     */
    protected abstract byte get() throws IOException; 

    /**
     * Reads bytes into a buffer from the current position.
     *
     * @param   buf  destination buffer
     * @param   offset  offset of first byte in <code>buf</code> to be written
     * @param   length  maximum number of bytes to be written to
     *                  <code>buf</code>
     */
    protected abstract void get( byte[] buf, int offset, int length )
            throws IOException;

    /**
     * Writes a single byte at the current position.
     *
     * @param   b  output byte
     */
    protected abstract void put( byte b ) throws IOException;

    /**
     * Writes bytes from a buffer to the current position.
     *
     * @param  buf  source buffer
     * @param  offset  offset of first byte in <code>buf</code> to be read
     * @param  length  number of bytes from <code>buf</code> to be read
     */
    protected abstract void put( byte[] buf, int offset, int length )
            throws IOException;

    /**
     * Returns the size of this buffer.  May be -1 if not known/unlimited.
     *
     * @return  length or -1
     */
    public abstract long length();

    /**
     * Returns the number of bytes remaining between the current position
     * and the end of the file.  If there is no end to the file, it
     * is permissible to return <code>Long.MAX_VALUE</code>;
     *
     * @return   number of bytes left in file
     */
    protected abstract long remaining();

    /*
     * Single item read methods.
     */

    public boolean readBoolean() throws IOException {
        return get() == (byte) 1;
    }

    public byte readByte() throws IOException {
        return get();
    }

    public short readShort() throws IOException {
        return (short) ( ( ( get() & 0xff ) << 8 )
                       | ( ( get() & 0xff ) << 0 ) );
    }

    public char readChar() throws IOException {
        return (char) ( ( ( get() & 0xff ) << 8 ) 
                      | ( ( get() & 0xff ) << 0 ) );
    }

    public int readInt() throws IOException {
        return ( ( ( get() & 0xff ) << 24 )
               | ( ( get() & 0xff ) << 16 )
               | ( ( get() & 0xff ) << 8 )
               | ( ( get() & 0xff ) << 0 ) );
    }

    public long readLong() throws IOException {
        return ( ( ( ((long) readInt()) & 0xffffffffL ) << 32 )
               | ( ( ((long) readInt()) & 0xffffffffL ) << 0 ) );
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat( readInt() );
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble( readLong() );
    }

    public int readUnsignedByte() throws IOException {
        return get() & 0xff;
    }

    public int readUnsignedShort() throws IOException {
        return readShort() & 0xffff;
    }

    /*
     * Multiple item read methods.
     */

    public String readUTF() throws IOException {
        return DataInputStream.readUTF( this );
    }

    /**
     * Not implemented - this method is deprecated in any case.
     *
     * @throws  UnsupportedOperationException  always
     * @deprecated  see {@link java.io.DataInputStream#readLine}
     */
    @Deprecated
    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads a specified number of bytes into an array.  Unlike the
     * read methods, this throws an EOFException if insufficient bytes
     * are available.  In this case all the bytes that can be read, will
     * be read before the exception is thrown.
     * 
     * @param   buf   the byte buffer into which to read
     * @param   start   the index in buf at which to start putting bytes
     * @param   len    the number of bytes which must be read
     * @throws  EOFException  if there are fewer than len bytes left
     */
    public void readFully( byte[] buf, int start, int len ) throws IOException {
        int nread = numAvailable( len, 1 );
        get( buf, start, nread );
        if ( nread < len ) {
            throw new EOFException();
        }
    }

    public int read( byte[] buf, int start, int length ) throws IOException {
        int nread = numAvailable( length, 1 );
        if ( nread == 0 ) {
            throw new EOFException();
        } 
        get( buf, start, nread );
        return nread;
    }

    public int read( boolean[] buf, int start, int length ) throws IOException {
        int nread = numAvailable( length, 1 );
        if ( nread == 0 ) {
            throw new EOFException();
        }
        for ( int i = 0; i < nread; i++ ) {
            buf[ i ] = readBoolean();
        }
        return nread;
    }

    public int read( short[] buf, int start, int length ) throws IOException {
        int nread = numAvailable( length, 2 );
        if ( nread == 0 ) {
            throw new EOFException();
        }
        for ( int i = 0; i < nread; i++ ) {
            buf[ start++ ] = readShort();
        }
        return nread;
    }

    public int read( char[] buf, int start, int length ) throws IOException {
        int nread = numAvailable( length, 2 );
        if ( nread == 0 ) {
            throw new EOFException();
        }
        for ( int i = 0; i < nread; i++ ) {
            buf[ start++ ] = readChar();
        }
        return nread;
    }

    public int read( int[] buf, int start, int length ) throws IOException {
        int nread = numAvailable( length, 4 );
        if ( nread == 0 ) {
            throw new EOFException();
        }
        for ( int i = 0; i < nread; i++ ) {
            buf[ start++ ] = readInt();
        }
        return nread;
    }

    public int read( long[] buf, int start, int length ) throws IOException {
        int nread = numAvailable( length, 8 );
        if ( nread == 0 ) {
            throw new EOFException();
        }
        for ( int i = 0; i < nread; i++ ) {
            buf[ start++ ] = readLong();
        }
        return nread;
    }

    public int read( float[] buf, int start, int length ) throws IOException {
        int nread = numAvailable( length, 4 );
        if ( nread == 0 ) {
            throw new EOFException();
        }
        for ( int i = 0; i < nread; i++ ) {
            buf[ start++ ] = readFloat();
        }
        return nread;
    }

    public int read( double[] buf, int start, int length ) throws IOException {
        int nread = numAvailable( length, 8 );
        if ( nread == 0 ) {
            throw new EOFException();
        }
        for ( int i = 0; i < nread; i++ ) {
            buf[ start++ ] = readDouble();
        }
        return nread;
    }

    public void readFully( byte[] buf ) throws IOException {
        readFully( buf, 0, buf.length );
    }

    public int read( byte[] buf ) throws IOException {
        return read( buf, 0, buf.length );
    }

    public int read( boolean[] buf ) throws IOException {
        return read( buf, 0, buf.length );
    }

    public int read( short[] buf ) throws IOException {
        return read( buf, 0, buf.length );
    }

    public int read( char[] buf ) throws IOException {
        return read( buf, 0, buf.length );
    }

    public int read( int[] buf ) throws IOException {
        return read( buf, 0, buf.length );
    }

    public int read( long[] buf ) throws IOException {
        return read( buf, 0, buf.length );
    }

    public int read( float[] buf ) throws IOException {
        return read( buf, 0, buf.length );
    }

    public int read( double[] buf ) throws IOException {
        return read( buf, 0, buf.length );
    }

    public int readArray( Object o ) throws IOException {
        long nread = readLArray( o );
        if ( (int) nread == nread ) {
            return (int) nread;
        }
        else {
            String msg = new StringBuffer()
                .append( "Read too many objects to report (" )
                .append( nread )
                .append( ">" )
                .append( Integer.MAX_VALUE )
                .append( ") - should use readLArray instead" )
                .toString();
            logger_.warning( msg );
            return Integer.MAX_VALUE;
        }
    }

    public long readLArray( Object o ) throws IOException {
        long nread = 0;
        return primitiveArrayRecurse( o, nread );
    }

    private long primitiveArrayRecurse( Object o, long nread )
            throws IOException {
        if ( o == null ) {
            return nread;
        }
     
        Class<?> elclass = o.getClass().getComponentType();
        if ( elclass == null ) {
            throw new IllegalArgumentException(
                "Invalid object: " + o + " is not an array" );
        }
        int nel = Array.getLength( o );

        /* If it's an array of Objects (presumed to be primitive arrays),
         * process the elements recursively. */
        if ( elclass.isAssignableFrom( Object.class ) ) { 
            for ( int i = 0; i < nel; i++ ) {
                primitiveArrayRecurse( ((Object[]) o)[ i ], nread );
            }
        }
            
        /* If it's an array of primitives, read them in. */
        else if ( elclass == byte.class ) {
            nread += read( (byte[]) o, 0, nel );
        }
        else if ( elclass == boolean.class ) {
            nread += read( (boolean[]) o, 0, nel );
        }
        else if ( elclass == short.class ) {
            nread += read( (short[]) o, 0, nel );
        }
        else if ( elclass == char.class ) {
            nread += read( (char[]) o, 0, nel );
        }
        else if ( elclass == int.class ) {
            nread += read( (int[]) o, 0, nel );
        }
        else if ( elclass == long.class ) {
            nread += read( (long[]) o, 0, nel );
        }
        else if ( elclass == float.class ) {
            nread += read( (float[]) o, 0, nel );
        }
        else if ( elclass == double.class ) {
            nread += read( (double[]) o, 0, nel );
        }
        else {
            throw new IllegalArgumentException( "Invalid object: " + o +
                                                " is not a primitive array" );
        }
        return nread;
    }

    /*
     * Single item write methods.
     */

    public void write( int val ) throws IOException {
        writeByte( val );
    }

    public void writeBoolean( boolean val ) throws IOException {
        put( val ? (byte) 1 : (byte) 0 );
    }

    public void writeByte( int val ) throws IOException {
        put( (byte) val );
    }

    public void writeShort( int val ) throws IOException {
        put( (byte) ( val >>> 8 ) );
        put( (byte) ( val >>> 0 ) );
    }

    public void writeChar( int val ) throws IOException {
        put( (byte) ( val >>> 8 ) );
        put( (byte) ( val >>> 0 ) );
    }

    public void writeInt( int val ) throws IOException {
        put( (byte) ( val >>> 24 ) );
        put( (byte) ( val >>> 16 ) );
        put( (byte) ( val >>> 8 ) );
        put( (byte) ( val >>> 0 ) );
    }

    public void writeLong( long val ) throws IOException {
        put( (byte) ( val >>> 56 ) );
        put( (byte) ( val >>> 48 ) );
        put( (byte) ( val >>> 40 ) );
        put( (byte) ( val >>> 32 ) );
        put( (byte) ( val >>> 24 ) );
        put( (byte) ( val >>> 16 ) );
        put( (byte) ( val >>> 8 ) );
        put( (byte) ( val >>> 0 ) );
    }

    public void writeFloat( float val ) throws IOException {
        writeInt( Float.floatToIntBits( val ) );
    }

    public void writeDouble( double val ) throws IOException {
        writeLong( Double.doubleToLongBits( val ) );
    }

    /*
     * Multiple item write methods.
     */

    public void write( byte[] buf, int start, int length ) throws IOException {
        int nwrite = numAvailable( length, 1 );
        put( buf, start, nwrite );
        if ( nwrite < length ) {
            throw writeOverflowException();
        }
    }

    public void write( boolean[] buf, int start, int length )
            throws IOException {
        while ( length-- > 0 ) {
            writeBoolean( buf[ start++ ] );
        }
    }

    public void write( short[] buf, int start, int length ) throws IOException {
        while ( length-- > 0 ) {
            writeShort( (int) buf[ start++ ] );
        }
    }

    public void write( char[] buf, int start, int length ) throws IOException {
        while ( length-- > 0 ) {
            writeChar( (int) buf[ start++ ] );
        }
    }

    public void write( int[] buf, int start, int length ) throws IOException {
        while ( length-- > 0 ) {
            writeInt( buf[ start++ ] );
        }
    }

    public void write( long[] buf, int start, int length ) throws IOException {
        while ( length-- > 0 ) {
            writeLong( buf[ start++ ] );
        }
    }

    public void write( float[] buf, int start, int length ) throws IOException {
        while ( length-- > 0 ) {
            writeFloat( buf[ start++ ] );
        }
    }

    public void write( double[] buf, int start, int length )
            throws IOException {
        while ( length-- > 0 ) {
            writeDouble( buf[ start++ ] );
        }
    }

    public void write( String[] strings, int start, int length )
            throws IOException {
        while ( length-- > 0 ) {
            writeBytes( strings[ start++ ] );
        }
    }

    public void writeBytes( String s ) throws IOException {
        write( s.getBytes(), 0, s.length() );
    }

    public void writeChars( String s ) throws IOException {
        int len = s.length();
        for ( int i = 0; i < len; i++ ) {
            writeChar( s.charAt( i ) );
        }
    }

    public void writeUTF( String str ) throws IOException {
        byte[] utfBytes = str.getBytes( "UTF-8" );
        byte[] buf = new byte[ utfBytes.length + 2 ];
        buf[0] =  (byte) ( ( utfBytes.length >>> 8 ) & 0xff );
        buf[1] =  (byte) ( utfBytes.length & 0xFF );
        System.arraycopy( utfBytes, 0, buf, 2, utfBytes.length );
        write( buf );
    }

    public void write( byte[] buf ) throws IOException {
        write( buf, 0, buf.length );
    }

    public void write( boolean[] buf ) throws IOException {
        write( buf, 0, buf.length );
    }

    public void write( short[] buf ) throws IOException {
        write( buf, 0, buf.length );
    }

    public void write( char[] buf ) throws IOException {
        write( buf, 0, buf.length );
    }

    public void write( int[] buf ) throws IOException {
        write( buf, 0, buf.length );
    }

    public void write( long[] buf ) throws IOException {
        write( buf, 0, buf.length );
    }

    public void write( float[] buf ) throws IOException {
        write( buf, 0, buf.length );
    }

    public void write( double[] buf ) throws IOException {
        write( buf, 0, buf.length );
    }

    public void write( String[] strings ) throws IOException {
        write( strings, 0, strings.length );
    }

    public void writeArray( Object o ) throws IOException {
        if ( o == null ) {
            return;
        }
        if ( o instanceof String ) {
            write( ((String) o).getBytes() );
        }
        Class<?> elclass = o.getClass().getComponentType();
        if ( elclass == null ) {
            throw new IllegalArgumentException(
                "Invalid object: " + o + " is not an array" );
        }
        int nel = Array.getLength( o );

        /* If it's an array of Objects (presumed to be primitive arrays),
         * process the elements recursively. */
        if ( elclass.isAssignableFrom( Object.class ) ) {
            for ( int i = 0; i < nel; i++ ) {
                writeArray( ((Object[]) o)[ i ] );
            }
        }

        /* If it's an array of primitives, write them out. */
        else if ( elclass == byte.class ) {
            write( (byte[]) o, 0, nel );
        }
        else if ( elclass == boolean.class ) {
            write( (boolean[]) o, 0, nel );
        }
        else if ( elclass == short.class ) {
            write( (short[]) o, 0, nel );
        }
        else if ( elclass == char.class ) {
            write( (char[]) o, 0, nel );
        }
        else if ( elclass == int.class ) {
            write( (int[]) o, 0, nel );
        }
        else if ( elclass == long.class ) {
            write( (long[]) o, 0, nel );
        }
        else if ( elclass == float.class ) {
            write( (float[]) o, 0, nel );
        }
        else if ( elclass == double.class ) {
            write( (double[]) o, 0, nel );
        }
        else {
            throw new IllegalArgumentException( "Invalid object: " + o +
                                                " is not a primitive array" );
        }
    }

    /*
     * Private methods.
     */

    private int numAvailable( int required, int size ) {
        int irem = (int) Math.min( Integer.MAX_VALUE, remaining() );
        int nav = Math.min( required, irem / size );
        return nav;
    }

    private IOException writeOverflowException() {
        return new IOException( "Attempted write beyond buffer limit" );
    }

}
