package uk.ac.starlink.fits;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.FitsUtil;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.TruncatedFileException;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.Cursor;
import uk.ac.starlink.array.Type;

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
     */
    static void skipHDUs( ArrayDataInput stream, int nskip )
            throws IOException {
        try {
            while ( nskip-- > 0 ) {
                Header hdr = new Header( stream );
                for ( long hbytes = getDataSize( hdr ); hbytes > 0; ) {
                    hbytes -= stream.skip( hbytes );
                }
            }
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
     * @param   buffer  a byte buffer of at least 9 elements containing
     *          the start of a file to test
     * @return  <tt>true</tt> iff the bytes in <tt>buffer</tt> look like 
     *          the start of a FITS file
     * @throws  IllegalArgumentException  if <tt>buffer</tt> is too short
     */
    public static boolean isMagic( byte[] buffer ) {
        if ( buffer.length < 9 ) {
            throw new IllegalArgumentException(
                "Supplied buffer must be at least 9 elements long" );
        }
        return (char) buffer[ 0 ] == 'S' &&
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
     * @param  dis  the input stream supplying the data
     * @param  hdr   the header to populate
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
                    count++;
                    if ( len == 0 ) {
                        throw new TruncatedFileException();
                    }
                    need -= len;
                }
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
            strm.skipBytes( pad );
        }
        catch ( IOException e ) {
            throw new TruncatedFileException( e.getMessage() );
        }

        return pad + count * 80;
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

    private static long getRawSize( Header hdr ) {
        int naxis = hdr.getIntValue( "NAXIS", 0 );
        if ( naxis <= 0 ) {
            return 0;
        }
        long nel = 1;
        for ( int i = 1; i <= naxis; i++ ) {
            nel *= hdr.getLongValue( "NAXIS" + i );
        }
        int bitpix = hdr.getIntValue( "BITPIX" );
        int pcount = 0;
        int gcount = 1;
        if ( hdr.containsKey( "XTENSION" ) ) {
            pcount = hdr.getIntValue( "PCOUNT", 0 );
            gcount = hdr.getIntValue( "GCOUNT", 1 );
        }
        return ( nel + pcount ) * gcount * Math.abs( bitpix ) / 8;
    }


}
