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
    private Boolean decodeChar( char c ) {
        switch ( c ) {
            case 'T':
            case 't':
            case '1':
                return Boolean.TRUE;
            case 'F':
            case 'f':
            case '0':
                return Boolean.FALSE;
            case '\0':
            case '?':
            case ' ':
                return null;
            default:
                return null;
        }
    }
    public Object decodeString( String txt ) {
        int nchar = txt.length();
        if ( nchar == 0 ) {
            return null;
        }
        else if ( nchar == 1 ) {
            return decodeChar( txt.charAt( 0 ) );
        }
        else if ( txt.equalsIgnoreCase( "true" ) ) {
            return Boolean.TRUE;
        }
        else if ( txt.equalsIgnoreCase( "false" ) ) {
            return Boolean.FALSE;
        }
        else {
            return null;
        }
    }
    public Object decodeStream( DataInput strm ) throws IOException {
        return decodeChar( (char) ( (char) 0x00ff & (char) strm.readByte() ) );
    }
}
