package uk.ac.starlink.array;

/**
 * Implements the OffsetMapper interface to map between arrays with 
 * the same shape but different ordering schemes.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ReorderingMapper implements OffsetMapper {

    private final OrderedNDShape oshape1;
    private final OrderedNDShape oshape2;
    private final long npix;

    /**
     * Constructs an OffsetMapper which maps between two arrays with the
     * same shape but different pixel sequences.
     *
     * @param   shape   the common shape of the two arrays
     * @param   order1  the ordering scheme of array 1
     * @param   order2  the ordering scheme of array 2
     */
    public ReorderingMapper( NDShape shape, Order order1, Order order2 ) {
        this.oshape1 = new OrderedNDShape( shape, order1 );
        this.oshape2 = new OrderedNDShape( shape, order2 );
        npix = shape.getNumPixels();
    }

    public long mapOffset( long off1 ) {
        return oshape2.positionToOffset( oshape1.offsetToPosition( off1 ) );
    }

    public long[] mapRange( long[] range1 ) {
        if ( range1[ 0 ] < 0L || range1[ 1 ] >= npix ) {
            throw new IndexOutOfBoundsException(
                "Values outside of 0.." + ( npix - 1L ) + " not supported" );
        }
        return new long[] { 0L, npix - 1L };
    }
}
