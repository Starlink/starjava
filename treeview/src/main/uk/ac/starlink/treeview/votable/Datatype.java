package uk.ac.starlink.treeview.votable;

import java.lang.reflect.Array;
import java.util.StringTokenizer;
import uk.ac.starlink.array.ArrayArrayImpl;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;

/**
 * Datatype object associated with a Field.
 * Instances of this class know about the size and shape of fields
 * as well as their numeric type, and provided methods for turning strings
 * or bytestreams into arrays of java objects.
 *
 * @author  Mark Taylor (Starlink)
 */
public abstract class Datatype {

    /**
     * Returns an object array based on the given text (space-separated
     * values for numeric types, normal string values for characters).
     * For numeric types the returned object will be a java array of 
     * the corresponding primitive type (the array may only have one
     * element for scalars).  For character types the result will be
     * a String (for zero or one dimensions) or an array of Strings
     * (for more than one dimension).
     *
     * @param   txt  a string encoding one or many values
     * @return  an object containing the decoded values
     */
    public abstract Object decodeString( String txt );

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

    public String getName() {
        return name;
    }

    public static Datatype makeDatatype( String name, long[] arraysize,
                                         String blank ) {
        if ( name.equals( "int" ) ||
             name.equals( "short" ) ||
             name.equals( "unsignedByte" ) ) {
            return new IntDatatype( name, arraysize, blank );
        }
        else if ( name.equals( "float" ) ||
                  name.equals( "double" ) ) {
            return new FloatDatatype( name, arraysize, blank );
        }
        else if ( name.equals( "char" ) ||
                  name.equals( "unicodeChar" ) ) {
            return new CharDatatype( name, arraysize, blank );
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

        public Number getNull() {
            return ( bad == 0 ) ? null : new Integer( bad );
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
    }

}
