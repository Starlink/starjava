package uk.ac.starlink.ttools.plot2;

import uk.ac.starlink.util.Sequence;
import uk.ac.starlink.util.Splittable;

/**
 * Interface for iterating over coordinates.
 * At each step, an N-dimensional coordinate array is available.
 * This sequence is splittable, so suitable in priniciple
 * for parallel processing.
 *
 * @author   Mark Taylor
 * @since    13 Seb 2019
 */
public interface CoordSequence extends Splittable<CoordSequence>, Sequence {

    /**
     * Returns the array used to store the coordinates for the current
     * position in this sequence.
     * It contains the coordinates corresponding to the last call of
     * the {@link #next} method.  Its contents before the first call
     * or after a call returning false are undefined.
     *
     * <p>This method returns the same value throughout the lifetime of
     * this sequence, it's only the contents that change to reflect
     * the current state of the iteration.
     */
    double[] getCoords();

    /**
     * Advances to the next entry.  No exception is thrown.
     */
    boolean next();
}
