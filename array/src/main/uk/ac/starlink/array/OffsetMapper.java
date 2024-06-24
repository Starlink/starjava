package uk.ac.starlink.array;

/**
 * Defines a mapping of pixel offsets in one array to their offsets in
 * another array.
 * <p>
 * This interface is intended to describe the relationship between 
 * the positions of pixels in two arrays.  For each offset into array 1
 * of interest, the {@link #mapOffset} method returns the offset of the
 * corresponding pixel position in array 2.
 *
 * @author   Mark Taylor (Starlink)
 * @see   PixelMapArrayImpl
 */
public interface OffsetMapper {

    /**
     * Returns an offset into array 2 corresponding to a given offset
     * in array 1.
     * Any <code>long</code> value may be returned, though depending on the
     * the size of array 2 it may not fall within its bounds. 
     * A negative value always corresponds to a pixel which does not
     * exist in array 2.  It is the responsibility of clients
     * of this OffsetMapper to cope with such out-of-bounds return
     * values sensibly.
     *
     * @param   off1   the offset into the array 1 giving a pixel position
     * @return  the offset into array 2 giving the position of
     *          the corresponding pixel off1 into array 1
     */
    long mapOffset( long off1 );

    /**
     * Returns a two-element array <code>(min,max)</code>
     * indicating the range of mapping
     * output values (array 2 offsets) which correspond to a 
     * given range of input values (array 1 offsets).
     * The return value constitutes a guarantee that {@link #mapOffset}
     * will not return any value outside of the range returned, 
     * as long as no value outside of the supplied range parameter
     * is not supplied to it.  The returned range is only used
     * for efficiency purposes and may be conservative (a larger range 
     * than will actually be returned).  A null value may be returned;
     * this, like a return value of
     * <code>{-Long.MIN_VALUE,Long.MAX_VALUE}</code>
     * constitutes no guarantee about the mapping output values.
     * 
     * @param  range1 a two-element array giving the lowest value and highest
     *                value (inclusive) which will be supplied to the 
     *                <code>mapOffset</code> method (range to be considered 
     *                in array 1)
     * @return  a two-element array giving the lowest value and highest 
     *          value (inclusive) which could be returned by the 
     *          <code>mapOffset</code> method (range which could be returned
     *          in array 2) under the above circumstances.
     *          May be <code>null</code> if the information is not available
     */
    long[] mapRange( long[] range1 );
}
