package uk.ac.starlink.fits;

import java.nio.charset.StandardCharsets;
import uk.ac.starlink.table.Tables;

/**
 * Serializes String values to byte arrays suitable for output into
 * FITS table columns.
 *
 * @author   Mark Taylor
 * @since    20 Mar 2026
 */
public abstract class StringEncoder {

    /**
     * Writes one byte per code point; non-ASCII code points are represented
     * by a replacement character.
     */
    public static final StringEncoder SQUASH_TO_ASCII =
        new AsciiEncoder( "ASCII", (byte) '?' );

    /** Default instance. */
    public static final StringEncoder DEFAULT = SQUASH_TO_ASCII;

    private final String name_;

    /**
     * Constructor.
     *
     * @param  name  encoder name
     */
    protected StringEncoder( String name ) {
        name_ = name;
    }

    /**
     * Encodes the given string into a new byte array.
     *
     * @param  txt  string to encode, not null
     * @return   new array containing encoded text
     */
    public abstract byte[] toBytes( String txt );

    /**
     * Encodes the given string into an existing byte array.
     * The encoding will be truncated if the supplied buffer is
     * too short.
     *
     * @param  txt  string to encode, not null
     * @param  buf  buffer to receive encoded text
     * @return   number of bytes written to buffer
     */
    public abstract int copyToBytes( String txt, byte[] buf );

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Maps each code point to a FITS-valid ASCII character.
     * If the code point is not ascii, a replacement character is substituted
     * in the output.
     */
    private static class AsciiEncoder extends StringEncoder {

        private final byte replacementChar_;

        /**
         * Constructor.
         *
         * @param  name  encoder name
         * @param  replacementChar   character to represent non-ASCII
         */
        AsciiEncoder( String name, byte replacementChar ) {
            super( name );
            replacementChar_ = replacementChar;
        }

        public byte[] toBytes( String txt ) {
            int[] codePoints = txt.codePoints().toArray();
            byte[] buf = new byte[ codePoints.length ];
            for ( int i = 0; i < codePoints.length; i++ ) {
                int cp = codePoints[ i ];
                buf[ i ] = FitsUtil.isFitsCharacter( cp )
                         ? (byte) cp
                         : replacementChar_;
            }
            return buf;
        }

        public int copyToBytes( String txt, byte[] buf ) {
            int[] index = new int[ 1 ];
            txt.codePoints().limit( buf.length ).forEach( cp -> {
                buf[ index[ 0 ]++ ] = FitsUtil.isFitsCharacter( cp )
                                    ? (byte) cp
                                    : replacementChar_;
            } );
            return index[ 0 ];
        }
    }
}
