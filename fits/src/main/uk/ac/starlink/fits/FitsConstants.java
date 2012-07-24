package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsUtil;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.TruncatedFileException;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.BufferedFile;
import nom.tam.util.Cursor;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.Loader;

/**
 * Utility class providing some constants and static methods related to 
 * FITS file format and starlink classes.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsConstants {

    /** Prefix for NDArray-related FITS header cards. */
    public static final String NDARRAY_PREFIX = "NDA_";

    /** Image of end-of-header card. */
    public static final HeaderCard END_CARD = new HeaderCard( 
        "END                                     " +
        "                                        " );

    /**
     * FITS header card for indicating NDArray origin values.
     * An integer (1, 2, ... ) is appended to this value for the different
     * axes like for NAXIS.
     */
    public static final String NDARRAY_ORIGIN = NDARRAY_PREFIX + "O";

    /** Prefix for NDX-related FITS header cards. */
    public static final String NDX_PREFIX = "NDX_";

    /** FITS header card for location (relative URL) of XML representation. */
    public static final String NDX_XML = NDX_PREFIX + "XML";

    /** Default encoding used for writing WCS into FITS headers. */
    public static final String WCS_ENCODING = "FITS-WCS";

    /** Standard size of a FITS block in bytes. */
    public static final int FITS_BLOCK = 2880;

    private static final String[] extensions = new String[] {
        ".fits", ".fit", ".fts",
        ".FITS", ".FIT", ".FTS",
        ".lilo", ".lihi", ".silo", ".sihi", ".mxlo",
        ".mxhi", ".rilo", ".rihi", ".vdlo", ".vdhi",
    };

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Gets the default permitted list of extensions which identify a 
     * FITS resource in the path part of a URL.
     * 
     * @return unmodifiable list of default FITS extensions - 
     *         ".fits", ".fit" etc
     */
    public static List defaultFitsExtensions() {
        return Collections.unmodifiableList( Arrays.asList( extensions ) );
    }

    static String originCardName( int naxis ) {
        return NDARRAY_ORIGIN + ( naxis + 1 );
    }

    static int typeToBitpix( Type type ) {
        if ( type == Type.BYTE ) {
            return BasicHDU.BITPIX_BYTE;
        }
        else if ( type == Type.SHORT ) {
            return BasicHDU.BITPIX_SHORT;
        }
        else if ( type == Type.INT ) {
            return BasicHDU.BITPIX_INT;
        }
        else if ( type == Type.FLOAT ) {
            return BasicHDU.BITPIX_FLOAT;
        }
        else if ( type == Type.DOUBLE ) {
            return BasicHDU.BITPIX_DOUBLE;
        }
        else {
            throw new AssertionError();
        }
    }

    /**
     * Skips forward over a given number of HDUs in the supplied stream.
     * If it reaches the end of the stream, it throws an IOException
     * with a Cause of a TruncatedFileException.
     *
     * @param  stream  the stream to skip through
     * @param  nskip  the number of HDUs to skip
     * @return  the number of bytes the stream was advanced
     */
    public static long skipHDUs( ArrayDataInput stream, int nskip )
            throws IOException {
        try {
            long advance = 0L;
            while ( nskip-- > 0 ) {
                Header hdr = new Header();
                int headsize = readHeader( hdr, stream );
                advance += headsize;
                long datasize = getDataSize( hdr );
                IOUtils.skipBytes( stream, datasize );
                advance += datasize;
            }
            return advance;
        }
        catch ( TruncatedFileException e ) {
            throw (IOException) new IOException( "FITS file too short: "
                                                 + e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Indicates whether the supplied buffer is the start of a FITS file.
     * Its contents is checked against the FITS 'magic number', which is
     * the ASCII string "<tt>SIMPLE&nbsp&nbsp;=</tt>".
     *
     * @param   buffer  a byte buffer containing
     *          the start of a file to test
     * @return  <tt>true</tt> iff the bytes in <tt>buffer</tt> look like 
     *          the start of a FITS file
     */
    public static boolean isMagic( byte[] buffer ) {
        return buffer.length >= 9 &&
               (char) buffer[ 0 ] == 'S' &&
               (char) buffer[ 1 ] == 'I' &&
               (char) buffer[ 2 ] == 'M' &&
               (char) buffer[ 3 ] == 'P' &&
               (char) buffer[ 4 ] == 'L' &&
               (char) buffer[ 5 ] == 'E' &&
               (char) buffer[ 6 ] == ' ' &&
               (char) buffer[ 7 ] == ' ' &&
               (char) buffer[ 8 ] == '=';
    }

    /**
     * Returns an input stream which can be used with the various FITS
     * classes based on a given DataSource object, positioned at the
     * start of the stream.
     *
     * @param  datsrc  the DataSource pointing to the file/HDU required
     * @return  an ArrayDataInput acquired from <tt>datsrc</tt>,
     */
    public static ArrayDataInput getInputStreamStart( DataSource datsrc )
            throws IOException {
        if ( datsrc instanceof FileDataSource && 
             datsrc.getCompression() == Compression.NONE ) {
            File file = ((FileDataSource) datsrc).getFile();
            try {
                MappedFile mf = new MappedFile( file.toString() );
                logger.config( "Mapping file " + file );
                return mf;
            }
            catch ( MappedFile.FileTooLongException e ) {
                logger.info( file + " too long for monolithic map" );
                if ( Loader.is64Bit() ) {
                    logger.info( file + " - mapping in blocks" );
                    return new MultiMappedFile( file,
                                                FileChannel.MapMode.READ_ONLY,
                                                1024 * 1024 * 256 );
                }
                else {
                    logger.info( "Won't try mapping in blocks on 32-bit JVM" );
                    logger.warning( "Might be faster on a 64-bit OS/JVM" );
                    return new BufferedFile( file.getPath(), "r" );
                }
            }
        }
        mappableWarning( datsrc );
        logger.config( "Buffering stream " + datsrc.getName() );
        return new BufferedDataInputStream( datsrc.getInputStream() );
    }

    /**
     * Advances the position of an input stream according to a position 
     * string.
     * The string may contain a non-negative integer giving the number of
     * HDUs to advance by.
     * If it's "0" or <tt>null</tt> no advance will be made.
     * The <tt>pos</tt> string will typically be the position attribute
     * of a {@link uk.ac.starlink.util.DataSource}.
     *
     * @param  strm  the stream to advance
     * @param  pos   number of HDUs to advance by (as a string)
     * @return  number of bytes the stream has been advanced
     */
    public static long positionStream( ArrayDataInput strm, String pos )
            throws IOException {

        /* If we have a position, try to position the stream accordingly. */
        if ( pos != null ) {

            /* Get a non-negative HDU index. */
            int ihdu;
            try {
                ihdu = Integer.parseInt( pos );
            }
            catch ( NumberFormatException e ) {
                throw new IllegalArgumentException(
                    "Position indicator \"" + pos +
                    "\" is not an integer (should be HDU index) " + e );
            }
            if ( ihdu < 0 ) {
                throw new IllegalArgumentException(
                    "HDU index " + ihdu + " is < 0. " );
            }

            /* Skip forward the right number of HDUs. */
            return skipHDUs( strm, ihdu );
        }
        else {
            return 0L;
        }
    }

    /**
     * Returns an input stream which can be used with the various FITS
     * classes based on a given DataSource object.  If the DataSource
     * has a position attribute, it will be interpreted as the zero-based
     * index of the HDU to start the stream at.  Otherwise, the stream
     * will start at the primary HDU (as if position="0").
     * 
     * @param  datsrc  the DataSource pointing to the file/HDU required
     * @return  an ArrayDataInput acquired from <tt>datsrc</tt>,
     *          and positioned according to its position
     */
    public static ArrayDataInput getInputStream( DataSource datsrc ) 
            throws IOException {
        ArrayDataInput strm = getInputStreamStart( datsrc );
        positionStream( strm, datsrc.getPosition() );
        return strm;
    }

    /**
     * Populates a header from an input stream, reporting its length in bytes.
     * This does the same as {@link nom.tam.fits.Header#read}, but
     * it returns the number of bytes read from the input stream in order
     * to populate the header (including any padding bytes).  There is
     * no way to retrieve this information from the <tt>Header</tt> class
     * in general; though {@link nom.tam.fits.Header#getSize} 
     * will sometimes give you the right answer, in the case of 
     * duplicated header keywords it can give an underestimate.
     * This could be seen as a bug in <tt>nom.tam.fits</tt> classes,
     * but there may be code somewhere which relies on that behaviour.
     * <p>
     * You can make a Header from scratch by doing
     * <pre>
     *     Header hdr = new Header();
     *     int headsize = read( hdr, strm );
     * </pre>
     * This method also differs from the <tt>Header</tt> implementation
     * in that it does not print warnings to standard output about
     * duplicate keywords.
     *
     * @param  hdr   the header to populate
     * @param  strm  the input stream supplying the data
     * @return  the number of bytes in the FITS blocks which comprise the
     *          header content
     * @see  nom.tam.fits.Header#read
     */
    /* The implementation is mostly copied from the Header class itself. */
    public static int readHeader( Header hdr, ArrayDataInput strm ) 
            throws TruncatedFileException, IOException {
        Cursor iter = hdr.iterator();
        byte[] buffer = new byte[ 80 ];
        boolean firstCard = true;
        int count = 0;
        while ( true ) {
            int len;
            int need = 80;
            try {
                while ( need > 0 ) {
                    len = strm.read( buffer, 80 - need, need );
                    if ( len <= 0 ) {
                        throw new TruncatedFileException();
                    }
                    need -= len;
                }
                count++;
            }
            catch ( EOFException e ) {
                if ( firstCard && need == 80 ) {
                    throw e;
                }
                throw new TruncatedFileException( e.getMessage() );
            }

            String cbuf = new String( buffer );
            HeaderCard fcard = new HeaderCard( cbuf );
            if ( firstCard ) {
                String key = fcard.getKey();
                if ( key == null || 
                    ( !key.equals( "SIMPLE" ) && !key.equals( "XTENSION" ) ) ) {
                    throw new IOException( "Not FITS format" );
                }
                firstCard = false;
            }
            String key = fcard.getKey();

            /* Add the card. */
            if ( fcard != null ) {
                if ( fcard.isKeyValuePair() ) {
                    iter.add( fcard.getKey(), fcard );
                }
                else {
                    iter.add( fcard );
                }
            }

            if ( cbuf.substring( 0, 8 ).equals( "END     ") ) {
                break;
            }
        }

        int pad = FitsUtil.padding( count * 80 );
        try {
            IOUtils.skipBytes( strm, pad );
        }
        catch ( EOFException e ) {
            throw new TruncatedFileException( e.getMessage() );
        }

        return pad + count * 80;
    }

    /**
     * Writes a header object to a DataOutput.
     *
     * @param   strm  destination stream
     * @param   hdr  the header to write
     */
    public static void writeHeader( final DataOutput strm, Header hdr )
            throws IOException {
        ArrayDataOutput ostrm =
            new BufferedDataOutputStream( new OutputStream() {
                public void write( int b ) throws IOException {
                    strm.write( b );
                }
                public void write( byte[] b ) throws IOException {
                    strm.write( b );
                }
                public void write( byte[] b, int off, int len )
                        throws IOException {
                    strm.write( b, off, len );
                }
            } );
        try {
            hdr.write( ostrm );
            ostrm.flush();
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Writes a null header representing an empty primary HDU to a stream.
     *
     * @param  strm  stream to write to
     */
    public static void writeEmptyPrimary( DataOutput strm )
        throws IOException {
        try {
            Header dummy = new Header();
            dummy.addValue( "SIMPLE", true, "Standard FITS format" );
            dummy.addValue( "BITPIX", 8, "Character data" );
            dummy.addValue( "NAXIS", 0, "No image, just extensions" );
            dummy.addValue( "EXTEND", true, "There are standard extensions" );
            dummy.insertComment(
                      "Dummy header; see following table extension" );
            dummy.insertCommentStyle( "END", "" );
            writeHeader( strm, dummy );
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.toString() )
                               .initCause( e );
        }
    }

    /**
     * Utility function to find the number of bytes in the data segment
     * of an HDU.  As far as I can see, Header.getDataSize() ought to
     * do this, but it doesn't seem to.
     *
     * @param  hdr  the Header 
     * @return  the number of bytes in the data segment 
     *          associated with <tt>hdr</tt>
     */
    public static long getDataSize( Header hdr ) {
        long nel = getRawSize( hdr );
        if ( nel % FITS_BLOCK == 0 ) {
            return nel;
        }
        else {
            return ( ( nel / FITS_BLOCK ) + 1 ) * FITS_BLOCK;
        }
    }

    /**
     * Writes suitable messages through the logging system about a DataSource
     * that contains FITS, concerning whether it is mappable (hence efficient
     * to read).
     *
     * @param   datsrc  data source containing FITS
     */
    public static void mappableWarning( DataSource datsrc ) throws IOException {
        String msg = null;
        Level level = Level.INFO;
        if ( datsrc instanceof FileDataSource &&
             datsrc.getCompression() != Compression.NONE ) {
            File file = ((FileDataSource) datsrc).getFile();
            msg = "Can't map compressed FITS file " + file + "; "
                + "uncompress it for better performance";
            level = file.length() > 10 * 1024 * 1024 ? Level.WARNING
                                                     : Level.INFO;
        }
        else if ( ! ( datsrc instanceof FileDataSource ) ) {
            msg = "Can't map FITS stream " + datsrc.getName() + "; "
                + "use local uncompressed file for better performance";
            level = Level.INFO;
        }
        if ( msg != null ) {
            logger.log( level, msg );
        }
    }

    /**
     * Adds a string-valued card to the header.  If the value is too long,
     * it is truncated appropriately, and a warning is emitted through
     * the logging system.
     *
     * @param  hdr  header
     * @param  key  card key
     * @param  value  card value
     * @param  comment  card comment
     */
    public static void addTrimmedValue( Header hdr, String key, String value,
                                        String comment )
            throws HeaderCardException {
        if ( value != null && value.length() > 68 ) {
            value = value.substring( 0, 65 ) + "...";
            logger.warning( "Truncated long FITS header card " + key + " = " + 
                            value );
        }
        hdr.addValue( key, value, comment );
    }

    private static long getRawSize( Header hdr ) {
        int naxis = hdr.getIntValue( "NAXIS", 0 );
        if ( naxis <= 0 ) {
            return 0;
        }
        int bitpix = hdr.getIntValue( "BITPIX" );
        boolean isRandomGroups = hdr.getIntValue( "NAXIS1" ) == 0 &&
                                 hdr.getBooleanValue( "SIMPLE" ) &&
                                 hdr.getBooleanValue( "GROUPS" );
        long nel = 1;
        for ( int i = isRandomGroups ? 2 : 1; i <= naxis; i++ ) {
            nel *= hdr.getLongValue( "NAXIS" + i );
        }
        long pcount = 0;
        long gcount = 1;
        if ( hdr.containsKey( "XTENSION" ) || isRandomGroups ) {
            pcount = hdr.getLongValue( "PCOUNT", 0 );
            gcount = hdr.getLongValue( "GCOUNT", 1 );
        }
        return ( nel + pcount ) * gcount * Math.abs( bitpix ) / 8;
    }
}
