package uk.ac.starlink.treeview.votable;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import uk.ac.starlink.array.ArrayArrayImpl;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;

/**
 * Datatype object associated with a Field.
 * Instances of this class know about the size and shape of fields
 * as well as their numeric type, as well as how to decode various objects
 * into the objects they represent.
 * <p>
 * <h3>Value decoding<a name="decoding"></a></h3>
 * The various <tt>decode</tt> methods turn some kind of representation of
 * the given object into a standard representation.  The standard 
 * representation is based on the dimensionality and type of the datatype
 * object as follows:
 * <ul>
 * <li>Primitive numeric types (<tt>short</tt>, <tt>int</tt>, <tt>long</tt>, 
 *     <tt>float</tt>, <tt>double</tt>):
 *     a scalar is turned into a 1-element array of the
 *     appropriate primitive type; a vector is turned into an n-element
 *     array of the appropriate primitive type; and an N-dimensional
 *     array is turned into a {@link uk.ac.starlink.array.NDArray} of 
 *     the appropriate <tt>NDShape</tt> and <tt>Type</tt>
 * <li>Character types (<tt>char</tt>, <tt>unicodeChar</tt>):
 *     a scalar is turned into a 1-character String;
 *     a vector is turned into a String, and an N-dimensional array is
 *     turned into a vector of Strings (dimensions &gt;2 collapsed)
 * <li>Other types (<tt>boolean</tt>, <tt>bit</tt>, <tt>floatComplex</tt>,
 *     <tt>doubleComplex</tt>):
 *     currently unsupported
 * </ul>
 *
 * @author  Mark Taylor (Starlink)
 */
public abstract class Datatype {

    /**
     * Returns an object array based on the given text (space-separated
     * values for numeric types, normal string values for characters).
     *
     * @param   txt  a string encoding one or many values
     * @return  an object containing the decoded values
     * @see     #decoding
     */
    public abstract Object decodeString( String txt );

    /**
     * Returns an object array based on the given object, which may be 
     * an array of arrays (for instance float[][] to represent a 2-d array).
     *
     * @param   aoa  an object encoding the values.  May be an array, an
     *          array of arrays, an array of arrays of arrays... 
     *          All the base arrays must be of the same primitive or
     *          object type.
     * @return  an object containing the decoded values
     * @see     #decoding
     */
    public Object decodeArrayOfArrays( Object aoa ) {
        return packageArray( vectorise( aoa ) );
    }

    /**
     * Returns an object array read from the next bit of a given input
     * stream as raw bytes.  The VOTable binary format is used (I hope).
     */
    public abstract Object decodeStream( DataInput strm ) throws IOException;

    /**
     * Returns the blank value as a Number if an appropriate one exists.
     *
     * @return  a Number representing the bad value, or <tt>null</tt>
     */
    public Number getNull() {
        return null;
    }

    protected boolean isVariable;
    protected int sliceSize;
    private String name;
    private long[] arraysize;

    private Datatype( String name, long[] arraysize ) {
        this.name = name;
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
     * Returns the number of cells to use for this Datatype given a
     * certain number of available tokens.  This is just the arraysize
     * if it is fixed, or some large-enough multiple of the slice size
     * if it is variable.
     *
     * @param  ntok  the number of available tokens
     * @return  the number of cells to fill
     */
    protected int numCells( int ntok ) {
        if ( isVariable ) {
            return ( ( ntok + sliceSize - 1 ) / sliceSize ) * sliceSize;
        }
        else {
            return sliceSize;
        }
    }

    /**
     * Work out how many items are to be read from the supplied input stream.
     * This may be a fixed number for this Datatype, or it may require
     * reading a count from the stream.
     */
    protected int getNumItems( DataInput strm ) throws IOException {
        return sliceSize * ( isVariable ? strm.readInt() : 1 );
    }

    /**
     * Turns a primitive numeric array into an object suitable for 
     * returning as the result of a cell query.
     * The returned object may be the submitted array itself, or
     * it may be an NDArray based on it.
     *
     * @param  array  a numeric array of one of the types supported by
     *         NDArray
     */
    protected Object packageArray( Object array ) {

        /* If we've got a scalar or vector, the array itself will do. */
        if ( arraysize.length <= 1 ) {
            return array;
        }

        /* Otherwise, work out its shape and return it as an NDArray. */
        long[] dims = (long[]) arraysize.clone();
        if ( isVariable ) {
            dims[ dims.length - 1 ] = Array.getLength( array ) / sliceSize;
        }
        OrderedNDShape oshape = new OrderedNDShape( dims, Order.COLUMN_MAJOR );
        Number badval = getNull();
        return new BridgeNDArray( new ArrayArrayImpl( array, oshape, badval ) );
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

    private void accumulateListOfArrays( Object aoa, List loa ) {
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

    private boolean isBasicArray( Class cls ) {
        return cls.getComponentType().getComponentType() == null;
    }

    public String getName() {
        return name;
    }

    public static Datatype makeDatatype( String name, long[] arraysize,
                                         String blank ) {
        if ( name.equals( "int" ) ) {
            return new IntDatatype( name, arraysize, blank );
        }
        else if ( name.equals( "short" ) ) {
            return new ShortDatatype( name, arraysize, blank );
        }
        else if ( name.equals( "unsignedByte" ) ) {
            return new UnsignedByteDatatype( name, arraysize, blank );
        }
        else if ( name.equals( "float" ) ) {
            return new FloatDatatype( name, arraysize, blank );
        }
        else if ( name.equals( "double" ) ) {
            return new DoubleDatatype( name, arraysize, blank );
        }
        else if ( name.equals( "char" ) ) {
            return new CharDatatype( name, arraysize, blank );
        }
        else if ( name.equals( "unicodeChar" ) ) {
            return new UnicodeCharDatatype( name, arraysize, blank );
        }
        else {
            return new UnknownDatatype( name, arraysize, blank );
        }
    }


    private static class IntDatatype extends Datatype {
        private int bad = 0;

        public IntDatatype( String name, long[] arraysize, String blank ) {
            super( name, arraysize );
            if ( blank != null ) {
                try {
                    bad = Integer.parseInt( blank );
                }
                catch ( NumberFormatException e ) {
                    // never mind
                }
            }
        }
        
        public Object decodeString( String txt ) {
            StringTokenizer st = new StringTokenizer( txt );
            int ntok = st.countTokens();
            int ncell = numCells( ntok );
            int[] result = new int[ ncell ];
            for ( int i = 0; i < ncell; i++ ) {
                if ( i < ntok ) {
                    try {
                        result[ i ] = Integer.parseInt( st.nextToken() );
                    }
                    catch ( NumberFormatException e ) {
                        result[ i ] = bad;
                    }
                }
                else { 
                    result[ i ] = bad;
                }
            }
            return packageArray( result );
        }

        public Object decodeStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            int[] data = new int[ num ];
            for ( int i = 0; i < num; i++ ) {
                data[ i ] = strm.readInt();
            }
            return packageArray( data );
        }

        public Number getNull() {
            return ( bad == 0 ) ? null : new Integer( bad );
        }
    }

    private static class ShortDatatype extends IntDatatype {
        public ShortDatatype( String name, long[] arraysize, String blank ) {
            super( name, arraysize, blank );
        }

        public Object decodeStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            short[] data = new short[ num ];
            for ( int i = 0; i < num; i++ ) {
                data[ i ] = strm.readShort();
            }
            return packageArray( data );
        }
    }

    private static class UnsignedByteDatatype extends IntDatatype {
        public UnsignedByteDatatype( String name, long[] arraysize,
                                     String blank ) {
            super( name, arraysize, blank );
        }

        public Object decodeStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            short[] data = new short[ num ];  // note unsigned bytes need shorts
            for ( int i = 0; i < num; i++ ) {
                data[ i ] = (short) ( strm.readByte() | (short) 0x00 );
            }
            return packageArray( data );
        }
    }

    private static class FloatDatatype extends Datatype {
        private float bad = Float.NaN;

        public FloatDatatype( String name, long[] arraysize, String blank ) {
            super( name, arraysize );
        }

        public Object decodeString( String txt ) {
            StringTokenizer st = new StringTokenizer( txt );
            int ntok = st.countTokens();
            int ncell = numCells( ntok );
            float[] result = new float[ ncell ];
            for ( int i = 0; i < ncell; i++ ) {
                if ( i < ntok ) {
                    try {
                        result[ i ] = Float.parseFloat( st.nextToken() );
                    }
                    catch ( NumberFormatException e ) {
                        result[ i ] = bad;
                    }
                }
                else {
                    result[ i ] = bad;
                }
            }
            return packageArray( result );
        }

        public Number getNull() {
            return new Float( bad );
        }

        public Object decodeStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            float[] data = new float[ num ];
            for ( int i = 0; i < num; i++ ) {
                data[ i ] = strm.readFloat();
            }
            return packageArray( data );
        }
    }

    private static class DoubleDatatype extends FloatDatatype {
        public DoubleDatatype( String name, long[] arraysize, String blank ) {
            super( name, arraysize, blank );
        }

        public Object decodeStream( DataInput strm ) throws IOException {
            int num = getNumItems( strm );
            double[] data = new double[ num ];
            for ( int i = 0; i < num; i++ ) {
                data[ i ] = strm.readDouble();
            }
            return packageArray( data );
        }
    }

    private static class CharDatatype extends Datatype {
        private char bad = ' ';
        private long[] arraysize;

        public CharDatatype( String name, long[] arraysize, String blank ) {
            super( name, arraysize );
            this.arraysize = arraysize;
            if ( blank != null && blank.length() > 0 ) {
                bad = blank.charAt( 0 );
            }
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
                            sb.append( txt.charAt( k ) );
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
                data[ i ] = (char) ( strm.readByte() | (char) 0x00 );
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
    }

    private static class UnicodeCharDatatype extends CharDatatype {
        public UnicodeCharDatatype( String name, long[] arraysize,
                                    String blank ) {
            super( name, arraysize, blank );
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

    private static class UnknownDatatype extends Datatype {
        public UnknownDatatype( String name, long[] arraysize, String blank ) {
            super( name + "?? (unknown type)", new long[] { -1L } );
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
    }

}
