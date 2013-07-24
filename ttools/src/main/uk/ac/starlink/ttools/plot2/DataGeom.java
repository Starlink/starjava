package uk.ac.starlink.ttools.plot2;

import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Defines input positional coordinates for a plot
 * and their mapping to the data space of a suitable plot surface.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2013
 */
@Equality
public interface DataGeom {

    /**
     * Returns the dimensionality of the plot surface's plot space.
     *
     * @return   number of elements in data space coordinate array
     */
    int getDataDimCount();

    /**
     * Returns the definitions for the user-supplied coordinates
     * that indicate plot positions.
     *
     * @return   coordinate quantity array for this geometry
     */
    Coord[] getPosCoords();

    /**
     * Indicates whether the values read by the <code>readDataPos</code>
     * method correspond to a point position in the data space.
     * If true, a successful read will result in a position array
     * with a definite value for each coordinate.  If false, some of
     * the coordinates may be NaN.  A false return value would be
     * appropriate for instance if each tuple row for the plot layer
     * represented by this geom corresponds to a line rather than a
     * point in the data space.
     *
     * @return   true iff this geom represents point positions
     */
    boolean hasPosition();

    /**
     * Determines the base positional coordinates in data space
     * for the current row of a supplied tuple sequence.
     *
     * <p>An array of (at least) {@link #getDataDimCount} elements is
     * supplied, and on success the data space coordinate values of the
     * row of interest is written into it.
     *
     * @param   tseq   coordinate row sequence,
     *                 positioned at the row of interest
     * @param   icol   column index in <code>tseq</code> at which the geometry
     *                 columns start; in most cases this is zero
     * @param   dpos   array into which data space coordinates are written
     * @return  true  iff conversion was successful
     */
    boolean readDataPos( TupleSequence tseq, int icol, double[] dpos );

    /**
     * Returns a label for this DataGeom.
     * It may be used to distinguish from other geoms used in the
     * same plot type, so for instance call it "Cartesian" or "Polar"
     * rather than "Plane" if it's X,Y.
     *
     * @return   user-directed input coordinate type name
     */
    String getVariantName();
}
