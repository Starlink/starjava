package uk.ac.starlink.array;

import java.util.NoSuchElementException;

/**
 * Iterates over rows in an intersection between two NDShapes.
 * This utility class is provided to aid in reading and writing tiles; 
 * by creating an instance based on a pair of shapes, it will work out
 * the intersection and iterate over rows in that intersection which 
 * correspond to each other.
 * <p>
 * Normally the hasNext and next methods can be used to iterate from one
 * row (sequence of contiguous pixels) to the next, with getOffsetA and
 * getOffsetB giving the starting offset in the two base shapes and
 * getRowLength giving the length of the row (getRowLength is a constant -
 * it does not change between iterations).
 * If there is no overlap then getIntersection returns null and 
 * hasNext returns false straight off.
 * 
 * @author   Mark Taylor (Starlink)
 */
class IntersectionRowIterator {

    private OrderedNDShape aShape;
    private OrderedNDShape bShape;
    private OrderedNDShape inter;
    private boolean fitsLike;
    private int ndim;
    private long[] interDims;
    private long[] aDims;
    private long[] bDims;
    private int dim0;      // fastest varying dimension
    private int ndim1;     // dimensionality - 1
    private long[] interPos;
    private long aOff;
    private long bOff;
    private long nleft;

    /**
     * Construct an iterator based on two shapes of the same dimensionality
     * and a given pixel ordering scheme.
     *
     * @param  shapeA  one shape
     * @param  shapeB  the other shape
     * @param  order   the pixel ordering scheme to be used in the 
     *                 intersection iterations
     * @throws IllegalArgumentException  if shapeA and shapeB have 
     *         different dimensionalities
     *
     */
    public IntersectionRowIterator( NDShape shapeA, NDShape shapeB, 
                                    Order order ) {
        this.aShape = new OrderedNDShape( shapeA, order );
        this.bShape = new OrderedNDShape( shapeB, order );
        fitsLike = order.isFitsLike();
        NDShape intersect = shapeA.intersection( shapeB );
        if ( intersect != null ) {
            inter = new OrderedNDShape( intersect, order );
            ndim = shapeA.getNumDims();
            ndim1 = ndim - 1;
            dim0 = fitsLike ? 0 : ndim1;
            aDims = aShape.getDims();
            bDims = bShape.getDims();
            interDims = inter.getDims();
            nleft = 1L;
            for ( int j = 0; j < ndim1; j++ ) {
                int i = fitsLike ? ( j + 1 ) : ( ndim1 - 1 - j );
                nleft *= interDims[ i ];
            }
            interPos = new long[ ndim ];
            long[] absolutePos = inter.offsetToPosition( 0L );
            aOff = aShape.positionToOffset( absolutePos );
            bOff = bShape.positionToOffset( absolutePos );
        }
        else {
            nleft = 0L;
            aOff = -1L;
            bOff = -1L;
            inter = null;
        }
    }

    /**
     * Indicates whether there are more rows left.
     *
     * @return   true if there are more rows
     */
    public boolean hasNext() {
        return nleft > 0L;
    }

    /**
     * Moves to the next row.
     *
     * @throws   NoSuchElementException  if hasNext would return false
     */
    public void next() {
        long aStride = aDims[ dim0 ];
        long bStride = bDims[ dim0 ];
        for ( int j = 0; j < ndim1; j++ ) {
            int i = fitsLike ? j + 1 : ( ndim1 - 1 - j );
            aOff += aStride;
            bOff += bStride;
            if ( ++interPos[ i ] < interDims[ i ] ) {
                break;
            }
            else {
                interPos[ i ] = 0L;
                aOff -= interDims[ i ] * aStride;
                bOff -= interDims[ i ] * bStride;
                aStride *= aDims[ i ];
                bStride *= bDims[ i ];
            }
        }
        if ( --nleft < 0L ) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the offset into shape A at which this row begins.
     */
    public long getOffsetA() {
        return aOff;
    }

    /**
     * Returns the offset into shape B at which this row begins.
     */
    public long getOffsetB() {
        return bOff;
    }

    /**
     * Returns the intersection between the two shapes.  If there are
     * no pixels in common, null is returned.
     */
    public OrderedNDShape getIntersection() {
        return inter;
    }

    /**
     * Returns the length of each row - the number of contiguous pixels
     * starting at offsetA in shape A which correspond to pixels starting
     * at offsetB in shape B.  This return value is constant over the
     * life of this object.
     */
    public long getRowLength() {
        return interDims[ dim0 ];
    }

}
