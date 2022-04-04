// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

/**
 * Bit manipulation functions.
 *
 * <p>Note that for bitwise AND, OR, XOR of integer values etc you can use
 * the java bitwise operators
 * "<code>&amp;</code>", "<code>|</code>", "<code>^</code>".
 *
 * @author   Mark Taylor
 * @since    4 Apr 2022
 */
public class Bits {

    /**
     * Private constructor prevents instantiation.
     */
    private Bits() {
    }

    /**
     * Determines whether a given integer has a certain bit set to 1.
     *
     * @example  <code>hasBit(64, 6) = true</code>
     * @example  <code>hasBit(63, 6) = false</code>
     *
     * @param  value  integer whose bits are to be tested
     * @param  bitIndex    index of bit to be tested in range 0..63,
     *                     where 0 is the least significant bit
     * @return  true if bit is set; more or less equivalent to
     *          <code>(value &amp; 1L&lt;&lt;bitIndex) != 0</code>
     */
    public static boolean hasBit( long value, int bitIndex ) {
        return bitIndex >= 0 && bitIndex < 64 
            && ( value & ( 1L << bitIndex ) ) != 0;
    }

    /**
     * Returns the number of set bits in the 64-bit two's complement
     * representation of the integer argument.
     *
     * @example  <code>bitCount(64) = 1</code>
     * @example  <code>bitCount(3) = 2</code>
     *
     * @param  i  integer value
     * @return  number of "1" bits in the binary representation of
     *          <code>i</code>
     */
    public static int bitCount( long i ) {
        return Long.bitCount( i );
    }

    /**
     * Converts the integer argument to a binary string consisting
     * only of 1s and 0s.
     *
     * @example  <code>toBinary(42) = "101010"</code>
     * @example  <code>toBinary(255^7) = "11111000"</code>
     *
     * @param  value  integer value
     * @return  binary representation of <code>value</code>
     */
    public static String toBinary( long value ) {
        return Long.toBinaryString( value );
    }

    /**
     * Converts a string representing a binary number to its integer value.
     *
     * @example  <code>fromBinary("101010") = 42</code>
     *
     * @param    binVal  binary representation of value
     * @return   integer value represented by binary string <code>binVal</code>
     */
    public static int fromBinary( String binVal ) {
        return Integer.parseInt( binVal.trim(), 2 );
    }
}
