package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.IOException;

class CharDecoder extends Decoder {
    private char bad = ' ';
    private String badString;
    private long[] decodedShape;

    public CharDecoder( long[] arraysize ) {
        super( arraysize );
        int ndim = arraysize.length;
        if ( ndim == 0 ) {
            decodedShape = new long[ 0 ];
        }
        else {
            decodedShape = new long[ ndim - 1 ];
            System.arraycopy( arraysize, 1, decodedShape, 0, ndim - 1 );
        }
    }

    public Class getBaseClass() {
        return String.class;
    }

    public Object decodeString( String txt ) {
        int ntok = txt.length();
        int ncell = numCells( ntok );
        int ndim = arraysize.length;
        if ( ndim == 0 ) {  // single character
            if ( ntok > 0 ) {
                return txt.substring( 0, 1 );
            }
            else {
                return "" + bad;
            }
        }
        else if ( ndim == 1 && ntok == ncell ) {
            return txt;
        }
        else if ( ndim == 1 ) {
            StringBuffer sb = new StringBuffer( ncell );
            for ( int i = 0; i < ncell; i++ ) {
                if ( i < ntok ) {
                    sb.append( txt.charAt( i ) );
                }
                else {
                    sb.append( bad );
                }
            }
            return sb.toString();
        }
        else {
            int sleng = (int) arraysize[ 0 ];
            int nstr = ncell / sleng;
            String[] result = new String[ nstr ];
            int k = 0;
            for ( int i = 0; i < nstr; i++ ) {
                StringBuffer sb = new StringBuffer( sleng );
                for ( int j = 0; j < sleng; j++ ) {
                    if ( k < ntok ) {
                        sb.append( txt.charAt( k++ ) );
                    }
                    else {
                        sb.append( bad );
                    }
                }
                result[ i ] = sb.toString();
            }
            return result;
        }
    }

    public Object decodeStream( DataInput strm ) throws IOException {
        int num = getNumItems( strm );
        char[] data = new char[ num ];
        for ( int i = 0; i < num; i++ ) {
            data[ i ] = (char) ( (char) strm.readByte() & (char) 0x00ff );
        }
        return makeStrings( data );
    }

    protected Object makeStrings( char[] data ) {
        if ( isVariable && arraysize.length == 1 ) {
            return new String( data );
        }
        int nstr = data.length / sliceSize;
        String[] result = new String[ nstr ];
        for ( int i = 0; i < nstr; i++ ) {
            result[ i ] = new String( data, i * sliceSize, sliceSize );
        }
        return result;
    }

    void setNullValue( String txt ) {
        badString = txt;
    }

    public boolean isNull( Object obj, int index ) {
        if ( obj instanceof String ) {
            return obj.equals( badString );
        }
        else if ( obj instanceof String[] ) {
            return ((String[]) obj)[ index ].equals( badString );
        }
        else {
            return false;
        }
    }

    public long[] getDecodedShape() {
        return decodedShape;
    }
}


class UnicodeCharDecoder extends CharDecoder {

    public UnicodeCharDecoder( long[] arraysize ) {
        super( arraysize );
    }

    public Object decodeStream( DataInput strm ) throws IOException {
        int num = getNumItems( strm );
        char[] data = new char[ num ];
        for ( int i = 0; i < num; i++ ) {
            data[ i ] = strm.readChar();
        }
        return makeStrings( data );
    }
}

