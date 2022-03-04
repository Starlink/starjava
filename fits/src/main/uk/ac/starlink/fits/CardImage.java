package uk.ac.starlink.fits;

import java.nio.charset.StandardCharsets;

/**
 * Represents a FITS header card that will be written.
 * It is a thin wrapper around an 80-byte buffer providing the ASCII content.
 *
 * @author   Mark Taylor
 * @since    4 Mar 2022
 * @see    FitsUtil#writeHeader
 */
public class CardImage {

    private final byte[] buf80_;

    /**
     * Constructs a CardImage from an 80-element byte array.
     *
     * @param  buf80   80-byte array containing ASCII characters
     * @throws  IllegalArgumentException  if the array is the wrong length
     *                                    or contains illegal characters
     */
    public CardImage( byte[] buf80 ) {
        if ( buf80.length != 80 ) {
            throw new IllegalArgumentException( "Buffer wrong length ("
                                              + buf80.length + " != 80" );
        }
        for ( int i = 0; i < 80; i++ ) {
            checkFitsCharacter( ( (int) buf80[ i ] ) & 0xff );
        }
        buf80_ = buf80;
    }

    /**
     * Constructs a CardImage from an 80-character CharSequence.
     *
     * @param  txt80  80-character sequence containing ASCII characters
     * @throws  IllegalArgumentException  if the string is the wrong length
     *                                    or contains illegal characters
     */
    public CardImage( CharSequence txt80 ) {
        this( toByteArray( txt80 ) );
    }

    /**
     * Returns this image as a byte array.
     *
     * @return 80-element byte array
     */
    public byte[] getBytes() {
        return buf80_;
    }

    /**
     * @return  80-character string
     */
    @Override
    public String toString() {
        return new String( buf80_, StandardCharsets.US_ASCII );
    }

    /**
     * Converts an ASCII CharSequence to a byte array.
     *
     * @param  txt  string
     * @return  byte array of input ASCII characters
     * @throws  IllegalArgumentException  if any characters are
     *                                    not FITS compatible
     */
    private static byte[] toByteArray( CharSequence txt ) {
        int n = txt.length();
        byte[] buf = new byte[ n ];
        for ( int i = 0; i < n; i++ ) {
            char ch = txt.charAt( i );
            checkFitsCharacter( ch );
            buf[ i ] = (byte) ch;
        }
        return buf;
    }

    /**
     * Throws an IllegalArgumentException if the submitted character is
     * not a legal FITS header character (0x20..0x7e).
     *
     * @param  ch  character to check
     */
    private static void checkFitsCharacter( int ch ) {
        if ( ch < 32 || ch > 126 ) {
            throw new IllegalArgumentException( "Bad character: 0x"
                                              + Integer.toHexString( ch ) );
        }
    }
}
