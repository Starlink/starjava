package uk.ac.starlink.ttools.plot2;

import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * Translates the coordinates found in a Tuple to data space coordinates.
 * It also contains metadata about the coordinates to assist
 * in generating a UI to acquire them.
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
     * Determines the positional coordinates in data space for a supplied tuple.
     *
     * <p>A parameter supplies the index of the field in the tuple
     * at which the positional coordinate(s) can be found.
     * Each position is represented by {@link #getPosCoords} columns of
     * the tuple.
     * By convention positions are at the start of the tuple,
     * so if there is one position in the tuple it will be at icol=0,
     * and there are multiple positions the N'th one will be at
     * icol=N*getPosCoords().
     *
     * <p>An array of (at least) {@link #getDataDimCount} elements is
     * supplied, and on success the data space coordinate values
     * are written into it.
     *
     * @param   tuple  coordinate tuple
     * @param   icol   column index in <code>tuple</code> at which the
     *                 positional information starts
     * @param   dpos   array into which data space coordinates are written
     * @return  true  iff conversion was successful
     */
    boolean readDataPos( Tuple tuple, int icol, double[] dpos );

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
