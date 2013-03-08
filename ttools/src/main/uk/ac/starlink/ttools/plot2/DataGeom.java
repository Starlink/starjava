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
     * Determines the base positional coordinates in data space
     * for the current row of a supplied tuple sequence.
     * The positional coordinates are assumed to start at column
     * zero of the supplied tuple sequence.
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
