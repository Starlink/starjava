package uk.ac.starlink.ttools.moc;

import java.util.PrimitiveIterator;

/**
 * Defines an object capable of storing HEALPix tiles and producing
 * an ordered stream of spatial MOC UNIQ values.
 * Since a UNIQ value has a 1:1 mapping with a HEALPix tile,
 * the work required is to consolidate tiles within and between levels
 * and provide the result as an ordered sequence.
 *
 * <p>You make multiple calls to {@link #addTile}, then a call to
 * {@link #endTiles}, then the MOC is ready to interrogate.
 *
 * <p>In general instances of this interface are not expected to be
 * thread-safe.
 *
 * @author   Mark Taylor
 * @since    28 Jan 2025
 */
public interface MocBuilder {

    /**
     * Add a numbered tile at a given order.
     * Adding a tile that has already been added has no effect.
     *
     * @param  order  HEALPix order, in the range 0..29
     * @param  ipix   tile index within the given order,
     *                in the range 0..12*4^order
     */
    void addTile( int order, long ipix );

    /**
     * Signal that no further tiles will be added.
     */
    void endTiles();

    /**
     * Returns an array of the number of tiles present at each order
     * of the normalised MOC.  The length of the array gives the
     * number of orders present (the last element may not be zero,
     * unless only one element is returned).
     * The sum of the elements of this array gives the number of values
     * that will be returned by {@link #createOrderedUniqIterator}.
     *
     * <p>Should only be called after {@link #endTiles} has been called.
     */
    long[] getOrderCounts();

    /**
     * Returns an iterator over the UNIQ values represented by this MOC
     * in sequence.  That means that the lower-order ones come first.
     *
     * <p>Should only be called after {@link #endTiles} has been called.
     */
    PrimitiveIterator.OfLong createOrderedUniqIterator();
}
