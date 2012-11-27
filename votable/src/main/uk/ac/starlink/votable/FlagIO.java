package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Utility methods to performs I/O for flag arrays.
 * The methods here are suitable for use with the null flag arrays
 * defined by the BINARY2 serialization format.
 * The number of bytes read/written is (n+7)/8, that is the number of
 * bytes is the minimum which will accommodate the number of bit flags
 * required.
 *
 * @author   Mark Taylor
 * @since    15 Nov 2012
 */
class FlagIO {

    /**
     * Writes an array of flags to an output stream.
     * The number of bits written is determined by the length of the array.
     *
     * @param  out  output stream
     * @param  flags  flag array
     */
    public static void writeFlags( DataOutput out, boolean[] flags )
            throws IOException {
        int nflag = flags.length;
        int buf = 0;
        for ( int iflag = 0; iflag < nflag; iflag++ ) {
            int ibit = iflag % 8;
            if ( ibit == 0 && iflag > 0 ) {
                out.write( buf );
                buf = 0;
            }
            if ( flags[ iflag ] ) {
                buf |= mask( ibit );
            }
        }
        out.write( buf );
    }

    /**
     * Reads an array of flags from an input stream.
     * The number of bits read is determined by the length of the array.
     *
     * @param   in  input stream 
     * @param   flags  flag array
     */
    public static void readFlags( DataInput in, boolean[] flags )
            throws IOException {
        int nflag = flags.length;
        int buf = 0;
        for ( int iflag = 0; iflag < nflag; iflag++ ) {
            int ibit = iflag % 8;
            if ( ibit == 0 ) {
                buf = in.readUnsignedByte();
            }
            flags[ iflag ] = ( buf & mask( ibit ) ) != 0;
        }
    }

    /**
     * Bit mask for a given bit index.
     * The mask bit selection corresponds to that required for
     * BINARY2 flag encoding.
     *
     * @param   ibit  index of bit within byte; in range 0-7
     * @return  integer with one bit set, corresponding to bit index
     *          <code>ibit</code>
     */
    private static int mask( int ibit ) {
        return 0x80 >> ibit;
    }
}
