package uk.ac.starlink.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.InflaterInputStream;

/**
 * Represents a stream-like source of data.
 * Instances of this class can be used to encapsulate the data available
 * from a stream.  The idea is that the stream should return the same
 * sequence of bytes each time.
 * <p>
 * As well as the ability to return a stream, a <tt>DataSource</tt> may
 * also have a <tt>position</tt>, which corresponds to the 'ref' or 'frag'
 * part of a URL (the bit after the #).  This is an indication
 * of a location in the stream; it is a string, and its interpretation
 * is entirely up to the application (though may be specified by
 * the documentation of specific <tt>DataSource</tt> subclasses).
 * <p>
 * As well as providing the facility for several different objects to
 * get their own copy of the underlying input stream, this class also
 * handles decompression of the stream and provides a method for
 * reading the magic number.
 * Compression types are as understood by the associated {@link Compression}
 * class.
 * <p>
 * Any implementation which implements {@link #getRawInputStream} in such
 * a way as to return different byte sequences on different occasions
 * may lead to unpredictable behaviour from this class.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class DataSource {

    private byte[] magic;
    private int magicNbyte;
    private int eofPos;
    private InputStream strm;
    private Compression compress;
    private Boolean isASCII;
    private Boolean isEmpty;
    private Boolean isHTML;
    private String name;
    private String position;

    /* Initialise member variables. */
    { clearState(); }

    /** The number of bytes read by the {@link characterise} method. */
    private static final int TESTED_BYTES = 256;

    /** Maximum line length for stream considered as ASCII. **/
    private static final int MAX_LINE_LENGTH = 240;

    /**
     * Provides a new InputStream for this data source.
     * This method should be implemented by subclasses to provide
     * a new InputStream giving the raw content of the source each time
     * it is called.  The general contract of this method is that each
     * time it is called it will return a stream with the same content.
     *
     * @return  an InputStream containing the data of this source
     */
    abstract protected InputStream getRawInputStream() throws IOException;

    /**
     * Returns a URL which corresponds to this data source, if one exists.
     * An {@link java.net.URL#openConnection} method call on the URL
     * returned by this method should provide a stream with the
     * same content as the {@link #getRawInputStream} method of this
     * data source.  If no such URL exists or is known, then <tt>null</tt>
     * should be returned.  
     * <p>
     * If this source has a non-null position value, it will be appended
     * to the main part of the URL after a '#' character (as the URL's
     * ref part).
     *
     * @return  a URL corresponding to this source, or <tt>null</tt>
     */
    abstract public URL getURL();

    /**
     * Returns the length in bytes of the stream returned by 
     * <tt>getRawInputStream</tt>, if known.  If the length is not known
     * then -1 should be returned.
     * The implementation of this method in <tt>DataSource</tt> returns -1;
     * subclasses should override it if they can determine their length.
     *
     * @return  the length of the raw input stream, or -1
     */
    public long getRawLength() {
        return -1L;
    }

    /**
     * Returns the length of the stream returned by <tt>getInputStream</tt>
     * in bytes, if known.
     * A return value of -1 indicates that the length is unknown.
     * The return value of this method may change from -1 to a positive
     * value during the life of this object if it happens to work out
     * how long it is.
     *
     * @return  the length of the stream in bytes, or -1
     */
    public long getLength() {

        /* If we know the length because we have read off the end, return
         * that value. */
        if ( eofPos < Integer.MAX_VALUE ) {
            return (long) eofPos;
        }

        /* If the raw length is known and there is no compression we can 
         * return that value.  Otherwise, we just have to say we don't know. */
        long rawleng = getRawLength();
        if ( rawleng < 0L ||
             compress == null || compress != Compression.NONE ) {
            return -1L;
        }
        else {
            assert compress == Compression.NONE;
            return rawleng;
        }
    }

    /**
     * Returns a name for this source.
     * This name is mainly intended as a label identifying the source for use
     * in informational messages; it is not in general intended to be used
     * to provide an absolute reference to the source.  Thus, for instance,
     * if the source references a file, its name might be a relative
     * pathname or simple filename, rather than its absolute pathname.
     * To identify the source absolutely, the {@link #getURL} method
     * (or some suitable class-specific method) should be used.
     * If this source has a position, it should probably form part of
     * this name.
     *
     * @return  a name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this source.
     *
     * @param  a name
     * @see    #getName
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Returns the position associated with this source.
     * It is a string giving an indication of the part of the stream 
     * which is of interest.  Its interpretation is up to the application.
     *
     * @return  the position string, or <tt>null</tt>
     */
    public String getPosition() {
        return position;
    }

    /**
     * Sets the position associated with this source.
     * It is a strin giving an indication of the part of the stream
     * which is of interest.  Its interpretation is up to the application.
     *
     * @param  the new posisition (may be <tt>null</tt>)
     */
    public void setPosition( String position ) {
        this.position = position;
    }

    /**
     * Returns a System ID for this DataSource; this is a string 
     * representation of a file name or URL, as used by 
     * {@link javax.xml.transform.Source} and friends.
     * The return value may be <tt>null</tt> if none is known.
     * This does not contain any reference to the position.
     *
     * @return  the System ID string for this source, or <tt>null</tt>
     */
    public String getSystemId() {
        URL url = getURL();
        if ( url == null ) {
            return null;
        }
        else if ( url.getProtocol().equals( "file" ) ) {
            return url.getPath();
        }
        else {
            return url.toString();
        }
    }

    /**
     * Returns an object which will handle any required decompression 
     * for this stream.  A raw data stream is read and its magic number
     * (first few bytes) matched against known patterns to determine
     * if any known compression method is in use.
     * If no known compression is being used, the value 
     * <tt>Compression.NONE</tt> is returned.
     *
     * @return  a Compression object encoding this stream
     */
    public synchronized Compression getCompression() throws IOException {
        if ( compress == null ) {
            assert strm == null;
            InputStream raw = getRawInputStream();
            if ( ! markSupported( raw ) ) {
                raw = new BufferedInputStream( raw );
            }
            int nReq = Compression.MAGIC_SIZE;
            raw.mark( nReq );
            byte[] rawbuf = new byte[ nReq ];
            int nGot = raw.read( rawbuf );
            raw.reset();
            compress = Compression.getCompression( rawbuf );
            this.strm = compress.decompress( raw );
        }
        return compress;
    }

    /**
     * Sets the compression to be associated with this data source.
     * In general it will not be necessary or advisable to call this method,
     * since this object will figure it out using magic numbers of
     * the underlying stream.  It can be used if the compression
     * method is known, or to force use of a particular compression;
     * in particular <tt>setCompression(Compression.NONE)</tt> can
     * be used to force direct examination of the underlying stream
     * without decompression, even if the underlying stream is in fact
     * compressed.  
     * <p>
     * The effects of setting a compression to a mode (other than NONE)
     * which does not match the actual compression mode of the 
     * underlying stream are undefined, so this method should be used 
     * with care.
     *
     * @param  compress  the compression mode encoding the underlying
     *         stream
     */
    public synchronized void setCompression( Compression compress ) {
        if ( this.compress != compress ) {
            clearState();
            this.compress = compress;
        }
    }

    /**
     * Returns a DataSource representing the same underlying stream, 
     * but with a forced compression mode <tt>compress</tt>.
     * The returned <tt>DataSource</tt> object may be the same object 
     * as this one, but 
     * if it has a different compression mode from <tt>compress</tt>
     * a new one will be created.  As with {@link #setCompression},
     * the consequences of using a different value of <tt>compress</tt>
     * than the correct one (other than {@link Compression#NONE}
     * are unpredictable.
     *
     * @param  compress  the compression mode to be used for the returned
     *                   data source
     * @param  a data source with the same underlying stream as this,
     *         but a compression mode given by <tt>compress</tt>
     */
    public DataSource forceCompression( Compression compress ) {

        if ( this.compress == null ) {
            clearState();
            this.compress = compress;
            return this;
        }

        else if ( this.compress.equals( compress ) ) {
            return this;
        }

        else {
            final DataSource base = this;
            DataSource forced = new DataSource() {
                protected InputStream getRawInputStream() throws IOException {
                    return base.getRawInputStream();
                }
                public URL getURL() {
                    return base.getURL();
                }
            };
            forced.setName( base.getName() );
            forced.setCompression( compress );

            /* Return the new DataSource object. */
            return forced;
        }
    }

    /**
     * Returns an InputStream containing the whole of this DataSource.
     * If compression is detected in the underlying stream, it will be
     * decompressed.
     * The returned stream should be closed by the user when no 
     * longer required.
     *
     * @return  an input stream that reads from the beginning of the 
     *          underlying data source, decompressing it if appropriate
     */
    public synchronized InputStream getInputStream() throws IOException {

        /* If we have already read up to the end of the stream, we can
         * return a stream based on our copy of it.  This is likely to
         * be cheaper than the alternatives. */
        if ( eofPos < Integer.MAX_VALUE ) {
            assert magic.length == eofPos;
            return new ByteArrayInputStream( magic );
        }

        /* Find out what compression we are using.  This may have the
         * side effect of getting us a stream. */
        Compression compress = getCompression();

        /* Return either an existing stream, or a decompressed version of
         * a new raw stream got from the implementation. */
        InputStream result = ( strm == null ) 
                             ? compress.decompress( getRawInputStream() ) 
                             : strm;

        /* Make sure that we don't try to use the stream we've just returned
         * at a later date. */
        strm = null;

        /* Return the result. */
        return result;
    }

    /**
     * Writes the first few bytes of this DataSource into a supplied buffer.
     * The supplied buffer will be filled up with data 
     * (so <tt>buffer.length</tt> bytes will be read) if there are
     * enough bytes in the source.  Bytes beyond this number will be
     * left alone.  Any known compression in the underlying data source
     * is taken care of.
     * <p>
     * Bytes read by this method are remembered by this DataSource for
     * future use, so that multiple cheap calls of this method may 
     * be made as long as the length of <tt>buffer</tt> does not increase
     * on successive calls.  Furthermore, if the end of the stream 
     * is reached (because <tt>buffer.length</tt> exceeds the length of
     * the, possibly decompressed, stream) then this object will know
     * all the bytes in this stream and no subsequent calls to 
     * {@link #getRawInputStream} will be made.  Unless 
     * {@link #clearState} is called of course.
     *
     * @param  buffer  a buffer into which the start of this source should
     *                 be written.
     * @return the number of bytes actually written into <tt>buffer</tt>
     *         (may be less than <tt>buffer.length</tt> if this source
     *         contains fewer bytes)
     */
    public synchronized int getMagic( byte[] buffer ) throws IOException {

        /* See how many bytes have been requested. */
        int nReq = Math.min( buffer.length, eofPos );
        int nGot;

        /* Handle the case in which we need to read from the stream. */
        if ( nReq > magic.length ) {

            /* Get a new stream if we don't have one ready. */
            if ( strm == null ) {
                strm = getInputStream();
            }

            /* Make sure we can do mark/reset on it. */
            if ( ! markSupported( strm ) ) {
                strm = new BufferedInputStream( strm );
            }

            /* Mark it, read enough bytes into the supplied buffer, 
             * and reset to the start. */
            // Note that a 'Resetting to invalid mark' IOException encountered
            // here may result from an InputStream which doesn't support
            // marks but claims it does (markSupported returns true).
            // Sun's J2SE1.4.0 implementation of GZIPInputStream, amongst
            // others, features this bug.
            strm.mark( nReq );
            nGot = strm.read( buffer, 0, nReq );
            if ( nGot == -1 ) {
                nGot = 0;
            }
            strm.reset();

            /* Record whether the file is empty or not. */
            if ( nGot == 0 ) {
                isEmpty = Boolean.TRUE;
                isASCII = Boolean.FALSE;
                isHTML = Boolean.FALSE;
            }
            else {
                isEmpty = Boolean.FALSE;
            }

            /* If we fell off the end of the stream, remember where it was. */
            if ( nGot < nReq ) {
                eofPos = nGot;

                /* In this case we can also discard any stream we have,
                 * since we won't need it again - we can just use the
                 * copy in the magic buffer if we need to read it again. */
                close();
            }

            /* Keep a copy of the bytes that we read. */
            magic = new byte[ nGot ];
            System.arraycopy( buffer, 0, magic, 0, nGot );
        }

        /* We have already read these bytes; supply the data from the copy 
         * we kept. */
        else {
            assert nReq <= magic.length;
            nGot = nReq;
            System.arraycopy( magic, 0, buffer, 0, nGot );
        }

        /* Return the number of bytes actually supplied. */
        return nGot;
    }

    /**
     * Indicates whether the content of the source appears to be printable 
     * ASCII text or not.  A best guess is applied by looking at the
     * first few characters and seeing whether they contain any 
     * bytes which look like they are not printable, or if the lines
     * are very long; if neither obtains, it's assumed ASCII.
     * This is somewhat contrary to the internationalised/Unicode spirit 
     * of Java but I don't know how to write a suitably general 
     * method to determine whether data is printable or not.
     *
     * @return   true if the stream looks like it probably contains 
     *           printable ASCII data
     */
    public boolean isASCII() throws IOException {
        if ( isASCII == null ) {
            characterise();
        }
        return isASCII.booleanValue();
    }

    /**
     * Indicates whether there are any bytes in the stream.
     *
     * @return  <tt>true</tt> iff there are any bytes in the source
     */
    public boolean isEmpty() throws IOException {
        if ( isEmpty == null ) {
            characterise();
        }
        return isEmpty.booleanValue();
    }

    /**
     * Indicates whether this source looks like it contains HTML. 
     * This is just done by looking (case-insensitively) for the UTF-8 
     * sequence "<HTML" near the start of the stream.
     *
     * @return  <tt>true</tt> iff the source looks like it contains HTML
     */
    public boolean isHTML() throws IOException {
        if ( isHTML == null ) {
            characterise();
        }
        return isHTML.booleanValue();
    }

    /**
     * Grabs a few bytes from the start of the stream and makes some 
     * guesses about the general nature of the stream on the basis of these.
     */
    private synchronized void characterise() throws IOException {
        byte[] buf = new byte[ TESTED_BYTES ];
        int nGot = getMagic( buf );
        isEmpty = Boolean.valueOf( nGot == 0 );
        if ( isEmpty.booleanValue() ) {
            isASCII = Boolean.FALSE;
            isHTML = Boolean.FALSE;
        }
        else {
            int lleng = 0;
            boolean hasLongLines = false;
            boolean hasUnprintables = false;
            boolean hasHTMLElement = false;
            for ( int i = 0; i < nGot; i++ ) {
                boolean isret = false;
                boolean isctl = false;
                int bval = buf[ i ];
                switch ( bval ) {
                    case '\n':
                    case '\r':
                        lleng = 0;
                        // no break here is intentional
                    case '\t':
                    case '\f':
                    case (byte) 169:  // copyright symbol
                    case (byte) 163:  // pound sign
                        isctl = true;
                        break;
                    case '<':
                        if ( nGot - i > 10 && ! hasHTMLElement ) {
                            int j = i;
                            if ( ( buf[ ++j ] == 'h' || buf[ j ] == 'H' ) &&
                                 ( buf[ ++j ] == 't' || buf[ j ] == 'T' ) &&
                                 ( buf[ ++j ] == 'm' || buf[ j ] == 'M' ) &&
                                 ( buf[ ++j ] == 'l' || buf[ j ] == 'L' ) ) {
                                hasHTMLElement = true;
                            }
                        }
                        break;
                    default:
                        // no action
                }
                lleng++;
                if ( lleng > MAX_LINE_LENGTH ) {
                    hasLongLines = true;
                }
                if ( ( bval > 126 || bval  < 32 ) && ! isctl ) {
                    hasUnprintables = true;
                }
            }
            isASCII = Boolean.valueOf( ( ! hasLongLines ) && 
                                      ( ! hasUnprintables ) );
            isHTML = Boolean.valueOf( ( ! hasUnprintables ) && hasHTMLElement );
        }
    }

    /**
     * Closes any open streams owned and not yet dispatched by this 
     * DataSource.  Should be called if this object is no longer required,
     * or if it may not be required for some while.  Calling this method
     * does not prevent any other method being called on this object
     * in the future.
     * This method throws no checked exceptions; any <tt>IOException</tt>
     * thrown during closing any owned streams are simply discarded.
     */
    public synchronized void close() {
        if ( strm != null ) {
            try {
                strm.close();
            }
            catch ( IOException e ) {
                // no action
            }
            strm = null;
        }
    }

    /**
     * Returns a short description of this source (name plus compression type).
     *
     * @return  description of this DataSource
     */
    public String toString() {
        String result = getName();
        try {
            Compression comp = getCompression();
            if ( comp != Compression.NONE ) {
                result += " (" + comp + ")";
            }
        }
        catch ( IOException e ) {
            result += " (error determining compression)";
        }
        return result;
    }

    /**
     * Intialises all knowledge of this object about itself.
     */
    protected synchronized void clearState() {
        close();
        magic = new byte[ 0 ];
        magicNbyte = -1;
        eofPos = Integer.MAX_VALUE;
        strm = null;
        compress = null;
        isASCII = null;
        isEmpty = null;
        isHTML = null;
    }

    /**
     * Attempts to make a source given a name identifying its location.
     * Currently this must be either a file name or a URL.
     * If an <em>existing</em> file or valid URL exists with the given
     * <tt>name</tt>, a DataSource based on it will be returned.
     * Otherwise an IOException will be thrown.
     * <p>
     * If a '#' character exists in the string, text after it will be
     * interpreted as a position value.  Otherwise, the position is
     * considered to be <tt>null</tt>.
     *
     * @param  loc  the location of the data, with optional position
     * @return  a DataSource based on the data at <tt>name</tt>
     * @throws  IOException  if <tt>name</tt> does not name
     *          an existing readable file or valid URL
     */
    public static DataSource makeDataSource( String loc )
            throws IOException {

        /* Extract any position part. */
        String position;
        String name;
        int hashpos = loc.indexOf( '#' );
        if ( hashpos > 0 ) {
            position = loc.substring( hashpos + 1 );
            name = loc.substring( 0, hashpos );
        }
        else {
            position = null;
            name = loc;
        }

        /* If there is a file by this name, return a source based on that. */
        File file = new File( name );
        if ( file.exists() ) {
            return new FileDataSource( file, position );
        }
 
        /* Otherwise, see if we can make sense of it as a URL. */
        try {
            URL url = new URL( loc );
            return new URLDataSource( url );
        }
        catch ( MalformedURLException e ) {
            throw new FileNotFoundException( "Not extant file or valid URL: "
                                           + name );
        }
    }

    /**
     * Indicates whether a given stream supports the mark/reset functionality
     * of <tt>InputStream</tt>s.  This ought to be just the same as
     * calling {@link java.io.InputStream#markSupported}, but there is
     * bug in Sun's Java 1.4.1 implementation such that InflaterInputStream
     * and its subclasses (including GZIPInputStream) return <tt>true</tt>
     * from <tt>markSupported</tt> but do not correctly support this
     * functionality.  This bug is #4812237, reported by mbt 
     * in February 2003.
     *
     * @param  strm  the stream to test
     * @return  <tt>true</tt> iff the stream supports marks
     */
    public static boolean markSupported( InputStream strm ) {
        return strm.markSupported() 
            && ! ( strm instanceof InflaterInputStream );
    }

}
