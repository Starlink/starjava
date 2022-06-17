package uk.ac.starlink.fits;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.table.Tables;

/**
 * Utilities for working with FITS files.
 *
 * @author   Mark Taylor
 * @since    4 Mar 2022
 */
public class FitsUtil {

    /** FITS block length in bytes (@value). */
    public static final int BLOCK_LENG = 2880;

    /** FITS header card length in bytes (@value). */
    public static final int CARD_LENG = 80;

    /** Number of header cards per FITS block (@value). */
    public static final int CARDS_PER_BLOCK = BLOCK_LENG / CARD_LENG;

    /** Maximum No. of columns in standard FITS BINTABLE extension (@value). */
    public static final int MAX_NCOLSTD = 999;

    /** Regex pattern matching floating point value, no grouping. */
    public static final String FLOAT_REGEX =
        "(?:[+-]?(?:[0-9]*\\.[0-9]+|[0-9]+\\.?))(?:[ED][+-]?[0-9]+)?";

    // Note use of possessive quantifier to avoid an exponentially growing
    // stack when parsing many-element arrays.
    private static final Pattern FLOATARRAY_PATTERN =
        Pattern.compile( "\\s*[(]\\s*" + FLOAT_REGEX
                       + "(\\s*[,]\\s*" + FLOAT_REGEX + ")*+"
                       + "\\s*[)]\\s*" );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Private constructor prevents instantiation.
     */
    private FitsUtil() {
    }

    /**
     * Indicates whether the supplied buffer is the start of a FITS file.
     * Its contents is checked against the FITS 'magic number', which is
     * the ASCII string "<tt>SIMPLE&nbsp;&nbsp;=</tt>".
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
     * Indicates whether a given character is a legal FITS header character
     * (0x20..0x7e inclusive).
     *
     * @param  ch  character to check
     * @return   true iff ch is legal for inclusion in a FITS header
     */
    public static boolean isFitsCharacter( int ch ) {
        return ch >= 0x20 && ch <= 0x7e;
    }

    /**
     * Reads a FITS header from an input stream.
     * The stream is read until the end of the last header block.
     *
     * @param   in  input stream positioned at start of HDU
     * @return   header
     */
    public static FitsHeader readHeader( InputStream in ) throws IOException {
        List<ParsedCard<?>> list = new ArrayList<>();
        byte[] blockBuf = new byte[ BLOCK_LENG ];
        byte[] cardBuf = new byte[ CARD_LENG ];
        while ( true ) {

            /* Read next block into buffer. */
            for ( int ngot = 0; ngot < BLOCK_LENG; ) {
                int n = in.read( blockBuf, ngot, BLOCK_LENG - ngot );
                if ( n < 0 ) {
                    if ( ngot == 0 ) {
                        if ( list.size() == 0 ) {
                            throw new EOFException( "FITS ended before header");
                        }
                        else {
                            throw new EOFException( "FITS ended mid-header" );
                        }
                    }
                    else {
                        throw new EOFException( "FITS ended mid-block" );
                    }
                }
                ngot += n;
            }

            /* Read cards from buffer. */
            for ( int ipos = 0; ipos < BLOCK_LENG; ipos += CARD_LENG ) {
                System.arraycopy( blockBuf, ipos, cardBuf, 0, CARD_LENG );
                ParsedCard<?> card = parseCard( cardBuf );
                list.add( card );

                /* If END card is encountered, construct and return header. */
                if ( CardType.END == card.getType() ) {
                    ParsedCard<?>[] cards =
                        list.toArray( new ParsedCard<?>[ 0 ] );
                    return new FitsHeader( cards );
                }
            }
        }
    }

    /**
     * Turns an 80-byte array into a ParsedCard.
     * This will always succeed, but if the card doesn't look like a FITS
     * header, the result will have CardType.UNKNOWN.
     *
     * @param  buf80   80-byte array giving card image
     */
    public static ParsedCard<?> parseCard( byte[] buf80 ) {
        String txt80 = new String( buf80, StandardCharsets.US_ASCII );
        for ( CardType<?> type : CardType.CARD_TYPES ) {
            ParsedCard<?> card = type.toCard( txt80 );
            if ( card != null ) {
                return card;
            }
        }
        return new ParsedCard<Void>( null, CardType.UNKNOWN, null, null );
    }

    /**
     * Skips forward over a given number of HDUs in the supplied stream.
     * If it reaches the end of the stream, it throws an IOException
     * with a Cause of a TruncatedFileException.
     *
     * @param  in  the stream to skip through, positioned at start of HDU
     * @param  nskip  the number of HDUs to skip
     * @return  the number of bytes the stream was advanced
     */
    public static long skipHDUs( InputStream in, int nskip )
            throws IOException {
        long advance = 0L;
        while ( nskip-- > 0 ) {
            FitsHeader hdr = readHeader( in );
            advance += hdr.getHeaderByteCount();
            long datasize = hdr.getDataByteCount();
            IOUtils.skip( in, datasize );
            advance += datasize;
        }
        return advance;
    }

    /**
     * Utility method to round an integer value up to a multiple of
     * a given block size.
     *
     * @param  value  non-negative count
     * @param  blockSize   non-negative size of block
     * @return   smallest integer that is &gt;=<code>count</code>
     *           and a multiple of <code>blockSize</code>
     */
    public static long roundUp( long value, int blockSize ) {
        return ( ( value + blockSize - 1 ) / (long) blockSize ) * blockSize;
    }

    /**
     * Writes a FITS header whose content is supplied by an array of cards.
     * No checks are performed on the card content.
     * An END card must be included in the supplied array if required.
     * Padding is written to advance to a whole number of FITS blocks.
     *
     * @param  cards  cards forming content of header
     * @param  out  destination stream
     * @return  number of bytes written, including padding
     */
    public static int writeHeader( CardImage[] cards, OutputStream out )
            throws IOException {
        int ncard = cards.length;
        int nbyte = Tables.checkedLongToInt( roundUp( cards.length * CARD_LENG,
                                                      BLOCK_LENG ) );
        byte[] buf = new byte[ nbyte ];
        for ( int ic = 0; ic < ncard; ic++ ) {
            System.arraycopy( cards[ ic ].getBytes(), 0,
                              buf, ic * CARD_LENG, CARD_LENG );
        }
        Arrays.fill( buf, ncard * CARD_LENG, nbyte, (byte) 0x20 );
        out.write( buf );
        return nbyte;
    }

    /**
     * Writes a data-less Primary HDU.
     * It declares EXTEND = T, indicating that extension HDUs will follow.
     *
     * @param   out  destination stream
     */
    public static void writeEmptyPrimary( OutputStream out )
            throws IOException {
        CardFactory cfact = CardFactory.STRICT;
        CardImage[] cards = new CardImage[] {
            cfact.createLogicalCard( "SIMPLE", true, "Standard FITS format" ),
            cfact.createIntegerCard( "BITPIX", 8, "Character data" ),
            cfact.createIntegerCard( "NAXIS", 0, "No image, just extensions" ),
            cfact.createLogicalCard( "EXTEND", true,
                                     "There are standard extensions" ),
            cfact.createCommentCard( "Dummy header; "
                                   + "see following table extension" ),
            CardFactory.END_CARD,
        };
        writeHeader( cards, out );
    }

    /**
     * Checks that a table with the given number of columns can be written.
     * If the column count is not exceeded, nothing happens,
     * but if there are too many columns an informative IOException
     * is thrown.
     *
     * @param  wide   extended column convention
     *                - may be null for FITS standard behaviour only
     * @param  ncol   number of columns to write
     * @throws   IOException  if there are too many columns
     */
    public static void checkColumnCount( WideFits wide, int ncol )
            throws IOException {
        if ( wide == null ) {
            if ( ncol > MAX_NCOLSTD ) {
                String msg = new StringBuffer()
                    .append( "Too many columns " )
                    .append( ncol )
                    .append( " > " )
                    .append( MAX_NCOLSTD )
                    .append( " (FITS standard hard limit)" )
                    .toString();
                throw new IOException( msg );
            }
        }
        else {
            int nmax = wide.getExtColumnMax();
            if ( ncol > nmax ) {
                String msg = new StringBuffer()
                    .append( "Too many column " )
                    .append( ncol )
                    .append( " > " )
                    .append( nmax )
                    .append( " (limit of extended column convention" )
                    .toString();
                throw new IOException( msg );
            }
        }
    }

    /**
     * Attempts to interpret a string as a formatted numeric array.
     * The string has to be of the form "(x, x, ...)", where x has the
     * same form as a floating point header value.  Whitespace is permitted.
     * The output will be an int[] array if the tokens all look like 32-bit
     * integers, or a double[] array if the tokens all look like floating
     * point numbers, or null otherwise.
     *
     * <p>This is a bit hacky, it doesn't correspond to prescriptions in
     * the FITS stanard, but it's useful for some purposes.
     *
     * @param  txt  string
     * @return  int[] array or double[] array or null
     */
    public static Object asNumericArray( String txt ) {
        if ( FLOATARRAY_PATTERN.matcher( txt ).matches() ) {
            String[] tokens =
                txt.substring( txt.indexOf( '(' ) + 1, txt.indexOf( ')' ) )
                   .split( "\\s*,\\s*" );
            int n = tokens.length;
            boolean isInts = true;
            int[] ivals = new int[ n ];
            double[] dvals = new double[ n ];
            try {
                for ( int i = 0; i < n; i++ ) {
                    dvals[ i ] = Double.parseDouble( tokens[ i ] );
                    ivals[ i ] = (int) dvals[ i ];
                    isInts &= ivals[ i ] == dvals[ i ];
                }
            }
            catch ( NumberFormatException e ) {
                return null;
            }
            return isInts ? ivals : dvals;
        }
        else {
            return null;
        }
    }
}
