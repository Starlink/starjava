package uk.ac.starlink.array;

import java.io.IOException;

/**
 * Wraps an NDArray to produce a virtual ArrayImpl in which each pixel is
 * the same as a pixel of the base array, but in a different order.
 * An {@link OffsetMapper} object is used to define the mapping between pixels
 * in the base array and pixels in this array.
 * <p>
 * Note that no assumptions about contiguity of pixels can be made
 * while doing bulk data access on a PixelMapNDArray; where applicable
 * a {@link WindowArrayImpl} is likely to be more efficient.
 *
 * @author   Mark Taylor (Starlink)
 * @see   OffsetMapper
 * @see   WindowArrayImpl
 */
public class PixelMapArrayImpl extends WrapperArrayImpl {
    private final NDArray nda;
    private final OrderedNDShape oshape;
    private final OffsetMapper mapper;
    private final boolean allWithin;

    /**
     * Constructs a new ArrayImpl from a base NDArray, a shape and
     * an OffsetMapper.
     */

    /**
     * Constructs a new ArrayImpl from a base NDArray and an OffsetMapper
     * object which transforms the offset of a pixel in the base array 
     * to the offset of the same pixel in this PixelMapArrayImpl.
     * Since for a non-degenerate case a monotonic pixel offset ordering
     * in a PixelMapNDArray is likely to correspond to a non-monotonic
     * offset ordering in its base array, it is only permitted to
     * construct a PixelMapNDArray from a base array which offers
     * random access.
     *
     * @param  nda     the base NDArray which supplies the pixels
     * @param  oshape  the shape and pixel sequence of the NDArray to be
     *                 constructed
     * @param  mapper  an OffsetMapper object which turns the offset of a
     *                 pixel in this NDArray into the offset of the
     *                 corresponding pixel in the base NDArray
     * @throws  UnsupportedOperationException  if nda does not have random
     *            access
     */
    public PixelMapArrayImpl( NDArray nda, OrderedNDShape oshape,
                              OffsetMapper mapper ) {
        super( nda );
        this.nda = nda;
        this.oshape = oshape;
        this.mapper = mapper;

        /* Check we have a random base array. */
        if ( ! nda.isRandom() ) {
            throw new UnsupportedOperationException(
                "Base NDArray " + nda + " does not support random access" );
        }

        /* Work out whether all the pixels in this ArrayImpl have
         * corresponding pixels in the base NDArray. */
       long[] range = new long[] { 0L, oshape.getNumPixels() - 1L };
       long[] baseRange = mapper.mapRange( range );
       allWithin = ( baseRange != null )
                && ( baseRange[ 0 ] >= 0L )
                && ( baseRange[ 1 ] <= nda.getShape().getNumPixels() - 1L );
    }

    public OrderedNDShape getShape() {
        return oshape;
    }

    public AccessImpl getAccess() throws IOException {

        /* The implementation of the accessor object differs for efficiency
         * reasons according to whether we may assume that every
         * offset in the wrapper array will get mapped to an offset
         * within the bounds of the base array. */

        /* Dispatch an accessor which assumes that all points in this
         * array correspond to a point within the base array. */
        if ( allWithin ) {
            return new AccessImpl() {
                private ArrayAccess baseAcc = nda.getAccess();
                private long offset = 0L;
                public void setOffset( long off ) throws IOException {
                    offset = off;
                }
                public void read( Object buffer, int start, int size )
                        throws IOException {
                    while ( size-- > 0 ) {
                        baseAcc.setOffset( mapper.mapOffset( offset++ ) );
                        baseAcc.read( buffer, start++, 1 );
                    }
                }
                public void write( Object buffer, int start, int size )
                        throws IOException {
                    while ( size-- > 0 ) {
                        baseAcc.setOffset( mapper.mapOffset( offset++ ) );
                        baseAcc.write( buffer, start++, 1 );
                    }
                }
                public void close() throws IOException {
                    baseAcc.close();
                }
            };
        }

        /* Dispatch an accessor which checks whether each point in
         * this array corresponds to a point within the base array
         * and does something sensible if it doesn't. */
        else {
            return new AccessImpl() {
                private ArrayAccess baseAcc = nda.getAccess();
                private BadHandler baseHandler = nda.getBadHandler();
                private long baseNpix = nda.getShape().getNumPixels();
                private long offset = 0L;
                public void setOffset( long off ) throws IOException {
                    offset = off;
                }
                public void read( Object buffer, int start, int size )
                        throws IOException {
                    while ( size-- > 0 ) {
                        long baseOff = mapper.mapOffset( offset++ );
                        if ( baseOff >= 0 && baseOff < baseNpix ) {
                            baseAcc.setOffset( baseOff );
                            baseAcc.read( buffer, start, 1 );
                        }
                        else {
                            baseHandler.putBad( buffer, start );
                        }
                        start++;
                    }
                }
                public void write( Object buffer, int start, int size )
                        throws IOException {
                    while ( size-- > 0 ) {
                        long baseOff = mapper.mapOffset( offset++ );
                        if ( baseOff >= 0 && baseOff < baseNpix ) {
                            baseAcc.setOffset( baseOff );
                            baseAcc.write( buffer, start, 1 );
                        }
                        else {
                            // drop this pixel on the floor
                        }
                        start++;
                    }
                }
                public void close() throws IOException {
                    baseAcc.close();
                }
            };
        }
    }
}


