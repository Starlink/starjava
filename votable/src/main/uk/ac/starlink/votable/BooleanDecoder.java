package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.IOException;

class BooleanDecoder extends NumericDecoder {
    private char bad = ' ';

    BooleanDecoder( long[] arraysize ) {
        super( arraysize );
    }

    public Class getBaseClass() {
        return char.class;
    }

    public boolean isNull( Object array, int index ) {
        return ((char[]) array)[ index ] == bad;
    }

    void setNullValue( String txt ) {
        // silly
    }

    Object getEmptyArray( int size ) {
        return new char[ size ];
    }

    void decodeString1( Object array, int index, String txt ) {
        if ( txt.length() == 0 ) {
            setBad1( array, index );
            return;
        }
        else if ( txt.length() > 1 ) {
            txt = txt.trim();
        }
        ((char[]) array)[ index ] = decodeChar( txt.charAt( 0 ) );
    }

    void decodeStream1( Object array, int index, DataInput strm )
            throws IOException {
        ((char[]) array)[ index ] = 
            decodeChar( (char) ( (char) 0x00ff & (char) strm.readByte() ) );
    }

    void setBad1( Object array, int index ) {
        ((char[]) array)[ index ] = bad;
    }

    private char decodeChar( char chr ) {
        switch ( chr ) {
            case 'T':
            case 't':
            case '1':
                return 'T';
            case 'F':
            case 'f':
            case '0':
                return 'F';
            case '?':
            case ' ':
                return bad;
            default:
                return bad;
        }
    }
}

class ScalarBooleanDecoder extends BooleanDecoder {
    ScalarBooleanDecoder() {
        super( SCALAR_SIZE );
    }
    Object packageArray( Object array ) {
        char[] arr = (char[]) array;
        return isNull( arr, 0 ) ? null : new Character( arr[ 0 ] );
    }
}
