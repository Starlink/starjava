package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.IOException;

/**
 * Utility class with methods that can supply Decoders for reading
 * characters, strings and string arrays.
 *
 * @author   Mark Taylor (Starlink)
 */
abstract class CharDecoders {

    /**
     * Returns a decoder for a FIELD/PARAM with a declared 
     * datatype attribute of 'char' and declared arraysize attribute
     * as per a given dimensions array.
     *
     * @param  arrraysize  array representing value dimensions - last element
     *         may be -1 to indicate unknown
     * @return  decoder for <code>arraysize</code>-sized array
     *          of <code>char</code>s
     */
    public static Decoder makeCharDecoder( long[] arraysize ) {
        CharReader cread = new CharReader() {
            public char readCharFromStream( DataInput strm )
                    throws IOException {
                return (char) ( strm.readByte() & 0x00ff );
            }
            public int getCharSize() {
                return 1;
            }
        };
        return makeDecoder( arraysize, cread );
    }

    /**
     * Returns a decoder for a FIELD/PARAM with a declared
     * datatype attribute of 'unicodeChar' and declared arraysize attribute
     * as per a given dimensions array.
     *
     * @param  arrraysize  array representing value dimensions - last element
     *         may be -1 to indicate unknown
     * @return  decoder for <code>arraysize</code>-sized array of 
     *          <code>unicodeChar</code>s
     */
    public static Decoder makeUnicodeCharDecoder( long[] arraysize ) {
        CharReader cread = new CharReader() {
            public char readCharFromStream( DataInput strm )
                    throws IOException {
                return strm.readChar();
            }
            public int getCharSize() {
                return 2;
            }
        };
        return makeDecoder( arraysize, cread );
    }

    /**
     * Helper interface defining how to get a <code>char</code> from a stream.
     */
    private static interface CharReader {

        /**
         * Reads a character from a stream.
         *
         * @param   strm   input stream
         * @return   single character read from <code>strm</code>
         */
        char readCharFromStream( DataInput strm ) throws IOException;

        /**
         * Returns the number of bytes read for a single character
         * (a single call of {@link #readCharFromStream}).
         *
         * @return  byte count per character
         */
        int getCharSize();
    }


    /**
     * Returns a decoder for a character-type FIELD/PARAM with a given
     * arraysize and way of getting characters from a stream.
     * 
     * @param  arraysize  array representing value dimensions
     * @param  cread   character reader
     * @return  decoder 
     */
    private static Decoder makeDecoder( long[] arraysize, CharReader cread ) {
        int ndim = arraysize.length;

        /* Single character decoder. */
        if ( ndim == 0 || ndim == 1 && arraysize[ 0 ] == 1 ) {
            return new ScalarCharDecoder( cread );
        }

        /* If we have an assumed arraysize (non-strict VOTable parsing)
         * behave as if it's a variable-length array, except in the case
         * where we're decoding from a stream.  Attempting that would
         * probably be disastrous, since it would likely attempt to read
         * a character array a random number of bytes long, and fail wth
         * an OutOfMemoryError. */
        else if ( ndim == 1 && 
                  arraysize[ 0 ] == FieldElement.ASSUMED_ARRAYSIZE ) {
            return new ScalarStringDecoder( arraysize, cread ) {
                public Object decodeStream( DataInput strm )
                        throws IOException {
                    throw new RuntimeException( 
                        "Refuse to decode assumed char arraysize - try -D" +
                        VOElementFactory.STRICT_PROPERTY + "=true" );
                }
                public void skipStream( DataInput strm ) throws IOException {
                    decodeStream( strm );
                }
            };
        }

        /* Character vector (string) decoder. */
        else if ( ndim == 1 ) {
            return new ScalarStringDecoder( arraysize, cread );
        }

        /* String array decoder. */
        else {
            return new StringDecoder( arraysize, cread );
        }
    }

    /**
     * Decoder subclass for reading single character values.
     */
    private static class ScalarCharDecoder extends Decoder {
        final CharReader cread;

        ScalarCharDecoder( CharReader cread ) {
            super( Character.class, SCALAR_SIZE );
            this.cread = cread;
        }

        public Object decodeString( String txt ) {
            return new Character( txt.length() > 0 ? txt.charAt( 0 ) : '\0' );
        }

        public Object decodeStream( DataInput strm ) throws IOException {
            assert getNumItems( strm ) == 1;
            return new Character( cread.readCharFromStream( strm ) );
        }

        public void skipStream( DataInput strm ) throws IOException {
            assert getNumItems( strm ) == 1;
            skipBytes( strm, cread.getCharSize() );
        }

        public boolean isNull( Object array, int index ) {
            return false;
        }

        public void setNullValue( String txt ) {
        }
    }

    /**
     * Decoder subclass for reading single string (= 1-d character array)
     * values.
     */
    private static class ScalarStringDecoder extends Decoder {
        final CharReader cread; 

        ScalarStringDecoder( long[] arraysize, CharReader cread ) {
            super( String.class, arraysize );
            this.cread = cread;
        }

        public long[] getDecodedShape() {
            return SCALAR_SIZE;
        }

        public int getElementSize() {
            return (int) arraysize[ 0 ];
        }

        public Object decodeString( String txt ) {
            return txt;
        }

        public Object decodeStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            StringBuffer data = new StringBuffer( num );
            int i = 0;
            while ( i < num ) {
                char c = cread.readCharFromStream( strm );
                i++;
                if ( c == '\0' ) {
                    break;
                }
                data.append( c );
            }
            for ( ; i < num; i++ ) {
                cread.readCharFromStream( strm );
            }
            return new String( data );
        }

        public void skipStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            skipBytes( strm, num * cread.getCharSize() );
        }

        String makeString( CharSequence txt ) {
            int leng = 0;
            int sleng = txt.length();
            char[] buf = new char[ sleng ];
            for ( int i = 0; i < sleng; i++ ) {
                char c = txt.charAt( i );
                if ( c == '\0' ) {
                    break;
                }
                buf[ leng++ ] = c;
            }
            while ( leng > 0 && buf[ leng - 1 ] == ' ' ) {
                leng--;
            }
            return ( leng > 0 ) ? new String( buf, 0, leng )
                                : null;
        }

        public boolean isNull( Object array, int index ) {
            return false;
        }
        public void setNullValue( String txt ) {
        }
    }

    /**
     * Decoder subclass for reading arrays of strings (= multiple-dimensional
     * character array)l
     */
    private static class StringDecoder extends Decoder {
        final CharReader cread;
        final long[] decodedShape;
        final boolean isVariable;
        int fixedSize;

        StringDecoder( long[] arraysize, CharReader cread ) {
            super( String[].class, arraysize );
            this.cread = cread;
            int ndim = arraysize.length;
            decodedShape = new long[ ndim - 1 ];
            System.arraycopy( arraysize, 1, decodedShape, 0, ndim - 1 );
            isVariable = arraysize[ ndim - 1 ] < 0;
            if ( ! isVariable ) {
                fixedSize = 1;
                for ( int i = 0; i < arraysize.length; i++ ) {
                    fixedSize *= arraysize[ i ];
                }
            }
        }

        public long[] getDecodedShape() {
            return decodedShape;
        }

        public int getElementSize() {
            return (int) arraysize[ 0 ];
        }

        public int getNumItems( DataInput strm ) throws IOException {
            return isVariable ? super.getNumItems( strm ) : fixedSize;
        }

        public Object decodeString( String txt ) {
            return makeStrings( txt );
        }

        public Object decodeStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            StringBuffer sbuf = new StringBuffer( num );
            for ( int i = 0; i < num; i++ ) {
                sbuf.append( cread.readCharFromStream( strm ) );
            }
            return makeStrings( sbuf );
        }

        public void skipStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            skipBytes( strm, cread.getCharSize() * num );
        }
        
        public String[] makeStrings( CharSequence txt ) {
            int ntok = txt.length();
            int ncell = numCells( ntok );
            int sleng = (int) arraysize[ 0 ];
            int nstr = ncell / sleng;
            String[] result = new String[ nstr ];
            int k = 0;
            char[] buf = new char[ sleng ];
            for ( int i = 0; i < nstr && k < ntok; i++ ) {
                int leng = 0;
                while ( leng < sleng && k < ntok ) {
                    char c = txt.charAt( k++ );
                    if ( c == '\0' ) {
                        break;
                    }
                    buf[ leng++ ] = c;
                }
                while ( leng > 0 && buf[ leng - 1 ] == ' ' ) {
                    leng--;
                }
                if ( leng > 0 ) {
                    result[ i ] = new String( buf, 0, leng );
                }
                k = ( i + 1 ) * sleng;
            }
            return result;
        }

        public boolean isNull( Object array, int index ) {
            return false;
        }

        public void setNullValue( String txt ) {
        }
    }
}
