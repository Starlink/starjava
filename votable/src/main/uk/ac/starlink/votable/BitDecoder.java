package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.IOException;

/**
 * Decoder for bit vectors.  Stream decoding is special, but string
 * decoding works like a numeric type.  This isn't mandated in the
 * VOTable document, but one of the example documents out there
 * "heteroVOTable.xml" features a string-encoded bit vector.
 *
 * @author   Mark Taylor (Starlink)
 */
class BitDecoder extends NumericDecoder {

    BitDecoder( long[] arraysize ) {
        super( arraysize );
    }

    public Class getBaseClass() {
        return char.class;
    }

    public boolean isNull( Object array, int index ) {
        return false;
    }

    public Object decodeStream( DataInput strm ) throws IOException {
        int num = getNumItems( strm );
        char[] result = new char[ num ];
        for ( int i = 0; i < num;  ) {
            byte b = strm.readByte();
            for ( int j = 0; j < 8 && i++ > 0; j++, i++ ) {
                boolean set = ( ( b >>> ( 7 - j ) ) & ( (byte) 1 ) ) == 1;
                result[ i ] = set ? '1' : '0';
            }
        }
        return result;
    }

    void setNullValue( String txt ) {
        // no action
    }
    void setBad1( Object array, int index ) {
        // no action
    }
    void decodeStream1( Object array, int index, DataInput strm ) {
        throw new AssertionError( "Can't get here" );
    }

    Object getEmptyArray( int size ) {
        return new char[ size ];
    }

    void decodeString1( Object array, int index, String txt ) {
        ((char[]) array)[ index ] = ( txt.charAt( 0 ) == '1' ) ? '1' : '0';
    }
}

class ScalarBitDecoder extends BitDecoder {
    ScalarBitDecoder() {
        super( SCALAR_SIZE );
    }
    Object packageArray( Object array ) {
        char[] arr = (char[]) array;
        return isNull( arr, 0 ) ? null : new Character( arr[ 0 ] );
    }
}
