package uk.ac.starlink.ttools.cone;

/**
 * Defines a pixellation scheme which maps sky positions to long integers.
 *
 * @author   Mark Taylor
 * @since    6 Dec 2007
 */
public interface SkyTiling {

    /**
     * Returns the index of the tile in which a given sky position falls.
     *
     * @param   ra   right ascension in degrees
     * @param   dec  declination in degrees
     * @return  tile index for position
     */
    long getPositionTile( double ra, double dec );

    /**
     * Returns the bounds of a range of pixels which is known to cover
     * a given cone on the sky.
     * The return value is a two-element array, (lo,hi).
     * Every point in the specified cone must have a tile index which is
     * greater than or equal to the first element of that array, and
     * less than or equal to the second element.
     * If the question cannot be answered, or if the range is thought to
     * be too large to be of use (for instance, if it would take a
     * long time to calculate), then <code>null</code> may be returned.
     *
     * @param   ra   right ascension in degrees
     * @param   dec  declination in degrees
     * @param   radius  radius in degrees
     * @return  2-element array giving inclusive (low, high) bounds of
     *          tile range covering the cone
     */
    long[] getTileRange( double ra, double dec, double radius );
}
