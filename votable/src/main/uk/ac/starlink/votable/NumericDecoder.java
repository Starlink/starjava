package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.IOException;
import java.util.StringTokenizer;

abstract class NumericDecoder extends Decoder {

    /**
     * Does required setup for a NumericDecoder.
     *
     * @param  the dimensions of objects with this type - the last element
     *         of the array may be negative to indicate unknown slowest-varying
     *         dimension
     */
    NumericDecoder( long[] arraysize ) {
        super( arraysize );
    }

    /**
     * Returns an empty array of the given size, suitable for storing
     * elements decoded by this object.
     *
     * @param  size  number of elements in array
     * @return  the array ready to store things in
     */
    abstract Object getEmptyArray( int size );

    /**
     * Sets one element of a storage array bad.
     *
     * @param  array  array returned by a previous call to getEmptyArray
     * @param  index  the element of this array to set bad
     */
    abstract void setBad1( Object array, int index );

    /**
     * Reads a single value from a string and places it in a storage array.
     *
     * @param  array  array returned by a previous call to getEmptyArray
     * @param  index  the element of this array to store the result in
     * @param  txt    the string to read from
     * @throws IllegalArgumentException  if the string did not contain a
     *         valid value for this Decoder
     */
    abstract void decodeString1( Object array, int index, String txt );

    /**
     * Reads a single value from a stream and places it in a storage array.
     *
     *
     * @param  array  array returned by a previous call to getEmptyArray
     * @param  index  the element of this array to store the result in
     * @param  txt    the string to read from
     * @throws IOException  if there was a read error
     */
    abstract void decodeStream1( Object array, int index, DataInput strm )
            throws IOException;

    public Object decodeString( String txt ) {
        StringTokenizer st = new StringTokenizer( txt );
        int ntok = st.countTokens();
        int ncell = numCells( ntok );
        Object result = getEmptyArray( ncell );
        for ( int i = 0; i < ncell; i++ ) {
            if ( i < ntok ) {
                String tok = st.nextToken().trim();
                if ( ! tok.equals( blankString ) ) {
                    try {
                        decodeString1( result, i, tok );
                    }
                    catch ( IllegalArgumentException e ) {
                        setBad1( result, i );
                    }
                }
                else {
                    setBad1( result, i );
                }
            }
            else {
                setBad1( result, i );
            }
        }
        return packageArray( result );
    }

    public Object decodeStream( DataInput strm ) throws IOException {
        int num = getNumItems( strm );
        Object result = getEmptyArray( num );
        for ( int i = 0; i < num; i++ ) {
            decodeStream1( result, i, strm );
        }
        return packageArray( result );
    }
}

class ShortDecoder extends NumericDecoder {
    private short bad;
    ShortDecoder( long[] arraysize ) {
        super( arraysize );
    }
    void setNullValue( String txt ) {
        bad = Short.parseShort( txt );
    }
    Object getEmptyArray( int size ) {
        return new short[ size ];
    }
    void decodeString1( Object array, int index, String txt ) {
        ((short[]) array)[ index ] = Short.parseShort( txt );
    }
    void decodeStream1( Object array, int index, DataInput strm )
            throws IOException {
        ((short[]) array)[ index ] = strm.readShort();
    }
    void setBad1( Object array, int index ) {
        ((short[]) array)[ index ] = bad;
    }
}

class UnsignedByteDecoder extends ShortDecoder {
    UnsignedByteDecoder( long[] arraysize ) {
        super( arraysize );
    }
    void decodeStream1( Object array, int index, DataInput strm )
            throws IOException {
        ((short[]) array)[ index ] = 
            (short) ( (short) 0x00ff & (short) strm.readByte() );
    }
}

class IntDecoder extends NumericDecoder {
    private int bad;
    IntDecoder( long[] arraysize ) {
        super( arraysize );
    }
    void setNullValue( String txt ) {
        bad = Integer.parseInt( txt );
    }
    Object getEmptyArray( int size ) {
        return new int[ size ];
    }
    void decodeString1( Object array, int index, String txt ) {
        ((int[]) array)[ index ] = Integer.parseInt( txt );
    }
    void decodeStream1( Object array, int index, DataInput strm )
            throws IOException {
        ((int[]) array)[ index ] = strm.readInt();
    }
    void setBad1( Object array, int index ) {
        ((int[]) array)[ index ] = bad;
    }
}

class LongDecoder extends NumericDecoder {
    private long bad;
    LongDecoder( long[] arraysize ) {
        super( arraysize );
    }
    void setNullValue( String txt ) {
        bad = Long.parseLong( txt );
    }
    Object getEmptyArray( int size ) {
        return new long[ size ];
    }
    void decodeString1( Object array, int index, String txt ) {
        ((long[]) array)[ index ] = Long.parseLong( txt );
    }
    void decodeStream1( Object array, int index, DataInput strm )
            throws IOException {
        ((long[]) array)[ index ] = strm.readLong();
    }
    void setBad1( Object array, int index ) {
        ((long[]) array)[ index ] = bad;
    }
}

class FloatDecoder extends NumericDecoder {
    FloatDecoder( long[] arraysize ) {
        super( arraysize );
    }
    void setNullValue( String txt ) {
        // no action
    }
    Object getEmptyArray( int size ) {
        return new float[ size ];
    }
    void decodeString1( Object array, int index, String txt ) {
        ((float[]) array)[ index ] = Float.parseFloat( txt );
    }
    void decodeStream1( Object array, int index, DataInput strm )
            throws IOException {
        ((float[]) array)[ index ] = strm.readFloat();
    }
    void setBad1( Object array, int index ) {
        ((float[]) array)[ index ] = Float.NaN;
    }
}

class DoubleDecoder extends NumericDecoder {
    DoubleDecoder( long[] arraysize ) {
        super( arraysize );
    }
    void setNullValue( String txt ) {
        // no action
    }
    Object getEmptyArray( int size ) {
        return new double[ size ];
    }
    void decodeString1( Object array, int index, String txt ) {
        ((double[]) array)[ index ] = Double.parseDouble( txt );
    }
    void decodeStream1( Object array, int index, DataInput strm )
            throws IOException {
        ((double[]) array)[ index ] = strm.readDouble();
    }
    void setBad1( Object array, int index ) {
        ((double[]) array)[ index ] = Double.NaN;
    }
}
