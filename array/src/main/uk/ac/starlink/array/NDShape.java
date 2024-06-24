package uk.ac.starlink.array;

import java.util.Arrays;

/**
 * Represents the shape of an N-dimensional rectangular array.
 * The shape is represented by an N-element array of longs giving the
 * origin (coordinates of the first pixel) and another N-element array
 * of longs giving the dimensions (number of pixels in each dimension).
 * This shape is considered to contain pixels with coordinate 
 * <code>origin[i]&lt;=pos[i]&lt;origin[i]+dims[i]</code> in each dimension i.
 * An Iterator over all these pixels may be obtained by using the
 * {@link OrderedNDShape} class.
 * <p>
 * This object is immutable.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class NDShape implements Cloneable {

    /**
     * The default value of the origin in each dimension; its value is 1.
     * This value is used by the constructors which do not take origin arrays.
     */
    // Why isn't this zero?  It's unity because that is the default
    // for HDS arrays; an HDS array with no explicit origin information
    // is understood by the Starlink ARY system as being the same as one with
    // an origin of (1,1,..).  In order for the NDArray system to behave
    // the same way, this value has to be 1.  It looks a bit fortranny,
    // but doesn't really change anything, since you should never assume
    // the origin is anywhere in particular for an NDArray, and pixel
    // iterators etc are provided for you.  Think of it as like pixel
    // indices (in the NDF sense).
    public final static long DEFAULT_ORIGIN = 1L;

    /* Basic attributes */
    private final long[] origin;
    private final long[] dims;

    /* Derived attributes */
    private final int ndim;
    private final long[] limits;  // = origin + dims
    private final long[] ubnds;   // = origin + dims - 1
    private final long npix;

    /**
     * Creates an NDShape object from its origin and dimensions.
     *
     * @param   origin  an array representing the origin
     * @param   dims  an array representing the dimension extents
     * @throws  IllegalArgumentException  if origin and dims have different
     *          lengths or any of the dimensions are not positive
     */
    public NDShape( long[] origin, long[] dims ) {

        /* Store and validate basic attributes. */
        this.origin = (long[]) origin.clone();
        this.dims = (long[]) dims.clone();
        validate( origin, dims );

        /* Calculate and cache derived attributes. */
        ndim = dims.length;
        limits = new long[ ndim ];
        ubnds = new long[ ndim ];
        long np = 1L;
        for ( int i = 0; i < ndim; i++ ) {
            limits[ i ] = origin[ i ] + dims[ i ];
            ubnds[ i ] = limits[ i ] - 1;
            np *= dims[ i ];
        }
        npix = np;
    }

    /**
     * Creates an NDShape object from its origin and an integer array of
     * dimensions.
     *
     * @param   origin  an array representing the origin
     * @param   dims  an array representing the dimension extents
     * @throws  IllegalArgumentException  if origin and dims have different
     *          lengths or any of the dimensions are not positive
     */
    public NDShape( long[] origin, int[] dims ) {
        this( origin, intsToLongs( dims ) );
    }

    /**
     * Creates an NDShape object with a default origin from its dimensions.
     * Each element of the origin is taken to be {@link #DEFAULT_ORIGIN}.
     *
     * @param   dims  an array representing the dimension extents
     * @throws  IllegalArgumentException  if any of the dimensions are
     *          not positive
     */
    public NDShape( long[] dims ) {
        this( defaultOrigin( dims.length ), dims );
    }

    /**
     * Creates an NDShape object with a default origin from an integer
     * array of dimensions.
     * Each element of the origin is taken to be {@link #DEFAULT_ORIGIN}.
     *
     * @param   dims  an array representing the dimension extents
     * @throws  IllegalArgumentException  if any of the dimensions are
     *          not positive
     */
    public NDShape( int[] dims ) {
        this( defaultOrigin( dims.length ), dims );
    }

    /**
     * Creates an NDShape object with the same origin and dimensions as
     * an existing one.  Note this can be used to construct an 
     * object of class <code>NDShape</code> from an {@link OrderedNDShape}.
     *
     * @param   shape  existing NDShape object
     */
    public NDShape( NDShape shape ) {
        this( shape.getOrigin(), shape.getDims() );
    }
 
    /**
     * Returns the origin in each dimension of the NDShape.
     * 
     * @return   an array giving the origin of this shape.
     *           Changing this array will not affect the NDShape object.
     */
    public long[] getOrigin() {
        return (long[]) origin.clone();
    }

    /**
     * Returns the extents in each dimension of the NDShape.
     *
     * @return   an array giving the dimensions of this shape.
     *           Changing this array will not affect the NDShape object.
     */
    public long[] getDims() {
        return (long[]) dims.clone();
    }

    /**
     * Returns the exclusive upper limits in each dimension of the NDShape.
     * <code>limits[i]=origin[i]+dims[i]</code>.
     *
     * @return   an array giving the upper limits of this shape.
     *           Changing this array will not affect the NDShape object.
     */
    public long[] getLimits() {
        return (long[]) limits.clone();
    }

    /**
     * Returns the inclusive upper limits in each dimension of the NDShape.
     * <code>limits[i]=origin[i]+dims[i]-1</code>.
     *
     * @return   an array giving the upper limits of this shape.
     *           Changing this array will not affect the NDShape object.
     */
    public long[] getUpperBounds() {
        return (long[]) ubnds.clone();
    }

    /**
     * Returns the dimensionality of the NDShape.
     *
     * @return  the number of dimensions the array has.
     */
    public int getNumDims() {
        return ndim;
    }

    /**
     * Returns the number of cells in the array represented by this NDShape.
     *
     * @return  the number of cells in the array
     */
    public long getNumPixels() {
       return npix;
    }

    /**
     * Returns a NDShape giving the intersection between this shape and 
     * another one.
     *
     * @param  other  the other shape
     * @return   a new NDShape representing the overlap between this and other.
     *           If there is no such intersection (no pixels present in both)
     *           then null is returned.
     * @throws  IllegalArgumentException  if the other has a different
     *          dimensionality to this shape
     */
    public NDShape intersection( NDShape other ) {
        if ( other.getNumDims() != this.getNumDims() ) {
            throw new IllegalArgumentException(
                "Dimensionality mismatch between" + other + " and " + this );
        }
        long[] iOrigin = new long[ ndim ];
        long[] iDims = new long[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            long otherLimit = other.origin[ i ] + other.dims[ i ];
            iOrigin[ i ] = Math.max( this.origin[ i ], other.origin[ i ] );
            long iLimit = Math.min( limits[ i ], otherLimit );
            iDims[ i ] = iLimit - iOrigin[ i ];
            if ( iDims[ i ] <= 0 ) {
                return null;
            }
        }
        return new NDShape( iOrigin, iDims );
    }

    /**
     * Returns a NDShape giving the union of this shape and another one.
     *
     * @param  other  the other shape
     * @return  a new NDShape, the smallest possible which contains all 
     *          the pixels in this one and all the pixels in other
     * @throws  IllegalArgumentException  if the other has a different
     *          dimensionality to this shape
     */
    public NDShape union( NDShape other ) {
        if ( other.getNumDims() != this.getNumDims() ) {
            throw new IllegalArgumentException(
                "Dimensionality mismatch between" + other + " and " + this );
        }
        long[] uOrigin = new long[ ndim ];
        long[] uDims = new long[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            long otherLimit = other.origin[ i ] + other.dims[ i ];
            uOrigin[ i ] = Math.min( this.origin[ i ], other.origin[ i ] );
            long uLimit = Math.max( limits[ i ], otherLimit );
            uDims[ i ] = uLimit - uOrigin[ i ];
        }
        return new NDShape( uOrigin, uDims );
    }

    /**
     * Indicates whether a given point is within this shape. 
     *
     * @param  pos  the coordinates of a position
     * @return true if each for each dimension <code>i</code>, 
     *         <code>origin[i]&lt;=pos[i]&lt;origin[i]+dims[i]</code>
     */
    public boolean within( long[] pos ) {
        for ( int i = 0; i < ndim; i++ ) {
            if ( pos[ i ] < origin[ i ] ||
                 pos[ i ] >= limits[ i ] ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indicates whether another object represents the same shape as this.
     * Two shapes are the same if they have the same origin and dimensions.
     * Note this call differs from the equals method in that the shape of
     * an NDShape may be compared with that of an object of one of 
     * its subclasses.
     *
     * @param  other  an NDShape object for comparison with this one
     */
    public boolean sameShape( NDShape other ) {
        return other != null
            && Arrays.equals( other.getOrigin(), this.getOrigin() ) 
            && Arrays.equals( other.getDims(), this.getDims() );
    }

    public boolean equals( Object other ) {
        if ( other != null && other.getClass().equals( this.getClass() ) ) {
            NDShape o = (NDShape) other;
            return Arrays.equals( o.origin, this.origin )
                && Arrays.equals( o.dims, this.dims );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int hash = 5;
        for ( int i = 0; i < ndim; i++ ) {
            hash = hash * 23 + (int) origin[ i ];
            hash = hash * 23 + (int) dims[ i ];
        }
        return hash;
    }

    public Object clone() {
        try {
            return super.clone();
        }
        catch ( CloneNotSupportedException e ) {
            throw new AssertionError();
        }
    }

    public String toString() {
        return toString( this );
    }

    /**
     * Returns a string representation of a shape.
     * This currently returns a string like "(10+5,20+8)" for a shape with
     * origin (10,20) and dimensions (5,8).
     * <p>
     * As a special case, if any of the origin elements has the value 
     * <code>Long.MIN_VALUE</code>, then a "<code>*</code>" is written
     * in the corresponding position.
     *
     * @param  shape  the shape to describe
     * @return   a string describing shape
     */
    public static String toString( NDShape shape ) {
        StringBuffer buf = new StringBuffer( "(" );
        int ndim = shape.ndim;
        for ( int i = 0; i < ndim; i++ ) {
            long o = shape.origin[ i ];
            if ( o == Long.MIN_VALUE ) {
                buf.append( '*' );
            }
            else {
                buf.append( shape.origin[ i ] )
                   .append( '+' )
                   .append( shape.dims[ i ] );
            }
            buf.append( ( i < ndim - 1 ) ? ',' : ')' );
        }
        return buf.toString();
    }

    /**
     * Returns a string representation of a position.
     * This is a utility function which returns a string indicating the
     * value of a position vector, in a form like "(10,20,23)".
     * <p>
     * As a special case, if any of the elements has the value 
     * <code>Long.MIN_VALUE</code>, then a "<code>*</code>" is written
     * in the corresponding position.
     *
     * @param   pos  a vector of longs
     * @return  a string representation of pos
     */
    public static String toString( long[] pos ) {
        StringBuffer buf = new StringBuffer( "(" );
        for ( int i = 0; i < pos.length; i++ ) {
            if ( i > 0 ) {
                buf.append( ',' );
            }
            long val = pos[ i ];
            if ( val != Long.MIN_VALUE ) {
                buf.append( val );
            }
            else {
                buf.append( '*' );
            }
        }
        buf.append( ')' );
        return buf.toString();
    }

    /**
     * Turns a string specification of a shape into an NDShape object.
     * This method is effectively the inverse of {@link #toString()}.
     * <p>
     * Each dimension specification is separated from the next using 
     * a comma, and may be given as <i>lower</i>:<i>upper</i> inclusive
     * bounds or <i>origin</i>+<i>dimension</i>.  So a 100x100 array
     * with origin (50,50) may be written:
     * <pre>
     *     50:149,50:149
     * </pre>
     * or
     * <pre>
     *     50+100,50+100
     * </pre>
     * Straggling whitespace is tolerated.
     *
     * @param  str  the string representing the shape.
     * @return the corresponding NDShape
     * @throws IllegalArgumentException  if <code>str</code> does not match
     *         one of the understood formats for a shape
     */
    public static NDShape fromString( String str ) {
        String[] specs = str.split( " *, *", -1 );
        int ndim = specs.length;
        long[] origin = new long[ ndim ];
        long[] dims = new long[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            String spec = specs[ i ];
            if ( spec.indexOf( ':' ) >= 0 ) {
                String[] lu = spec.split( " *: *", 2 );
                origin[ i ] = Integer.parseInt( lu[ 0 ] );
                dims[ i ] = Integer.parseInt( lu[ 1 ] ) - origin[ i ] + 1;
            }
            else if ( spec.indexOf( '+' ) >= 0 ) {
                String[] od = spec.split( " *\\+ *", 2 );
                origin[ i ] = Integer.parseInt( od[ 0 ] );
                dims[ i ] = Integer.parseInt( od[ 1 ] );
            }
            else {
                throw new IllegalArgumentException( "Bad shape format " + str );
            }
        }
        return new NDShape( origin, dims );
    }

    /**
     * Convenience method for converting an array of <code>int</code> values 
     * to an array of <code>long</code> values.
     *
     * @param  iarray  an array of integers
     * @return  an array of long integers with the same values as 
     * <code>iarray</code>
     */
    public static long[] intsToLongs( int[] iarray ) {
        long[] larray = new long[ iarray.length ];
        for ( int i = 0; i < iarray.length; i++ ) {
            larray[ i ] = iarray[ i ];
        }
        return larray;
    }

    /**
     * Convenience method for converting an array of <code>long</code> values
     * to an array of <code>int</code> values.  Unlike a normal java typecast,
     * if a conversion overflow occurs an IndexOutOfBoundsException 
     * will be thrown.
     *
     * @param  larray  an array of long integers
     * @return  an array of integers with the same values as <code>larray</code>
     * @throws   IndexOutOfBoundsException if any of the elements of
     *           <code>larray</code> is out of the range 
     *           <code>Integer.MIN_VALUE..Integer.MAX_VALUE</code>
     */
    public static int[] longsToInts( long[] larray ) {
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

    /**
     * Returns a default origin array of a given dimensionality. 
     * Each element has the value DEFAULT_ORIGIN.
     *
     * @param  ndim  the required dimensionality.
     */
    static long[] defaultOrigin( int ndim ) {
        long[] origin = new long[ ndim ];
        Arrays.fill( origin, DEFAULT_ORIGIN );
        return origin;
    }

    /**
     * Invoked by constructors to sanity check the private members.
     */
    private static void validate( long[] origin, long[] dims ) {
        if ( origin.length != dims.length ) {
            throw new IllegalArgumentException(
                "Origin and dimsension arrays have different lengths" );
        }
        if ( dims.length == 0 ) {
            throw new IllegalArgumentException(
                "Zero-dimensional shape not permitted" );
        }
        for ( int i = 0; i < origin.length; i++ ) {
            if ( dims[ i ] < 1 ) {
                throw new IllegalArgumentException(
                   "Dimensions less than 1 not allowed " +
                   toString( dims ) );
            }
        }
    }

}
