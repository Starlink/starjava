package uk.ac.starlink.array;

/**
 * Implements the OffsetMapper interface to map between arrays with 
 * different shapes and/or ordering schemes.
 *
 * @author   Mark Taylor (Starlink)
 */
public class OrderedShapeMapper implements OffsetMapper {
    private final OrderedNDShape oshape1;
    private final OrderedNDShape oshape2;

    /**
     * Constructs an OffsetMapper which maps between two arrays with 
     * different pixel sequences.
     *
     * @param  oshape1  ordered shape giving the pixel sequence of array 1
     * @param  oshape2  ordered shape giving the pixel sequence of array 2
     */
    public OrderedShapeMapper( OrderedNDShape oshape1,
                               OrderedNDShape oshape2 ) {
        this.oshape1 = oshape1;
        this.oshape2 = oshape2;
    }

    public long mapOffset( long off1 ) {
        long[] pos = oshape1.offsetToPosition( off1 );
        return oshape2.within( pos ) ? oshape2.positionToOffset( pos ) : -1L;
    }

    public long[] mapRange( long[] range1 ) {

        /* Could improve efficiency by writing this method properly. */
        return null;
    }
}

