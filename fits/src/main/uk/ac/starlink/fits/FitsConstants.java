package uk.ac.starlink.fits;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.TruncatedFileException;
import nom.tam.util.ArrayDataInput;
import uk.ac.starlink.array.Type;

/**
 * Utility class providing some constants and static methods related to 
 * FITS file format and starlink classes.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsConstants {

    /**
     * Origin of an NDArray in each dimension if not specified.
     * I've used 1 here to match HDS arrays - but should it be zero?
     */
    public static final long DEFAULT_ORIGIN = 1L;

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

    /** FITS header card for location (relative URL) of Variance NDArray. */
    public static final String NDX_VARIANCE = NDX_PREFIX + "VAR";

    /** FITS header card for location (relative URL) of Quality NDArray. */
    public static final String NDX_QUALITY = NDX_PREFIX + "QUA";

    /** FITS header card for value of bad bits mask. */
    public static final String NDX_BADBITS = NDX_PREFIX + "BBT";

    /** FITS header card for location (relative URL) of ETC character array. */
    public static final String NDX_ETC = NDX_PREFIX + "ETC";

    /** FITS header card for value of title string. */
    public static final String NDX_TITLE = NDX_PREFIX + "TIT";

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

    static long[] defaultOrigin( int ndim ) {
        long[] origin = new long[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            origin[ i ] = DEFAULT_ORIGIN;
        }
        return origin;
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
                stream.skip( getDataSize( hdr ) );
            }
        }
        catch ( TruncatedFileException e ) {
            throw (IOException) new IOException( "FITS file too short: "
                                                 + e.getMessage() )
                               .initCause( e );
        }
    }

    /*
     * Utility function to find the number of bytes in the data segment
     * of an HDU.  As far as I can see, Header.getDataSize() ought to
     * do this, but it doesn't seem to.
     */
    static long getDataSize( Header hdr ) {
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
