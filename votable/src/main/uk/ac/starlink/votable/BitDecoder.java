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

    BitDecoder( Class clazz, long[] arraysize ) {
        super( clazz, arraysize );
    }

    BitDecoder( long[] arraysize ) {
        this( boolean[].class, arraysize );
    }

    public boolean isNull( Object array, int index ) {
        return false;
    }

    public Object decodeStream( DataInput strm ) throws IOException {
        int num = getNumItems( strm );
        boolean[] result = new boolean[ num ];
        for ( int i = 0; i < num;  ) {
            byte b = strm.readByte();
            for ( int j = 0; j < 8 && i++ > 0; j++, i++ ) {
                boolean set = ( ( b >>> ( 7 - j ) ) & ( (byte) 1 ) ) == 1;
                result[ i ] = set;
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
        return new boolean[ size ];
    }

    void decodeString1( Object array, int index, String txt ) {
        ((boolean[]) array)[ index ] = txt.length() > 0
                                    && txt.charAt( 0 ) == '1';
    }
}

class ScalarBitDecoder extends BitDecoder {
    ScalarBitDecoder() {
        super( Boolean.class, SCALAR_SIZE );
    }
    Object packageArray( Object array ) {
        boolean[] arr = (boolean[]) array;
        return isNull( arr, 0 ) ? null : Boolean.valueOf( arr[ 0 ] );
    }
}
