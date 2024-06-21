package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.util.IOUtils;

/**
 * Decoder object associated with a Field.
 * Instances of this class know about the size and shape of fields
 * as well as their numeric type, and can decode various data sources
 * into the objects they represent.  To construct a decoder use the
 * static {@link #makeDecoder} method.
 *
 * <p>The various <code>decode</code> methods turn some kind of
 * representation of
 * the given object into a standard representation.  The standard
 * representation is in accordance with the recommendations made in
 * the the {@link uk.ac.starlink.table} package.
 *
 * @author  Mark Taylor (Starlink)
 */
abstract class Decoder {

    static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );
    static final long[] SCALAR_SIZE = new long[ 0 ];

    protected String blankString;
    protected boolean isVariable;
    protected int sliceSize;
    protected long[] arraysize;
    private final Class<?> clazz;

    /**
     * Sets the null (bad) value to be used by the decoder from a string.
     *
     * @param  txt  a string representation of the bad value
     * @throws IllegalArgumentException  if the string cannot be parsed
     */
    abstract void setNullValue( String txt );

    /**
     * Returns an object array based on the given text (space-separated
     * values for numeric types, normal string values for characters).
     *
     * @param   txt  a string encoding one or many values
     * @return  an object containing the decoded values
     */
    abstract public Object decodeString( String txt );

    /**
     * Returns an object array read from the next bit of a given input
     * stream as raw bytes.  The VOTable BINARY/BINARY2 format is used.
     *
     * @param  strm  a DataInput object from which bytes are to be read
     */
    abstract public Object decodeStream( DataInput strm ) throws IOException;

    /**
     * Skips over the bytes in a stream corresponding to a single cell.
     * The effect is the same as calling {@link #decodeStream}, but no
     * data is returned.
     *
     * @param  strm  a DataInput object from which bytes are to be read
     */
    abstract public void skipStream( DataInput strm ) throws IOException;

    /**
     * Indicates whether an element of a given array matches the Null value
     * used by this decoder.
     *
     * @param  array  the array in which the element to check is
     * @param  index  the index into <code>array</code> at which the element to
     *         check is
     * @return <code>true</code> iff the <code>index</code>'th element
     *         of <code>array</code> matches the Null value for this decoder
     */
    abstract public boolean isNull( Object array, int index );

    /**
     * Does required setup for a decoder given its shape.
     *
     * @param  clazz  the class to which all objects returned by the
     *         <code>decode*</code> methods will belong
     * @param  arraysize  the dimensions of objects with this type -
     *         the last element of the array may be negative to
     *         indicate unknown slowest-varying dimension
     */
    protected Decoder( Class<?> clazz, long[] arraysize ) {
        this.clazz = clazz;
        this.arraysize = arraysize;
        int ndim = arraysize.length;
        if ( ndim == 0 ) {
            isVariable = false;
            sliceSize = 1;
        }
        else if ( arraysize[ ndim - 1 ] < 0 ) {
            isVariable = true;
            long ss = 1;
            for ( int i = 0; i < ndim - 1; i++ ) {
                ss *= arraysize[ i ];
            }
            sliceSize = (int) ss;
        }
        else {
            isVariable = false;
            long ss = 1;
            for ( int i = 0; i < ndim; i++ ) {
                ss *= arraysize[ i ];
            }
            sliceSize = (int) ss;
        }
    }

    /**
     * Returns the class for objects returned by this decoder.
     * Objects returned by the <code>decode*</code> methods of this decoder
     * will be instances of the class returned by this method,
     * or <code>null</code>.
     *
     * @param  returned object class
     */
    public Class<?> getContentClass() {
        return clazz;
    }

    /**
     * Returns the number of cells to use for this Decoder given a
     * certain number of available tokens.  This is just the arraysize
     * if it is fixed, or some large-enough multiple of the slice size
     * if it is variable.
     *
     * @param  ntok  the number of available tokens
     * @return  the number of cells to fill
     */
    int numCells( int ntok ) {
        if ( isVariable ) {
            return ( ( ntok + sliceSize - 1 ) / sliceSize ) * sliceSize;
        }
        else {
            return sliceSize;
        }
    }

    /**
     * Work out how many items are to be read from the supplied input stream.
     * This may be a fixed number for this Decoder, or it may require
     * reading a count from the stream.
     */
    int getNumItems( DataInput strm ) throws IOException {
        return isVariable ? strm.readInt() : sliceSize;
    }

    /**
     * Gets the shape of items returned by this decoder.  By default this
     * is the same as the <code>arraysize</code>, but decoders may
     * change the shape from that defined by the <code>arraysize</code>
     * attribute of the FIELD element.
     * In particular, the <code>char</code> and <code>unicodeChar</code>
     * decoders package an array of characters as a String.
     *
     * @return  the shape of objects returned by this decoder.
     *          The last element might be negative to indicate variable size
     */
    public long[] getDecodedShape() {
        return arraysize;
    }

    /**
     * Gets the 'element size' of items returned by this decoder.
     * This has the same meaning as
     * {@link uk.ac.starlink.table.ValueInfo#getElementSize};
     * the Decoder implementation returns -1, but character-type decoders
     * override this.
     *
     * @return   notional size of each element an array of values decoded
     *           by this object, or -1 if unknown
     */
    public int getElementSize() {
        return -1;
    }

    /**
     * Skips over a given number of bytes in a stream without reading them.
     *
     * @param   strm  stream
     * @return  byte count
     */
    static void skipBytes( DataInput strm, long num ) throws IOException {
        IOUtils.skipBytes( strm, num );
    }

    /**
     * Create a decoder given its datatype, shape and blank (bad) value.
     * The shape is specified by the <code>arraysize</code> parameter,
     * which gives array dimensions.   The last element of this array
     * may be negative to indicate an unknown last (slowest varying)
     * dimension.
     *
     * <p>All the decoders named in the VOTable 1.0 document are supported:
     * <ul>
     * <li>boolean
     * <li>bit
     * <li>unsignedByte
     * <li>short
     * <li>int
     * <li>long
     * <li>char
     * <li>unicodeChar
     * <li>float
     * <li>double
     * <li>floatComplex
     * <li>doubleComplex
     * </ul>
     *
     * @param  datatype  the datatype name, that is the value of the
     *         VOTable "datatype" attribute
     * @param  arraysize  shape of the array
     * @param  blank  a string giving the bad value
     * @return a Decoder object capable of decoding values according to
     *         its name and shape
     */
    public static Decoder makeDecoder( String datatype, long[] arraysize,
                                       String blank ) {

        /* Work out if we have an effectively scalar quantity (either an
         * actual scalar or an array with one element. */
        boolean isScalar;
        int ndim = arraysize.length;
        if (ndim == 0) {
            isScalar = true;
        }
        else if ( arraysize[ ndim - 1 ] > 0 ) {
            int nel = 1;
            for ( int i = 0; i < ndim; i++ ) {
                nel *= arraysize[ i ];
            }
            isScalar = nel == 1;
        }
        else {
            isScalar = false;
        }

        /* Construct a decoder for the arraysize and datatype. */
        Decoder dec;
        if ( datatype.equals( "boolean" ) ) {
            dec = isScalar ? BooleanDecoder.createScalarBooleanDecoder()
                           : new BooleanDecoder( arraysize );
        }
        else if ( datatype.equals( "bit" ) ) {
            dec = isScalar ? BitDecoder.createScalarBitDecoder()
                           : new BitDecoder( arraysize );
        }
        else if ( datatype.equals( "unsignedByte" ) ) {
            dec = isScalar ? new NumericDecoder.ScalarUnsignedByteDecoder()
                           : new NumericDecoder.UnsignedByteDecoder( arraysize);
        }
        else if ( datatype.equals( "short" ) ) {
            dec = isScalar ? new NumericDecoder.ScalarShortDecoder()
                           : new NumericDecoder.ShortDecoder( arraysize );
        }
        else if ( datatype.equals( "int" ) ) {
            dec = isScalar ? new NumericDecoder.ScalarIntDecoder()
                           : new NumericDecoder.IntDecoder( arraysize );
        }
        else if ( datatype.equals( "long" ) ) {
            dec = isScalar ? new NumericDecoder.ScalarLongDecoder()
                           : new NumericDecoder.LongDecoder( arraysize );
        }
        else if ( datatype.equals( "char" ) ) {
            dec = CharDecoders.makeCharDecoder( arraysize );
        }
        else if ( datatype.equals( "unicodeChar" ) ) {
            dec = CharDecoders.makeUnicodeCharDecoder( arraysize );
        }
        else if ( datatype.equals( "float" ) ) {
            dec = isScalar ? new NumericDecoder.ScalarFloatDecoder()
                           : new NumericDecoder.FloatDecoder( arraysize );
        }
        else if ( datatype.equals( "double" ) ) {
            dec = isScalar ? new NumericDecoder.ScalarDoubleDecoder()
                           : new NumericDecoder.DoubleDecoder( arraysize );
        }
        else if ( datatype.equals( "floatComplex" ) ) {
            long[] arraysize2 = new long[ arraysize.length + 1 ];
            arraysize2[ 0 ] = 2L;
            System.arraycopy( arraysize, 0, arraysize2, 1, arraysize.length );
            dec = new NumericDecoder.FloatDecoder( arraysize2 );
        }
        else if ( datatype.equals( "doubleComplex" ) ) {
            long[] arraysize2 = new long[ arraysize.length + 1 ];
            arraysize2[ 0 ] = 2L;
            System.arraycopy( arraysize, 0, arraysize2, 1, arraysize.length );
            dec = new NumericDecoder.DoubleDecoder( arraysize2 );
        }
        else {
            logger.warning( "Unknown data type " + datatype +
                            " - treat as string"
                          + ", but may cause problems" );
            dec = new UnknownDecoder();
        }

        /* Set the blank value. */
        if ( blank != null && blank.trim().length() > 0 ) {
            try {
                dec.setNullValue( blank );
            }
            catch ( IllegalArgumentException e ) {
                logger.warning( "Bad null value " + blank );
            }
        }

        /* Return the new Decoder object. */
        return dec;
    }


    private static class UnknownDecoder extends Decoder {
        public UnknownDecoder() {
            super( String.class, new long[ 0 ] );
        }

        public Object decodeString( String txt ) {
            return txt;
        }

        public Object decodeStream( DataInput strm ) {
            throw new UnsupportedOperationException(
                "Can't do STREAM decode of unknown data type " + this );
        }

        public void skipStream( DataInput strm ) {
            decodeStream( strm );
        }

        void setNullValue( String txt ) {}

        public boolean isNull( Object array, int index ) {
            return false;
        }
    }

    static int[] longsToInts( long[] larray ) {
        int[] iarray = new int[ larray.length ];
        for ( int i = 0; i < larray.length; i++ ) {
            if ( larray[ i ] < Integer.MIN_VALUE ||
                 larray[ i ] > Integer.MAX_VALUE ) {
                throw new IndexOutOfBoundsException(
                    "Long value " + larray[ i ] + " out of integer range" );
            }
            else {
                iarray[ i ] = (int) larray[ i ];
            }
        }
        return iarray;
    }


}
