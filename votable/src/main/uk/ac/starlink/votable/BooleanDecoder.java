package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.IOException;

class BooleanDecoder extends NumericDecoder {

    BooleanDecoder( Class clazz, long[] arraysize ) {
        super( clazz, arraysize );
    }

    BooleanDecoder( long[] arraysize ) {
        this( boolean[].class, arraysize );
    }

    public boolean isNull( Object array, int index ) {
        return false;
    }

    void setNullValue( String txt ) {
        // silly
    }

    Object getEmptyArray( int size ) {
        return new boolean[ size ];
    }

    void decodeString1( Object array, int index, String txt ) {
        int leng = txt.length();
        Boolean flag = null;
        for ( int i = 0; flag == null && i < leng; i++ ) {
            switch ( txt.charAt( i ) ) {
                case 'T':
                case 't':
                case '1':
                    flag = Boolean.TRUE;
                    break;
                case 'F':
                case 'f':
                case '0':
                    flag = Boolean.FALSE;
                    break;
            }
        }
        ((boolean[]) array)[ index ] = flag == Boolean.TRUE;
    }

    void decodeStream1( Object array, int index, DataInput strm )
            throws IOException {
        boolean flag;
        switch ( (char) 0x00ff & (char) strm.readByte() ) {
            case 'T':
            case 't':
            case '1':
                flag = true;
                break;
            default:
                flag = false;
        }
        ((boolean[]) array)[ index ] = flag;
    }

    void setBad1( Object array, int index ) {
        // can't get here?
    }
}

class ScalarBooleanDecoder extends BooleanDecoder {
    ScalarBooleanDecoder() {
        super( Boolean.class, SCALAR_SIZE );
    }
    Object packageArray( Object array ) {
        boolean[] arr = (boolean[]) array;
        return Boolean.valueOf( arr[ 0 ] );
    }
}
