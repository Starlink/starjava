package uk.ac.starlink.array;

import java.util.Arrays;
import java.util.Iterator;
import java.io.IOException;

/**
 * Wraps an NDArray to produce a virtual ArrayImpl with a different shape.
 * The result is a 'window' on the base array; a given position
 * (coordinate vector) refers to the same pixel in both base and
 * wrapping array, but pixels outside the window will not exist in
 * the wrapping array and pixels in the window which do not fall within
 * the bounds of the base array are given the bad value (for a readable
 * array) or ignored (for a writable array).
 * <p>
 * The window may fall wholly or partially within or without the
 * bounds of the base array, but must be of the same dimensionality as it.
 *
 * @author   Mark Taylor (Starlink)
 */
public class WindowArrayImpl extends WrapperArrayImpl {

    private final NDArray nda;
    private final NDShape window;
    private final OrderedNDShape oshape;
    private final BadHandler badHandler;
    private final OrderedNDShape baseShape;
    private final OrderedNDShape inter;
    private final Order order;
    private final long firstIn;
    private final long lastIn;
    private final long npix;
    private final long baseNpix;

    /**
     * Constructs a new ArrayImpl from a base NDArray and a given window shape.
     *
     * @param  nda    the base NDArray which supplies the original pixel values
     * @param  window the shape of the window through which the base 
     *                array will be viewed.  Must have the same number
     *                of dimensions as <code>nda</code>
     * @throws  IllegalArgumentException  if window has a different number of
     *                dimensions from <code>nda</code>
     */
    public WindowArrayImpl( NDArray nda, NDShape window ) {
        super( nda );
        this.nda = nda;
        this.window = window;

        if ( nda.getShape().getNumDims() != window.getNumDims() ) {
            throw new IllegalArgumentException( "Dimensionality mismatch: " +
                "window dims (" + window.getNumDims() + ") != " +
                "NDArray dims (" + nda.getShape().getNumDims() + ")" );
        }

        baseShape = nda.getShape();
        order = baseShape.getOrder();
        oshape = new OrderedNDShape( window, order );
        npix = oshape.getNumPixels();
        baseNpix = baseShape.getNumPixels();
        badHandler = nda.getBadHandler();
        NDShape intersect = baseShape.intersection( oshape );

        if ( intersect != null ) {
            inter = new OrderedNDShape( intersect, order );
            long np1 = inter.getNumPixels() - 1L;
            firstIn = oshape.positionToOffset( inter.offsetToPosition( 0L ) );
            lastIn = oshape.positionToOffset( inter.offsetToPosition( np1 ) );
        }
        else {
            inter = null;
            firstIn = 0L;
            lastIn = 0L;
        }
    }

    public OrderedNDShape getShape() {
        return oshape;
    }

    public BadHandler getBadHandler() {
        return badHandler;
    }

    public AccessImpl getAccess() throws IOException {
        if ( inter != null ) {
            return new AccessImpl() {

                private ArrayAccess acc = nda.getAccess();
                private long offset = 0L;

                public void setOffset( long off ) {
                    offset = off;
                }

                public void read( Object buffer, int start, int size ) 
                        throws IOException {
                    while ( size > 0 ) {
                        if ( inCommon( offset ) ) {
                            int num = numCommon( offset, size );
                            acc.setOffset( baseOff( offset ) );
                            acc.read( buffer, start, num );
                            size -= num;
                            start += num;
                            offset += num;
                        }
                        else {
                            int num = numNotCommon( offset, size );
                            badHandler.putBad( buffer, start, num );
                            size -= num;
                            start += num;
                            offset += num;
                        }
                    }
                }
   
                public void write( Object buffer, int start, int size )
                        throws IOException {
                    while ( size > 0 ) {
                        if ( inCommon( start ) ) {
                            int num = numCommon( offset, size ); 
                            acc.setOffset( baseOff( offset ) );
                            acc.write( buffer, start, num );
                            size -= num;
                            start += num;
                            offset += num;
                        }
                        else {
                            int num = numNotCommon( offset, size ); 
                            size -= num;
                            start += num;
                            offset += num;
                        }
                    }
                }

                public void close() throws IOException {
                    acc.close();
                }
            };
        }

        /* No intersection - return a trivial accessor. */
        else {
            return new AccessImpl() {
                public void setOffset( long off ) {
                }
                public void read( Object buffer, int start, int size ) {
                    badHandler.putBad( buffer, start, size );
                }
                public void write( Object buffer, int start, int size ) {
                }
                public void close() {
                }
            };
        }
    }

    /**
     * Gives the offset into the base array corresponding to the
     * offset into this window array. 
     */
    private long baseOff( long off ) {
        return baseShape.positionToOffset( oshape.offsetToPosition( off ) );
    }

    /**
     * Indicates whether the offset into this window array represents
     * a pixel which is in the base array.
     */
    private boolean inCommon( long off ) {
        return baseShape.within( oshape.offsetToPosition( off ) );
    }

    /**
     * Given that the pixel at offset off is in the base array, 
     * what is the number of contiguous pixels starting at off 
     * (inclusive) which are all contiguous in the base array?  
     * May return a lower limit.
     *
     * Should probably come up with a faster implementation.
     *
     * @param  the starting offset in this array; must be in
     *         the intersection
     * @param  the maximum value to be returned
     */
    private int numCommon( long off, int max ) {
        long boff = baseOff( off );
        Iterator iw = oshape.pixelIterator( off, npix - off );
        Iterator ib = baseShape.pixelIterator( boff, baseNpix - boff );
        iw.next();
        ib.next();
        int ncomm = 1;
        while ( iw.hasNext() && ib.hasNext() &&
                Arrays.equals( (long[]) iw.next(), (long[]) ib.next() ) &&
                --max > 0 ) {
            ncomm++;
        }
        return ncomm;
    }

    /**
     * Given that the pixel at offset off is not in the base array,
     * what is the number of contiguous pixels starting at off
     * (inclusive) which are all not in the base array?
     * May return a lower limit.
     *
     * Should come up with a faster implementation.
     *
     * @param  the starting offset in this array; must be out of 
     *         the intersection
     * @param  the maximum value to be returned
     */
    private int numNotCommon( long off, int max ) {
        if ( off < firstIn ) {
            return (int) Math.min( firstIn - off, (long) max );
        }
        else if ( off > lastIn ) {
            return (int) Math.min( npix - off, (long) max );
        }
        else {
            Iterator iw = oshape.pixelIterator( off, npix - off );
            int nncomm = 1;
            iw.next();
            while ( iw.hasNext() && 
                    ! baseShape.within( (long[]) iw.next() ) &&
                    --max > 0 ) {
                nncomm++;
            }
            return nncomm;
        }
    }
}
