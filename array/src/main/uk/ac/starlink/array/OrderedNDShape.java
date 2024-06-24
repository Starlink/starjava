package uk.ac.starlink.array;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Represents the arrangement of pixels within an N-dimensional array.
 * Instances of this class are {@link NDShape}s with an additional
 * ordering specified which indicates the sequence in which pixels 
 * are encountered.  This class thus defines a mapping between an
 * ordered sequence of positions and their coordinates in N-dimensional
 * space.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class OrderedNDShape extends NDShape implements Cloneable {

    /* Basic Attributes. */
    private final long[] origin;
    private final long[] dims;
    private final Order order;

    /* Derived attributes. */
    private final int ndim;
    private final long[] limits;  // = origin + dims
    private final long npix;
    private final boolean hasFitsOrder;

    /**
     * Creates an OrderedNDShape from its origin, dimensions and ordering.
     *
     * @param   origin  an array representing the origin
     * @param   dims    an array representing the dimension extents
     * @param   order   an Order object specifying the pixel ordering scheme.
     *                  If the null value is supplied, an arbitrary ordering
     *                  scheme will be applied.
     * @throws  IllegalArgumentException  if origin and dims have different
     *          lengths or any of the dimensions are not positive
     */
    public OrderedNDShape( long[] origin, long[] dims, Order order ) {

        /* Invoke superconstructor. */
        super( origin, dims );

        /* Store basic attributes. */
        this.origin = (long[]) origin.clone();
        this.dims = (long[]) dims.clone();
        this.order = ( order == null ) ? Order.COLUMN_MAJOR : order;

        /* Calculate and cache derived attributes. */
        hasFitsOrder = this.order.isFitsLike();
        ndim = dims.length;
        long np = 1L;
        limits = new long[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            limits[ i ] = origin[ i ] + dims[ i ];
            np *= dims[ i ];
        }
        npix = np;
    }

    /**
     * Creates an OrderedNDShape with a default origin from its dimensions
     * and ordering.
     * Each element of the origin array has the value {@link #DEFAULT_ORIGIN}.
     *
     * @param   dims    an array representing the dimension extents
     * @param   order   an Order object specifying the pixel ordering scheme.
     *                  If the null value is supplied, an arbitrary ordering
     *                  scheme will be applied.
     * @throws  IllegalArgumentException  if any of the dimensions are not
     *          positive
     */
    public OrderedNDShape( long[] dims, Order order ) {
        this( defaultOrigin( dims.length ), dims, order );
    }

    /**
     * Creates an OrderedNDShape from a NDShape and an ordering.
     *
     * @param   shape  a NDShape object
     * @param   order  an Order object specifying the pixel ordering scheme
     *                  If the null value is supplied, an arbitrary ordering
     *                  scheme will be applied.
     */
    public OrderedNDShape( NDShape shape, Order order ) {
        this( shape.getOrigin(), shape.getDims(), order );
    }

    /**
     * Creates a new OrderedNDShape from an NDShape.
     * If <code>shape</code> is in fact an instance
     * of <code>OrderedNDShape</code>,
     * the created object will be equivalent to it.  If not, a new 
     * OrderedNDShape object will be created with an arbitrary 
     * ordering scheme.
     *
     * @param  shape  an NDShape or OrderedNDShape object
     */
    public OrderedNDShape( NDShape shape ) {
        this( shape,
              ( shape instanceof OrderedNDShape ) 
                   ? ((OrderedNDShape) shape).getOrder()
                   : null );
    }

    /**
     * Gets the ordering scheme of this object.
     *
     * @return  an Order object indicating the ordering scheme
     */
    public Order getOrder() {
        return order;
    }

    /**
     * Returns an Iterator which will iterate over a range of pixels 
     * in this OrderedNDShape's pixel sequence.  
     * The <code>next</code> 
     * method of the returned Iterator returns an array of longs 
     * giving the coordinates of the next pixel in the sequence.
     * Note that this long array is the same object every time, but its
     * contents change at each iteration.  It should not be modified
     * by clients.
     *
     * @param   start  the first offset in the pixel sequence over which to 
     *                 iterate
     * @param   length the number of pixels over which to iterate
     * @return  an Iterator over cells
     * @throws  IllegalArgumentException   if start and length would imply 
     *          iteration outside of this shape or length is negative
     */
    public Iterator pixelIterator( final long start, final long length ) {
        if ( start < 0L || length < 0L || start + length > npix ) {
            throw new IllegalArgumentException();
        }
        return new Iterator() {
            private long[] pos;
            {
                if ( start == 0L ) {
                    pos = new long[ ndim ];
                    for ( int i = 0; i < ndim; i++ ) {
                        pos[ i ] = limits[ i ] - 1L;
                    }
                }
                else {
                    pos = offsetToPosition( start - 1L );
                }
            }
            private long nleft = length;
            public boolean hasNext() {
                return nleft > 0;
            }
            public Object next() {
                if ( nleft-- > 0 ) {
                    for ( int j = 0; j < ndim; j++ ) {
                        int i = hasFitsOrder ? j : ( ndim - 1 - j );
                        if ( ++pos[ i ] < limits[ i ] ) {
                            break;
                        }
                        else {
                            pos[ i ] = origin[ i ];
                        }
                    }
                    return pos;
                }
                else {
                    throw new NoSuchElementException();
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns an Iterator which will iterate over all the pixels in this
     * OrderedNDShape's pixel sequence.
     * The <code>next</code> 
     * method of the returned Iterator returns an array of longs 
     * giving the coordinates of the next pixel in the sequence.
     * Note that this long array is the same object every time, but its
     * contents change at each iteration.  It should not be modified
     * by clients.
     * <p>
     * Equivalent to <code>pixelIterator(0,getNumPixels())</code>;
     *
     * @return  an Iterator over cells
     */
    public Iterator pixelIterator() {
        return pixelIterator( 0, npix );
    }
     

    /**
     * Returns the offset of a pixel having given coordinates within 
     * the sequence of pixels defined by this OrderedNDShape.
     * It will validate its input, and so may not be maximally efficient.
     *
     * @param   pos    a coordinate vector giving a pixel position
     * @return  the offset into this shape's pixel sequence at which the pixel
     *          at <code>pos</code> occurs
     * @throws  IndexOutOfBoundsException  if pos is outside this shape
     */
    public long positionToOffset( long[] pos ) {
        boolean hasFitsOrder = order.isFitsLike();
        long offset = 0L;
        long step = 1L;
        for ( int j = 0; j < ndim; j++ ) {
            int i = hasFitsOrder ? j : ( ndim - 1 - j );
            long c = pos[ i ];
            if ( c < origin[ i ] || c >= limits[ i ] ) {
                throw new IndexOutOfBoundsException();
            }
            offset += ( c - origin[ i ] ) * step;
            step *= dims[ i ];
        }
        return offset;
    }

    /**
     * Determines the coordinates of a pixel at a given offset within the
     * pixel sequence.
     * It will validate its input, and so may not be maximally efficient.
     *
     * @param   offset  an offset into the list of pixels
     * @return  the coordinates of the pixel at <code>offset</code>
     * @throws  IndexOutOfBoundsException  if <code>offset</code> 
     *          is outside this shape
     */
    public long[] offsetToPosition( long offset ) {
        boolean hasFitsOrder = order.isFitsLike();
        if ( offset < 0 || offset >= npix ) {
            throw new IndexOutOfBoundsException( 
                "Offset " + offset + " out of range 0.." + npix );
        }
        long[] p = (long[]) origin.clone();
        for ( int k = 0; k < ndim; k++ ) {
            int i = hasFitsOrder ? k : ( ndim - 1 - k );
            p[ i ] += offset % dims[ i ];
            offset /= dims[ i ];
        }
        return p;
    }

    public Object clone() {
        return super.clone();
    }

    /**
     * Indicates whether another object represents the same pixel sequence
     * as this.  Two shapes are the same if they have the same 
     * origins, dimensions and ordering scheme.  A true result from this 
     * method implies that pixel iterators returned from the two 
     * objects will behave in exactly the same way (present the same
     * pixel positions in the same order).
     *
     * @param  other  the shape to compare with this one
     * @return  true  iff the pixel sequences are the same
     */
    public boolean sameSequence( OrderedNDShape other ) {
        return Arrays.equals( other.getOrigin(), this.getOrigin() )
            && Arrays.equals( other.getDims(), this.getDims() )
            && ( other.getOrder() == this.getOrder() || ndim == 1 );
    }

    /**
     * Indicates whether another object is equivalent to this one.
     *
     * @param  other  an OrderedNDShape object for comparison with this one
     */
    public boolean equals( Object other ) {
        if ( other != null && other.getClass().equals( this.getClass() ) ) {
            OrderedNDShape o = (OrderedNDShape) other;
            return Arrays.equals( o.origin, this.origin )
                && Arrays.equals( o.dims, this.dims ) 
                && order.equals( o.getOrder() );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int hash = order.hashCode();
        for ( int i = 0; i < ndim; i++ ) {
            hash = hash * 23 + (int) origin[ i ];
            hash = hash * 23 + (int) dims[ i ];
        }
        return hash;
    }

    public String toString() {
        return super.toString() + ":" + order;
    }

}
