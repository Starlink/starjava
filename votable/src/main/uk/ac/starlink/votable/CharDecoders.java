package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import uk.ac.starlink.table.Tables;

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
     * <p>This currently reads on the assumption of UTF-8 encoding.
     * At VOTable 1.5 and earlier, only 7-bit ASCII characters are permitted,
     * but UTF-8 may be illegally present anyway, so it's probably reasonable
     * to interpret it as such.
     *
     * @param  arraysize  array representing value dimensions - last element
     *         may be -1 to indicate unknown
     * @return  decoder for <code>arraysize</code>-sized array
     *          of <code>char</code>s
     */
    public static Decoder makeCharDecoder( long[] arraysize ) {
        return makeDecoder( arraysize, CharReader.UTF8 );
    }

    /**
     * Returns a decoder for a FIELD/PARAM with a declared
     * datatype attribute of 'unicodeChar' and declared arraysize attribute
     * as per a given dimensions array.
     *
     * <p>This type is effectively UTF-16 with a restriction to BMP-only
     * characters.  If non-BMP characters are present, we cope with them
     * anyway.
     *
     * @param  arrraysize  array representing value dimensions - last element
     *         may be -1 to indicate unknown
     * @return  decoder for <code>arraysize</code>-sized array of 
     *          <code>unicodeChar</code>s
     */
    public static Decoder makeUnicodeCharDecoder( long[] arraysize ) {
        return makeDecoder( arraysize, CharReader.UTF16 );
    }

    /**
     * Helper interface defining how to get a <code>char</code> from a stream.
     */
    private static abstract class CharReader {

        public final int elSize_;

        /** Reader implementation for UTF-8 input. */
        public static final CharReader UTF8 = new CharReader( 1 ) {
            public char readSingleCharFromStream( DataInput in )
                    throws IOException {
                byte b = in.readByte();
                return b >= 0 ? (char) b : '?';
            }
            public String decodeString( byte[] buf, int offset, int leng ) {
                return new String( buf, offset, leng, StandardCharsets.UTF_8 );
            }
            public byte[] toBytes( String txt ) {
                return txt.getBytes( StandardCharsets.UTF_8 );
            }
        };

        /** Reader implementation for UTF-16 input. */
        public static final CharReader UTF16 = new CharReader( 2 ) {
            public char readSingleCharFromStream( DataInput in )
                    throws IOException {
                return in.readChar();
            }
            public String decodeString( byte[] buf, int offset, int leng ) {
                return new String( buf, offset, leng,
                                   StandardCharsets.UTF_16BE );
            }
            public byte[] toBytes( String txt ) {
                return txt.getBytes( StandardCharsets.UTF_16BE );
            }
        };

        /**
         * Constructor.
         *
         * @param  elSize  number of bytes per input element
         */
        CharReader( int elSize ) {
            elSize_ = elSize;
        }

        /**
         * Reads a single character from a stream.
         * This is not used as part of a sequence of reads to build
         * up a string.
         *
         * @param   in   input stream
         * @return   single character
         */
        abstract char readSingleCharFromStream( DataInput in )
                throws IOException;

        /**
         * Decodes a byte sequence into a string.
         *
         * @param  buf  byte buffer
         * @param  offset   start of byte sequence in buffer
         * @param  leng   length of byte sequence in buffer
         * @return  output string
         */
        abstract String decodeString( byte[] buf, int offset, int leng );

        /**
         * Unpacks a string read from VOTable XML into a byte buffer.
         *
         * @param  txt  text
         * @return  byte buffer with text encoding
         */
        abstract byte[] toBytes( String txt );
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
                public String decodeStream( DataInput strm )
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
     * Takes a string value and trims it by truncating it at the first NUL,
     * then stripping any trailing spaces.
     *
     * @param  txt  input string
     * @return   packed string
     */
    private static String packString( CharSequence txt ) {
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
                            : "";
    }

    /**
     * Decoder subclass for reading single character values.
     */
    private static class ScalarCharDecoder extends Decoder {
        final CharReader cread_;

        ScalarCharDecoder( CharReader cread ) {
            super( Character.class, SCALAR_SIZE );
            cread_ = cread;
        }

        public Character decodeString( String txt ) {
            return Character.valueOf( txt.length() > 0 ? txt.charAt( 0 )
                                                       : '\0' );
        }

        public Character decodeStream( DataInput strm ) throws IOException {
            assert getNumItems( strm ) == 1;
            return Character.valueOf( cread_.readSingleCharFromStream( strm ) );
        }

        public void skipStream( DataInput strm ) throws IOException {
            assert getNumItems( strm ) == 1;
            skipBytes( strm, cread_.elSize_ );
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
        final CharReader cread_;

        ScalarStringDecoder( long[] arraysize, CharReader cread ) {
            super( String.class, arraysize );
            cread_ = cread;
        }

        public long[] getDecodedShape() {
            return SCALAR_SIZE;
        }

        public int getElementSize() {
            return (int) arraysize[ 0 ];
        }

        public String decodeString( String txt ) {
            return txt;
        }

        public String decodeStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            byte[] buf = new byte[ num * cread_.elSize_ ];
            strm.readFully( buf );
            return packString( cread_.decodeString( buf, 0, buf.length ) );
        }

        public void skipStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            skipBytes( strm, num * cread_.elSize_ );
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
        final CharReader cread_;
        final long[] decodedShape_;
        final boolean isVariable_;
        final int fixedSize_;

        StringDecoder( long[] arraysize, CharReader cread ) {
            super( String[].class, arraysize );
            cread_ = cread;
            int ndim = arraysize.length;
            decodedShape_ = new long[ ndim - 1 ];
            System.arraycopy( arraysize, 1, decodedShape_, 0, ndim - 1 );
            isVariable_ = arraysize[ ndim - 1 ] < 0;
            if ( ! isVariable_ ) {
                long size = 1;
                for ( long dim : arraysize ) {
                    size *= dim;
                }
                fixedSize_ = Tables.checkedLongToInt( size );
            }
            else {
                fixedSize_ = -1;
            }
        }

        public long[] getDecodedShape() {
            return decodedShape_;
        }

        public int getElementSize() {
            return (int) arraysize[ 0 ];
        }

        public int getNumItems( DataInput strm ) throws IOException {
            return isVariable_ ? super.getNumItems( strm ) : fixedSize_;
        }

        public Object decodeString( String txt ) {
            return extractStrings( cread_.toBytes( txt ) );
        }

        public Object decodeStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            byte[] buf = new byte[ num * cread_.elSize_ ];
            strm.readFully( buf );
            return extractStrings( buf );
        }

        public void skipStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            skipBytes( strm, cread_.elSize_ * num );
        }
        
        /**
         * Unpacks a byte buffer containing characters for the string array
         * into a 1-d array with an element for each string scalar.
         *
         * @param  buf  buffer containing byte content for string array
         */
        private String[] extractStrings( byte[] buf ) {
            int ntok = buf.length / cread_.elSize_;
            int ncell = numCells( ntok );
            int sleng = (int) arraysize[ 0 ];
            int nstr = ncell / sleng;
            String[] result = new String[ nstr ];
            for ( int istr = 0; istr < nstr; istr++ ) {
                int leng = sleng * cread_.elSize_;
                int ioff = istr * leng;
                if ( ioff < buf.length ) {
                    String txt =
                        cread_.decodeString( buf, ioff,
                                             Math.min( leng,
                                                       buf.length - ioff ) );
                    result[ istr ] = packString( txt );
                }
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
