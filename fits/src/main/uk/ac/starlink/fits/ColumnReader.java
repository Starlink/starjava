package uk.ac.starlink.fits;

import java.io.DataInput;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nom.tam.fits.FitsException;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.table.Tables;

/**
 * Abstract class defining what needs to be done to read from a
 * stream and return an object representing a value in a given
 * table column for a BINTABLE FITS table.
 *
 * @author  Mark Taylor
 * @since   8 Jul 2008
 */
abstract class ColumnReader {

    private final Class clazz_;
    private final int[] shape_;
    private final int length_;

    /**
     * Constructs a new reader with a given content class, shape and length.
     *
     * @param clazz  the class which <tt>readValue</tt> will return
     * @param shape  the shape to be imposed on the array returned by
     *               <tt>readValue</tt>, or <tt>null</tt> if that
     *               returns a scalar
     * @param length  the number of bytes <tt>readValue</tt> reads from
     *                the stream
     */
    ColumnReader( Class clazz, int[] shape, int length ) {
        clazz_ = clazz;
        shape_ = shape;
        length_ = length;
    }

    /**
     * Constructs a scalar reader with a given content class and length.
     *
     * @param clazz  the class which <tt>readValue</tt> will return
     *               (shouldn't be an array)
     * @param length  the number of bytes <tt>readValue</tt> reads from
     *                the stream
     */
    ColumnReader( Class clazz, int length ) {
        this( clazz, null, length );
    }

    /**
     * Reads bytes from a stream to return an object.
     *
     * @param  stream containing bytes to turn into an object
     * @return  an object read from the stream of type
     *          <tt>getContentClass</tt> (or <tt>null</tt>)
     */
    abstract Object readValue( DataInput stream ) throws IOException;

    /**
     * Returns the class which objects returned by <tt>readValue</tt>
     * will belong to.
     *
     * @return  value class
     */
    Class getContentClass() {
        return clazz_;
    }

    /**
     * Returns the shape imposed on array elements in the sense of
     * ValueInfo.getShape();
     *
     * @param  shape, or null for scalars
     */
    int[] getShape() {
        return shape_;
    }

    /**
     * Returns string size in the sense of ValueInfo.getElementSize().
     *
     * @return element size, or -1 if not applicable
     */
    int getElementSize() {
        return -1;
    }

    /**
     * Returns the number of bytes each call to <tt>readValue</tt> reads
     * from the stream.
     *
     * @return  byte count
     */
    int getLength() {
        return length_;
    }

    /**
     * Constructs a ColumnReader object suitable for reading a given column
     * of a table.
     *
     * @param   tform  TFORM string from FITS header for column
     * @param   scale  factor to scale numerical values by
     * @param   zero   offset to add to numerical values
     * @param   hasBlank  true if a magic value is regarded as blank
     * @param   blank   value represnting magic null value
     *                  (only used if hasBlank is true)
     * @param   tdims  dimensions specified by TDIMS card, or null if none
     *                 given
     * @return  a reader suitable for reading this type of column
     */
    public static ColumnReader createColumnReader( String tform, double scale,
                                                   double zero,
                                                   boolean hasBlank,
                                                   long blank, int[] tdims,
                                                   final long heapStart )
            throws FitsException {

        /* Parse TFORM to find repeat count and data type. */
        Matcher fmatch = Pattern.compile( "([0-9]*)([LXBIJKAEDCMPQ])(.*)" )
                                .matcher( tform );
        if ( ! fmatch.lookingAt() ) {
            throw new FitsException( "Error parsing TFORM value " + tform );
        }
        String scount = fmatch.group( 1 );
        final int count = scount.length() == 0
                  ? 1
                  : Integer.parseInt( scount );
        char type = fmatch.group( 2 ).charAt( 0 );
        String matchA = fmatch.group( 3 ).trim();

        /* Work out a sensible dims array which may or may not be the same
         * as the TDIMS value. */
        int[] dims;
        if ( type == 'P' || type == 'Q' ) {

            /* In most cases, an array of unknown size and shape 
             * is appropriate. */
            if ( tdims == null ) {
                dims = new int[] { -1 };
            }

            /* If a TDIMn shape has been supplied, just pass that on.
             * I don't really see how it's going to make sense, but software
             * downstream might be able to do something with it (and hopefully
             * will not get too confused by the fact that the size of elements
             * does not match their declared size). */
            else {
                dims = tdims;
            }
        }
        else {
            if ( count == 1 ) {
                dims = null;
            }
            else if ( tdims == null ) {
                dims = new int[] { count };
            }
            else {
                int nel = 1;
                for ( int i = 0; i < tdims.length; i++ ) {
                    nel *= tdims[ i ];
                }
                dims = nel == count ? tdims : new int[] { count };
            }
        }

        /* Variable sized array case ('P' or 'Q' descriptors). */
        if ( type == 'P' ) {

            /* If will be doing random access and know the start of the heap
             * we can cope with this. */
            if ( heapStart > 0 ) {
                char vtype = matchA.charAt( 0 );
                final ArrayReader aReader =
                    createArrayReader( vtype, scale, zero,
                                       hasBlank, blank, dims );
                return new ColumnReader( aReader.getContentClass(),
                                         aReader.getShape(), 8 ) {
                    Object readValue( DataInput stream ) throws IOException {
                        int nel = stream.readInt();
                        int offset = stream.readInt();
                        if ( stream instanceof RandomAccess ) {
                            if ( nel > 0 ) {
                                RandomAccess rStream = (RandomAccess) stream;
                                long point = rStream.getFilePointer();
                                rStream.seek( heapStart + offset );
                                Object array = aReader.readArray( stream, nel );
                                rStream.seek( point );
                                return array;
                            }
                            else {
                                return aReader.readArray( stream, 0 );
                            }
                        }
                        else {
                            return null;
                        }
                    }
                    int getElementSize() {
                        return aReader.getElementSize();
                    }
                };
            }

            /* Otherwise we can't do variable sized arrays. */
            else {
                final String value =
                    "(Variable-length arrays not supported " +
                    "in sequential mode)";
                return new ColumnReader( String.class, count * 8 ) {
                    Object readValue( DataInput stream )
                            throws IOException {
                        for ( int i = 0; i < count; i++ ) {
                            int nel = stream.readInt();
                            int offset = stream.readInt();
                        }
                        return value;
                    }
                    int getElementSize() {
                        return value.length();
                    }
                };
            }
        }

        else if ( type == 'Q' ) {
            if ( heapStart > 0 ) {
                char vtype = matchA.charAt( 0 );
                final ArrayReader aReader =
                    createArrayReader( vtype, scale, zero,
                                       hasBlank, blank, dims );
                return new ColumnReader( aReader.getContentClass(),
                                         aReader.getShape(), 16 ) {
                    Object readValue( DataInput stream ) throws IOException {
                        long lnel = stream.readLong();
                        long offset = stream.readLong();
                        if ( stream instanceof RandomAccess ) {
                            int nel = Tables.checkedLongToInt( lnel );
                            if ( nel > 0 ) {
                                RandomAccess rStream = (RandomAccess) stream;
                                long point = rStream.getFilePointer();
                                rStream.seek( heapStart + offset );
                                Object array = aReader.readArray( stream, nel );
                                rStream.seek( point );
                                return array;
                            }
                            else {
                                return aReader.readArray( stream, 0 );
                            }
                        }
                        else {
                            return null;
                        }
                    }
                    int getElementSize() {
                        return aReader.getElementSize();
                    }
                };
            }
            else {
                final String value =
                    "(Variable-length arrays not supported " +
                    "in sequential mode)";
                return new ColumnReader( String.class, count * 16 ) {
                    Object readValue( DataInput stream )
                            throws IOException {
                        for ( int i = 0; i < count; i++ ) {
                            long nel = stream.readLong();
                            long offset = stream.readLong();
                        }
                        return value;
                    }
                    int getElementSize() {
                        return value.length();
                    }
                };
            }
        }

        /* Scalar value case. */
        else if ( count == 1 ) {
            return createScalarColumnReader( type, scale, zero,
                                             hasBlank, blank );
        }

        /* Fixed size array case. */
        else {
            final ArrayReader aReader =
                createArrayReader( type, scale, zero, hasBlank, blank, dims );
            return new ColumnReader( aReader.getContentClass(),
                                     aReader.getShape(),
                                     aReader.getByteCount( count ) ) {
                Object readValue( DataInput stream ) throws IOException {
                    return aReader.readArray( stream, count );
                }
                int getElementSize() {
                    return aReader.getElementSize();
                }
            };
        }
    }

    /** 
     * Returns a new column reader for a scalar column.
     *
     * @param   type  TFORM data type character
     * @param   scale  factor to scale numerical values by
     * @param   zero   offset to add to numerical values
     * @param   hasBlank  true if a magic value is regarded as blank
     * @param   blank   value represnting magic null value
     *                  (only used if hasBlank is true)
     * @return  new column reader
     */
    private static ColumnReader createScalarColumnReader(
            char type, final double scale, final double zero,
            final boolean hasBlank, final long blank ) {
        final boolean isScaled = ( scale != 1.0 || zero != 0.0 );
        final boolean isOffset = ( scale == 1.0 && zero != 0.0 );
        final boolean intOffset = isOffset &&
                                  (double) Math.round( zero ) == zero;
        final ColumnReader reader;
        switch ( type ) {

            /* Logical. */
            case 'L':
                reader = new ColumnReader( Boolean.class, 1 ) {
                    Object readValue( DataInput stream )
                            throws IOException {    
                        switch ( stream.readByte() ) {
                            case (byte) 'T':        
                                return Boolean.TRUE; 
                            case (byte) 'F':
                                return Boolean.FALSE;
                            default:
                                return null;
                        }
                    } 
                };
                return reader;

            /* Unsigned byte - this is a bit fiddly, since a java byte is
             * signed and a FITS byte is unsigned.  We cope with this in
             * general by reading the byte as a short.  However, in the
             * special case that the scaling is exactly right to store
             * a signed byte as an unsigned one (scale=1, zero=-128) we
             * can transform a FITS byte directly into a java one. */
            case 'B':
                final short mask = (short) 0x00ff;
                boolean shortable = intOffset && zero >= Short.MIN_VALUE
                                              && zero < Short.MAX_VALUE - 256;
                final short sZero = (short) zero;
                //  if ( zero == -128.0 && scale == 1.0 ) {
                //      reader = new ColumnReader( Byte.class, 1 ) {
                //          Object readValue( DataInput stream )
                //                  throws IOException {
                //              byte val = stream.readByte();
                //              return ( hasBlank && val == (byte) blank )
                //                          ? null
                //                          : new Byte( (byte)
                //                                      ( val ^ (byte) 0x80 ) );
                //          }
                //      };
                //  }
                if ( shortable ) {
                    reader = new ColumnReader( Short.class, 1 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            byte val = stream.readByte();
                            return ( hasBlank && val == (byte) blank )
                                        ? null
                                        : new Short( (short)
                                                     ( ( val & mask ) +
                                                         sZero ) );
                        }
                    };
                }
                else if ( isScaled ) {
                    reader = new ColumnReader( Float.class, 1 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            byte val = stream.readByte();
                            return ( hasBlank && val == (byte) blank )
                                        ? null
                                        : new Float( ( val & mask )
                                                     * scale + zero );
                        }
                    };
                }
                else {
                    reader = new ColumnReader( Short.class, 1 ) {
                        Object readValue( DataInput stream )  
                                throws IOException {
                            byte val = stream.readByte();
                            return ( hasBlank && val == (byte) blank )
                                        ? null
                                        : new Short( (short)
                                                     ( val & mask ) );
                        }
                    };
                }
                return reader;

            /* Short. */
            case 'I':
                if ( isScaled ) {
                    reader = new ColumnReader( Float.class, 2 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            short val = stream.readShort();
                            return ( hasBlank && val == (short) blank )
                                        ? null
                                        : new Float( (float)
                                                   ( val * scale + zero ) );
                        }
                    };
                }
                else {
                    reader = new ColumnReader( Short.class, 2 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            short val = stream.readShort();
                            return ( hasBlank && val == (short) blank )
                                        ? null
                                        : new Short( val );
                        }
                    };
                }
                return reader;

            /* Integer. */
            case 'J':
                if ( isScaled ) {
                    reader = new ColumnReader( Double.class, 4 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            int val = stream.readInt();
                            return ( hasBlank && val == (int) blank )
                                        ? null
                                        : new Double( val * scale + zero );
                        }
                    };
                }
                else {
                    reader = new ColumnReader( Integer.class, 4 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            int val = stream.readInt();
                            return ( hasBlank && val == (int) blank )
                                        ? null
                                        : new Integer( val );
                        }
                    };
                }
                return reader;

            /* Long. */
            case 'K':
                if ( isScaled ) {
                    reader = new ColumnReader( Double.class, 8 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            long val = stream.readLong();
                            return ( hasBlank && val == (long) blank )
                                        ? null
                                        : new Double( val * scale + zero );
                        }
                    };
                }
                else {
                    reader = new ColumnReader( Long.class, 8 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            long val = stream.readLong();
                            return ( hasBlank && val == (long) blank )
                                        ? null
                                        : new Long( val );
                        }
                    };
                }
                return reader;

            /* Character. */
            case 'A':
                reader = new ColumnReader( Character.class, 1 ) {
                    Object readValue( DataInput stream )
                            throws IOException {
                        char c = (char) ( stream.readByte() & 0xff );
                        return new Character( c );
                    }
                };
                return reader;

            /* Floating point. */
            case 'E':
                if ( isScaled ) {
                    reader = new ColumnReader( Float.class, 4 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            float val = stream.readFloat();
                            return new Float( val * scale + zero );
                        }
                    };
                }
                else {
                    reader = new ColumnReader( Float.class, 4 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            float val = stream.readFloat();
                            return new Float( val );
                        }
                    };
                }
                return reader;    

            /* Double precision. */
            case 'D':
                if ( isScaled ) {
                    reader = new ColumnReader( Double.class, 8 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            double val = stream.readDouble();
                            return new Double( val );
                        }
                    };
                }
                else {
                    reader = new ColumnReader( Double.class, 8 ) {
                        Object readValue( DataInput stream )
                                throws IOException {
                            double val = stream.readDouble();
                            return new Double( val * scale + zero );
                        }
                    };
                }
                return reader;

            /* Complex. */
            case 'C':
            case 'M':
                final int[] complexDims = new int[] { 2 };
                final ArrayReader complexReader =
                    type == 'C'
                        ? createFloatsArrayReader( complexDims, scale, zero )
                        : createDoublesArrayReader( complexDims, scale, zero );
                return new ColumnReader( complexReader.getContentClass(),
                                         complexDims,
                                         complexReader.getByteCount( 2 ) ) {
                    Object readValue( DataInput stream ) throws IOException {
                        return complexReader.readArray( stream, 2 );
                    }
                    int getElementSize() {
                        return complexReader.getElementSize();
                    }
                };

            default:
                throw new AssertionError( "Unknown TFORM type " + type );
        }
    }

    /**
     * Returns a new ArrayReader object for a given data type.
     *
     * @param   type  TFORM data type character
     * @param   scale  factor to scale numerical values by
     * @param   zero   offset to add to numerical values
     * @param   hasBlank  true if a magic value is regarded as blank
     * @param   blank   value represnting magic null value
     *                  (only used if hasBlank is true)
     * @param   dims    apparent dimensions of FITS array 
     *                  (not necessarily same as dimensions of read data)
     * @return  new array reader
     */
    private static ArrayReader createArrayReader( char type, final double scale,
                                                  final double zero,
                                                  final boolean hasBlank,
                                                  final long blank,
                                                  final int[] dims ) {
        final boolean isScaled = ( scale != 1.0 || zero != 0.0 );
        final boolean isOffset = ( scale == 1.0 && zero != 0.0 );
        final boolean intOffset = isOffset &&
                                  (double) Math.round( zero ) == zero;
        final ArrayReader reader;
        switch ( type ) {

            /* Logical. */
            case 'L':
                reader = new ArrayReader( boolean[].class, dims, 1 ) {
                    Object readArray( DataInput stream, int count )
                            throws IOException {
                        boolean[] value = new boolean[ count ];
                        for ( int i = 0; i < count; i++ ) {
                            value[ i ] = stream.readByte() == (byte) 'T';
                        }
                        return value;
                    }
                };
                return reader;

            /* Bits. */
            case 'X':
                reader = new ArrayReader( boolean[].class, dims, -1 ) {
                    Object readArray( DataInput stream, int count )
                            throws IOException {
                        boolean[] value = new boolean[ count ];
                        int ibit = 0;
                        int b = 0;
                        for ( int i = 0; i < count; i++ ) {
                            if ( ibit == 0 ) {
                                ibit = 8;
                                b = stream.readByte();
                            }
                            value[ i ] = ( b & 0x01 ) != 0;
                            b = b >>> 1;
                            ibit--;
                        }
                        return value;
                    }
                    int getByteCount( int nel ) {
                        return ( nel + 7 ) / 8;
                    }
                };
                return reader;

            /* Unsigned byte - this is a bit fiddly, since a java byte is
             * signed and a FITS byte is unsigned.  We cope with this in
             * general by reading the byte as a short.  However, in the
             * special case that the scaling is exactly right to store
             * a signed byte as an unsigned one (scale=1, zero=-128) we
             * can transform a FITS byte directly into a java one. */
            case 'B':
                final short mask = (short) 0x00ff;
                boolean shortable = intOffset && zero >= Short.MIN_VALUE
                                              && zero < Short.MAX_VALUE - 256;
                final short sZero = (short) zero;
                //  if ( zero == -128.0 && scale == 1.0 ) {
                //      reader = new ColumnReader( byte[].class, dims,
                //                                 1 * count ) {
                //          Object readValue( DataInput stream )
                //                  throws IOException {
                //              byte[] value = new byte[ count ];
                //              for ( int i = 0; i < count; i++ ) {
                //                  byte val = stream.readByte();
                //                  value[ i ] = (byte) ( val ^ (byte) 0x80 );
                //              }
                //              return value;
                //          }
                //      };
                //  }
                if ( shortable ) {
                    reader = new ArrayReader( short[].class, dims, 1 ) {
                        Object readArray( DataInput stream, int count )
                                throws IOException {
                            short[] value = new short[ count ];
                            for ( int i = 0; i < count; i++ ) {
                                byte val = stream.readByte();
                                value[ i ] = (short) ( ( val & mask ) + sZero );
                            }
                            return value;
                        }
                    };
                }
                else if ( isScaled ) {
                    reader = new ArrayReader( float[].class, dims, 1 ) {
                        Object readArray( DataInput stream, int count )
                                throws IOException {
                            double[] value = new double[ count ];
                            for ( int i = 0; i < count; i++ ) {
                                byte val = stream.readByte();
                                value[ i ] =
                                    ( hasBlank && val == (byte) blank )
                                         ? Float.NaN
                                         : (float) ( ( val & mask )
                                                     * scale + zero );
                            }
                            return value;
                        }
                    };
                }
                else {
                    reader = new ArrayReader( short[].class, dims, 1 ) {
                        Object readArray( DataInput stream, int count )
                                throws IOException {
                            short[] value = new short[ count ];
                            for ( int i = 0; i < count; i++ ) {
                                byte val = stream.readByte();
                                value[ i ] = (short) ( val & mask );
                            }
                            return value;
                        }
                    };
                }
                return reader;

            /* Short. */
            case 'I':
                if ( isScaled ) {
                    reader = new ArrayReader( float[].class, dims, 2 ) {
                        Object readArray( DataInput stream, int count )
                                throws IOException {
                            double[] value = new double[ count ];
                            for ( int i = 0; i < count; i++ ) {
                                short val = stream.readShort();
                                value[ i ] =
                                    ( hasBlank && val == (short) blank )
                                         ? Float.NaN
                                         : (float) ( val * scale + zero );
                            }
                            return value;
                        }
                    };
                }
                else {
                    reader = new ArrayReader( short[].class, dims, 2 ) {
                        Object readArray( DataInput stream, int count )
                                throws IOException {
                            short[] value = new short[ count ];
                            for ( int i = 0; i < count; i++ ) {
                                short val = stream.readShort();
                                value[ i ] = val;
                            }
                            return value;
                        }
                    };
                }
                return reader;

            /* Integer. */
            case 'J':
                if ( isScaled ) {
                    reader = new ArrayReader( double[].class, dims, 4 ) {
                        Object readArray( DataInput stream, int count )
                                throws IOException {
                            double[] value = new double[ count ];
                            for ( int i = 0; i < count; i++ ) {
                                int val = stream.readInt();
                                value[ i ] =
                                    ( hasBlank && val == (int) blank )
                                         ? Double.NaN
                                         : val * scale + zero;
                            }
                            return value;
                        }
                    };
                }
                else {
                    reader = new ArrayReader( int[].class, dims, 4 ) {
                        Object readArray( DataInput stream, int count )
                                throws IOException {
                            int[] value = new int[ count ];
                            for ( int i = 0; i < count; i++ ) {
                                int val = stream.readInt();
                                // can't do anything with a blank
                                value[ i ] = val;
                            }
                            return value;
                        }
                    };
                }
                return reader;

            /* Long. */
            case 'K':
                if ( isScaled ) {
                    reader = new ArrayReader( double[].class, dims, 8 ) {
                        Object readArray( DataInput stream, int count )
                                throws IOException {
                            double[] value = new double[ count ];
                            for ( int i = 0; i < count; i++ ) {
                                long val = stream.readLong();
                                value[ i ] =
                                    ( hasBlank && val == (long) blank )
                                         ? Double.NaN
                                         : val * scale + zero;
                            }
                            return value;
                        }
                    };
                }
                else {
                    reader = new ArrayReader( long[].class, dims, 8 ) {
                        Object readArray( DataInput stream, int count )
                                throws IOException {
                            long[] value = new long[ count ];
                            for ( int i = 0; i < count; i++ ) {
                                long val = stream.readLong();
                                // can't do anything with a blank
                                value[ i ] = val;
                            }
                            return value;
                        }
                    };
                }
                return reader;

            /* Characters. */
            case 'A':
                if ( dims.length == 1 ) {
                    reader = new ArrayReader( String.class, null, 1 ) {
                        Object readArray( DataInput stream, int count )
                                throws IOException {
                            return readString( stream, count );
                        }
                        int getElementSize() {
                            return dims[ 0 ];
                        }
                    };
                }
                else {
                    int nel = 1;
                    for ( int i = 1; i < dims.length; i++ ) {
                        nel *= dims[ i ];
                    }
                    final int stringLength = dims[ 0 ];
                    final int nString = nel;
                    int[] shape = new int[ dims.length - 1 ];
                    System.arraycopy( dims, 1, shape, 0, dims.length - 1 );
                    reader = new ArrayReader( String[].class, shape, 1 ) {
                        Object readArray( DataInput stream, int count )
                                throws IOException {
                            int nString =
                                ( count + stringLength - 1 ) / stringLength;
                            String[] value = new String[ nString ];
                            for ( int i = 0; i < nString; i++ ) {
                                int nchar = Math.min( count, stringLength );
                                value[ i ] = readString( stream, nchar );
                                count -= nchar;
                            }
                            assert count == 0;
                            return value;
                        }
                        int getElementSize() {
                            return stringLength;
                        }
                    };
                }
                return reader;

            /* Floating point. */
            case 'E':
                return createFloatsArrayReader( dims, scale, zero );

            /* Double precision. */
            case 'D':
                return createDoublesArrayReader( dims, scale, zero );

            /* Single precision complex. */
            case 'C':
                return createFloatsArrayReader( complexShape( dims ),
                                                scale, zero );

            /* Double precision complex. */
            case 'M':
                return createDoublesArrayReader( complexShape( dims ),
                                                 scale, zero );

            /* No known TFORM type. */
            default:
                throw new AssertionError( "Unknown TFORM type " + type );
        }
    }

    /**
     * Returns a new array reader for reading single precision floating point
     * values.
     *
     * @param  shape  shape of the array to read
     * @param   scale  factor to scale numerical values by
     * @param   zero   offset to add to numerical values
     * @return  new array reader
     */
    private static ArrayReader createFloatsArrayReader( int[] shape,
                                                        final double scale,
                                                        final double zero ) {
        final boolean isScaled = scale != 1.0 || zero != 0.0;
        if ( isScaled ) {
            return new ArrayReader( float[].class, shape, 4 ) {
                Object readArray( DataInput stream, int count ) 
                        throws IOException {
                    float[] value = new float[ count ];
                    for ( int i = 0; i < count; i++ ) {
                        float val = stream.readFloat();
                        value[ i ] = (float) ( val * scale + zero );
                    }
                    return value;
                }
            };
        }
        else {
            return new ArrayReader( float[].class, shape, 4 ) {
                Object readArray( DataInput stream, int count )
                        throws IOException {
                    float[] value = new float[ count ];
                    for ( int i = 0; i < count; i++ ) {
                        value[ i ] = stream.readFloat();
                    }
                    return value;
                }
            };
        }
    }

    /**
     * Returns a new array reader for reading double precision floating point
     * values.
     *
     * @param  shape  shape of the array to read
     * @param   scale  factor to scale numerical values by
     * @param   zero   offset to add to numerical values
     * @return  new array reader
     */
    private static ArrayReader createDoublesArrayReader( int[] shape,
                                                         final double scale,
                                                         final double zero ) {
        final boolean isScaled = scale != 1.0 || zero != 0.0;
        if ( isScaled ) {
            return new ArrayReader( double[].class, shape, 8 ) {
                Object readArray( DataInput stream, int count )
                        throws IOException {
                    double[] value = new double[ count ];
                    for ( int i = 0; i < count; i++ ) {
                        double val = stream.readDouble();
                        value[ i ] = val * scale + zero;
                    }
                    return value;
                }
            };
        }
        else {
            return new ArrayReader( double[].class, shape, 8 ) {
                Object readArray( DataInput stream, int count )
                        throws IOException {
                    double[] value = new double[ count ];
                    for ( int i = 0; i < count; i++ ) {
                        value[ i ] = stream.readDouble();
                    }
                    return value;
                }
            };
        }
    }

    /**
     * Reads a string from a data stream.
     * A fixed number of bytes are read from the stream, but the returned
     * object is a variable-length string with trailing spaces omitted.
     * If it's all spaces, <tt>null</tt> is returned.
     *
     * @param  stream  the stream to read from
     * @param  count  number of bytes to read from the stream
     * @return  string read
     */
    private static String readString( DataInput stream, int count )
            throws IOException {
        char[] letters = new char[ count ];
        int last = -1;
        boolean end = false;
        for ( int i = 0; i < count; i++ ) {
            char letter = (char) ( stream.readByte() & 0xff );
            if ( letter == 0 ) {
                end = true;
            }
            if ( ! end ) {
                letters[ i ] = letter;
                if ( letter != ' ' ) {
                    last = i;
                }
            }
        }
        int leng = last + 1;
        return leng == 0 ? null
                         : new String( letters, 0, leng );
    }

    /**
     * Returns a dimensions array based on a given one, but with an extra
     * dimension of extent 2 prepended to the list.
     *
     * @param  dims  intial dimensions array (<tt>null</tt> is interpreted
     *               as a zero-dimensional array
     * @return  like <tt>dims</tt> but with a 2 at the start
     */
    private static int[] complexShape( int[] dims ) {
        if ( dims == null ) {
            return new int[] { 2 };
        }
        else {
            int[] shape = new int[ dims.length + 1 ];
            shape[ 0 ] = 2;
            System.arraycopy( dims, 0, shape, 1, dims.length );
            return shape;
        }
    }

    /**
     * Abstract class defining an object which can read an array of similarly
     * typed values from a stream.
     */
    private static abstract class ArrayReader {

        private final Class clazz_;
        private final int[] shape_;
        private final int elBytes_;

        /**
         * Constructor.
         *
         * @param   clazz  class of values read by this reader
         * @param   shape  shape of values read by this reader
         * @param   elBytes  number of bytes read from stream for each element
         */
        ArrayReader( Class clazz, int[] shape, int elBytes ) {
            clazz_ = clazz;
            shape_ = shape;
            elBytes_ = elBytes;
        }

        /**
         * Reads an array from the current position in a stream.
         *
         * @param  stream   stream to read from
         * @param  count   number of items to read
         */
        abstract Object readArray( DataInput stream, int count )
                throws IOException;

        /** 
         * Returns the class of objects returned from readArray.
         *
         * @return  content class
         */
        Class getContentClass() {
            return clazz_;
        }

        /**
         * Returns the shape of returned values in the sense of 
         * ValueInfo.getShape();
         *
         * @param  shape, or null for scalars
         */
        int[] getShape() {
            return shape_;
        }

        /**
         * Returns string size in the sense of ValueInfo.getElementSize().
         *
         * @return element size, or -1 if not applicable
         */
        int getElementSize() {
            return -1;
        }

        /**
         * Returns the number of bytes read from the input stream 
         * when <code>count</code> elements are read.
         *
         * @param  count  number of elements read from array
         * @return  number of bytes read
         */
        int getByteCount( int count ) {
            return elBytes_ * count;
        }
    }
}
