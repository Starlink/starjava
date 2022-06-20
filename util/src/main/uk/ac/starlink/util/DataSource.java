package uk.ac.starlink.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

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
 * handles decompression of the stream.
 * Compression types are as understood by the associated {@link Compression}
 * class.
 * <p>
 * For efficiency, a buffer of the bytes at the start of the stream
 * called the 'intro buffer'
 * is recorded the first time that the stream is read.  This can then
 * be used for magic number queries cheaply, without having to open
 * a new input stream.  In the case that the whole input stream 
 * is shorter than the intro buffer, the underlying input stream 
 * never has to be read again.
 * <p>
 * Any implementation which implements {@link #getRawInputStream} in such
 * a way as to return different byte sequences on different occasions
 * may lead to unpredictable behaviour from this class.
 *
 * @author   Mark Taylor (Starlink)
 * @see   Compression
 */
public abstract class DataSource {

    private int introLimit;
    private byte[] intro;
    private InputStream strm;
    private Compression compress;
    private String name;
    private String position;

    public static final int DEFAULT_INTRO_LIMIT = 512;
    public static final String MARK_WORKAROUND_PROPERTY = "mark.workaround";
    private static Boolean markWorkaround_;

    /**
     * Constructs a DataSource with a given size of intro buffer.
     *
     * @param  introLimit  the maximum number of bytes in the intro buffer
     */
    public DataSource( int introLimit ) {
        setIntroLimit( introLimit );
    }

    /**
     * Constructs a DataSource with a default size of intro buffer.
     */
    public DataSource() {
        this( DEFAULT_INTRO_LIMIT );
    }

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
    public URL getURL() {
        return null;
    }

    /**
     * Returns the maximum length of the intro buffer.
     *
     * @return  maximum length of the intro buffer
     */
    public int getIntroLimit() {
        return introLimit;
    }

    /**
     * Sets the maximum size of the intro buffer to a new value.
     * Setting the intro limit to a new value will discard any state
     * which this source has, so for reasons of efficiency it's not 
     * a good idea to call this method except immediately after the 
     * source has been constructed and before any reads have taken place.
     *
     * @param   limit  the new maximum length of the intro buffer
     */
    public void setIntroLimit( int limit ) {
        if ( limit != introLimit ) {
            clearState();
        }
        this.introLimit = limit;
    }

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
    public synchronized long getLength() {

        /* If we know the length because we have read off the end, return
         * that value. */
        if ( intro != null && intro.length < introLimit ) {
            return (long) intro.length;
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
     * @param  name a name
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
     * It is a string giving an indication of the part of the stream
     * which is of interest.  Its interpretation is up to the application.
     *
     * @param  position  the new posisition (may be <tt>null</tt>)
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
     * Does the first read of the raw input stream to determine the
     * compression and fill up the intro buffer.  Following a call to this
     * method, the <tt>intro</tt> and <tt>compression</tt> members
     * will be set correctly (and are guaranteed not null).
     */
    private synchronized void initialise() throws IOException {

        /* Get a version of the raw (i.e. possibly compressed) input stream. */
        InputStream rawStrm = getRawInputStream();

        /* If we don't already know the compression type, work it out. */
        if ( compress == null ) {

            /* Ensure we can do mark/reset on this stream. */
            if ( ! rawStrm.markSupported() || getMarkWorkaround() ) {
                rawStrm = new BufferedInputStream( rawStrm );
            }

            /* Read enough bytes to determine compression. */
            int nReq = Compression.MAGIC_SIZE;
            rawStrm.mark( nReq );
            byte[] rawbuf = new byte[ nReq ];
            int nGot = rawStrm.read( rawbuf );
            compress = ( nGot == nReq ) ? Compression.getCompression( rawbuf )
                                        : Compression.NONE;

            /* Reset the stream. */
            rawStrm.reset();
        }

        /* Get a new stream which is the uncompressed version of the raw one. */
        InputStream introStrm = compress.decompress( rawStrm );

        /* Ensure we can do mark/reset on it. */
        if ( ! introStrm.markSupported() || getMarkWorkaround() ) {
            introStrm = new BufferedInputStream( introStrm );
        }

        /* Read bytes into a buffer up to a maximum of introLimit. */
        // Note that a 'Resetting to invalid mark' IOException encountered
        // here may result from an InputStream which doesn't support
        // marks but claims it does (markSupported returns true).
        // Sun's J2SE1.4.0 implementation of GZIPInputStream, amongst
        // others, features this bug.
        introStrm.mark( introLimit );
        byte[] buf = new byte[ introLimit ];
        int leng = 0;
        for ( int b; ( b = introStrm.read() ) >= 0 && leng < introLimit;
              leng++ ) {
            buf[ leng ] = (byte) b;
        }

        /* Set the intro buffer from the result. */
        if ( leng == introLimit ) {
            intro = buf;
        }
        else {
            intro = new byte[ leng ];
            System.arraycopy( buf, 0, intro, 0, leng );
        }

        /* If we have the whole content in the intro buffer, we can 
         * discard the stream since we know all the content. */
        if ( intro.length < introLimit ) {
            introStrm.close();
        }

        /* Otherwise reset it and store it for later use. */
        else {
            try {
                introStrm.reset();
            }
            catch ( IOException e ) {
                String msg = new StringBuffer()
               .append( e.getMessage() )
               .append( "\n" )
               .append( "If you have received a " )
               .append( "\"Resetting to an invalid mark\" error,\n" )
               .append( "you have probably come across a " )
               .append( "bug in some library classes (not STILTS ones).\n" )
               .append( "Try running with -D" )
               .append( MARK_WORKAROUND_PROPERTY )
               .append( "=true." )
               .toString();
                throw (IOException) new IOException( msg ).initCause( e );
            }
            strm = introStrm;
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
            initialise();
        }
        return compress;
    }

    /**
     * Returns the intro buffer, first reading it if this hasn't been
     * done before.  The intro buffer will contain the first few bytes
     * of the decompressed stream.  The number of bytes it contains
     * (the size of the returned byte[] array) will be the smaller of
     * <tt>introLimit</tt> and the length of the underlying uncompressed
     * stream.
     * <p>
     * The returned buffer is the original not a copy - don't change its
     * contents!
     *
     * @return  the first few bytes of the uncompressed stream, up to a 
     *          limit of <tt>introLimit</tt>
     */
    public synchronized byte[] getIntro() throws IOException {
        if ( intro == null ) {
            initialise();
        }
        return intro;
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
     * @return  a data source with the same underlying stream as this,
     *         but a compression mode given by <tt>compress</tt>
     */
    public synchronized DataSource forceCompression( Compression compress ) {

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
        if ( getIntro().length < introLimit ) {
            return new ByteArrayInputStream( intro );
        }

        /* Return either an existing stream, or a decompressed version of
         * a new raw stream got from the implementation. */
        InputStream result = ( strm == null )
                           ? getCompression().decompress( getRawInputStream() )
                           : strm;

        /* Make sure that we don't try to use the stream we've just returned
         * at a later date. */
        strm = null;

        /* Return the result. */
        return result;
    }

    /**
     * Returns an input stream which appears just the same as the
     * one returned by {@link #getInputStream}, but only incurs the
     * expense of obtaining an actual input stream (by calling
     * {@link #getRawInputStream} if more bytes are read than the
     * cached magic number.  This is an efficient way to read if you
     * need an InputStream but may only end up reading the first
     * few bytes of it.
     *
     * @return  an input stream that reads from the beginning of the
     *          underlying data source, decompressing it if appropriate
     */
    public synchronized InputStream getHybridInputStream() throws IOException {

        /* If we have already read up to the end of the stream, just
         * return a stream based on our copy of it. */
        if ( getIntro().length < introLimit ) {
            return new ByteArrayInputStream( intro );
        }    

        /* Otherwise, construct a new stream composed of the magic part
         * and a normal part starting from the position corresponding to
         * the end of the magic part (which will not get used if it's
         * not required. */
        else {
            InputStream introStream = new ByteArrayInputStream( intro );
            InputStream remainderStream = new SkipInputStream( intro.length );
            return new SequenceInputStream( introStream, remainderStream );
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
     * Initialises all knowledge of this object about itself.
     */
    private void clearState() {
        close();
        intro = null;
        strm = null;
        compress = null;
    }

    /**
     * Attempts to make a source given a string identifying its location
     * as a file, URL or system command output.
     * This may be one of the following options:
     * <ul>
     * <li>filename</li>
     * <li>URL</li>
     * <li>a string preceded by "&lt;" or followed by "|",
     *     giving a shell command line (may not work on all platforms)</li>
     * </ul>
     *
     * <p>If a '#' character exists in the string, text after it will be
     * interpreted as a position value.  Otherwise, the position is
     * considered to be <tt>null</tt>.
     *
     * <p><strong>Note:</strong> this method presents a security risk if the
     * <code>loc</code> string is vulnerable to injection.
     * Consider using the variant method
     * {@link #makeDataSource(java.lang.String,boolean)
     *         makeDataSource}(loc,false) in such cases.
     * This method just calls <code>makeDataSource(loc,true)</code>.
     *
     * @param  loc  the location of the data, with optional position
     * @return  a DataSource based on the data at <tt>loc</tt>
     * @throws  IOException  if <tt>loc</tt> does not name
     *          an existing readable file or valid URL
     */
    public static DataSource makeDataSource( String loc )
            throws IOException {
        return makeDataSource( loc, true );
    }

    /**
     * Attempts to make a source given a string identifying its location
     * as a file, URL or optionally a system command output.
     *
     * <p>The supplied <code>loc</code> may be one of the following:
     * <ul>
     * <li>filename</li>
     * <li>URL</li>
     * <li><em>only if</em> <code>allowSystem=true</code>:
     *     a string preceded by "&lt;" or followed by "|",
     *     giving a shell command line (may not work on all platforms)</li>
     * </ul>
     *
     * <p>If a '#' character exists in the string, text after it will be
     * interpreted as a position value.  Otherwise, the position is
     * considered to be <tt>null</tt>.
     *
     * <p><strong>Note:</strong> setting <code>allowSystem=true</code> may
     * introduce a security risk if the <code>loc</code> string is
     * vulnerable to injection.
     *
     * @param  loc  the location of the data, with optional position
     * @param  allowSystem  whether to allow system commands
     *                      using the format above
     * @return  a DataSource based on the data at <tt>loc</tt>
     * @throws  IOException  if <tt>loc</tt> does not name
     *          an existing readable file or valid URL
     */
    public static DataSource makeDataSource( String loc, boolean allowSystem )
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

        /* Try it as a filename. */
        File file = new File( name );
        if ( fileExists( file ) ) {
            return new FileDataSource( file, position );
        }

        /* Try it as a shell command. */
        if ( allowSystem ) {
            String cmdLine = getShellCommandLine( name );
            if ( cmdLine != null ) {
                ProcessBuilder pb = ProcessDataSource
                                   .createCommandLineProcessBuilder( cmdLine );
                DataSource src = new ProcessDataSource( pb );
                src.setPosition( position );
                return src;
            }
        }

        /* Try it as a URL. */
        try {
            return new URLDataSource( new URL( loc ) );
        }
        catch ( MalformedURLException e ) {
        }

        /* No luck. */
        String msg = new StringBuffer()
            .append( "No file" )
            .append( allowSystem ? ", URL or command " : " or URL " )
            .append( '"' )
            .append( name )
            .append( '"' )
            .toString();
        throw new FileNotFoundException( msg );
    }

    /**
     * Makes a source from a URL.  If <tt>url</tt> is a file-protocol URL
     * referencing an existing file then 
     * a <tt>FileDataSource</tt> will be returned, otherwise it will be
     * a <tt>URLDataSource</tt>.  Under certain circumstances, it may
     * be more efficient to use a FileDataSource than a URLDataSource,
     * which is why this method may be worth using.
     *
     * @param  url  location of the data stream
     * @return   data source which returns the data at <tt>url</tt>
     */
    public static DataSource makeDataSource( URL url ) {
        if ( url.getProtocol().equals( "file" ) ) {
            try {
                return new FileDataSource( new File( url.getFile() ),
                                           url.getRef() );
            }
            catch ( IOException e ) {
            }
        }
        return new URLDataSource( url );
    }

    /**
     * Returns an input stream based on the given location string.
     * The content of the stream may be compressed or uncompressed data;
     * the returned stream will be an uncompressed version.
     * The following options are allowed for the location:
     * <ul>
     * <li>filename</li>
     * <li>URL</li>
     * <li>"-" meaning standard input</li>
     * <li><em>only if</em> <code>allowSystem=true</code>:
     *     a string preceded by "&lt;" or followed by "|",
     *     giving a shell command line (may not work on all platforms)</li>
     * </ul>
     *
     * <p><strong>Note:</strong> setting <code>allowSystem=true</code> may
     * introduce a security risk if the <code>loc</code> string is
     * vulnerable to injection.
     *
     * @param  location  URL, filename, "cmdline|"/"&lt;cmdline", or "-"
     * @param  allowSystem  whether to allow system commands
     *                      using the format above
     * @return  uncompressed stream containing the data at <tt>location</tt>
     * @throws  FileNotFoundException  if <tt>location</tt> cannot be
     *          interpreted as a source of bytes
     * @throws  IOException  if there is an error obtaining the stream
     */
    public static InputStream getInputStream( String location,
                                              boolean allowSystem )
            throws IOException {
        return Compression
              .decompressStatic( getRawInputStream( location, allowSystem ) );
    }

    /**
     * Returns a raw input stream named by a given location string.
     * Possible location values are as for {@link #getInputStream}.
     *
     * @param  location  URL, filename, "cmdline|"/"&lt;cmdline", or "-"
     * @param  allowSystem  whether to allow system commands
     *                      using the format above
     * @return  stream containing the raw content at <tt>location</tt>
     * @throws  FileNotFoundException  if <tt>location</tt> cannot be
     *          interpreted as a source of bytes
     * @throws  IOException  if there is an error obtaining the stream
     */
    private static InputStream getRawInputStream( String location,
                                                  boolean allowSystem )
            throws IOException {

        /* Minus sign means standard input. */
        if ( location.equals( "-" ) ) {
            return System.in;
        }

        /* Try it as a filename. */
        File file = new File( location );
        if ( fileExists( file ) ) {
            return new FileInputStream( file );
        }

        /* Try it as a shell command line. */
        if ( allowSystem ) {
            String cmdLine = getShellCommandLine( location );
            if ( cmdLine != null ) {
                ProcessBuilder pb = ProcessDataSource
                                   .createCommandLineProcessBuilder( cmdLine );
                return pb.start().getInputStream();
            }
        }

        /* Try it as a URL. */
        try {
            URL url = new URL( location );
            URLConnection conn = url.openConnection();
            conn = URLUtils.followRedirects( conn, null );
            return conn.getInputStream();
        }
        catch ( MalformedURLException e ) {
        }

        /* No luck. */
        String msg = new StringBuffer()
            .append( "No file" )
            .append( allowSystem ? ", URL or command " : " or URL " )
            .append( '"' )
            .append( location )
            .append( '"' )
            .toString();
        throw new FileNotFoundException( msg );
    }

    /**
     * Tries to interpret a data location string as a reference to a shell
     * command line.
     * Currently either of the forms "<code>&lt;cmdline</code>"
     * or "<code>cmdline|</code>" is recognised.
     *
     * @param  location  user-supplied location string
     * @return  command-line part of string if it looks like a command-line
     *          specification; otherwise null
     */
    private static String getShellCommandLine( String location ) {
        if ( location != null && location.length() > 0 ) {
            if ( location.charAt( 0 ) == '<' ) {
                return location.substring( 1 );
            }
            if ( location.charAt( location.length() - 1 ) == '|' ) {
                return location.substring( 0, location.length() - 1 );
            }
        }
        return null;
    }

    /**
     * Returns true if we are working around potential bugs in InputStream
     * {@link java.io.InputStream#mark}/{@link java.io.InputStream#reset}
     * methods (common, including in J2SE classes).
     * The return value is dependent on the system property named
     * {@link #MARK_WORKAROUND_PROPERTY}.
     *
     * @return   true  iff we are working around mark/reset bugs
     */
    public static boolean getMarkWorkaround() {
        if ( markWorkaround_ == null ) {
            try {
                markWorkaround_ =
                    Boolean.valueOf( System
                                    .getProperty( MARK_WORKAROUND_PROPERTY ) );
            }
            catch ( Throwable e ) {
                markWorkaround_ = Boolean.FALSE;
            }
        }
        return markWorkaround_.booleanValue();
    }

    /**
     * Sets whether we want to work around bugs in InputStream mark/reset
     * methods.
     *
     * @param  workaround   true to employ the workaround
     */
    public static void setMarkWorkaround( boolean workaround ) {
        markWorkaround_ = Boolean.valueOf( workaround );
    }

    /**
     * Determines whether a file exists.
     * Unlike File.exists(), it will not throw a SecurityException,
     * it just returns false instead.
     *
     * @param  file  abstract file
     * @return  true if file is known to exist
     */
    private static boolean fileExists( File file ) {
        try {
            return file.exists();
        }
        catch ( SecurityException e ) {
            return false;
        }
    }

    /**
     * Private class which provides an input stream corresponding to this
     * DataSource, but skipping the first few bytes.  The idea is that
     * it consumes no expensive resources if it is never read.
     */
    private class SkipInputStream extends InputStream {
        private InputStream base;
        private final int nskip;
        SkipInputStream( int nskip ) {
            this.nskip = nskip;
        }
        private InputStream getBase() throws IOException {
            if ( base == null ) {
                base = getInputStream();
                for ( int i = 0; i < nskip; ) {
                    int nb = (int) base.skip( nskip );
                    if ( nb > 0 ) {
                        i += nb;
                    }
                    else if ( base.read() >= 0 ) {
                        i++;
                    }
                    else {
                        throw new EOFException( "Unexpected end of file" );
                    }
                }
            }
            return base;
        }
        public int read() throws IOException {
            return getBase().read();
        }
        public int read( byte[] b ) throws IOException {
            return getBase().read( b );
        }
        public int read( byte[] b, int off, int len ) throws IOException {
            return getBase().read( b, off, len );
        }
        public long skip( long n ) throws IOException {
            return getBase().skip( n );
        }
        public int available() throws IOException {
            return base == null ? 0 : base.available();
        }
        public void close() throws IOException {
            if ( base != null ) {
                base.close();
            }
        }
        public boolean markSupported() {
            return false;
        }
    }

}
