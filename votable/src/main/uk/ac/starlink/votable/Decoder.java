package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * Decoder object associated with a Field.
 * Instances of this class know about the size and shape of fields
 * as well as their numeric type, and can decode various data sources 
 * into the objects they represent.  To construct a decoder use the
 * static {@link makeDecoder} method.
 * 
 * <p>The various <tt>decode</tt> methods turn some kind of representation of
 * the given object into a standard representation.  The standard
 * representation is in accordance with the recommendations made in
 * the the {@link uk.ac.starlink.table} package.
 *
 * @author  Mark Taylor (Starlink)
 */
abstract class Decoder {

    static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );

    private Object blank;
    protected String blankString;
    protected boolean isVariable;
    protected int sliceSize;
    protected long[] arraysize;

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
     * stream as raw bytes.  The VOTable binary format is used (I hope).
     *
     * @param  strm  a DataInput object from which bytes are to be read
     */
    abstract public Object decodeStream( DataInput strm ) throws IOException;

    /**
     * Indicates whether an element of a given array matches the Null value
     * used by this decoder.
     *
     * @param  array  the array in which the element to check is
     * @param  index  the index into <tt>array</tt> at which the element to
     *         check is
     * @return <tt>true</tt> iff the <tt>index</tt>'th element of <tt>array</tt>
     *         matches the Null value for this decoder
     */
    abstract public boolean isNull( Object array, int index );

    /**
     * Returns the base class for objects returned by this decoder.
     * Objects returned by the <tt>decode*</tt> methods of this decoder
     * will be arrays of the class returned by this method.
     *
     * @param  the class of which decoded values are arrays (or possibly
     *         NDArrays or something, depending on <tt>packageArray</tt>
     */
    abstract public Class getBaseClass();

    /**
     * Does required setup for a decoder given its shape.
     *
     * @param  the dimensions of objects with this type - the last element
     *         of the array may be negative to indicate unknown slowest-varying
     *         dimension
     */
    protected Decoder( long[] arraysize ) {
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
        return sliceSize * ( isVariable ? strm.readInt() : 1 );
    }

    /**
     * Turns a primitive numeric array into an object suitable for
     * returning as the result of a cell query.
     * <p>The current implementation just returns the array itself;
     * in principle you could decide to return it as an NDArray or something.
     *
     * @param  array  a java array object
     */
    Object packageArray( Object array ) {

        /* If we've got a scalar or vector, the array itself will do. */
        if ( arraysize.length <= 1 ) {
            return array;
        }
        else {
            return array;
        }
    }

    /**
     * Returns an object array based on the given object, which may be
     * an array of arrays (for instance float[][] to represent a 2-d array).
     *
     * @param   aoa  an object encoding the values.  May be an array, an
     *          array of arrays, an array of arrays of arrays...
     *          All the base arrays must be of the same primitive or
     *          object type.
     * @return  an object containing the decoded values
     */
    public Object decodeArrayOfArrays( Object aoa ) {
        return packageArray( vectorise( aoa ) );
    }

    /**
     * Turns an array of arrays of some primitive type into a single
     * java primitive array.
     * <p>
     * This method is only necessary because the nom.tam.fits classes
     * return data as arrays of arrays..  I should really write my
     * own fits data access classes which return data as vectors in the
     * first place.
     *
     * @param   an array, or an array of arrays, or an array of arrays of
     *          arrays... all the base arrays must be of the same
     *          primitive or object type
     * @return  a single java array containing all the elements in the
     *          base arrays of <tt>aoa</tt>
     */
    private Object vectorise( Object aoa ) {
        Class cls = aoa.getClass();

        /* If it's a vector already, return it. */
        if ( isBasicArray( aoa.getClass() ) ) {
            return aoa;
        }

        /* Construct a List of 1-d arrays containing the data. */
        List loa = new ArrayList();
        accumulateListOfArrays( aoa, loa );
        int na = loa.size();
        int nel = 0;
        for ( Iterator it = loa.iterator(); it.hasNext(); ) {
            nel += Array.getLength( it.next() );
        }

        /* Create a new 1-d array to contain the elements. */
        Object vec = Array.newInstance( loa.get( 0 ).getClass()
                                           .getComponentType(), nel );

        /* Copy the elements in. */
        int vpos = 0;
        for ( Iterator it = loa.iterator(); it.hasNext(); ) {
            Object array = it.next();
            int asiz = Array.getLength( array );
            System.arraycopy( array, 0, vec, vpos, asiz );
            vpos += asiz;
        }
        return vec;
    }
    private static void accumulateListOfArrays( Object aoa, List loa ) {
        Class cls = aoa.getClass();
        if ( isBasicArray( cls ) ) {
            loa.add( aoa );
        }
        else {
            int na = Array.getLength( aoa );
            for ( int i = 0; i < na; i++ ) {
                accumulateListOfArrays( Array.get( aoa, i ), loa );
            }
        }
    }
    private static boolean isBasicArray( Class cls ) {
        return cls.getComponentType().getComponentType() == null;
    }

    /**
     * Create a decoder given its datatype, shape and blank (bad) value.
     * The shape is specified by the <tt>arraysize</tt> parameter, 
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
        Decoder dec;
        if ( datatype.equals( "boolean" ) ) {
            dec = new BooleanDecoder( arraysize );
        }
        else if ( datatype.equals( "bit" ) ) {
            dec = new BitDecoder( arraysize );
        }
        else if ( datatype.equals( "unsignedByte" ) ) {
            dec = new UnsignedByteDecoder( arraysize );
        }
        else if ( datatype.equals( "short" ) ) {
            dec = new ShortDecoder( arraysize );
        }
        else if ( datatype.equals( "int" ) ) {
            dec = new IntDecoder( arraysize );
        }
        else if ( datatype.equals( "long" ) ) {
            dec = new LongDecoder( arraysize );
        }
        else if ( datatype.equals( "char" ) ) {
            dec = new CharDecoder( arraysize );
        }
        else if ( datatype.equals( "unicodeChar" ) ) {
            dec = new UnicodeCharDecoder( arraysize );
        }
        else if ( datatype.equals( "float" ) ) {
            dec = new FloatDecoder( arraysize );
        }
        else if ( datatype.equals( "double" ) ) {
            dec = new DoubleDecoder( arraysize );
        }
        else if ( datatype.equals( "floatComplex" ) ) {
            long[] arraysize2 = new long[ arraysize.length + 1 ];
            arraysize2[ 0 ] = 2L;
            System.arraycopy( arraysize, 0, arraysize2, 1, arraysize.length );
            dec = new FloatDecoder( arraysize2 );
        }
        else if ( datatype.equals( "doubleComplex" ) ) {
            long[] arraysize2 = new long[ arraysize.length + 1 ];
            arraysize2[ 0 ] = 2L;
            System.arraycopy( arraysize, 0, arraysize2, 1, arraysize.length );
            dec = new DoubleDecoder( arraysize2 );
        }
        else {
            logger.warning( "Unknown data type " + datatype + 
                         " - may cause processing problems" );
            dec = new UnknownDecoder( arraysize );
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
        public UnknownDecoder( long[] arraysize ) {
            super( new long[] { -1L } );
        }

        public Class getBaseClass() {
            return String.class;
        }

        public Object decodeString( String txt ) {
            StringTokenizer st = new StringTokenizer( txt );
            int ntok = st.countTokens();
            String[] result = new String[ ntok ];
            for ( int i = 0; i < ntok; i++ ) {
                result[ i ] = st.nextToken();
            }
            return result;
        }

        public Object decodeStream( DataInput strm ) {
            throw new UnsupportedOperationException(
                "Can't do STREAM decode of unknown data type " + this );
        }

        void setNullValue( String txt ) {}

        public boolean isNull( Object array, int index ) {
            return false;
        }
    }

    private static int[] longsToInts( long[] larray ) {
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
